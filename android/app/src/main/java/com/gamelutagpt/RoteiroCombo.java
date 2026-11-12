package com.gamelutagpt;

/**
 * Um combo apertado quadro a quadro no jogo de verdade, para gravar no PC (GravarLuta, passo
 * {@code rota:}): os mesmos passos e a mesma regra do executor dos testes
 * ({@code BateriaCombos.executar}): cada passo é apertado até sair, e o próximo vem em seguida.
 * Assim o vídeo mostra exatamente o combo que o teste do motor conferiu. O rival fica parado.
 *
 * <p>Passos: L M H 2L 2M 2H 6H (→ + H) jL jM jH j2H, S (Onda), S2 S3 S4, SUPER, A (assist),
 * J (pulo), SJ (super pulo), D (corrida), C ou C160 (corre segurando → até ficar a 260 ou 160 px
 * do rival), W30 (espera 30 quadros), DHC (TAG no Super: a troca no Super).
 */
final class RoteiroCombo {
    private final String[] passos;
    private int k = 0, t = 0;
    private AttackDefinition antes;
    private int antesQuadro, antesCorrente, antesIniciados;
    private boolean noChao, assistFora, puloNoAr;
    private int titular;

    RoteiroCombo(String rota) { passos = rota.trim().split("[ ,]+"); }

    boolean acabou() { return k >= passos.length; }

    /** Entrada do jogador neste quadro (antes do passo do motor). */
    void preencher(CombatEngine e, FighterInput in) {
        in.clear();
        if (acabou()) return;
        CombatFighter me = e.fighter(0), rival = e.fighter(1);
        if (t > 0 && feito(passos[k], e, me, rival)) {
            k++;
            t = 0;
            if (acabou()) return;
        }
        String passo = passos[k];
        if (t == 0) {
            antes = me.attack;
            antesQuadro = me.attackFrame;
            antesCorrente = me.chain.size();
            antesIniciados = me.golpesIniciados;
            noChao = me.grounded;
            puloNoAr = me.airJumpUsed;
            assistFora = e.team(0).assistOut();
            titular = e.team(0).point;
        }
        t++;
        int limite = passo.startsWith("W") ? Integer.parseInt(passo.substring(1)) + 1 : passo.startsWith("C") ? 90 : 45;
        if (t > limite) {   // não saiu: o roteiro para (o vídeo mostra onde)
            k = passos.length;
            return;
        }
        apertar(passo, me, in);
    }

    private static void apertar(String passo, CombatFighter me, FighterInput in) {
        int frente = me.facing > 0 ? 1 : 5;
        switch (passo) {
            case "L": case "jL": in.light = true; break;
            case "M": case "jM": in.medium = true; break;
            case "H": case "jH": in.heavy = true; break;
            case "2L": in.direction = me.facing > 0 ? 2 : 4; in.light = true; break;
            case "2M": in.direction = me.facing > 0 ? 2 : 4; in.medium = true; break;
            case "2H": in.direction = me.facing > 0 ? 2 : 4; in.heavy = true; break;
            case "6H": in.direction = frente; in.heavy = true; break;
            case "j2H": in.direction = 3; in.heavy = true; break;
            case "S": in.special = "M"; break;
            case "SUPER": in.superAttack = true; break;
            case "A": case "DHC": in.assist = true; break;
            case "J": in.jump = true; in.direction = me.facing > 0 ? 8 : 6; break;
            case "SJ": in.superJump = true; in.direction = me.facing > 0 ? 8 : 6; break;
            case "D": in.dash = true; in.direction = frente; break;
            default:
                if (passo.startsWith("C")) {
                    in.dash = !me.forwardDashing;
                    in.direction = frente;
                } else if (passo.matches("S[2-4]")) {
                    // O especial de comando já resolvido, como o comando completo faria.
                    me.commandButton = "M";
                    me.buffer.push(InputBuffer.Button.SPECIAL, passo, false);
                }
        }
    }

    private boolean feito(String passo, CombatEngine e, CombatFighter me, CombatFighter rival) {
        if (passo.startsWith("W")) return t >= Integer.parseInt(passo.substring(1));
        boolean novo = me.attacking() && me.attack != null &&
            (me.attack != antes || me.attackFrame < antesQuadro || me.chain.size() > antesCorrente ||
             me.golpesIniciados > antesIniciados);
        switch (passo) {
            case "J": case "SJ": return noChao && !me.grounded || !noChao && !puloNoAr && me.airJumpUsed;
            case "D": return me.forwardDashing;
            case "A": return !assistFora && e.team(0).assistOut();
            case "DHC": return e.cues().contains("DHC") || e.team(0).point != titular;
            case "S": return novo && me.attack.id.startsWith("S") && me.attack.kind != AttackDefinition.Kind.SUPER;
            case "SUPER": return novo && me.attack.kind == AttackDefinition.Kind.SUPER;
            default:
                if (passo.startsWith("C")) {
                    float perto = passo.length() > 1 ? Float.parseFloat(passo.substring(1)) : 260f;
                    return Math.abs(rival.x - me.x) <= perto;
                }
                return novo && me.attack.id.equals(passo);
        }
    }
}
