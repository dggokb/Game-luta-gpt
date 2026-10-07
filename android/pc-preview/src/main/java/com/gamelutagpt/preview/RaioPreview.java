package com.gamelutagpt.preview;

import com.gamelutagpt.stage.StagePack;
import com.gamelutagpt.stage.StageScene;
import com.gamelutagpt.stage.StageWorld;
import com.gamelutagpt.ultra.RaioFinal;
import com.gamelutagpt.ultra.UltraDefinition;
import com.gamelutagpt.ultra.UltraPack;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Grava em PNG o raio final do ultra no cenário, com os sprites do jogo e o contador de
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
    private static final int CHARGE = 36, EXTEND = 8, INTERVAL = 4, GAP = 6, FADE = 18, BLAST_HITSTOP = 12;
    private static final float PUSH_PER_HIT = 7f, RECOIL_PER_HIT = 1.5f, STAND_HEIGHT = 226f;
    // Atlas gerados (GeneratedCharacters): largura, altura e raiz de cada quadro.
    private static final String SPRITES = "app/src/main/res/drawable-nodpi/";
    private static final int[] BEAM_ATLAS = {276, 253, 143, 244};
    private static final int[] HIT_ATLAS = {191, 241, 91, 232};

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
        BufferedImage beamSprite = ImageIO.read(new File(SPRITES + "player_base_ultra_beam.png"));
        BufferedImage hitSprite = ImageIO.read(new File(SPRITES + "player_base_hit_stand.png"));
        RaioFinal raio = new RaioFinal();
        RaioFinal.Frame f = new RaioFinal.Frame();
        UltraDefinition.Beam beam = pack.definition.beam;
        float[] chargeHands = beam.chargeHands != null ? beam.chargeHands : new float[]{30f, 120f};
        float[] fireHands = beam.fireHands != null ? beam.fireHands : new float[]{154f, 152f};

        int hits = beam.hits;
        int fireFrame = CHARGE;
        int blastFrame = CHARGE + EXTEND + INTERVAL * (hits - 1) + GAP;
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
                if (done < hits && beamFrame >= fireFrame + EXTEND + INTERVAL * done) {
                    done++;
                    combo++;
                    lastHit = clock;
                    dx += PUSH_PER_HIT;
                    ax -= RECOIL_PER_HIT;
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
                    dx += 840f * dt;
                }
            }
            clock += dt;
            if (blast < 0f) targetX = dx - 0.4f * 34f;
            if (i % 2 == 1) continue; // 30 quadros por segundo bastam

            scene.setCamera(ZOOM, left, top);
            scene.update(dt * 2f);
            BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.scale(image.getWidth() / VW, image.getHeight() / VH);
            Java2DRenderCanvas canvas = new Java2DRenderCanvas(g, image.getWidth(), image.getHeight());
            scene.drawBackground(canvas);

            AffineTransform saved = g.getTransform();
            g.scale(ZOOM, ZOOM);
            g.translate(-left, -top);
            int afterBlast = beamFrame - blastFrame;
            float strength = afterBlast <= 0 ? 1f : Math.max(0f, 1f - afterBlast / (float)FADE);
            float dark = beamFrame < fireFrame ? Math.min(1f, beamFrame / 12f) : strength;
            g.setColor(new Color(8, 6, 20, Math.round(120 * dark)));
            g.fillRect(-2000, -2000, 6000, 6000);

            f.bodyX = ax;
            f.groundY = GROUND_Y;
            f.bodyHeight = STAND_HEIGHT;
            f.chargeX = ax + chargeHands[0];
            f.chargeY = GROUND_Y - chargeHands[1];
            f.originX = ax + fireHands[0];
            f.originY = GROUND_Y - fireHands[1];
            f.targetX = targetX;
            f.facing = 1;
            f.time = clock;
            f.charge = Math.min(1f, beamFrame / (float)fireFrame);
            f.sinceFire = beamFrame >= fireFrame ? (beamFrame - fireFrame) * dt : -1f;
            float extend = Math.max(0f, Math.min(1f, (beamFrame - fireFrame) / (float)EXTEND));
            f.reach = 1f - (1f - extend) * (1f - extend);
            f.hits = done;
            f.sinceHit = lastHit >= 0f ? clock - lastHit : -1f;
            f.sinceBlast = blast >= 0f ? clock - blast : -1f;
            f.strength = strength;

            raio.drawBehind(canvas, pack, f);
            sprite(g, hitSprite, HIT_ATLAS, 4, blast >= 0f ? 3 : 1 + (done % 2), dx, dy, -1);
            sprite(g, beamSprite, BEAM_ATLAS, 9, RaioFinal.poseFrame(beamFrame, fireFrame, blastFrame, FADE), ax, GROUND_Y, 1);
            raio.drawFront(canvas, pack, f);
            if (blast >= 0f && clock - blast < 0.22f) {
                g.setColor(new Color(255, 250, 225, Math.round(230 * (1f - (clock - blast) / 0.22f))));
                g.fillRect(-2000, -2000, 6000, 6000);
            }
            g.setTransform(saved);
            scene.drawForeground(canvas);

            if (combo > 3 || beamFrame > 0) {
                float bump = lastHit >= 0f ? Math.max(0f, 1f - (clock - lastHit) / 0.12f) : 0f;
                if (blast >= 0f) bump = Math.max(bump, Math.max(0f, 1f - (clock - blast) / 0.12f));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(46f * (1f + 0.32f * bump))));
                g.setColor(new Color(255, 214 + Math.round(41f * bump), 92 + Math.round(163f * bump)));
                g.drawString(combo + " HITS", 40, 300);
            }
            g.dispose();
            ImageIO.write(image, "png", new File(out, String.format(Locale.ROOT, "quadro_%03d.png", i / 2)));
        }
        System.out.println("quadros em " + out.getPath());
    }

    /** Um quadro do atlas com a raiz em (x, chão), virado para {@code facing} (a arte olha para a direita). */
    private static void sprite(Graphics2D g, BufferedImage atlas, int[] cell, int columns, int frame, float x, float y,
                               int facing) {
        int w = cell[0], h = cell[1];
        int sx = (frame % columns) * w, sy = (frame / columns) * h;
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        if (facing < 0) g.scale(-1, 1);
        g.drawImage(atlas, -cell[2], -cell[3], w - cell[2], h - cell[3], sx, sy, sx + w, sy + h, null);
        g.setTransform(saved);
    }
}
