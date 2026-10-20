package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** O que a luta conta ao narrador: começo, golpes, combos, Super, K.O., vitória, tempo. */
public class NarradorLutaTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    /** Uma batalha 2x2 com o narrador ouvindo. */
    private static final class Luta {
        final FighterState[] p1 = {new FighterState(BASE, "P1"), new FighterState(TWO, "P2")};
        final FighterState[] cpu = {new FighterState(NPC, "C1"), new FighterState(BASE, "C2")};
        final CombatEngine engine = new CombatEngine(p1[0], cpu[0], 500f, 600f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        final BattleFlow flow = new BattleFlow();
        final NarradorLuta narrador = new NarradorLuta();
        final List<String> eventos = new ArrayList<>();
        final List<Map<String, String>> vars = new ArrayList<>();

        Luta() {
            engine.setTeam(0, p1);
            engine.setTeam(1, cpu);
            flow.setBattle(true);
            flow.reset();
            flow.startCountdown();
            narrador.reset();
            narrador.setNomes(c -> c == BASE ? "Ryūga" : "Kōga");   // nomes do elenco
        }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
            flow.update(engine, true);
            narrador.update(engine, flow, (e, v) -> {
                eventos.add(e);
                vars.add(new HashMap<>(v));
            });
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void go() { steps(BattleFlow.READY_FRAMES + 2); }

        Map<String, String> de(String evento) {
            int i = eventos.lastIndexOf(evento);
            return i < 0 ? null : vars.get(i);
        }
    }

    @Test public void theFightStartsWithBothNames() {
        Luta l = new Luta();
        l.steps(BattleFlow.READY_FRAMES - 2);
        assertFalse("nada antes do GO", l.eventos.contains("inicio"));
        l.go();
        assertEquals(1, java.util.Collections.frequency(l.eventos, "inicio"));
        assertEquals("Ryūga", l.de("inicio").get("p1"));
        assertEquals("Kōga", l.de("inicio").get("p2"));
    }

    @Test public void theFirstHitAndAComboAreCalled() {
        Luta l = new Luta();
        l.go();
        // L > M > H do P1 colado: o primeiro golpe e, se o combo passar de 5, o combo.
        String[] seq = {"L", "M", "H"};
        int k = 0;
        l.in[0].light = true;
        for (int i = 0; i < 120; i++) {
            l.step();
            for (CombatEngine.HitEvent h : l.engine.events()) {
                if (h.attacker == 0 && !h.blocked && k < 2) {
                    k++;
                    if ("M".equals(seq[k])) l.in[0].medium = true; else l.in[0].heavy = true;
                }
            }
        }
        assertTrue(l.eventos.contains("primeiro_golpe"));
        assertEquals("Ryūga", l.de("primeiro_golpe").get("atacante"));
        assertEquals("um primeiro golpe só", 1, java.util.Collections.frequency(l.eventos, "primeiro_golpe"));
    }

    @Test public void aLongComboIsCalledWithItsSize() {
        Luta l = new Luta();
        l.go();
        // Combo longo forçado: o motor fecha a sessão do defensor com 6 acertos.
        for (int i = 0; i < 6; i++) {
            l.in[0].light = true;
            l.steps(14);
        }
        l.steps(60);
        ComboSession s = l.engine.lastSession(1);
        if (s != null && s.hitCount >= NarradorLuta.COMBO) {
            String evento = s.hitCount >= NarradorLuta.COMBO_GRANDE ? "combo_grande" : "combo";
            assertTrue(l.eventos.contains(evento));
            assertEquals(String.valueOf(s.hitCount), l.de(evento).get("n"));
        }
    }

    @Test public void theSuperIsCalledWhenItStarts() {
        Luta l = new Luta();
        l.go();
        l.p1[0].superMeter = CombatConfig.MAX_METER;
        l.in[0].superAttack = true;
        l.steps(10);
        assertTrue(l.eventos.contains("super"));
        assertEquals("Ryūga", l.de("super").get("atacante"));
    }

    @Test public void knockoutsAndTheWinnerAreCalled() {
        Luta l = new Luta();
        l.go();
        // O titular do CPU cai: K.O. dele e o parceiro entra.
        l.cpu[0].takeDamage(l.cpu[0].life, 0);
        l.steps(200);
        assertTrue(l.eventos.contains("ko"));
        assertEquals("Kōga", l.de("ko").get("perdedor"));
        assertTrue("o parceiro entra", l.eventos.contains("troca"));
        // O parceiro cai também: fim da luta, vitória do P1.
        l.cpu[1].takeDamage(l.cpu[1].life, 0);
        l.steps(30);
        assertTrue(l.flow.over());
        assertTrue(l.eventos.contains("perfeito") || l.eventos.contains("vitoria"));
        String evento = l.eventos.contains("perfeito") ? "perfeito" : "vitoria";
        assertEquals("Ryūga", l.de(evento).get("vencedor"));
        assertEquals("o fim é a última fala", evento, l.eventos.get(l.eventos.size() - 1));
    }

    @Test public void lowLifeAndTheLastSecondsAreCalledOnce() {
        Luta l = new Luta();
        l.go();
        l.p1[0].takeDamage(l.p1[0].life * 8 / 10, 0);
        l.steps(5);
        assertEquals(1, java.util.Collections.frequency(l.eventos, "apuros"));
        assertEquals("Ryūga", l.de("apuros").get("nome"));
        l.steps(BattleFlow.TIME_LIMIT_FRAMES - (BattleFlow.URGENT_SECONDS - 1) * BattleFlow.FRAMES_PER_SECOND);
        assertEquals(1, java.util.Collections.frequency(l.eventos, "tempo"));
    }

    @Test public void timeOverNamesWhoHadMoreLife() {
        Luta l = new Luta();
        l.go();
        l.cpu[0].takeDamage(l.cpu[0].life / 3, 0);
        l.steps(BattleFlow.TIME_LIMIT_FRAMES + 10);
        assertTrue(l.flow.timeOver());
        assertTrue(l.eventos.contains("tempo_esgotado"));
        assertEquals("Ryūga", l.de("tempo_esgotado").get("vencedor"));
    }

    @Test public void everyEventItAsksForExistsInTheFile() throws Exception {
        String json = new String(java.nio.file.Files.readAllBytes(
            new java.io.File("src/main/assets/historia/narrador.json").toPath()), java.nio.charset.StandardCharsets.UTF_8);
        for (String evento : new String[] {"inicio", "primeiro_golpe", "combo", "combo_grande", "super", "ultra",
            "guard_break", "combo_breaker", "defesa_perfeita", "agarrao", "punicao", "troca", "apuros", "dominando",
            "virada", "tempo", "ko", "ko_ultra", "vitoria", "perfeito", "tempo_esgotado", "empate"}) {
            assertTrue("narrador.json tem " + evento, json.contains("\"" + evento + "\""));
        }
    }
}
