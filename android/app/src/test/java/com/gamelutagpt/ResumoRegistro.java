package com.gamelutagpt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Lê um registro de luta (o arquivo do ENVIAR REGISTRO, ou o de uma bateria) e resume cada luta:
 * pulos e super pulos de cada lado e como terminaram, golpes mais usados, acertos e defesas, o que
 * a CPU mais "pensou" e a duração. Roda à mão: {@code java ... ResumoRegistro arquivo.txt}.
 */
public final class ResumoRegistro {
    private ResumoRegistro() {}

    static final class Luta {
        String cabecalho = "", fim = "";
        double segundos;
        final int[] pulos = new int[2], superPulos = new int[2], acertos = new int[2], defesas = new int[2];
        final int[] puloAcertou = new int[2], puloDefendido = new int[2], puloApanhou = new int[2];
        final List<Map<String, Integer>> golpes = Arrays.asList(new TreeMap<>(), new TreeMap<>());
        final Map<String, Integer> pensa = new TreeMap<>();
    }

    static Luta[] ler(String texto) {
        List<Luta> lutas = new ArrayList<>();
        Luta l = null;
        boolean[] noAr = new boolean[2];
        int[] resultado = new int[2];
        for (String linha : texto.split("\n")) {
            if (linha.startsWith("=== LUTA ")) {
                l = new Luta();
                l.cabecalho = linha.substring(9);
                lutas.add(l);
                noAr[0] = noAr[1] = false;
                continue;
            }
            if (l == null) continue;
            if (linha.startsWith("=== ")) { l.fim = linha.substring(4); continue; }
            String[] p = linha.trim().split("  ", 3);
            if (p.length < 3) continue;
            double t;
            try { t = Double.parseDouble(p[0].trim()); } catch (NumberFormatException e) { continue; }
            l.segundos = Math.max(l.segundos, t);
            int s = p[1].equals("JOG") ? 0 : p[1].equals("CPU") ? 1 : -1;
            String o = p[2];
            if (s >= 0 && o.startsWith("pensa: ")) {
                l.pensa.merge(o.substring(7), 1, Integer::sum);
                continue;
            }
            if (s < 0) continue;
            // Botões e direções do jogador não são o que ele está fazendo.
            if (o.startsWith("direção ") || o.startsWith("aperta")) continue;
            if (o.startsWith("ACERTA ") || o.startsWith("DEFENDE ")) {
                boolean acertou = o.startsWith("ACERTA ");
                if (acertou) l.acertos[s]++; else l.defesas[s]++;
                if (noAr[s]) resultado[s] = Math.max(resultado[s], acertou ? 2 : 1);
                if (noAr[1 - s] && acertou) resultado[1 - s] = Math.max(resultado[1 - s], 3);
                continue;
            }
            if (o.startsWith("golpe ")) {
                String id = o.substring(6).split("[ \\[]")[0];
                l.golpes.get(s).merge(id, 1, Integer::sum);
            }
            boolean pulo = o.startsWith("pulo ") || o.startsWith("SUPER PULO");
            if (pulo && !noAr[s]) {
                l.pulos[s]++;
                if (o.startsWith("SUPER PULO")) l.superPulos[s]++;
                noAr[s] = true;
                resultado[s] = 0;
            } else if (noAr[s] && !pulo && !o.contains("(no ar)") && !o.startsWith("apanha no ar")) {
                if (resultado[s] == 2) l.puloAcertou[s]++;
                else if (resultado[s] == 1) l.puloDefendido[s]++;
                else if (resultado[s] == 3) l.puloApanhou[s]++;
                noAr[s] = false;
            }
        }
        return lutas.toArray(new Luta[0]);
    }

    static String top(Map<String, Integer> m, int n) {
        StringBuilder b = new StringBuilder();
        m.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(n)
            .forEach(e -> b.append(b.length() > 0 ? ", " : "").append(e.getKey()).append(" ").append(e.getValue()));
        return b.toString();
    }

    public static void main(String[] args) throws IOException {
        String texto = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        Luta[] lutas = ler(texto);
        System.out.println("# Resumo do registro (" + lutas.length + " lutas)");
        int i = 0;
        for (Luta l : lutas) {
            System.out.println();
            System.out.printf(Locale.ROOT, "## Luta %d: %s%n%n%s, %.0f s%n%n", ++i, l.cabecalho, l.fim, l.segundos);
            System.out.println("| | jogador | CPU |");
            System.out.println("|---|---|---|");
            System.out.printf(Locale.ROOT, "| pulos (super pulos) | %d (%d) | %d (%d) |%n", l.pulos[0], l.superPulos[0], l.pulos[1], l.superPulos[1]);
            System.out.printf(Locale.ROOT, "| pulo: acertou / defendido / apanhou no ar | %d / %d / %d | %d / %d / %d |%n",
                l.puloAcertou[0], l.puloDefendido[0], l.puloApanhou[0], l.puloAcertou[1], l.puloDefendido[1], l.puloApanhou[1]);
            System.out.printf(Locale.ROOT, "| acertos / defendidos | %d / %d | %d / %d |%n", l.acertos[0], l.defesas[0], l.acertos[1], l.defesas[1]);
            System.out.printf(Locale.ROOT, "| golpes mais usados | %s | %s |%n", top(l.golpes.get(0), 6), top(l.golpes.get(1), 6));
            System.out.println();
            System.out.println("O que a CPU mais pensou: " + top(l.pensa, 10));
        }
    }
}
