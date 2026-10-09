package com.gamelutagpt;

import android.graphics.ColorFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/**
 * Real Android graphics initialization smoke regression test. A 3x5 color
 * matrix (15 entries) previously crashed GameView.onCreate before its first
 * frame because android.graphics.ColorMatrix requires FOUR rows of 5.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class SpriteStartupSmokeTest {
    @Test public void inverseCanvasFilterBuildsWithoutMissingMatrixRow() {
        // AndroidRenderCanvas is instantiated as a GameView field, before
        // Android's Activity can display the first frame.
        assertNotNull(new AndroidRenderCanvas());
    }

    @Test public void entireGameViewCanBeCreatedWithoutAndroidColorMatrixCrash() {
        assertNotNull(new GameView(org.robolectric.RuntimeEnvironment.getApplication()));
    }

    @Test public void opponentColorFilterCanBeCreatedDuringActivityStartup() {
        ColorFilter filter=SpriteFighterRenderer.washedOut();
        assertNotNull(filter);
    }
}
