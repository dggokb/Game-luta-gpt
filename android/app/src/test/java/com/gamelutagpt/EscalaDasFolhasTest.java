package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** A arte HD só é decodificada menor onde a tela não precisa dela inteira. */
public class EscalaDasFolhasTest {
    private static float escala(String id, int largura, int altura) {
        CharacterDefinition c = GeneratedCharacters.get(id);
        CharacterDefinition.Atlas idle = c.animation(SpriteStates.IDLE).atlas;
        return SpriteAtlasCache.escalaPara(c, idle, SpriteAtlasCache.densidadeTela(largura, altura));
    }

    @Test public void hdShrinksOnlyWhereTheScreenIsSmall() {
        assertEquals(0.75f, escala("p03", 1280, 720), 0f);
        // Retrato ou paisagem: o lado maior é sempre os 1280 virtuais.
        assertEquals(0.75f, escala("p03", 720, 1280), 0f);
        assertEquals(1f, escala("p03", 1920, 1080), 0f);
        assertEquals(1f, escala("p03", 2400, 1080), 0f);
        assertEquals(1f, escala("p03", 2560, 1440), 0f);
    }

    @Test public void normalFightZoomNeverStretchesAReducedSheet() {
        for (String id : GeneratedCharacters.SELECTABLE) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            for (int[] tela : new int[][] {{1280, 720}, {1600, 720}, {1920, 1080}, {2400, 1080}}) {
                float densidade = SpriteAtlasCache.densidadeTela(tela[0], tela[1]);
                for (CharacterDefinition.Animation a : c.animations.values()) {
                    float e = SpriteAtlasCache.escalaPara(c, a.atlas, densidade);
                    if (e == 1f) continue;
                    float ampliacao = c.profile.worldScale / a.atlas.pixelScale / e * CameraRig.CAMERA_ZOOM * densidade;
                    assertTrue(id + " " + a.atlas.resource + " ampliada " + ampliacao, ampliacao <= 1.01f);
                    assertTrue("nunca abaixo da arte 1x", e * a.atlas.pixelScale >= 1f);
                }
            }
        }
    }

    @Test public void canonicalArtAndUnknownScreensStayWhole() {
        assertEquals(1f, escala("p08", 1280, 720), 0f);
        assertEquals(1f, escala(GeneratedCharacters.OPPONENT, 1280, 720), 0f);
        assertEquals(1f, escala("p03", 0, 0), 0f);
    }
}
