package com.gamelutagpt;

import java.util.*;

/**
 * O laço da luta real (registro do jogador, 01/11, docs/balanco/registro-luta.md): rasteira (2M)
 * derruba; no levantar da CPU, ou a rasteira de novo em cima da hora (meaty) ou o pulo com o jM
 * caindo nela quando ela levanta; o jM entrou ou foi defendido, a rasteira ao cair. De longe, o
 * pulo com jM e a rasteira ao cair. Arcade de 7 lutas, a MESMA CPU (a memória dela passa de uma luta
 * para a outra), nível 5 → 8, como a BateriaVoadora.
 *
 * <pre>args: [arcades]</pre>
 */
public final class BateriaLevantar {
    private BateriaLevantar() {}

    static final class Est {
        int lutas, vitorias;
        long quadros;
        int meaty2M, meaty2MAcerta, meaty2MDefende;
        int meatyJM, meatyJMAcerta, meatyJMDefende;
        int jmDepois2M, jmDepois2MAcerta, jmDepois2MDefende;   // a rasteira depois do jM (entrou ou defendido)
        int levantou, levantouDefendendo, levantouAtacando;
        int danoJog, danoCpu;
    }

    /** Quadros até o rival caído ficar livre (contando o levantar), ou -1 se não está caído. */
    static int livreEm(CombatEngine e, CombatFighter r) {
        CombatConfig c = e.config;
        if (r.status == CombatFighter.Status.WAKEUP) {
            if (r.rollFrames > 0) return -1;
            return Math.max(0, e.wakeupFrames(r) - r.knockdownFrame);
        }
        if (r.status != CombatFighter.Status.KNOCKDOWN || r.knockdownFrame < c.knockdownFallFrames) return -1;
        if (r.wakeup == CombatFighter.WAKE_BACK_ROLL || r.wakeup == CombatFighter.WAKE_FORWARD_ROLL) return -1;
        int down = c.knockdownDownFrames;
        if (r.wakeup == CombatFighter.WAKE_QUICK) down = Math.min(down, c.quickRiseDownFrames);
        if (r.wakeup == CombatFighter.WAKE_DELAY) down += c.delayWakeupFrames;
        return Math.max(0, c.knockdownFallFrames + down - r.knockdownFrame) + e.wakeupFrames(r);
    }

    /** Estado do roteiro (por luta). */
    static final class Roteiro {
        int st;                // 0 chão, 1 pulou (jM ainda não), 2 jM saiu: rasteira ao cair
        int plano = -1;        // no rival caído: 0 rasteira em cima da hora, 1 pulo com jM
        int pulouAntes;        // quadros de antecedência do pulo no levantar
        boolean meatyJ, meatyR;
        int rasteiraDepoisJm;  // a próxima rasteira é a depois do jM
        final Random r;
        Roteiro(long s) { r = new Random(s); }
    }

    static BateriaBalanco.Mao oki(Roteiro ro, int startup2M) {
        return (luta, lado, in) -> {
            CombatEngine e = luta.e;
            CombatFighter me = luta.f(lado), cpu = luta.f(1 - lado);
            in.clear();
            float gap = Math.abs(cpu.x - me.x);
            if (!me.grounded) {
                if (ro.st == 1 && me.vy > 0f && gap < 200f && me.canAct()) {
                    in.medium = true;
                    ro.st = 2;
                }
                return;
            }
            if (ro.st >= 1) {
                // Caiu: rasteira (o jogador aperta M o tempo todo; segura até sair).
                in.direction = MotionParser.relative(3, me.facing);
                in.medium = true;
                if (me.attacking()) {
                    ro.rasteiraDepoisJm = ro.st == 2 ? 1 : 0;
                    ro.st = 0;
                }
                return;
            }
            if (!me.canAct()) return;
            int livre = livreEm(e, cpu);
            if (livre >= 0) {
                if (ro.plano < 0) {
                    ro.plano = ro.r.nextInt(2);
                    ro.pulouAntes = 30 + ro.r.nextInt(8);
                }
                if (ro.plano == 0) {
                    if (gap > 175f) in.direction = MotionParser.relative(1, me.facing);
                    else if (gap < 110f) in.direction = MotionParser.relative(5, me.facing);
                    if (livre <= startup2M - 1) {
                        in.direction = MotionParser.relative(3, me.facing);
                        in.medium = true;
                        ro.meatyR = true;
                    }
                } else {
                    if (gap > 200f) in.direction = MotionParser.relative(1, me.facing);
                    else if (gap < 120f) in.direction = MotionParser.relative(5, me.facing);
                    if (livre <= ro.pulouAntes) {
                        in.jump = true;
                        in.direction = MotionParser.relative(gap > 150f ? 8 : 7, me.facing);
                        ro.st = 1;
                        ro.meatyJ = true;
                    }
                }
                return;
            }
            ro.plano = -1;
            if (gap > 330f) {
                in.direction = MotionParser.relative(1, me.facing);
            } else if (gap >= 160f) {
                in.jump = true;
                in.direction = MotionParser.relative(8, me.facing);
                ro.st = 1;
            } else {
                in.direction = MotionParser.relative(3, me.facing);
                in.medium = true;
            }
        };
    }

    static void arcade(int k, Est[] porLuta) {
        Random r = new Random(860_000 + k);
        AiController cpu = null;
        int startup2M = BateriaBalanco.ch("player_base").attack("2M").startupFrames;
        for (int i = 0; i < BateriaVoadora.NIVEIS.length; i++) {
            String rival = BateriaBalanco.ELENCO[1 + r.nextInt(BateriaBalanco.ELENCO.length - 1)];
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {"player_base"}, new String[] {rival},
                BateriaVoadora.NIVEIS[i], 870_000 + k * 31L + i, new CombatConfig());
            if (cpu == null) cpu = l.ai[1];
            else {
                cpu.leitura().novaLuta();
                cpu.setLevel(BateriaVoadora.NIVEIS[i]);
                l.ai[1] = cpu;
            }
            Roteiro ro = new Roteiro(880_000 + k * 31L + i);
            l.mao[0] = oki(ro, startup2M);
            Est e = porLuta[i];
            CombatFighter.Status antes = CombatFighter.Status.NEUTRAL;
            int livreDesde = -1;
            boolean meatyJ = false, meatyR = false, depoisJm = false;
            while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                l.passo();
                CombatFighter c = l.f(1);
                if (antes == CombatFighter.Status.WAKEUP && c.status != CombatFighter.Status.WAKEUP) {
                    e.levantou++;
                    livreDesde = l.quadro;
                }
                if (livreDesde >= 0 && l.quadro - livreDesde == 2) {
                    if (c.attacking()) e.levantouAtacando++;
                    else if (l.e.guardFromInput(c) != CombatFighter.GUARD_NONE || c.status == CombatFighter.Status.BLOCKSTUN) e.levantouDefendendo++;
                    livreDesde = -1;
                }
                antes = c.status;
                if (ro.meatyJ) { ro.meatyJ = false; meatyJ = true; e.meatyJM++; }
                if (ro.meatyR) { ro.meatyR = false; if (!meatyR) e.meaty2M++; meatyR = true; }
                if (ro.rasteiraDepoisJm == 1) { ro.rasteiraDepoisJm = 0; depoisJm = true; }
                for (CombatEngine.HitEvent h : l.e.events()) {
                    if (h.defender == 0) e.danoCpu += h.damage;
                    if (h.attacker != 0) continue;
                    e.danoJog += h.damage;
                    if ("jM".equals(h.moveId) && meatyJ) {
                        if (h.blocked) e.meatyJMDefende++; else e.meatyJMAcerta++;
                        meatyJ = false;
                    }
                    if ("2M".equals(h.moveId)) {
                        if (meatyR) {
                            if (h.blocked) e.meaty2MDefende++; else e.meaty2MAcerta++;
                        } else if (depoisJm) {
                            e.jmDepois2M++;
                            if (h.blocked) e.jmDepois2MDefende++; else e.jmDepois2MAcerta++;
                        }
                        meatyR = false;
                        depoisJm = false;
                    }
                }
                CombatFighter j = l.f(0);
                if (j.grounded && !j.attacking() && j.canAct()) { meatyR = meatyR && livreEm(l.e, c) >= 0; }
                if (j.grounded && j.canAct() && !j.attacking() && ro.st == 0) meatyJ = false;
            }
            e.lutas++;
            e.quadros += l.quadro;
            if (l.flow.winner() == 0) e.vitorias++;
        }
    }

    static String pc(int a, int b) { return BateriaVoadora.pc(a, b); }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        // modo: depois (padrão) | antes (sem levantar defendendo nem troca) | semTroca
        String modo = args.length > 1 ? args[1] : "depois";
        AiController.saidaLigada = !modo.equals("antes");
        AiController.trocaBaixaLigada = modo.equals("depois");
        Est[] porLuta = new Est[BateriaVoadora.NIVEIS.length];
        for (int i = 0; i < porLuta.length; i++) porLuta[i] = new Est();
        for (int k = 0; k < n; k++) arcade(k, porLuta);
        System.out.println("# O laço da luta real: rasteira, levantar com rasteira em cima da hora ou pulo com jM, rasteira depois do jM (" + n + " arcades, a mesma CPU nas 7 lutas; " + modo + ")");
        System.out.println();
        System.out.println("| luta (nível) | jogador vence | levantou: defendendo / atacando | rasteira no levantar: acerta / defendida | jM no levantar: acerta / defendido | rasteira depois do jM: acerta / defendida | dano do jogador por luta / da CPU |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (int i = 0; i < porLuta.length; i++) {
            Est e = porLuta[i];
            System.out.printf(Locale.ROOT, "| %d (%d) | %s | %s / %s | %s / %s | %s / %s | %s / %s | %.0f / %.0f |%n", i + 1, BateriaVoadora.NIVEIS[i],
                pc(e.vitorias, e.lutas), pc(e.levantouDefendendo, e.levantou), pc(e.levantouAtacando, e.levantou),
                pc(e.meaty2MAcerta, e.meaty2MAcerta + e.meaty2MDefende), pc(e.meaty2MDefende, e.meaty2MAcerta + e.meaty2MDefende),
                pc(e.meatyJMAcerta, e.meatyJMAcerta + e.meatyJMDefende), pc(e.meatyJMDefende, e.meatyJMAcerta + e.meatyJMDefende),
                pc(e.jmDepois2MAcerta, e.jmDepois2M), pc(e.jmDepois2MDefende, e.jmDepois2M),
                e.danoJog / (double) e.lutas, e.danoCpu / (double) e.lutas);
        }
    }
}
