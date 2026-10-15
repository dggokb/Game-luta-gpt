package android.graphics;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/** Estilo do desenho, como o Paint do Android. */
public class Paint {
    public static final int ANTI_ALIAS_FLAG = 0x01;
    public static final int FILTER_BITMAP_FLAG = 0x02;
    public static final int DITHER_FLAG = 0x04;
    public static final int FAKE_BOLD_TEXT_FLAG = 0x20;

    public enum Style { FILL, STROKE, FILL_AND_STROKE }

    public enum Cap { BUTT, ROUND, SQUARE }

    public enum Join { MITER, ROUND, BEVEL }

    public enum Align { LEFT, CENTER, RIGHT }

    public static class FontMetrics {
        public float top, ascent, descent, bottom, leading;
    }

    static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    int flags;
    int color = Color.BLACK;
    Style style = Style.FILL;
    float strokeWidth = 0f;
    float strokeMiter = 4f;
    Cap cap = Cap.BUTT;
    Join join = Join.MITER;
    Shader shader;
    ColorFilter colorFilter;
    Xfermode xfermode;
    PathEffect pathEffect;
    float textSize = 12f;
    Typeface typeface;
    Align textAlign = Align.LEFT;
    float textScaleX = 1f;
    float textSkewX = 0f;
    float letterSpacing = 0f;

    public Paint() { this(0); }

    public Paint(int flags) { this.flags = flags; }

    public Paint(Paint paint) { set(paint); }

    public void set(Paint src) {
        flags = src.flags; color = src.color; style = src.style; strokeWidth = src.strokeWidth;
        strokeMiter = src.strokeMiter; cap = src.cap; join = src.join; shader = src.shader;
        colorFilter = src.colorFilter; xfermode = src.xfermode; pathEffect = src.pathEffect; textSize = src.textSize; typeface = src.typeface;
        textAlign = src.textAlign; textScaleX = src.textScaleX; textSkewX = src.textSkewX; letterSpacing = src.letterSpacing;
    }

    public void reset() { set(new Paint()); }

    public int getFlags() { return flags; }

    public void setFlags(int flags) { this.flags = flags; }

    public final boolean isAntiAlias() { return (flags & ANTI_ALIAS_FLAG) != 0; }

    public void setAntiAlias(boolean aa) { flags = aa ? flags | ANTI_ALIAS_FLAG : flags & ~ANTI_ALIAS_FLAG; }

    public final boolean isFilterBitmap() { return (flags & FILTER_BITMAP_FLAG) != 0; }

    public void setFilterBitmap(boolean filter) { flags = filter ? flags | FILTER_BITMAP_FLAG : flags & ~FILTER_BITMAP_FLAG; }

    public void setDither(boolean dither) { flags = dither ? flags | DITHER_FLAG : flags & ~DITHER_FLAG; }

    public final boolean isFakeBoldText() { return (flags & FAKE_BOLD_TEXT_FLAG) != 0; }

    public void setFakeBoldText(boolean fakeBold) { flags = fakeBold ? flags | FAKE_BOLD_TEXT_FLAG : flags & ~FAKE_BOLD_TEXT_FLAG; }

    public int getColor() { return color; }

    public void setColor(int color) { this.color = color; }

    public void setARGB(int a, int r, int g, int b) { color = Color.argb(a, r, g, b); }

    public int getAlpha() { return color >>> 24; }

    public void setAlpha(int a) { color = (color & 0x00FFFFFF) | ((Math.max(0, Math.min(255, a))) << 24); }

    public Style getStyle() { return style; }

    public void setStyle(Style style) { this.style = style; }

    public float getStrokeWidth() { return strokeWidth; }

    public void setStrokeWidth(float width) { strokeWidth = width; }

    public float getStrokeMiter() { return strokeMiter; }

    public void setStrokeMiter(float miter) { strokeMiter = miter; }

    public Cap getStrokeCap() { return cap; }

    public void setStrokeCap(Cap cap) { this.cap = cap; }

    public Join getStrokeJoin() { return join; }

    public void setStrokeJoin(Join join) { this.join = join; }

    public Shader getShader() { return shader; }

    public Shader setShader(Shader shader) { this.shader = shader; return shader; }

    public ColorFilter getColorFilter() { return colorFilter; }

    public ColorFilter setColorFilter(ColorFilter filter) { colorFilter = filter; return filter; }

    public Xfermode getXfermode() { return xfermode; }

    public Xfermode setXfermode(Xfermode xfermode) { this.xfermode = xfermode; return xfermode; }

    public PathEffect getPathEffect() { return pathEffect; }

    public PathEffect setPathEffect(PathEffect effect) { pathEffect = effect; return effect; }

    public float getTextSize() { return textSize; }

    public void setTextSize(float textSize) { this.textSize = textSize; }

    public Typeface getTypeface() { return typeface; }

    public Typeface setTypeface(Typeface typeface) { this.typeface = typeface; return typeface; }

    public Align getTextAlign() { return textAlign; }

    public void setTextAlign(Align align) { textAlign = align; }

    public float getTextScaleX() { return textScaleX; }

    public void setTextScaleX(float scaleX) { textScaleX = scaleX; }

    public float getTextSkewX() { return textSkewX; }

    public void setTextSkewX(float skewX) { textSkewX = skewX; }

    public float getLetterSpacing() { return letterSpacing; }

    public void setLetterSpacing(float letterSpacing) { this.letterSpacing = letterSpacing; }

    public void setShadowLayer(float radius, float dx, float dy, int shadowColor) {}

    public void clearShadowLayer() {}

    public float measureText(String text) {
        if (text == null || text.isEmpty()) return 0f;
        return glyphs(text).advance * horizontalScale();
    }

    public float measureText(String text, int start, int end) { return measureText(text.substring(start, end)); }

    public float measureText(char[] text, int index, int count) { return measureText(new String(text, index, count)); }

    /**
     * Quantos caracteres (do começo, ou do fim com {@code measureForwards} falso) cabem em
     * {@code maxWidth}; a largura medida vai em {@code measuredWidth[0]}.
     */
    public int breakText(String text, boolean measureForwards, float maxWidth, float[] measuredWidth) {
        int n = text.length(), fit = 0;
        float width = 0f;
        for (int count = 1; count <= n; count++) {
            String part = measureForwards ? text.substring(0, count) : text.substring(n - count);
            float w = measureText(part);
            if (w > maxWidth) break;
            fit = count;
            width = w;
        }
        if (measuredWidth != null && measuredWidth.length > 0) measuredWidth[0] = width;
        return fit;
    }

    public void getTextBounds(String text, int start, int end, Rect bounds) {
        String s = text.substring(start, end);
        if (s.isEmpty()) { bounds.set(0, 0, 0, 0); return; }
        Rectangle2D b = outline(s).getBounds2D();
        bounds.set((int) Math.floor(b.getMinX()), (int) Math.floor(b.getMinY()), (int) Math.ceil(b.getMaxX()), (int) Math.ceil(b.getMaxY()));
    }

    /** Negativo, como no Android. */
    public float ascent() { return -lineMetrics().getAscent(); }

    public float descent() { return lineMetrics().getDescent(); }

    public float getFontSpacing() { LineMetrics m = lineMetrics(); return m.getAscent() + m.getDescent() + m.getLeading(); }

    public FontMetrics getFontMetrics() {
        FontMetrics fm = new FontMetrics();
        getFontMetrics(fm);
        return fm;
    }

    public float getFontMetrics(FontMetrics fm) {
        LineMetrics m = lineMetrics();
        if (fm != null) {
            fm.ascent = -m.getAscent();
            fm.top = fm.ascent * 1.1f;
            fm.descent = m.getDescent();
            fm.bottom = fm.descent * 1.1f;
            fm.leading = m.getLeading();
        }
        return m.getAscent() + m.getDescent() + m.getLeading();
    }

    // ------------------------------------------------------------------ PC

    Font font() {
        Typeface t = typeface == null ? Typeface.DEFAULT : typeface;
        return t.font(textSize);
    }

    /** Fontes "condensed" saem um pouco mais estreitas. */
    float horizontalScale() {
        Typeface t = typeface == null ? Typeface.DEFAULT : typeface;
        return textScaleX * (t.condensed ? 0.86f : 1f);
    }

    private LineMetrics lineMetrics() { return font().getLineMetrics("Ag", FRC); }

    /** Glifos já montados, compartilhados por todos os Paint (o HUD redesenha o mesmo texto todo quadro). */
    static final class Glyphs {
        final java.awt.Shape outline;
        final float advance;

        Glyphs(GlyphVector gv) {
            outline = gv.getOutline();
            advance = (float) gv.getLogicalBounds().getWidth();
        }
    }

    private static final java.util.Map<String, Glyphs> GLYPHS =
        new java.util.LinkedHashMap<String, Glyphs>(1024, 0.75f, true) {
            @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Glyphs> eldest) {
                return size() > 1024;
            }
        };

    Glyphs glyphs(String text) {
        Font f = font();
        String key = f.getFamily() + '|' + f.getStyle() + '|' + f.getSize2D() + '|' + letterSpacing + '|' + text;
        synchronized (GLYPHS) {
            Glyphs cached = GLYPHS.get(key);
            if (cached != null) return cached;
        }
        GlyphVector gv = fallbackFont(f, text).createGlyphVector(FRC, text);
        if (letterSpacing != 0f) {
            float extra = letterSpacing * textSize;
            for (int i = 0; i < gv.getNumGlyphs(); i++) {
                java.awt.geom.Point2D p = gv.getGlyphPosition(i);
                gv.setGlyphPosition(i, new java.awt.geom.Point2D.Double(p.getX() + extra * i, p.getY()));
            }
        }
        Glyphs glyphs = new Glyphs(gv);
        synchronized (GLYPHS) {
            GLYPHS.put(key, glyphs);
        }
        return glyphs;
    }

    /** Contorno do texto com a origem na linha de base, já com alinhamento, escala e inclinação. */
    java.awt.Shape outline(String text) { return outline(text, 0f, 0f); }

    java.awt.Shape outline(String text, float x, float y) {
        Glyphs gv = glyphs(text);
        float scale = horizontalScale();
        float advance = gv.advance * scale;
        float dx = textAlign == Align.CENTER ? -advance / 2f : textAlign == Align.RIGHT ? -advance : 0f;
        AffineTransform t = new AffineTransform();
        t.translate(x + dx, y);
        if (textSkewX != 0f) t.shear(textSkewX, 0);
        if (scale != 1f) t.scale(scale, 1);
        return t.createTransformedShape(gv.outline);
    }

    /** Se a fonte não tem algum caractere (ex.: setas), usa a "Dialog", que junta várias fontes. */
    private static Font fallbackFont(Font f, String text) {
        if (f.canDisplayUpTo(text) == -1) return f;
        Font dialog = new Font(Font.DIALOG, f.getStyle(), 1).deriveFont(f.getSize2D());
        return dialog.canDisplayUpTo(text) == -1 ? dialog : f;
    }
}
