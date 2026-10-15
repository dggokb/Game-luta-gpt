package com.gamelutagpt;

import android.view.KeyEvent;
import java.util.Locale;

/**
 * Como mostrar os botões nos textos do jogo (o tutorial), conforme o que o jogador está
 * usando: o toque, o teclado ou um controle. O controle é reconhecido pelo nome/tipo
 * (PlayStation, Nintendo); qualquer outro usa o padrão Xbox.
 *
 * <p>Os textos usam marcas que viram o botão certo: {@code {L} {M} {H} {COMBO} {SUPER}
 * {AGARRAO}}. As direções continuam em setas (setas/WASD no teclado, D-pad/analógico no
 * controle). O mapa de teclas é o do {@link HardwareInput}.
 */
final class Comandos {
    enum Esquema { TOQUE, TECLADO, XBOX, PLAYSTATION, NINTENDO }

    private Comandos() {}

    /** O controle pelo nome do aparelho (Android) ou pelo tipo que o SDL informa (PC). */
    static Esquema doControle(String nome) {
        String n = nome == null ? "" : nome.toUpperCase(Locale.ROOT);
        if (n.contains("PS3") || n.contains("PS4") || n.contains("PS5") || n.contains("SONY") ||
            n.contains("DUALSHOCK") || n.contains("DUALSENSE") || n.contains("PLAYSTATION") ||
            n.equals("WIRELESS CONTROLLER")) {
            return Esquema.PLAYSTATION;
        }
        if (n.contains("NINTENDO") || n.contains("SWITCH") || n.contains("JOYCON") || n.contains("JOY-CON") ||
            n.contains("PRO CONTROLLER")) {
            return Esquema.NINTENDO;
        }
        return Esquema.XBOX;
    }

    /** Botão de controle (A/B/X/Y, gatilhos, START...)? D-pad não conta: o teclado também manda. */
    static boolean isBotaoDeControle(int keyCode) {
        return keyCode >= KeyEvent.KEYCODE_BUTTON_A && keyCode <= KeyEvent.KEYCODE_BUTTON_MODE;
    }

    /** Tecla que só um teclado manda (letras, números, F1–F12, Enter, Esc...). */
    static boolean isTeclaDeTeclado(int keyCode) {
        if (isBotaoDeControle(keyCode)) return false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_UNKNOWN:
                return false;
            default:
                return true;
        }
    }

    /** O texto com as marcas trocadas pelos botões do esquema. */
    static String texto(String modelo, Esquema e) {
        if (modelo.indexOf('{') < 0) return modelo;
        return modelo
            .replace("{AGARRAO}", agarrao(e))
            .replace("{COMBO}", botao(e, "COMBO"))
            .replace("{SUPER}", botao(e, "SUPER"))
            .replace("{L}", botao(e, "L"))
            .replace("{M}", botao(e, "M"))
            .replace("{H}", botao(e, "H"));
    }

    /** Um botão: L, M, H, COMBO ou SUPER. */
    static String botao(Esquema e, String id) {
        switch (e) {
            case TECLADO: return "[" + tecla(id) + "]";
            case XBOX: return "[" + padrao(id, "X", "Y", "B", "A", "LB") + "]";
            case PLAYSTATION: return "[" + padrao(id, "□", "△", "○", "×", "L1") + "]";
            case NINTENDO: return "[" + padrao(id, "Y", "X", "A", "B", "L") + "]";
            default: return id;
        }
    }

    private static String agarrao(Esquema e) {
        switch (e) {
            case TECLADO: return "[H] (ou " + botao(e, "L") + " + " + botao(e, "M") + " juntos)";
            case XBOX: return "[RT] (ou " + botao(e, "L") + " + " + botao(e, "M") + " juntos)";
            case PLAYSTATION: return "[R2] (ou " + botao(e, "L") + " + " + botao(e, "M") + " juntos)";
            case NINTENDO: return "[ZR] (ou " + botao(e, "L") + " + " + botao(e, "M") + " juntos)";
            default: return "L + M juntos (o botão entre eles)";
        }
    }

    /** Agarrão sem palavras (lista de comandos, em qualquer idioma): "[H] / [J] + [K]". */
    static String agarraoCurto(Esquema e) {
        String juntos = botao(e, "L") + " + " + botao(e, "M");
        switch (e) {
            case TECLADO: return "[H] / " + juntos;
            case XBOX: return "[RT] / " + juntos;
            case PLAYSTATION: return "[R2] / " + juntos;
            case NINTENDO: return "[ZR] / " + juntos;
            default: return juntos;
        }
    }

    /** Teclas do HardwareInput: J K L = L M H, U = COMBO, I = SUPER. */
    private static String tecla(String id) {
        switch (id) {
            case "L": return "J";
            case "M": return "K";
            case "H": return "L";
            case "COMBO": return "U";
            case "SUPER": return "I";
            default: return id;
        }
    }

    /**
     * Mesma posição em qualquer controle (o SDL e o Android mapeiam por posição): esquerda =
     * L, cima = M, direita = H, baixo = COMBO, ombro esquerdo = SUPER.
     */
    private static String padrao(String id, String esquerda, String cima, String direita, String baixo, String ombro) {
        switch (id) {
            case "L": return esquerda;
            case "M": return cima;
            case "H": return direita;
            case "COMBO": return baixo;
            case "SUPER": return ombro;
            default: return id;
        }
    }
}
