package android.util;

/** PC: o tamanho da "tela do aparelho" que o jogo considera ao decidir a definição da arte. */
public class DisplayMetrics {
    public int widthPixels;
    public int heightPixels;
    public float density = 1f;
    public int densityDpi = 160;

    public DisplayMetrics() {}

    /**
     * -Dgameluta.tela=1280x720 simula a tela de um celular (para conferir a arte reduzida).
     * Sem a propriedade: a tela do PC; sem tela (testes, captura), 4K, que decodifica tudo inteiro.
     */
    public static DisplayMetrics doPc() {
        DisplayMetrics m = new DisplayMetrics();
        String tela = System.getProperty("gameluta.tela");
        if (tela != null && tela.matches("\\d+x\\d+")) {
            String[] p = tela.split("x");
            m.widthPixels = Integer.parseInt(p[0]);
            m.heightPixels = Integer.parseInt(p[1]);
        } else if (!java.awt.GraphicsEnvironment.isHeadless()) {
            java.awt.Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            m.widthPixels = d.width;
            m.heightPixels = d.height;
        } else {
            m.widthPixels = 3840;
            m.heightPixels = 2160;
        }
        return m;
    }
}
