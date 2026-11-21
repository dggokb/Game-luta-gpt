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
        BateriaCombos.Exec x = koga("6M S2 SUPER", 5);
        assertTrue("o Super entrou", x.superUsado);
    }

    @Test public void k6VoadoraCorrendoLancaParaOAr() {
        BateriaCombos.Exec x = combo("C DH W45 C140 2H SJ jL jM J jL jM jH", 500f, false, 2);
        assertTrue("o combo emenda", x.valida);
        assertEquals("acertos", 7, x.acertos);
    }

    @Test public void k7FechoDeSuper() {
        BateriaCombos.Exec x = koga("L L M M S SG S SUPER", 10);
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

    @Test public void aCpuFazOsCombosDeAssinaturaDoKoga() {
        int[] n = cpuAssinaturas(6);
        assertTrue("a CPU emendou os dois socos na investida: " + n[0], n[0] >= 1);
        assertTrue("a CPU emendou a Garra no M: " + n[1], n[1] >= 1);
    }
}
