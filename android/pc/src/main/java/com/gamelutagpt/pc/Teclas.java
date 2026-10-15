package com.gamelutagpt.pc;

import android.view.KeyEvent;

/** Teclas do AWT para os códigos do Android (o mapa de controles fica no HardwareInput do jogo). */
final class Teclas {
    private Teclas() {}

    /** Ç do teclado ABNT2: fica onde o ";" do teclado americano fica (pushblock). */
    private static final int CEDILHA = java.awt.event.KeyEvent.getExtendedKeyCodeForChar('ç');

    static int paraAndroid(int awt, int extended) {
        if (extended == CEDILHA) return KeyEvent.KEYCODE_SEMICOLON;
        return paraAndroid(awt);
    }

    static int paraAndroid(int awt) {
        if (awt >= java.awt.event.KeyEvent.VK_A && awt <= java.awt.event.KeyEvent.VK_Z)
            return KeyEvent.KEYCODE_A + (awt - java.awt.event.KeyEvent.VK_A);
        if (awt >= java.awt.event.KeyEvent.VK_0 && awt <= java.awt.event.KeyEvent.VK_9)
            return KeyEvent.KEYCODE_0 + (awt - java.awt.event.KeyEvent.VK_0);
        if (awt >= java.awt.event.KeyEvent.VK_F1 && awt <= java.awt.event.KeyEvent.VK_F12)
            return KeyEvent.KEYCODE_F1 + (awt - java.awt.event.KeyEvent.VK_F1);
        switch (awt) {
            case java.awt.event.KeyEvent.VK_UP: case java.awt.event.KeyEvent.VK_KP_UP: return KeyEvent.KEYCODE_DPAD_UP;
            case java.awt.event.KeyEvent.VK_DOWN: case java.awt.event.KeyEvent.VK_KP_DOWN: return KeyEvent.KEYCODE_DPAD_DOWN;
            case java.awt.event.KeyEvent.VK_LEFT: case java.awt.event.KeyEvent.VK_KP_LEFT: return KeyEvent.KEYCODE_DPAD_LEFT;
            case java.awt.event.KeyEvent.VK_RIGHT: case java.awt.event.KeyEvent.VK_KP_RIGHT: return KeyEvent.KEYCODE_DPAD_RIGHT;
            case java.awt.event.KeyEvent.VK_ENTER: return KeyEvent.KEYCODE_ENTER;
            case java.awt.event.KeyEvent.VK_ESCAPE: return KeyEvent.KEYCODE_ESCAPE;
            case java.awt.event.KeyEvent.VK_SPACE: return KeyEvent.KEYCODE_SPACE;
            case java.awt.event.KeyEvent.VK_BACK_SPACE: return KeyEvent.KEYCODE_DEL;
            case java.awt.event.KeyEvent.VK_DELETE: return KeyEvent.KEYCODE_FORWARD_DEL;
            case java.awt.event.KeyEvent.VK_TAB: return KeyEvent.KEYCODE_TAB;
            case java.awt.event.KeyEvent.VK_SEMICOLON: return KeyEvent.KEYCODE_SEMICOLON;
            case java.awt.event.KeyEvent.VK_COMMA: return KeyEvent.KEYCODE_COMMA;
            case java.awt.event.KeyEvent.VK_PERIOD: return KeyEvent.KEYCODE_PERIOD;
            case java.awt.event.KeyEvent.VK_SLASH: return KeyEvent.KEYCODE_SLASH;
            case java.awt.event.KeyEvent.VK_MINUS: return KeyEvent.KEYCODE_MINUS;
            case java.awt.event.KeyEvent.VK_EQUALS: return KeyEvent.KEYCODE_EQUALS;
            case java.awt.event.KeyEvent.VK_SHIFT: return KeyEvent.KEYCODE_SHIFT_LEFT;
            case java.awt.event.KeyEvent.VK_CONTROL: return KeyEvent.KEYCODE_CTRL_LEFT;
            case java.awt.event.KeyEvent.VK_ALT: return KeyEvent.KEYCODE_ALT_LEFT;
            default: return KeyEvent.KEYCODE_UNKNOWN;
        }
    }
}
