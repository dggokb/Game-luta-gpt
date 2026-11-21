package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;

/**
 * Combos de assinatura do Kōga (21/11, docs/personagens/combos-assinatura.md): o brigão de perto.
 * A Garra do tigre saindo do M, os dois socos no estômago emendando na Investida da voadora, o
 * Uppercut da casa que joga em arco (corre, pega no chão e recomeça), o 2H que lança para o combo
 * no ar, a bica (→ + M, o golpe alto) abrindo para o S2 e o Super, a voadora correndo que lança, o
 * fecho de Super e o H que quica na parede no canto. Cada combo é apertado no motor de verdade
 * contra o Ryūga parado e todas as pancadas têm de emendar. E a CPU usa os dois de assinatura.
 */
public class ComboKogaTest {
    private static final String KOGA = "p03", YINLU = "p02";

    private static BateriaCombos.Exec combo(String rota, float gap, boolean canto, int primeiroAcertoAte) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = gap;
        cn.canto = canto;
        cn.primeiroAcertoAte = primeiroAcertoAte;
        return BateriaCombos.executar(KOGA, YINLU, Arrays.asList(rota.split(" ")), cn);
    }

    private static BateriaCombos.Exec koga(String rota, int acertos) {
        BateriaCombos.Exec x = combo(rota, 100f, false, 1);
        assertTrue(rota + ": o combo emenda", x.valida);
        assertEquals(rota + ": acertos", acertos, x.acertos);
        return x;
    }

    @Test public void k1GarraSaiDoM() {
        // O H joga longe na parede: a Garra sai do M (L, L, M, M e os três cortes).
        koga("L L M M S SG S", 7);
    }

    @Test public void k2EstomagoEmendaNaInvestida() {
        // Os dois socos (2 acertos) e a investida inteira (7 acertos, o último derruba).
        koga("L M M 6H S3", 12);
    }

    @Test public void k3UppercutJogaEmArcoEElePegaNoChao() {
        BateriaCombos.Exec so = koga("L M M S2", 4);
        assertTrue("o uppercut joga longe: " + so.gapFinal, so.gapFinal > 300f);
        koga("L M M S2 W40 C120 2M L M 6H S3", 9);
    }

    @Test public void k4SobeNoAr() {
        koga("2L M 2H SJ jL jM J jL jM jH", 8);
    }

    @Test public void k5BicaAbreParaOS2EOSuper() {
        // A bica é golpe alto (de surpresa): abre o combo; no meio dele o atordoamento é curto.
        assertTrue(GeneratedCharacters.get(KOGA).attack(GolpeAlto.BINDING).overhead);
        BateriaCombos.Exec x = koga("6M S2 SUPER", 15);
        assertTrue("o Super entrou", x.superUsado);
    }

    @Test public void k6VoadoraCorrendoLancaParaOAr() {
        BateriaCombos.Exec x = combo("C DH W45 C140 2H SJ jL jM J jL jM jH", 500f, false, 2);
        assertTrue("o combo emenda", x.valida);
        assertEquals("acertos", 7, x.acertos);
    }

    @Test public void k7FechoDeSuper() {
        BateriaCombos.Exec x = koga("L L M M S SG S SUPER", 20);
        assertTrue("o Super entrou", x.superUsado);
    }

    @Test public void k8NoCantoOHQuicaNaParede() {
        BateriaCombos.Exec x = combo("L L M M H 6H S3", 100f, true, 1);
        assertTrue("o combo emenda", x.valida);
        assertEquals("acertos", 14, x.acertos);
        assertEquals("o H quicou na parede", 1, x.parede);
    }

    // ---------------------------------------------------------------- a CPU

    @Test public void aCpuAlternaAsRotasDeAssinatura() {
        CharacterDefinition c = GeneratedCharacters.get(KOGA);
        assertEquals(ComboBook.route(c), ComboBook.routeDaVez(c, 0));
        assertEquals(Arrays.asList("L", "M", "M", "6H", "S3"), ComboBook.routeDaVez(c, 1));
        assertEquals(Arrays.asList("L", "L", "M", "M", "S"), ComboBook.routeDaVez(c, 2));
        assertEquals(ComboBook.route(c), ComboBook.routeDaVez(c, 3));
        // Quem não tem assinatura fica sempre na rota do estilo.
        CharacterDefinition ryuga = GeneratedCharacters.get("player_base");
        assertEquals(ComboBook.route(ryuga), ComboBook.routeDaVez(ryuga, 1));
    }

    /** {combos com → + H seguido do S3, combos com a Garra saindo do M}, nas lutas da CPU contra o boneco. */
    private static int[] cpuAssinaturas(long seeds) {
        int estomago = 0, garra = 0;
        for (long seed = 1; seed <= seeds; seed++) {
            CombatConfig cfg = new CombatConfig();
            cfg.justDefenseFrames = -1;
            CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.defaultCharacter(), "P1"),
                new FighterState(GeneratedCharacters.get(KOGA), "CPU"), 600f, 760f, cfg);
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(8);
            FighterInput p = new FighterInput(), out = new FighterInput();
            List<String> seq = new ArrayList<>();
            int ultimo = -1000;
            for (int f = 0; f < 60 * 60; f++) {
                e.fighter(0).state.life = e.fighter(0).state.profile.maxLife;
                e.fighter(1).state.life = e.fighter(1).state.profile.maxLife;
                cpu.fill(e, 1, out);
                e.step(p, out);
                for (CombatEngine.HitEvent h : e.events()) {
                    if (h.attacker != 1 || h.blocked) continue;
                    if (f - ultimo > 90) {
                        if (temSequencia(seq, "6H", "S3")) estomago++;
                        if (temSequencia(seq, "M", "S1A")) garra++;
                        seq.clear();
                    }
                    seq.add(h.moveId);
                    ultimo = f;
                }
            }
            if (temSequencia(seq, "6H", "S3")) estomago++;
            if (temSequencia(seq, "M", "S1A")) garra++;
        }
        return new int[] {estomago, garra};
    }

    private static boolean temSequencia(List<String> seq, String antes, String depois) {
        for (int i = 0; i + 1 < seq.size(); i++) if (antes.equals(seq.get(i)) && depois.equals(seq.get(i + 1))) return true;
        return false;
    }

    // ---------------------------------------------------------------- o Super (21/11)

    /** O Super novo: investe, 12 socos rápidos e a cabeçada que lança para a frente (13 pancadas). */
    @Test public void oSuperInvesteSocaELancaNaCabecada() {
        AttackDefinition sup = GeneratedCharacters.get(KOGA).attack("SUPER");
        assertEquals(13, sup.maxHits);
        assertEquals("a cabeçada lança para a frente, não para cima", AttackDefinition.Launch.ARC, sup.launch);
        for (float gap : new float[] {120f, 300f, 450f, 800f}) {
            CombatConfig cfg = new CombatConfig();
            cfg.justDefenseFrames = -1;
            CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.get(KOGA), "P1"),
                new FighterState(GeneratedCharacters.get(YINLU), "P2"), 600f, 600f + gap, cfg);
            e.fighter(0).state.superMeter = 5000;
            e.fighter(1).state.life = 10_000_000;
            FighterInput i0 = new FighterInput(), i1 = new FighterInput();
            i0.superAttack = true;
            int acertos = 0;
            float x0 = e.fighter(1).x, frente = 0f, alto = 0f;
            List<Integer> quadros = new ArrayList<>();
            for (int t = 0; t < 200; t++) {
                e.step(i0, i1);
                i0.clear();
                for (CombatEngine.HitEvent h : e.events()) {
                    if (h.attacker != 0 || h.blocked || !"SUPER".equals(h.moveId)) continue;
                    acertos++;
                    quadros.add(e.fighter(0).attackFrame);
                }
                if (acertos == 13) {
                    frente = Math.max(frente, e.fighter(1).x - x0);
                    alto = Math.max(alto, Arena.GROUND_Y - e.fighter(1).y);
                }
            }
            assertEquals("de " + gap + " px ele corre até a frente do rival e as 13 pegam", 13, acertos);
            assertTrue("a cabeçada joga longe para a frente: " + frente, frente > 400f);
            assertTrue("e baixo, não para cima: " + alto, alto < 160f);
            for (int i = 1; i < 12; i++) assertEquals("os socos a cada 4 quadros: " + quadros, 4, quadros.get(i) - quadros.get(i - 1));
        }
    }

    // ---------------------------------------------------------------- o S3 corre até o rival (21/11)

    /** O ↓↙← corre até o rival e só então sai a voadora (o dono: "e aí sim dá o chute"). */
    @Test public void oS3CorreAteORivalEDaOChute() {
        for (float gap : new float[] {150f, 450f, 700f}) {
            CombatConfig cfg = new CombatConfig();
            cfg.justDefenseFrames = -1;
            CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.get(KOGA), "P1"),
                new FighterState(GeneratedCharacters.get(YINLU), "P2"), 300f, 300f + gap, cfg);
            e.fighter(1).state.life = 10_000_000;
            FighterInput i0 = new FighterInput(), i1 = new FighterInput();
            i0.special = "S3";
            int acertos = 0, corrida = 0;
            for (int t = 0; t < 240; t++) {
                e.step(i0, i1);
                i0.clear();
                corrida = Math.max(corrida, e.fighter(0).corrida);
                for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0 && !h.blocked) acertos++;
            }
            assertEquals("de " + gap + " px: a voadora e os seis socos", 7, acertos);
            if (gap >= 450f) assertTrue("de longe ele corre antes de chutar: " + corrida, corrida > 5);
        }
        // A CPU sabe que o S3 chega longe (a corrida entra no alcance dela).
        AttackDefinition s3 = GeneratedCharacters.get(KOGA).attack("S3");
        assertTrue("alcance da corrida para a CPU: " + CpuSpecials.travel(s3), CpuSpecials.travel(s3) >= 500f);
    }

    @Test public void aCpuAcertaOSuperNovo() {
        int inteiros = 0;
        for (long seed = 1; seed <= 6; seed++) {
            CombatConfig cfg = new CombatConfig();
            cfg.justDefenseFrames = -1;
            CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.defaultCharacter(), "P1"),
                new FighterState(GeneratedCharacters.get(KOGA), "CPU"), 600f, 760f, cfg);
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(8);
            FighterInput p = new FighterInput(), out = new FighterInput();
            int seguidos = 0;
            for (int f = 0; f < 60 * 60; f++) {
                e.fighter(0).state.life = e.fighter(0).state.profile.maxLife;
                e.fighter(1).state.life = e.fighter(1).state.profile.maxLife;
                e.fighter(1).state.superMeter = 5000;
                cpu.fill(e, 1, out);
                e.step(p, out);
                for (CombatEngine.HitEvent h : e.events()) {
                    if (h.attacker != 1 || h.blocked) continue;
                    if (!"SUPER".equals(h.moveId)) { seguidos = 0; continue; }
                    if (++seguidos == 13) inteiros++;
                }
                if (!e.fighter(1).attacking()) seguidos = 0;
            }
        }
        assertTrue("a CPU acertou o Super inteiro: " + inteiros, inteiros >= 1);
    }

    @Test public void aCpuFazOsCombosDeAssinaturaDoKoga() {
        int[] n = cpuAssinaturas(6);
        assertTrue("a CPU emendou os dois socos na investida: " + n[0], n[0] >= 1);
        assertTrue("a CPU emendou a Garra no M: " + n[1], n[1] >= 1);
    }
}
