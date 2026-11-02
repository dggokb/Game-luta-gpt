package com.gamelutagpt;

import java.util.*;

/**
 * Os combos da CPU contra quem joga como o jogador (pedido de 02/11: "ela não comba, não
 * pressiona, não pune, mesmo no nível 8; dificilmente faz combo maior que 3 sem o Super"). O
 * jogador roteirizado é o do laço da luta real (BateriaLevantar: rasteira, pulo com jM, rasteira
 * no levantar); a CPU, nos níveis 5 a 8. Mede, por combo da CPU: golpes, dano, quantos chegam a 4
 * golpes, quantos usam o Super, e o lançador (2H que acertou) seguido no ar.
 *
 * <pre>args: [lutas por nível] [modo]</pre>
 */
public final class BateriaCombosCpu {
    private BateriaCombosCpu() {}

    static final class Est {
        int lutas, vitoriasCpu, combos, golpes, quatroMais, comSuper, lancadores, lancadorSeguido, ataques;
        long dano, quadros;
        int[] tamanho = new int[8];
        final Map<String, int[]> umGolpe = new TreeMap<>();   // golpe que abriu e parou: [vezes, tentou seguir]
    }

    static boolean preso(CombatFighter f) {
        return f.inHitstun() || f.status == CombatFighter.Status.KNOCKDOWN && f.knockdownFrame < 4;
    }

    static void luta(int nivel, long semente, Est e) {
        Random r = new Random(semente);
        String cpu = BateriaBalanco.ELENCO[r.nextInt(BateriaBalanco.ELENCO.length)];
        String jog = BateriaBalanco.ELENCO[r.nextInt(BateriaBalanco.ELENCO.length)];
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {jog}, new String[] {cpu}, nivel, semente, new CombatConfig());
        BateriaLevantar.Roteiro ro = new BateriaLevantar.Roteiro(semente * 7 + 1);
        AttackDefinition m2 = BateriaBalanco.ch(jog).attack("2M");
        l.mao[0] = BateriaLevantar.oki(ro, m2 != null ? m2.startupFrames : 8);
        int n = 0;
        long dano = 0;
        boolean sup = false, lancou = false, seguiu = false, tentou = false;
        String abriu = null;
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            boolean antes = preso(l.f(0));
            l.passo();
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.attacker != 1 || h.blocked) continue;
                if (!antes && n > 0) { fechar(e, n, dano, sup, lancou, seguiu); n = 0; dano = 0; sup = lancou = seguiu = false; }
                if (lancou && !l.f(1).grounded) seguiu = true;
                if (n == 0) { abriu = h.moveId; tentou = false; }
                n++;
                dano += h.damage;
                if ("SUPER".equals(h.moveId) || "ULTRA".equals(h.moveId)) sup = true;
                if ("2H".equals(h.moveId) && !l.f(0).grounded) lancou = true;
                antes = true;
            }
            CombatFighter c = l.f(1);
            if (c.attacking() && c.attackFrame == 1) e.ataques++;
            if (n == 1 && preso(l.f(0)) && c.attacking() && c.attackFrame == 1 && c.attack != null && !c.attack.id.equals(abriu)) tentou = true;
            if (n > 0 && !preso(l.f(0))) {
                if (n == 1) { int[] v = e.umGolpe.computeIfAbsent(abriu + (l.f(0).grounded ? "" : " (ele no ar)"), k -> new int[2]); v[0]++; if (tentou) v[1]++; }
                fechar(e, n, dano, sup, lancou, seguiu);
                n = 0; dano = 0; sup = lancou = seguiu = false;
            }
        }
        e.lutas++;
        e.quadros += l.quadro;
        if (l.flow.winner() == 1) e.vitoriasCpu++;
    }

    static void fechar(Est e, int n, long dano, boolean sup, boolean lancou, boolean seguiu) {
        e.combos++;
        e.golpes += n;
        e.dano += dano;
        if (n >= 4) e.quatroMais++;
        if (sup) e.comSuper++;
        if (lancou) { e.lancadores++; if (seguiu) e.lancadorSeguido++; }
        e.tamanho[Math.min(7, n)]++;
    }

    static Est rodar(int nivel, int n) {
        Est e = new Est();
        for (int i = 0; i < n; i++) luta(nivel, 970_000 + nivel * 10_000L + i, e);
        return e;
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 120;
        String modo = args.length > 1 ? args[1] : "agora";
        AiController.emendaLigada = !modo.equals("antes");
        AiController.contatoLigado = !modo.equals("antes") && !modo.equals("emenda");
        AiController.soltoLigado = modo.equals("agora");
        AiController.shinkaiNoAcerto = modo.equals("agora");
        AiController.saidaLigada = !modo.equals("semSaida");
        if (modo.equals("semSaida")) { AiController.emendaLigada = AiController.contatoLigado = AiController.soltoLigado = AiController.shinkaiNoAcerto = true; }
        System.out.println("# Combos da CPU contra o laço do jogador (" + n + " lutas por nível, 1x1, duplas sorteadas; " + modo + ")");
        System.out.println();
        System.out.println("| nível | CPU vence | golpes por minuto | combos por luta | golpes por combo | 1 / 2 / 3 / 4+ golpes | com Super | dano por combo | lançador seguido no ar |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        for (int nivel = 5; nivel <= 8; nivel++) {
            Est e = rodar(nivel, n);
            int c = Math.max(1, e.combos);
            int quatro = 0;
            for (int i = 4; i < 8; i++) quatro += e.tamanho[i];
            System.out.printf(Locale.ROOT, "| %d | %s | %.0f | %.1f | %.2f | %s / %s / %s / %s | %s | %.0f | %s |%n", nivel,
                BateriaVoadora.pc(e.vitoriasCpu, e.lutas), e.ataques / (e.quadros / 3600.0), e.combos / (double) e.lutas, e.golpes / (double) c,
                BateriaVoadora.pc(e.tamanho[1], c), BateriaVoadora.pc(e.tamanho[2], c), BateriaVoadora.pc(e.tamanho[3], c),
                BateriaVoadora.pc(quatro, c), BateriaVoadora.pc(e.comSuper, c), e.dano / (double) c,
                BateriaVoadora.pc(e.lancadorSeguido, e.lancadores));
            if (nivel == 8 || nivel == 5) {
                StringBuilder b = new StringBuilder("  nível " + nivel + ", combos de 1 golpe (golpe: vezes, tentou seguir): ");
                e.umGolpe.entrySet().stream().sorted((x, y) -> y.getValue()[0] - x.getValue()[0]).limit(12)
                    .forEach(en -> b.append(en.getKey()).append(' ').append(en.getValue()[0]).append('/').append(en.getValue()[1]).append(", "));
                System.err.println(b);
            }
        }
    }
}
