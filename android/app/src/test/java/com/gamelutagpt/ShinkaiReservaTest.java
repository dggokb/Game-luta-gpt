package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * A regra da reserva para todos os SHINKAIs (02/11): cada tipo ganha uma vida nova ao ligar, sem
 * cronômetro, com o limite, a Ruptura e a transformação quebrada, e mantém a característica dele.
 */
public class ShinkaiReservaTest {
    private static final class Match {
        final FighterState a, b;
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(String p1, String p2, float xa, float xb) {
            a = new FighterState(GeneratedCharacters.get(p1), "P1");
            b = new FighterState(GeneratedCharacters.get(p2), "CPU");
            engine = new CombatEngine(a, b, xa, xb, new CombatConfig());
        }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        boolean ate(String cue, int n) {
            for (int i = 0; i < n; i++) {
                step();
                if (engine.cues().contains(cue)) return true;
            }
            return false;
        }

        void shinkai() {
            a.shinkai = CombatConfig.SHINKAI_MAX;
            in[0].shinkai = true;
            step();
            assertTrue(engine.cues().contains("SHINKAI"));
            for (int i = 0, n = engine.shinkaiAtivacaoQuadros(engine.fighter(0)); i < n; i++) step();
        }
    }

    @Test public void todoTipoGanhaAVidaNovaEFicaSemCronometro() {
        for (String id : new String[] {"p13", "p15", "p08", "p02"}) {
            Match m = new Match(id, "p15", 300f, 1500f);
            m.shinkai();
            assertNotEquals(Shinkai.Type.PERCEPCAO, m.a.shinkaiType);
            assertEquals(id, m.engine.config.ryugaReservaVida, m.a.reserva);
            for (int i = 0; i < 60 * 15; i++) m.step();
            assertTrue(id + ": parado 15 s, segue no SHINKAI (sem os 8 s de antes)", m.a.inShinkai());
        }
    }

    @Test public void aReservaQuebraTambemNosOutros() {
        Match m = new Match("p13", "p15", 300f, 1500f);
        m.shinkai();
        m.a.takeDamage(m.a.reserva - m.engine.config.ryugaReservaLimite + 10, 0);
        assertTrue(m.ate("LIMITE", 3));
        m.a.takeDamage(m.a.reserva + 100, 0);
        assertTrue(m.ate("SHINKAI_QUEBRA", 3));
        assertFalse(m.a.inShinkai());
        assertEquals(m.engine.config.ryugaQuebraExaustao, m.a.exaustaoQuadros, 2);
    }

    @Test public void osOutrosTambemRompemEDesligam() {
        Match m = new Match("p02", "p15", 300f, 1500f);
        m.shinkai();
        m.a.takeDamage(m.a.reserva - 200, 0);
        m.in[0].shinkai = true;
        assertTrue(m.ate("RUPTURA", 3));
        assertTrue(m.a.emRuptura());
        int vida = m.a.life;
        for (int i = 0; i < 60; i++) m.step();
        assertTrue("a Ruptura gasta a vida", m.a.life < vida);
        m.in[0].shinkai = true;
        assertTrue(m.ate("RUPTURA_FIM", 3));
        assertFalse(m.a.inShinkai());
    }

    @Test public void oSumicoDoSpeedGastaAReserva() {
        String speed = null;
        for (String id : BateriaBalanco.ELENCO) if (Shinkai.typeOf(id) == Shinkai.Type.SPEED) { speed = id; break; }
        assertNotNull(speed);
        Match m = new Match(speed, "p15", 600f, 900f);
        m.shinkai();
        int antes = m.a.reserva;
        // → → no chão, livre, perto: some e aparece atrás.
        for (int i = 0; i < 12 && m.a.reserva == antes; i++) {
            m.in[0].direction = MotionParser.relative(1, m.engine.fighter(0).facing);
            m.in[0].dash = true;
            m.step();
            m.in[0].direction = 0;
            m.step();
        }
        assertEquals(antes - m.engine.config.shinkaiReservaSumico, m.a.reserva);
    }
}
