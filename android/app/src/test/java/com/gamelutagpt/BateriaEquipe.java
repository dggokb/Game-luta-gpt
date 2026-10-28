package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Auditoria, frente C (docs/balanco/auditoria.md): Combo Breaker (T12), SHINKAI (T15, T16),
 * assistências (T17, T18), TAG (T19), DHC (T20) e duplas oficiais (T21), medidos no motor de
 * verdade com sementes fixas. Usa a base da {@link BateriaBalanco}. Roda à mão:
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaEquipe [t12|t15|t16|t17|t18|t19|t20|t21|tudo]</pre>
 */
public final class BateriaEquipe {
    private BateriaEquipe() {}

    static final int NIVEL = BateriaBalanco.NIVEL;
    static final String[] ELENCO = BateriaBalanco.ELENCO;
    static final String[][] DUPLAS = {
        {"player_base", "p02"}, {"p03", "p04"}, {"p05", "p06"}, {"p07", "p08"},
        {"p09", "p10"}, {"p11", "p12"}, {"p13", "p14"}, {"p15", "p16"}};

    static CharacterDefinition ch(String id) { return GeneratedCharacters.get(id); }

    static String nome(String id) { return BateriaBalanco.nome(id); }

    static String pct(double v) { return BateriaBalanco.pct(v); }

    static String f1(double v) { return BateriaBalanco.f1(v); }

    static void titulo(String s) { BateriaBalanco.titulo(s); }

    /** Luta com config própria e posições próprias. */
    static BateriaBalanco.Luta luta(String[] t0, String[] t1, long semente, CombatConfig cfg, float x0, float x1) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(t0, t1, NIVEL, semente, cfg);
        if (x0 != 420f || x1 != 980f) l.e.restage(x0, x1);
        return l;
    }

    // ================================================================== T12 Combo Breaker

    static final class Quebra {
        int danoAntes, danoTotal, troco, acerto;
        boolean quebrou, noAr;
    }

    /**
     * O combo do atacante num boneco que não defende; com {@code naAltura} > 0, o boneco quebra
     * quando o combo chega a esse acerto (0: nunca; -1: só no ar). Depois da quebra o boneco vira
     * CPU nível 8 para o troco.
     */
    static Quebra quebrar(String atk, long semente, int naAltura, boolean canto, CombatConfig cfg) {
        float x0 = canto ? Arena.LEFT_BOUND + 260f : 900f, x1 = canto ? Arena.LEFT_BOUND + 60f : 1040f;
        // No canto o boneco fica entre o atacante e a parede.
        BateriaBalanco.Luta l = luta(new String[] {atk}, new String[] {"player_base"}, semente, cfg, x0, x1);
        l.f(1).state.superMeter = CombatConfig.METER_PER_BAR;
        l.f(0).state.superMeter = 0;
        Quebra q = new Quebra();
        final boolean[] solto = {false};
        l.mao[1] = (lu, lado, in) -> {
            if (solto[0]) return;
            in.clear();
            lu.f(1).state.superMeter = CombatConfig.METER_PER_BAR;
            ComboSession s = lu.e.session(1);
            if (!lu.e.canBreakCombo(lu.f(1)) || s == null) return;
            boolean ar = !lu.f(1).grounded;
            if (naAltura > 0 && s.hitCount >= naAltura || naAltura < 0 && ar) {
                in.light = true;
                q.quebrou = true;
                q.noAr = ar;
                q.acerto = s.hitCount;
            }
        };
        l.mao[0] = (lu, lado, in) -> lu.f(0).state.superMeter = 0;
        ComboSession contra = null;
        int depois = -1;
        for (int fr = 0; fr < 60 * 30; fr++) {
            l.passo();
            ComboSession s = l.e.session(1);
            if (contra == null && s != null && s.active) contra = s;
            if (contra != null && depois < 0 && l.e.cues().contains("COMBO_BREAKER")) {
                q.danoAntes = contra.comboDamage;
                solto[0] = true;
                depois = fr;
                l.f(1).state.superMeter = 0;
            }
            if (contra != null && !contra.active && depois < 0) {
                if (contra.hitCount >= 2) {
                    q.danoTotal = contra.comboDamage;
                    q.quebrou = false;
                    return q;
                }
                contra = null;
                q.quebrou = false;
            }
            if (depois >= 0) {
                ComboSession fim = l.e.lastSession(0);
                ComboSession viva = l.e.session(0);
                if (fim != null && fim.attacker == 1 && (viva == null || !viva.active) && fr > depois + 5 &&
                    l.e.framesSinceSessionEnd(0) == 1) {
                    q.troco = fim.comboDamage;
                    return q;
                }
                if (fr > depois + 200) return q;
            }
        }
        return q;
    }

    static CombatConfig cfgBreaker(int atordoa, int corrida) {
        CombatConfig c = new CombatConfig();
        c.comboBreakerStunFrames = atordoa;
        c.comboBreakerChaseFrames = corrida;
        return c;
    }

    /** Vitórias do lado A (mão a) em n lutas espelhadas 2x2, com a config dada. */
    static double confronto(BateriaBalanco.Mao a, BateriaBalanco.Mao b, int n, long base, java.util.function.Supplier<CombatConfig> cfg) {
        int vence = 0;
        for (int i = 0; i < n; i++) {
            Random r = new Random(base + i);
            String[] dupla = BateriaBalanco.par(r);
            boolean troca = (i & 1) == 1;
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(dupla, dupla, NIVEL, base + i, cfg.get());
            l.mao[troca ? 1 : 0] = a;
            l.mao[troca ? 0 : 1] = b;
            if (l.rodar() == (troca ? 1 : 0)) vence++;
        }
        return vence / (double)n;
    }

    static void t12() {
        titulo("T12 Combo Breaker");
        System.out.println("#### Altura do combo e lugar (16 atacantes × 10 sementes; boneco com 1 barra, sem defender)");
        System.out.println();
        System.out.println("| quebra | lugar | quebras | levou antes | dano evitado | troco | troco ≥ 1 combo de 3 acertos |");
        System.out.println("|---|---|---|---|---|---|---|");
        Object[][] casos = {{2, false, "no 2º acerto"}, {3, false, "no 3º acerto"}, {4, false, "no 4º acerto"},
            {-1, false, "no ar"}, {2, true, "no 2º acerto"}, {-1, true, "no ar"}};
        for (Object[] c : casos) {
            int altura = (Integer)c[0];
            boolean canto = (Boolean)c[1];
            int n = 0, cheio = 0;
            double antes = 0, evitado = 0, troco = 0;
            for (String atk : ELENCO) {
                for (long s = 1; s <= 10; s++) {
                    Quebra sem = quebrar(atk, s, 0, canto, new CombatConfig());
                    Quebra com = quebrar(atk, s, altura, canto, new CombatConfig());
                    if (!com.quebrou || sem.danoTotal == 0) continue;
                    n++;
                    antes += com.danoAntes;
                    evitado += Math.max(0, sem.danoTotal - com.danoAntes);
                    troco += com.troco;
                    if (com.troco >= 900) cheio++;
                }
            }
            System.out.printf(Locale.ROOT, "| %s | %s | %d | %.0f | %.0f | %.0f | %s |%n", c[2], canto ? "canto" : "meio", n,
                antes / Math.max(1, n), evitado / Math.max(1, n), troco / Math.max(1, n), pct(cheio / (double)Math.max(1, n)));
        }

        System.out.println();
        System.out.println("#### A/B: regra atual × \"volta ao neutro\" (lutas 2x2 espelhadas, 240 por linha)");
        System.out.println();
        System.out.println("| regra | atordoamento | corrida automática | troco médio | sempre quebra vence nunca quebra |");
        System.out.println("|---|---|---|---|---|");
        int[][] regras = {{44, 40}, {24, 0}, {12, 0}};
        String[] nomes = {"atual", "meio-termo", "volta ao neutro"};
        for (int k = 0; k < regras.length; k++) {
            final int at = regras[k][0], co = regras[k][1];
            double troco = 0;
            int n = 0;
            for (String atk : ELENCO) {
                for (long s = 1; s <= 6; s++) {
                    Quebra com = quebrar(atk, s, 2, false, cfgBreaker(at, co));
                    if (!com.quebrou) continue;
                    troco += com.troco;
                    n++;
                }
            }
            double v = confronto(BateriaBalanco.SEMPRE_QUEBRA, BateriaBalanco.NUNCA_QUEBRA, 240, 110_000, () -> cfgBreaker(at, co));
            System.out.printf(Locale.ROOT, "| %s | %d quadros | %s | %.0f | %s |%n", nomes[k], at, co > 0 ? co + " quadros" : "não",
                troco / Math.max(1, n), pct(v));
        }
        double controle = confronto(BateriaBalanco.NUNCA_QUEBRA, BateriaBalanco.NUNCA_QUEBRA, 240, 110_000, CombatConfig::new);
        System.out.printf(Locale.ROOT, "%nControle (nunca × nunca, mesmas sementes): %s.%n", pct(controle));
    }

    // ================================================================== T15 SHINKAI

    static final class Janela {
        double danoFeito, danoLevado, barraGanha, barraPassiva, supers, distancia, defesas, quadros, vidaVolta, ecoDano;
        int n;
    }

    /**
     * 1x1 espelhado: o lado 0 (CPU nível 8) liga o SHINKAI do tipo dado no GO (ou não liga, com
     * tipo null); o lado 1 é a CPU nível 8 sem SHINKAI. Mede os 8 s do SHINKAI.
     */
    static void janelaShinkai(Janela j, String c, Shinkai.Type tipo, long semente, CombatConfig cfg) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {c}, new String[] {c}, NIVEL, semente, cfg);
        if (tipo != null) BateriaBalanco.forcarTipo(l.f(0).state, tipo);
        l.f(0).state.shinkai = tipo != null ? CombatConfig.SHINKAI_MAX : 0;
        final int[] ligou = {-1};
        l.mao[0] = (lu, lado, in) -> {
            if (tipo != null && ligou[0] < 0 && lu.e.teams.canShinkai(lu.f(0))) {
                in.shinkai = true;
                ligou[0] = lu.quadro;
            } else in.shinkai = false;
        };
        l.mao[1] = (lu, lado, in) -> {
            in.shinkai = false;
            lu.f(1).state.shinkai = 0;
        };
        // Sem SHINKAI: a mesma janela começa no GO.
        while (!l.acabou() && l.flow.phase() != BattleFlow.Phase.GO && l.flow.phase() != BattleFlow.Phase.FIGHT) l.passo();
        int inicio = l.quadro;
        if (tipo == null) ligou[0] = inicio;
        while (!l.acabou() && ligou[0] < 0 && l.quadro < inicio + 120) l.passo();
        if (ligou[0] < 0) return;
        int dano0 = l.est[0].dano + l.est[0].danoChip, dano1 = l.est[1].dano + l.est[1].danoChip;
        int defesas0 = l.est[0].defendidos;
        int vidaVermelha = l.f(0).state.recoverableLife;
        int barraAntes = l.f(0).state.superMeter;
        double ganha = 0, passiva = 0, dist = 0, supers = 0;
        int quadros = 0, vidaAntes = l.f(0).state.life;
        int fim = l.quadro + cfg.shinkaiFrames + cfg.shinkaiFlashFrames;
        int levado = 0;
        while (!l.acabou() && l.quadro < fim) {
            int barra = l.f(0).state.superMeter;
            boolean dentro = l.f(0).state.inShinkai() && !l.e.superFreezeActive();
            l.passo();
            int d = l.f(0).state.superMeter - barra;
            if (d > 0) ganha += d;
            if (dentro && tipo == Shinkai.Type.ENERGY) passiva += cfg.shinkaiEnergyMeterPerFrame;
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.attacker == 0 && "SUPER".equals(h.moveId)) supers++;
                if (h.attacker == 1) levado += h.damage;
            }
            dist += Math.abs(l.f(0).x - l.f(1).x);
            quadros++;
        }
        j.danoFeito += l.est[0].dano + l.est[0].danoChip - dano0;
        j.danoLevado += levado;
        j.defesas += l.est[1].defendidos - 0;
        j.barraGanha += ganha;
        j.barraPassiva += passiva;
        j.supers += supers;
        j.distancia += dist / Math.max(1, quadros);
        j.quadros += quadros;
        j.vidaVolta += Math.max(0, l.f(0).state.life - (vidaAntes - levado));
        j.n++;
    }

    static void t15() {
        titulo("T15 Valor dos quatro SHINKAI (F02 ENERGY, F03 eco SPEED)");
        System.out.println("1x1 espelhado nos 16 lutadores (15 sementes cada): o lado A liga o SHINKAI no GO, o lado B (mesma CPU)");
        System.out.println("não tem. Janela de 8 s + o brilho. \"sem\" = a mesma janela sem ligar nada.");
        System.out.println();
        System.out.println("| tipo | dano de A | dano em A | saldo | saldo a mais que sem | barra ganha (A) | da barra passiva | Supers de A | vida que voltou | distância média | defesas de B |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|");
        Shinkai.Type[] tipos = {null, Shinkai.Type.POWER, Shinkai.Type.SPEED, Shinkai.Type.ENERGY, Shinkai.Type.TECHNIQUE};
        double saldoSem = 0;
        for (Shinkai.Type t : tipos) {
            Janela j = new Janela();
            for (String c : ELENCO) for (long s = 1; s <= 15; s++) janelaShinkai(j, c, t, 120_000 + s * 31 + c.hashCode(), new CombatConfig());
            double n = Math.max(1, j.n), saldo = (j.danoFeito - j.danoLevado) / n;
            if (t == null) saldoSem = saldo;
            System.out.printf(Locale.ROOT, "| %s | %.0f | %.0f | %+.0f | %s | %.2f barra | %.2f barra | %.2f | %.0f | %.0f px | %.1f |%n",
                t == null ? "sem" : t.label, j.danoFeito / n, j.danoLevado / n, saldo, t == null ? "–" : String.format(Locale.ROOT, "%+.0f", saldo - saldoSem),
                j.barraGanha / n / 1000, j.barraPassiva / n / 1000, j.supers / n, j.vidaVolta / n, j.distancia / n, j.defesas / n);
        }
        // F03: o eco do SPEED desligado nas mesmas sementes.
        Janela com = new Janela(), sem = new Janela();
        for (String c : ELENCO) {
            for (long s = 1; s <= 15; s++) {
                long semente = 120_000 + s * 31 + c.hashCode();
                janelaShinkai(com, c, Shinkai.Type.SPEED, semente, new CombatConfig());
                CombatConfig semEco = new CombatConfig();
                semEco.shinkaiSpeedEchoDamagePermille = 0;
                janelaShinkai(sem, c, Shinkai.Type.SPEED, semente, semEco);
            }
        }
        System.out.printf(Locale.ROOT, "%nF03 eco do SPEED: dano de A em 8 s %.0f com o eco, %.0f com o eco sem dano (%+.0f, %s).%n",
            com.danoFeito / com.n, sem.danoFeito / sem.n, (com.danoFeito - sem.danoFeito) / com.n,
            pct((com.danoFeito - sem.danoFeito) / Math.max(1, sem.danoFeito)));
        CombatConfig c = new CombatConfig();
        System.out.printf(Locale.ROOT, "F02 conta do ENERGY: %d por quadro × %d quadros = %.2f barra (o relógio do SHINKAI para nos congelamentos).%n",
            c.shinkaiEnergyMeterPerFrame, c.shinkaiFrames, c.shinkaiEnergyMeterPerFrame * c.shinkaiFrames / 1000.0);
    }

    // ================================================================== T16 resposta ao SHINKAI

    static final String[] RESPOSTAS = {"CPU nível 8", "defesa perfeita", "fugir (trás + backdash)", "agarrão de perto", "parado", "TAG na hora"};

    /** Dano que B leva nos 8 s do SHINKAI de A (CPU nível 8), com a resposta dada. */
    static double[] resposta(String c, Shinkai.Type tipo, int qual, boolean canto, long semente) {
        boolean dupla = qual == 5;
        String[] tB = dupla ? new String[] {c, BateriaBalanco.parceiro(c)} : new String[] {c};
        float x0 = canto ? Arena.LEFT_BOUND + 300f : 900f, x1 = canto ? Arena.LEFT_BOUND + 40f : 1260f;
        BateriaBalanco.Luta l = luta(new String[] {c}, tB, semente, new CombatConfig(), x0, x1);
        if (tipo != null) BateriaBalanco.forcarTipo(l.f(0).state, tipo);
        l.f(0).state.shinkai = tipo != null ? CombatConfig.SHINKAI_MAX : 0;
        final int[] ligou = {-1};
        l.mao[0] = (lu, lado, in) -> {
            if (tipo != null && ligou[0] < 0 && lu.e.teams.canShinkai(lu.f(0))) {
                in.shinkai = true;
                ligou[0] = lu.quadro;
            } else in.shinkai = false;
        };
        final int[] k = {0};
        l.mao[1] = (lu, lado, in) -> {
            lu.f(1).state.shinkai = 0;
            in.shinkai = false;
            if (qual == 0) return;
            CombatFighter me = lu.f(1), o = lu.f(0);
            float gap = Math.abs(o.x - me.x);
            int tras = o.x >= me.x ? 5 : 1;
            k[0]++;
            switch (qual) {
                case 1: in.clear(); BateriaBalanco.defender(lu, 1, in); break;
                case 2:
                    in.clear();
                    in.direction = tras;
                    if (k[0] % 25 == 0 && gap < 260) in.backdash = true;
                    if (me.status == CombatFighter.Status.BLOCKSTUN || o.attacking()) BateriaBalanco.defender(lu, 1, in);
                    break;
                case 3:
                    in.clear();
                    BateriaBalanco.defender(lu, 1, in);
                    if (gap < 130 && me.canAct() && !o.attacking()) { in.direction = 0; in.grab = true; }
                    break;
                case 4: in.clear(); break;
                case 5:
                    in.clear();
                    BateriaBalanco.defender(lu, 1, in);
                    if (ligou[0] >= 0 && lu.quadro >= ligou[0] + 50 && me.canAct()) in.tag = true;
                    break;
                default: break;
            }
        };
        while (!l.acabou() && l.flow.phase() != BattleFlow.Phase.FIGHT && l.flow.phase() != BattleFlow.Phase.GO) l.passo();
        int inicio = l.quadro;
        while (!l.acabou() && tipo != null && ligou[0] < 0 && l.quadro < inicio + 120) l.passo();
        CombatConfig cfg = l.e.config;
        int fim = l.quadro + cfg.shinkaiFrames + cfg.shinkaiFlashFrames;
        double levou = 0, deu = 0;
        while (!l.acabou() && l.quadro < fim) {
            l.passo();
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.attacker == 0) levou += h.damage;
                else deu += h.damage;
            }
        }
        return new double[] {levou, deu};
    }

    static void t16() {
        titulo("T16 Resposta contra cada SHINKAI");
        System.out.println("A (CPU nível 8) liga o SHINKAI no GO; B responde por 8 s. Dano que B leva / que B dá (16 lutadores × 6 sementes).");
        System.out.println("\"sem\" = A sem SHINKAI, mesma resposta. No canto, B começa encostado na parede.");
        System.out.println();
        for (boolean canto : new boolean[] {false, true}) {
            System.out.println("##### " + (canto ? "No canto" : "No meio"));
            System.out.println();
            StringBuilder cab = new StringBuilder("| resposta de B | sem |"), sep = new StringBuilder("|---|---|");
            Shinkai.Type[] tipos = Shinkai.Type.values();
            for (Shinkai.Type t : tipos) { cab.append(' ').append(t.label).append(" |"); sep.append("---|"); }
            System.out.println(cab);
            System.out.println(sep);
            for (int q = 0; q < RESPOSTAS.length; q++) {
                StringBuilder linha = new StringBuilder("| " + RESPOSTAS[q] + " |");
                for (int ti = -1; ti < tipos.length; ti++) {
                    Shinkai.Type t = ti < 0 ? null : tipos[ti];
                    double lev = 0, deu = 0;
                    int n = 0;
                    for (String c : ELENCO) {
                        for (long s = 1; s <= 6; s++) {
                            double[] r = resposta(c, t, q, canto, 130_000 + s * 17 + c.hashCode());
                            lev += r[0];
                            deu += r[1];
                            n++;
                        }
                    }
                    linha.append(String.format(Locale.ROOT, " %.0f / %.0f |", lev / n, deu / n));
                }
                System.out.println(linha);
            }
            System.out.println();
        }
    }

    // ================================================================== T17 assistências

    static final class Assist {
        String golpe, tipo;
        int inicio = -1, alcanceMin = -1, alcanceMax = -1, dano, hitstun, blockstun;
        double antiAereo;
        int vantagemSemAssist = Integer.MIN_VALUE, vantagemComAssist = Integer.MIN_VALUE;
        boolean lanca;
    }

    /** Chama o assist de {@code x} (titular p01) com o boneco a {@code gap}; quadros até acertar (-1: não acertou). */
    static int[] chamarAssist(String x, float gap, int boneco, int atraso) {
        String titular = x.equals("player_base") ? "p02" : "player_base";
        BateriaBalanco.Luta l = luta(new String[] {titular, x}, new String[] {"player_base"}, 1, new CombatConfig(), 900f, 900f + gap);
        final int[] f = {-1, -1};
        l.mao[0] = (lu, lado, in) -> {
            in.clear();
            if (f[0] < 0 && lu.f(0).canAct() && lu.flow.phase() == BattleFlow.Phase.FIGHT && lu.quadro >= f[1] + atraso && f[1] >= 0) {
                in.assist = true;
                f[0] = lu.quadro;
            }
        };
        l.mao[1] = (lu, lado, in) -> {
            in.clear();
            CombatFighter me = lu.f(1);
            if (lu.flow.phase() == BattleFlow.Phase.FIGHT && f[1] < 0) f[1] = lu.quadro;
            if (boneco == 1) BateriaBalanco.defender(lu, 1, in);
            if (boneco == 2 && me.grounded && me.canAct()) { in.jump = true; in.direction = 7; }
        };
        for (int fr = 0; fr < 60 * 6; fr++) {
            l.passo();
            if (f[0] < 0) continue;
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.attacker != 0) continue;
                CombatFighter d = l.f(1);
                return new int[] {l.quadro - f[0], h.damage, h.blocked ? d.stunTotal : 0, h.blocked ? 0 : d.stunTotal, d.grounded ? 0 : 1, h.blocked ? 1 : 0};
            }
            if (l.quadro > f[0] + 150) break;
        }
        return null;
    }

    /** Vantagem do titular (p01) num H defendido, com ou sem o assist chamado junto. */
    static int vantagemCobertura(String x, boolean comAssist) {
        BateriaBalanco.Luta l = luta(new String[] {"player_base", x}, new String[] {"player_base"}, 1, new CombatConfig(), 900f, 1040f);
        final int[] f = {-1};
        l.mao[0] = (lu, lado, in) -> {
            in.clear();
            if (f[0] < 0 && lu.f(0).canAct() && lu.flow.phase() == BattleFlow.Phase.FIGHT) {
                in.heavy = true;
                in.assist = comAssist;
                f[0] = lu.quadro;
            }
        };
        l.mao[1] = (lu, lado, in) -> { in.clear(); BateriaBalanco.defender(lu, 1, in); };
        int livreA = -1, livreD = -1, defendeu = -1;
        for (int fr = 0; fr < 600; fr++) {
            l.passo();
            if (f[0] < 0) continue;
            for (CombatEngine.HitEvent h : l.e.events()) if (h.attacker == 0 && h.blocked) defendeu = l.quadro;
            if (defendeu < 0) continue;
            CombatFighter d = l.f(1);
            boolean preso = d.status == CombatFighter.Status.BLOCKSTUN || d.frozen() || !d.canAct();
            if (preso) livreD = -1;
            else if (livreD < 0) livreD = l.quadro;
            if (livreA < 0 && l.f(0).canAct() && l.f(0).status == CombatFighter.Status.NEUTRAL) livreA = l.quadro;
            if (livreA >= 0 && livreD >= 0 && !l.e.team(0).assistOut() && l.quadro > livreD + 2) break;
        }
        if (livreA < 0 || livreD < 0) return Integer.MIN_VALUE;
        return livreD - livreA;
    }

    static void t17() {
        titulo("T17 Função das assistências (F06)");
        System.out.println("O assist de cada um, chamado pelo Ryūga (o assist do próprio Ryūga é chamado pela Yin-lu).");
        System.out.println("Início = quadros do aperto até o 1º acerto no boneco mais perto que ele alcança. Alcance = distâncias (do titular) em");
        System.out.println("que acerta o boneco parado, de 100 a 1000 px (o assist aparece 70 px atrás do titular). Antiaéreo = em quantos de 12 tempos");
        System.out.println("de chamada ele acerta o boneco que fica pulando parado a 250 px (cobertura vertical, não um antiaéreo de reação).");
        System.out.println("Cobertura = vantagem do titular num H defendido sem / com o assist chamado junto. O assist não tem corpo que o rival");
        System.out.println("acerte (CombatEngine só testa o golpe dele contra o rival), então ele mesmo nunca é punido.");
        System.out.println();
        System.out.println("| lutador | golpe | tipo | início | alcance | dano | hitstun | blockstun | antiaéreo | cobertura (H defendido) | função |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|");
        for (String x : ELENCO) {
            CharacterDefinition c = ch(x);
            String mv = c.fighter.assistMove;
            AttackDefinition at = "S".equals(mv) && c.fighter.energy != null ? c.fighter.energy.attack : c.attack(mv);
            String tipo = at == null ? "?" : at.melee() ? "corpo a corpo" : "projétil" + (at.low ? " baixo" : "");
            int inicio = -1, min = -1, max = -1, dano = 0, hs = 0, bs = 0;
            for (int g = 100; g <= 1000; g += 50) {
                int[] r = chamarAssist(x, g, 0, 0);
                if (r == null) continue;
                if (min < 0) { min = g; inicio = r[0]; dano = r[1]; hs = r[3]; }
                max = g;
            }
            int[] blk = chamarAssist(x, Math.max(150, min < 0 ? 200 : min), 1, 0);
            if (blk != null) bs = blk[2];
            int aa = 0;
            for (int atraso = 0; atraso < 48; atraso += 4) {
                int[] r = chamarAssist(x, 250, 2, atraso);
                if (r != null && r[4] == 1) aa++;
            }
            int semA = vantagemCobertura(x, false), comA = vantagemCobertura(x, true);
            String funcao;
            double aaf = aa / 12.0;
            if (min < 0) funcao = "não acerta parado";
            else if (!at.melee() && max - min >= 500) funcao = aaf >= 0.5 ? "projétil com antiaéreo" : "projétil (controle de chão)";
            else if (aaf >= 0.5) funcao = "antiaéreo";
            else if (max <= 350) funcao = "extensão de perto";
            else funcao = "investida / alcance médio";
            System.out.printf(Locale.ROOT, "| %s | %s (%s) | %s | %s | %s | %d | %d | %d | %s | %s / %s | %s |%n", nome(x), mv, at == null ? "?" : at.id, tipo,
                inicio < 0 ? "–" : inicio + " q", min < 0 ? "–" : min + "–" + max + " px", dano, hs, bs, pct(aaf),
                semA == Integer.MIN_VALUE ? "–" : String.format(Locale.ROOT, "%+d", semA), comA == Integer.MIN_VALUE ? "–" : String.format(Locale.ROOT, "%+d", comA), funcao);
        }
    }

    // ================================================================== T18 assist nos combos

    /** Igual a BateriaBalanco.executar, com o parceiro escolhido e Ultra/DHC. */
    static BateriaBalanco.Execucao executar(String atk, String parceiro, List<String> rota, int barras, int[] extra) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), p = new FighterState(ch(parceiro), "P1");
        FighterState d = new FighterState(ch("player_base"), "P2");
        CombatEngine e = new CombatEngine(a, d, 900f, 1040f, cfg);
        e.setTeam(0, a, p);
        // Para medir a vida recuperável o boneco fica com a vida normal (o combo não chega a zerar).
        if (extra == null) d.life = 10_000_000;
        a.superMeter = barras * CombatConfig.METER_PER_BAR;
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        BateriaBalanco.Execucao x = new BateriaBalanco.Execucao();
        ComboSession sess = null;
        CombatFighter me = e.fighter(0), dummy = e.fighter(1);
        int congelado = 0, gasto = 0;
        for (int k = 0; k < rota.size(); k++) {
            String acao = rota.get(k);
            int espera = acao.equals("W6") ? 6 : acao.equals("W14") ? 14 : 0;
            AttackDefinition antes = me.attack;
            int antesQuadro = me.attackFrame;
            FighterState estado = me.state;
            boolean feito = false;
            int limite = acao.equals("DHC") || acao.equals("ULTRA") ? 150 : 45;
            for (int t = 0; t < limite && !feito; t++) {
                in0.clear();
                in1.clear();
                if (dummy.status == CombatFighter.Status.AIR_HITSTUN && dummy.stunLeft == 0) in1.light = true;
                int frente = me.facing > 0 ? 1 : 5;
                if (espera == 0) {
                    switch (acao) {
                        case "L": in0.light = true; break;
                        case "M": in0.medium = true; break;
                        case "H": in0.heavy = true; break;
                        case "2L": in0.direction = me.facing > 0 ? 2 : 4; in0.light = true; break;
                        case "2M": in0.direction = me.facing > 0 ? 2 : 4; in0.medium = true; break;
                        case "2H": in0.direction = me.facing > 0 ? 2 : 4; in0.heavy = true; break;
                        case "S": in0.special = "M"; break;
                        case "SUPER": in0.superAttack = true; break;
                        case "ULTRA": in0.ultra = true; in0.superAttack = true; in0.direction = 3; break;
                        case "A": case "DHC": in0.assist = true; break;
                        case "J": in0.jump = true; in0.direction = me.facing > 0 ? 8 : 6; break;
                        case "SJ": in0.superJump = true; in0.direction = me.facing > 0 ? 8 : 6; break;
                        case "D": in0.dash = true; in0.direction = frente; break;
                        case "jL": in0.light = true; break;
                        case "jM": in0.medium = true; break;
                        case "jH": in0.heavy = true; break;
                        case "j2H": in0.direction = 3; in0.heavy = true; break;
                        default: break;
                    }
                }
                boolean noChao = me.grounded;
                boolean assistAntes = e.team(0).assistOut();
                int barra = me.state.superMeter;
                e.step(in0, in1);
                if (me.state.superMeter < barra) gasto += barra - me.state.superMeter;
                if (e.superFreezeActive()) congelado++;
                if (e.ultraConnected() >= 0) BateriaBalanco.ultraSemPagina(e, e.ultraConnected());
                ComboSession s = e.session(1);
                if (sess == null && s != null && s.active) sess = s;
                if (sess != null && s != sess && k > 0) return x;
                if (espera > 0) {
                    feito = t + 1 >= espera;
                    continue;
                }
                boolean novo = me.attacking() && me.attack != null && (me.attack != antes || me.attackFrame < antesQuadro);
                switch (acao) {
                    case "J": case "SJ": feito = noChao && !me.grounded; break;
                    case "D": feito = me.forwardDashing; break;
                    case "A": feito = !assistAntes && e.team(0).assistOut(); break;
                    case "DHC": feito = me.state != estado; break;
                    case "ULTRA": feito = me.status == CombatFighter.Status.ULTRA || me.inUltraSequence() || e.ultraConnected() >= 0; break;
                    case "S": feito = novo && me.attack.id.startsWith("S") && me.attack.kind != AttackDefinition.Kind.SUPER; break;
                    case "SUPER": feito = novo && me.attack.kind == AttackDefinition.Kind.SUPER; break;
                    default: feito = novo && me.attack.id.equals(acao); break;
                }
                if (feito) {
                    antes = me.attack;
                    antesQuadro = me.attackFrame;
                }
            }
            if (!feito) return x;
            if (k == 0 && sess == null) {
                for (int t = 0; t < 30 && sess == null; t++) {
                    in0.clear();
                    e.step(in0, in1);
                    ComboSession s = e.session(1);
                    if (s != null && s.active) sess = s;
                }
                if (sess == null) return x;
            }
            if (sess != null && !sess.active) return x;
            x.assist |= acao.equals("A");
        }
        for (int t = 0; t < 600 && sess != null && (sess.active || me.inUltraSequence() || me.status == CombatFighter.Status.ULTRA); t++) {
            in0.clear();
            in1.clear();
            if (dummy.status == CombatFighter.Status.AIR_HITSTUN && dummy.stunLeft == 0) in1.light = true;
            int barra = me.state.superMeter;
            FighterState estado = me.state;
            e.step(in0, in1);
            if (me.state.superMeter < barra) gasto += barra - me.state.superMeter;
            if (e.superFreezeActive()) congelado++;
            if (e.ultraConnected() >= 0) BateriaBalanco.ultraSemPagina(e, e.ultraConnected());
        }
        if (sess == null) return x;
        x.valida = sess.hitCount >= 2;
        x.dano = sess.comboDamage;
        x.acertos = sess.hitCount;
        x.parede = sess.wallBounces;
        x.chao = sess.groundBounces;
        x.duracao = sess.comboDuration;
        x.superUsado = sess.usesOf("SUPER") > 0;
        if (extra != null) {
            extra[0] = congelado;
            extra[1] = gasto;
            extra[2] = d.recoverableLife;
            extra[3] = Math.round(Math.abs(dummy.x - me.x));
            extra[4] = Math.round(Math.min(dummy.x - Arena.LEFT_BOUND, Arena.RIGHT_BOUND - dummy.x));
        }
        return x;
    }

    static final String[] ACOES = {"L", "M", "H", "2L", "2M", "2H", "S", "SUPER", "A", "J", "SJ", "D", "jL", "jM", "jH", "j2H", "W6", "W14"};

    static final class Cand {
        final List<String> rota;
        final BateriaBalanco.Execucao x;
        final double nota;

        Cand(List<String> rota, BateriaBalanco.Execucao x, double nota) { this.rota = rota; this.x = x; this.nota = nota; }
    }

    /** Busca em feixe (como BateriaBalanco.buscar) com o parceiro escolhido; {@code soComAssist}: só vale rota com assist. */
    static Cand buscar(String atk, String parceiro, int barras, boolean soComAssist, boolean comSuper, int largura, int fundo) {
        List<Cand> feixe = new ArrayList<>();
        feixe.add(new Cand(new ArrayList<>(), new BateriaBalanco.Execucao(), 0));
        Cand melhor = null;
        java.util.Set<String> vistos = new java.util.HashSet<>();
        for (int nivel = 0; nivel < fundo; nivel++) {
            List<Cand> filhos = new ArrayList<>();
            for (Cand c : feixe) {
                for (String acao : ACOES) {
                    if (!comSuper && acao.equals("SUPER")) continue;
                    if (nivel == 0 && !(acao.equals("L") || acao.equals("2L") || acao.equals("M") || acao.equals("2M") || acao.equals("H"))) continue;
                    List<String> r = new ArrayList<>(c.rota);
                    r.add(acao);
                    BateriaBalanco.Execucao x = executar(atk, parceiro, r, barras, null);
                    if (!x.valida && nivel > 0) continue;
                    int n = r.size();
                    String chave = x.dano + "/" + x.acertos + "/" + x.parede + x.chao + (x.assist ? "A" : "") + "/" + n + "/" +
                        String.join(",", r.subList(Math.max(0, n - 3), n));
                    if (!vistos.add(chave)) continue;
                    double nota = x.dano + (soComAssist && x.assist ? 3000 : 0);
                    Cand f = new Cand(r, x, nota);
                    filhos.add(f);
                    if (x.valida && (!soComAssist || x.assist) && (melhor == null || x.dano > melhor.x.dano)) melhor = f;
                }
            }
            if (filhos.isEmpty()) break;
            filhos.sort((p, q) -> q.nota != p.nota ? Double.compare(q.nota, p.nota) : p.x.acertos != q.x.acertos ? q.x.acertos - p.x.acertos
                : String.join(",", p.rota).compareTo(String.join(",", q.rota)));
            feixe = filhos.subList(0, Math.min(largura, filhos.size()));
        }
        return melhor;
    }

    static void t18() {
        titulo("T18 Assistência nos combos");
        System.out.println("Busca em feixe (largura 40, 14 passos, sem Super, sem barra) do maior combo de cada titular com cada parceiro;");
        System.out.println("\"sem assist\" é a mesma busca proibindo o assist. Ganho = maior com assist − maior sem assist.");
        System.out.println();
        String[] titulares = {"player_base", "p03", "p09", "p11"};
        for (String t : titulares) {
            Cand semA = buscar(t, BateriaBalanco.parceiro(t), 0, false, false, 40, 14);
            // sem assist: busca sem a ação A.
            int semAssist = semAssistMax(t);
            List<String[]> linhas = new ArrayList<>();
            List<String> parceiros = new ArrayList<>();
            for (String x : ELENCO) if (!x.equals(t)) parceiros.add(x);
            List<String> res = parceiros.parallelStream().map(x -> {
                Cand c = buscar(t, x, 0, true, false, 40, 14);
                return x + "\t" + (c == null ? 0 : c.x.dano) + "\t" + (c == null ? "–" : String.join(" ", c.rota));
            }).collect(java.util.stream.Collectors.toList());
            res.sort((p, q) -> Integer.parseInt(q.split("\t")[1]) - Integer.parseInt(p.split("\t")[1]));
            System.out.println("##### Titular " + nome(t) + " (maior sem assist: " + semAssist + ")");
            System.out.println();
            System.out.println("| parceiro | maior combo com assist | ganho | rota |");
            System.out.println("|---|---|---|---|");
            for (String r : res) {
                String[] p = r.split("\t");
                int dano = Integer.parseInt(p[1]);
                System.out.printf(Locale.ROOT, "| %s%s | %d | %+d | %s |%n", nome(p[0]), p[0].equals(BateriaBalanco.parceiro(t)) ? " (oficial)" : "",
                    dano, dano - semAssist, p[2]);
            }
            System.out.println();
        }
    }

    static int semAssistMax(String t) {
        // A mesma busca com o parceiro que nunca entra: sem a ação A no catálogo.
        List<Cand> feixe = new ArrayList<>();
        feixe.add(new Cand(new ArrayList<>(), new BateriaBalanco.Execucao(), 0));
        int melhor = 0;
        java.util.Set<String> vistos = new java.util.HashSet<>();
        for (int nivel = 0; nivel < 14; nivel++) {
            List<Cand> filhos = new ArrayList<>();
            for (Cand c : feixe) {
                for (String acao : ACOES) {
                    if (acao.equals("A") || acao.equals("SUPER")) continue;
                    if (nivel == 0 && !(acao.equals("L") || acao.equals("2L") || acao.equals("M") || acao.equals("2M") || acao.equals("H"))) continue;
                    List<String> r = new ArrayList<>(c.rota);
                    r.add(acao);
                    BateriaBalanco.Execucao x = executar(t, BateriaBalanco.parceiro(t), r, 0, null);
                    if (!x.valida && nivel > 0) continue;
                    int n = r.size();
                    String chave = x.dano + "/" + x.acertos + "/" + n + "/" + String.join(",", r.subList(Math.max(0, n - 3), n));
                    if (!vistos.add(chave)) continue;
                    filhos.add(new Cand(r, x, x.dano));
                    if (x.valida) melhor = Math.max(melhor, x.dano);
                }
            }
            if (filhos.isEmpty()) break;
            filhos.sort((p, q) -> q.nota != p.nota ? Double.compare(q.nota, p.nota) : p.x.acertos != q.x.acertos ? q.x.acertos - p.x.acertos
                : String.join(",", p.rota).compareTo(String.join(",", q.rota)));
            feixe = filhos.subList(0, Math.min(40, filhos.size()));
        }
        return melhor;
    }

    // ================================================================== T19 TAG

    /** TAG direto (↓+TAG) com o rival a {@code gap}: o rival parado, defendendo ou pulando para trás. */
    static String[] tagDireto(float gap, int rival) {
        BateriaBalanco.Luta l = luta(new String[] {"player_base", "p02"}, new String[] {"player_base"}, 1, new CombatConfig(), 900f, 900f + gap);
        final int[] f = {-1, -1, 0};
        l.mao[0] = (lu, lado, in) -> {
            in.clear();
            if (f[0] < 0 && lu.f(0).canAct() && lu.flow.phase() == BattleFlow.Phase.FIGHT) { in.tag = true; f[0] = lu.quadro; }
        };
        l.mao[1] = (lu, lado, in) -> {
            in.clear();
            CombatFighter me = lu.f(1), o = lu.f(0);
            if (rival == 1) BateriaBalanco.defender(lu, 1, in);
            if (rival == 2 && f[0] >= 0 && me.grounded && me.canAct() && f[2] == 0) { in.backdash = true; f[2] = 1; }
            // Punição: livre com o rival perto e ainda preso, aperta o golpe mais forte que alcança.
            if (f[1] >= 0 && me.canAct()) {
                float d = Math.abs(o.x - me.x);
                if (d < 160) { in.direction = 0; in.light = true; }
            }
        };
        String resultado = "erro";
        int kickFim = -1, livreRival = -1, livreNovo = -1;
        boolean punido = false;
        FighterState antes = l.e.team(0).pointState();
        for (int fr = 0; fr < 60 * 6; fr++) {
            l.passo();
            if (f[0] < 0) continue;
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.attacker == 0 && "TAG_KICK".equals(h.moveId)) resultado = h.blocked ? "defendida" : "acertou";
                if (h.attacker == 1 && !h.blocked && kickFim >= 0) punido = true;
            }
            CombatFighter p = l.f(0);
            boolean chutando = p.attacking() && p.attack != null && VoadoraTroca.ID.equals(p.attack.id);
            if (l.e.team(0).pointState() != antes && !chutando && kickFim < 0 && l.e.team(0).tagPhase == TeamSystem.TAG_NONE) {
                kickFim = l.quadro;
                f[1] = 1;
            }
            if (kickFim >= 0) {
                if (livreNovo < 0 && p.canAct()) livreNovo = l.quadro;
                if (livreRival < 0 && l.f(1).canAct()) livreRival = l.quadro;
                if (l.quadro > kickFim + 60) break;
            }
        }
        if (resultado.equals("erro") && kickFim >= 0) resultado = "errou";
        int vant = livreNovo >= 0 && livreRival >= 0 ? livreRival - livreNovo : Integer.MIN_VALUE;
        return new String[] {resultado, kickFim < 0 ? "–" : Integer.toString(kickFim - f[0]),
            vant == Integer.MIN_VALUE ? "–" : String.format(Locale.ROOT, "%+d", vant), punido ? "sim" : "não"};
    }

    static void t19() {
        titulo("T19 Segurança do TAG");
        System.out.println("#### TAG direto (↓+TAG): Ryūga sai, a Yin-lu entra na voadora. Vantagem: + quem entrou age primeiro;");
        System.out.println("\"–\" = o rival ainda estava no chão/atordoado 1 s depois.");
        System.out.println();
        System.out.println("| distância | rival | voadora | quadros do aperto até ela acabar | vantagem de quem entrou |");
        System.out.println("|---|---|---|---|---|");
        String[] rivais = {"parado", "defendendo", "backdash"};
        for (float gap : new float[] {150, 350, 600, 900}) {
            for (int r = 0; r < 3; r++) {
                String[] t = tagDireto(gap, r);
                System.out.printf(Locale.ROOT, "| %.0f | %s | %s | %s | %s |%n", gap, rivais[r], t[0], t[1], t[2]);
            }
        }

        System.out.println();
        System.out.println("#### Assist → TAG perdido: o que o titular fazia no quadro em que o assist acabou (lutas CPU × CPU, 400)");
        System.out.println();
        Map<String, Integer> causa = new java.util.TreeMap<>();
        int pedidos = 0, perdidos = 0, gc = 0, gcAcertou = 0, gcPunido = 0, gcBarra = 0, dhc = 0;
        for (int i = 0; i < 400; i++) {
            Random r = new Random(140_000 + i);
            String[] d0 = BateriaBalanco.par(r), d1 = BateriaBalanco.par(r);
            BateriaBalanco.Luta lu = new BateriaBalanco.Luta(d0, d1, NIVEL, 140_000 + i, new CombatConfig());
            int[] fase = {TeamSystem.ASSIST_NONE, TeamSystem.ASSIST_NONE};
            boolean[] conv = new boolean[2];
            int[] gcQuadro = {-1, -1};
            while (!lu.acabou() && lu.quadro < 60 * 60 * 4) {
                lu.passo();
                for (int lado = 0; lado < 2; lado++) {
                    TeamSystem.Side s = lu.e.team(lado);
                    if (s.convert && !conv[lado]) pedidos++;
                    if (fase[lado] == TeamSystem.ASSIST_ATTACK && s.assistPhase == TeamSystem.ASSIST_LEAVE && s.convert) {
                        perdidos++;
                        CombatFighter p = lu.f(lado);
                        String k = p.status + (p.grounded ? "" : " no ar");
                        causa.merge(k, 1, Integer::sum);
                    }
                    conv[lado] = s.convert;
                    fase[lado] = s.assistPhase;
                    if (gcQuadro[lado] >= 0) {
                        for (CombatEngine.HitEvent h : lu.e.events()) {
                            if (h.attacker == lado && !h.blocked && lu.quadro - gcQuadro[lado] < 40) { gcAcertou++; gcQuadro[lado] = -2 - lu.quadro; }
                        }
                    }
                }
                if (lu.e.cues().contains("GUARD_CANCEL")) {
                    gc++;
                    for (int lado = 0; lado < 2; lado++) {
                        if (lu.f(lado).attacking() && lu.f(lado).invulnFrames > 0) gcQuadro[lado] = lu.quadro;
                    }
                }
                if (lu.e.cues().contains("DHC")) dhc++;
                for (int lado = 0; lado < 2; lado++) {
                    int q = gcQuadro[lado];
                    int desde = q >= 0 ? lu.quadro - q : q < -1 ? lu.quadro - (-2 - q) : -1;
                    if (desde >= 0 && desde < 90) {
                        for (CombatEngine.HitEvent h : lu.e.events()) if (h.attacker == 1 - lado && !h.blocked) { gcPunido++; gcQuadro[lado] = -1; }
                    } else if (desde >= 90) gcQuadro[lado] = -1;
                }
            }
        }
        System.out.printf(Locale.ROOT, "%d pedidos, %d perdidos (%s). Situação do titular nos perdidos: %s.%n", pedidos, perdidos,
            pct(perdidos / (double)Math.max(1, pedidos)), causa);
        System.out.printf(Locale.ROOT, "%nGuard Cancel TAG nas mesmas lutas: %d (%.2f por luta), acertou em %s, quem entrou levou golpe em até 1,5 s em %s. DHC: %d.%n",
            gc, gc / 400.0, pct(gcAcertou / (double)Math.max(1, gc)), pct(gcPunido / (double)Math.max(1, gc)), dhc);

        System.out.println();
        System.out.println("#### TAG apertado no congelamento do acerto (H acertando o boneco; aperto em cada quadro do congelamento)");
        System.out.println();
        System.out.println("| botão | apertos no congelamento | saiu | quadros até sair |");
        System.out.println("|---|---|---|---|");
        for (int botao = 0; botao < 2; botao++) {
            int tent = 0, saiu = 0;
            double atraso = 0;
            for (int k = 0; k < 14; k++) {
                int[] r = tagNoCongelamento(botao == 1, k);
                if (r == null) continue;
                tent++;
                if (r[0] >= 0) { saiu++; atraso += r[0]; }
            }
            System.out.printf(Locale.ROOT, "| %s | %d | %d | %s |%n", botao == 1 ? "↓+TAG (troca)" : "TAG (assist)", tent, saiu, saiu == 0 ? "–" : f1(atraso / saiu));
        }
        System.out.println();
        System.out.println("#### Assist → TAG sem pressão (TAG k quadros depois do assist; boneco parado)");
        System.out.println();
        System.out.println("| k | troca | quadros até a Yin-lu agir |");
        System.out.println("|---|---|---|");
        for (int k : new int[] {1, 10, 20, 31, 32, 34, 40, 44}) {
            int[] r = BateriaBalanco.trocaDepoisDoAssist(k, false, 1);
            System.out.printf(Locale.ROOT, "| %d | %s | %s |%n", k, r[0] < 0 ? "não" : "sim", r[0] < 0 ? "–" : Integer.toString(r[0]));
        }
    }

    /** H acerta o boneco; no quadro {@code k} do congelamento, aperta TAG (ou ↓+TAG). Quadros do aperto até sair, ou -1. */
    static int[] tagNoCongelamento(boolean troca, int k) {
        BateriaBalanco.Luta l = luta(new String[] {"player_base", "p02"}, new String[] {"player_base"}, 1, new CombatConfig(), 900f, 1030f);
        l.mao[1] = BateriaBalanco.PARADO;
        final int[] f = {-1, -1, -1};   // [H apertado, congelamento começou, aperto do TAG]
        l.mao[0] = (lu, lado, in) -> {
            in.clear();
            CombatFighter p = lu.f(0);
            if (f[0] < 0 && p.canAct() && lu.flow.phase() == BattleFlow.Phase.FIGHT) { in.heavy = true; f[0] = lu.quadro; }
            if (f[1] < 0 && f[0] >= 0 && p.frozen()) f[1] = lu.quadro;
            if (f[1] >= 0 && f[2] < 0 && lu.quadro == f[1] + k) {
                if (!p.frozen()) { f[2] = -2; return; }
                if (troca) in.tag = true; else in.assist = true;
                f[2] = lu.quadro;
            }
        };
        FighterState antes = l.e.team(0).pointState();
        for (int fr = 0; fr < 400; fr++) {
            l.passo();
            if (f[2] == -2) return null;
            if (f[2] < 0) continue;
            TeamSystem.Side s = l.e.team(0);
            if (troca ? s.tagging() || s.pointState() != antes : s.assistOut()) return new int[] {l.quadro - f[2]};
            if (l.quadro > f[2] + 60) return new int[] {-1};
        }
        return new int[] {-1};
    }

    // ================================================================== T20 DHC

    static void t20() {
        titulo("T20 Eficiência do DHC");
        System.out.println("Mesmo início (M H) no boneco, com 3 barras: Super sozinho, Super + DHC (TAG no Super já solto) e Ultra.");
        System.out.println("Congelado = quadros de tela parada (o Ultra conta só a parte no motor; a página do mangá fica fora).");
        System.out.println();
        System.out.println("| dupla | fecho | dano | acertos | barras gastas | vida recuperável do rival | congelado | distância final | rival da parede |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        String[][] fechos = {{"M", "H", "SUPER"}, {"M", "H", "SUPER", "DHC"}, {"M", "H", "ULTRA"}};
        String[] nomes = {"Super", "Super + DHC", "Ultra"};
        double[][] soma = new double[3][3];
        int[] cont = new int[3];
        for (String[] d : DUPLAS) {
            for (int f = 0; f < fechos.length; f++) {
                int[] extra = new int[5];
                BateriaBalanco.Execucao x = executar(d[0], d[1], Arrays.asList(fechos[f]), 3, extra);
                if (!x.valida) {
                    System.out.printf(Locale.ROOT, "| %s + %s | %s | não saiu | | | | | | |%n", nome(d[0]), nome(d[1]), nomes[f]);
                    continue;
                }
                soma[f][0] += x.dano;
                soma[f][1] += extra[1];
                soma[f][2] += extra[0];
                cont[f]++;
                System.out.printf(Locale.ROOT, "| %s + %s | %s | %d | %d | %.1f | %d | %d | %d px | %d px |%n", nome(d[0]), nome(d[1]), nomes[f], x.dano,
                    x.acertos, extra[1] / 1000.0, extra[2], extra[0], extra[3], extra[4]);
            }
        }
        System.out.println();
        for (int f = 0; f < 3; f++) {
            if (cont[f] == 0) continue;
            System.out.printf(Locale.ROOT, "Média %s: %.0f de dano, %.1f barra, %.0f quadros congelados (%.0f de dano por barra).%n", nomes[f],
                soma[f][0] / cont[f], soma[f][1] / cont[f] / 1000, soma[f][2] / cont[f], soma[f][0] / Math.max(1, soma[f][1] / 1000));
        }
    }

    // ================================================================== T21 duplas oficiais

    static void t21() {
        titulo("T21 Sinergia das duplas oficiais");
        System.out.println("Cada titular com o parceiro oficial e com três alternativos, contra as outras sete duplas oficiais");
        System.out.println("(CPU nível 8 × CPU nível 8, 2x2, 8 lutas por adversário, metade de cada lado).");
        System.out.println();
        System.out.println("| titular | oficial | vitórias | alternativo 1 | alternativo 2 | alternativo 3 | melhor parceiro |");
        System.out.println("|---|---|---|---|---|---|---|");
        List<String[]> linhas = new ArrayList<>();
        for (int i = 0; i < DUPLAS.length; i++) linhas.add(new String[] {Integer.toString(i)});
        List<String> out = linhas.parallelStream().map(li -> {
            int i = Integer.parseInt(li[0]);
            String t = DUPLAS[i][0], oficial = DUPLAS[i][1];
            // Três alternativos fixos: o parceiro oficial das três duplas seguintes.
            String[] parceiros = {oficial, DUPLAS[(i + 1) % 8][1], DUPLAS[(i + 3) % 8][0], DUPLAS[(i + 5) % 8][1]};
            double[] taxa = new double[parceiros.length];
            for (int p = 0; p < parceiros.length; p++) {
                int v = 0, n = 0;
                for (int o = 0; o < DUPLAS.length; o++) {
                    if (o == i) continue;
                    for (int s = 0; s < 8; s++) {
                        boolean troca = (s & 1) == 1;
                        String[] meu = {t, parceiros[p]};
                        BateriaBalanco.Luta l = new BateriaBalanco.Luta(troca ? DUPLAS[o] : meu, troca ? meu : DUPLAS[o], NIVEL,
                            150_000 + i * 1000 + o * 50 + s, new CombatConfig());
                        int w = l.rodar();
                        if (w == (troca ? 1 : 0)) v++;
                        n++;
                    }
                }
                taxa[p] = v / (double)n;
            }
            int melhor = 0;
            for (int p = 1; p < taxa.length; p++) if (taxa[p] > taxa[melhor]) melhor = p;
            return String.format(Locale.ROOT, "| %s | %s | %s | %s %s | %s %s | %s %s | %s |", nome(t), nome(oficial), pct(taxa[0]),
                nome(parceiros[1]), pct(taxa[1]), nome(parceiros[2]), pct(taxa[2]), nome(parceiros[3]), pct(taxa[3]),
                melhor == 0 ? "oficial" : nome(parceiros[melhor]));
        }).collect(java.util.stream.Collectors.toList());
        for (String l : out) System.out.println(l);
    }

    public static void main(String[] args) {
        String qual = args.length > 0 ? args[0] : "tudo";
        long t0 = System.nanoTime();
        System.out.println("# Auditoria, frente C");
        // O cache do dano do Ultra (HashMap) é preenchido antes das buscas em paralelo.
        for (String id : ELENCO) BateriaBalanco.danoUltra(id);
        if (qual.equals("dbg17")) {
            for (String x : new String[] {"p07", "p12", "p13", "p14", "p15", "p02"}) {
                for (int g = 100; g <= 400; g += 50) {
                    int[] a = chamarAssist(x, g, 0, 0), b = chamarAssist(x, g, 1, 0);
                    System.out.println(x + " " + g + " parado " + (a == null ? "-" : Arrays.toString(a)) + " defendendo " + (b == null ? "-" : Arrays.toString(b)));
                }
            }
        }
        if (qual.equals("t12") || qual.equals("tudo")) t12();
        if (qual.equals("t15") || qual.equals("tudo")) t15();
        if (qual.equals("t16") || qual.equals("tudo")) t16();
        if (qual.equals("t17") || qual.equals("tudo")) t17();
        if (qual.equals("t18") || qual.equals("tudo")) t18();
        if (qual.equals("t19") || qual.equals("tudo")) t19();
        if (qual.equals("t20") || qual.equals("tudo")) t20();
        if (qual.equals("t21") || qual.equals("tudo")) t21();
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
