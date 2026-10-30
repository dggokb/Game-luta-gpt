package android.graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * O Canvas do Android desenhado com Java2D. É a peça que deixa o jogo inteiro (luta, HUD,
 * seleção, efeitos) rodar no PC sem mudar uma linha de desenho: cada chamada vira a mesma
 * operação no {@link Graphics2D}.
 *
 * <p>Diferenças tratadas aqui: ângulos de arco (o Android gira no sentido horário),
 * camadas ({@link #saveLayer}) feitas com imagens fora da tela, filtros de cor aplicados
 * pixel a pixel (com cache) e bitmaps em perspectiva ({@link Matrix#setPolyToPoly}).
 */
public class Canvas {
    private static final class Layer {
        final Graphics2D parent;
        final BufferedImage parentImage;
        final BufferedImage image;
        final int x, y, w, h;
        final int alpha;
        final ColorFilter filter;

        Layer(Graphics2D parent, BufferedImage parentImage, BufferedImage image, int x, int y, int w, int h, int alpha, ColorFilter filter) {
            this.parent = parent;
            this.parentImage = parentImage;
            this.image = image;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.alpha = alpha;
            this.filter = filter;
        }
    }

    /** Imagens de camada já alocadas (o reflexo do chão abre três camadas por quadro). */
    private final java.util.List<BufferedImage> freeLayers = new java.util.ArrayList<>();

    private BufferedImage takeLayer(int w, int h) {
        BufferedImage best = null;
        for (BufferedImage img : freeLayers) {
            if (img.getWidth() >= w && img.getHeight() >= h && (best == null || img.getWidth() * img.getHeight() < best.getWidth() * best.getHeight())) best = img;
        }
        if (best != null) {
            freeLayers.remove(best);
            Graphics2D c = best.createGraphics();
            c.setComposite(AlphaComposite.Clear);
            c.fillRect(0, 0, w, h);
            c.dispose();
            return best;
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    private void giveLayer(BufferedImage img) {
        if (freeLayers.size() >= 6) freeLayers.remove(0);
        freeLayers.add(img);
    }

    private static final class Entry {
        final AffineTransform transform;
        final Shape clip;
        final Layer layer;

        Entry(AffineTransform transform, Shape clip, Layer layer) {
            this.transform = transform;
            this.clip = clip;
            this.layer = layer;
        }
    }

    private Graphics2D g;
    /** Imagem por trás de {@link #g}, quando conhecida: permite misturar pixels direto ({@link Blender}). */
    private BufferedImage image;
    private int width, height;
    private final Deque<Entry> stack = new ArrayDeque<>();
    private Bitmap target;

    public Canvas() {}

    public Canvas(Bitmap bitmap) {
        setBitmap(bitmap);
    }

    public void setBitmap(Bitmap bitmap) {
        if (g != null && target != null) g.dispose();
        target = bitmap;
        if (bitmap != null) begin(bitmap.image().createGraphics(), bitmap.image(), bitmap.getWidth(), bitmap.getHeight());
    }

    /** Extensão do PC: desenha em {@code target} (o Graphics2D é dele). */
    public void begin(Graphics2D graphics, BufferedImage target, int width, int height) {
        begin(graphics, width, height);
        image = target != null && Blender.supports(target) ? target : null;
    }

    /** Extensão do PC: passa a desenhar em {@code graphics}, que tem {@code width}x{@code height} pixels. */
    public void begin(Graphics2D graphics, int width, int height) {
        stack.clear();
        g = graphics;
        image = null;
        this.width = width;
        this.height = height;
        hints(g);
    }

    /** Extensão do PC: fecha camadas esquecidas abertas e devolve o destino. */
    public Graphics2D end() {
        while (!stack.isEmpty()) restore();
        return g;
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public boolean isHardwareAccelerated() { return true; }

    // ------------------------------------------------------------------ pilha

    public int save() {
        stack.push(new Entry(g.getTransform(), g.getClip(), null));
        return stack.size();
    }

    public int getSaveCount() { return stack.size() + 1; }

    public void restore() {
        if (stack.isEmpty()) throw new IllegalStateException("Underflow in restore");
        Entry e = stack.pop();
        if (e.layer != null) {
            Graphics2D layerGraphics = g;
            g = e.layer.parent;
            image = e.layer.parentImage;
            layerGraphics.dispose();
            g.setTransform(e.transform);
            g.setClip(e.clip);
            compositeLayer(e.layer);
        }
        g.setTransform(e.transform);
        g.setClip(e.clip);
    }

    public void restoreToCount(int saveCount) {
        if (saveCount < 1) throw new IllegalArgumentException("saveCount");
        while (stack.size() >= saveCount) restore();
    }

    public int saveLayer(RectF bounds, Paint paint) {
        return bounds == null ? saveLayer(0, 0, 0, 0, paint, true) : saveLayer(bounds.left, bounds.top, bounds.right, bounds.bottom, paint, false);
    }

    public int saveLayer(RectF bounds, Paint paint, int saveFlags) { return saveLayer(bounds, paint); }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint) {
        return saveLayer(left, top, right, bottom, paint, false);
    }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint, int saveFlags) {
        return saveLayer(left, top, right, bottom, paint, false);
    }

    public int saveLayerAlpha(RectF bounds, int alpha, int saveFlags) { return saveLayerAlpha(bounds, alpha); }

    public int saveLayerAlpha(RectF bounds, int alpha) {
        Paint p = new Paint();
        p.setAlpha(alpha);
        return saveLayer(bounds, p);
    }

    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha) {
        Paint p = new Paint();
        p.setAlpha(alpha);
        return saveLayer(left, top, right, bottom, p, false);
    }

    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha, int saveFlags) {
        return saveLayerAlpha(left, top, right, bottom, alpha);
    }

    private int saveLayer(float left, float top, float right, float bottom, Paint paint, boolean whole) {
        if (areas != null) contar(new Rectangle2D.Float(left, top, right - left, bottom - top), "camada");
        AffineTransform t = g.getTransform();
        Shape clip = g.getClip();
        Rectangle2D device = new Rectangle2D.Double(0, 0, width, height);
        if (!whole) {
            Rectangle2D b = t.createTransformedShape(new Rectangle2D.Float(Math.min(left, right), Math.min(top, bottom),
                Math.abs(right - left), Math.abs(bottom - top))).getBounds2D();
            device = device.createIntersection(b);
        }
        if (clip != null) device = device.createIntersection(t.createTransformedShape(clip).getBounds2D());
        int x = (int) Math.floor(device.getMinX()), y = (int) Math.floor(device.getMinY());
        int w = (int) Math.ceil(device.getMaxX()) - x, h = (int) Math.ceil(device.getMaxY()) - y;
        Layer layer;
        Graphics2D next;
        if (w <= 0 || h <= 0) {
            // Camada vazia: os desenhos vão para uma imagem de 1 pixel e somem.
            BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
            next = img.createGraphics();
            next.setClip(0, 0, 0, 0);
            layer = new Layer(g, image, img, 0, 0, 1, 1, 0, null);
        } else {
            BufferedImage img = takeLayer(w, h);
            next = img.createGraphics();
            next.setClip(0, 0, w, h);
            hints(next);
            AffineTransform lt = AffineTransform.getTranslateInstance(-x, -y);
            lt.concatenate(t);
            next.setTransform(lt);
            if (clip != null) next.clip(clip);
            layer = new Layer(g, image, img, x, y, w, h, paint == null ? 255 : paint.getAlpha(), paint == null ? null : paint.colorFilter);
        }
        stack.push(new Entry(t, clip, layer));
        g = next;
        image = layer.image;
        return stack.size();
    }

    private void compositeLayer(Layer layer) {
        BufferedImage img = layer.image;
        if (layer.alpha > 0) {
            if (layer.filter != null) filterInPlace(img, layer.w, layer.h, layer.filter);
            AffineTransform before = g.getTransform();
            g.setTransform(new AffineTransform());
            boolean done = blendDirect(img, layer.x, layer.y, layer.w, layer.h, layer.alpha);
            g.setTransform(before);
            if (done) {
                if (layer.w > 1) giveLayer(img);
                return;
            }
            AffineTransform saved = g.getTransform();
            Composite composite = g.getComposite();
            g.setTransform(new AffineTransform());
            if (layer.alpha < 255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.alpha / 255f));
            g.drawImage(img, layer.x, layer.y, layer.x + layer.w, layer.y + layer.h, 0, 0, layer.w, layer.h, null);
            g.setComposite(composite);
            g.setTransform(saved);
        }
        if (layer.w > 1) giveLayer(img);
    }

    // ------------------------------------------------------------------ transformações e recorte

    public void translate(float dx, float dy) { g.translate(dx, dy); }

    public void scale(float sx, float sy) { g.scale(sx, sy); }

    public final void scale(float sx, float sy, float px, float py) {
        g.translate(px, py);
        g.scale(sx, sy);
        g.translate(-px, -py);
    }

    public void rotate(float degrees) { g.rotate(Math.toRadians(degrees)); }

    public final void rotate(float degrees, float px, float py) { g.rotate(Math.toRadians(degrees), px, py); }

    public void skew(float sx, float sy) { g.shear(sx, sy); }

    public void concat(Matrix matrix) { if (matrix != null) g.transform(matrix.toAffine()); }

    public boolean clipRect(float left, float top, float right, float bottom) {
        g.clip(new Rectangle2D.Float(Math.min(left, right), Math.min(top, bottom), Math.abs(right - left), Math.abs(bottom - top)));
        return !isClipEmpty();
    }

    public boolean clipRect(int left, int top, int right, int bottom) {
        return clipRect((float) left, (float) top, (float) right, (float) bottom);
    }

    public boolean clipRect(RectF rect) { return clipRect(rect.left, rect.top, rect.right, rect.bottom); }

    public boolean clipRect(Rect rect) { return clipRect(rect.left, rect.top, rect.right, rect.bottom); }

    public boolean clipRect(float left, float top, float right, float bottom, Region.Op op) {
        return clipShape(new Rectangle2D.Float(Math.min(left, right), Math.min(top, bottom), Math.abs(right - left), Math.abs(bottom - top)), op);
    }

    public boolean clipRect(RectF rect, Region.Op op) { return clipRect(rect.left, rect.top, rect.right, rect.bottom, op); }

    public boolean clipRect(Rect rect, Region.Op op) { return clipRect(rect.left, rect.top, rect.right, rect.bottom, op); }

    public boolean clipPath(Path path, Region.Op op) { return clipShape(path.shape, op); }

    /** Recorte com operação: INTERSECT como sempre; DIFFERENCE tira a forma do recorte de agora. */
    private boolean clipShape(Shape s, Region.Op op) {
        if (op == Region.Op.INTERSECT) {
            g.clip(s);
        } else if (op == Region.Op.DIFFERENCE) {
            Shape atual = g.getClip();
            java.awt.geom.Area a = new java.awt.geom.Area(atual != null ? atual : new Rectangle2D.Double(-1e6, -1e6, 2e6, 2e6));
            a.subtract(new java.awt.geom.Area(s));
            g.setClip(a);
        } else if (op == Region.Op.REPLACE) {
            g.setClip(s);
        }
        return !isClipEmpty();
    }

    /** A transformação de agora (afim). */
    public void getMatrix(Matrix ctm) { ctm.setAffine(g.getTransform()); }

    public Matrix getMatrix() {
        Matrix m = new Matrix();
        getMatrix(m);
        return m;
    }

    /** Troca a transformação (null = nenhuma). */
    public void setMatrix(Matrix matrix) { g.setTransform(matrix == null ? new AffineTransform() : matrix.toAffine()); }

    public boolean clipPath(Path path) {
        g.clip(path.shape);
        return !isClipEmpty();
    }

    public boolean getClipBounds(Rect bounds) {
        Shape clip = g.getClip();
        Rectangle2D b;
        if (clip == null) {
            try {
                b = g.getTransform().createInverse().createTransformedShape(new Rectangle2D.Float(0, 0, width, height)).getBounds2D();
            } catch (NoninvertibleTransformException e) {
                b = new Rectangle2D.Float();
            }
        } else {
            b = clip.getBounds2D();
        }
        bounds.set((int) Math.floor(b.getMinX()), (int) Math.floor(b.getMinY()), (int) Math.ceil(b.getMaxX()), (int) Math.ceil(b.getMaxY()));
        return !b.isEmpty();
    }

    private boolean isClipEmpty() {
        Shape clip = g.getClip();
        return clip != null && clip.getBounds2D().isEmpty();
    }


    // ------------------------------------------------------------------ medição de pintura (PC)

    /**
     * Ferramenta: com um mapa aqui, cada desenho soma a área de tela que cobre (a caixa no
     * dispositivo, cortada pela tela e pelo recorte), por quem chamou. Mede quantas vezes o
     * quadro pinta a tela (o que pesa na GPU de celular).
     */
    public static volatile java.util.Map<String, double[]> areas;

    private void contar(Shape forma, String tipo) {
        java.util.Map<String, double[]> m = areas;
        if (m == null || forma == null || g == null) return;
        Rectangle2D b = g.getTransform().createTransformedShape(forma).getBounds2D();
        Shape clip = g.getClip();
        Rectangle2D c = clip != null ? g.getTransform().createTransformedShape(clip).getBounds2D() : new Rectangle2D.Float(0, 0, width, height);
        Rectangle2D.intersect(b, c, b);
        Rectangle2D.intersect(b, new Rectangle2D.Float(0, 0, width, height), b);
        if (b.isEmpty()) return;
        String quem = "?";
        for (StackTraceElement e : new Throwable().getStackTrace()) {
            String k = e.getClassName();
            if (k.startsWith("android.") || k.startsWith("java.")) continue;
            if (k.endsWith("AndroidRenderCanvas")) continue;
            quem = k.substring(k.lastIndexOf('.') + 1) + "." + e.getMethodName() + ":" + e.getLineNumber();
            break;
        }
        double[] v = m.computeIfAbsent(tipo + " " + quem, x -> new double[2]);
        v[0] += b.getWidth() * b.getHeight();
        v[1]++;
    }

    // ------------------------------------------------------------------ formas

    public void drawColor(int color) { fillDevice(color, AlphaComposite.SrcOver); }

    public void drawColor(int color, PorterDuff.Mode mode) {
        fillDevice(color, mode == PorterDuff.Mode.CLEAR ? AlphaComposite.Clear : mode == PorterDuff.Mode.SRC ? AlphaComposite.Src : AlphaComposite.SrcOver);
    }

    public void drawARGB(int a, int r, int gr, int b) { drawColor(Color.argb(a, r, gr, b)); }

    public void drawRGB(int r, int gr, int b) { drawColor(Color.rgb(r, gr, b)); }

    public void drawPaint(Paint paint) {
        if (areas != null) {
            try { contar(g.getTransform().createInverse().createTransformedShape(new Rectangle2D.Float(0, 0, width, height)), "cor"); }
            catch (NoninvertibleTransformException ignored) { }
        }
        AffineTransform saved = g.getTransform();
        try {
            Shape all = saved.createInverse().createTransformedShape(new Rectangle2D.Float(0, 0, width, height));
            fill(all, paint);
        } catch (NoninvertibleTransformException ignored) {
            // Escala zero: não há o que pintar.
        }
    }

    private void fillDevice(int color, Composite composite) {
        if (areas != null) {
            AffineTransform t0 = g.getTransform();
            try { contar(t0.createInverse().createTransformedShape(new Rectangle2D.Float(0, 0, width, height)), "cor"); }
            catch (NoninvertibleTransformException ignored) { }
        }
        AffineTransform saved = g.getTransform();
        Composite c = g.getComposite();
        g.setTransform(new AffineTransform());
        g.setComposite(composite);
        g.setColor(new java.awt.Color(color, true));
        g.fillRect(0, 0, width, height);
        g.setComposite(c);
        g.setTransform(saved);
    }

    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        if (preencherRetangulo(Math.min(left, right), Math.min(top, bottom), Math.max(left, right), Math.max(top, bottom), paint)) return;
        draw(new Rectangle2D.Float(Math.min(left, right), Math.min(top, bottom), Math.abs(right - left), Math.abs(bottom - top)), paint);
    }

    public void drawRect(RectF r, Paint paint) { drawRect(r.left, r.top, r.right, r.bottom, paint); }

    public void drawRect(Rect r, Paint paint) { drawRect(r.left, r.top, r.right, r.bottom, paint); }

    public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, Paint paint) {
        if (right <= left || bottom <= top) return;
        draw(Path.roundRect(left, top, right, bottom, rx, ry), paint);
    }

    public void drawRoundRect(RectF r, float rx, float ry, Paint paint) { drawRoundRect(r.left, r.top, r.right, r.bottom, rx, ry, paint); }

    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        if (radius <= 0f) return;
        if (desenharBrilho(cx, cy, radius, paint)) return;
        draw(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f), paint);
    }

    public void drawOval(float left, float top, float right, float bottom, Paint paint) {
        draw(new Ellipse2D.Float(Math.min(left, right), Math.min(top, bottom), Math.abs(right - left), Math.abs(bottom - top)), paint);
    }

    public void drawOval(RectF r, Paint paint) { drawOval(r.left, r.top, r.right, r.bottom, paint); }

    public void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        // Android: graus no sentido horário a partir das 3 horas; Arc2D: anti-horário.
        int type = useCenter ? Arc2D.PIE : paint.style == Paint.Style.STROKE ? Arc2D.OPEN : Arc2D.CHORD;
        draw(new Arc2D.Float(left, top, right - left, bottom - top, -startAngle, -sweepAngle, type), paint);
    }

    public void drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        drawArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, useCenter, paint);
    }

    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        // Linha é sempre traço, qualquer que seja o estilo.
        stroke(new Line2D.Float(startX, startY, stopX, stopY), paint, paint.strokeWidth);
    }

    public void drawLines(float[] pts, int offset, int count, Paint paint) {
        for (int i = offset; i + 3 < offset + count; i += 4) drawLine(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], paint);
    }

    public void drawLines(float[] pts, Paint paint) { drawLines(pts, 0, pts.length, paint); }

    public void drawPoint(float x, float y, Paint paint) {
        float r = Math.max(paint.strokeWidth, 1f) / 2f;
        Shape s = paint.cap == Paint.Cap.ROUND ? new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r) : new Rectangle2D.Float(x - r, y - r, 2 * r, 2 * r);
        fill(s, paint);
    }

    public void drawPath(Path path, Paint paint) { draw(path.shape, paint); }

    // ------------------------------------------------------------------ texto

    public void drawText(String text, float x, float y, Paint paint) {
        if (text == null || text.isEmpty()) return;
        if (areas != null) contar(new Rectangle2D.Float(x - paint.measureText(text), y - paint.getTextSize(), paint.measureText(text) * 2f, paint.getTextSize() * 1.3f), "texto");
        if (paint.shader == null && TextImages.ENABLED) {
            drawCachedText(text, x, y, paint);
            return;
        }
        drawTextShape(text, x, y, paint);
    }

    private void drawTextShape(String text, float x, float y, Paint paint) {
        Shape outline = paint.outline(text, x, y);
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        // O texto do Android sai sempre suavizado.
        boolean fakeBold = paint.isFakeBoldText();
        float boldWidth = fakeBold ? paint.textSize / 26f : 0f;
        Paint.Style style = paint.style;
        if (style == Paint.Style.FILL || style == Paint.Style.FILL_AND_STROKE) {
            prepare(paint, true);
            g.fill(outline);
            if (fakeBold) strokeShape(outline, paint, boldWidth, true);
        }
        if (style == Paint.Style.STROKE || style == Paint.Style.FILL_AND_STROKE) {
            strokeShape(outline, paint, paint.strokeWidth + boldWidth, true);
        }
        finish(paint);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    /**
     * Texto já rasterizado: o HUD repete as mesmas palavras todo quadro, e o contorno dos
     * glifos é a parte mais cara do Java2D. A imagem vale para a mesma escala/rotação.
     */
    private void drawCachedText(String text, float x, float y, Paint paint) {
        AffineTransform t = g.getTransform();
        String key = TextImages.key(text, paint, t);
        TextImages.Entry e = TextImages.get(key);
        if (e == null) {
            AffineTransform linear = new AffineTransform(t.getScaleX(), t.getShearY(), t.getShearX(), t.getScaleY(), 0, 0);
            Shape outline = linear.createTransformedShape(paint.outline(text, 0f, 0f));
            double scale = Math.sqrt(Math.abs(t.getDeterminant()));
            float stroke = (paint.style == Paint.Style.FILL ? 0f : paint.strokeWidth) + (paint.isFakeBoldText() ? paint.textSize / 26f : 0f);
            int pad = (int) Math.ceil(stroke * scale / 2 + 2);
            java.awt.Rectangle b = outline.getBounds();
            int w = b.width + 2 * pad, h = b.height + 2 * pad;
            if (w <= 0 || h <= 0 || (long) w * h > 4_000_000L) {
                drawTextShape(text, x, y, paint);
                return;
            }
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D saved = g;
            int savedW = width, savedH = height;
            Graphics2D tg = img.createGraphics();
            hints(tg);
            AffineTransform at = AffineTransform.getTranslateInstance(pad - b.x, pad - b.y);
            at.concatenate(linear);
            tg.setTransform(at);
            g = tg;
            try {
                drawTextShape(text, 0f, 0f, paint);
            } finally {
                g = saved;
                width = savedW;
                height = savedH;
                tg.dispose();
            }
            e = new TextImages.Entry(img, b.x - pad, b.y - pad);
            TextImages.put(key, e);
        }
        double[] anchor = {x, y};
        t.transform(anchor, 0, anchor, 0, 1);
        g.setTransform(new AffineTransform());
        g.drawImage(e.image, (int) Math.round(anchor[0]) + e.dx, (int) Math.round(anchor[1]) + e.dy, null);
        g.setTransform(t);
    }

    /** Textos rasterizados (LRU). -Dgameluta.textoSemCache=true desliga. */
    static final class TextImages {
        static final boolean ENABLED = !Boolean.getBoolean("gameluta.textoSemCache");

        static final class Entry {
            final BufferedImage image;
            final int dx, dy;

            Entry(BufferedImage image, int dx, int dy) {
                this.image = image;
                this.dx = dx;
                this.dy = dy;
            }
        }

        private static final Map<String, Entry> CACHE = new LinkedHashMap<String, Entry>(512, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) { return size() > 600; }
        };

        static String key(String text, Paint p, AffineTransform t) {
            Typeface tf = p.typeface == null ? Typeface.DEFAULT : p.typeface;
            return new StringBuilder(text.length() + 96).append(text).append('\u0000')
                .append(tf.family).append(tf.style).append(tf.condensed).append('|')
                .append(p.textSize).append('|').append(p.textScaleX).append('|').append(p.textSkewX).append('|')
                .append(p.letterSpacing).append('|').append(p.textAlign.ordinal()).append('|').append(p.style.ordinal())
                .append('|').append(p.strokeWidth).append('|').append(p.isFakeBoldText()).append('|').append(p.color)
                .append('|').append(p.cap.ordinal()).append(p.join.ordinal())
                .append('|').append(p.colorFilter == null ? 0 : p.colorFilter.hashCode())
                .append('|').append(Math.round(t.getScaleX() * 1000)).append(',').append(Math.round(t.getShearX() * 1000))
                .append(',').append(Math.round(t.getShearY() * 1000)).append(',').append(Math.round(t.getScaleY() * 1000))
                .toString();
        }

        static synchronized Entry get(String key) { return CACHE.get(key); }

        static synchronized void put(String key, Entry e) { CACHE.put(key, e); }
    }

    public void drawText(String text, int start, int end, float x, float y, Paint paint) {
        drawText(text.substring(start, end), x, y, paint);
    }

    public void drawText(CharSequence text, int start, int end, float x, float y, Paint paint) {
        drawText(text.subSequence(start, end).toString(), x, y, paint);
    }

    public void drawText(char[] text, int index, int count, float x, float y, Paint paint) {
        drawText(new String(text, index, count), x, y, paint);
    }

    // ------------------------------------------------------------------ vértices

    public enum VertexMode { TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN }

    /**
     * PC: só a tira de triângulos com a imagem de um {@link BitmapShader} (o chão e as bandeiras
     * do cenário). Cada triângulo leva a imagem pela transformação afim que liga os pontos da
     * imagem aos da tela; a imagem se repete fora dela.
     */
    public void drawVertices(VertexMode mode, int vertexCount, float[] verts, int vertOffset, float[] texs, int texOffset,
                             int[] colors, int colorOffset, short[] indices, int indexOffset, int indexCount, Paint paint) {
        if (mode != VertexMode.TRIANGLE_STRIP || texs == null || !(paint.shader instanceof BitmapShader)) return;
        java.awt.Paint textura = paint.shader.toAwt();
        int n = vertexCount / 2;
        Composite saved = g.getComposite();
        java.awt.Paint savedPaint = g.getPaint();
        Object interp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        if (paint.getAlpha() < 255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, paint.getAlpha() / 255f));
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // Sem suavizar a borda: triângulos vizinhos se encostam sem costura (como na GPU).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setPaint(textura);
        AffineTransform base = g.getTransform();
        for (int i = 0; i + 2 < n; i++) {
            int a = vertOffset + 2 * i, ta = texOffset + 2 * i;
            double x0 = verts[a], y0 = verts[a + 1], x1 = verts[a + 2], y1 = verts[a + 3], x2 = verts[a + 4], y2 = verts[a + 5];
            double u0 = texs[ta], v0 = texs[ta + 1], u1 = texs[ta + 2], v1 = texs[ta + 3], u2 = texs[ta + 4], v2 = texs[ta + 5];
            // Imagem -> tela: (u,v) -> (x,y) nos três cantos.
            double du1 = u1 - u0, dv1 = v1 - v0, du2 = u2 - u0, dv2 = v2 - v0;
            double det = du1 * dv2 - du2 * dv1;
            if (Math.abs(det) < 1e-9) continue;
            double dx1 = x1 - x0, dy1 = y1 - y0, dx2 = x2 - x0, dy2 = y2 - y0;
            double m00 = (dx1 * dv2 - dx2 * dv1) / det, m01 = (dx2 * du1 - dx1 * du2) / det;
            double m10 = (dy1 * dv2 - dy2 * dv1) / det, m11 = (dy2 * du1 - dy1 * du2) / det;
            AffineTransform t = new AffineTransform(m00, m10, m01, m11, x0 - m00 * u0 - m01 * v0, y0 - m10 * u0 - m11 * v0);
            java.awt.geom.Path2D.Double tri = new java.awt.geom.Path2D.Double();
            tri.moveTo(u0, v0);
            tri.lineTo(u1, v1);
            tri.lineTo(u2, v2);
            tri.closePath();
            g.setTransform(base);
            g.transform(t);
            g.fill(tri);
        }
        g.setTransform(base);
        g.setPaint(savedPaint);
        if (interp != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp);
        if (aa != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        g.setComposite(saved);
    }

    // ------------------------------------------------------------------ bitmaps

    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
        drawBitmap(bitmap, null, new RectF(left, top, left + bitmap.getWidth(), top + bitmap.getHeight()), paint);
    }

    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) {
        drawBitmap(bitmap, src, new RectF(dst), paint);
    }

    public void drawBitmap(Bitmap bitmap, Rect src, RectF dst, Paint paint) {
        BufferedImage image = bitmap.image();
        int sx0 = 0, sy0 = 0, sx1 = image.getWidth(), sy1 = image.getHeight();
        if (src != null) {
            sx0 = Math.max(0, src.left);
            sy0 = Math.max(0, src.top);
            sx1 = Math.min(image.getWidth(), src.right);
            sy1 = Math.min(image.getHeight(), src.bottom);
        }
        if (sx1 <= sx0 || sy1 <= sy0 || dst.width() == 0f || dst.height() == 0f) return;
        if (areas != null) contar(new Rectangle2D.Float(Math.min(dst.left, dst.right), Math.min(dst.top, dst.bottom), Math.abs(dst.width()), Math.abs(dst.height())), "img");
        int alpha = paint == null ? 255 : paint.getAlpha();
        if (alpha <= 0) return;
        ColorFilter filter = paint == null ? null : paint.colorFilter;
        BufferedImage source = image;
        if (filter != null) {
            source = FilteredRegions.get(image, sx0, sy0, sx1, sy1, filter);
            sx1 -= sx0;
            sy1 -= sy0;
            sx0 = sy0 = 0;
        }
        // Redução forte (retratos grandes em caixas pequenas): usa uma cópia menor, sem serrilhado.
        double deviceScale = deviceScale() * Math.min(Math.abs(dst.width()) / (sx1 - sx0), Math.abs(dst.height()) / (sy1 - sy0));
        int halvings = Mipmaps.halvings(deviceScale);
        if (halvings > 0) {
            BufferedImage reduced = Mipmaps.level(source, halvings);
            double fx = reduced.getWidth() / (double) source.getWidth(), fy = reduced.getHeight() / (double) source.getHeight();
            source = reduced;
            sx0 = (int) Math.floor(sx0 * fx);
            sy0 = (int) Math.floor(sy0 * fy);
            sx1 = Math.max(sx0 + 1, (int) Math.ceil(sx1 * fx));
            sy1 = Math.max(sy0 + 1, (int) Math.ceil(sy1 * fy));
        }
        Composite saved = g.getComposite();
        if (alpha < 255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
        if (drawAxisAligned(source, sx0, sy0, sx1, sy1, dst, alpha)) {
            g.setComposite(saved);
            return;
        }
        bitmapHints(paint);
        // Destino em subpixel: transforma o recorte em vez de arredondar o retângulo.
        AffineTransform saveT = g.getTransform();
        g.translate(dst.left, dst.top);
        g.scale(dst.width() / (sx1 - sx0), dst.height() / (sy1 - sy0));
        g.drawImage(source, 0, 0, sx1 - sx0, sy1 - sy0, sx0, sy0, sx1, sy1, null);
        g.setTransform(saveT);
        g.setComposite(saved);
    }

    /**
     * Caminho rápido: sem rotação, a imagem vai escalada pelo {@link Resampler} para o
     * tamanho exato em pixels e é só copiada. Devolve false quando não se aplica.
     */
    private boolean drawAxisAligned(BufferedImage source, int sx0, int sy0, int sx1, int sy1, RectF dst, int alpha) {
        if (!Resampler.ENABLED) return false;
        AffineTransform t = g.getTransform();
        if (Math.abs(t.getShearX()) > 1e-6 || Math.abs(t.getShearY()) > 1e-6) return false;
        double ax = t.getScaleX() * dst.left + t.getTranslateX(), bx = t.getScaleX() * dst.right + t.getTranslateX();
        double ay = t.getScaleY() * dst.top + t.getTranslateY(), by = t.getScaleY() * dst.bottom + t.getTranslateY();
        boolean flipX = bx < ax, flipY = by < ay;
        int dx0 = (int) Math.round(Math.min(ax, bx)), dx1 = (int) Math.round(Math.max(ax, bx));
        int dy0 = (int) Math.round(Math.min(ay, by)), dy1 = (int) Math.round(Math.max(ay, by));
        int dw = dx1 - dx0, dh = dy1 - dy0;
        if (dw <= 0 || dh <= 0) return true;
        // Fora da tela: nada a desenhar.
        if (dx1 <= 0 || dy1 <= 0 || dx0 >= width || dy0 >= height) return true;
        if ((long) dw * dh > 8_000_000L) return false;
        g.setTransform(new AffineTransform());
        if (!flipX && !flipY && dw == sx1 - sx0 && dh == sy1 - sy0) {
            if (sx0 == 0 && sy0 == 0 && dw == source.getWidth() && dh == source.getHeight()
                && blendDirect(source, dx0, dy0, dw, dh, alpha)) {
                g.setTransform(t);
                return true;
            }
            g.drawImage(source, dx0, dy0, dx1, dy1, sx0, sy0, sx1, sy1, null);
        } else {
            Resampler.Scaled scaled = Resampler.get(source, sx0, sy0, sx1, sy1, dw, dh, flipX, flipY);
            if (scaled == null) {
                // Tamanho mudando a cada quadro (o zoom da câmera): ampliada e misturada numa
                // passada só, em paralelo; sem o retângulo de recorte, o caminho normal do Java2D.
                boolean feito = blendScaledDirect(source, sx0, sy0, sx1, sy1, dx0, dy0, dw, dh, flipX, flipY, alpha);
                g.setTransform(t);
                return feito;
            }
            BufferedImage si = scaled.image;
            if (!blendDirect(si, dx0 + scaled.dx, dy0 + scaled.dy, si.getWidth(), si.getHeight(), alpha)) {
                g.drawImage(si, dx0 + scaled.dx, dy0 + scaled.dy, null);
            }
        }
        g.setTransform(t);
        return true;
    }

    /**
     * Mistura {@code src} (inteira) em ({@code x}, {@code y}) do destino, em coordenadas de
     * dispositivo, pelos núcleos todos. Só quando o recorte atual é um retângulo.
     */
    /** Como o {@link #blendDirect}, com a imagem escalada na mistura (transformação já zerada). */
    private boolean blendScaledDirect(BufferedImage src, int sx0, int sy0, int sx1, int sy1, int dx0, int dy0,
                                      int dw, int dh, boolean flipX, boolean flipY, int alpha) {
        if (!Blender.ENABLED || image == null || !Blender.supports(src)) return false;
        if (!(g.getComposite() instanceof AlphaComposite) || ((AlphaComposite) g.getComposite()).getRule() != AlphaComposite.SRC_OVER) return false;
        int cx0 = 0, cy0 = 0, cx1 = image.getWidth(), cy1 = image.getHeight();
        Shape clip = g.getClip();
        if (clip != null) {
            if (!(clip instanceof java.awt.Rectangle) && !(clip instanceof Rectangle2D)) return false;
            Rectangle2D r = clip.getBounds2D();
            cx0 = (int) Math.ceil(r.getMinX() - 1e-6);
            cy0 = (int) Math.ceil(r.getMinY() - 1e-6);
            cx1 = (int) Math.floor(r.getMaxX() + 1e-6);
            cy1 = (int) Math.floor(r.getMaxY() + 1e-6);
        }
        Blender.blendScaled(src, sx0, sy0, sx1, sy1, image, dx0, dy0, dw, dh, flipX, flipY, cx0, cy0, cx1, cy1, alpha);
        return true;
    }

    /** Brilhos redondos já desenhados, por (degradê, raios em degraus de 2%). */
    private static final java.util.LinkedHashMap<Object, BufferedImage> BRILHOS =
        new java.util.LinkedHashMap<Object, BufferedImage>(64, 0.75f, true) {
            @Override protected boolean removeEldestEntry(java.util.Map.Entry<Object, BufferedImage> e) { return size() > 96; }
        };

    /**
     * Brilho redondo (círculo cheio com o degradê radial centrado nele, sem rotação): as lanternas,
     * os halos e as fogueiras do cenário. O Java2D pinta degradê radial com borda suave numa thread
     * só (~3–5 ms por quadro); aqui ele é desenhado uma vez por tamanho e só misturado.
     */
    private boolean desenharBrilho(float cx, float cy, float radius, Paint paint) {
        if (!Blender.ENABLED || image == null || paint == null || paint.getStyle() != Paint.Style.FILL) return false;
        if (!(paint.shader instanceof RadialGradient) || paint.colorFilter != null || paint.xfermode != null) return false;
        RadialGradient rg = (RadialGradient) paint.shader;
        if (!rg.naBorda || rg.localTransform() != null || rg.centroX != cx || rg.centroY != cy || rg.raio != radius) return false;
        AffineTransform t = g.getTransform();
        if (Math.abs(t.getShearX()) > 1e-6 || Math.abs(t.getShearY()) > 1e-6) return false;
        double rx = Math.abs(radius * t.getScaleX()), ry = Math.abs(radius * t.getScaleY());
        if (rx < 2 || ry < 2 || rx * ry > 1_000_000) return false;
        int qx = (int) Math.round(Math.log(rx) / Math.log(1.02)), qy = (int) Math.round(Math.log(ry) / Math.log(1.02));
        java.util.List<Object> chave = java.util.Arrays.asList(rg, qx, qy);
        BufferedImage spr;
        synchronized (BRILHOS) {
            spr = BRILHOS.get(chave);
            if (spr == null) {
                double ex = Math.pow(1.02, qx), ey = Math.pow(1.02, qy);
                int w = (int) Math.ceil(ex * 2) + 2, h = (int) Math.ceil(ey * 2) + 2;
                spr = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D sg = spr.createGraphics();
                sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                sg.translate(w / 2.0, h / 2.0);
                sg.scale(ex / radius, ey / radius);
                sg.translate(-cx, -cy);
                sg.setPaint(rg.toAwt());
                sg.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f));
                sg.dispose();
                BRILHOS.put(chave, spr);
            }
        }
        double dcx = t.getScaleX() * cx + t.getTranslateX(), dcy = t.getScaleY() * cy + t.getTranslateY();
        int x = (int) Math.round(dcx - spr.getWidth() / 2.0), y = (int) Math.round(dcy - spr.getHeight() / 2.0);
        g.setTransform(new AffineTransform());
        boolean feito = blendDirect(spr, x, y, spr.getWidth(), spr.getHeight(), paint.getAlpha());
        g.setTransform(t);
        return feito;
    }

    /**
     * Retângulo grande sem rotação, cor sólida ou degradê vertical de duas cores, "fonte sobre
     * destino": pintado linha a linha pelos núcleos todos. O Java2D fazia cada um numa thread só
     * (~1,5 ms por retângulo translúcido de tela cheia; a neblina, ~1,3 ms).
     */
    private boolean preencherRetangulo(float l, float t, float r, float b, Paint paint) {
        if (!Blender.ENABLED || image == null || paint == null || paint.getStyle() != Paint.Style.FILL) return false;
        if (paint.colorFilter != null || paint.xfermode != null) return false;
        LinearGradient lg = null;
        if (paint.shader != null) {
            if (!(paint.shader instanceof LinearGradient) || paint.shader.localTransform() != null) return false;
            lg = (LinearGradient) paint.shader;
            if (!lg.vertical2) return false;
        }
        if (!(g.getComposite() instanceof AlphaComposite)) return false;
        AlphaComposite ac = (AlphaComposite) g.getComposite();
        if (ac.getRule() != AlphaComposite.SRC_OVER || ac.getAlpha() < 0.999f) return false;
        AffineTransform tr = g.getTransform();
        if (Math.abs(tr.getShearX()) > 1e-6 || Math.abs(tr.getShearY()) > 1e-6) return false;
        double sy = tr.getScaleY(), ty = tr.getTranslateY();
        double ax = tr.getScaleX() * l + tr.getTranslateX(), bx = tr.getScaleX() * r + tr.getTranslateX();
        double ay = sy * t + ty, by = sy * b + ty;
        int x0 = (int) Math.round(Math.min(ax, bx)), x1 = (int) Math.round(Math.max(ax, bx));
        int y0 = (int) Math.round(Math.min(ay, by)), y1 = (int) Math.round(Math.max(ay, by));
        // Só retângulo grande: a borda suavizada do Java2D numa forma pequena aparece.
        if (x1 - x0 < 48 || y1 - y0 < 24) return false;
        int cx0 = 0, cy0 = 0, cx1 = image.getWidth(), cy1 = image.getHeight();
        Shape clip = g.getClip();
        if (clip != null) {
            if (!(clip instanceof java.awt.Rectangle) && !(clip instanceof Rectangle2D)) return false;
            // O recorte vem no espaço do usuário: leva para o dispositivo.
            Rectangle2D rc = tr.createTransformedShape(clip).getBounds2D();
            cx0 = (int) Math.ceil(rc.getMinX() - 1e-6);
            cy0 = (int) Math.ceil(rc.getMinY() - 1e-6);
            cx1 = (int) Math.floor(rc.getMaxX() + 1e-6);
            cy1 = (int) Math.floor(rc.getMaxY() + 1e-6);
        }
        x0 = Math.max(x0, Math.max(cx0, 0));
        y0 = Math.max(y0, Math.max(cy0, 0));
        x1 = Math.min(x1, Math.min(cx1, image.getWidth()));
        y1 = Math.min(y1, Math.min(cy1, image.getHeight()));
        if (x1 <= x0 || y1 <= y0) return true;
        int[] cores = new int[y1 - y0];
        int extra = lg == null ? 255 : paint.getAlpha();
        if (lg == null) {
            java.util.Arrays.fill(cores, Blender.premultiplicar(paint.color, 255));
        } else {
            for (int y = y0; y < y1; y++) {
                // O meio da linha no espaço do degradê, como o Java2D amostra.
                double ly = (y + 0.5 - ty) / sy;
                double f = Math.max(0, Math.min(1, (ly - lg.gy0) / (lg.gy1 - lg.gy0)));
                cores[y - y0] = Blender.premultiplicar(misturar(lg.cor0, lg.cor1, f), extra);
            }
        }
        Blender.fillRows(image, x0, y0, x1, y1, cores);
        return true;
    }

    private static int misturar(int c0, int c1, double f) {
        int a = (int) Math.round((c0 >>> 24) + ((c1 >>> 24) - (c0 >>> 24)) * f);
        int r = (int) Math.round(((c0 >> 16) & 0xFF) + (((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF)) * f);
        int gg = (int) Math.round(((c0 >> 8) & 0xFF) + (((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF)) * f);
        int b = (int) Math.round((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * f);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private boolean blendDirect(BufferedImage src, int x, int y, int w, int h, int alpha) {
        if (!Blender.ENABLED || image == null || !Blender.supports(src)) return false;
        if (!(g.getComposite() instanceof AlphaComposite) || ((AlphaComposite) g.getComposite()).getRule() != AlphaComposite.SRC_OVER) return false;
        AffineTransform t = g.getTransform();
        if (!t.isIdentity()) {
            if (t.getType() != AffineTransform.TYPE_TRANSLATION) return false;
            x += (int) Math.round(t.getTranslateX());
            y += (int) Math.round(t.getTranslateY());
        }
        // A imagem da camada tem a origem deslocada: o Graphics2D dela já desconta isso.
        int cx0 = 0, cy0 = 0, cx1 = image.getWidth(), cy1 = image.getHeight();
        Shape clip = g.getClip();
        if (clip != null) {
            if (!(clip instanceof java.awt.Rectangle) && !(clip instanceof Rectangle2D)) return false;
            Rectangle2D r = clip.getBounds2D();
            if (!t.isIdentity()) r = new Rectangle2D.Double(r.getX() + t.getTranslateX(), r.getY() + t.getTranslateY(), r.getWidth(), r.getHeight());
            cx0 = (int) Math.ceil(r.getMinX() - 1e-6);
            cy0 = (int) Math.ceil(r.getMinY() - 1e-6);
            cx1 = (int) Math.floor(r.getMaxX() + 1e-6);
            cy1 = (int) Math.floor(r.getMaxY() + 1e-6);
        }
        if (w != src.getWidth() || h != src.getHeight()) {
            // Só a parte de cima/esquerda (camadas reaproveitadas maiores que o necessário).
            cx1 = Math.min(cx1, x + w);
            cy1 = Math.min(cy1, y + h);
        }
        Blender.blend(src, image, x, y, cx0, cy0, cx1, cy1, alpha);
        return true;
    }

    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
        BufferedImage image = bitmap.image();
        int alpha = paint == null ? 255 : paint.getAlpha();
        if (alpha <= 0) return;
        if (paint != null && paint.colorFilter != null) {
            image = FilteredRegions.get(image, 0, 0, image.getWidth(), image.getHeight(), paint.colorFilter);
        }
        Composite saved = g.getComposite();
        if (alpha < 255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
        bitmapHints(paint);
        if (matrix == null || matrix.isAffine()) {
            g.drawImage(image, matrix == null ? new AffineTransform() : matrix.toAffine(), null);
        } else {
            Perspective.draw(g, image, matrix, width, height);
        }
        g.setComposite(saved);
    }

    // ------------------------------------------------------------------ pintura

    private void draw(Shape shape, Paint paint) {
        if (areas != null) contar(shape, "forma");
        switch (paint.style) {
            case FILL:
                fill(shape, paint);
                break;
            case STROKE:
                stroke(shape, paint, paint.strokeWidth);
                break;
            default:
                fill(shape, paint);
                stroke(shape, paint, paint.strokeWidth);
                break;
        }
    }

    private void fill(Shape shape, Paint paint) {
        AffineTransform local = prepare(paint, paint.isAntiAlias());
        if (local != null) {
            AffineTransform saved = g.getTransform();
            try {
                Shape s = local.createInverse().createTransformedShape(shape);
                g.transform(local);
                g.fill(s);
            } catch (NoninvertibleTransformException ignored) {
                // Shader degenerado: não desenha.
            }
            g.setTransform(saved);
        } else {
            g.fill(shape);
        }
        finish(paint);
    }

    private void stroke(Shape shape, Paint paint, float width) {
        strokeShape(shape, paint, width, paint.isAntiAlias());
        finish(paint);
    }

    private void strokeShape(Shape shape, Paint paint, float width, boolean antialias) {
        prepare(paint, antialias);
        Stroke s = g.getStroke();
        if (paint.pathEffect instanceof DashPathEffect) {
            DashPathEffect d = (DashPathEffect) paint.pathEffect;
            g.setStroke(new BasicStroke(Math.max(0f, width), cap(paint.cap), join(paint.join), Math.max(1f, paint.strokeMiter),
                d.intervals, d.phase));
        } else {
            g.setStroke(new BasicStroke(Math.max(0f, width), cap(paint.cap), join(paint.join), Math.max(1f, paint.strokeMiter)));
        }
        g.draw(shape);
        g.setStroke(s);
    }

    private Composite paintComposite;

    /** Cor (ou degradê), transparência e suavização do Paint. Devolve a matriz local do shader. */
    private AffineTransform prepare(Paint paint, boolean antialias) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        paintComposite = null;
        java.awt.Paint shader = paint.shader == null ? null : paint.shader.toAwt();
        if (shader != null) {
            g.setPaint(shader);
            int alpha = paint.getAlpha();
            if (alpha < 255) {
                paintComposite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
            }
            return paint.shader.localTransform();
        }
        int color = paint.colorFilter == null ? paint.color : paint.colorFilter.filter(paint.color);
        g.setColor(new java.awt.Color(color, true));
        if (paint.xfermode instanceof PorterDuffXfermode && ((PorterDuffXfermode) paint.xfermode).mode == PorterDuff.Mode.CLEAR) {
            paintComposite = g.getComposite();
            g.setComposite(AlphaComposite.Clear);
        }
        return null;
    }

    private void finish(Paint paint) {
        if (paintComposite != null) {
            g.setComposite(paintComposite);
            paintComposite = null;
        }
    }

    private double deviceScale() {
        AffineTransform t = g.getTransform();
        return Math.sqrt(Math.abs(t.getDeterminant()));
    }

    private void bitmapHints(Paint paint) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private static int cap(Paint.Cap cap) {
        return cap == Paint.Cap.ROUND ? BasicStroke.CAP_ROUND : cap == Paint.Cap.SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT;
    }

    private static int join(Paint.Join join) {
        return join == Paint.Join.ROUND ? BasicStroke.JOIN_ROUND : join == Paint.Join.BEVEL ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER;
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
    }

    /** Aplica o filtro de cor em cada pixel (imagem ARGB pré-multiplicada). */
    static void filterInPlace(BufferedImage img, ColorFilter filter) {
        filterInPlace(img, img.getWidth(), img.getHeight(), filter);
    }

    /** Só o canto {@code w}x{@code h} da imagem. */
    static void filterInPlace(BufferedImage img, int w, int h, ColorFilter filter) {
        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        int stride = img.getWidth();
        int lastIn = 0, lastOut = premultiply(filter.filter(0));
        for (int y = 0; y < h; y++) {
            for (int i = y * stride, end = i + w; i < end; i++) {
                int p = px[i];
                if (p == lastIn) { px[i] = lastOut; continue; }
                int out = premultiply(filter.filter(unpremultiply(p)));
                lastIn = p;
                lastOut = out;
                px[i] = out;
            }
        }
    }

    static int unpremultiply(int p) {
        int a = p >>> 24;
        if (a == 255 || a == 0) return a == 0 ? 0 : p;
        int r = Math.min(255, ((p >> 16) & 0xFF) * 255 / a);
        int gr = Math.min(255, ((p >> 8) & 0xFF) * 255 / a);
        int b = Math.min(255, (p & 0xFF) * 255 / a);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    static int premultiply(int c) {
        int a = c >>> 24;
        if (a == 255) return c;
        if (a == 0) return 0;
        int r = (((c >> 16) & 0xFF) * a + 127) / 255;
        int gr = (((c >> 8) & 0xFF) * a + 127) / 255;
        int b = ((c & 0xFF) * a + 127) / 255;
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    // ------------------------------------------------------------------ caches

    /** Recortes de bitmap já filtrados (o CPU pálido e os flashes de dano reaproveitam o quadro). */
    static final class FilteredRegions {
        private static final int MAX = 96;
        private static final Map<Key, BufferedImage> CACHE = new LinkedHashMap<Key, BufferedImage>(128, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> eldest) { return size() > MAX; }
        };

        private static final class Key {
            /** {@link ImageIds}: a chave não segura o atlas (ele sai da memória na troca de dupla). */
            final int image;
            final int x0, y0, x1, y1;
            final ColorFilter filter;

            Key(int image, int x0, int y0, int x1, int y1, ColorFilter filter) {
                this.image = image;
                this.x0 = x0;
                this.y0 = y0;
                this.x1 = x1;
                this.y1 = y1;
                this.filter = filter;
            }

            @Override public boolean equals(Object o) {
                if (!(o instanceof Key)) return false;
                Key k = (Key) o;
                return k.image == image && k.x0 == x0 && k.y0 == y0 && k.x1 == x1 && k.y1 == y1 && k.filter.equals(filter);
            }

            @Override public int hashCode() {
                return ((image * 31 + x0) * 31 + y0) * 31 + x1 * 17 + y1 + filter.hashCode() * 7;
            }
        }

        static synchronized BufferedImage get(BufferedImage image, int x0, int y0, int x1, int y1, ColorFilter filter) {
            Key key = new Key(ImageIds.of(image), x0, y0, x1, y1, filter);
            BufferedImage out = CACHE.get(key);
            if (out != null) return out;
            out = new BufferedImage(x1 - x0, y1 - y0, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = out.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.drawImage(image, -x0, -y0, null);
            g.dispose();
            filterInPlace(out, filter);
            CACHE.put(key, out);
            return out;
        }
    }

    /**
     * Cópias da imagem inteira reduzidas pela metade, sucessivamente (como mipmaps), para
     * desenhar imagens grandes bem pequenas sem serrilhado. Calculadas uma vez por imagem:
     * os atlas dos lutadores trocam de recorte a cada quadro, mas o nível é o mesmo.
     */
    static final class Mipmaps {
        private static final java.util.WeakHashMap<BufferedImage, BufferedImage[]> LEVELS = new java.util.WeakHashMap<>();

        /** Quantas reduções pela metade cabem na escala (0 = imagem original). */
        static int halvings(double scale) {
            int h = 0;
            for (double s = scale; s < 0.5 && h < 6; s *= 2) h++;
            return h;
        }

        static synchronized BufferedImage level(BufferedImage image, int halvings) {
            if (halvings <= 0) return image;
            BufferedImage[] levels = LEVELS.get(image);
            if (levels == null) {
                levels = new BufferedImage[7];
                LEVELS.put(image, levels);
            }
            if (levels[halvings] != null) return levels[halvings];
            BufferedImage previous = level(image, halvings - 1);
            int w = Math.max(1, previous.getWidth() / 2), h = Math.max(1, previous.getHeight() / 2);
            BufferedImage half = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = half.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(previous, 0, 0, w, h, null);
            g.dispose();
            levels[halvings] = half;
            return half;
        }
    }

    /** Bitmap com perspectiva: amostra cada pixel do destino pela matriz inversa (cacheado). */
    static final class Perspective {
        private static final int MAX = 32;
        private static final class Warped {
            final BufferedImage image;
            final int x, y;

            Warped(BufferedImage image, int x, int y) {
                this.image = image;
                this.x = x;
                this.y = y;
            }
        }

        private static final Map<String, Warped> CACHE = new LinkedHashMap<String, Warped>(64, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, Warped> eldest) { return size() > MAX; }
        };
        private static final java.util.WeakHashMap<BufferedImage, Integer> IDS = new java.util.WeakHashMap<>();
        private static int nextId;

        static void draw(Graphics2D g, BufferedImage image, Matrix matrix, int width, int height) {
            AffineTransform t = g.getTransform();
            double[] dev = {t.getScaleX(), t.getShearX(), t.getTranslateX(), t.getShearY(), t.getScaleY(), t.getTranslateY(), 0, 0, 1};
            double[] full = dev.clone();
            Matrix.multiply(full, matrix.m);
            Warped w;
            synchronized (Perspective.class) {
                Integer id = IDS.get(image);
                if (id == null) IDS.put(image, id = ++nextId);
                StringBuilder key = new StringBuilder().append(id);
                for (double v : full) key.append(',').append((float) v);
                String k = key.toString();
                w = CACHE.get(k);
                if (w == null) {
                    w = warp(image, full, width, height);
                    if (w != null) CACHE.put(k, w);
                }
            }
            if (w == null) return;
            g.setTransform(new AffineTransform());
            g.drawImage(w.image, w.x, w.y, null);
            g.setTransform(t);
        }

        private static Warped warp(BufferedImage image, double[] m, int width, int height) {
            int sw = image.getWidth(), sh = image.getHeight();
            double[] corners = {0, 0, sw, 0, sw, sh, 0, sh};
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (int i = 0; i < 8; i += 2) {
                double wgt = m[6] * corners[i] + m[7] * corners[i + 1] + m[8];
                if (wgt <= 0) return null;
                double x = (m[0] * corners[i] + m[1] * corners[i + 1] + m[2]) / wgt;
                double y = (m[3] * corners[i] + m[4] * corners[i + 1] + m[5]) / wgt;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
            int x0 = Math.max(0, (int) Math.floor(minX)), y0 = Math.max(0, (int) Math.floor(minY));
            int x1 = Math.min(width, (int) Math.ceil(maxX)), y1 = Math.min(height, (int) Math.ceil(maxY));
            if (x1 <= x0 || y1 <= y0) return null;
            double[] inv = Matrix.invert3(m);
            if (inv == null) return null;
            int ow = x1 - x0, oh = y1 - y0;
            // Fonte muito maior que o destino: reduz antes, para não serrilhar.
            double reduce = Math.max(sw / (double) ow, sh / (double) oh);
            BufferedImage src = image;
            double srcScale = 1;
            while (reduce > 2 && src.getWidth() > 2 && src.getHeight() > 2) {
                BufferedImage half = new BufferedImage(src.getWidth() / 2, src.getHeight() / 2, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D hg = half.createGraphics();
                hg.setComposite(AlphaComposite.Src);
                hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                hg.drawImage(src, 0, 0, half.getWidth(), half.getHeight(), null);
                hg.dispose();
                srcScale *= half.getWidth() / (double) src.getWidth();
                src = half;
                reduce /= 2;
            }
            int[] in = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
            int iw = src.getWidth(), ih = src.getHeight();
            BufferedImage out = new BufferedImage(ow, oh, BufferedImage.TYPE_INT_ARGB_PRE);
            int[] px = ((DataBufferInt) out.getRaster().getDataBuffer()).getData();
            for (int y = 0; y < oh; y++) {
                double dy = y0 + y + 0.5;
                for (int x = 0; x < ow; x++) {
                    double dx = x0 + x + 0.5;
                    double wgt = inv[6] * dx + inv[7] * dy + inv[8];
                    double u = (inv[0] * dx + inv[1] * dy + inv[2]) / wgt * srcScale - 0.5;
                    double v = (inv[3] * dx + inv[4] * dy + inv[5]) / wgt * srcScale - 0.5;
                    if (u < -0.5 || v < -0.5 || u > iw - 0.5 || v > ih - 0.5) continue;
                    px[y * ow + x] = bilinear(in, iw, ih, u, v);
                }
            }
            return new Warped(out, x0, y0);
        }

        private static int bilinear(int[] in, int w, int h, double u, double v) {
            int x = (int) Math.floor(u), y = (int) Math.floor(v);
            double fx = u - x, fy = v - y;
            int x0 = Math.max(0, Math.min(w - 1, x)), x1 = Math.max(0, Math.min(w - 1, x + 1));
            int y0 = Math.max(0, Math.min(h - 1, y)), y1 = Math.max(0, Math.min(h - 1, y + 1));
            int p00 = in[y0 * w + x0], p10 = in[y0 * w + x1], p01 = in[y1 * w + x0], p11 = in[y1 * w + x1];
            int result = 0;
            for (int shift = 0; shift < 32; shift += 8) {
                double top = ((p00 >>> shift) & 0xFF) * (1 - fx) + ((p10 >>> shift) & 0xFF) * fx;
                double bottom = ((p01 >>> shift) & 0xFF) * (1 - fx) + ((p11 >>> shift) & 0xFF) * fx;
                result |= ((int) Math.round(top * (1 - fy) + bottom * fy) & 0xFF) << shift;
            }
            return result;
        }
    }
}
