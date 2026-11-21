package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Os especiais já têm o movimento no desenho: o motor de animação não põe antecipação nem
 * continuidade neles (o corpo ia e voltava a cada acerto). Os normais continuam com elas.
 */
public class AnimacaoNosEspeciaisTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    /** Maior inclinação (graus, em módulo) enquanto o golpe da sequência roda. */
    private static float maiorInclinacao(int[] direcoes, PadInput.Button botao) {
        CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"),
            500f, 640f, new CombatConfig());
        PadInput pad = new PadInput();
        FighterInput in = new FighterInput(), cpu = new FighterInput();
        AnimacaoLutador animacao = new AnimacaoLutador();
        SpriteMotion sprite = new SpriteMotion(BASE);
        float maior = 0f;
        boolean golpeou = false;
        for (int q = 0; q < 120; q++) {
            if (q < direcoes.length * 2) pad.setDirection(direcoes[q / 2]);
            else pad.setDirection(0);
            if (q == direcoes.length * 2 - 1) pad.press(botao);
            pad.drainInto(in);
            e.step(in, cpu);
            CombatFighter f = e.fighter(0);
            animacao.passo(f, sprite, e, f.y);
            if (f.attacking()) {
                golpeou = true;
                maior = Math.max(maior, Math.abs(animacao.pose().inclinacao));
            }
        }
        assertTrue("o golpe saiu", golpeou);
        return maior;
    }

    /** O motor começa desligado (21/11) e as medidas abaixo o ligam só enquanto medem. */
    private static float comMotorLigado(int[] direcoes, PadInput.Button botao) {
        boolean antes = AnimacaoLutador.AJUSTES.ligado;
        AnimacaoLutador.AJUSTES.ligado = true;
        try {
            return maiorInclinacao(direcoes, botao);
        } finally {
            AnimacaoLutador.AJUSTES.ligado = antes;
        }
    }

    @Test public void especialNaoBalancaOCorpo() {
        // S2: → ↓ ↘ + H (direções da tela, olhando para a direita).
        assertTrue(comMotorLigado(new int[] {1, 3, 2}, PadInput.Button.HEAVY) < 0.5f);
    }

    @Test public void normalForteContinuaComAntecipacaoEContinuidade() {
        assertTrue(comMotorLigado(new int[] {0}, PadInput.Button.HEAVY) > 1f);
    }

    /** Pedido do dono (21/11): o jogo começa sem nenhuma mexida por cima do desenho. */
    @Test public void oMotorComecaDesligadoENaoMexeNoGolpe() {
        assertFalse(AnimacaoLutador.AJUSTES.ligado);
        assertEquals("ANIM OFF", AnimacaoLutador.rotulo());
        assertTrue(maiorInclinacao(new int[] {0}, PadInput.Button.HEAVY) < 0.01f);
    }
}
