package com.gamelutagpt;

import java.util.*;

/**
 * Segurança dos assists (frente H5, item 6): o assist chamado sozinho e defendido, a punição que o
 * titular leva depois de chamar, o assist com o rival cruzando por cima e o uso no canto.
 */
public class SondaAssist {
    static final String[] ASSISTS = {"p07", "p12", "p13", "p14", "p15", "player_base", "p08", "p04"};
    static final String POINT = "p02";

    static CombatEngine engine(String partner, String rival, float x0, float x1) {
        FighterState a = new FighterState(GeneratedCharacters.get(partner.equals(POINT) ? "player_base" : POINT), "P1");
        FighterState p = new FighterState(GeneratedCharacters.get(partner), "P1");
        FighterState d = new FighterState(GeneratedCharacters.get(rival), "P2");
        CombatEngine e = new CombatEngine(a, d, x0, x1, new CombatConfig());
        e.setTeam(0, a, p);
        return e;
    }

    /** O rival só defende em pé (segura para trás): o que o assist faz e a vantagem quando o rival se solta. */
    static String blocked(String partner, float gap) {
        CombatEngine e = engine(partner, "player_base", 900f, 900f + gap);
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        in[0].assist = true;
        int hit = 0, block = 0, lastContact = -1, rivalFree = -1, assistGone = -1;
        for (int t = 0; t < 200; t++) {
            CombatFighter d = e.fighter(1);
            in[1].direction = e.fighter(0).x >= d.x ? 4 : 2; // agachado para trás (como BateriaBalanco.defender)
            e.step(in[0], in[1]);
            in[0].clearPresses();
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0) { if (h.blocked) block++; else hit++; lastContact = t; }
            if (assistGone < 0 && e.team(0).assist == null && t > 2) assistGone = t;
            if (lastContact == t) rivalFree = -1;
            if (lastContact >= 0 && rivalFree < 0 && e.fighter(1).canAct()) rivalFree = t;
        }
        if (lastContact < 0) return "não toca";
        // Vantagem: quadros entre o rival se soltar e o assist sair da tela (o titular está livre o tempo todo;
        // positivo = o rival se solta com o assist já fora, nada a punir no assist; o titular só leva se for alcançado).
        return String.format(Locale.ROOT, "%s, rival preso +%d q", hit > 0 ? "acertou" : "defendido", Math.max(0, rivalFree - lastContact));
    }

    /** Titular chama o assist a {@code gap} e passa a defender; o rival é a CPU nível 8. Acertos limpos no titular em 2 s. */
    static int[] punished(String partner, float gap, boolean call, int seeds) {
        int punishedFights = 0, hitsTotal = 0;
        for (int s = 1; s <= seeds; s++) {
            CombatEngine e = engine(partner, "player_base", 900f, 900f + gap);
            AiController ai = new AiController(new OpponentAi(new Random(s)), new Random(s * 31L + 7));
            ai.setEnabled(true);
            ai.setFullKit(true);
            ai.setLevel(8);
            FighterInput[] in = {new FighterInput(), new FighterInput()};
            in[0].assist = call;
            int hits = 0;
            for (int t = 0; t < 120; t++) {
                CombatFighter me = e.fighter(0);
                CombatFighter ri = e.fighter(1);
                boolean alto = !ri.grounded || ri.attacking() && ri.attack != null && ri.attack.overhead;
                boolean direita = ri.x >= me.x;
                in[0].direction = alto ? (direita ? 5 : 1) : (direita ? 4 : 2); // BateriaBalanco.defender
                in[1].clearPresses();
                ai.fill(e, 1, in[1]);
                e.step(in[0], in[1]);
                in[0].clearPresses();
                for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 1 && !h.blocked && h.defender == 0) hits++;
            }
            if (hits > 0) punishedFights++;
            hitsTotal += hits;
        }
        return new int[] {punishedFights, hitsTotal};
    }

    /** O rival pula por cima do titular (cruzado) e o titular chama o assist no meio do pulo. */
    static String crossed(String partner, int callAt) {
        CombatEngine e = engine(partner, "player_base", 900f, 1060f);
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        int hit = 0, wrong = 0;
        boolean called = false;
        float assistStart = Float.NaN;
        for (int t = 0; t < 200; t++) {
            CombatFighter d = e.fighter(1);
            if (t == 0) { in[1].jump = true; in[1].direction = d.facing > 0 ? 9 : 7; }
            else in[1].direction = 5;
            if (t == callAt) { in[0].assist = true; called = true; }
            e.step(in[0], in[1]);
            in[0].clearPresses(); in[1].clearPresses();
            TeamSystem.Side side = e.team(0);
            if (side.assist != null && Float.isNaN(assistStart)) assistStart = side.assist.x;
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0) hit++;
        }
        return hit > 0 ? "acerta" : "erra";
    }

    /** Canto: titular de costas para a parede (rival a 250) e titular encurralando o rival na parede. */
    static String corner(String partner, boolean pointInCorner) {
        float x0 = pointInCorner ? Arena.LEFT_BOUND + 20f : Arena.RIGHT_BOUND - 270f;
        float x1 = pointInCorner ? x0 + 250f : Arena.RIGHT_BOUND - 20f;
        CombatEngine e = engine(partner, "player_base", x0, x1);
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        in[0].assist = true;
        int hit = 0;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minGap = Float.MAX_VALUE;
        for (int t = 0; t < 200; t++) {
            e.step(in[0], in[1]);
            in[0].clearPresses();
            TeamSystem.Side side = e.team(0);
            if (side.assist != null && side.assistPhase == TeamSystem.ASSIST_ATTACK) {
                minX = Math.min(minX, side.assist.x);
                maxX = Math.max(maxX, side.assist.x);
            }
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0) hit++;
        }
        boolean inside = minX >= Arena.LEFT_BOUND - 1f && maxX <= Arena.RIGHT_BOUND + 1f;
        return (hit > 0 ? "acerta" : "erra") + (inside ? "" : " (golpe fora da arena)");
    }

    public static void main(String[] args) {
        int seeds = 20;
        System.out.println("#### Assist sozinho, rival defendendo em pé (titular " + POINT + ")");
        System.out.println();
        System.out.println("| assist | 150 px | 250 px | 350 px | 450 px |");
        System.out.println("|---|---|---|---|---|");
        for (String a : ASSISTS) {
            StringBuilder b = new StringBuilder("| " + a);
            for (float g : new float[] {150f, 250f, 350f, 450f}) b.append(" | ").append(blocked(a, g));
            System.out.println(b.append(" |"));
        }
        System.out.println();
        if (args.length > 0) return;
        System.out.println("#### Punição no titular: chama o assist e defende; rival CPU nível 8, " + seeds + " sementes, 2 s");
        System.out.println();
        System.out.println("| assist | 150 px | 250 px | 350 px |");
        System.out.println("|---|---|---|---|");
        StringBuilder ctrl = new StringBuilder("| sem chamar (controle)");
        for (float g : new float[] {150f, 250f, 350f}) {
            int[] r = punished("p07", g, false, seeds);
            ctrl.append(String.format(Locale.ROOT, " | %d de %d lutas (%d acertos)", r[0], seeds, r[1]));
        }
        System.out.println(ctrl.append(" |"));
        for (String a : ASSISTS) {
            StringBuilder b = new StringBuilder("| " + a);
            for (float g : new float[] {150f, 250f, 350f}) {
                int[] r = punished(a, g, true, seeds);
                b.append(String.format(Locale.ROOT, " | %d de %d lutas (%d acertos)", r[0], seeds, r[1]));
            }
            System.out.println(b.append(" |"));
        }
        System.out.println();
        System.out.println("#### Rival cruzando por cima (pula de 160 px; assist chamado no quadro k do pulo) e canto");
        System.out.println();
        System.out.println("| assist | k = 5 | k = 15 | k = 25 | k = 35 | titular no canto, rival a 250 | rival no canto, titular a 250 |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (String a : ASSISTS) {
            System.out.printf(Locale.ROOT, "| %s | %s | %s | %s | %s | %s | %s |%n", a, crossed(a, 5), crossed(a, 15), crossed(a, 25), crossed(a, 35),
                corner(a, true), corner(a, false));
        }
    }
}
