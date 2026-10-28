package android.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Imagem em memória: um {@link BufferedImage} ARGB pré-multiplicado (o mais rápido no Java2D). */
public final class Bitmap {
    public static final int DENSITY_NONE = 0;

    public enum Config { ALPHA_8, RGB_565, ARGB_4444, ARGB_8888, RGBA_F16, HARDWARE }

    BufferedImage image;
    private int density = DENSITY_NONE;
    private final Config config;
    /** Bytes deste bitmap na conta de {@link #bytesVivos()} (zera ao reciclar ou ser coletado). */
    private final Conta conta;

    /** Pixels decodificados que ainda existem (o "heap nativo" do Android, onde ficam os bitmaps). */
    private static final java.util.concurrent.atomic.AtomicLong VIVOS = new java.util.concurrent.atomic.AtomicLong();
    private static final java.lang.ref.Cleaner COLETOR = java.lang.ref.Cleaner.create();

    private static final class Conta implements Runnable {
        private final java.util.concurrent.atomic.AtomicLong bytes;
        Conta(long bytes) { this.bytes = new java.util.concurrent.atomic.AtomicLong(bytes); VIVOS.addAndGet(bytes); }
        @Override public void run() { VIVOS.addAndGet(-bytes.getAndSet(0)); }
    }

    Bitmap(BufferedImage image) {
        this.image = toPremultiplied(image);
        this.config = Config.ARGB_8888;
        conta = new Conta((long) this.image.getWidth() * this.image.getHeight() * 4L);
        COLETOR.register(this, conta);
    }

    /** Extensão do PC: bytes de todos os bitmaps ainda vivos (para medir carregamentos). */
    public static long bytesVivos() { return VIVOS.get(); }

    /** Extensão do PC: a imagem do Java2D (somente leitura para quem desenha). */
    public BufferedImage image() {
        if (image == null) throw new IllegalStateException("Bitmap reciclado");
        return image;
    }

    /** Extensão do PC. */
    public static Bitmap wrap(BufferedImage image) { return new Bitmap(image); }

    public int getWidth() { return image().getWidth(); }

    public int getHeight() { return image().getHeight(); }

    public int getDensity() { return density; }

    public void setDensity(int density) { this.density = density; }

    public Config getConfig() { return config; }

    public boolean hasAlpha() { return true; }

    public boolean isRecycled() { return image == null; }

    public void recycle() { image = null; conta.run(); }

    public int getByteCount() { return getWidth() * getHeight() * 4; }

    public int getPixel(int x, int y) { return image().getRGB(x, y); }

    public void setPixel(int x, int y, int color) { image().setRGB(x, y, color); }

    public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
        image().getRGB(x, y, width, height, pixels, offset, stride);
    }

    public void eraseColor(int color) {
        Graphics2D g = image().createGraphics();
        g.setComposite(java.awt.AlphaComposite.Src);
        g.setColor(new java.awt.Color(color, true));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        return new Bitmap(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE));
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        BufferedImage src = source.image();
        if (x < 0 || y < 0 || width <= 0 || height <= 0 || x + width > src.getWidth() || y + height > src.getHeight())
            throw new IllegalArgumentException("recorte fora do bitmap");
        if (x == 0 && y == 0 && width == src.getWidth() && height == src.getHeight()) return source;
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = copy.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Src);
        g.drawImage(src, -x, -y, null);
        g.dispose();
        return new Bitmap(copy);
    }

    /** PC: uma cópia (a da placa de vídeo é igual à comum aqui). */
    public Bitmap copy(Config config, boolean isMutable) { return createBitmap(this); }

    public static Bitmap createBitmap(Bitmap src) { return createBitmap(src, 0, 0, src.getWidth(), src.getHeight()); }

    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        BufferedImage out = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, filter
            ? java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR : java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src.image(), 0, 0, dstWidth, dstHeight, null);
        g.dispose();
        return new Bitmap(out);
    }

    static BufferedImage toPremultiplied(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB_PRE) return image;
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = out.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Src);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return out;
    }
}
