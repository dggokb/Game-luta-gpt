package com.gamelutagpt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A regra fechada de qual reação em pé cada golpe pede (docs/revisao-base/mecanica-ataque.md):
 * forte, especial e Super empurram; fraco e médio jogam a cabeça (acima de 72% da altura) ou
 * dobram o tronco. Os vídeos de reação são gravados para esta regra: mudar aqui pede vídeo novo.
 */
public class ReacaoDoGolpeTest {
    private static CombatFighter defensor() {
        return new CombatEngine(new FighterState(GeneratedCharacters.get("player_base"), "P1"),
            new FighterState(GeneratedCharacters.get("player_base"), "P2"), 500f, 640f, new CombatConfig()).fighter(1);
    }

    private static int reacao(AttackDefinition.Strength forca, float alturaDoAcerto) {
        CombatFighter d = defensor();
        CombatEngine.HitEvent e = new CombatEngine.HitEvent(0, 1, 100, false, false, "X");
        e.strength = forca;
        float alto = d.character().visualStandHeight;
        return GameView.reacaoDoGolpe(e, d, d.y - alto * alturaDoAcerto);
    }

    @Test public void forteEspecialESuperEmpurram() {
        assertEquals(GameView.REACAO_FORTE, reacao(AttackDefinition.Strength.HEAVY, 0.5f));
        assertEquals(GameView.REACAO_FORTE, reacao(AttackDefinition.Strength.SPECIAL, 0.9f));
        assertEquals(GameView.REACAO_FORTE, reacao(AttackDefinition.Strength.SUPER, 0.2f));
    }

    @Test public void fracoEMedioPeloLugarDoAcerto() {
        assertEquals(GameView.REACAO_ROSTO, reacao(AttackDefinition.Strength.LIGHT, 0.85f));
        assertEquals(GameView.REACAO_TRONCO, reacao(AttackDefinition.Strength.LIGHT, 0.55f));
        assertEquals(GameView.REACAO_ROSTO, reacao(AttackDefinition.Strength.MEDIUM, 0.75f));
        assertEquals(GameView.REACAO_TRONCO, reacao(AttackDefinition.Strength.MEDIUM, 0.3f));
    }
}
