package android.graphics;

import java.awt.Font;

/**
 * Fontes do PC. O Android usa Roboto; aqui sai a fonte sem serifa do sistema (o Java
 * completa os símbolos que faltarem, como as setas do D-pad, com outras fontes).
 */
public class Typeface {
    public static final int NORMAL = 0, BOLD = 1, ITALIC = 2, BOLD_ITALIC = 3;

    public static final Typeface DEFAULT = new Typeface(Font.SANS_SERIF, NORMAL, false);
    public static final Typeface DEFAULT_BOLD = new Typeface(Font.SANS_SERIF, BOLD, false);
    public static final Typeface SANS_SERIF = DEFAULT;
    public static final Typeface SERIF = new Typeface(Font.SERIF, NORMAL, false);
    public static final Typeface MONOSPACE = new Typeface(Font.MONOSPACED, NORMAL, false);

    final String family;
    final int style;
    /** "sans-serif-condensed": desenhada mais estreita. */
    final boolean condensed;
    private final Font base;

    private Typeface(String family, int style, boolean condensed) {
        this.family = family;
        this.style = style;
        this.condensed = condensed;
        int awt = ((style & BOLD) != 0 ? Font.BOLD : 0) | ((style & ITALIC) != 0 ? Font.ITALIC : 0);
        base = new Font(family, awt, 1);
    }

    public static Typeface create(String familyName, int style) {
        String name = familyName == null ? "" : familyName.toLowerCase(java.util.Locale.ROOT);
        String family = name.contains("mono") ? Font.MONOSPACED : name.equals("serif") ? Font.SERIF : Font.SANS_SERIF;
        return new Typeface(family, style, name.contains("condensed"));
    }

    public static Typeface create(Typeface family, int style) {
        Typeface f = family == null ? DEFAULT : family;
        return new Typeface(f.family, style, f.condensed);
    }

    public static Typeface defaultFromStyle(int style) { return create(DEFAULT, style); }

    public int getStyle() { return style; }

    public final boolean isBold() { return (style & BOLD) != 0; }

    public final boolean isItalic() { return (style & ITALIC) != 0; }

    Font font(float size) { return base.deriveFont(size); }
}
