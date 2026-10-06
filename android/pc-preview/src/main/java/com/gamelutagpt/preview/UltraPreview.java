package com.gamelutagpt.preview;

import com.gamelutagpt.ultra.PaginaFinal;
import com.gamelutagpt.ultra.UltraGrade;
import com.gamelutagpt.ultra.UltraListener;
import com.gamelutagpt.ultra.UltraPack;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 * Visualizador da "Página Final" no PC.
 *
 * <pre>
 * Janela:   UltraPreview [pasta-do-ultra]
 *           clique ou espaço = toque, R = repetir
 * Quadros:  UltraPreview [pasta-do-ultra] --quadros saida/ [--toque perfeito|bom|cedo|nenhum]
 * </pre>
 *
 * Sem pasta, usa o ultra provisório (só arte desenhada por código).
 */
public final class UltraPreview {
    private static final float VW = PaginaFinal.VW;
    private static final float VH = PaginaFinal.VH;

    private final PaginaFinal pagina;
    private final UltraPack pack;
    private float damageDealt;
    private String status = "";

    private UltraPreview(UltraPack pack) {
        this.pack = pack;
        this.pagina = new PaginaFinal(new UltraListener() {
            @Override
            public void onUltraHit(int hitIndex, float damageFraction) {
                damageDealt += damageFraction;
                log("acerto " + hitIndex + ": " + Math.round(damageFraction * 100) + "% do dano");
            }

            @Override
            public void onUltraSound(String soundId) {
                log("som: " + soundId);
            }

            @Override
            public void onUltraFinished(UltraGrade grade) {
                status = String.format(Locale.ROOT, "fim: %s, dano total %.0f%%  (R repete)", grade, damageDealt * 100);
                log(status);
            }
        });
    }

    private void log(String message) {
        System.out.printf(Locale.ROOT, "[%5.2fs] %s%n", pagina.clock(), message);
    }

    private void restart() {
        damageDealt = 0f;
        status = "";
        pagina.start(pack, 1);
    }

    /** Fundo que imita a luta, para ver a página quebrando por cima dela. */
    private static void drawFakeGame(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(21, 55, 103), 0, VH, new Color(240, 171, 99)));
        g.fillRect(0, 0, (int)VW, (int)VH);
        g.setColor(new Color(60, 90, 70));
        g.fillRect(0, 565, (int)VW, (int)VH - 565);
        g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(244, 183, 59));
        g.drawLine(420, 470, 420, 520);
        g.fillOval(395, 415, 50, 50);
        g.setColor(new Color(74, 205, 232));
        g.drawLine(860, 470, 860, 520);
        g.fillOval(835, 415, 50, 50);
    }

    private void renderTo(Graphics2D g, int width, int height) {
        g.scale(width / VW, height / VH);
        drawFakeGame(g);
        pagina.render(new Java2DRenderCanvas(g, width, height));
    }

    // ---------------------------------------------------------------- modos

    private void runWindow() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D)graphics.create();
                renderTo(g, getWidth(), getHeight());
                g.dispose();
                if (!pagina.isActive()) {
                    graphics.setColor(Color.WHITE);
                    graphics.drawString(status.isEmpty() ? "R para tocar" : status, 16, 24);
                }
            }
        };
        panel.setPreferredSize(new Dimension(1280, 720));
        panel.setFocusable(true);
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pagina.tap();
            }
        });
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) pagina.tap();
                if (e.getKeyCode() == KeyEvent.VK_R) restart();
            }
        });

        JFrame frame = new JFrame("Página Final — " + pack.definition.name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();

        final long[] previous = {System.nanoTime()};
        new Timer(16, e -> {
            long now = System.nanoTime();
            float dt = Math.min(0.033f, (now - previous[0]) / 1_000_000_000f);
            previous[0] = now;
            pagina.update(dt);
            panel.repaint();
        }).start();
        restart();
    }

    private void runFrames(File outDir, String tapMode) throws IOException {
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            throw new IOException("não consegui criar " + outDir);
        }
        float tapAt = PaginaFinal.TARGET_AT;
        if ("bom".equals(tapMode)) tapAt += 0.12f;
        if ("cedo".equals(tapMode)) tapAt -= 0.40f;
        boolean tapped = "nenhum".equals(tapMode);

        final float dt = 1f / 30f;
        int width = 960;
        int height = 540;
        restart();
        int index = 0;
        while (pagina.isActive() && index < 600) {
            pagina.update(dt);
            if (!tapped && pagina.clock() >= tapAt) {
                pagina.tap();
                tapped = true;
            }
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            renderTo(g, width, height);
            g.dispose();
            ImageIO.write(image, "png", new File(outDir, String.format(Locale.ROOT, "quadro_%03d.png", index++)));
        }
        System.out.println(index + " quadros em " + outDir.getPath());
    }

    public static void main(String[] args) throws Exception {
        String folder = null;
        File framesDir = null;
        String tapMode = "perfeito";
        for (int i = 0; i < args.length; i++) {
            if ("--quadros".equals(args[i]) && i + 1 < args.length) framesDir = new File(args[++i]);
            else if ("--toque".equals(args[i]) && i + 1 < args.length) tapMode = args[++i];
            else folder = args[i];
        }

        UltraPack pack;
        if (folder == null) {
            pack = UltraPack.placeholder("EXPLOSÃO SOLAR", 0xFFF4B73B);
        } else {
            File dir = new File(folder).getAbsoluteFile();
            pack = UltraPack.load(new FileRenderAssets(dir.getParentFile()), dir.getName());
            for (String warning : pack.warnings) System.out.println("aviso: " + warning);
        }

        UltraPreview preview = new UltraPreview(pack);
        if (framesDir != null) {
            preview.runFrames(framesDir, tapMode);
        } else {
            SwingUtilities.invokeLater(preview::runWindow);
        }
    }
}
