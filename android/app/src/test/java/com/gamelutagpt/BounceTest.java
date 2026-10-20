package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Wall bounce and ground bounce (v0.83). */
public class BounceTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Sim {
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Sim(float xa, float xb) {
            this(xa, xb, new CombatConfig());
        }

        Sim(float xa, float xb, CombatConfig config) {
            engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"), xa, xb, config);
        }

        CombatFighter f(int i) { return engine.fighter(i); }
        CombatConfig config() { return engine.config; }
        String attackId(int i) { return f(i).attacking() ? f(i).attack.id : ""; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        boolean cue(String id) { return engine.cues().contains(id); }

        /** L > M > H chain up close; returns once H has hit. */
        void chainToHeavy() {
            in[0].light = true;
            for (int i = 0; i < 120; i++) {
                if (attackId(0).equals("L")) in[0].medium = true;
                if (attackId(0).equals("M")) in[0].heavy = true;
                step();
                for (CombatEngine.HitEvent e : engine.events()) if ("H".equals(e.moveId)) return;
            }
            fail("H never hit");
        }

        /**
         * Both in the air, the defender in air hitstun right in front and a little below (the
         * stomp comes from above: the jump falls faster than a body in hitstun); the attacker
         * swings ↓ + jH (j2H).
         */
        void airHeavy() {
            for (int i = 0; i < 2; i++) {
                f(i).grounded = false;
                f(i).y = Arena.GROUND_Y - (i == 0 ? 200f : 160f);
                f(i).vy = 0f;
            }
            f(1).status = CombatFighter.Status.AIR_HITSTUN;
            f(1).stunLeft = f(1).stunTotal = 60;
            in[0].direction = 3;
            in[0].heavy = true;
            step();
            assertEquals("j2H", attackId(0));
        }
    }

    @Test public void heavyInAComboBouncesOffTheWallAndCanBeFollowedUp() {
        Sim s = new Sim(500f, 600f);
        s.chainToHeavy();
        CombatFighter d = s.f(1);
        assertEquals(CombatFighter.Status.AIR_HITSTUN, d.status);
        assertEquals(CombatFighter.BOUNCE_WALL, d.bounce);
        assertEquals(1, s.engine.session(1).wallBounces);
        float wall = d.bounceWallX;
        assertEquals("the screen-edge wall", s.f(0).x + s.config().wallBounceDistance, wall, 40f);
        boolean bounced = false;
        for (int i = 0; i < 80 && !bounced; i++) {
            s.step();
            bounced = s.cue("WALL_BOUNCE");
        }
        assertTrue(bounced);
        assertEquals(wall, d.x, 0.5f);
        assertTrue("pops up", d.vy < 0f);
        assertTrue("comes back toward the attacker", d.vx < 0f);
        assertTrue("still in the combo", s.engine.session(1) != null && d.inHitstun());
        for (int i = 0; i < 200 && d.status != CombatFighter.Status.KNOCKDOWN; i++) s.step();
        assertEquals("no air recovery after a bounce: it lands knocked down", CombatFighter.Status.KNOCKDOWN, d.status);
    }

    @Test public void theWallBounceRouteFitsAFullAirStringAfterTheLauncher() {
        // L > M > H na parede, corre, 2H, super pulo, jL > jM > jH: o ar inteiro entra (o H que
        // acerta no chão não gasta juggle; com limite de 6 pontos, a rota ainda caberia).
        Sim s = new Sim(500f, 600f);
        s.chainToHeavy();
        assertEquals("o H da parede é de graça", 0, s.engine.session(1).juggleCount);
        s.in[0].direction = 1;
        boolean lancou = false;
        for (int i = 0; i < 120 && !lancou; i++) {
            if (Math.abs(s.f(1).x - s.f(0).x) < 150f && !s.f(0).attacking() && s.f(0).grounded) {
                s.in[0].direction = 3;
                s.in[0].heavy = true;
            }
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if ("2H".equals(e.moveId)) lancou = true;
        }
        assertTrue("o 2H levantou", lancou);
        for (int i = 0; i < 40 && s.f(0).grounded; i++) {
            s.in[0].direction = i < 3 ? 0 : 7;
            s.step();
        }
        s.in[0].direction = 0;
        assertTrue("super pulo", s.f(0).superJumping);
        java.util.List<String> noAr = new java.util.ArrayList<>();
        String[] seq = {"L", "M", "H"};
        boolean pede = true;
        for (int i = 0; i < 200 && noAr.size() < seq.length && !s.f(0).grounded; i++) {
            String b = seq[noAr.size()];
            if (pede) {
                if (b.equals("L")) s.in[0].light = true; else if (b.equals("M")) s.in[0].medium = true; else s.in[0].heavy = true;
            }
            s.step();
            boolean acertou = false;
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) { noAr.add(e.moveId); acertou = true; }
            pede = acertou || !s.f(0).attacking();
        }
        assertEquals(java.util.Arrays.asList("jL", "jM", "jH"), noAr);
        assertEquals("2H + jL + jM + jH", 6, s.engine.session(1) != null
            ? s.engine.session(1).juggleCount : s.engine.lastSession(1).juggleCount);
    }

    @Test public void withoutAJuggleLimitTheAirComboStillEnds() {
        // Sem limite de juggle: L > M > 2H, super pulo e jL > jM > pulo no ar até não dar mais.
        // Passa dos 6 pontos de antes, mas acaba (os pulos no ar acabam e o rival cai).
        Sim s = new Sim(500f, 590f);
        s.f(1).state.life = Integer.MAX_VALUE / 4;
        s.in[0].light = true;
        boolean lancou = false;
        for (int i = 0; i < 90 && !lancou; i++) {
            if (s.attackId(0).equals("L")) s.in[0].medium = true;
            if (s.attackId(0).equals("M")) { s.in[0].direction = 3; s.in[0].heavy = true; }
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if ("2H".equals(e.moveId)) lancou = true;
        }
        assertTrue(lancou);
        for (int i = 0; i < 30 && s.f(0).grounded; i++) {
            s.in[0].direction = i < 2 ? 0 : 7;
            s.step();
        }
        s.in[0].direction = 0;
        int k = 0, maior = 0;
        boolean pede = true;
        for (int i = 0; i < 600 && !s.f(0).grounded; i++) {
            int fase = k % 3;
            if (pede) {
                if (fase == 0) s.in[0].light = true; else if (fase == 1) s.in[0].medium = true; else s.in[0].direction = 7;
            }
            s.step();
            s.in[0].direction = 0;
            boolean acertou = false;
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) acertou = true;
            if (fase == 2) {
                if (s.f(0).vy < 0f && !s.f(0).attacking()) k++;
                pede = true;
            } else {
                if (acertou) k++;
                pede = acertou || !s.f(0).attacking();
            }
            if (s.engine.session(1) != null) maior = Math.max(maior, s.engine.session(1).hitCount);
        }
        ComboSession fim = s.engine.lastSession(1) != null ? s.engine.lastSession(1) : s.engine.session(1);
        assertTrue("passa dos 6 pontos de juggle de antes: " + fim.juggleCount, fim.juggleCount > 6);
        for (int i = 0; i < 240 && s.engine.session(1) != null; i++) s.step();
        assertNull("o combo acaba", s.engine.session(1));
        assertTrue("e não vira loop: " + maior, maior <= 12);
    }

    @Test public void aLoneHeavyInNeutralIsAPlainHit() {
        Sim s = new Sim(500f, 600f);
        s.in[0].heavy = true;
        for (int i = 0; i < 40 && s.f(1).framesSinceHit > 0; i++) s.step();
        assertEquals(CombatFighter.BOUNCE_NONE, s.f(1).bounce);
        assertEquals(CombatFighter.Status.HITSTUN, s.f(1).status);
    }

    @Test public void oneWallBouncePerCombo() {
        Sim s = new Sim(500f, 600f);
        s.in[0].light = true;
        for (int i = 0; i < 120; i++) {
            if (s.engine.session(1) != null) s.engine.session(1).wallBounces = s.config().maxWallBounces;
            if (s.attackId(0).equals("L")) s.in[0].medium = true;
            if (s.attackId(0).equals("M")) s.in[0].heavy = true;
            s.step();
            boolean heavy = false;
            for (CombatEngine.HitEvent e : s.engine.events()) heavy |= "H".equals(e.moveId);
            if (heavy) break;
        }
        assertEquals(CombatFighter.BOUNCE_NONE, s.f(1).bounce);
        assertEquals(0f, s.f(1).vx, 0f);
    }

    @Test public void airHeavyDrivesTheDefenderIntoTheFloorAndItBouncesUp() {
        Sim s = new Sim(600f, 690f);
        s.airHeavy();
        CombatFighter d = s.f(1);
        for (int i = 0; i < 30 && d.bounce != CombatFighter.BOUNCE_GROUND; i++) s.step();
        assertEquals(CombatFighter.BOUNCE_GROUND, d.bounce);
        assertTrue("driven down", d.vy > 0f);
        boolean bounced = false;
        for (int i = 0; i < 60 && !bounced; i++) {
            s.step();
            bounced = s.cue("GROUND_BOUNCE");
        }
        assertTrue(bounced);
        assertFalse("back in the air", d.grounded);
        assertTrue(d.vy < 0f);
        for (int i = 0; i < 200 && d.status != CombatFighter.Status.KNOCKDOWN; i++) s.step();
        assertEquals(CombatFighter.Status.KNOCKDOWN, d.status);
    }

    @Test public void groundBounceSpentFromASuperJumpEndsLikeTheSlam() {
        CombatConfig config = new CombatConfig();
        config.maxGroundBounces = 0;            // as if this combo already used it
        Sim s = new Sim(600f, 690f, config);
        s.f(0).superJumping = true;
        s.airHeavy();
        CombatFighter d = s.f(1);
        for (int i = 0; i < 30 && d.framesSinceHit > 0; i++) s.step();
        assertEquals(CombatFighter.BOUNCE_NONE, d.bounce);
        assertTrue("slammed", d.slammed);
    }

    @Test public void spentGroundBounceWithoutSuperJumpIsAPlainAirHit() {
        CombatConfig config = new CombatConfig();
        config.maxGroundBounces = 0;
        Sim s = new Sim(600f, 690f, config);
        s.airHeavy();
        CombatFighter d = s.f(1);
        for (int i = 0; i < 30 && d.framesSinceHit > 0; i++) s.step();
        assertEquals(CombatFighter.BOUNCE_NONE, d.bounce);
        assertFalse(d.slammed);
        assertEquals(CombatFighter.Status.AIR_HITSTUN, d.status);
    }

    /** Segura → depois do H que bateu na parede: sai correndo sem o → → (como o 2H dá o super pulo). */
    @Test public void holdingForwardAfterAWallBounceRunsWithoutTheDoubleTap() {
        Sim s = new Sim(500f, 600f);
        s.chainToHeavy();
        assertEquals(CombatFighter.BOUNCE_WALL, s.f(1).bounce);
        s.in[0].direction = 1;
        boolean correu = false;
        for (int i = 0; i < 60 && !correu; i++) {
            s.step();
            correu = s.f(0).forwardDashing;
        }
        assertTrue("segurando → depois da parede, corre", correu);
        assertEquals("a janela foi usada", 0, s.f(0).wallChase);
    }

    @Test public void holdingForwardInNeutralOnlyWalks() {
        Sim s = new Sim(500f, 900f);
        s.in[0].direction = 1;
        for (int i = 0; i < 40; i++) s.step();
        assertFalse("sem parede no combo, segurar → só anda", s.f(0).forwardDashing);
    }

    /**
     * O 2H que pega o adversário no ar (voltando da parede) não lança mais alto que um 2H do
     * chão: senão ele sai na frente e o super pulo não alcança para o ↓ + jH.
     */
    @Test public void launcherOnAnAirborneDefenderPeaksAtTheGroundLaunchHeight() {
        Sim s = new Sim(500f, 580f);
        CombatFighter d = s.f(1);
        d.grounded = false;
        d.y = Arena.GROUND_Y - 160f;
        d.vy = 0f;
        d.status = CombatFighter.Status.AIR_HITSTUN;
        d.stunLeft = d.stunTotal = 60;
        s.in[0].direction = 3;
        s.in[0].heavy = true;
        s.step();
        for (int i = 0; i < 40 && !d.launched; i++) s.step();
        assertTrue("lançou", d.launched);
        float topo = 0f;
        for (int i = 0; i < 120 && !d.grounded; i++) {
            topo = Math.max(topo, Arena.GROUND_Y - d.y);
            s.step();
        }
        CombatConfig c = s.config();
        float doChao = c.launchSpeed * c.launchSpeed / (2f * c.gravity);
        assertEquals("o topo é o de um lançamento do chão", doChao, topo, 30f);
    }
}
