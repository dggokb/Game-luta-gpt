package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Dragon Rush: no SHINKAI (qualquer tipo), → → não corre: voa no rival, no chão e no ar, bate
 * com impacto e deixa emendar o combo. Um por combo.
 */
public class DragonRushTest {
    private static final class Match {
        final FighterState first, second;
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(String p1, String p2, float xa, float xb) {
            first = new FighterState(GeneratedCharacters.get(p1), "P1");
            second = new FighterState(GeneratedCharacters.get(p2), "P2");
            engine = new CombatEngine(first, second, xa, xb, new CombatConfig());
        }

        CombatFighter f(int i) { return engine.fighter(i); }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void shinkai() {
            first.shinkai = CombatConfig.SHINKAI_MAX;
            in[0].shinkai = true;
            step();
            assertTrue(engine.cues().contains("SHINKAI"));
            steps(engine.shinkaiAtivacaoQuadros(f(0)) + 2);
        }

        /** Passa até o golpe {@code id} de quem está no lado 0 acertar (ou falha). */
        CombatEngine.HitEvent until(String id, int max) {
            for (int i = 0; i < max; i++) {
                step();
                for (CombatEngine.HitEvent e : engine.events()) {
                    if (e.attacker == 0 && id.equals(e.moveId)) return e;
                }
            }
            return null;
        }
    }

    @Test public void semShinkaiOTraçoParaFrenteContinuaCorrida() {
        Match m = new Match("p13", "player_base", 600f, 1300f);
        m.in[0].dash = true;
        m.in[0].direction = 1;
        m.step();
        assertFalse(m.engine.cues().contains("DRAGON_RUSH"));
        assertTrue(m.f(0).forwardDashing);
    }

    @Test public void noShinkaiVoaNoRivalEAcertaDeLonge() {
        Match m = new Match("p13", "player_base", 500f, 1250f);
        m.shinkai();
        float x0 = m.f(0).x;
        m.in[0].dash = true;
        m.step();
        assertTrue(m.engine.cues().contains("DRAGON_RUSH"));
        assertEquals(CombatEngine.DRAGON_RUSH, m.f(0).attack.id);
        CombatEngine.HitEvent hit = m.until(CombatEngine.DRAGON_RUSH, 60);
        assertNotNull("o Dragon Rush encosta e acerta", hit);
        assertFalse(hit.blocked);
        assertTrue("atravessou a distância voando", m.f(0).x - x0 > 500f);
        assertTrue("o rival fica preso", m.f(1).inHitstun());
    }

    @Test public void depoisDoAcertoEmendaUmNormal() {
        Match m = new Match("p13", "player_base", 500f, 1250f);
        m.shinkai();
        m.in[0].dash = true;
        m.step();
        assertNotNull(m.until(CombatEngine.DRAGON_RUSH, 60));
        // Logo depois do impacto: o H cancela a recuperação e pega o rival ainda preso.
        CombatEngine.HitEvent h = null;
        for (int i = 0; i < 40 && h == null; i++) {
            if (!m.f(0).attacking() || CombatEngine.DRAGON_RUSH.equals(m.f(0).attack.id)) m.in[0].heavy = true;
            m.step();
            for (CombatEngine.HitEvent e : m.engine.events()) if (e.attacker == 0 && "H".equals(e.moveId)) h = e;
        }
        assertNotNull("o H emendou no Dragon Rush", h);
        assertFalse("combo: o H não foi defendido", h.blocked);
    }

    @Test public void voaParaCimaNoRivalNoAr() {
        Match m = new Match("p13", "player_base", 500f, 1100f);
        m.shinkai();
        // O rival pula; no alto do pulo, o Dragon Rush sobe atrás dele.
        m.in[1].jump = true;
        m.step();
        m.steps(14);
        assertFalse(m.f(1).grounded);
        m.in[0].dash = true;
        m.step();
        float altura = 0f;
        CombatEngine.HitEvent hit = null;
        for (int i = 0; i < 60 && hit == null; i++) {
            m.step();
            altura = Math.max(altura, Arena.GROUND_Y - m.f(0).y);
            for (CombatEngine.HitEvent e : m.engine.events()) if (e.attacker == 0 && CombatEngine.DRAGON_RUSH.equals(e.moveId)) hit = e;
        }
        assertNotNull("pegou o rival no ar", hit);
        assertTrue("subiu voando (" + altura + ")", altura > 60f);
    }

    @Test public void umPorCombo() {
        Match m = new Match("p13", "player_base", 500f, 1250f);
        m.shinkai();
        m.in[0].dash = true;
        m.step();
        assertNotNull(m.until(CombatEngine.DRAGON_RUSH, 60));
        // Emenda o L e tenta outro Dragon Rush no meio do combo: não sai.
        CombatEngine.HitEvent l = null;
        for (int i = 0; i < 40 && l == null; i++) {
            if (!m.f(0).attacking() || CombatEngine.DRAGON_RUSH.equals(m.f(0).attack.id)) m.in[0].light = true;
            m.step();
            for (CombatEngine.HitEvent e : m.engine.events()) if (e.attacker == 0 && "L".equals(e.moveId)) l = e;
        }
        assertNotNull(l);
        m.in[0].dash = true;
        m.step();
        assertFalse("o segundo Dragon Rush não sai no mesmo combo", m.engine.cues().contains("DRAGON_RUSH"));
    }

    @Test public void speedPertoAindaSome() {
        Match m = new Match("p16", "p15", 600f, 900f);
        m.shinkai();
        m.in[0].dash = true;
        m.step();
        assertTrue(m.engine.cues().contains("VANISH"));
        assertFalse(m.engine.cues().contains("DRAGON_RUSH"));
    }
}
