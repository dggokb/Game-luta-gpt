package android.view;

/** Evento de tecla com os mesmos códigos do Android (o PC converte as teclas do AWT para eles). */
public class KeyEvent {
    public static final int ACTION_DOWN = 0, ACTION_UP = 1;

    public static final int KEYCODE_UNKNOWN = 0;
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_0 = 7, KEYCODE_1 = 8, KEYCODE_2 = 9, KEYCODE_3 = 10, KEYCODE_4 = 11;
    public static final int KEYCODE_5 = 12, KEYCODE_6 = 13, KEYCODE_7 = 14, KEYCODE_8 = 15, KEYCODE_9 = 16;
    public static final int KEYCODE_DPAD_UP = 19, KEYCODE_DPAD_DOWN = 20, KEYCODE_DPAD_LEFT = 21, KEYCODE_DPAD_RIGHT = 22;
    public static final int KEYCODE_DPAD_CENTER = 23;
    public static final int KEYCODE_A = 29, KEYCODE_B = 30, KEYCODE_C = 31, KEYCODE_D = 32, KEYCODE_E = 33;
    public static final int KEYCODE_F = 34, KEYCODE_G = 35, KEYCODE_H = 36, KEYCODE_I = 37, KEYCODE_J = 38;
    public static final int KEYCODE_K = 39, KEYCODE_L = 40, KEYCODE_M = 41, KEYCODE_N = 42, KEYCODE_O = 43;
    public static final int KEYCODE_P = 44, KEYCODE_Q = 45, KEYCODE_R = 46, KEYCODE_S = 47, KEYCODE_T = 48;
    public static final int KEYCODE_U = 49, KEYCODE_V = 50, KEYCODE_W = 51, KEYCODE_X = 52, KEYCODE_Y = 53;
    public static final int KEYCODE_Z = 54;
    public static final int KEYCODE_COMMA = 55, KEYCODE_PERIOD = 56;
    public static final int KEYCODE_ALT_LEFT = 57, KEYCODE_ALT_RIGHT = 58, KEYCODE_SHIFT_LEFT = 59, KEYCODE_SHIFT_RIGHT = 60;
    public static final int KEYCODE_TAB = 61, KEYCODE_SPACE = 62, KEYCODE_ENTER = 66, KEYCODE_DEL = 67;
    public static final int KEYCODE_MINUS = 69, KEYCODE_EQUALS = 70, KEYCODE_LEFT_BRACKET = 71, KEYCODE_RIGHT_BRACKET = 72;
    public static final int KEYCODE_BACKSLASH = 73, KEYCODE_SEMICOLON = 74, KEYCODE_APOSTROPHE = 75, KEYCODE_SLASH = 76;
    public static final int KEYCODE_BUTTON_A = 96, KEYCODE_BUTTON_B = 97, KEYCODE_BUTTON_C = 98, KEYCODE_BUTTON_X = 99;
    public static final int KEYCODE_BUTTON_Y = 100, KEYCODE_BUTTON_Z = 101, KEYCODE_BUTTON_L1 = 102, KEYCODE_BUTTON_R1 = 103;
    public static final int KEYCODE_BUTTON_L2 = 104, KEYCODE_BUTTON_R2 = 105, KEYCODE_BUTTON_THUMBL = 106;
    public static final int KEYCODE_BUTTON_THUMBR = 107, KEYCODE_BUTTON_START = 108, KEYCODE_BUTTON_SELECT = 109;
    public static final int KEYCODE_BUTTON_MODE = 110, KEYCODE_ESCAPE = 111, KEYCODE_FORWARD_DEL = 112;
    public static final int KEYCODE_CTRL_LEFT = 113, KEYCODE_CTRL_RIGHT = 114;
    public static final int KEYCODE_F1 = 131, KEYCODE_F2 = 132, KEYCODE_F3 = 133, KEYCODE_F4 = 134, KEYCODE_F5 = 135;
    public static final int KEYCODE_F6 = 136, KEYCODE_F7 = 137, KEYCODE_F8 = 138, KEYCODE_F9 = 139, KEYCODE_F10 = 140;
    public static final int KEYCODE_F11 = 141, KEYCODE_F12 = 142;

    private final int action;
    private final int keyCode;
    private final int repeatCount;

    public KeyEvent(int action, int code) { this(action, code, 0); }

    /** Extensão do PC: com a contagem de repetição (tecla segurada). */
    public KeyEvent(int action, int code, int repeatCount) {
        this.action = action;
        this.keyCode = code;
        this.repeatCount = repeatCount;
    }

    public final int getAction() { return action; }

    public final int getKeyCode() { return keyCode; }

    public final int getRepeatCount() { return repeatCount; }

    @Override public String toString() { return "KeyEvent{action=" + action + ", keyCode=" + keyCode + ", repeat=" + repeatCount + "}"; }
}
