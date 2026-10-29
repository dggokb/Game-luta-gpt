package com.gamelutagpt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Mede o alcance real (o do desenho, por onde o golpe acerta de fato) de cada golpe de chão de
 * cada lutador contra o Ryūga parado, em pé e agachado, e escreve {@code CpuReachTable.java}: a
 * régua da CPU ({@link CpuReach}). Rodar de novo quando os sprites ou os golpes mudarem; o
 * {@code CpuReachTest} avisa quando a tabela não confere com o motor.
 * Uso: GeraAlcanceCpu [caminho do CpuReachTable.java]
 */
public final class GeraAlcanceCpu {
    private GeraAlcanceCpu() {}

    /** O alvo da medida (o corpo dele sai da conta: a tabela guarda a ponta, centro a frente). */
    static final String ALVO = "player_base";
    static final String[] GOLPES = {"L", "M", "H", "2L", "2M", "2H", GolpeAlto.BINDING, "S", "S2", "S3", "S4"};
    static final int PASSO = 2, MAX = 640;

    /** Golpes de chão do lutador que a tabela mede (corpo a corpo: projétil não tem ponta). */
    static List<String> golpes(CharacterDefinition c) {
        List<String> out = new ArrayList<>();
        for (String g : GOLPES) {
            AttackDefinition a = c.attack(g);
            if (a == null || !a.melee()) continue;
            if (g.startsWith("S") && !g.equals("S")) {
                CharacterDefinition.Special s = c.specials.get(g);
                if (s == null || s.air || s.projectile != null) continue;
            }
            if (g.equals("S") && (c.fighter.energyAir || c.fighter.energy == null)) continue;
            if (g.equals(GolpeAlto.BINDING) && !c.moves.containsKey(g)) continue;
            out.add(g);
        }
        return out;
    }

    /** O golpe {@code g} de {@code id}, apertado com o rival a {@code gap} (eixo a eixo), acerta? */
    static boolean acerta(String id, String g, int gap, boolean agachado) {
        CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.get(id), "P1"),
            new FighterState(GeneratedCharacters.get(ALVO), "P2"), 1000f, 1000f + gap, new CombatConfig());
        FighterInput a = new FighterInput(), b = new FighterInput();
        boolean comecou = false;
        for (int fr = 0; fr < 150; fr++) {
            a.clear();
            b.clear();
            b.direction = agachado ? 3 : 0;
            if (fr == 0) aperta(g, a);
            float x0 = e.fighter(0).x, x1 = e.fighter(1).x;
            e.step(a, b);
            if (fr == 0) {
                // O empurrão dos corpos encostados não afasta antes do golpe: começa na distância pedida.
                e.fighter(0).x = x0;
                e.fighter(1).x = x1;
            }
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0 && !h.blocked) return true;
            if (e.fighter(0).attacking()) comecou = true;
            else if (comecou) break;
        }
        return false;
    }

    /** Como a CPU aperta (AiController.press e o especial pronto). */
    static void aperta(String g, FighterInput in) {
        if (g.startsWith("S")) {
            in.special = g.equals("S") ? "M" : g;
            return;
        }
        if (g.equals(GolpeAlto.BINDING)) in.direction = 1;
        else if (g.startsWith("2")) in.direction = 3;
        char b = g.charAt(g.length() - 1);
        in.light = b == 'L';
        in.medium = b == 'M';
        in.heavy = b == 'H';
    }

    /** Maior distância eixo a eixo em que acerta, menos a meia largura do alvo; -1 se nunca acerta. */
    static int ponta(String id, String g, boolean agachado) {
        int melhor = -1;
        for (int gap = 20; gap <= MAX; gap += PASSO) {
            if (acerta(id, g, gap, agachado)) melhor = gap;
            else if (melhor > 0 && gap > melhor + 80) break;
        }
        return melhor < 0 ? -1 : melhor - Math.round(GeneratedCharacters.get(ALVO).fighter.body.halfWidth);
    }

    static String linha(String id) {
        CharacterDefinition c = GeneratedCharacters.get(id);
        StringBuilder sb = new StringBuilder();
        for (String g : golpes(c)) {
            sb.append(String.format(Locale.ROOT, "%s %s %d %d;", id, g, ponta(id, g, false), ponta(id, g, true)));
        }
        return sb.toString();
    }

    static List<String> ids() {
        List<String> ids = new ArrayList<>(Arrays.asList(GeneratedCharacters.SELECTABLE));
        ids.add(GeneratedCharacters.OPPONENT);
        return ids;
    }

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "src/main/java/com/gamelutagpt/CpuReachTable.java");
        List<String> linhas = ids().parallelStream().map(GeraAlcanceCpu::linha).collect(Collectors.toList());
        StringBuilder src = new StringBuilder();
        src.append("package com.gamelutagpt;\n\n");
        src.append("/**\n");
        src.append(" * Gerado por GeraAlcanceCpu (teste): o alcance real de cada golpe de chão, medido no motor contra o\n");
        src.append(" * Ryūga parado (o golpe acerta pelo desenho). Cada entrada: lutador, golpe, a ponta contra quem\n");
        src.append(" * está em pé e contra quem está agachado (px do centro de quem bate até a frente do corpo do\n");
        src.append(" * rival; -1 = não acerta). Não editar à mão: rodar o GeraAlcanceCpu de novo.\n");
        src.append(" */\n");
        src.append("final class CpuReachTable {\n");
        src.append("    private CpuReachTable() {}\n\n");
        src.append("    static final String DATA =\n");
        for (int i = 0; i < linhas.size(); i++) {
            src.append("        \"").append(linhas.get(i)).append('"').append(i + 1 < linhas.size() ? " +\n" : ";\n");
        }
        src.append("}\n");
        Files.write(out, src.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("escrito: " + out.toAbsolutePath());
    }
}
