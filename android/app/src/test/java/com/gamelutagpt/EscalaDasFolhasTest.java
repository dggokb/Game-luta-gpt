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

    @Test public void lighterGraphicsLevelsDecodeSmallerSheets() {
        // Celular 2400x1080: no LEVE a tela sai a 60% e a folha cheia só gastava memória de vídeo.
        CharacterDefinition c = GeneratedCharacters.get("p15");
        float densidade = SpriteAtlasCache.densidadeTela(2400, 1080);
        float leve = densidade * QualidadeGrafica.escalaDaTela(QualidadeGrafica.ESCALA[QualidadeGrafica.LEVE], 1080);
        for (CharacterDefinition.Animation a : c.animations.values()) {
            if (a.atlas.pixelScale < 2f) continue; // arte 1x: já é a menor
            float e = SpriteAtlasCache.escalaPara(c, a.atlas, leve, SpriteAtlasCache.AMPLIACAO_REDUZIDA);
            float ampliacao = c.profile.worldScale / a.atlas.pixelScale / e * CameraRig.CAMERA_ZOOM * leve;
            assertTrue(a.atlas.resource + " ampliada " + ampliacao, ampliacao <= SpriteAtlasCache.AMPLIACAO_REDUZIDA);
            assertEquals(a.atlas.resource, 0.75f, e, 0f);
        }
        assertEquals(1f, SpriteAtlasCache.escalaPara(c, c.animation(SpriteStates.IDLE).atlas, densidade), 0f);
    }

    @Test public void canonicalArtAndUnknownScreensStayWhole() {
        assertEquals(1f, escala("p08", 1280, 720), 0f);
        assertEquals(1f, escala(GeneratedCharacters.OPPONENT, 1280, 720), 0f);
        assertEquals(1f, escala("p03", 0, 0), 0f);
    }
}
