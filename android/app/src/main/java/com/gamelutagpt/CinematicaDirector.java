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

    /** Um movimento em andamento. */
    private static final class Ordem {
        final CinematicaLuta.Movimento movimento;
        final int limite;
        int frame;
        /** O comando já saiu (o lutador estava livre para obedecer). */
        boolean pediu;
        boolean saiuDoChao;

        Ordem(CinematicaLuta.Movimento movimento) {
            this.movimento = movimento;
            limite = Math.max(1, Math.round(movimento.segundos * FPS));
        }
    }

    private final Ordem[] ordens = new Ordem[2];

    /** Começa um movimento do lado 0 (jogador) ou 1 (CPU); "parar" cancela o que estiver rodando. */
    void mover(int lado, CinematicaLuta.Movimento movimento) {
        ordens[lado] = movimento.tipo == CinematicaLuta.Movimento.Tipo.PARAR ? null : new Ordem(movimento);
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

    /** Espaço livre entre os dois corpos (px do mundo). */
    static float vao(CombatFighter a, CombatFighter b) {
        return Math.abs(a.x - b.x) - a.body().halfWidth - b.body().halfWidth;
    }
}
