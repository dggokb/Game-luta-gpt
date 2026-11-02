package com.gamelutagpt;

import java.util.*;

/**
 * Ar contra ar montado de propósito (segundo registro real, 02/11: o jM do jogador ganhava do jH
 * da CPU). Os dois pulam um na direção do outro: a CPU com o plano dela de pular para dentro (ela
 * escolhe o golpe), o jogador com um atraso e um golpe do roteiro. Cada cena vai até o primeiro
 * acerto no ar (ou até os dois caírem) e conta quem ganhou.
 *
 * <pre>args: [antes|depois] (padrão: os dois, lado a lado)</pre>
 */
public final class BateriaArAr {
    private BateriaArAr() {}

    /** O golpe do jogador no ar: o do registro (jM ao alcançar), o jM cedo, o jH descendo, nada. */
    enum Golpe { JM, JM_CEDO, JL, JH_DESCENDO, NADA }

    static boolean rastro;
    static final int[] DISTANCIAS = {300, 380, 460};
    static final int[] ATRASOS = {-10, -5, 0, 5, 10};   // quadros do pulo do jogador depois do da CPU
    static final int[] NIVEIS = {5, 8};
    static final String[] JOGADORES = {"p15", "player_base"};

    /** 0 nada, 1 jogador acerta, 2 CPU acerta, 3 troca (os dois no mesmo quadro), 4 defendido. */
    static int cena(String jog, String cpu, int nivel, int dist, int atraso, Golpe g, long semente) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {jog}, new String[] {cpu}, nivel, semente,
            new CombatConfig(), 700f, 700f + dist);
        // Espera o "LUTE": os dois parados (a CPU vai vendo, a percepção dela enche).
        int[] t = {-1};
        boolean[] jogPulou = {false}, jogBateu = {false};
        l.mao[1] = (luta, lado, in) -> {
            if (t[0] == 0) luta.ai[1].pularParaDentro(luta.f(1), in);
            else if (t[0] < 0) in.clear();
        };
        l.mao[0] = (luta, lado, in) -> {
            CombatFighter me = luta.f(0), r = luta.f(1);
            in.clear();
            if (t[0] < 0) return;
            if (!jogPulou[0]) {
                if (t[0] >= atraso && me.grounded && me.canAct()) {
                    in.jump = true;
                    in.direction = MotionParser.relative(8, me.facing);
                    jogPulou[0] = true;
                }
                return;
            }
            if (me.grounded || jogBateu[0] || !me.canAct()) return;
            float gap = Math.abs(r.x - me.x);
            boolean aperta;
            switch (g) {
                case JM: aperta = gap < 200f; break;
                case JM_CEDO: aperta = gap < 240f; break;
                case JL: aperta = gap < 190f; break;
                case JH_DESCENDO: aperta = me.vy > 0f && gap < 220f; break;
                default: aperta = false;
            }
            if (aperta) {
                if (g == Golpe.JL) in.light = true;
                else if (g == Golpe.JH_DESCENDO) in.heavy = true;
                else in.medium = true;
                jogBateu[0] = true;
            }
        };
        int espera = 0;
        while (l.flow.holdsFighters() && espera++ < 600) l.passo();
        for (int i = 0; i < 30; i++) l.passo();   // a CPU vendo o rival parado
        // Os dois no lugar de novo (a espera pode ter mexido), na distância da cena.
        t[0] = 0;
        boolean jogadorSaiu = false, cpuSaiu = false;
        for (int q = 0; q < 110; q++) {
            boolean noArJ = BateriaArArAux.pulando(l.f(0)), noArC = BateriaArArAux.pulando(l.f(1));
            l.passo();
            t[0]++;
            if (!l.f(0).grounded) jogadorSaiu = true;
            if (!l.f(1).grounded) cpuSaiu = true;
            if (rastro) {
                CombatFighter fj = l.f(0), fc = l.f(1);
                System.out.printf(Locale.ROOT, "t=%d gap=%.0f J[%s %s f%d y=%.0f vy=%.0f] C[%s %s f%d y=%.0f vy=%.0f] razão=%s%n", t[0], Math.abs(fj.x - fc.x),
                    fj.status, fj.attack != null ? fj.attack.id : "-", fj.attackFrame, fj.y, fj.vy,
                    fc.status, fc.attack != null ? fc.attack.id : "-", fc.attackFrame, fc.y, fc.vy, l.ai[1].lastReason);
            }
            boolean j = false, c = false, def = false;
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.projectile) continue;
                if (!(noArJ && noArC)) return 0;   // alguém já tinha caído: não é ar contra ar
                if (h.blocked) def = true;
                else if (h.attacker == 0) j = true;
                else c = true;
            }
            if (j && c) return 3;
            if (j) return 1;
            if (c) return 2;
            if (def) return 4;
            if (jogadorSaiu && cpuSaiu && l.f(0).grounded && l.f(1).grounded) return 0;
        }
        return 0;
    }

    static int[] rodar(boolean depois) {
        AiController.arArLigado = depois;
        int[] r = new int[5];
        long k = 0;
        for (String jog : JOGADORES)
            for (String cpu : BateriaBalanco.ELENCO)
                for (int nivel : NIVEIS)
                    for (int dist : DISTANCIAS)
                        for (int atraso : ATRASOS)
                            for (Golpe g : Golpe.values())
                                r[cena(jog, cpu, nivel, dist, atraso, g, 950_000 + k++)]++;
        return r;
    }

    static int[] porGolpe(boolean depois, Golpe g, int nivel) {
        AiController.arArLigado = depois;
        int[] r = new int[5];
        long k = 0;
        for (String jog : JOGADORES)
            for (String cpu : BateriaBalanco.ELENCO)
                for (int dist : DISTANCIAS)
                    for (int atraso : ATRASOS)
                        r[cena(jog, cpu, nivel, dist, atraso, g, 960_000 + k++)]++;
        return r;
    }

    static String linha(String nome, int[] r) {
        int tot = r[0] + r[1] + r[2] + r[3] + r[4];
        return String.format(Locale.ROOT, "| %s | %s | %s | %s | %s | %s |", nome,
            BateriaVoadora.pc(r[1], tot), BateriaVoadora.pc(r[2], tot), BateriaVoadora.pc(r[3], tot),
            BateriaVoadora.pc(r[4], tot), BateriaVoadora.pc(r[0], tot));
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("rastro")) {
            rastro = true;
            AiController.arArLigado = args.length < 2 || !args[1].equals("antes");
            System.out.println("resultado " + cena("p15", "p02", 8, 380, 0, Golpe.JM, 1));
            return;
        }
        System.out.println("# Ar contra ar montado (os dois pulam um na direção do outro)");
        System.out.println();
        System.out.println("Jogador p15 e Ryūga contra os 16; distâncias " + Arrays.toString(DISTANCIAS) + ", atraso do pulo dele " +
            Arrays.toString(ATRASOS) + " quadros, níveis " + Arrays.toString(NIVEIS) + ". Fração das cenas (300 por linha).");
        for (int nivel : NIVEIS) {
            System.out.println();
            System.out.println("## Nível " + nivel);
            System.out.println();
            System.out.println("| golpe do jogador | CPU | jogador acerta | CPU acerta | troca | defendido | ninguém acerta |");
            System.out.println("|---|---|---|---|---|---|---|");
            for (Golpe g : Golpe.values()) {
                for (boolean depois : new boolean[] {false, true}) {
                    int[] r = porGolpe(depois, g, nivel);
                    System.out.println(linha(g + " | " + (depois ? "depois" : "antes"), r));
                }
            }
        }
        AiController.arArLigado = true;
    }
}

final class BateriaArArAux {
    private BateriaArArAux() {}

    static boolean pulando(CombatFighter f) {
        return !f.grounded && (f.status == CombatFighter.Status.NEUTRAL || f.status == CombatFighter.Status.ATTACK);
    }
}
