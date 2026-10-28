package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * SHINKAI Clash: ligar o SHINKAI com o rival já no dele vira um cabo de guerra jogável; cada
 * tipo empurra de um jeito, quem leva a marca até o fim (ou está na frente no fim do tempo)
 * acerta forte e fica com o SHINKAI cheio, e quem perde voa e perde o dele.
 */
public class ShinkaiClashTest {
    private static final class Match {
        final FighterState first, cpu;
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(String p1, String p2) {
            first = new FighterState(GeneratedCharacters.get(p1), "P1");
            cpu = new FighterState(GeneratedCharacters.get(p2), "CPU");
            engine = new CombatEngine(first, cpu, 500f, 900f, new CombatConfig());
        }

        CombatFighter f(int i) { return engine.fighter(i); }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void shinkai(int side) {
            (side == 0 ? first : cpu).shinkai = CombatConfig.SHINKAI_MAX;
            in[side].shinkai = true;
            step();
        }

        /** P1 liga, o brilho passa, o CPU responde: começa o confronto; espera a entrada. */
        ShinkaiClash clash() {
            shinkai(0);
            steps(engine.config.shinkaiFlashFrames + 2);
            shinkai(1);
            assertTrue(engine.cues().contains("SHINKAI_CLASH"));
            ShinkaiClash c = engine.shinkaiClash();
            assertNotNull(c);
            while (!c.disputando()) step();
            return c;
        }
    }

    @Test public void answeringTheRivalsShinkaiStartsTheClash() {
        Match m = new Match("p13", "p16");
        ShinkaiClash c = m.clash();
        assertEquals(Shinkai.Type.POWER, c.tipo[0]);
        assertEquals(Shinkai.Type.SPEED, c.tipo[1]);
        assertEquals("face to face", 1, m.f(0).facing);
        assertEquals(-1, m.f(1).facing);
        assertEquals(m.engine.config.shinkaiClashGap, m.f(1).x - m.f(0).x, 0.5f);
        assertTrue(m.f(0).grounded && m.f(1).grounded);
    }

    @Test public void withoutTheRivalInShinkaiThereIsNoClash() {
        Match m = new Match("p13", "p16");
        m.shinkai(0);
        m.steps(m.engine.config.shinkaiFlashFrames + 2);
        assertNull(m.engine.shinkaiClash());
        // Depois que o do P1 acaba, o CPU liga sozinho: sem confronto.
        m.first.shinkaiFrames = 0;
        m.shinkai(1);
        assertNull(m.engine.shinkaiClash());
    }

    @Test public void theFightStopsDuringTheClash() {
        Match m = new Match("p13", "p16");
        m.clash();
        int a = m.first.shinkaiFrames, b = m.cpu.shinkaiFrames;
        float x = m.f(0).x;
        for (int i = 0; i < 60; i++) {
            m.in[0].direction = 1;
            m.step();
        }
        assertEquals("the SHINKAI clocks wait", a, m.first.shinkaiFrames);
        assertEquals(b, m.cpu.shinkaiFrames);
        assertEquals("the stick does not move him: the choreography does", c(m).coreo.x[0], m.f(0).x, 0.01f);
        assertFalse(m.f(0).attacking());
    }

    private static ShinkaiClash c(Match m) { return m.engine.shinkaiClash(); }

    @Test public void theClashTravelsThroughTheGroundTheAirAndTheSpeedScenes() {
        Match m = new Match("p13", "p16");
        ShinkaiClash c = m.clash();
        java.util.Set<CoreografiaClash.Cena> cenas = java.util.EnumSet.noneOf(CoreografiaClash.Cena.class);
        boolean noAr = false, sumiu = false;
        int choques = 0;
        for (int i = 0; i < ShinkaiClash.DURACAO - 2 && m.engine.shinkaiClash() != null; i++) {
            m.step();
            cenas.add(c.coreo.cena);
            noAr |= !m.f(0).grounded && !m.f(1).grounded;
            sumiu |= !c.coreo.visivel[0] && !c.coreo.visivel[1];
            if (c.coreo.choque && c.coreo.cena != CoreografiaClash.Cena.CHOQUE) choques++;
        }
        assertEquals("ground clash, air exchange and speed duel", 3, cenas.size());
        assertTrue("they fight in the air", noAr);
        assertTrue("they vanish in the speed duel", sumiu);
        assertTrue("blows burst in the air: " + choques, choques >= 8);
    }

    @Test public void endingInTheAirTheyFallBackToTheGround() {
        Match m = new Match("p13", "p16");
        ShinkaiClash c = m.clash();
        // Ninguém empurra: termina empatado no fim do tempo (o choque final é no chão), e se acabar
        // no ar os dois caem.
        while (m.engine.shinkaiClash() != null && c.coreo.cena != CoreografiaClash.Cena.AR) m.step();
        c.marca = ShinkaiClash.META - 1;
        m.in[0].heavy = true;
        m.steps(2);
        assertNull(m.engine.shinkaiClash());
        m.steps(120);
        assertTrue("both land", m.f(0).grounded && m.f(1).grounded);
    }

    @Test public void theOneWhoPushesMoreWinsHitsHardAndKeepsItsShinkai() {
        Match m = new Match("p16", "p13");
        ShinkaiClash c = m.clash();
        int life = m.cpu.life;
        for (int i = 0; i < 400 && m.engine.shinkaiClash() != null; i++) {
            if (i % 4 == 0) m.in[0].light = true;
            m.step();
        }
        assertNull(m.engine.shinkaiClash());
        assertEquals(ShinkaiClash.Fim.VITORIA, c.fim);
        assertEquals(0, c.lado);
        assertTrue(m.engine.cues().contains("SHINKAI_CLASH_WIN"));
        assertEquals(life - m.engine.config.shinkaiClashDamage, m.cpu.life);
        assertEquals("the loser loses its SHINKAI", 0, m.cpu.shinkaiFrames);
        assertEquals("the winner's is full again", m.engine.config.shinkaiFrames, m.first.shinkaiFrames);
        assertEquals(CombatFighter.Status.AIR_HITSTUN, m.f(1).status);
        assertTrue("thrown away", m.f(1).vx > 0f);
    }

    @Test public void holdingBackGivesUpWithoutDamageButLosesTheShinkai() {
        Match m = new Match("p13", "p16");
        ShinkaiClash c = m.clash();
        int life = m.first.life;
        for (int i = 0; i < ShinkaiClash.DESISTIR + 2 && m.engine.shinkaiClash() != null; i++) {
            m.in[0].direction = 5;   // trás (o P1 olha para a direita)
            m.step();
        }
        assertEquals(ShinkaiClash.Fim.DESISTENCIA, c.fim);
        assertEquals(0, c.lado);
        assertEquals(life, m.first.life);
        assertEquals(0, m.first.shinkaiFrames);
        assertTrue("the other keeps its SHINKAI", m.cpu.shinkaiFrames > 0);
    }

    @Test public void nobodyPushingIsATieAndBothKeepTheirShinkai() {
        Match m = new Match("p13", "p16");
        ShinkaiClash c = m.clash();
        m.steps(ShinkaiClash.DURACAO + 2);
        assertEquals(ShinkaiClash.Fim.EMPATE, c.fim);
        assertTrue(m.first.shinkaiFrames > 0 && m.cpu.shinkaiFrames > 0);
        assertEquals(m.first.profile.maxLife, m.first.life);
    }

    // ------------------------------------------------------- cada tipo empurra de um jeito

    private static ShinkaiClash puro(Shinkai.Type a, Shinkai.Type b) {
        return new ShinkaiClash(a, b, 0, 1, -1);
    }

    @Test public void powerPushesHardWithHeavyButMustBreathe() {
        ShinkaiClash c = puro(Shinkai.Type.POWER, Shinkai.Type.POWER);
        FighterInput h = new FighterInput(), nada = new FighterInput();
        h.heavy = true;
        c.passo(h, nada);
        int um = c.marca;
        assertTrue(um > 40);
        c.passo(h, nada);
        assertEquals("waiting after the heavy push", um, c.marca);
        FighterInput l = new FighterInput();
        ShinkaiClash fraco = puro(Shinkai.Type.POWER, Shinkai.Type.POWER);
        l.light = true;
        fraco.passo(l, nada);
        assertTrue("the light pushes little", fraco.marca < um / 2);
    }

    @Test public void speedPushesWellWithFastLights() {
        ShinkaiClash c = puro(Shinkai.Type.SPEED, Shinkai.Type.POWER);
        FighterInput l = new FighterInput(), nada = new FighterInput();
        for (int i = 0; i < 40; i++) {
            l.light = i % 4 == 0;
            c.passo(l, nada);
        }
        assertTrue(c.marca >= 10 * 30);
    }

    @Test public void energyChargesWhileHoldingAndPushesOnRelease() {
        ShinkaiClash c = puro(Shinkai.Type.ENERGY, Shinkai.Type.POWER);
        FighterInput in = new FighterInput(), nada = new FighterInput();
        in.holding = true;
        for (int i = 0; i < 60; i++) c.passo(in, nada);
        assertEquals("nothing while holding", 0, c.marca);
        assertEquals(60, c.carga[0]);
        in.holding = false;
        c.passo(in, nada);
        assertEquals(60 * 60 / 16, c.marca);
        ShinkaiClash curto = puro(Shinkai.Type.ENERGY, Shinkai.Type.POWER);
        in.holding = true;
        for (int i = 0; i < 20; i++) curto.passo(in, nada);
        in.holding = false;
        curto.passo(in, nada);
        assertTrue("a long charge is worth much more than three short ones", 3 * curto.marca < c.marca);
    }

    @Test public void techniquePushesOnTheBeat() {
        ShinkaiClash c = puro(Shinkai.Type.TECHNIQUE, Shinkai.Type.POWER);
        FighterInput in = new FighterInput(), nada = new FighterInput();
        // Fora do pulso (depois do pulso anterior, antes do meio): quase nada.
        while (c.fase() != 5) c.passo(nada, nada);
        in.medium = true;
        c.passo(in, nada);
        int fora = c.marca;
        assertTrue(fora > 0 && fora < 20);
        // No pulso: muito.
        while (c.fase() != ShinkaiClash.BATIDA - 1) c.passo(nada, nada);
        c.passo(in, nada);
        assertTrue("on the beat: " + (c.marca - fora), c.marca - fora >= 100);
        // De novo no mesmo pulso: já foi.
        int depois = c.marca;
        c.passo(in, nada);
        assertTrue(c.marca - depois < 20);
    }

    @Test public void theStrongCpuWinsAgainstAnIdlePlayer() {
        Match m = new Match("p13", "p16");
        ShinkaiClash c = m.clash();
        AiController ai = new AiController(new OpponentAi(new java.util.Random(3)), new java.util.Random(5));
        ai.setEnabled(true);
        ai.setLevel(8);
        for (int i = 0; i < 400 && m.engine.shinkaiClash() != null; i++) {
            ai.fill(m.engine, 1, m.in[1]);
            m.step();
        }
        assertEquals(ShinkaiClash.Fim.VITORIA, c.fim);
        assertEquals(1, c.lado);
    }

    /** Esmurrar o botão não acha o ritmo: só o primeiro toque de cada pulso vale. */
    @Test public void techniqueMashingLosesToTheRhythm() {
        ShinkaiClash mash = puro(Shinkai.Type.TECHNIQUE, Shinkai.Type.POWER);
        ShinkaiClash ritmo = puro(Shinkai.Type.TECHNIQUE, Shinkai.Type.POWER);
        FighterInput a = new FighterInput(), b = new FighterInput(), nada = new FighterInput();
        for (int i = 0; i < 240; i++) {
            a.medium = i % 3 == 0;
            mash.passo(a, nada);
            b.medium = (ritmo.quadro + 1 - ritmo.entrada) % ShinkaiClash.BATIDA == 0;
            ritmo.passo(b, nada);
        }
        assertTrue("no ritmo " + ritmo.marca + " > esmurrando " + mash.marca, ritmo.marca > 2 * mash.marca);
    }

    /** ENERGY: o toque curto de tela (o dedo fica uns quadros no botão) empurra, menos que a carga cheia. */
    @Test public void energyShortTouchPushesButLessThanTheFullCharge() {
        ShinkaiClash c = puro(Shinkai.Type.ENERGY, Shinkai.Type.POWER);
        FighterInput in = new FighterInput(), nada = new FighterInput();
        int toques = 0;
        for (int i = 0; i < 71; i++) {
            in.clear();
            int t = i % 8;
            if (t == 0) in.medium = true;
            in.holding = t < 3;
            c.passo(in, nada);
            if (t == 0) toques++;
        }
        assertTrue("o toque empurra: " + c.marca, c.marca >= toques * ShinkaiClash.TOQUE_ENERGY - ShinkaiClash.TOQUE_ENERGY);
        ShinkaiClash cheia = puro(Shinkai.Type.ENERGY, Shinkai.Type.POWER);
        in.clear();
        in.holding = true;
        for (int i = 0; i < ShinkaiClash.CARGA_MAX; i++) cheia.passo(in, nada);
        in.holding = false;
        cheia.passo(in, nada);
        assertTrue("segurar e soltar rende mais no mesmo tempo: " + cheia.marca + " > " + c.marca, cheia.marca > c.marca);
    }

    /**
     * A CPU empurra na mesma média em todos os tipos (o tipo não decide o confronto): a soma dos
     * empurrões de cada tipo na disputa inteira, com a CPU dos dois lados.
     */
    @Test public void theCpuPushesTheSameOnAverageWithEveryType() {
        String[] porTipo = {"p13", "p16", "p08", "p02"};   // POWER, SPEED, ENERGY, TECHNIQUE
        double[] media = new double[porTipo.length];
        for (int t = 0; t < porTipo.length; t++) {
            long soma = 0, quadros = 0;
            for (int seed = 0; seed < 12; seed++) {
                Match m = new Match(porTipo[t], porTipo[t]);
                ShinkaiClash c = m.clash();
                AiController[] ai = new AiController[2];
                for (int k = 0; k < 2; k++) {
                    ai[k] = new AiController(new OpponentAi(new java.util.Random(seed * 2 + k)), new java.util.Random(seed * 7L + k));
                    ai[k].setEnabled(true);
                    ai[k].setLevel(8);
                }
                while (!c.acabou()) {
                    ai[0].fill(m.engine, 0, m.in[0]);
                    ai[1].fill(m.engine, 1, m.in[1]);
                    m.step();
                    for (int k = 0; k < 2; k++) if (c.idadeEmpurrao[k] == 0) soma += c.ultimoEmpurrao[k];
                    quadros += 2;
                }
            }
            media[t] = soma / (double)quadros;
        }
        double min = Math.min(Math.min(media[0], media[1]), Math.min(media[2], media[3]));
        double max = Math.max(Math.max(media[0], media[1]), Math.max(media[2], media[3]));
        assertTrue("empurrão por quadro POWER/SPEED/ENERGY/TECHNIQUE: " + java.util.Arrays.toString(media), max < 1.15 * min);
    }
}
