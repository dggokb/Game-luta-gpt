package com.gamelutagpt;

import java.util.*;

/**
 * SHINKAI em luta completa (frente H5, item 7; nada no jogo muda): cada tipo contra o mesmo time sem
 * SHINKAI (a barra de SHINKAI do outro lado fica zerada), CPU nível 8 dos dois lados, 1x1 e 2x2.
 * Mede vitórias, o que a janela de 8 s rende e o que acontece depois com a barra de Super acumulada:
 * em que ela é gasta (Super, Ultra, DHC, Guard Cancel, Breaker), quanto dano isso faz e quanto sobra.
 */
public class BateriaShinkaiBarra {
    static final class Est {
        int lutas, vitorias, decididas, janelas;
        long danoJanela, barraJanela;          // dano e barra ganha dentro da janela (barra em milésimos)
        long gastoDepois, danoBarraDepois;     // barra gasta depois da janela e dano de Super/Ultra/DHC depois dela
        long gastoTotal, danoBarraTotal;       // a luta inteira (para o lado sem SHINKAI também)
        long sobra;                            // barra que sobrou no fim
        final Map<String, Long> usos = new TreeMap<>();
        void uso(String k, long v) { usos.merge(k, v, Long::sum); }
    }

    /** Uma luta: lado {@code la} com o tipo {@code t} (null = ninguém usa SHINKAI). */
    static void luta(String[] time, Shinkai.Type t, int la, long semente, Est a, Est b) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(time, time, BateriaBalanco.NIVEL, semente, new CombatConfig());
        if (t != null) for (FighterState m : l.e.team(la).members) BateriaBalanco.forcarTipo(m, t);
        BateriaBalanco.Mao semShinkai = (lu, lado, in) -> {
            if (t == null || lado != la) for (FighterState m : lu.e.team(lado).members) m.shinkai = 0;
            in.shinkai &= t != null && lado == la;
        };
        l.mao[0] = semShinkai;
        l.mao[1] = semShinkai;
        Est[] est = new Est[2];
        est[la] = a;
        est[1 - la] = b;
        boolean[] dentro = new boolean[2], depois = new boolean[2];
        int[] barraAntes = new int[2];
        int[] inicioJanela = new int[2], gastoJanela = new int[2];
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            for (int s = 0; s < 2; s++) barraAntes[s] = l.f(s).state.superMeter;
            l.passo();
            Set<String> cues = new HashSet<>(l.e.cues());
            for (int s = 0; s < 2; s++) {
                CombatFighter f = l.f(s);
                Est e = est[s];
                boolean agora = f.state.inShinkai();
                if (agora && !dentro[s]) {
                    e.janelas++;
                    inicioJanela[s] = barraAntes[s];
                    gastoJanela[s] = 0;
                }
                int barra = f.state.superMeter;
                int gasto = Math.max(0, barraAntes[s] - barra);
                if (gasto > 0) {
                    String uso = cues.contains("DHC") ? "DHC" : cues.contains("GUARD_CANCEL") ? "Guard Cancel"
                        : cues.contains("COMBO_BREAKER") ? "Breaker"
                        : f.status == CombatFighter.Status.ULTRA || f.inUltraSequence() ? "Ultra"
                        : f.attacking() && f.attack.kind == AttackDefinition.Kind.SUPER ? "Super" : "outro";
                    e.gastoTotal += gasto;
                    if (agora) gastoJanela[s] += gasto;
                    if (depois[s] && !agora) {
                        e.gastoDepois += gasto;
                        e.uso(uso, gasto);
                    }
                }
                if (!agora && dentro[s]) {
                    // A janela acabou: barra ganha nela = o que tem agora − o que tinha + o que gastou dentro.
                    e.barraJanela += barra - inicioJanela[s] + gastoJanela[s];
                    depois[s] = true;
                }
                dentro[s] = agora;
            }
            for (CombatEngine.HitEvent h : l.e.events()) {
                CombatFighter f = l.f(h.attacker);
                Est e = est[h.attacker];
                boolean deBarra = f.status == CombatFighter.Status.ULTRA || f.inUltraSequence() ||
                    f.attacking() && f.attack.kind == AttackDefinition.Kind.SUPER;
                if (dentro[h.attacker]) e.danoJanela += h.damage;
                if (deBarra) {
                    e.danoBarraTotal += h.damage;
                    if (depois[h.attacker] && !dentro[h.attacker]) e.danoBarraDepois += h.damage;
                }
            }
        }
        int v = l.flow.winner();
        for (int s = 0; s < 2; s++) {
            est[s].lutas++;
            est[s].sobra += l.f(s).state.superMeter;
            if (v == 0 || v == 1) {
                est[s].decididas++;
                if (v == s) est[s].vitorias++;
            }
        }
    }

    static String linha(String nome, Est e) {
        double bar = CombatConfig.METER_PER_BAR;
        long usos = e.usos.values().stream().mapToLong(Long::longValue).sum();
        StringBuilder u = new StringBuilder();
        for (Map.Entry<String, Long> x : e.usos.entrySet()) {
            if (u.length() > 0) u.append(", ");
            u.append(x.getKey()).append(' ').append(Math.round(100.0 * x.getValue() / Math.max(1, usos))).append('%');
        }
        return String.format(Locale.ROOT, "| %s | %.0f%% | %.2f | %.0f | %.2f barra | %.2f barra | %s | %.0f | %.0f | %.2f barra |",
            nome, 100.0 * e.vitorias / Math.max(1, e.decididas), e.janelas / (double)Math.max(1, e.lutas),
            e.danoJanela / (double)Math.max(1, e.janelas), e.barraJanela / bar / Math.max(1, e.janelas),
            e.gastoDepois / bar / Math.max(1, e.lutas), u.length() == 0 ? "–" : u.toString(),
            e.gastoDepois == 0 ? 0.0 : e.danoBarraDepois / (e.gastoDepois / bar),
            e.gastoTotal == 0 ? 0.0 : e.danoBarraTotal / (e.gastoTotal / bar), e.sobra / bar / Math.max(1, e.lutas));
    }

    public static void main(String[] args) {
        long t0 = System.nanoTime();
        System.out.println("# SHINKAI em luta completa: rendimento e uso da barra acumulada (frente H5, item 7)");
        System.out.println();
        System.out.println("Cada tipo contra o mesmo time sem SHINKAI (CPU nível 8 dos dois lados, mesmas sementes, metade das lutas de cada lado).");
        System.out.println("\"sem\" = os dois lados sem SHINKAI (controle). Barra em barras de Super (1000).");
        String[] h = {"| tipo | vence | janelas por luta | dano na janela | barra ganha na janela | barra gasta depois da janela (por luta) | em que foi gasta | dano por barra gasta depois | dano por barra (luta toda) | barra que sobrou no fim |",
            "|---|---|---|---|---|---|---|---|---|---|"};
        for (String modo : new String[] {"1x1", "2x2"}) {
            System.out.println();
            System.out.println("## " + modo + (modo.equals("1x1") ? " (os 16, 8 lutas cada)" : " (128 duplas sorteadas)"));
            System.out.println();
            for (String x : h) System.out.println(x);
            List<Shinkai.Type> tipos = new ArrayList<>();
            tipos.add(null);
            tipos.addAll(Arrays.asList(Shinkai.Type.values()));
            Est semOutro = null;
            for (Shinkai.Type t : tipos) {
                Est a = new Est(), b = new Est();
                int n = 0;
                if (modo.equals("1x1")) {
                    for (String c : BateriaBalanco.ELENCO) for (int i = 0; i < 8; i++, n++) luta(new String[] {c}, t, i & 1, 50_000 + n, a, b);
                } else {
                    for (int i = 0; i < 128; i++) {
                        String[] dupla = BateriaBalanco.par(new Random(60_000 + i));
                        luta(dupla, t, i & 1, 60_000 + i, a, b);
                    }
                }
                System.out.println(linha(t == null ? "sem" : t.label, a));
                if (t == null) semOutro = b;
                else System.out.println(linha("  (o lado sem SHINKAI contra " + t.label + ")", b));
            }
        }
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
