package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** A câmera se adianta para o lado de quem corre e volta ao parar, sem tranco. */
public class CameraLeadTest {
    @Test public void leadsTheRunAndComesBack() {
        CameraRig camera = new CameraRig();
        float last = 0f;
        for (int i = 0; i < 90; i++) {
            camera.updateLead(1);
            assertTrue("no jump", Math.abs(camera.lead() - last) < 6f);
            last = camera.lead();
        }
        assertEquals(CameraRig.CAMERA_RUN_LEAD * 0.5f, camera.lead(), 1f);
        for (int i = 0; i < 120; i++) camera.updateLead(0);
        assertEquals(0f, camera.lead(), 0.5f);
        // Os dois correndo um para o outro: nada.
        for (int i = 0; i < 90; i++) camera.updateLead(1 - 1);
        assertEquals(0f, camera.lead(), 0.5f);
    }
}
