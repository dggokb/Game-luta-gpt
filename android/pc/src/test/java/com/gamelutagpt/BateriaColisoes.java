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
    static final String RIVAL = "player_base";
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

    static Caso tentar(String quem, String g, boolean agachado, int dist, int aperto, File png) throws Exception {
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
        boolean projetil = g.equals("Sproj");
        boolean viuGolpe = false;
        // O rival assenta na postura antes.
        for (int i = 0; i < 12; i++) {
            in0.clear(); in1.clear();
            if (agachado) in1.direction = 3;
            e.step(in0, in1);
            upd.invoke(jogo, ra, a); upd.invoke(jogo, rd, d);
        }
        for (int q = 0; q < 110; q++) {
            entrada(in0, g, q, aperto);
            in1.clear();
            if (agachado) in1.direction = 3;
            meio = (a.x + d.x) / 2f;
            // Quem apanha: como estava antes do passo (o corpo que o motor confere).
            desenhar(CAMADA_D, cd, rd, d);
            CAMADA_D.getPixels(PD, 0, W, 0, 0, W, H);
            if (projetil) { desenharProjeteis(e); CAMADA_P.getPixels(PP, 0, W, 0, 0, W, H); }
            e.step(in0, in1);
            upd.invoke(jogo, ra, a);
            upd.invoke(jogo, rd, d);
            boolean acerto = false;
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0 && !h.blocked) acerto = true;
            boolean ativo = a.attacking() && a.attack != null && a.attack.isActive(a.attackFrame);
            if (!ativo && !acerto && !(projetil && !e.energyProjectiles.isEmpty())) {
                if (viuGolpe && !a.attacking() && e.energyProjectiles.isEmpty()) break;
                if (q == 0 || c.sobreBase == 0 && !viuGolpe) {
                    // A sobreposição do começo (corpo com corpo, sem golpe).
                    desenhar(CAMADA_A, ca, ra, a);
                    CAMADA_A.getPixels(PA, 0, W, 0, 0, W, H);
                    c.sobreBase = Math.max(c.sobreBase, sobreposicao(PA, PD));
                }
                continue;
            }
            viuGolpe = true;
            // Quem bate: depois do passo (o quadro do golpe que o motor conferiu).
            desenhar(CAMADA_A, ca, ra, a);
            CAMADA_A.getPixels(PA, 0, W, 0, 0, W, H);
            if (projetil) for (int i = 0; i < PA.length; i++) if ((PP[i] >>> 24) > (PA[i] >>> 24)) PA[i] = PP[i];
            int sobre = sobreposicao(PA, PD);
            if (acerto) {
                c.acertou = true;
                c.quadroAcerto = q;
                c.sobreAcerto = sobre;
                c.folga = sobre > 0 ? 0f : folga(PA, PD);
                if (png != null) salvar(png, c, a, d);
                break;
            }
            c.sobreMax = Math.max(c.sobreMax, sobre - c.sobreBase);
            if (png != null && c.sobreMax == sobre - c.sobreBase && c.sobreMax > 0) salvar(png, c, a, d);
        }
        if (c.acertou && c.folga > 12f) c.tipo = "fantasma";
        else if (!c.acertou && c.sobreMax >= 60) c.tipo = "falha";
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
        if (m != null) {
            float l = a.facing > 0 ? a.x + m[0] : a.x - m[1], r = a.facing > 0 ? a.x + m[1] : a.x - m[0];
            g.setColor(Color.RED);
            g.drawRect(Math.round(l + ox), Math.round(a.y - m[3] + oy), Math.round(r - l), Math.round(m[3] - m[2]));
        }
        // O corpo de quem apanha (as faixas da postura), em verde.
        float[] corpo = CombatEngine.corpoAtual(d);
        g.setColor(Color.GREEN);
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
        String[] chao = {"L", "M", "H", "2L", "2M", "2H"};
        for (String g : chao) {
            AttackDefinition at = ch.attack(g);
            if (at == null) continue;
            int max = Math.round(at.reach) + 160;
            for (boolean ag : new boolean[] {false, true}) {
                for (int dist = 70; dist <= max; dist += 10) casos.add(tentar(quem, g, ag, dist, -1, null));
            }
        }
        // S: o de corpo (golpe do personagem) ou o projétil (energia).
        boolean temProjetil = ch.fighter.energy != null;
        for (boolean ag : new boolean[] {false, true}) {
            if (temProjetil) {
                for (int dist : new int[] {200, 320, 450}) casos.add(tentar(quem, "Sproj", ag, dist, -1, null));
            } else {
                for (int dist = 70; dist <= 360; dist += 15) casos.add(tentar(quem, "S", ag, dist, -1, null));
            }
        }
        for (String g : new String[] {"jL", "jM", "jH"}) {
            if (ch.attack(g) == null) continue;
            for (boolean ag : new boolean[] {false, true}) {
                for (int dist = 140; dist <= 380; dist += 20) {
                    for (int aperto : new int[] {14, 20, 26}) casos.add(tentar(quem, g, ag, dist, aperto, null));
                }
            }
        }
        // Cruzado: pula por cima e solta o jM na descida, já do outro lado.
        for (boolean ag : new boolean[] {false, true}) {
            for (int dist = 40; dist <= 160; dist += 10) {
                for (int aperto : new int[] {20, 26, 30}) casos.add(tentar(quem, "cruzado", ag, dist, aperto, null));
            }
        }
        return casos;
    }

    public static void main(String[] args) throws Exception {
        File saida = new File(args[0]);
        saida.mkdirs();
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
}
