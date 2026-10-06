package com.gamelutagpt.preview;

import com.gamelutagpt.stage.StagePack;
import com.gamelutagpt.stage.StageScene;
import com.gamelutagpt.stage.StageWorld;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Visualizador de cenário no PC, com uma câmera de luta simulada (afastar,
 * aproximar, andar pelo cenário e super pulo) para ver o falso 3D.
 *
 * <pre>
 * Janela:   StagePreview pasta-do-cenario
 * Quadros:  StagePreview pasta-do-cenario --quadros saida/
 * </pre>
 */
public final class StagePreview {
    // Medidas da luta (Arena e CameraRig do jogo).
    private static final float GROUND_Y = 565f;
    private static final float WORLD_WIDTH = 2600f;
    private static final float CAMERA_ZOOM = 1.12f;
    private static final float CAMERA_MIN_ZOOM = 0.78f;
    private static final float GROUND_SCREEN_Y = 552f;
    private static final float VW = StageWorld.VW;
    private static final float VH = StageWorld.VH;

    private final StageScene scene;
    private float time;
    private float p1x, p2x, p1y;
    private float zoom = CAMERA_ZOOM, left, top;

    private StagePreview(StagePack pack) {
        StageWorld world = new StageWorld(GROUND_Y, WORLD_WIDTH, CAMERA_ZOOM, 700f, GROUND_SCREEN_Y);
        scene = new StageScene(pack, world);
        script(0f);
    }

    /** Roteiro de 12 s da "luta": posições dos dois lutadores e altura do pulo. */
    private void script(float t) {
        float loop = t % 12f;
        float a, b;
        if (loop < 2f) {
            a = 420f; b = 980f;
        } else if (loop < 4f) {
            float k = smooth((loop - 2f) / 2f);
            a = 420f - 250f * k; b = 980f + 450f * k;
        } else if (loop < 6f) {
            float k = smooth((loop - 4f) / 2f);
            a = 170f + 480f * k; b = 1430f - 670f * k;
        } else if (loop < 9f) {
            float k = smooth((loop - 6f) / 3f);
            a = 650f + 1200f * k; b = 760f + 1200f * k;
        } else {
            float k = smooth((loop - 9f) / 3f);
            a = 1850f - 1430f * k; b = 1960f - 980f * k;
        }
        p1x = a;
        p2x = b;
        float jump = loop > 9.2f && loop < 10.6f ? (float)Math.sin((loop - 9.2f) / 1.4f * Math.PI) : 0f;
        p1y = GROUND_Y - 520f * jump;
        camera(jump > 0f);
    }

    private static float smooth(float k) {
        float c = Math.max(0f, Math.min(1f, k));
        return c * c * (3f - 2f * c);
    }

    /** Mesmo enquadramento do CameraRig, sem a suavização. */
    private void camera(boolean superJump) {
        float separation = Math.abs(p1x - p2x);
        float zoomX = VW / Math.max(VW / CAMERA_ZOOM, separation + 520f);
        float highest = Math.min(p1y - 300f, GROUND_Y - 300f);
        float zoomY = (GROUND_SCREEN_Y - 64f) / Math.max(1f, GROUND_Y - highest);
        zoom = Math.max(CAMERA_MIN_ZOOM, Math.min(CAMERA_ZOOM, superJump ? zoomX : Math.min(zoomX, zoomY)));
        float visible = VW / zoom;
        float center = Math.max(visible / 2f, Math.min(WORLD_WIDTH - visible / 2f, (p1x + p2x) / 2f));
        left = center - visible / 2f;
        top = GROUND_Y - GROUND_SCREEN_Y / zoom;
        if (highest < top + 64f / zoom) top = highest - 64f / zoom;
    }

    private void update(float dt) {
        time += dt;
        script(time);
        scene.setCamera(zoom, left, top);
        scene.update(dt);
    }

    private void render(Graphics2D g, int width, int height) {
        g.scale(width / VW, height / VH);
        Java2DRenderCanvas canvas = new Java2DRenderCanvas(g, width, height);
        scene.drawBackground(canvas);

        // Lutadores falsos no plano z = 0, com o mesmo transform do jogo.
        java.awt.geom.AffineTransform saved = g.getTransform();
        g.scale(zoom, zoom);
        g.translate(-left, -top);
        float reflection = scene.fighterReflection();
        drawFighter(g, p1x, p1y, new Color(244, 183, 59), reflection);
        drawFighter(g, p2x, GROUND_Y, new Color(74, 205, 232), reflection);
        g.setTransform(saved);

        scene.drawForeground(canvas);
        g.setColor(Color.WHITE);
        g.drawString(String.format(Locale.ROOT, "zoom %.2f  x %.0f", zoom, left + VW / zoom / 2f), 12, 20);
    }

    private static void drawFighter(Graphics2D g, float x, float footY, Color color, float reflection) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(Math.round(x - 45), Math.round(GROUND_Y - 10), 90, 20);
        if (reflection > 0f) {
            java.awt.geom.AffineTransform saved = g.getTransform();
            g.translate(0, GROUND_Y);
            g.scale(1, -1);
            g.translate(0, -GROUND_Y);
            stick(g, x, footY, new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * reflection)));
            g.setTransform(saved);
        }
        stick(g, x, footY, color);
    }

    private static void stick(Graphics2D g, float x, float footY, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(22, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int fx = Math.round(x), fy = Math.round(footY);
        g.drawLine(fx, fy - 70, fx, fy - 150);
        g.drawLine(fx, fy - 70, fx - 30, fy);
        g.drawLine(fx, fy - 70, fx + 30, fy);
        g.drawLine(fx, fy - 140, fx + 45, fy - 110);
        g.fillOval(fx - 26, fy - 205, 52, 52);
    }

    // ---------------------------------------------------------------- modos

    private void runWindow() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D)graphics.create();
                render(g, getWidth(), getHeight());
                g.dispose();
            }
        };
        panel.setPreferredSize(new Dimension(1280, 720));
        JFrame frame = new JFrame("Cenário — " + scene.definition().name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        final long[] previous = {System.nanoTime()};
        new Timer(16, e -> {
            long now = System.nanoTime();
            update(Math.min(0.033f, (now - previous[0]) / 1_000_000_000f));
            previous[0] = now;
            panel.repaint();
        }).start();
    }

    private void runFrames(File outDir, float seconds) throws IOException {
        if (!outDir.isDirectory() && !outDir.mkdirs()) throw new IOException("não consegui criar " + outDir);
        float dt = 1f / 30f;
        int frames = Math.round(seconds / dt);
        for (int i = 0; i < frames; i++) {
            update(dt);
            BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            render(g, image.getWidth(), image.getHeight());
            g.dispose();
            ImageIO.write(image, "png", new File(outDir, String.format(Locale.ROOT, "quadro_%03d.png", i)));
        }
        System.out.println(frames + " quadros em " + outDir.getPath());
    }

    public static void main(String[] args) throws Exception {
        String folder = null;
        File framesDir = null;
        float seconds = 12f;
        for (int i = 0; i < args.length; i++) {
            if ("--quadros".equals(args[i]) && i + 1 < args.length) framesDir = new File(args[++i]);
            else if ("--segundos".equals(args[i]) && i + 1 < args.length) seconds = Float.parseFloat(args[++i]);
            else folder = args[i];
        }
        if (folder == null) {
            System.err.println("uso: StagePreview pasta-do-cenario [--quadros saida/] [--segundos 12]");
            System.exit(2);
        }
        File dir = new File(folder).getAbsoluteFile();
        StagePack pack = StagePack.load(new FileRenderAssets(dir.getParentFile()), dir.getName());
        for (String warning : pack.warnings) System.out.println("aviso: " + warning);
        StagePreview preview = new StagePreview(pack);
        if (framesDir != null) preview.runFrames(framesDir, seconds);
        else SwingUtilities.invokeLater(preview::runWindow);
    }
}
