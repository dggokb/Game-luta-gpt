package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class AjusteCaixasTest {
    private final CharacterDefinition p07 = GeneratedCharacters.get("p07");

    @Before public void clean() { AjusteCaixas.clearForTest(); }

    private AjusteCaixas.Campo campo(List<AjusteCaixas.Item> itens, String path) {
        for (AjusteCaixas.Item item : itens) for (AjusteCaixas.Campo f : item.campos) if (f.path.equals(path)) return f;
        throw new AssertionError(path);
    }

    @Test public void everyMoveHasReachAndHeightAndTheBodyIsShared() {
        List<AjusteCaixas.Item> itens = AjusteCaixas.itens(p07);
        assertEquals("CORPO", itens.get(0).id);
        assertEquals("AGACHADO", itens.get(1).id);
        AjusteCaixas.Campo reach = campo(itens, "moves.L.reach");
        assertEquals(p07.moves.get("L").attack.reach, reach.base, 0f);
        campo(itens, "specialMoves.S2.hitHeight");
        // A base do acerto estica a caixa para baixo (gancho): por padrão, altura menos a folga.
        assertEquals(Math.round(p07.moves.get("2H").attack.hitFloor), campo(itens, "moves.2H.hitFloor").base, 0f);
        campo(itens, "fighter.energy.spawnX");
        // A largura do corpo aparece em pé e agachado: um ajuste vale para os dois.
        AjusteCaixas.adjust(p07, itens.get(0).campos.get(0), +2);
        assertEquals(p07.fighter.body.halfWidth + 4, AjusteCaixas.value(p07, itens.get(1).campos.get(0)), 0f);
        assertEquals(1, AjusteCaixas.changed(p07));
    }

    @Test public void adjustmentOnAnOldBaseStopsCounting() {
        AjusteCaixas.Campo f = new AjusteCaixas.Campo("moves.L.reach", "alcance", 88f, 2f);
        AjusteCaixas.adjust(p07, f, +4);
        assertEquals(96f, AjusteCaixas.value(p07, f), 0f);
        AjusteCaixas.Campo baked = new AjusteCaixas.Campo("moves.L.reach", "alcance", 96f, 2f);
        assertEquals("depois de gravado, a base nova não confere", 96f, AjusteCaixas.value(p07, baked), 0f);
        AjusteCaixas.Campo other = new AjusteCaixas.Campo("moves.L.reach", "alcance", 90f, 2f);
        assertEquals(90f, AjusteCaixas.value(p07, other), 0f);
    }

    @Test public void exportHasBeforeAndAfter() throws Exception {
        List<AjusteCaixas.Item> itens = AjusteCaixas.itens(p07);
        AjusteCaixas.Campo reach = campo(itens, "moves.H.reach");
        AjusteCaixas.adjust(p07, reach, +5);
        JSONObject out = new JSONObject(AjusteCaixas.exportJson("t", "d"));
        assertEquals("ajuste-caixas", out.getString("tipo"));
        JSONArray list = out.getJSONArray("ajustes");
        assertEquals(1, list.length());
        JSONObject a = list.getJSONObject(0);
        assertEquals("p07", a.getString("personagem"));
        assertEquals("moves.H.reach", a.getString("campo"));
        assertEquals(Math.round(reach.base), a.getInt("antes"));
        assertEquals(Math.round(reach.base) + 10, a.getInt("valor"));
    }
}
