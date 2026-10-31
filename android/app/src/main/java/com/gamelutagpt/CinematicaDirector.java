package com.gamelutagpt;

import com.gamelutagpt.historia.CinematicaLuta;

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

    /** Começa um movimento do lado 0 (jogador) ou 1 (CPU); "parar" cancela o que estiver rodando. */
    void mover(int lado, CinematicaLuta.Movimento movimento) {
        ordens[lado] = movimento.tipo == CinematicaLuta.Movimento.Tipo.PARAR ? null : new Ordem(movimento);
    }

    /** O lado 0 (jogador) ou 1 (CPU) vai até o outro e dá o golpe do QTE. */
    void golpear(int lado, CinematicaLuta.Golpe golpe) {
        ordens[lado] = new Ordem(golpe);
    }

    boolean movendo(int lado) {
        return ordens[lado] != null;
    }

    /** Cancela tudo (fim da cena, luta nova). */
    void parar() {
        ordens[0] = ordens[1] = null;
    }

    /** O comando dos dois neste quadro (chame antes do {@code engine.step}). */
    void fill(CombatEngine engine, FighterInput p1, FighterInput cpu) {
        p1.clear();
        cpu.clear();
        drive(engine, 0, p1);
        drive(engine, 1, cpu);
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
