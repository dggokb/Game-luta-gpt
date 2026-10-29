package com.gamelutagpt;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Frente G1: o que a CPU faz com o kit de especiais (S1–S4). Ficha dos especiais de cada um
 * (função lida dos dados), e uso por minuto de cada especial em lutas CPU × CPU 1x1 contra o
 * elenco, com as mesmas sementes antes e depois.
 * Uso: BateriaEspeciaisCpu [ficha|uso|tudo] [lutas por par]
 */
public final class BateriaEspeciaisCpu {
    private BateriaEspeciaisCpu() {}

    static final String[] ELENCO = BateriaBalanco.ELENCO;

    static String funcao(CharacterDefinition c, String id) {
        AttackDefinition a = c.attack(id);
        if (a == null) return "-";
        CharacterDefinition.Special s = c.specials.get(id);
        AttackDefinition.Mechanics m = a.mechanics;
        StringBuilder b = new StringBuilder();
        if (s == null && "S".equals(id)) b.append(c.fighter.energy != null ? "projétil(S1)" : "S1");
        if (s != null && s.projectile != null) {
            b.append(s.projectile.trapFuse != null ? "armadilha" : s.projectile.puddleFrames > 0 ? "poça" : "projétil");
            if (s.projectile.gravity > 0) b.append("-arco");
        }
        if (m.grab) b.append(" agarrão(").append((int) m.grabRange).append(")");
        if (m.counter != AttackDefinition.Reads.NONE) b.append(" contra-").append(m.counter);
        if (m.evade != AttackDefinition.Reads.NONE) b.append(" esquiva-").append(m.evade);
        if (m.repel) b.append(" repel");
        if (m.armorHits > 0) b.append(" armadura").append(m.armorHits);
        if (m.advanceSpeed != 0) b.append(" avanço").append((int) m.advanceSpeed);
        if (m.teleportFrame >= 0) b.append(" teleporte");
        if (m.pull > 0) b.append(" puxa");
        if (m.dives()) b.append(" mergulho");
        if (m.poisonDamage > 0) b.append(" veneno");
        if (m.echoFrames > 0) b.append(" eco");
        if (m.chargeFrames > 0) b.append(" carga");
        if (m.lifeCost > 0) b.append(" vida");
        if (m.rebound) b.append(" rebote");
        if (m.pound) b.append(" pancadas");
        if (m.hopHeight > 0) b.append(" salto");
        if (a.launch == AttackDefinition.Launch.LAUNCH) b.append(" lança");
        if (a.overhead) b.append(" alto");
        if (a.low) b.append(" baixo");
        if (s != null && s.air) b.append(" (ar)");
        if (s != null && s.hold) b.append(" (carga-dir)");
        return b.toString().trim();
    }

    static void ficha() {
        System.out.println("| Personagem | Golpe | Nome | Função | Dano | Início | Ativo | Recup. | Alcance | Altura |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|");
        for (String id : ELENCO) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            for (String g : new String[] {"S", "S2", "S3", "S4"}) {
                AttackDefinition a = c.attack(g);
                if (a == null) continue;
                CharacterDefinition.Special s = c.specials.get(g);
                System.out.printf(Locale.ROOT, "| %s | %s | %s | %s | %d | %d | %d | %d | %.0f | %.0f |%n",
                    id, g.equals("S") ? "S1" : g, s != null && s.name != null ? s.name : c.fighter.energyName,
                    funcao(c, g), a.damage, a.startupFrames, a.activeFrames, a.recoveryFrames, a.reach, a.hitHeight);
            }
        }
    }

    /** Usos por minuto de S1..S4 de cada um (1x1 contra os outros 15), e vitórias. */
    static void uso(int porPar) {
        int n = ELENCO.length;
        Map<String, double[]> usos = new HashMap<>();
        double[] min = new double[n];
        int[] vit = new int[n], lutas = new int[n];
        Map<String, double[]> acertos = new HashMap<>(), defesas = new HashMap<>();
        double[] dano = new double[n], danoEsp = new double[n];
        for (String id : ELENCO) {
            acertos.put(id, new double[5]);
            defesas.put(id, new double[5]);
        }
        for (int a = 0; a < n; a++) {
            usos.put(ELENCO[a], new double[5]);
        }
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                if (a == b) continue;
                for (int i = 0; i < porPar; i++) {
                    BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {ELENCO[a]}, new String[] {ELENCO[b]},
                        BateriaBalanco.NIVEL, 110_000 + a * 1000 + b * 20 + i, new CombatConfig());
                    AttackDefinition[] antes = new AttackDefinition[2];
                    int[] frameAntes = {-1, -1};
                    int[] slotAtual = {-1, -1};
                    boolean[] acertou = new boolean[2], defendeu = new boolean[2];
                    while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                        l.passo();
                        for (int lado = 0; lado < 2; lado++) {
                            CombatFighter f = l.f(lado);
                            AttackDefinition at = f.attacking() ? f.attack : null;
                            boolean novo = at != null && (at != antes[lado] || f.attackFrame < frameAntes[lado]);
                            if ((novo || at == null) && slotAtual[lado] >= 0) {
                                if (acertou[lado]) acertos.get(f.state.character.id)[slotAtual[lado]]++;
                                else if (defendeu[lado]) defesas.get(f.state.character.id)[slotAtual[lado]]++;
                                slotAtual[lado] = -1;
                            }
                            if (novo) {
                                int k = slot(at.id);
                                if (k >= 0) usos.get(f.state.character.id)[k]++;
                                slotAtual[lado] = k;
                                acertou[lado] = defendeu[lado] = false;
                            }
                            // Acertou: o golpe (ou o contra-golpe/repel que pegou: countered).
                            if (at != null && (f.outcome == CombatFighter.Outcome.HIT || f.countered)) acertou[lado] = true;
                            if (at != null && f.outcome == CombatFighter.Outcome.BLOCK) defendeu[lado] = true;
                            // O repel que desfez a bola (sem acerto no corpo) também conta.
                            if (at != null && at.mechanics.repel && l.e.cues().contains("REPEL")) acertou[lado] = true;
                            antes[lado] = at;
                            frameAntes[lado] = at != null ? f.attackFrame : -1;
                        }
                    }
                    for (int lado = 0; lado < 2; lado++) {
                        int idx = lado == 0 ? a : b;
                        dano[idx] += l.est[lado].dano;
                        for (String g : new String[] {"S2", "S3", "S4"}) danoEsp[idx] += l.est[lado].porGolpe.getOrDefault(g, 0);
                    }
                    min[a] += l.quadro / 3600.0;
                    min[b] += l.quadro / 3600.0;
                    lutas[a]++;
                    lutas[b]++;
                    int w = l.flow.winner();
                    if (w == 0) vit[a]++;
                    else if (w == 1) vit[b]++;
                }
            }
        }
        System.out.println("Cada célula: usos por minuto (% que acertou / % defendido).");
        System.out.println("| Personagem | S1/min | S2/min | S3/min | S4/min | Super/min | S2–S4 acertam | dano de S2–S4 | vence |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        for (int a = 0; a < n; a++) {
            double[] u = usos.get(ELENCO[a]);
            double[] h = acertos.get(ELENCO[a]), d = defesas.get(ELENCO[a]);
            double usosEsp = u[1] + u[2] + u[3];
            System.out.printf(Locale.ROOT, "| %s | %s | %s | %s | %s | %s | %s | %.0f%% | %.0f%% |%n", ELENCO[a],
                cel(u[0], h[0], d[0], min[a]), cel(u[1], h[1], d[1], min[a]), cel(u[2], h[2], d[2], min[a]), cel(u[3], h[3], d[3], min[a]), cel(u[4], h[4], d[4], min[a]),
                usosEsp > 0 ? String.format(Locale.ROOT, "%.0f%%", 100.0 * (h[1] + h[2] + h[3]) / usosEsp) : "-",
                100.0 * danoEsp[a] / Math.max(1.0, dano[a]), 100.0 * vit[a] / lutas[a]);
        }
    }

    /** Usos por minuto (e a parte que acertou). */
    static String cel(double usos, double acertos, double defesas, double minutos) {
        if (usos == 0) return "0";
        return String.format(Locale.ROOT, "%.1f (%.0f/%.0f)", usos / minutos, 100.0 * acertos / usos, 100.0 * defesas / usos);
    }

    /** As duplas oficiais (as seis do torneio e as duas que ainda não estão prontas). */
    static final String[][] DUPLAS = {{"player_base", "p02"}, {"p03", "p04"}, {"p05", "p06"}, {"p07", "p08"},
        {"p09", "p10"}, {"p11", "p12"}, {"p13", "p14"}, {"p15", "p16"}};

    /** 2x2 entre as duplas oficiais, todas contra todas, trocando de lado: vitórias de cada dupla. */
    static void duplas(int porPar) {
        int n = DUPLAS.length;
        int[] vit = new int[n], lutas = new int[n];
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                if (a == b) continue;
                for (int i = 0; i < porPar; i++) {
                    BateriaBalanco.Luta l = new BateriaBalanco.Luta(DUPLAS[a], DUPLAS[b], BateriaBalanco.NIVEL,
                        120_000 + a * 1000 + b * 20 + i, new CombatConfig());
                    int w = l.rodar();
                    lutas[a]++;
                    lutas[b]++;
                    if (w == 0) vit[a]++;
                    else if (w == 1) vit[b]++;
                }
            }
        }
        System.out.println("| Dupla | vence |");
        System.out.println("|---|---|");
        for (int a = 0; a < n; a++) {
            System.out.printf(Locale.ROOT, "| %s + %s | %.0f%% |%n", DUPLAS[a][0], DUPLAS[a][1], 100.0 * vit[a] / lutas[a]);
        }
    }

    static int slot(String id) {
        switch (id) {
            case "S": return 0;
            case "S2": return 1;
            case "S3": return 2;
            case "S4": return 3;
            case "SUPER": return 4;
            default: return -1;
        }
    }

    public static void main(String[] args) {
        String qual = args.length > 0 ? args[0] : "tudo";
        int porPar = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        long t0 = System.nanoTime();
        if (qual.equals("ficha") || qual.equals("tudo")) ficha();
        if (qual.equals("uso") || qual.equals("tudo")) uso(porPar);
        if (qual.equals("duplas") || qual.equals("tudo")) duplas(porPar);
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
