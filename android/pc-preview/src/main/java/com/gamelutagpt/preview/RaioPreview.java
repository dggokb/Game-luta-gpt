package com.gamelutagpt.preview;

import com.gamelutagpt.stage.StagePack;
import com.gamelutagpt.stage.StageScene;
import com.gamelutagpt.stage.StageWorld;
import com.gamelutagpt.ultra.RaioFinal;
import com.gamelutagpt.ultra.UltraPack;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Grava em PNG o raio final do ultra no cenário, com lutadores falsos e o contador de
 * hits, no mesmo ritmo do motor (valores padrão do CombatConfig).
 *
 * <pre>
 * RaioPreview pasta-do-ultra pasta-do-cenario --quadros saida/
 * </pre>
 */
public final class RaioPreview {
    private static final float GROUND_Y = 565f;
    private static final float VW = StageWorld.VW;
    private static final float VH = StageWorld.VH;
    private static final float ZOOM = 1.12f;
    // CombatConfig: ultraBeam*.
    private static final int EXTEND = 8, INTERVAL = 4, GAP = 6, FADE = 18, BLAST_HITSTOP = 12;
    private static final float PUSH_PER_HIT = 7f, STAND_HEIGHT = 226f;

    public static void main(String[] args) throws IOException {
        if (args.length != 4 || !"--quadros".equals(args[2])) {
            System.err.println("uso: RaioPreview pasta-do-ultra pasta-do-cenario --quadros saida/");
            System.exit(2);
        }
        File ultraDir = new File(args[0]).getAbsoluteFile();
        File stageDir = new File(args[1]).getAbsoluteFile();
        File out = new File(args[3]);
        if (!out.isDirectory() && !out.mkdirs()) throw new IOException("não consegui criar " + out);

        UltraPack pack = UltraPack.load(new FileRenderAssets(ultraDir.getParentFile()), ultraDir.getName());
        StagePack stagePack = StagePack.load(new FileRenderAssets(stageDir.getParentFile()), stageDir.getName());
        for (String warning : pack.warnings) System.out.println("aviso: " + warning);
        StageScene scene = new StageScene(stagePack, new StageWorld(GROUND_Y, 2600f, ZOOM, 700f, 552f));
        RaioFinal raio = new RaioFinal();
        RaioFinal.Frame f = new RaioFinal.Frame();

        int hits = pack.definition.beam.hits;
        int blastFrame = EXTEND + INTERVAL * (hits - 1) + GAP;
        float ax = 380f, dx = 900f, dy = GROUND_Y, dvy = 0f;
        float left = 640f - VW / ZOOM / 2f, top = GROUND_Y - 552f / ZOOM;
        int beamFrame = 0, hitstop = 0, done = 0, combo = 3;
        float clock = 0f, lastHit = -1f, blast = -1f, targetX = 0f;
        float dt = 1f / 60f;
        int total = blastFrame + FADE + BLAST_HITSTOP + 20;
        for (int i = 0; i < total; i++) {
            // Mesmo passo do motor: acertos por contagem, pausa de impacto na explosão.
            if (hitstop > 0) {
                hitstop--;
            } else {
                beamFrame++;
                if (done < hits && beamFrame >= EXTEND + INTERVAL * done) {
                    done++;
                    combo++;
                    lastHit = clock;
                    dx += PUSH_PER_HIT;
                } else if (done == hits && beamFrame >= blastFrame) {
                    done++;
                    combo++;
                    blast = clock;
                    hitstop = BLAST_HITSTOP;
                    dvy = -1150f;
                }
                if (blast >= 0f && hitstop == 0) {
                    dvy += 3600f * dt;
                    dy = Math.min(GROUND_Y, dy + dvy * dt);
                    dx += 420f * dt * 2f;
                }
            }
            clock += dt;
            if (blast < 0f) targetX = dx - 0.4f * 40f;
            if (i % 2 == 1) continue; // 30 quadros por segundo bastam

            scene.setCamera(ZOOM, left, top);
            scene.update(dt * 2f);
            BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.scale(image.getWidth() / VW, image.getHeight() / VH);
            Java2DRenderCanvas canvas = new Java2DRenderCanvas(g, image.getWidth(), image.getHeight());
            scene.drawBackground(canvas);

            AffineTransform saved = g.getTransform();
            g.scale(ZOOM, ZOOM);
            g.translate(-left, -top);
            int afterBlast = beamFrame - blastFrame;
            float strength = afterBlast <= 0 ? 1f : Math.max(0f, 1f - afterBlast / (float)FADE);
            g.setColor(new Color(8, 6, 20, Math.round(110 * strength)));
            g.fillRect(-2000, -2000, 6000, 6000);
            stick(g, ax, GROUND_Y, new Color(244, 183, 59), true);
            stick(g, dx, dy, new Color(74, 205, 232), false);
            if (strength > 0f || blast >= 0f && clock - blast < RaioFinal.BLAST_DURATION) {
                f.originX = ax + STAND_HEIGHT * 0.68f;
                f.originY = GROUND_Y - STAND_HEIGHT * 0.67f;
                f.targetX = targetX;
                f.facing = 1;
                f.time = clock;
                float extend = Math.min(1f, beamFrame / (float)EXTEND);
                f.reach = 1f - (1f - extend) * (1f - extend);
                f.hits = done;
                f.sinceHit = lastHit >= 0f ? clock - lastHit : -1f;
                f.sinceBlast = blast >= 0f ? clock - blast : -1f;
                f.strength = strength;
                raio.draw(canvas, pack, f);
            }
            if (blast >= 0f && clock - blast < 0.22f) {
                g.setColor(new Color(255, 250, 225, Math.round(230 * (1f - (clock - blast) / 0.22f))));
                g.fillRect(-2000, -2000, 6000, 6000);
            }
            g.setTransform(saved);
            scene.drawForeground(canvas);

            float bump = lastHit >= 0f ? Math.max(0f, 1f - (clock - lastHit) / 0.12f) : 0f;
            if (blast >= 0f) bump = Math.max(bump, Math.max(0f, 1f - (clock - blast) / 0.12f));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(46f * (1f + 0.32f * bump))));
            g.setColor(new Color(255, 214 + Math.round(41f * bump), 92 + Math.round(163f * bump)));
            g.drawString(combo + " HITS", 40, 300);
            g.dispose();
            ImageIO.write(image, "png", new File(out, String.format(Locale.ROOT, "quadro_%03d.png", i / 2)));
        }
        System.out.println("quadros em " + out.getPath());
    }

    private static void stick(Graphics2D g, float x, float footY, Color color, boolean firing) {
        g.setColor(color);
        g.setStroke(new BasicStroke(22, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int fx = Math.round(x), fy = Math.round(footY);
        g.drawLine(fx, fy - 70, fx, fy - 150);
        g.drawLine(fx, fy - 70, fx - 30, fy);
        g.drawLine(fx, fy - 70, fx + 30, fy);
        if (firing) {
            g.drawLine(fx, fy - 140, Math.round(x + STAND_HEIGHT * 0.66f), Math.round(fy - STAND_HEIGHT * 0.67f));
        } else {
            g.drawLine(fx, fy - 140, fx + 45, fy - 110);
        }
        g.fillOval(fx - 26, fy - 205, 52, 52);
    }
}
