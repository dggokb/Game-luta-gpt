package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

/** A régua da CPU (CpuReach) é o alcance real do motor, e a CPU não solta o golpe que erra o agachado. */
public class CpuReachTest {
    @Test
    public void theTableMatchesTheEngine() {
        // Sprites ou golpes mudaram e a tabela não: rodar o GeraAlcanceCpu de novo.
        List<String> wrong = GeraAlcanceCpu.ids().parallelStream().flatMap(id -> {
            CharacterDefinition c = GeneratedCharacters.get(id);
            return GeraAlcanceCpu.golpes(c).stream().map(g -> {
                float[] m = CpuReach.measured(c, g);
                if (m == null) return id + " " + g + " fora da tabela";
                int stand = GeraAlcanceCpu.ponta(id, g, false), crouch = GeraAlcanceCpu.ponta(id, g, true);
                if (Math.abs(stand - m[0]) > GeraAlcanceCpu.PASSO || Math.abs(crouch - m[1]) > GeraAlcanceCpu.PASSO) {
                    return id + " " + g + ": tabela " + m[0] + "/" + m[1] + ", motor " + stand + "/" + crouch;
                }
                return null;
            });
        }).filter(s -> s != null).collect(Collectors.toList());
        assertTrue(wrong.toString(), wrong.isEmpty());
    }

    @Test
    public void theRulerIsTheDrawnReach() {
        CharacterDefinition ryuga = GeneratedCharacters.get("player_base");
        CharacterDefinition.Body body = ryuga.fighter.body;
        float[] m = CpuReach.measured(ryuga, "2M");
        assertNotNull(m);
        float nominal = CombatRules.maxCenterDistance(ryuga.move("2M", false), body);
        float real = m[0] + body.halfWidth;
        assertTrue("o desenho alcança mais que o pacote: " + nominal + " → " + real, real > nominal + 20f);
        // Entre o alcance do pacote e o real, a CPU agora sabe que o 2M pega (e pega mesmo).
        float gap = (nominal + real) / 2f;
        assertTrue(CpuReach.reaches(ryuga, "2M", false, gap, body, false));
        assertFalse(CombatRules.reaches(ryuga, "2M", false, gap, body));
        assertTrue(GeraAlcanceCpu.acerta("player_base", "2M", Math.round(gap), false));
    }

    @Test
    public void againstACrouchingRivalTheCpuDoesNotThrowWhatWhiffs() {
        // O L, o M e o H do Kōga passam por cima de quem está agachado: a CPU não escolhe nenhum deles.
        CharacterDefinition koga = GeneratedCharacters.get("p03");
        CharacterDefinition.Body body = GeneratedCharacters.get("player_base").fighter.body;
        for (String g : new String[] {"L", "M", "H"}) {
            assertFalse(g, CpuReach.reaches(koga, g, false, 120f, body, true));
            assertFalse(g + " de fato erra", GeraAlcanceCpu.acerta("p03", g, 120, true));
        }
        for (int i = 0; i < 50; i++) {
            String pick = CpuReach.pickInRange(OpponentAi.GROUND_ATTACKS_VS_CROUCH, OpponentAi.GROUND_WEIGHTS_VS_CROUCH,
                koga, false, 150f, body, true, i / 50.0);
            assertFalse(pick, "L".equals(pick) || "M".equals(pick) || "H".equals(pick) || "6M".equals(pick));
        }
    }

    @Test
    public void outsideTheTableTheRulerIsThePack() {
        // No ar (e para quem não está na tabela) vale o alcance do pacote, como antes.
        CharacterDefinition ryuga = GeneratedCharacters.get("player_base");
        CharacterDefinition.Body body = ryuga.fighter.body;
        assertEquals(CombatRules.maxCenterDistance(ryuga.move("H", true), body),
            CpuReach.maxCenterDistance(ryuga, "H", true, body, false), 0.001f);
    }
}
