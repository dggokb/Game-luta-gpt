package com.gamelutagpt;

import android.view.KeyEvent;

/**
 * Teclado e controle físico: o mesmo mapa vale para o jogo de PC (as teclas chegam
 * convertidas para os códigos do Android) e para teclado ou controle Bluetooth no
 * Android. Só guarda o que está segurado e traduz teclas; quem decide o golpe é o motor.
 *
 * <p>Os códigos {@code KeyEvent.KEYCODE_*} são constantes de compilação: a classe não
 * carrega nada do Android em tempo de execução e é testável em JUnit puro.
 *
 * <pre>
 * Luta        teclado            controle
 * direção     WASD ou setas      D-pad / analógico
 * L / M / H   J / K / L          X / Y / B
 * AUTO        U                  A
 * SUPER       I                  LB
 * TAG         O                  RB
 * agarrão     H (ou J + K)       RT
 * pushblock   ; (ou K + L)       R3
 * OVERDRIVE   P (ou I + O)       LT
 * pausa       Esc                START
 * seleção     F2                 SELECT (no modo história: a pausa)
 * DEBUG       F1                 (só no modo teste, como os botões de teste)
 * IA on/off   F3
 * vida P1/CPU F5 / F6
 * </pre>
 *
 * <p>Nos menus (inicial, capítulos, pausa, seleção, história) as teclas viram os botões do
 * controle que o GameView já entende: WASD = D-pad, J/Espaço = A, K = B, R = X, Tab = Y,
 * Q/E/O = L1. Enter, Esc, Back e os botões do controle passam como estão.
 */
final class HardwareInput {
    /** Comandos fora da luta (os mesmos botões de teste da tela). */
    enum Action { NONE, DEBUG, AI, CHARACTER_SELECT, HEAL_PLAYER, HEAL_OPPONENT }

    private boolean up, down, left, right;
    /** L, M ou H segurados (golpes carregáveis). */
    private boolean light, medium, heavy;

    /** Direção segurada no formato do D-pad da tela: 1..8 a partir da direita, 0 neutro. */
    int direction() {
        // SOCD: esquerda + direita = neutro; cima + baixo = cima.
        int x = (right ? 1 : 0) - (left ? 1 : 0);
        int y = up ? -1 : down ? 1 : 0;
        if (x == 0 && y == 0) return 0;
        if (y == 0) return x > 0 ? 1 : 5;
        if (x == 0) return y > 0 ? 3 : 7;
        if (y > 0) return x > 0 ? 2 : 4;
        return x > 0 ? 8 : 6;
    }

    /**
     * Atualiza o que está segurado. Devolve true quando a tecla é de direção (a direção
     * do pad precisa ser refeita).
     */
    boolean setHeld(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_W: up = pressed; return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_S: down = pressed; return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_A: left = pressed; return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_D: right = pressed; return true;
            default: return false;
        }
    }

    /** Guarda se L/M/H continuam apertados (o pad pergunta para os golpes carregáveis). */
    void setButtonHeld(int keyCode, boolean pressed) {
        PadInput.Button b = button(keyCode);
        if (b == PadInput.Button.LIGHT) light = pressed;
        else if (b == PadInput.Button.MEDIUM) medium = pressed;
        else if (b == PadInput.Button.HEAVY) heavy = pressed;
    }

    boolean attackHeld() {
        return light || medium || heavy;
    }

    void reset() {
        up = down = left = right = false;
        light = medium = heavy = false;
    }

    /** Botão de luta da tecla, ou null. */
    static PadInput.Button button(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_J: case KeyEvent.KEYCODE_BUTTON_X: return PadInput.Button.LIGHT;
            case KeyEvent.KEYCODE_K: case KeyEvent.KEYCODE_BUTTON_Y: return PadInput.Button.MEDIUM;
            case KeyEvent.KEYCODE_L: case KeyEvent.KEYCODE_BUTTON_B: return PadInput.Button.HEAVY;
            case KeyEvent.KEYCODE_U: case KeyEvent.KEYCODE_BUTTON_A: return PadInput.Button.AUTO;
            case KeyEvent.KEYCODE_I: case KeyEvent.KEYCODE_BUTTON_L1: return PadInput.Button.SUPER;
            case KeyEvent.KEYCODE_O: case KeyEvent.KEYCODE_BUTTON_R1: return PadInput.Button.TAG;
            case KeyEvent.KEYCODE_H: case KeyEvent.KEYCODE_BUTTON_R2: return PadInput.Button.THROW;
            case KeyEvent.KEYCODE_SEMICOLON: case KeyEvent.KEYCODE_BUTTON_THUMBR: return PadInput.Button.PUSHBLOCK;
            case KeyEvent.KEYCODE_P: case KeyEvent.KEYCODE_BUTTON_L2: return PadInput.Button.OVERDRIVE;
            default: return null;
        }
    }

    /** Comandos da luta. Esc/Back/START não estão aqui: abrem a pausa (GameView). */
    static Action action(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_F1: return Action.DEBUG;
            case KeyEvent.KEYCODE_F3: return Action.AI;
            case KeyEvent.KEYCODE_F2:
            case KeyEvent.KEYCODE_BUTTON_SELECT: return Action.CHARACTER_SELECT;
            case KeyEvent.KEYCODE_F5: return Action.HEAL_PLAYER;
            case KeyEvent.KEYCODE_F6: return Action.HEAL_OPPONENT;
            default: return Action.NONE;
        }
    }

    /** Tecla de direção da luta (setas, D-pad ou WASD). */
    static boolean isDirectionKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_D:
                return true;
            default:
                return false;
        }
    }

    /** A tecla interessa à luta (direção, botão ou comando)? */
    static boolean handles(int keyCode) {
        return isDirectionKey(keyCode) || button(keyCode) != null || action(keyCode) != Action.NONE;
    }

    /**
     * Tecla dos menus no formato do controle. Só as letras mudam (WASD = D-pad, J/Espaço = A,
     * K/Backspace = B, R/U = X, Tab/I = Y, Q/E/O = L1); as outras teclas passam como estão,
     * para Enter, Esc, Back e os botões do controle manterem o que cada tela faz com eles.
     */
    static int menuKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_W: return KeyEvent.KEYCODE_DPAD_UP;
            case KeyEvent.KEYCODE_S: return KeyEvent.KEYCODE_DPAD_DOWN;
            case KeyEvent.KEYCODE_A: return KeyEvent.KEYCODE_DPAD_LEFT;
            case KeyEvent.KEYCODE_D: return KeyEvent.KEYCODE_DPAD_RIGHT;
            case KeyEvent.KEYCODE_SPACE: case KeyEvent.KEYCODE_J: return KeyEvent.KEYCODE_BUTTON_A;
            case KeyEvent.KEYCODE_DEL: case KeyEvent.KEYCODE_K: return KeyEvent.KEYCODE_BUTTON_B;
            case KeyEvent.KEYCODE_R: case KeyEvent.KEYCODE_U: return KeyEvent.KEYCODE_BUTTON_X;
            case KeyEvent.KEYCODE_TAB: case KeyEvent.KEYCODE_I: return KeyEvent.KEYCODE_BUTTON_Y;
            case KeyEvent.KEYCODE_Q: case KeyEvent.KEYCODE_E: case KeyEvent.KEYCODE_O: return KeyEvent.KEYCODE_BUTTON_L1;
            default: return keyCode;
        }
    }

    /** Direção do D-pad (segurar repete, para andar o cursor dos menus). */
    static boolean isDpad(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }
}
