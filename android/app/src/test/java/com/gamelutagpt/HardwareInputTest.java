package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;
import org.junit.Test;

/** Teclado e controle físico: direção segurada, botões, comandos e a tela de seleção. */
public class HardwareInputTest {
    @Test
    public void heldKeysBuildTheScreenDirection() {
        HardwareInput in = new HardwareInput();
        assertEquals(0, in.direction());
        assertTrue(in.setHeld(KeyEvent.KEYCODE_D, true));
        assertEquals(1, in.direction());
        in.setHeld(KeyEvent.KEYCODE_S, true);
        assertEquals(2, in.direction());
        in.setHeld(KeyEvent.KEYCODE_D, false);
        assertEquals(3, in.direction());
        in.setHeld(KeyEvent.KEYCODE_DPAD_LEFT, true);
        assertEquals(4, in.direction());
        in.setHeld(KeyEvent.KEYCODE_S, false);
        assertEquals(5, in.direction());
        in.setHeld(KeyEvent.KEYCODE_W, true);
        assertEquals(6, in.direction());
        in.setHeld(KeyEvent.KEYCODE_DPAD_LEFT, false);
        assertEquals(7, in.direction());
        in.setHeld(KeyEvent.KEYCODE_DPAD_RIGHT, true);
        assertEquals(8, in.direction());
        assertFalse(in.setHeld(KeyEvent.KEYCODE_J, true));
        in.reset();
        assertEquals(0, in.direction());
    }

    @Test
    public void opposingDirectionsFollowSocdRules() {
        HardwareInput in = new HardwareInput();
        in.setHeld(KeyEvent.KEYCODE_A, true);
        in.setHeld(KeyEvent.KEYCODE_D, true);
        assertEquals("esquerda + direita = neutro", 0, in.direction());
        in.setHeld(KeyEvent.KEYCODE_W, true);
        in.setHeld(KeyEvent.KEYCODE_S, true);
        assertEquals("cima + baixo = cima", 7, in.direction());
    }

    @Test
    public void downDirectionsFromKeysAskForTagAndUltra() {
        HardwareInput in = new HardwareInput();
        in.setHeld(KeyEvent.KEYCODE_S, true);
        assertTrue(ControlsLayout.isDownDirection(in.direction()));
        in.setHeld(KeyEvent.KEYCODE_A, true);
        assertTrue(ControlsLayout.isDownDirection(in.direction()));
    }

    @Test
    public void keyboardAndPadReachTheSameButtons() {
        assertEquals(PadInput.Button.LIGHT, HardwareInput.button(KeyEvent.KEYCODE_J));
        assertEquals(PadInput.Button.LIGHT, HardwareInput.button(KeyEvent.KEYCODE_BUTTON_X));
        assertEquals(PadInput.Button.MEDIUM, HardwareInput.button(KeyEvent.KEYCODE_K));
        assertEquals(PadInput.Button.MEDIUM, HardwareInput.button(KeyEvent.KEYCODE_BUTTON_Y));
        assertEquals(PadInput.Button.HEAVY, HardwareInput.button(KeyEvent.KEYCODE_L));
        assertEquals(PadInput.Button.HEAVY, HardwareInput.button(KeyEvent.KEYCODE_BUTTON_B));
        assertEquals(PadInput.Button.AUTO, HardwareInput.button(KeyEvent.KEYCODE_BUTTON_A));
        assertEquals(PadInput.Button.SUPER, HardwareInput.button(KeyEvent.KEYCODE_I));
        assertEquals(PadInput.Button.TAG, HardwareInput.button(KeyEvent.KEYCODE_BUTTON_R1));
        assertEquals(PadInput.Button.THROW, HardwareInput.button(KeyEvent.KEYCODE_H));
        assertEquals(PadInput.Button.PUSHBLOCK, HardwareInput.button(KeyEvent.KEYCODE_SEMICOLON));
        assertEquals(PadInput.Button.OVERDRIVE, HardwareInput.button(KeyEvent.KEYCODE_BUTTON_L2));
        assertNull(HardwareInput.button(KeyEvent.KEYCODE_W));
        assertNull(HardwareInput.button(KeyEvent.KEYCODE_ENTER));
    }

    @Test
    public void separateKeysStillMakeThrowAndPushblock() {
        // J + K no mesmo quadro = agarrão, como os dois dedos na tela.
        PadInput pad = new PadInput();
        FighterInput out = new FighterInput();
        pad.press(HardwareInput.button(KeyEvent.KEYCODE_J));
        pad.press(HardwareInput.button(KeyEvent.KEYCODE_K));
        pad.drainInto(out);
        assertTrue(out.grab);
        pad.press(HardwareInput.button(KeyEvent.KEYCODE_K));
        pad.press(HardwareInput.button(KeyEvent.KEYCODE_L));
        pad.drainInto(out);
        assertTrue(out.pushblock);
    }

    @Test
    public void heldAttackKeysChargeMoves() {
        HardwareInput in = new HardwareInput();
        assertFalse(in.attackHeld());
        in.setButtonHeld(KeyEvent.KEYCODE_L, true);
        assertTrue(in.attackHeld());
        in.setButtonHeld(KeyEvent.KEYCODE_I, true);
        in.setButtonHeld(KeyEvent.KEYCODE_L, false);
        assertFalse("SUPER não carrega", in.attackHeld());
        in.setButtonHeld(KeyEvent.KEYCODE_BUTTON_X, true);
        assertTrue(in.attackHeld());
        in.reset();
        assertFalse(in.attackHeld());
    }

    @Test
    public void testCommandsHaveKeys() {
        assertEquals(HardwareInput.Action.DEBUG, HardwareInput.action(KeyEvent.KEYCODE_F1));
        assertEquals(HardwareInput.Action.CHARACTER_SELECT, HardwareInput.action(KeyEvent.KEYCODE_F2));
        assertEquals(HardwareInput.Action.CHARACTER_SELECT, HardwareInput.action(KeyEvent.KEYCODE_BUTTON_SELECT));
        assertEquals(HardwareInput.Action.AI, HardwareInput.action(KeyEvent.KEYCODE_F3));
        assertEquals(HardwareInput.Action.HEAL_PLAYER, HardwareInput.action(KeyEvent.KEYCODE_F5));
        assertEquals(HardwareInput.Action.HEAL_OPPONENT, HardwareInput.action(KeyEvent.KEYCODE_F6));
        assertEquals(HardwareInput.Action.NONE, HardwareInput.action(KeyEvent.KEYCODE_J));
        // Esc e START ficam com a pausa do GameView.
        assertEquals(HardwareInput.Action.NONE, HardwareInput.action(KeyEvent.KEYCODE_ESCAPE));
        assertEquals(HardwareInput.Action.NONE, HardwareInput.action(KeyEvent.KEYCODE_BUTTON_START));
        assertTrue(HardwareInput.handles(KeyEvent.KEYCODE_W));
        assertTrue(HardwareInput.isDirectionKey(KeyEvent.KEYCODE_DPAD_LEFT));
        assertFalse(HardwareInput.isDirectionKey(KeyEvent.KEYCODE_J));
        assertTrue(HardwareInput.handles(KeyEvent.KEYCODE_F5));
        assertFalse(HardwareInput.handles(KeyEvent.KEYCODE_Z));
    }

    @Test
    public void menuKeysTranslateOnlyLetters() {
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, HardwareInput.menuKey(KeyEvent.KEYCODE_A));
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, HardwareInput.menuKey(KeyEvent.KEYCODE_S));
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, HardwareInput.menuKey(KeyEvent.KEYCODE_J));
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, HardwareInput.menuKey(KeyEvent.KEYCODE_SPACE));
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, HardwareInput.menuKey(KeyEvent.KEYCODE_K));
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, HardwareInput.menuKey(KeyEvent.KEYCODE_R));
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, HardwareInput.menuKey(KeyEvent.KEYCODE_TAB));
        assertEquals(KeyEvent.KEYCODE_BUTTON_L1, HardwareInput.menuKey(KeyEvent.KEYCODE_Q));
        // O que cada tela já trata passa igual.
        assertEquals(KeyEvent.KEYCODE_ENTER, HardwareInput.menuKey(KeyEvent.KEYCODE_ENTER));
        assertEquals(KeyEvent.KEYCODE_ESCAPE, HardwareInput.menuKey(KeyEvent.KEYCODE_ESCAPE));
        assertEquals(KeyEvent.KEYCODE_BUTTON_START, HardwareInput.menuKey(KeyEvent.KEYCODE_BUTTON_START));
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, HardwareInput.menuKey(KeyEvent.KEYCODE_DPAD_UP));
        assertTrue(HardwareInput.isDpad(HardwareInput.menuKey(KeyEvent.KEYCODE_W)));
        assertFalse(HardwareInput.isDpad(KeyEvent.KEYCODE_BUTTON_A));
    }
}
