package com.gamelutagpt;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;

/**
 * Fase A da auditoria (docs/balanco/auditoria.md): a execução repetível sem animação (A04), o
 * registro das partidas em CSV com os campos mínimos do documento e a prova de determinismo (A05:
 * a mesma semente, rodada duas vezes, dá o mesmo estado em todos os quadros).
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaBase [determinismo|csv &lt;arquivo&gt; &lt;lutas&gt;]</pre>
 */
public final class BateriaBase {
    private BateriaBase() {}

    static final String CABECALHO = "commit_sha,test_id,fighter,opponent,assist_partner,opponent_partner,starting_distance," +
        "initial_super_bars,cpu_level,random_seed,frames_elapsed,hits,blocks,damage_dealt,damage_taken,max_combo_hits," +
        "max_combo_damage,max_combo_frames,meter_spent,assists_called,tags_completed,breakers_used,shinkai_activations," +
        "result,state_hash";

    /** Uma partida 2x2 registrada, do ponto de vista do lado 0. */
    static final class Registro {
        String[] t0, t1;
        long semente;
        int quadros, vencedor, maxComboAcertos, maxComboDano, maxComboQuadros, barraGasta, assists, trocas, shinkais;
        long hash = 1125899906842597L;
        BateriaBalanco.Estat[] est;

        String csv(String commit, String teste) {
            BateriaBalanco.Estat a = est[0], b = est[1];
            return String.join(",", commit, teste, t0[0], t1[0], t0[1], t1[1], "560", "0", Integer.toString(BateriaBalanco.NIVEL),
                Long.toString(semente), Integer.toString(quadros), Integer.toString(a.acertos), Integer.toString(b.defendidos),
                Integer.toString(a.dano), Integer.toString(b.dano), Integer.toString(maxComboAcertos), Integer.toString(maxComboDano),
                Integer.toString(maxComboQuadros), Integer.toString(barraGasta), Integer.toString(assists), Integer.toString(trocas),
                Integer.toString(a.breakers), Integer.toString(shinkais),
                vencedor == 0 ? "win" : vencedor == 1 ? "loss" : "draw", Long.toHexString(hash));
        }
    }

    static Registro jogar(String[] t0, String[] t1, long semente) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(t0, t1, BateriaBalanco.NIVEL, semente, new CombatConfig());
        Registro r = new Registro();
        r.t0 = t0;
        r.t1 = t1;
        r.semente = semente;
        ComboSession vista = null;
        int barraAntes = 0;
        boolean assistAntes = false;
        FighterState titular = l.e.team(0).pointState();
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            l.passo();
            CombatFighter a = l.f(0), b = l.f(1);
            // Estado dos dois a cada quadro: posição, vida, barra e o que estão fazendo.
            for (CombatFighter f : new CombatFighter[] {a, b}) {
                r.hash = r.hash * 31 + Float.floatToIntBits(f.x);
                r.hash = r.hash * 31 + Float.floatToIntBits(f.y);
                r.hash = r.hash * 31 + f.state.life;
                r.hash = r.hash * 31 + f.state.superMeter;
                r.hash = r.hash * 31 + f.status.ordinal();
            }
            ComboSession s = l.e.lastSession(1);
            if (s != null && s != vista && s.attacker == 0) {
                vista = s;
                if (s.comboDamage > r.maxComboDano) {
                    r.maxComboDano = s.comboDamage;
                    r.maxComboAcertos = s.hitCount;
                    r.maxComboQuadros = s.comboDuration;
                }
            }
            int barra = a.state.superMeter;
            if (barra < barraAntes && a.state == titular) r.barraGasta += barraAntes - barra;
            barraAntes = barra;
            boolean assist = l.e.team(0).assistOut();
            if (assist && !assistAntes) r.assists++;
            assistAntes = assist;
            if (l.e.team(0).pointState() != titular) {
                titular = l.e.team(0).pointState();
                barraAntes = titular.superMeter;
                r.trocas++;
            }
            if (l.e.cues().contains("SHINKAI") && a.state.inShinkai()) r.shinkais++;
        }
        r.quadros = l.quadro;
        r.vencedor = l.flow.winner();
        r.est = l.est;
        return r;
    }

    static String[] dupla(Random r) {
        return BateriaBalanco.par(r);
    }

    // ------------------------------------------------------------------ T24 (proxy): o comando no toque

    static String desenho(int[] cmd, boolean hold) {
        String[] setas = {"·", "→", "↘", "↓", "↙", "←", "↖", "↑", "↗"};
        StringBuilder sb = new StringBuilder(hold ? "[" : "");
        for (int i = 0; i < cmd.length; i++) {
            sb.append(setas[cmd[i]]);
            if (hold && i == 0) sb.append("]");
            if (i < cmd.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Uma tentativa do comando {@code key} ("S1".."S4") como um polegar no direcional da tela:
     * cada direção fica de {@code minQ} a {@code maxQ} quadros, a diagonal às vezes é pulada, às
     * vezes o polegar escorrega para ↗ no fim, e o botão vem 0–4 quadros depois. Devolve o que saiu.
     */
    static String tentar(String id, String key, Random r, int minQ, int maxQ, double pulaDiagonal, double escorrega) {
        CharacterDefinition c = BateriaBalanco.ch(id);
        CombatEngine e = new CombatEngine(new FighterState(c, "P1"), new FighterState(GeneratedCharacters.opponentCharacter(), "P2"),
            500f, 1500f, new CombatConfig());
        FighterInput in = new FighterInput(), npc = new FighterInput();
        CombatFighter p = e.fighter(0);
        int[] cmd;
        boolean hold, air = false;
        if (key.equals("S1")) {
            cmd = c.fighter.energyCommand;
            hold = c.fighter.energyHold;
        } else {
            CharacterDefinition.Special sp = c.specials.get(key);
            if (sp == null) return null;
            cmd = sp.command();
            hold = sp.hold;
            air = sp.air;
        }
        if (air) {
            in.direction = 7;
            e.step(in, npc);
            in.clearPresses();
            in.direction = 0;
            for (int i = 0; i < 6; i++) { e.step(in, npc); in.clearPresses(); }
        }
        if (hold) {
            in.direction = cmd[0];
            int carga = e.config.chargeFrames - 4 + r.nextInt(20);
            for (int i = 0; i < carga; i++) { e.step(in, npc); in.clearPresses(); }
            in.direction = cmd[1];
            int atraso = r.nextInt(3);
            for (int i = 0; i < atraso; i++) { e.step(in, npc); in.clearPresses(); }
        } else {
            int ultima = -1;
            for (int k = 0; k < cmd.length; k++) {
                int d = cmd[k];
                boolean diagonal = d == 2 || d == 4 || d == 6 || d == 8;
                if (diagonal && k > 0 && k < cmd.length - 1 && r.nextDouble() < pulaDiagonal) continue;
                if (d == ultima) {
                    in.direction = 0;
                    e.step(in, npc);
                    in.clearPresses();
                }
                in.direction = d;
                int q = minQ + r.nextInt(maxQ - minQ + 1);
                for (int i = 0; i < q; i++) { e.step(in, npc); in.clearPresses(); }
                ultima = d;
            }
            if (r.nextDouble() < escorrega) {
                in.direction = cmd[cmd.length - 1] == 5 ? 6 : 8;
                e.step(in, npc);
                in.clearPresses();
                in.direction = cmd[cmd.length - 1];
            }
            int atraso = r.nextInt(5);
            for (int i = 0; i < atraso; i++) { e.step(in, npc); in.clearPresses(); }
        }
        in.medium = true;
        e.step(in, npc);
        in.clearPresses();
        in.direction = 0;
        for (int i = 0; i < 6 && !p.attacking(); i++) { e.step(in, npc); in.clearPresses(); }
        if (p.attacking()) return p.attack.id;
        if (!p.grounded && !air) return "pulo";
        return "nada";
    }

    static void comandos() {
        String[][] perfis = {{"limpo (2 quadros por direção)", "2", "2", "0", "0"},
            {"toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega)", "2", "4", "0.10", "0.05"},
            {"toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega)", "1", "3", "0.30", "0.15"}};
        java.util.Map<String, int[]> porComando = new java.util.TreeMap<>();
        java.util.Map<String, java.util.Map<String, Integer>> erros = new java.util.TreeMap<>();
        System.out.println("| perfil | comando | tentativas | saiu certo | saiu outro especial | saiu golpe normal | pulou | nada |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        for (String[] pf : perfis) {
            porComando.clear();
            for (String id : BateriaBalanco.ELENCO) {
                CharacterDefinition c = BateriaBalanco.ch(id);
                for (String key : new String[] {"S1", "S2", "S3", "S4"}) {
                    int[] cmd;
                    boolean hold;
                    if (key.equals("S1")) { cmd = c.fighter.energyCommand; hold = c.fighter.energyHold; }
                    else { CharacterDefinition.Special sp = c.specials.get(key); if (sp == null) continue; cmd = sp.command(); hold = sp.hold;
                        if (sp.air) continue; }
                    if (cmd == null) continue;
                    String forma = desenho(cmd, hold);
                    String esperado = key.equals("S1") ? "S" : key;
                    Random r = new Random(id.hashCode() * 31L + key.hashCode());
                    int[] t = porComando.computeIfAbsent(forma, k -> new int[6]);
                    for (int i = 0; i < 60; i++) {
                        String saiu = tentar(id, key, r, Integer.parseInt(pf[1]), Integer.parseInt(pf[2]),
                            Double.parseDouble(pf[3]), Double.parseDouble(pf[4]));
                        t[0]++;
                        if (esperado.equals(saiu)) t[1]++;
                        else if (saiu != null && saiu.startsWith("S") && !saiu.equals("SUPER")) t[2]++;
                        else if ("pulo".equals(saiu)) t[4]++;
                        else if ("nada".equals(saiu)) t[5]++;
                        else t[3]++;
                    }
                }
            }
            for (java.util.Map.Entry<String, int[]> en : porComando.entrySet()) {
                int[] t = en.getValue();
                System.out.printf(Locale.ROOT, "| %s | %s | %d | %s | %s | %s | %s | %s |%n", pf[0], en.getKey(), t[0],
                    BateriaBalanco.pct(t[1] / (double)t[0]), BateriaBalanco.pct(t[2] / (double)t[0]), BateriaBalanco.pct(t[3] / (double)t[0]),
                    BateriaBalanco.pct(t[4] / (double)t[0]), BateriaBalanco.pct(t[5] / (double)t[0]));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String qual = args.length > 0 ? args[0] : "determinismo";
        String commit = System.getProperty("commit", "local");
        if (qual.equals("comandos")) { comandos(); return; }
        if (qual.equals("determinismo")) {
            // Dois casos (o documento pede dois), cada um rodado duas vezes do zero.
            long[] sementes = {7, 1234};
            for (long s : sementes) {
                Random r = new Random(s);
                String[] t0 = dupla(r), t1 = dupla(r);
                Registro x = jogar(t0, t1, s), y = jogar(t0, t1, s);
                System.out.printf(Locale.ROOT, "semente %d (%s+%s × %s+%s): %d quadros, hash %s / %s -> %s%n", s, t0[0], t0[1], t1[0], t1[1],
                    x.quadros, Long.toHexString(x.hash), Long.toHexString(y.hash),
                    x.hash == y.hash && x.csv(commit, "A05").equals(y.csv(commit, "A05")) ? "IGUAL" : "DIFERENTE");
            }
            return;
        }
        if (qual.equals("csv")) {
            String arquivo = args[1];
            int n = Integer.parseInt(args[2]);
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Paths.get(arquivo), StandardCharsets.UTF_8))) {
                w.println(CABECALHO);
                for (int i = 0; i < n; i++) {
                    Random r = new Random(500_000 + i);
                    w.println(jogar(dupla(r), dupla(r), 500_000 + i).csv(commit, "A05-amostra"));
                }
            }
            System.out.println(n + " lutas em " + arquivo);
        }
    }
}
