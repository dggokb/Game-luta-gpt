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

}
