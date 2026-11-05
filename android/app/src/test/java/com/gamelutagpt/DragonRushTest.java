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
        // Depois da sequência (o H apertado nela vira pancada): o H cancela a recuperação e pega
        // o rival ainda preso.
        CombatEngine.HitEvent h = null;
        for (int i = 0; i < 140 && h == null; i++) {
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
        for (int i = 0; i < 140 && l == null; i++) {
            if (!m.f(0).attacking() || CombatEngine.DRAGON_RUSH.equals(m.f(0).attack.id)) m.in[0].light = true;
            m.step();
            for (CombatEngine.HitEvent e : m.engine.events()) if (e.attacker == 0 && "L".equals(e.moveId)) l = e;
        }
        assertNotNull(l);
        m.in[0].dash = true;
        m.step();
        assertFalse("o segundo Dragon Rush não sai no mesmo combo", m.engine.cues().contains("DRAGON_RUSH"));
    }

    @Test public void doisDragonRushQueSeEncontramAbremOShinkaiClash() {
        Match m = new Match("p13", "player_base", 500f, 1300f);
        m.shinkai();
        // O P2 também transforma (responder a ativação é o choque de auras, não o Clash).
        m.second.shinkai = CombatConfig.SHINKAI_MAX;
        m.in[1].shinkai = true;
        m.step();
        m.steps(m.engine.shinkaiAtivacaoQuadros(m.f(1)) + 4);
        assertTrue(m.second.inShinkai());
        assertNull("a ativação não abre o Clash", m.engine.shinkaiClash());
        m.in[0].dash = true;
        m.in[1].dash = true;
        m.step();
        boolean clash = false, pancada = false;
        for (int i = 0; i < 60 && !clash; i++) {
            m.step();
            clash = m.engine.cues().contains("DRAGON_RUSH_CLASH");
            for (CombatEngine.HitEvent e : m.engine.events()) if (CombatEngine.DRAGON_RUSH.equals(e.moveId)) pancada = true;
        }
        assertTrue("os dois Dragon Rush se encontraram", clash);
        assertFalse("ninguém apanhou: virou o Clash", pancada);
        assertNotNull(m.engine.shinkaiClash());
    }

    @Test public void dragonRushNoRivalTransformadoParadoNaoAbreClash() {
        Match m = new Match("p13", "player_base", 500f, 1300f);
        m.shinkai();
        m.second.shinkai = CombatConfig.SHINKAI_MAX;
        m.in[1].shinkai = true;
        m.step();
        m.steps(m.engine.shinkaiAtivacaoQuadros(m.f(1)) + 4);
        m.in[0].dash = true;
        m.step();
        assertTrue(CombatEngine.dragonRush(m.f(0)));
        // Só um voou: acerta, erra ou o rival esquiva, mas não vira o Clash.
        for (int i = 0; i < 60; i++) {
            m.step();
            assertNull(m.engine.shinkaiClash());
        }
        assertFalse(CombatEngine.dragonRush(m.f(0)));
    }

    /** Acerta o Dragon Rush (p13 no Ryūga parado) e devolve o Match já na sequência. */
    private static Match naSequencia(float xa, float xb) {
        Match m = new Match("p13", "player_base", xa, xb);
        m.shinkai();
        m.in[0].dash = true;
        m.step();
        assertNotNull("o Dragon Rush acertou", m.until(CombatEngine.DRAGON_RUSH, 60));
        assertTrue("começou a sequência", m.f(0).rushSeq > 0);
        return m;
    }

    /** Passa a sequência segurando {@code dir} (e apertando L a cada {@code mash} quadros, 0 = nunca). */
    private static int[] passarSequencia(Match m, int dir, int mash) {
        int socos = 0, dano = 0;
        for (int i = 0; i < 120 && (m.f(0).rushSeq > 0 || i == 0); i++) {
            m.in[0].direction = dir;
            if (mash > 0 && i % mash == 0) m.in[0].light = true;
            m.step();
            for (CombatEngine.HitEvent e : m.engine.events()) {
                if (e.attacker == 0 && CombatEngine.DRAGON_RUSH_SOCO.equals(e.moveId)) {
                    socos++;
                    dano += e.damage;
                }
            }
        }
        return new int[]{socos, dano};
    }

    @Test public void naSequenciaApertarAtaqueDaMaisPancadas() {
        Match parado = naSequencia(500f, 1250f);
        int vida0 = parado.second.life;
        int[] semApertar = passarSequencia(parado, 0, 0);
        int perdeuParado = vida0 - parado.second.life;
        assertEquals("sem apertar, nenhuma pancada", 0, semApertar[0]);

        Match apertando = naSequencia(500f, 1250f);
        int vida1 = apertando.second.life;
        int[] comApertar = passarSequencia(apertando, 0, 2);
        int perdeuApertando = vida1 - apertando.second.life;
        assertEquals("apertando, as 5 pancadas", new CombatConfig().dragonRushSocoMax, comApertar[0]);
        assertTrue("apertar dá mais dano (" + perdeuApertando + " x " + perdeuParado + ")",
            perdeuApertando > perdeuParado + 100);
        // Os botões viraram pancadas, não um L (nem cancelaram a sequência).
        assertFalse("o L não saiu no meio", "L".equals(apertando.f(0).attack == null ? null : apertando.f(0).attack.id)
            && apertando.f(0).rushSeq > 0);
    }

    @Test public void segurarParaCimaLancaORival() {
        Match m = naSequencia(500f, 1250f);
        passarSequencia(m, 7, 0);
        assertTrue(m.engine.cues().contains("DRAGON_RUSH_CIMA") || m.f(0).rushFim == 1);
        float alto = 0f;
        for (int i = 0; i < 50; i++) {
            m.step();
            alto = Math.max(alto, Arena.GROUND_Y - m.f(1).y);
        }
        assertTrue("o rival subiu (" + alto + ")", alto > 120f);
        assertEquals(CombatFighter.Status.AIR_HITSTUN, m.f(1).status);
    }

    @Test public void segurarParaTrasTrocaDeLado() {
        Match m = naSequencia(500f, 1250f);
        assertTrue("o rival estava na frente (à direita)", m.f(1).x > m.f(0).x);
        passarSequencia(m, 5, 0);
        assertEquals(2, m.f(0).rushFim);
        assertTrue("o rival foi para trás dele (à esquerda)", m.f(1).x < m.f(0).x);
        assertEquals("ele virou para o rival", -1, m.f(0).facing);
        assertTrue(m.f(1).inHitstun());
    }

    @Test public void semDirecaoORivalFicaNaFrentePreso() {
        Match m = naSequencia(500f, 1250f);
        passarSequencia(m, 0, 0);
        assertEquals(0, m.f(0).rushFim);
        assertTrue(m.f(1).x > m.f(0).x);
        assertEquals(CombatFighter.Status.HITSTUN, m.f(1).status);
    }

    @Test public void oVooAtravessaABolaDeEnergia() {
        // p15 solta a bola (S1) e o p13 voa por dentro dela até acertar.
        Match m = new Match("p13", "p15", 400f, 1300f);
        m.shinkai();
        m.in[1].special = "S";
        m.step();
        boolean bolaNoAr = false;
        for (int i = 0; i < 40 && !bolaNoAr; i++) {
            m.step();
            bolaNoAr = !m.engine.energyProjectiles.isEmpty();
        }
        assertTrue("a bola saiu", bolaNoAr);
        m.in[0].dash = true;
        m.step();
        boolean atravessou = false, apanhouDaBola = false;
        CombatEngine.HitEvent hit = null;
        for (int i = 0; i < 60 && hit == null; i++) {
            m.step();
            atravessou |= m.engine.cues().contains("DRAGON_RUSH_ATRAVESSA");
            for (CombatEngine.HitEvent e : m.engine.events()) {
                if (e.attacker == 1 && e.projectile) apanhouDaBola = true;
                if (e.attacker == 0 && CombatEngine.DRAGON_RUSH.equals(e.moveId)) hit = e;
            }
        }
        assertTrue("passou pela bola", atravessou);
        assertFalse("a bola não pegou", apanhouDaBola);
        assertNotNull("e acertou o rival", hit);
    }

    @Test public void o2HDoRivalVenceOVoo() {
        Match m = new Match("p13", "player_base", 500f, 1100f);
        m.shinkai();
        m.in[0].dash = true;
        m.step();
        // O rival sobe com o 2H quando o voo está chegando.
        boolean apertou = false, rushAcertou = false, doisHAcertou = false;
        for (int i = 0; i < 60; i++) {
            float gap = Math.abs(m.f(1).x - m.f(0).x);
            if (!apertou && gap < 330f) {
                m.in[1].direction = 3;
                m.in[1].heavy = true;
                apertou = true;
            } else if (apertou) {
                m.in[1].direction = 3;
            }
            m.step();
            for (CombatEngine.HitEvent e : m.engine.events()) {
                if (e.attacker == 0 && CombatEngine.DRAGON_RUSH.equals(e.moveId)) rushAcertou = true;
                if (e.attacker == 1 && "2H".equals(e.moveId) && !e.blocked) doisHAcertou = true;
            }
            if (doisHAcertou) break;
        }
        assertTrue(apertou);
        assertTrue("o 2H pegou quem vinha voando", doisHAcertou);
        assertFalse("o voo não acertou", rushAcertou);
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
