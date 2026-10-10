package com.gamelutagpt;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class P01VisualTuningTest {
    @Test public void editsArePersistedClampedAndSpecificToOneAnimation() {
        Context context=org.robolectric.RuntimeEnvironment.getApplication();
        P01VisualTuning.reset(context);
        assertEquals(1f,P01VisualTuning.scale("player_base_dash"),.001f);
        P01VisualTuning.adjust(context,0,+1);
        P01VisualTuning.adjust(context,1,+1);
        P01VisualTuning.adjust(context,1,+1);
        P01VisualTuning.adjust(context,2,-1);
        assertEquals(1.05f,P01VisualTuning.scale("player_base_dash"),.001f);
        assertEquals(1.10f,P01VisualTuning.scale("player_base_crouch_heavy"),.001f);
        assertEquals(.95f,P01VisualTuning.scale("player_base_victory"),.001f);
        assertEquals(1f,P01VisualTuning.scale("player_base_idle"),.001f);
        P01VisualTuning.load(context);
        assertEquals(105,P01VisualTuning.percent(0));
        assertEquals(110,P01VisualTuning.percent(1));
        assertEquals(95,P01VisualTuning.percent(2));
        for(int i=0;i<12;i++) P01VisualTuning.adjust(context,0,-1);
        assertEquals(80,P01VisualTuning.percent(0));
        for(int i=0;i<15;i++) P01VisualTuning.adjust(context,2,+1);
        assertEquals(125,P01VisualTuning.percent(2));
        P01VisualTuning.reset(context);
        for(int i=0;i<3;i++)assertEquals(100,P01VisualTuning.percent(i));
    }
}
