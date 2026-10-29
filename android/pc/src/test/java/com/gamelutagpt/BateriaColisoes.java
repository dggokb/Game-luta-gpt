package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.pc.DadosDoJogo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * T25 da auditoria (docs/balanco/auditoria.md): o motor acerta quando o desenho encosta? Para cada
 * golpe, simula contra o Ryūga parado (em pé e agachado) em várias distâncias, desenha os dois
 * lutadores como a luta desenha (cada um numa camada) e compara os pixels opacos com a decisão do
 * motor:
 * <ul>
 *   <li>acerto fantasma: o motor acertou e os pixels não se tocam (folga em px do mundo);</li>
 *   <li>falha visual: o membro passa por dentro do corpo durante o golpe e o motor não acertou.</li>
 * </ul>
 * Roda à mão (classes do :pc no classpath): {@code java ... com.gamelutagpt.BateriaColisoes saida/ [ids]}.
 * Grava casos.tsv (um por tentativa) e PNGs dos piores casos com as caixas do motor por cima.
 */
public final class BateriaColisoes {
    private BateriaColisoes() {}

    static final String[] ELENCO = {"player_base", "p02", "p03", "p04", "p05", "p06", "p07", "p08",
        "p09", "p10", "p11", "p12", "p13", "p14", "p15", "p16"};
    static final String RIVAL = System.getProperty("t25.rival", "player_base");
    /** Pixel opaco: o que o motor das caixas também conta (rastro meio transparente não). */
    static final int ALFA = 128;
    static final int W = 1300, H = 1050;
    static final float CHAO_PX = 960f;

    static GameView jogo;
    static Method upd, drawF;
    static Field rendP, rendO;
    static Object efeitos;
    static Method drawProj;
    static final AndroidRenderCanvas RC = new AndroidRenderCanvas();
    static final Bitmap CAMADA_A = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    static final Bitmap CAMADA_D = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    static final Bitmap CAMADA_P = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    static final int[] PA = new int[W * H], PD = new int[W * H], PP = new int[W * H];

    /** Uma tentativa: golpe, postura do rival, distância (e o quadro do aperto no pulo). */
    static final class Caso {
        String quem, golpe, postura;
        int distancia, aperto = -1;
        boolean acertou;
        int quadroAcerto = -1;
        /** No quadro do acerto: folga entre os pixels (0 = encostam) e pixels sobrepostos. */
        float folga = -1;
        int sobreAcerto;
        /** Sem acerto: a maior sobreposição do golpe acima da do começo. */
        int sobreMax, sobreBase;
        String tipo = "ok";
    }

    // ------------------------------------------------------------------ desenho

    static float meio;
    /** O corpo de quem apanha antes do passo (o que o motor conferiu). */
    static float[] corpoAntes;

    static void desenhar(Bitmap b, Canvas c, SpriteFighterRenderer r, CombatFighter f) throws Exception {
        b.eraseColor(0);
        c.save();
        c.translate(W / 2f - meio, CHAO_PX - Arena.GROUND_Y);
        drawF.invoke(jogo, c, r, f, false, false);
        c.restore();
    }

    static void desenharProjeteis(CombatEngine e) throws Exception {
        CAMADA_P.eraseColor(0);
        Canvas c = new Canvas(CAMADA_P);
        c.save();
        c.translate(W / 2f - meio, CHAO_PX - Arena.GROUND_Y);
        RC.begin(c);
        drawProj.invoke(efeitos, c, RC, e.energyProjectiles);
        c.restore();
    }

    /** Por quadro: pixels sobrepostos só dentro da caixa do membro do golpe (a do motor, com a folga). */
    static int[] membro;

    static int sobreNoMembro(CombatFighter a) {
        float[] m = CombatEngine.golpeNoDesenho(a, a.attackFrame);
        if (m == null) return sobreposicao(PA, PD);
        // As duas caixas do membro na diagonal contam pelo retângulo que cobre as duas.
        float x0 = Math.min(m[0], a.body().halfWidth) - CombatEngine.FOLGA_DESENHO, x1 = CombatEngine.maxDe(m, 1) + CombatEngine.FOLGA_DESENHO;
        if (a.attack.reachCap > 0f) x1 = Math.min(x1, a.attack.reachCap + CombatEngine.FOLGA_DESENHO);
        float l = a.facing > 0 ? a.x + x0 : a.x - x1, r = a.facing > 0 ? a.x + x1 : a.x - x0;
        float ox = W / 2f - meio, oy = CHAO_PX - Arena.GROUND_Y;
        int px0 = Math.max(0, Math.round(l + ox)), px1 = Math.min(W - 1, Math.round(r + ox));
        int py0 = Math.max(0, Math.round(a.y - CombatEngine.maxDe(m, 3) - CombatEngine.FOLGA_DESENHO + oy));
        int py1 = Math.min(H - 1, Math.round(a.y - CombatEngine.minDe(m, 2) + CombatEngine.FOLGA_DESENHO + oy));
        int n = 0;
        for (int y = py0; y <= py1; y++) for (int x = px0; x <= px1; x++) {
            int i = y * W + x;
            if ((PA[i] >>> 24) >= ALFA && (PD[i] >>> 24) >= ALFA) n++;
        }
        return n;
    }

    static int sobreposicao(int[] a, int[] d) {
        int n = 0;
        for (int i = 0; i < a.length; i++) if ((a[i] >>> 24) >= ALFA && (d[i] >>> 24) >= ALFA) n++;
        return n;
    }

    /** Menor distância (px) entre um pixel opaco de {@code a} e um de {@code d}; 0 se encostam. */
    static float folga(int[] a, int[] d) {
        // Caixa onde os dois estão, com margem.
        int x0 = W, x1 = -1, y0 = H, y1 = -1;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int i = y * W + x;
            if ((a[i] >>> 24) >= ALFA || (d[i] >>> 24) >= ALFA) {
                if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
            }
        }
        if (x1 < 0) return -1;
        int w = x1 - x0 + 1, h = y1 - y0 + 1;
        // Transformada de distância (chanfro 3-4) a partir do corpo de quem apanha.
        int[] dt = new int[w * h];
        final int INF = 1 << 28;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            dt[y * w + x] = (d[(y + y0) * W + x + x0] >>> 24) >= ALFA ? 0 : INF;
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x, v = dt[i];
            if (x > 0) v = Math.min(v, dt[i - 1] + 3);
            if (y > 0) {
                v = Math.min(v, dt[i - w] + 3);
                if (x > 0) v = Math.min(v, dt[i - w - 1] + 4);
                if (x < w - 1) v = Math.min(v, dt[i - w + 1] + 4);
            }
            dt[i] = v;
        }
        for (int y = h - 1; y >= 0; y--) for (int x = w - 1; x >= 0; x--) {
            int i = y * w + x, v = dt[i];
            if (x < w - 1) v = Math.min(v, dt[i + 1] + 3);
            if (y < h - 1) {
                v = Math.min(v, dt[i + w] + 3);
                if (x < w - 1) v = Math.min(v, dt[i + w + 1] + 4);
                if (x > 0) v = Math.min(v, dt[i + w - 1] + 4);
            }
            dt[i] = v;
        }
        int min = INF;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if ((a[(y + y0) * W + x + x0] >>> 24) >= ALFA) min = Math.min(min, dt[y * w + x]);
        }
        return min >= INF ? -1 : min / 3f;
    }

    // ------------------------------------------------------------------ uma tentativa

    static void preparar(String quem) throws Exception {
        Method sel = GameView.class.getDeclaredMethod("selectTeamsNow", String.class, String.class, String.class, String.class, boolean.class);
        sel.setAccessible(true);
        sel.invoke(jogo, quem, quem, RIVAL, RIVAL, true);
        Field intro = GameView.class.getDeclaredField("introFrame");
        intro.setAccessible(true);
        intro.setInt(jogo, -1);
    }

    /** Aperta o golpe {@code g} no quadro 0 (no chão) ou {@code aperto} quadros depois do pulo. */
    static void entrada(FighterInput in, String g, int quadro, int aperto) {
        in.clear();
        boolean pulo = g.startsWith("j") || g.equals("cruzado");
        if (pulo) {
            if (quadro == 0) { in.jump = true; in.direction = 8; return; }
            if (quadro != aperto) return;
            char b = g.equals("cruzado") ? 'M' : g.charAt(1);
            if (b == 'L') in.light = true; else if (b == 'M') in.medium = true; else in.heavy = true;
            return;
        }
        if (g.startsWith("2")) in.direction = 2;
        if (quadro != 0) return;
        switch (g) {
            case "S": case "Sproj": in.special = "M"; break;
            default: {
                char b = g.charAt(g.length() - 1);
                if (b == 'L') in.light = true; else if (b == 'M') in.medium = true; else in.heavy = true;
            }
        }
    }

    static final int QUADROS = 110;
    /** Por quadro da última rodada com golpe: o rival estava na frente (dx >= MIN_MELEE_DISTANCE)? */
    static final boolean[] ladoCerto = new boolean[QUADROS];

    /**
     * Uma tentativa e, sem acerto, a mesma ação sem o botão (parado, agachado ou o pulo puro): a
     * falha visual é o que o golpe acrescenta de sobreposição, não o corpo que já encostava.
     */
    static Caso tentar(String quem, String g, boolean agachado, int dist, int aperto, File png) throws Exception {
        int[] comGolpe = new int[QUADROS], semGolpe = new int[QUADROS];
        java.util.Arrays.fill(comGolpe, -1);
        java.util.Arrays.fill(semGolpe, -1);
        Caso c = rodada(quem, g, agachado, dist, aperto, false, comGolpe, null, -1, png);
        // O golpe da frente só vale com o rival na frente (o motor exige MIN_MELEE_DISTANCE); o
        // cruzado só vale em cima ou atrás. Fora disso a sobreposição é o corpo passando por cima.
        boolean cruzado = g.equals("cruzado");
        for (int q = 0; q < QUADROS; q++) {
            if (comGolpe[q] >= 0 && ladoCerto[q] != !cruzado && !g.startsWith("S")) comGolpe[q] = -1;
        }
        if (!c.acertou) {
            rodada(quem, g, agachado, dist, aperto, true, semGolpe, comGolpe, -1, null);
            int pior = -1;
            for (int q = 0; q < QUADROS; q++) {
                if (comGolpe[q] < 0) continue;
                int extra = comGolpe[q] - Math.max(0, semGolpe[q]);
                if (extra > c.sobreMax) { c.sobreMax = extra; pior = q; }
            }
            c.sobreBase = pior >= 0 ? Math.max(0, semGolpe[pior]) : 0;
            c.quadroAcerto = pior;
            if (png != null && pior < 0) {
                // Sem sobra: a foto do meio dos quadros ativos (o golpe passando longe do corpo).
                int q0 = -1, q1 = -1;
                for (int q = 0; q < QUADROS; q++) if (comGolpe[q] >= 0) { if (q0 < 0) q0 = q; q1 = q; }
                pior = q0 < 0 ? -1 : (q0 + q1) / 2;
            }
            if (png != null && pior >= 0) rodada(quem, g, agachado, dist, aperto, false, new int[QUADROS], null, pior, png);
        }
        if (c.acertou && c.folga > 12f) c.tipo = "fantasma";
        else if (!c.acertou && c.sobreMax >= 60) c.tipo = "falha";
        return c;
    }

    /**
     * {@code sobre[q]}: pixels sobrepostos no quadro q (só nos quadros do golpe, ou nos marcados em
     * {@code quais}). {@code semBotao}: a mesma ação sem o golpe. {@code fotoEm}: o quadro do PNG da falha.
     */
    static Caso rodada(String quem, String g, boolean agachado, int dist, int aperto, boolean semBotao, int[] sobre, int[] quais,
                       int fotoEm, File png) throws Exception {
        CombatEngine e = jogo.engine;
        float xa = 1000f, xd = xa + dist;
        e.restage(xa, xd);
        CombatFighter a = e.fighter(0), d = e.fighter(1);
        a.state.life = d.state.life = 1_000_000;
        a.state.superMeter = 0;
        SpriteFighterRenderer ra = (SpriteFighterRenderer)rendP.get(jogo), rd = (SpriteFighterRenderer)rendO.get(jogo);
        Canvas ca = new Canvas(CAMADA_A), cd = new Canvas(CAMADA_D);
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        Caso c = new Caso();
        c.quem = quem; c.golpe = g; c.postura = agachado ? "agachado" : "em pé"; c.distancia = dist; c.aperto = aperto;
        boolean projetil = g.equals("Sproj") && !semBotao;
        boolean viuGolpe = false;
        // O rival assenta na postura antes.
        for (int i = 0; i < 12; i++) {
            in0.clear(); in1.clear();
            if (agachado) in1.direction = 3;
            e.step(in0, in1);
            upd.invoke(jogo, ra, a); upd.invoke(jogo, rd, d);
        }
        for (int q = 0; q < QUADROS; q++) {
            entrada(in0, g, q, aperto);
            if (semBotao) { in0.light = in0.medium = in0.heavy = false; in0.special = null; }
            in1.clear();
            if (agachado) in1.direction = 3;
            meio = (a.x + d.x) / 2f;
            boolean querQuadro = quais != null ? quais[q] >= 0 : true;
            // Quem apanha: como estava antes do passo (o corpo que o motor confere).
            corpoAntes = CombatEngine.corpoAtual(d);
            if (querQuadro) {
                desenhar(CAMADA_D, cd, rd, d);
                CAMADA_D.getPixels(PD, 0, W, 0, 0, W, H);
                if (projetil) { desenharProjeteis(e); CAMADA_P.getPixels(PP, 0, W, 0, 0, W, H); }
            }
            e.step(in0, in1);
            upd.invoke(jogo, ra, a);
            upd.invoke(jogo, rd, d);
            if (!semBotao && quais == null) ladoCerto[q] = (d.x - a.x) * a.facing >= CombatRules.MIN_MELEE_DISTANCE;
            if (quais != null) {
                if (querQuadro) {
                    desenhar(CAMADA_A, ca, ra, a);
                    CAMADA_A.getPixels(PA, 0, W, 0, 0, W, H);
                    sobre[q] = sobreposicao(PA, PD);
                }
                continue;
            }
            boolean acerto = false;
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0 && !h.blocked) acerto = true;
            boolean ativo = a.attacking() && a.attack != null && a.attack.isActive(a.attackFrame);
            if (!ativo && !acerto && !(projetil && !e.energyProjectiles.isEmpty())) {
                if (viuGolpe && !a.attacking() && e.energyProjectiles.isEmpty()) break;
                continue;
            }
            viuGolpe = true;
            // Quem bate: depois do passo (o quadro do golpe que o motor conferiu).
            desenhar(CAMADA_A, ca, ra, a);
            CAMADA_A.getPixels(PA, 0, W, 0, 0, W, H);
            if (projetil) for (int i = 0; i < PA.length; i++) if ((PP[i] >>> 24) > (PA[i] >>> 24)) PA[i] = PP[i];
            int s = sobreposicao(PA, PD);
            sobre[q] = s;
            if (membro != null) membro[q] = sobreNoMembro(a);
            if (acerto) {
                c.acertou = true;
                c.quadroAcerto = q;
                c.sobreAcerto = s;
                c.folga = s > 0 ? 0f : folga(PA, PD);
                if (png != null) salvar(png, c, a, d);
                break;
            }
            if (png != null && q == fotoEm) {
                c.sobreMax = s;
                salvar(png, c, a, d);
            }
        }
        return c;
    }

    /** O quadro com os dois desenhos e as caixas do motor por cima. */
    static void salvar(File png, Caso c, CombatFighter a, CombatFighter d) throws IOException {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(40, 44, 52));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(90, 90, 90));
        g.drawLine(0, (int)CHAO_PX, W, (int)CHAO_PX);
        g.drawImage(CAMADA_D.image(), 0, 0, null);
        g.drawImage(CAMADA_A.image(), 0, 0, null);
        float ox = W / 2f - meio, oy = CHAO_PX - Arena.GROUND_Y;
        g.setStroke(new BasicStroke(2f));
        // O membro medido do golpe (o que o motor usa), em vermelho.
        float[] m = CombatEngine.golpeNoDesenho(a, a.attackFrame);
        for (int j = 0; m != null && j + 3 < m.length; j += 4) {
            float l = a.facing > 0 ? a.x + m[j] : a.x - m[j + 1], r = a.facing > 0 ? a.x + m[j + 1] : a.x - m[j];
            g.setColor(Color.RED);
            g.drawRect(Math.round(l + ox), Math.round(a.y - m[j + 3] + oy), Math.round(r - l), Math.round(m[j + 3] - m[j + 2]));
        }
        // O corpo de quem apanha (as faixas da postura), em verde.
        float[] corpo = corpoAntes;
        g.setColor(Color.GREEN);
        if (corpo == null) {
            // Sem caixas medidas da postura: a caixa genérica do corpo (Body), tracejada.
            CharacterDefinition.Body b = d.body();
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f));
            float h = b.height(d.crouchingBody());
            g.drawRect(Math.round(d.x - b.halfWidth + ox), Math.round(d.y - h + oy), Math.round(2 * b.halfWidth), Math.round(h));
            g.setStroke(new BasicStroke(2f));
        }
        if (corpo != null) {
            for (int i = 0; i + 3 < corpo.length; i += 4) {
                float l = d.facing > 0 ? d.x + corpo[i] : d.x - corpo[i + 1];
                float r = d.facing > 0 ? d.x + corpo[i + 1] : d.x - corpo[i];
                g.drawRect(Math.round(l + ox), Math.round(d.y - corpo[i + 3] + oy), Math.round(r - l), Math.round(corpo[i + 3] - corpo[i + 2]));
            }
        }
        g.setColor(Color.WHITE);
        g.drawString(String.format(Locale.ROOT, "%s %s contra %s %s a %d px%s  motor: %s  pixels: %s",
            c.quem, c.golpe, RIVAL, c.postura, c.distancia, c.aperto >= 0 ? " (aperto " + c.aperto + ")" : "",
            c.acertou ? "ACERTOU" : "não acertou",
            c.acertou ? (c.sobreAcerto > 0 ? "encostam (" + c.sobreAcerto + ")" : String.format(Locale.ROOT, "folga %.0f px", c.folga))
                : "sobrepõem " + c.sobreMax), 12, 20);
        g.dispose();
        ImageIO.write(img, "png", png);
    }

    // ------------------------------------------------------------------ varredura

    static List<Caso> varrer(String quem) throws Exception {
        List<Caso> casos = new ArrayList<>();
        CharacterDefinition ch = GeneratedCharacters.get(quem);
        String[] chao = Boolean.getBoolean("t25.soPulos") ? new String[0] : new String[] {"L", "M", "H", "2L", "2M", "2H"};
        for (String g : chao) {
            AttackDefinition at = ch.attack(g);
            if (at == null) continue;
            int max = Math.round(at.reach) + 160;
            for (boolean ag : Boolean.getBoolean("t25.soAgachado") ? new boolean[] {true} : new boolean[] {false, true}) {
                for (int dist = 70; dist <= max; dist += 10) casos.add(tentar(quem, g, ag, dist, -1, null));
            }
        }
        // S: o de corpo (golpe do personagem) ou o projétil (energia).
        boolean temProjetil = ch.fighter.energy != null;
        for (boolean ag : Boolean.getBoolean("t25.soPulos") ? new boolean[0] : new boolean[] {false, true}) {
            if (temProjetil) {
                for (int dist : new int[] {200, 320, 450}) casos.add(tentar(quem, "Sproj", ag, dist, -1, null));
            } else {
                for (int dist = 70; dist <= 360; dist += 15) casos.add(tentar(quem, "S", ag, dist, -1, null));
            }
        }
        for (String g : Boolean.getBoolean("t25.soChao") ? new String[0] : new String[] {"jL", "jM", "jH"}) {
            if (ch.attack(g) == null) continue;
            for (boolean ag : new boolean[] {false, true}) {
                for (int dist = 160; dist <= 400; dist += 20) {
                    for (int aperto : new int[] {24, 28, 32, 36}) casos.add(tentar(quem, g, ag, dist, aperto, null));
                }
            }
        }
        // Cruzado: pula por cima e solta o jM na descida, já do outro lado.
        for (boolean ag : Boolean.getBoolean("t25.soChao") ? new boolean[0] : new boolean[] {false, true}) {
            for (int dist = 160; dist <= 280; dist += 10) {
                for (int aperto : new int[] {28, 31, 34, 37}) casos.add(tentar(quem, "cruzado", ag, dist, aperto, null));
            }
        }
        return casos;
    }

    static void iniciar() throws Exception {
        Context ctx = DadosDoJogo.localizar().contexto(null);
        jogo = new GameView(ctx);
        jogo.setTouchControlsVisible(false);
        upd = GameView.class.getDeclaredMethod("updateFighterSprite", SpriteFighterRenderer.class, CombatFighter.class);
        upd.setAccessible(true);
        drawF = GameView.class.getDeclaredMethod("drawFighter", Canvas.class, SpriteFighterRenderer.class, CombatFighter.class,
            boolean.class, boolean.class);
        drawF.setAccessible(true);
        rendP = GameView.class.getDeclaredField("spriteFighterRenderer");
        rendP.setAccessible(true);
        rendO = GameView.class.getDeclaredField("opponentSpriteRenderer");
        rendO.setAccessible(true);
        Field ef = GameView.class.getDeclaredField("effects");
        ef.setAccessible(true);
        efeitos = ef.get(jogo);
        drawProj = EffectsRenderer.class.getDeclaredMethod("drawEnergyProjectiles", Canvas.class, RenderCanvas.class, List.class);
        drawProj.setAccessible(true);
    }

    public static void main(String[] args) throws Exception {
        File saida = new File(args[0]);
        saida.mkdirs();
        iniciar();
        String[] ids = args.length > 1 ? args[1].split(",") : ELENCO;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(new File(saida, "casos.tsv").toPath(), StandardCharsets.UTF_8))) {
            w.println("quem\tgolpe\tpostura\tdistancia\taperto\tacertou\tquadro\tfolga\tsobre_acerto\tsobre_max\tsobre_base\ttipo");
            for (String quem : ids) {
                long t0 = System.nanoTime();
                preparar(quem);
                List<Caso> casos = varrer(quem);
                int fant = 0, falha = 0;
                for (Caso c : casos) {
                    w.printf(Locale.ROOT, "%s\t%s\t%s\t%d\t%d\t%b\t%d\t%.1f\t%d\t%d\t%d\t%s%n", c.quem, c.golpe, c.postura, c.distancia, c.aperto,
                        c.acertou, c.quadroAcerto, c.folga, c.sobreAcerto, c.sobreMax, c.sobreBase, c.tipo);
                    if (c.tipo.equals("fantasma")) fant++;
                    if (c.tipo.equals("falha")) falha++;
                }
                w.flush();
                System.out.printf(Locale.ROOT, "%s: %d tentativas, %d fantasmas, %d falhas (%.0f s)%n", quem, casos.size(), fant, falha,
                    (System.nanoTime() - t0) / 1e9);
            }
        }
        // Imagens: java ... BateriaColisoes saida/ png quem golpe postura distancia aperto
        System.exit(0);
    }

    /** Refaz um caso e salva o PNG (para os piores casos da tabela). */
    public static final class Foto {
        public static void main(String[] args) throws Exception {
            iniciar();
            File dir = new File(args[0]);
            dir.mkdirs();
            // Cada caso: quem:golpe:postura:distancia:aperto
            for (int i = 1; i < args.length; i++) {
                String[] p = args[i].split(":");
                preparar(p[0]);
                File png = new File(dir, String.format("%s_%s_%s_%s%s.png", p[0], p[1], p[2].equals("agachado") ? "agachado" : "empe", p[3],
                    p[4].equals("-1") ? "" : "_a" + p[4]));
                Caso c = tentar(p[0], p[1], p[2].equals("agachado"), Integer.parseInt(p[3]), Integer.parseInt(p[4]), png);
                System.out.println(png.getName() + ": " + c.tipo + " folga " + c.folga + " sobre " + c.sobreMax);
            }
            System.exit(0);
        }
    }

    /**
     * Refaz as falhas de um casos.tsv contando só a sobreposição dentro da caixa do membro (o braço
     * ou a perna que o motor usa), não o corpo de quem bate que avança: java ... Refaz casos.tsv saida.tsv
     */
    public static final class Refaz {
        public static void main(String[] args) throws Exception {
            iniciar();
            List<String> linhas = Files.readAllLines(new File(args[0]).toPath(), StandardCharsets.UTF_8);
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(new File(args[1]).toPath(), StandardCharsets.UTF_8))) {
                w.println("quem\tgolpe\tpostura\tdistancia\taperto\tsobre_max\tmembro_max\tquadro_membro");
                String atual = null;
                for (String linha : linhas.subList(1, linhas.size())) {
                    String[] p = linha.split("\t");
                    if (!p[11].equals("falha")) continue;
                    if (!p[0].equals(atual)) { preparar(p[0]); atual = p[0]; }
                    int[] sobre = new int[QUADROS];
                    membro = new int[QUADROS];
                    java.util.Arrays.fill(membro, -1);
                    rodada(p[0], p[1], p[2].equals("agachado"), Integer.parseInt(p[3]), Integer.parseInt(p[4]), false, sobre, null, -1, null);
                    boolean cruzado = p[1].equals("cruzado");
                    int max = 0, qm = -1;
                    for (int q = 0; q < QUADROS; q++) {
                        if (membro[q] < 0 || (!p[1].startsWith("S") && ladoCerto[q] == cruzado)) continue;
                        if (membro[q] > max) { max = membro[q]; qm = q; }
                    }
                    membro = null;
                    w.printf(Locale.ROOT, "%s\t%s\t%s\t%s\t%s\t%s\t%d\t%d%n", p[0], p[1], p[2], p[3], p[4], p[9], max, qm);
                    w.flush();
                }
            }
            System.exit(0);
        }
    }

    /** Quadro a quadro de um caso (quem:golpe:postura:distancia:aperto): onde estão, o golpe e o corpo. */
    public static final class Sonda {
        public static void main(String[] args) throws Exception {
            iniciar();
            for (String arg : args) {
                String[] p = arg.split(":");
                preparar(p[0]);
                boolean agachado = p[2].equals("agachado");
                int aperto = Integer.parseInt(p[4]);
                CombatEngine e = jogo.engine;
                e.restage(1000f, 1000f + Integer.parseInt(p[3]));
                CombatFighter a = e.fighter(0), d = e.fighter(1);
                a.state.life = d.state.life = 1_000_000;
                SpriteFighterRenderer ra = (SpriteFighterRenderer)rendP.get(jogo), rd = (SpriteFighterRenderer)rendO.get(jogo);
                Canvas ca = new Canvas(CAMADA_A), cd = new Canvas(CAMADA_D);
                FighterInput in0 = new FighterInput(), in1 = new FighterInput();
                for (int i = 0; i < 12; i++) {
                    in0.clear(); in1.clear();
                    if (agachado) in1.direction = 3;
                    e.step(in0, in1);
                }
                System.out.println("== " + arg);
                for (int q = 0; q < 60; q++) {
                    entrada(in0, p[1], q, aperto);
                    in1.clear();
                    if (agachado) in1.direction = 3;
                    upd.invoke(jogo, rd, d);
                    meio = (a.x + d.x) / 2f;
                    desenhar(CAMADA_D, cd, rd, d);
                    CAMADA_D.getPixels(PD, 0, W, 0, 0, W, H);
                    float[] corpo = CombatEngine.corpoAtual(d);
                    e.step(in0, in1);
                    upd.invoke(jogo, ra, a);
                    desenhar(CAMADA_A, ca, ra, a);
                    CAMADA_A.getPixels(PA, 0, W, 0, 0, W, H);
                    boolean hit = false;
                    for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0) hit = true;
                    float[] g = a.attacking() ? CombatEngine.golpeNoDesenho(a, a.attackFrame) : null;
                    System.out.printf(Locale.ROOT, "q%02d dx=%.0f alt=%.0f vy=%.1f %s %s f%d ativo=%b hit=%b membro=%s corpo=%s sobre=%d%n",
                        q, (d.x - a.x) * a.facing, Arena.GROUND_Y - a.y, a.vy, a.status, a.attack != null ? a.attack.id : "-", a.attackFrame,
                        a.attacking() && a.attack.isActive(a.attackFrame), hit,
                        g == null ? "-" : java.util.Arrays.toString(g), corpo == null ? "-" : java.util.Arrays.toString(corpo), sobreposicao(PA, PD));
                }
            }
            System.exit(0);
        }
    }
}
