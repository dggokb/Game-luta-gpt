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

}
