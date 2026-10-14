package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class AjusteTamanhoTest {
    private final CharacterDefinition p02 = GeneratedCharacters.get("p02");

    private CharacterDefinition.Atlas atlas(String state) { return p02.animations.get(state).atlas; }

    @Before public void clean() { AjusteTamanho.clearForTest(); }

    @Test public void stepsAreOnePercentClampedAndRoundTripTo100() {
        CharacterDefinition.Atlas dash = atlas("DASH");
        assertEquals(100, AjusteTamanho.percent(dash));
        AjusteTamanho.adjust(dash, +5);
        AjusteTamanho.adjust(dash, -1);
        assertEquals(104, AjusteTamanho.percent(dash));
        AjusteTamanho.adjust(dash, +100);
        assertEquals(140, AjusteTamanho.percent(dash));
        AjusteTamanho.adjust(dash, -200);
        assertEquals(70, AjusteTamanho.percent(dash));
        AjusteTamanho.set(dash, 1f);
        assertEquals(0, AjusteTamanho.changed(p02));
    }

    @Test public void sheetsSharedByAnimationsAreOneEntry() {
        List<AjusteTamanho.Folha> folhas = AjusteTamanho.folhas(p02);
        AjusteTamanho.Folha idle = folhas.get(0);
        assertEquals("p02_idle", idle.atlas.resource);
        assertTrue(idle.animacoes.contains("IDLE"));
        assertTrue(idle.animacoes.contains("COMBAT"));
        long distinct = folhas.stream().map(f -> f.atlas.resource).distinct().count();
        assertEquals(folhas.size(), distinct);
        AjusteTamanho.adjust(atlas("COMBAT"), +3);
        assertEquals(103, AjusteTamanho.percent(atlas("IDLE")));
    }

    @Test public void keyChangesWhenTheBakedSheetChanges() {
        CharacterDefinition.Atlas a = atlas("VICTORY");
        CharacterDefinition.Atlas rebuilt = new CharacterDefinition.Atlas(a.resource, a.width - 20, a.height - 30,
            a.rootX, a.rootY, a.columns, a.count, a.pixelScale);
        AjusteTamanho.adjust(a, -8);
        assertEquals(92, AjusteTamanho.percent(a));
        assertEquals("depois de gravado na folha o ajuste antigo não vale mais", 100, AjusteTamanho.percent(rebuilt));
    }

    @Test public void exportListsOnlyChangedSheetsAsValidJson() throws Exception {
        assertEquals(0, new JSONObject(AjusteTamanho.exportJson("t", "d")).getJSONArray("ajustes").length());
        AjusteTamanho.adjust(atlas("DASH"), +5);
        AjusteTamanho.adjust(atlas("THROW_GRAB"), -3);
        JSONObject out = new JSONObject(AjusteTamanho.exportJson("0.1-teste", "20261014-1000"));
        assertEquals("ajuste-tamanho", out.getString("tipo"));
        JSONArray list = out.getJSONArray("ajustes");
        assertEquals(2, list.length());
        JSONObject dash = list.getJSONObject(0);
        assertEquals("p02", dash.getString("personagem"));
        assertEquals("p02_dash", dash.getString("folha"));
        assertEquals(1.05, dash.getDouble("fator"), 1e-9);
        JSONObject grab = list.getJSONObject(1);
        assertEquals(0.97, grab.getDouble("fator"), 1e-9);
        JSONArray states = grab.getJSONArray("animacoes");
        assertEquals("THROW_GRAB", states.getString(0));
        assertEquals("THROW_TOSS", states.getString(1));
    }
}
