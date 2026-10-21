package com.gamelutagpt;

import com.gamelutagpt.motion.Ajustes;
import com.gamelutagpt.motion.Leitura;
import com.gamelutagpt.motion.MotorAnimacao;
import com.gamelutagpt.motion.Pose;

/**
 * Ponte entre a luta e o motor de animação (motion-core): a cada passo lê o CombatFighter e o
 * clipe escolhido e entrega ao {@link MotorAnimacao}. Só apresentação: o motor de combate não
 * lê nada daqui. Guia: docs/motor-animacao.md.
 */
final class AnimacaoLutador {
    /** Os mesmos ajustes para todos os lutadores (o botão ANIM liga e desliga). */
    static final Ajustes AJUSTES = new Ajustes();

    /** Níveis do botão ANIM (e da tecla F4): intensidade, e 0 = desligado. */
    static final float[] NIVEIS = {1f, 1.5f, 0.5f, 0f};
    private static int nivel;

    /** Próximo nível: 100% → 150% → 50% → desligado → 100%. */
    static void proximoNivel() {
        nivel = (nivel + 1) % NIVEIS.length;
        AJUSTES.ligado = NIVEIS[nivel] > 0f;
        if (AJUSTES.ligado) AJUSTES.intensidade = NIVEIS[nivel];
    }

    /** Rótulo do botão ANIM. */
    static String rotulo() {
        return AJUSTES.ligado ? "ANIM " + Math.round(AJUSTES.intensidade * 100f) + "%" : "ANIM OFF";
    }

    private final Leitura leitura = new Leitura();
    final MotorAnimacao motor = new MotorAnimacao(AJUSTES);

    Pose pose() {
        return motor.pose();
    }

    void zerar() {
        motor.zerar();
    }

    /** Um passo da luta, depois do motor de combate e da escolha do clipe. */
    void passo(CombatFighter f, SpriteMotion sprite, CombatEngine engine, float visualY) {
        Leitura l = leitura;
        l.limparEventos();
        l.noChao = f.grounded;
        l.vx = f.travel / CombatConfig.DT;
        l.vy = f.vy;
        l.olhando = f.facing;
        l.x = f.x;
        l.y = visualY;
        l.clipe = sprite.clip;
        l.quadro = sprite.frame();
        l.golpe = f.attacking() || f.status == CombatFighter.Status.ULTRA ||
            f.status == CombatFighter.Status.THROW;
        l.atingido = f.inHitstun() || f.status == CombatFighter.Status.BLOCKSTUN ||
            f.status == CombatFighter.Status.KNOCKDOWN || f.status == CombatFighter.Status.THROWN;
        AttackDefinition attack = f.attacking() ? f.attack : null;
        l.golpeAtivo = attack != null && attack.isActive(f.attackFrame);
        l.preparando = attack != null && f.attackFrame < attack.startupFrames;
        l.forca = attack != null ? peso(attack.strength) : 0;
        // Os dois para a frente e os dois para trás: no chão, o dash e o backdash; no ar, o air
        // dash. O pulo que sai da corrida leva o embalo (e o rastro); o super pulo não.
        l.dash = (f.grounded && (f.forwardDashing || f.backdashFrames > 0)) || f.airDashFrames > 0;
        l.embalo = !f.grounded && f.jumpMode == CombatFighter.PULO_CORRIDA;
        l.peso = f.character().fighter.animationWeight;
        l.congelado = f.frozen() || engine.superFreezeActive();
        for (CombatEngine.HitEvent e : engine.events()) {
            if (e.defender != f.index || engine.fighter(f.index) != f) continue;
            CombatFighter quemBate = engine.fighter(e.attacker);
            l.empurrao = f.x >= quemBate.x ? 1 : -1;
            if (e.blocked) l.defendeu = true;
            else l.apanhou = Math.max(l.apanhou, e.strength != null ? peso(e.strength) : 1);
        }
        motor.passo(l);
    }

    /** 0 fraco, 1 médio, 2 forte, 3 especial ou Super. */
    private static int peso(AttackDefinition.Strength s) {
        switch (s) {
            case LIGHT: return 0;
            case MEDIUM: return 1;
            case HEAVY: return 2;
            default: return 3;
        }
    }
}
