package android.graphics;

/** PC: a imagem que se repete (só o que o jogo usa: REPEAT nos dois eixos, em drawVertices). */
public class BitmapShader extends Shader {
    final Bitmap bitmap;
    final TileMode tileX, tileY;

    public BitmapShader(Bitmap bitmap, TileMode tileX, TileMode tileY) {
        this.bitmap = bitmap;
        this.tileX = tileX;
        this.tileY = tileY;
    }

    @Override java.awt.Paint toAwt() {
        java.awt.image.BufferedImage img = bitmap.image();
        return new java.awt.TexturePaint(img, new java.awt.geom.Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight()));
    }
}
