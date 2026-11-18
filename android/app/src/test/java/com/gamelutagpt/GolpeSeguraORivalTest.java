package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * O golpe de várias pancadas segura o rival (o dono, 18/11: "tem supers que o adversário fica
 * solto, quicando no ar; parece que o golpe não segura o personagem"). No chão ele fica em pé
 * atordoado na frente (o S3 do Ryūga, que paira rente ao chão, não dá mais o tranco para cima); no
 * ar ele fica preso na altura em que foi pego até a última pancada, e nenhuma escapa. Os golpes que
 * voam de verdade (o S2 e o S4 da Yin-lu) levam o rival subindo junto, de propósito.
 */
public class GolpeSeguraORivalTest {
    private static final String[][] GOLPES = {
        {"player_base", "S3"}, {"player_base", "SUPER"},
        {"p15", "S3"}, {"p15", "SUPER"},
        {"p16", "S4"}, {"p16", "SUPER"},
        {"p02", "SUPER"},
    };

    /** Solta o golpe e devolve a altura do rival em cada acerto; [-1] se ele ficou solto com o golpe andando. */
    private static List<Integer> alturas(String id, String golpe, boolean noAr, int[] solto) {
        CharacterDefinition c = GeneratedCharacters.get(id);
        CombatEngine e = new CombatEngine(new FighterState(c, "P1"), new FighterState(GeneratedCharacters.get("p03"), "P2"),
            900f, 1010f, new CombatConfig());
        e.fighter(1).state.life = 10_000_000;
        e.fighter(0).state.superMeter = 5000;
        FighterInput i0 = new FighterInput(), i1 = new FighterInput();
        if (noAr) {
            CombatFighter d = e.fighter(1);
            d.y = Arena.GROUND_Y - 110f;
            d.grounded = false;
            d.status = CombatFighter.Status.AIR_HITSTUN;
            d.stunLeft = d.stunTotal = 40;
            d.vy = -200f;
        }
        if (golpe.equals("SUPER")) i0.superAttack = true; else i0.special = golpe;
        List<Integer> h = new ArrayList<>();
        boolean ativo = false;
        for (int t = 0; t < 400; t++) {
            e.step(i0, i1);
            i0.clear();
            CombatFighter p = e.fighter(0), d = e.fighter(1);
            for (CombatEngine.HitEvent ev : e.events()) if (ev.attacker == 0 && !ev.blocked) h.add(Math.round(Arena.GROUND_Y - d.y));
            boolean golpeando = p.attacking() || p.status != CombatFighter.Status.NEUTRAL;
            if (golpeando) ativo = true;
            if (!h.isEmpty() && golpeando && d.status == CombatFighter.Status.NEUTRAL) solto[0]++;
            if (ativo && !golpeando && t > 30) break;
        }
        return h;
    }

    @Test public void noChaoORivalFicaEmPeAteOUltimo() {
        for (String[] g : GOLPES) {
            if (g[0].equals("p02")) continue;   // o Super dela é contra-golpe: no chão só sai lendo um golpe
            int[] solto = {0};
            List<Integer> h = alturas(g[0], g[1], false, solto);
            int esperado = GeneratedCharacters.get(g[0]).attack(g[1]).maxHits;
            assertTrue(g[0] + " " + g[1] + ": acertou " + h, h.size() >= Math.min(esperado, 2));
            for (int i = 0; i < h.size() - 1; i++) assertTrue(g[0] + " " + g[1] + ": em pé até o último " + h, h.get(i) <= 3);
            assertEquals(g[0] + " " + g[1] + ": nunca solto", 0, solto[0]);
        }
    }

    @Test public void noArORivalFicaPresoNaAlturaEmQueFoiPego() {
        for (String[] g : GOLPES) {
            int[] solto = {0};
            List<Integer> h = alturas(g[0], g[1], true, solto);
            assertTrue(g[0] + " " + g[1] + ": acertou " + h, h.size() >= 2);
            for (int x : h) assertTrue(g[0] + " " + g[1] + ": preso na mesma altura " + h, Math.abs(x - h.get(0)) <= 6);
            assertEquals(g[0] + " " + g[1] + ": nunca solto", 0, solto[0]);
        }
    }

    @Test public void nenhumaPancadaDaRajadaEscapaNoAr() {
        int[] solto = {0};
        assertEquals("as 9 da rajada da Natisuko", 9, alturas("p15", "S3", true, solto).size());
    }
}
