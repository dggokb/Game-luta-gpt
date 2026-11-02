package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** A CPU usa os especiais S2–S4 pela função que os dados dão a cada um (CpuSpecials). */
public class CpuSpecialsTest {
    private static CpuSpecials.Role role(String id, String move) {
        for (CpuSpecials.Option o : CpuSpecials.of(GeneratedCharacters.get(id))) {
            if (o.id.equals(move)) return o.role;
        }
        return null;
    }

    @Test
    public void theRoleOfEachSpecialComesFromItsData() {
        assertEquals(CpuSpecials.Role.REPEL, role("p15", "S2"));
        assertEquals(CpuSpecials.Role.COUNTER, role("p02", "S3"));
        assertEquals(CpuSpecials.Role.COUNTER, role("p11", "S3"));
        assertEquals(CpuSpecials.Role.EVADE, role("p04", "S3"));
        assertEquals(CpuSpecials.Role.EVADE, role("p10", "S3"));
        assertEquals(CpuSpecials.Role.GRAB, role("p13", "S3"));
        assertEquals(CpuSpecials.Role.TRAP, role("p16", "S2"));
        assertEquals(CpuSpecials.Role.PROJECTILE, role("p05", "S2"));
        assertEquals(CpuSpecials.Role.PROJECTILE, role("p11", "S2"));
        assertEquals(CpuSpecials.Role.ARMOR, role("p09", "S3"));
        assertEquals(CpuSpecials.Role.DIVE, role("p03", "S4"));
        assertEquals(CpuSpecials.Role.DIVE, role("p14", "S"));
        assertEquals(CpuSpecials.Role.ECHO, role("p06", "S2"));
        assertEquals(CpuSpecials.Role.PULL, role("p06", "S3"));
        assertEquals(CpuSpecials.Role.ANTI_AIR, role("player_base", "S2"));
        assertEquals(CpuSpecials.Role.LOW, role("p07", "S4"));
    }

    /** Especiais S2–S4 começados pelo lado 0 em lutas CPU × CPU no nível dado. */
    private static Map<String, Integer> specialsUsed(String self, String rival, int level, int fights) {
        Map<String, Integer> used = new HashMap<>();
        for (int i = 0; i < fights; i++) {
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {self}, new String[] {rival}, level, 500 + i,
                new CombatConfig());
            AttackDefinition before = null;
            while (!l.acabou() && l.quadro < 60 * 60 * 2) {
                l.passo();
                CombatFighter f = l.f(0);
                AttackDefinition now = f.attacking() ? f.attack : null;
                if (now != null && now != before && now.id.matches("S[234]")) used.merge(now.id, 1, Integer::sum);
                before = now;
            }
        }
        return used;
    }

    private static int total(Map<String, Integer> used) {
        int n = 0;
        for (int v : used.values()) n += v;
        return n;
    }

    @Test
    public void theCpuUsesTheCommandSpecials() {
        Map<String, Integer> p05 = specialsUsed("p05", "p11", 8, 6);
        assertTrue("p05 solta a shuriken (S2): " + p05, p05.getOrDefault("S2", 0) > 0);
        assertTrue("p05 entra com o corte (S4): " + p05, p05.getOrDefault("S4", 0) > 0);
        // A esquiva contra a bola é um dos planos que ela aprende (AiController.escolherPlanoBola):
        // contra o p11, que solta pouca bola, ela aprende a só defender; contra quem solta bola
        // de verdade a esquiva-sumiço aparece.
        Map<String, Integer> contraBola = specialsUsed("p05", "player_base", 8, 6);
        assertTrue("p05 usa a esquiva-sumiço (S3): " + contraBola, contraBola.getOrDefault("S3", 0) > 0);
        // A Onda do Ryūga aparece menos nas lutas em que ele se transforma (o SHINKAI é uma vida
        // nova e a luta fica perto dele): mais lutas para ver o repel.
        Map<String, Integer> p15 = specialsUsed("p15", "player_base", 8, 14);
        assertTrue("p15 repele a onda (S2): " + p15, p15.getOrDefault("S2", 0) > 0);
    }

    @Test
    public void lowLevelsUseFewerSpecials() {
        int low = total(specialsUsed("p05", "p11", 1, 6));
        int high = total(specialsUsed("p05", "p11", 8, 6));
        assertTrue("nível 1: " + low + ", nível 8: " + high, low * 2 < high);
    }

    @Test
    public void theRepelUndoesTheBallItWaitedFor() {
        int repels = 0;
        for (int i = 0; i < 6; i++) {
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {"p15"}, new String[] {"player_base"}, 8, 700 + i,
                new CombatConfig());
            while (!l.acabou() && l.quadro < 60 * 60 * 2) {
                l.passo();
                if (l.e.cues().contains("REPEL")) repels++;
            }
        }
        assertTrue("repels: " + repels, repels > 0);
    }

    @Test
    public void theRepelSendsTheBallBackAtTheThrower() {
        CharacterDefinition p15 = GeneratedCharacters.get("p15"), base = GeneratedCharacters.get("player_base");
        CombatEngine e = new CombatEngine(new FighterState(p15, "P1"), new FighterState(base, "P2"), 500f, 900f, new CombatConfig());
        CombatFighter natisuko = e.fighter(0), ryuga = e.fighter(1);
        natisuko.status = CombatFighter.Status.ATTACK;
        natisuko.attack = p15.attack("S2");
        natisuko.attackFrame = 5;   // a aura no ar
        AttackDefinition onda = base.fighter.energy.attack;
        e.energyProjectiles.add(new Projectile(620f, Arena.GROUND_Y - 160f, 900f, 760f, 24f, 850, 0, 1, -1, onda));
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        boolean repeliu = false, voltou = false, acertouEle = false;
        int vida = natisuko.state.life;
        for (int i = 0; i < 120 && !acertouEle; i++) {
            e.step(in0, in1);
            if (e.cues().contains("REPEL")) repeliu = true;
            for (Projectile p : e.energyProjectiles) if (p.ownerIndex == 0 && p.direction > 0) voltou = true;
            for (CombatEngine.HitEvent ev : e.events()) if (ev.attacker == 0 && ev.projectile && !ev.blocked) acertouEle = true;
        }
        assertTrue("a aura repeliu", repeliu);
        assertTrue("a bola voltou, agora dela", voltou);
        assertTrue("e acertou quem soltou", acertouEle);
        assertEquals("ela não tomou dano", vida, natisuko.state.life);
    }

    @Test
    public void aChargeSpecialNeedsTheCharge() {
        // Sem segurar trás o tempo da carga, o [←] → não sai (para a CPU também).
        CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.get("p09"), "P1"),
            new FighterState(GeneratedCharacters.get("player_base"), "P2"), 400f, 560f, new CombatConfig());
        AiController cpu = new AiController(new OpponentAi(new java.util.Random(3)), new java.util.Random(4));
        cpu.setEnabled(true);
        cpu.setFullKit(true);
        cpu.setLevel(8);
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        for (int i = 0; i < 60 * 20; i++) {
            cpu.fill(e, 0, in0);
            CombatFighter me = e.fighter(0);
            if ("S4".equals(in0.special)) {
                assertTrue("S4 sem carga no quadro " + i,
                    me.motion.charged(false, me.clock, e.config.chargeFrames, e.config.chargeKeepFrames));
            }
            in1.clear();
            e.step(in0, in1);
        }
    }
}
