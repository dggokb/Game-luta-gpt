package com.gamelutagpt.pc;

import android.util.Log;
import android.view.KeyEvent;
import com.gamelutagpt.GameView;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

/**
 * Controles (gamepad) no PC pelo SDL (Jamepad): o mapa é o do Xbox em qualquer controle que
 * o SDL conheça (Xbox, PlayStation, Switch Pro, genéricos). Cada botão vira a tecla do
 * Android equivalente e entra no jogo pelo mesmo caminho do teclado ({@code HardwareInput}).
 * O analógico esquerdo vale como D-pad. Sem a biblioteca nativa, segue só com o teclado.
 */
final class Controles implements Runnable {
    private static final String TAG = "Controles";
    private static final int MAX_CONTROLES = 4;
    private static final float ZONA_MORTA = 0.5f;
    /** Segurar uma direção repete como no teclado (andar o cursor da seleção). */
    private static final long REPETIR_APOS_MS = 400, REPETIR_A_CADA_MS = 110;

    /** Botões na ordem do estado: código do Android de cada um. */
    private static final int[] CODIGOS = {
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
    };

    private final GameView jogo;
    private final boolean[] antes = new boolean[CODIGOS.length];
    private final long[] desde = new long[CODIGOS.length];
    private final int[] repeticoes = new int[CODIGOS.length];
    private ControllerManager sdl;
    private Thread thread;
    private volatile boolean rodando;

    Controles(GameView jogo) { this.jogo = jogo; }

    void iniciar() {
        if (Boolean.getBoolean("gameluta.semControle")) return;
        try {
            sdl = new ControllerManager();
            sdl.initSDLGamepad();
        } catch (Throwable e) {
            // Biblioteca nativa ausente (sistema não suportado): só teclado e mouse.
            Log.w(TAG, "controle indisponível: " + e);
            sdl = null;
            return;
        }
        rodando = true;
        thread = new Thread(this, "Controles");
        thread.setDaemon(true);
        thread.start();
    }

    void parar() {
        rodando = false;
        Thread t = thread;
        if (t != null) {
            try {
                t.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (sdl != null) {
            try {
                sdl.quitSDLGamepad();
            } catch (Throwable ignored) {
                // Fechando o jogo.
            }
        }
    }

    @Override public void run() {
        boolean[] agora = new boolean[CODIGOS.length];
        int conectadosAntes = -1;
        while (rodando) {
            try {
                sdl.update();
                java.util.Arrays.fill(agora, false);
                int conectados = 0;
                for (int i = 0; i < MAX_CONTROLES; i++) {
                    ControllerState s = sdl.getState(i);
                    if (!s.isConnected) continue;
                    conectados++;
                    ler(s, agora);
                }
                if (conectados != conectadosAntes) {
                    if (conectadosAntes >= 0 || conectados > 0) Log.i(TAG, conectados + " controle(s) conectado(s)");
                    conectadosAntes = conectados;
                }
                aplicar(agora, System.currentTimeMillis());
                Thread.sleep(8);
            } catch (InterruptedException e) {
                return;
            } catch (RuntimeException e) {
                Log.w(TAG, "erro lendo o controle", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    private static void ler(ControllerState s, boolean[] b) {
        // O analógico esquerdo (Y para cima é positivo) soma ao D-pad.
        b[0] |= s.dpadUp || s.leftStickY > ZONA_MORTA;
        b[1] |= s.dpadDown || s.leftStickY < -ZONA_MORTA;
        b[2] |= s.dpadLeft || s.leftStickX < -ZONA_MORTA;
        b[3] |= s.dpadRight || s.leftStickX > ZONA_MORTA;
        b[4] |= s.a;
        b[5] |= s.b;
        b[6] |= s.x;
        b[7] |= s.y;
        b[8] |= s.lb;
        b[9] |= s.rb;
        b[10] |= s.leftTrigger > 0.5f;
        b[11] |= s.rightTrigger > 0.5f;
        b[12] |= s.start;
        b[13] |= s.back;
        b[14] |= s.leftStickClick;
        b[15] |= s.rightStickClick;
    }

    private void aplicar(boolean[] agora, long ms) {
        for (int i = 0; i < CODIGOS.length; i++) {
            int codigo = CODIGOS[i];
            if (agora[i] && !antes[i]) {
                desde[i] = ms;
                repeticoes[i] = 0;
                jogo.onKeyDown(codigo, new KeyEvent(KeyEvent.ACTION_DOWN, codigo, 0));
            } else if (!agora[i] && antes[i]) {
                jogo.onKeyUp(codigo, new KeyEvent(KeyEvent.ACTION_UP, codigo));
            } else if (agora[i] && i < 4) {
                long segurando = ms - desde[i];
                int devidas = segurando < REPETIR_APOS_MS ? 0 : 1 + (int) ((segurando - REPETIR_APOS_MS) / REPETIR_A_CADA_MS);
                while (repeticoes[i] < devidas) {
                    repeticoes[i]++;
                    jogo.onKeyDown(codigo, new KeyEvent(KeyEvent.ACTION_DOWN, codigo, repeticoes[i]));
                }
            }
            antes[i] = agora[i];
        }
    }
}
