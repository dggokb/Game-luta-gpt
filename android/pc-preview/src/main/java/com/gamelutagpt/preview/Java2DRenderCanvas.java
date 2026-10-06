package com.gamelutagpt.preview;

import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/** Implementação do {@link RenderCanvas} para PC, com Java2D. */
public final class Java2DRenderCanvas implements RenderCanvas {
    private static final class State {
        final AffineTransform transform;
        final Shape clip;

        State(AffineTransform transform, Shape clip) {
            this.transform = transform;
            this.clip = clip;
        }
    }

    private static final class Layer {
        final Graphics2D parent;
        final BufferedImage image;

        Layer(Graphics2D parent, BufferedImage image) {
            this.parent = parent;
            this.image = image;
        }
    }

    private final Deque<State> stack = new ArrayDeque<>();
    private final Deque<Layer> layers = new ArrayDeque<>();
    private final int pixelWidth;
    private final int pixelHeight;
    private Graphics2D g;

    /**
     * @param g destino, já escalado para coordenadas virtuais 1280x720
     * @param pixelWidth largura real em pixels do destino
     * @param pixelHeight altura real em pixels do destino
     */
    public Java2DRenderCanvas(Graphics2D g, int pixelWidth, int pixelHeight) {
        this.g = g;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        applyHints(g);
    }

    private static void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    @Override
    public void save() {
        stack.push(new State(g.getTransform(), g.getClip()));
    }

    @Override
    public void restore() {
        State state = stack.pop();
        g.setTransform(state.transform);
        g.setClip(state.clip);
    }

    @Override
    public void translate(float dx, float dy) {
        g.translate(dx, dy);
    }

    @Override
    public void scale(float sx, float sy, float px, float py) {
        g.translate(px, py);
        g.scale(sx, sy);
        g.translate(-px, -py);
    }

    @Override
    public void rotate(float degrees, float px, float py) {
        g.rotate(Math.toRadians(degrees), px, py);
    }

    @Override
    public void clipPolygon(float[] xy) {
        g.clip(path(xy));
    }

    @Override
    public void fillRect(float left, float top, float right, float bottom, int argb) {
        g.setColor(new Color(argb, true));
        g.fill(new Rectangle2D.Float(left, top, right - left, bottom - top));
    }

    @Override
    public void fillPolygon(float[] xy, int argb) {
        g.setColor(new Color(argb, true));
        g.fill(path(xy));
    }

    @Override
    public void strokePolygon(float[] xy, float width, int argb) {
        g.setColor(new Color(argb, true));
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.draw(path(xy));
    }

    @Override
    public void fillCircle(float cx, float cy, float radius, int argb) {
        g.setColor(new Color(argb, true));
        g.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f));
    }

    @Override
    public void strokeCircle(float cx, float cy, float radius, float width, int argb) {
        g.setColor(new Color(argb, true));
        g.setStroke(new BasicStroke(width));
        g.draw(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f));
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {
        g.setColor(new Color(argb, true));
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }

    @Override
    public void drawImage(RenderImage image, float left, float top, float right, float bottom, int alpha) {
        BufferedImage source = ((Java2DImage)image).image;
        Composite previous = g.getComposite();
        if (alpha < 255) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
        }
        AffineTransform saved = g.getTransform();
        g.translate(left, top);
        g.scale((right - left) / source.getWidth(), (bottom - top) / source.getHeight());
        g.drawImage(source, 0, 0, null);
        g.setTransform(saved);
        g.setComposite(previous);
    }

    @Override
    public void drawImageRegion(
        RenderImage image,
        float srcLeft,
        float srcTop,
        float srcRight,
        float srcBottom,
        float left,
        float top,
        float right,
        float bottom,
        int alpha
    ) {
        BufferedImage source = ((Java2DImage)image).image;
        int sx0 = Math.max(0, Math.round(srcLeft));
        int sy0 = Math.max(0, Math.round(srcTop));
        int sx1 = Math.min(source.getWidth(), Math.round(srcRight));
        int sy1 = Math.min(source.getHeight(), Math.round(srcBottom));
        if (sx1 <= sx0 || sy1 <= sy0) return;
        Composite previous = g.getComposite();
        if (alpha < 255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
        AffineTransform saved = g.getTransform();
        g.translate(left, top);
        g.scale((right - left) / (sx1 - sx0), (bottom - top) / (sy1 - sy0));
        g.drawImage(source, 0, 0, sx1 - sx0, sy1 - sy0, sx0, sy0, sx1, sy1, null);
        g.setTransform(saved);
        g.setComposite(previous);
    }

    @Override
    public void fillRadialGlow(float cx, float cy, float radius, int argbCenter) {
        if (radius <= 0f) return;
        g.setPaint(new RadialGradientPaint(cx, cy, radius, new float[] {0f, 1f},
            new Color[] {new Color(argbCenter, true), new Color(argbCenter & 0x00FFFFFF, true)}));
        g.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f));
    }

    @Override
    public void fillVerticalGradient(float left, float top, float right, float bottom, int argbTop, int argbBottom) {
        if (bottom <= top) return;
        g.setPaint(new GradientPaint(0f, top, new Color(argbTop, true), 0f, bottom, new Color(argbBottom, true)));
        g.fill(new Rectangle2D.Float(left, top, right - left, bottom - top));
    }

    @Override
    public void fillOval(float left, float top, float right, float bottom, int argb) {
        g.setColor(new Color(argb, true));
        g.fill(new Ellipse2D.Float(left, top, right - left, bottom - top));
    }

    @Override
    public void drawText(
        String text,
        float x,
        float y,
        float size,
        int fillArgb,
        int strokeArgb,
        float strokeWidth,
        boolean italic
    ) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD | (italic ? Font.ITALIC : 0), 1).deriveFont(size);
        GlyphVector glyphs = font.createGlyphVector(g.getFontRenderContext(), text);
        float width = (float)glyphs.getLogicalBounds().getWidth();
        Shape outline = glyphs.getOutline(x - width / 2f, y);
        if (strokeWidth > 0f) {
            g.setColor(new Color(strokeArgb, true));
            g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(outline);
        }
        g.setColor(new Color(fillArgb, true));
        g.fill(outline);
    }

    @Override
    public void beginInvert() {
        BufferedImage layer = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D layerGraphics = layer.createGraphics();
        applyHints(layerGraphics);
        layerGraphics.setTransform(g.getTransform());
        layerGraphics.setClip(g.getClip());
        layers.push(new Layer(g, layer));
        g = layerGraphics;
    }

    @Override
    public void endInvert() {
        Layer layer = layers.pop();
        g.dispose();
        g = layer.parent;

        int[] pixels = new int[pixelWidth * pixelHeight];
        layer.image.getRGB(0, 0, pixelWidth, pixelHeight, pixels, 0, pixelWidth);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            if ((p >>> 24) != 0) pixels[i] = (p & 0xFF000000) | (~p & 0x00FFFFFF);
        }
        layer.image.setRGB(0, 0, pixelWidth, pixelHeight, pixels, 0, pixelWidth);

        AffineTransform saved = g.getTransform();
        Shape savedClip = g.getClip();
        g.setTransform(new AffineTransform());
        g.setClip(null);
        g.drawImage(layer.image, 0, 0, null);
        g.setTransform(saved);
        g.setClip(savedClip);
    }

    private static Path2D.Float path(float[] xy) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i + 1 < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        path.closePath();
        return path;
    }
}
