package com.gamelutagpt;

import com.gamelutagpt.historia.CinematicaLuta;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Os lutadores na cinemática da luta ({@link CinematicaLuta}). Enquanto a cena tem o controle,
 * esta classe escreve o comando dos dois a cada quadro, como as demos ({@link DemoDirector}):
 * o pad e a IA ficam de fora. Os movimentos saem do motor de luta de verdade (pulo com a
 * direção para trás, backdash, dash segurando para frente), só que apertados pelo roteiro, então
 * a animação, a física e as paredes são as da luta.
 */
final class CinematicaDirector {
    static final float FPS = 60f;
    /**
     * Golpe do QTE: chega até esse vão entre os corpos antes de bater (px do mundo), ou até
     * encostar (os corpos se empurram e o vão para de diminuir).
     */
    static final float ALCANCE = 30f;
    /** Golpe do QTE: o tempo máximo para chegar perto e bater. */
    static final float GOLPE_SEGUNDOS = 5f;

    /** Um movimento em andamento. */
    private static final class Ordem {
        final CinematicaLuta.Movimento movimento;
        /** Um golpe (o QTE) no lugar do movimento. */
        final CinematicaLuta.Golpe golpe;
        final int limite;
        int frame;
        /** O comando já saiu (o lutador estava livre para obedecer). */
        boolean pediu;
        int pediuEm;
        boolean saiuDoChao;
        /** Golpe: o vão no quadro anterior (encostou quando ele para de diminuir). */
        float vaoAntes = Float.MAX_VALUE;

        Ordem(CinematicaLuta.Movimento movimento) {
            this.movimento = movimento;
            golpe = null;
            limite = Math.max(1, Math.round(movimento.segundos * FPS));
        }

        Ordem(CinematicaLuta.Golpe golpe) {
            movimento = null;
            this.golpe = golpe;
            limite = Math.round(GOLPE_SEGUNDOS * FPS);
        }
    }

    private final Ordem[] ordens = new Ordem[2];

    /** Os lances da troca de golpes. */
    enum Lance {
        /** Chega perto e solta de 1 a 3 golpes; o outro defende. */
        BLOQUEIO,
        /** Para pouco antes e o golpe passa raspando no vento. */
        VENTO,
        /** Solta o golpe forte e o outro pula para trás bem na hora: passa no vento. */
        ESQUIVA,
        /** Pula por cima e desce com o golpe; o outro defende e ainda leva o combo defendido. */
        SALTO,
        /** Os dois correm e soltam o golpe médio juntos: punho contra punho. */
        CHOQUE;

        static Lance de(String nome) {
            return valueOf(nome.toUpperCase(java.util.Locale.ROOT));
        }
    }

    /**
     * A troca de golpes sem acertar (entre os QTE): em cada vez sai um lance (os do roteiro em
     * ordem, depois sorteados): defesa, golpe no vento, esquiva, salto ou choque. Depois um dos
     * dois recua (ou nenhum) e a vez quase sempre passa para o outro. O jogo segura a vida e a
     * guarda enquanto isso ({@link #trocando}).
     */
    private static final class Troca {
        final int limite;
        final Random sorteio;
        final Deque<Lance> roteiro = new ArrayDeque<>();
        int frame;
        int atacante;
        Lance lance = Lance.BLOQUEIO;
        /** 0 escolhe o lance, 1 chega perto, 2 golpes, 3 separa, 4 acabando, 5 no ar (salto). */
        int fase;
        int faseFrame;
        int golpes;
        float alvoVao;
        int proximoGolpe;
        int recua;
        /** O vão no quadro anterior (encostou quando ele para de diminuir). */
        float vaoAntes;
        /** O mesmo atacou duas vezes seguidas: a próxima é do outro. */
        boolean repetiu;
        /** Salto: já saiu do chão; já soltou o golpe no ar. */
        boolean noAr;
        boolean bateuNoAr;
        /** Choque: quadros até cada um apertar (o mais lento aperta antes). */
        int[] apertaEm = {-1, -1};

        Troca(float segundos, long semente, List<String> lances) {
            limite = Math.max(1, Math.round(segundos * FPS));
            sorteio = new Random(semente);
            atacante = sorteio.nextInt(2);
            if (lances != null) for (String l : lances) roteiro.add(Lance.de(l));
        }

        Lance sortear() {
            if (!roteiro.isEmpty()) return roteiro.poll();
            int r = sorteio.nextInt(100);
            return r < 30 ? Lance.BLOQUEIO : r < 45 ? Lance.VENTO : r < 60 ? Lance.ESQUIVA : r < 77 ? Lance.SALTO : Lance.CHOQUE;
        }
    }

    private static final String[] BOTOES = {"L", "M", "H"};
    private Troca troca;
    /** Choques de golpes (punho contra punho) desde o começo: o jogo põe o impacto em cada um. */
    private int choques;

    /** Começa um movimento do lado 0 (jogador) ou 1 (CPU); "parar" cancela o que estiver rodando. */
    void mover(int lado, CinematicaLuta.Movimento movimento) {
        troca = null;
        ordens[lado] = movimento.tipo == CinematicaLuta.Movimento.Tipo.PARAR ? null : new Ordem(movimento);
    }

    /** O lado 0 (jogador) ou 1 (CPU) vai até o outro e dá o golpe do QTE. */
    void golpear(int lado, CinematicaLuta.Golpe golpe) {
        troca = null;
        ordens[lado] = new Ordem(golpe);
    }

    /**
     * Os dois trocam golpes sem acertar por {@code segundos} (no tempo da luta). {@code lances}:
     * os primeiros lances em ordem ("bloqueio", "vento", "esquiva", "salto", "choque"); o resto
     * é sorteado. Pode ser null.
     */
    void trocar(float segundos, long semente, List<String> lances) {
        ordens[0] = ordens[1] = null;
        troca = new Troca(segundos, semente, lances);
    }

    void trocar(float segundos, long semente) {
        trocar(segundos, semente, null);
    }

    /** Quantos choques de golpes já saíram (o jogo compara com o último que viu). */
    int choques() {
        return choques;
    }

    /** De que lado é quem ataca na troca agora (0 ou 1), ou -1 (ninguém, ou os dois no choque). */
    int atacanteDaTroca() {
        Troca t = troca;
        if (t == null || t.lance == Lance.CHOQUE) return -1;
        return t.fase == 1 || t.fase == 2 || t.fase == 5 ? t.atacante : -1;
    }

    /** A troca de golpes ainda não acabou (a vez em andamento termina antes). */
    boolean trocando() {
        return troca != null;
    }

    boolean movendo(int lado) {
        return ordens[lado] != null;
    }

    /** Cancela tudo (fim da cena, luta nova). */
    void parar() {
        ordens[0] = ordens[1] = null;
        troca = null;
    }

    /** O comando dos dois neste quadro (chame antes do {@code engine.step}). */
    void fill(CombatEngine engine, FighterInput p1, FighterInput cpu) {
        p1.clear();
        cpu.clear();
        if (troca != null) {
            trocar(engine, new FighterInput[] {p1, cpu});
            return;
        }
        drive(engine, 0, p1);
        drive(engine, 1, cpu);
    }

    private void trocar(CombatEngine engine, FighterInput[] in) {
        Troca t = troca;
        t.frame++;
        t.faseFrame++;
        int a = t.atacante, d = 1 - a;
        CombatFighter atk = engine.fighter(a), def = engine.fighter(d);
        if (atk.ko() || def.ko()) {
            troca = null;
            return;
        }
        boolean livres = atk.canAct() && def.canAct() && atk.grounded && def.grounded;
        float vao = vao(atk, def);
        switch (t.fase) {
            case 0:
                if (t.frame >= t.limite) {
                    t.fase = 4;
                    t.faseFrame = 0;
                    return;
                }
                if (!livres) return;
                // Quase sempre a vez passa para o outro (nunca três seguidas do mesmo).
                if (t.frame > 1) {
                    boolean passa = t.repetiu || t.sorteio.nextInt(10) < 7;
                    if (passa) t.atacante = 1 - t.atacante;
                    t.repetiu = !passa;
                }
                t.lance = t.sortear();
                // O salto precisa de espaço para pular: colados, vira defesa.
                if (t.lance == Lance.SALTO && vao(engine.fighter(t.atacante), engine.fighter(1 - t.atacante)) < 90f) {
                    t.lance = Lance.BLOQUEIO;
                }
                t.alvoVao = t.lance == Lance.VENTO ? 45f + t.sorteio.nextInt(35)
                    : t.lance == Lance.SALTO ? 230f : t.lance == Lance.CHOQUE ? 60f : ALCANCE;
                t.vaoAntes = Float.MAX_VALUE;
                t.golpes = t.lance == Lance.ESQUIVA ? 1 : 1 + t.sorteio.nextInt(3);
                t.proximoGolpe = 0;
                t.noAr = t.bateuNoAr = false;
                t.apertaEm[0] = t.apertaEm[1] = -1;
                t.fase = 1;
                t.faseFrame = 0;
                return;
            case 1: {
                boolean encostou = t.faseFrame > 4 && vao >= t.vaoAntes - 0.5f && vao < 60f;
                t.vaoAntes = vao;
                if (t.lance == Lance.CHOQUE) {
                    choque(engine, t, in, vao, encostou);
                    return;
                }
                if (vao > t.alvoVao && !encostou && t.faseFrame < 100) {
                    // Longe, corre; perto, anda. Quem vai defender já levanta a guarda.
                    if (atk.canAct() || atk.forwardDashing) in[a].direction = DemoDirector.Ctx.forward(atk);
                    if (vao > t.alvoVao + 140f && atk.canAct() && !atk.forwardDashing) in[a].dash = true;
                    if (vao < 70f) in[d].direction = DemoDirector.Ctx.back(def);
                    return;
                }
                if (t.lance == Lance.SALTO) {
                    if (!atk.canAct() || !atk.grounded) return;
                    // Pulo para frente (↗ ou ↖) por cima da distância.
                    in[a].jump = true;
                    in[a].direction = atk.facing > 0 ? 8 : 6;
                    in[d].direction = DemoDirector.Ctx.back(def);
                    t.fase = 5;
                    t.faseFrame = 0;
                    return;
                }
            }
                t.fase = 2;
                t.faseFrame = 0;
                // fall through: o primeiro golpe sai já.
            case 2:
                if (t.lance == Lance.ESQUIVA) {
                    // O forte sai e quem ia levar pula para trás no mesmo instante: passa no vento.
                    if (t.golpes > 0 && atk.canAct() && def.canAct() && def.grounded) {
                        CinematicaDirector.apertar("H", in[a]);
                        in[d].jump = true;
                        in[d].direction = def.facing > 0 ? 6 : 8;
                        t.golpes = 0;
                        t.proximoGolpe = t.faseFrame + 6;
                    } else if (t.golpes == 0 && t.faseFrame > t.proximoGolpe && atk.canAct() && def.grounded) {
                        separar(t, -1);
                    } else if (t.golpes == 0) {
                        in[d].direction = DemoDirector.Ctx.back(def);
                    }
                    if (t.faseFrame > 120) separar(t, -1);
                    return;
                }
                // Quem defende segura para trás (defesa em pé) a vez inteira.
                in[d].direction = DemoDirector.Ctx.back(def);
                // A defesa empurrou longe demais: o resto do combo não sai do outro lado da tela.
                if (t.golpes > 0 && t.faseFrame > 0 && vao > t.alvoVao + 60f) t.golpes = 0;
                if (t.golpes > 0 && t.faseFrame >= t.proximoGolpe && atk.canAct()) {
                    CinematicaDirector.apertar(BOTOES[t.sorteio.nextInt(BOTOES.length)], in[a]);
                    t.golpes--;
                    t.proximoGolpe = t.faseFrame + 10 + t.sorteio.nextInt(8);
                } else if ((t.golpes == 0 && atk.canAct() && t.faseFrame > t.proximoGolpe) || t.faseFrame > 150) {
                    int r = t.sorteio.nextInt(10);
                    separar(t, r < 2 ? a : r < 4 ? d : -1);
                }
                return;
            case 5:
                // No ar: segura para frente, solta o forte descendo; quem está embaixo defende.
                in[d].direction = DemoDirector.Ctx.back(def);
                if (!atk.grounded) {
                    t.noAr = true;
                    in[a].direction = DemoDirector.Ctx.forward(atk);
                    if (!t.bateuNoAr && (atk.vy > 0f || vao < 70f)) {
                        CinematicaDirector.apertar("H", in[a]);
                        t.bateuNoAr = true;
                    }
                } else if (t.noAr && atk.canAct()) {
                    // Pousou: o combo defendido continua no chão.
                    t.fase = 2;
                    t.faseFrame = 0;
                    t.alvoVao = ALCANCE;
                    t.golpes = 1 + t.sorteio.nextInt(2);
                    t.proximoGolpe = 4;
                }
                if (t.faseFrame > 120) separar(t, -1);
                return;
            case 3:
                // Separa: um dos dois recua (ou nenhum); quem defendeu segura a defesa um pouco mais.
                if (t.faseFrame < 12 && t.lance != Lance.CHOQUE) in[d].direction = DemoDirector.Ctx.back(def);
                if (t.faseFrame == 12 && t.recua >= 0 && engine.fighter(t.recua).canAct()) in[t.recua].backdash = true;
                if (t.faseFrame >= (t.recua >= 0 ? 30 : 14) && (livres || t.faseFrame > 90)) {
                    t.fase = 0;
                    t.faseFrame = 0;
                }
                return;
            default:
                // Acabando: espera os dois livres (ou um instante) para o roteiro seguir.
                if (livres || t.faseFrame > 60) troca = null;
        }
    }

    private static void separar(Troca t, int recua) {
        t.fase = 3;
        t.faseFrame = 0;
        t.recua = recua;
    }

    /**
     * Choque: os dois vão um para o outro e soltam o médio de modo que os dois fiquem ativos no
     * mesmo quadro (quem tem o golpe mais lento aperta antes): os golpes se chocam. O médio, e
     * não o forte: o forte em pé que pega alguém no meio do golpe vira punish counter (gira e
     * derruba um lado só).
     */
    private void choque(CombatEngine engine, Troca t, FighterInput[] in, float vao, boolean encostou) {
        CombatFighter[] f = {engine.fighter(0), engine.fighter(1)};
        if (t.apertaEm[0] < 0) {
            if (vao > t.alvoVao && !encostou && t.faseFrame < 120) {
                for (int i = 0; i < 2; i++) {
                    if (f[i].canAct() || f[i].forwardDashing) in[i].direction = DemoDirector.Ctx.forward(f[i]);
                    if (vao > 220f && f[i].canAct() && !f[i].forwardDashing) in[i].dash = true;
                }
                return;
            }
            if (!f[0].canAct() || !f[1].canAct()) return;
            int s0 = inicio(f[0]), s1 = inicio(f[1]);
            int base = t.faseFrame;
            t.apertaEm[0] = base + Math.max(0, s1 - s0);
            t.apertaEm[1] = base + Math.max(0, s0 - s1);
        }
        boolean apertou = false;
        for (int i = 0; i < 2; i++) {
            if (t.apertaEm[i] == t.faseFrame) {
                CinematicaDirector.apertar("M", in[i]);
                apertou = true;
            }
        }
        if (apertou && t.faseFrame == Math.max(t.apertaEm[0], t.apertaEm[1])) choques++;
        if (t.faseFrame > Math.max(t.apertaEm[0], t.apertaEm[1])) separar(t, -1);
    }

    /** Quadros até o médio em pé do lutador ficar ativo. */
    private static int inicio(CombatFighter f) {
        AttackDefinition m = f.character().attack("M");
        return m != null ? m.startupFrames : 6;
    }

    private void drive(CombatEngine engine, int lado, FighterInput in) {
        Ordem o = ordens[lado];
        if (o == null) return;
        CombatFighter f = engine.fighter(lado);
        CombatFighter outro = engine.fighter(1 - lado);
        if (o.frame++ >= o.limite || f.ko()) {
            ordens[lado] = null;
            return;
        }
        // Quem está apanhando, caído ou no meio de um golpe termina isso antes de obedecer.
        boolean livre = f.canAct() && f.grounded;
        if (o.golpe != null) {
            golpe(f, outro, lado, o, in, livre);
            return;
        }
        switch (o.movimento.tipo) {
            case TITULAR:
                // No 2x2, quem estava na vez troca com o titular (os irmãos são os titulares).
                if (engine.teams.starterOnPoint(lado)) {
                    ordens[lado] = null;
                } else if (!o.pediu && engine.teams.tagToStarter(lado, f)) {
                    o.pediu = true;
                }
                return;
            case PULAR_TRAS:
                if (!o.pediu) {
                    if (!livre) return;
                    in.jump = true;
                    in.direction = f.facing > 0 ? 6 : 8; // ↖ ou ↗: para trás e para cima
                    o.pediu = true;
                    return;
                }
                // No ar, segura para trás (o pulo vai se afastando); acaba ao pousar.
                in.direction = DemoDirector.Ctx.back(f);
                if (!f.grounded) {
                    o.saiuDoChao = true;
                } else if (o.saiuDoChao) {
                    ordens[lado] = null;
                }
                return;
            case RECUAR:
                if (!o.pediu) {
                    if (!livre) return;
                    in.backdash = true;
                    o.pediu = true;
                    return;
                }
                if (f.backdashFrames == 0 && o.frame > 2) ordens[lado] = null;
                return;
            case CORRER:
            case ANDAR:
                if (o.movimento.distancia >= 0f && vao(f, outro) <= o.movimento.distancia) {
                    ordens[lado] = null;
                    return;
                }
                if (!livre && !f.forwardDashing) return;
                in.direction = DemoDirector.Ctx.forward(f);
                // Correr: o dash para frente continua enquanto segura para frente.
                if (o.movimento.tipo == CinematicaLuta.Movimento.Tipo.CORRER && !f.forwardDashing) in.dash = true;
                return;
            default:
                ordens[lado] = null;
        }
    }

    /**
     * O golpe do QTE: espera o alvo ficar de pé (levantando de uma queda, ainda apanhando),
     * corre até ele e aperta o golpe; acaba quando quem bateu pode agir de novo.
     */
    private void golpe(CombatFighter f, CombatFighter alvo, int lado, Ordem o, FighterInput in, boolean livre) {
        if (o.pediu) {
            if (o.frame - o.pediuEm > 4 && f.canAct() && !f.attacking()) ordens[lado] = null;
            return;
        }
        if (!alvo.hittable() || !alvo.grounded || alvo.inHitstun()) return;
        float vao = vao(f, alvo);
        boolean encostou = f.forwardDashing && vao >= o.vaoAntes - 0.5f;
        o.vaoAntes = vao;
        if (vao > ALCANCE && !encostou) {
            if (!livre && !f.forwardDashing) return;
            in.direction = DemoDirector.Ctx.forward(f);
            if (!f.forwardDashing) in.dash = true;
            return;
        }
        if (!f.canAct()) return;
        apertar(o.golpe.golpe, in);
        o.pediu = true;
        o.pediuEm = o.frame;
    }

    /** O botão do golpe: L/M/H, agachado (2L...: ↓ junto) ou um especial já resolvido (S, S2...). */
    static void apertar(String golpe, FighterInput in) {
        if (golpe.startsWith("S")) {
            // "S" é a energia (o forte); S2, S3... o especial com esse id.
            in.special = golpe.equals("S") || golpe.equals("S1") ? "H" : golpe;
            return;
        }
        char botao = golpe.charAt(golpe.length() - 1);
        if (golpe.startsWith("2")) in.direction = 3; // ↓
        in.light = botao == 'L';
        in.medium = botao == 'M';
        in.heavy = botao == 'H';
    }

    /** Espaço livre entre os dois corpos (px do mundo). */
    static float vao(CombatFighter a, CombatFighter b) {
        return Math.abs(a.x - b.x) - a.body().halfWidth - b.body().halfWidth;
    }
}
