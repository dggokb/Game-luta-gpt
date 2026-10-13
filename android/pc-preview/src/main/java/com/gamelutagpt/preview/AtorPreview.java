package com.gamelutagpt.preview;
import com.gamelutagpt.stage.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/**
 * Folha de conferência dos atores de fundo: cada animação em alguns instantes, no cenário,
 * com a câmera de repouso.
 *
 * <pre>AtorPreview pasta-do-cenario id-do-ator anim1,anim2,... x-na-tela saida.png</pre>
 */
public final class AtorPreview {
  public static void main(String[] a) throws Exception {
    File dir = new File(a[0]);
    StagePack pack = StagePack.load(new FileRenderAssets(dir.getParentFile().getParentFile()), "stages/" + dir.getName());
    for (String w : pack.warnings) System.out.println("aviso: " + w);
    StageScene scene = new StageScene(pack, new StageWorld(565f, 2600f, 1.24f, 700f, 552f));
    String[] anims = a[2].split(",");
    float[] times = {0.05f, 0.12f, 0.4f, 0.8f, 1.2f, 1.6f, 2.0f, 2.6f, 3.2f};
    int cw = 200, chh = 300;
    BufferedImage out = new BufferedImage(cw * times.length, chh * anims.length, BufferedImage.TYPE_INT_RGB);
    Graphics2D go = out.createGraphics();
    int x0 = Integer.parseInt(a[3]);
    for (int r = 0; r < anims.length; r++) {
      scene.update(1f); // volta ao parado
      for (int i = 0; i < 200; i++) scene.update(1f/60f);
      scene.actorPlay(a[1], anims[r]);
      float t = 0f;
      for (int k = 0; k < times.length; k++) {
        while (t < times[k]) { scene.update(1f/60f); t += 1f/60f; }
        BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        scene.setCamera(1.24f, 700f - 1280f / (2f * 1.24f), 565f - 552f / 1.24f);
        scene.drawBackground(new Java2DRenderCanvas(g, 1280, 720));
        go.drawImage(img.getSubimage(x0 - cw / 2, 140, cw, chh), k * cw, r * chh, null);
        go.setColor(Color.WHITE); go.drawString(anims[r] + " " + times[k] + "s", k * cw + 4, r * chh + 14);
      }
    }
    ImageIO.write(out, "png", new File(a[4]));
  }
}
