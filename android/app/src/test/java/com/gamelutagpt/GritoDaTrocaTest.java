package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** "Troca comigo!": o parceiro grita quando a vida de quem luta entra no vermelho. */
public class GritoDaTrocaTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState second = new FighterState(TWO, "P2");
        final FighterState cpu = new FighterState(NPC, "CPU");
        final FighterState cpuPartner = new FighterState(TWO, "CPU2");
        final CombatEngine engine = new CombatEngine(first, cpu, 500f, 1100f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        final GritoDaTroca grito = new GritoDaTroca();

        Match() {
            engine.setTeam(0, first, second);
            engine.setTeam(1, cpu, cpuPartner);
        }

        int step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
            return grito.update(engine);
        }

        static void vida(FighterState s, int permille) {
            s.life = (int)(s.profile.maxLife * (long)permille / 1000L);
        }
    }

    @Test public void thePartnerShoutsWhenThePointCrossesIntoTheRed() {
        Match m = new Match();
        assertEquals(0, m.step());
        Match.vida(m.first, 400);
        assertEquals(0, m.step());
        Match.vida(m.first, 240);
        assertEquals("jogador", 1, m.step());
        assertEquals("uma vez só", 0, m.step());
        Match.vida(m.first, 200);
        assertEquals("já estava no vermelho", 0, m.step());
        // Saiu do vermelho (a vida vermelha voltou) e entrou de novo: grita de novo.
        Match.vida(m.first, 300);
        m.step();
        Match.vida(m.first, 250);
        assertEquals(1, m.step());
    }

    @Test public void bothSidesShout() {
        Match m = new Match();
        m.step();
        Match.vida(m.cpu, 100);
        assertEquals("adversário", 2, m.step());
    }

    @Test public void noShoutWithoutAStandingPartnerOrOnTheKnockout() {
        Match m = new Match();
        m.step();
        m.second.life = 0;
        Match.vida(m.first, 100);
        assertEquals("parceiro caído", 0, m.step());

        Match k = new Match();
        k.step();
        k.first.life = 0;
        assertEquals("o golpe que derruba", 0, k.step());
    }

    @Test public void comingInAlreadyRedDoesNotShout() {
        Match m = new Match();
        Match.vida(m.second, 100);
        m.step();
        m.in[0].tag = true;
        int gritos = 0;
        for (int i = 0; i < 120; i++) gritos |= m.step();
        assertSame("trocou", m.second, m.engine.team(0).pointState());
        assertEquals(0, gritos);
    }

    @Test public void theTextCallsThePointByName() {
        assertEquals("Ryūga! Troca comigo!", GritoDaTroca.texto("Ryūga", t -> t));
        assertEquals("Troca comigo!", GritoDaTroca.texto(null, t -> t));
        assertEquals("Kōga! Tag me in!", GritoDaTroca.texto("Kōga", t -> "{nome}! Tag me in!"));
    }
}
