package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Voo de verdade (11/11): o S2 (shoryuken) e o S3 (tatsumaki) do Ryūga tiram o corpo do chão e
 * caem pela gravidade do pulo, em vez de só levantar o desenho. O ponto onde ele pousa é o lugar
 * novo dele; para quem defende, o golpe continua sendo de chão.
 */
public class VooDoGolpeTest {
    private static final CharacterDefinition RYUGA = GeneratedCharacters.get("player_base");

    /** O Ryūga solta o especial longe do rival e anda o golpe inteiro; devolve o caminho dele. */
    private static float[][] caminho(String especial) {
        return caminho(RYUGA, especial);
    }

    /** {@code quem} solta o especial longe do rival; devolve o caminho dele, quadro a quadro. */
    private static float[][] caminho(CharacterDefinition quem, String especial) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(quem, RYUGA, 500f, 1400f);
        CombatFighter me = s.f(0);
        me.commandButton = "M";
        me.buffer.push(InputBuffer.Button.SPECIAL, especial, false);
        float[][] out = new float[120][];
        for (int fr = 0; fr < out.length; fr++) {
            s.step();
            out[fr] = new float[] {me.x, Arena.GROUND_Y - me.y, me.grounded ? 1f : 0f,
                me.attacking() && especial.equals(me.attack.id) ? me.attackFrame : -1f,
                CombatEngine.atacaDoAr(me) ? 1f : 0f};
        }
        return out;
    }

    @Test public void oShoryukenSobeAvancaECaiPelaGravidade() {
        float[][] c = caminho("S2");
        AttackDefinition.Mechanics m = RYUGA.attack("S2").mechanics;
        assertTrue("o S2 voa", m.flies());
        int decolou = -1, pousou = -1;
        float alto = 0f;
        for (int i = 0; i < c.length; i++) {
            if (decolou < 0 && c[i][2] == 0f) decolou = i;
            if (decolou >= 0 && pousou < 0 && c[i][2] == 1f) pousou = i;
            alto = Math.max(alto, c[i][1]);
            if (c[i][2] == 0f) assertEquals("no ar, o shoryuken ainda se defende agachado", 0f, c[i][4], 0f);
        }
        assertTrue("saiu do chão", decolou > 0);
        assertTrue("pousou", pousou > decolou);
        assertEquals("sobe a altura do voo", m.flightHeight, alto, 12f);
        // A gravidade do pulo leva ~33 quadros para subir 170 e voltar; o desenho pousa no quadro 42.
        int noAr = pousou - decolou;
        assertTrue("tempo no ar " + noAr, noAr >= 28 && noAr <= m.flightEnd - m.flightStart + 1);
        assertTrue("pousa adiante: " + (c[pousou][0] - c[0][0]), c[pousou][0] - c[0][0] > 90f);
        // O golpe segue depois do pouso (a animação do pouso) e termina no chão, sem voltar.
        assertTrue("a recuperação acontece no chão", c[pousou][3] >= m.flightEnd);
        float xFinal = c[c.length - 1][0];
        assertEquals("o lugar novo é onde pousou", c[pousou][0], xFinal, 6f);
        assertEquals(0f, c[c.length - 1][1], 0.01f);
    }

    @Test public void oTatsumakiAndaRetoNoAr() {
        float[][] c = caminho("S3");
        AttackDefinition.Mechanics m = RYUGA.attack("S3").mechanics;
        assertTrue("o S3 voa e paira", m.flies() && m.flightHover);
        float min = Float.MAX_VALUE, max = 0f;
        int noAr = 0;
        for (float[] q : c) {
            if (q[2] == 0f && q[3] >= m.flightStart + 10 && q[3] < m.flightEnd - 12) {
                min = Math.min(min, q[1]);
                max = Math.max(max, q[1]);
                noAr++;
            }
        }
        assertTrue("fica no ar enquanto chuta", noAr > 20);
        assertEquals("anda reto: a altura não muda no meio do giro", 0f, max - min, 2f);
        assertEquals("na altura do voo (o passo de 1/60 s perde um pouco no topo)", m.flightHeight, max, m.flightHeight * 0.2f);
        assertTrue("termina no chão", c[c.length - 1][2] == 1f);
        assertTrue("anda para a frente", c[c.length - 1][0] - c[0][0] > 200f);
    }

    @Test public void oChuteDaGarcaSaiDoChaoSemAndar() {
        // A Yin-lu (11/11): o S2 sobe numa perna (60 px) e cai onde saiu; o S4 gira no ar.
        CharacterDefinition yinlu = GeneratedCharacters.get("p02");
        float[][] c = caminho(yinlu, "S2");
        float alto = 0f;
        boolean voou = false;
        for (float[] q : c) {
            alto = Math.max(alto, q[1]);
            voou |= q[2] == 0f;
            if (q[2] == 0f) assertEquals("no ar, ainda se defende agachado", 0f, q[4], 0f);
        }
        assertTrue("o S2 sai do chão", voou);
        assertEquals("sobe uns 60 px", 60f, alto, 12f);
        assertEquals("cai onde saiu", c[0][0], c[c.length - 1][0], 4f);
        float[][] giro = caminho(yinlu, "S4");
        boolean noAr = false;
        for (float[] q : giro) noAr |= q[2] == 0f && q[3] >= 0f;
        assertTrue("o S4 tira o corpo do chão", noAr);
        assertEquals("e termina no chão", 1f, giro[giro.length - 1][2], 0f);
    }

    /**
     * O rival solta a rasteira (2M) a {@code gap} px enquanto o Ryūga faz o S2 (ou fica parado);
     * devolve {o Ryūga apanhou da rasteira, a rasteira saiu}.
     */
    private static boolean[] rasteira(boolean shoryuken, float gap, int quando) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(RYUGA, RYUGA, 600f, 600f + gap);
        CombatFighter me = s.f(0), rival = s.f(1);
        int vida = me.state.life;
        if (shoryuken) {
            me.commandButton = "M";
            me.buffer.push(InputBuffer.Button.SPECIAL, "S2", false);
        }
        boolean saiu = false;
        for (int fr = 0; fr < 70; fr++) {
            if (fr == quando) {
                s.in[1].direction = 3;
                s.in[1].medium = true;
            }
            s.step();
            s.in[1].medium = false;
            saiu |= rival.attacking() && "2M".equals(rival.attack.id) && rival.attack.isActive(rival.attackFrame);
            if (fr >= 40) s.in[1].direction = 0;
        }
        return new boolean[] {me.state.life < vida, saiu};
    }

    @Test public void aRasteiraPassaPorBaixoDoShoryuken() {
        // A rasteira chega quando o Ryūga está no alto do S2 (decola no quadro 7, topo perto do 24).
        float gap = 200f;   // de 160 a 260 px a rasteira pega o Ryūga parado
        int quando = 8;
        boolean[] parado = rasteira(false, gap, quando);
        assertTrue("controle: a rasteira sai e pega o Ryūga parado", parado[1] && parado[0]);
        boolean[] noAr = rasteira(true, gap, quando);
        assertTrue("a rasteira saiu", noAr[1]);
        assertFalse("a rasteira passa por baixo de quem está no ar", noAr[0]);
    }
}
