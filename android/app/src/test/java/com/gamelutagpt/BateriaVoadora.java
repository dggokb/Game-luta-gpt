package com.gamelutagpt;

import java.util.*;

/**
 * Voadora e rasteira (pedido do jogador, 02/11: "ganhei o torneio só com voadora e rasteira, ele
 * tomou todas, o jogo não aprende"). Um jogador roteirizado (o Ryūga) que só faz isso, num arcade de
 * 7 lutas com a CPU subindo do nível 5 ao 8 e a MESMA CPU (a memória dela passa de uma luta para a
 * outra, como no jogo). Mede, luta a luta, o que a CPU faz contra a voadora e contra a rasteira.
 *
 * <pre>args: [arcades] [roteiro]  roteiro: voadora (pula + jH, rasteira ao cair) | rasteira (só 2M)</pre>
 */
public final class BateriaVoadora {
    private BateriaVoadora() {}

    static final int[] NIVEIS = {5, 5, 6, 6, 7, 7, 8};

    static final class Est {
        int lutas, vitorias, pulos, tiradoDoAr, voadoraAcertou, voadoraDefendida;
        int rasteiras, rasteiraAcertou, rasteiraDefendida;
        long quadros;
        int aaTentou, aaVoadoraGanhou;
    }

    /** Pula de meia distância, chuta (jH) descendo e dá a rasteira (2M) ao cair; de perto, rasteira. */
    static BateriaBalanco.Mao voadora(boolean pula) {
        int[] st = {0};
        return (luta, lado, in) -> {
            CombatFighter me = luta.f(lado), cpu = luta.f(1 - lado);
            in.clear();
            float gap = Math.abs(cpu.x - me.x);
            if (!me.grounded) {
                if (st[0] == 1 && me.vy > 0f && gap < 220f && me.canAct()) {
                    in.heavy = true;
                    st[0] = 2;
                }
                return;
            }
            if (!me.canAct()) return;
            if (st[0] >= 1) {
                // Caiu: rasteira na hora.
                st[0] = 0;
                in.direction = MotionParser.relative(3, me.facing);
                in.medium = true;
                return;
            }
            if (gap > 330f) {
                in.direction = MotionParser.relative(1, me.facing);
            } else if (pula && gap >= 190f) {
                in.jump = true;
                in.direction = MotionParser.relative(8, me.facing);
                st[0] = 1;
            } else if (gap < 200f) {
                in.direction = MotionParser.relative(3, me.facing);
                in.medium = true;
            } else {
                in.direction = MotionParser.relative(1, me.facing);
            }
        };
    }

    static void arcade(int k, boolean pula, Est[] porLuta) {
        Random r = new Random(820_000 + k);
        AiController cpu = null;
        for (int i = 0; i < NIVEIS.length; i++) {
            String rival = BateriaBalanco.ELENCO[1 + r.nextInt(BateriaBalanco.ELENCO.length - 1)];
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {"player_base"}, new String[] {rival}, NIVEIS[i],
                830_000 + k * 31L + i, new CombatConfig());
            if (cpu == null) cpu = l.ai[1];
            else {
                // A mesma CPU do arcade: lembra (mais fraco) do que leu nas lutas anteriores.
                cpu.leitura().novaLuta();
                cpu.setLevel(NIVEIS[i]);
                l.ai[1] = cpu;
            }
            l.mao[0] = voadora(pula);
            Est e = porLuta[i];
            boolean noAr = false;
            while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                l.passo();
                CombatFighter j = l.f(0);
                if (!j.grounded && !noAr && j.status == CombatFighter.Status.NEUTRAL) e.pulos++;
                noAr = !j.grounded;
                for (CombatEngine.HitEvent h : l.e.events()) {
                    if (h.attacker == 1 && h.defender == 0 && !h.blocked && !j.grounded && "2H".equals(h.moveId)) e.tiradoDoAr++;
                    if (h.attacker == 0 && "jH".equals(h.moveId)) { if (h.blocked) e.voadoraDefendida++; else e.voadoraAcertou++; }
                    if (h.attacker == 0 && "2M".equals(h.moveId)) { if (h.blocked) e.rasteiraDefendida++; else e.rasteiraAcertou++; }
                }
                if (j.attacking() && j.attackFrame == 1 && "2M".equals(j.attack.id)) e.rasteiras++;
                CombatFighter c = l.f(1);
                if (!j.grounded && c.attacking() && c.attackFrame == 1 && "2H".equals(c.attack.id)) e.aaTentou++;
                for (CombatEngine.HitEvent h : l.e.events())
                    if (h.attacker == 0 && "jH".equals(h.moveId) && !h.blocked && c.attacking() && "2H".equals(c.attack.id)) e.aaVoadoraGanhou++;
            }
            e.lutas++;
            e.quadros += l.quadro;
            if (l.flow.winner() == 0) e.vitorias++;
        }
    }

    static String pc(int a, int b) { return b == 0 ? "–" : String.format(Locale.ROOT, "%.0f%%", 100.0 * a / b); }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        boolean pula = args.length < 2 || !args[1].equals("rasteira");
        Est[] porLuta = new Est[NIVEIS.length];
        for (int i = 0; i < porLuta.length; i++) porLuta[i] = new Est();
        for (int k = 0; k < n; k++) arcade(k, pula, porLuta);
        System.out.println("# " + (pula ? "Voadora e rasteira" : "Só rasteira") + " contra o arcade (" + n + " arcades, a mesma CPU nas 7 lutas)");
        System.out.println();
        System.out.println("| luta (nível) | jogador vence | pulos por luta | antiaéreo tentado | tirado do ar (2H) | voadora ganha do 2H | voadora: acerta / defendida | rasteiras por luta | rasteira: acerta / defendida |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        for (int i = 0; i < porLuta.length; i++) {
            Est e = porLuta[i];
            int vo = e.voadoraAcertou + e.voadoraDefendida, ra = e.rasteiraAcertou + e.rasteiraDefendida;
            System.out.printf(Locale.ROOT, "| %d (%d) | %s | %.1f | %s | %s | %s | %s / %s | %.1f | %s / %s |%n", i + 1, NIVEIS[i],
                pc(e.vitorias, e.lutas), e.pulos / (double) e.lutas, pc(e.aaTentou, e.pulos), pc(e.tiradoDoAr, e.pulos),
                pc(e.aaVoadoraGanhou, e.aaTentou),
                pc(e.voadoraAcertou, e.pulos), pc(e.voadoraDefendida, e.pulos),
                e.rasteiras / (double) e.lutas, pc(e.rasteiraAcertou, e.rasteiras), pc(e.rasteiraDefendida, e.rasteiras));
        }
    }
}
