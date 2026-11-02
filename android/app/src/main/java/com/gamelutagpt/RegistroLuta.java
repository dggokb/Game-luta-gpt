package com.gamelutagpt;

import java.util.ArrayDeque;
import java.util.Locale;

/**
 * Registro de luta (pedido do jogador, 02/11: "registra todos os movimentos e a linha do tempo,
 * para você entender o que aconteceu na luta real"). A linha do tempo de cada luta contra a CPU:
 * <ul>
 *   <li>o que cada lado passa a fazer (parado, anda, agacha, segura a defesa, corre, backdash, pulo,
 *   super pulo, golpe, apanha, defende, caído, levanta, agarrão...), com a posição e a distância;</li>
 *   <li>cada acerto e cada defesa (golpe, dano, vidas), e os avisos do motor (SHINKAI, PARRY,
 *   COMBO BREAKER...);</li>
 *   <li>os botões e as direções que o jogador aperta;</li>
 *   <li>o porquê de cada decisão da CPU (o que ela "pensou": antiaéreo, pune, espera...);</li>
 *   <li>a cada segundo, as vidas, as barras e o SHINKAI dos dois.</li>
 * </ul>
 * Guarda as últimas {@link #LUTAS_GUARDADAS} lutas na memória; a pausa (ENVIAR REGISTRO) manda tudo
 * num arquivo de texto. Java puro: as baterias escrevem no mesmo formato, para comparar.
 */
final class RegistroLuta {
    static final int LUTAS_GUARDADAS = 12;
    /** Uma luta não passa disto (uns 2,5 MB de texto): o resto é cortado. */
    static final int MAX_CHARS = 2_500_000;

    private final ArrayDeque<String> prontas = new ArrayDeque<>();
    private StringBuilder atual;
    private final String[] acao = new String[2];
    private String razao;
    private int direcao = Integer.MIN_VALUE;
    private String apertos = "";
    private final boolean[] pulou = new boolean[2], noChao = {true, true};
    private boolean fechada = true;
    private int quadros;

    /** Começa uma luta nova ({@code cabecalho}: modo, nível, personagens...). */
    void comecar(String cabecalho) {
        if (atual != null && !fechada && quadros > 0) fechar("(luta interrompida)");
        atual = new StringBuilder(64 * 1024);
        atual.append("=== LUTA ").append(cabecalho).append('\n');
        atual.append("tempo em segundos da luta (60 quadros = 1 s, sem contar a velocidade da pausa). JOG = jogador, CPU = a CPU.\n");
        acao[0] = acao[1] = null;
        razao = null;
        direcao = Integer.MIN_VALUE;
        apertos = "";
        pulou[0] = pulou[1] = false;
        noChao[0] = noChao[1] = true;
        fechada = false;
        quadros = 0;
    }

    /** Um quadro da luta, depois do passo do motor. */
    void quadro(CombatEngine e, BattleFlow flow, FighterInput jogador, String razaoCpu) {
        if (atual == null || fechada) return;
        quadros++;
        String t = String.format(Locale.ROOT, "%7.2f", e.frame() / 60.0);
        if (jogador != null) entradas(t, e.fighter(0), jogador);
        for (int s = 0; s < 2; s++) {
            CombatFighter f = e.fighter(s);
            // Pulou de verdade: saiu do chão livre (não lançado, não no Clash).
            if (f.grounded) pulou[s] = false;
            else if (noChao[s] && f.status == CombatFighter.Status.NEUTRAL && e.shinkaiClash() == null) pulou[s] = true;
            noChao[s] = f.grounded;
            String a = acao(e, f);
            if (a.startsWith("pulo") && !pulou[s]) a = "no ar (se recupera)";
            if (!a.equals(acao[s])) {
                acao[s] = a;
                linha(t, quem(s), String.format(Locale.ROOT, "%s  [x=%.0f dist=%.0f]", a, f.x, Math.abs(e.fighter(0).x - e.fighter(1).x)));
            }
        }
        for (CombatEngine.HitEvent h : e.events()) {
            String tipo = h.blocked ? "DEFENDE" : "ACERTA";
            linha(t, quem(h.attacker), String.format(Locale.ROOT, "%s %s%s → %s  dano %d  (vidas %d × %d)", tipo,
                h.moveId, h.projectile ? " (projétil)" : "", quem(h.defender), h.damage,
                e.fighter(0).state.life, e.fighter(1).state.life));
        }
        for (String c : e.cues()) linha(t, "MOTOR", c);
        if (razaoCpu != null && !razaoCpu.isEmpty() && !razaoCpu.equals(razao)) {
            razao = razaoCpu;
            linha(t, "CPU", "pensa: " + razaoCpu);
        }
        if (e.frame() % 60 == 0) {
            FighterState a = e.fighter(0).state, b = e.fighter(1).state;
            linha(t, "~", String.format(Locale.ROOT, "vidas %d × %d  barras %.1f × %.1f  SHINKAI %s × %s  dist %.0f",
                a.life, b.life, a.superBars(), b.superBars(), shinkai(a), shinkai(b),
                Math.abs(e.fighter(0).x - e.fighter(1).x)));
        }
        if (flow != null && flow.over()) {
            int v = flow.winner();
            fechar("FIM: " + (v == 0 ? "o JOGADOR venceu" : v == 1 ? "a CPU venceu" : "empate"));
        } else if (atual.length() > MAX_CHARS) {
            fechar("(registro cortado: luta longa demais)");
        }
    }

    private static String shinkai(FighterState s) {
        if (s.inShinkai()) return "ligado";
        if (s.shinkaiUsed) return "usado";
        return (s.shinkai * 100 / CombatConfig.SHINKAI_MAX) + "%";
    }

    private void entradas(String t, CombatFighter f, FighterInput in) {
        int rel = MotionParser.relative(in.direction, f.facing);
        if (rel != direcao) {
            direcao = rel;
            linha(t, "JOG", "direção " + seta(rel));
        }
        StringBuilder b = new StringBuilder();
        if (in.light) b.append(" L");
        if (in.medium) b.append(" M");
        if (in.heavy) b.append(" H");
        if (in.special != null) b.append(" especial ").append(in.special);
        if (in.superAttack) b.append(" SUPER");
        if (in.ultra) b.append(" ULTRA");
        if (in.shinkai) b.append(" SHINKAI");
        if (in.assist) b.append(" ASSIST");
        if (in.tag) b.append(" TAG");
        if (in.grab) b.append(" AGARRÃO");
        if (in.pushblock) b.append(" PUSHBLOCK");
        if (in.jump) b.append(" PULO");
        if (in.superJump) b.append(" SUPER-PULO");
        if (in.dash) b.append(" DASH");
        if (in.backdash) b.append(" BACKDASH");
        // Segurado (o dash da corrida, o botão carregando) conta uma vez: só o aperto novo.
        String agora = b.toString();
        if (b.length() > 0 && !agora.equals(apertos)) linha(t, "JOG", "aperta" + b);
        apertos = agora;
    }

    /** A direção vista por quem olha para o rival: → frente, ← trás. */
    static String seta(int rel) {
        switch (rel) {
            case 1: return "→ frente";
            case 2: return "↘ baixo-frente";
            case 3: return "↓ baixo";
            case 4: return "↙ baixo-trás";
            case 5: return "← trás";
            case 6: return "↖ cima-trás";
            case 7: return "↑ cima";
            case 8: return "↗ cima-frente";
            default: return "· solto";
        }
    }

    /** O que o lutador está fazendo, em palavras. */
    static String acao(CombatEngine e, CombatFighter f) {
        // A coreografia do SHINKAI Clash leva os dois pela arena: não é pulo nem golpe deles.
        if (e.shinkaiClash() != null) return "SHINKAI CLASH";
        switch (f.status) {
            case ATTACK:
                return "golpe " + (f.attack != null ? f.attack.id : "?") + (f.grounded ? "" : " (no ar)");
            case HITSTUN: return "apanha";
            case AIR_HITSTUN: return "apanha no ar";
            case BLOCKSTUN: return "defende o golpe";
            case KNOCKDOWN: return "caído";
            case WAKEUP: return f.rolling() ? "rola" : "levanta";
            case THROW: return "agarra";
            case THROWN: return "é agarrado";
            case ULTRA: return "ULTRA";
            default: break;
        }
        if (!f.grounded) {
            if (f.superJumping) return "SUPER PULO";
            int lado = f.jumpDirection * f.facing;
            return lado > 0 ? "pulo para frente" : lado < 0 ? "pulo para trás" : "pulo no lugar";
        }
        if (f.backdashFrames > 0) return "backdash";
        if (f.forwardDashing) return "corre";
        int g = e.guardFromInput(f);
        if (g == CombatFighter.GUARD_LOW) return "segura a defesa baixa";
        if (g != CombatFighter.GUARD_NONE) return "segura a defesa alta";
        if (f.crouching) return "agachado";
        int rel = f.input != null ? MotionParser.relative(f.input.direction, f.facing) : 0;
        if (rel == 1) return "anda para frente";
        if (rel == 5) return "anda para trás";
        return "parado";
    }

    private static String quem(int lado) { return lado == 0 ? "JOG" : "CPU"; }

    private void linha(String t, String quem, String o) {
        atual.append(t).append("  ").append(quem).append("  ").append(o).append('\n');
    }

    private void fechar(String fim) {
        atual.append("=== ").append(fim).append(String.format(Locale.ROOT, " (%d quadros)%n%n", quadros));
        fechada = true;
        prontas.addLast(atual.toString());
        while (prontas.size() > LUTAS_GUARDADAS) prontas.removeFirst();
    }

    /** Lutas guardadas (com a que está em andamento, se tiver começado). */
    int lutas() { return prontas.size() + (!fechada && quadros > 0 ? 1 : 0); }

    /** Todas as lutas guardadas e a em andamento, num texto só. */
    String exportar(String cabecalhoArquivo) {
        StringBuilder b = new StringBuilder(cabecalhoArquivo).append("\n\n");
        for (String l : prontas) b.append(l);
        if (!fechada && quadros > 0) b.append(atual).append("=== (luta em andamento quando o registro foi enviado)\n");
        return b.toString();
    }
}
