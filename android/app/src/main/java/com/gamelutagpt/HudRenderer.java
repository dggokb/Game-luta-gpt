package com.gamelutagpt;

import static com.gamelutagpt.ControlsLayout.*;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.RadialGradient;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.content.Context;
import java.io.InputStream;
import java.io.IOException;

/**
 * Screen-space HUD and touch controls plus the opponent's overhead panel. It reads
 * everything through {@link State}; it never changes game state.
 */
final class HudRenderer {
    /** Read-only view of what the HUD shows. */
    interface State {
        FighterState active();
        FighterState reserve();
        FighterState opponent();
        CharacterDefinition opponentReserveCharacter();
        /** O parceiro do CPU no 2x2 (com a vida dele), ou null no treino. */
        default FighterState opponentReserve() { return null; }
        /** Modo batalha (IA ON): a luta de verdade, 2x2. */
        default boolean battleActive() { return false; }
        /** Relógio no centro do topo: os segundos da batalha, ou ∞ (treino, cena da história). */
        default String clockLabel() { return "\u221E"; }
        /** Últimos segundos da batalha: o relógio fica vermelho e pulsa. */
        default boolean clockUrgent() { return false; }
        /** Assist do CPU no 2x2: 0 em uso, subindo até 1 quando está pronto. */
        default float opponentTagReadyRatio() { return 0f; }
        String versionLabel();
        String stateLabel();
        int facing();
        boolean aiEnabled();
        boolean debugEnabled();
        /** The pair on the player's side, as "p01+p03". */
        String teamLabel();
        /** 0 while tagging, rising to 1 when the tag is ready. */
        float tagReadyRatio();
        default int presentationEpoch() { return 0; }
        default boolean assistInUse() { return false; }
        String tagCooldownLabel();
        String tagButtonLabel();
        /** Text of the TAG button while it can be used: ASSIST, or TROCA during Assist → Tag. */
        String tagButtonTitle();
        boolean canTag();
        boolean canSuper();
        /** ↓ held with three bars: the SUPER button would start the ultra. */
        boolean ultraReady();
        int dpadDirection();
        boolean pressed(Control control);
        /** A luta atual faz parte de um episódio do modo história. */
        default boolean storyActive() { return false; }
        /** O treino guiado dos golpes (modo tutorial) está rodando. */
        default boolean tutorialActive() { return false; }
        /** Tutorial ou luta com conversa: o canto dos botões de teste é dos quadros laterais. */
        default boolean hideTestButtons() { return tutorialActive(); }
        /** Botão do topo da luta: a seleção no modo teste, o MENU (pausa) no modo história. */
        default String selectButtonLabel() { return "◀ SELEÇÃO"; }
        /** Botão do motor de animação (nível ou OFF), ou null para não mostrar. */
        default String animacaoLabel() { return null; }
        /** Rótulo do botão {@code i} da coluna de DESEMPENHO (null: a coluna não aparece). */
        default String perfLabel(int i) { return null; }
        default boolean perfOn(int i) { return true; }
        default boolean animacaoLigada() { return false; }
        /** Quadros que o SHINKAI dura (a barra ativa esvazia nesse tempo). */
        default int shinkaiDuration() { return 480; }
        /** Demo playing (index into DemoDirector.VERSIONS), or -1. */
        int demoPlaying();
        String demoTitle();
        String demoCaption();
    }

    /** Values of the overhead panel above the opponent (world space). */
    static final class OpponentPanel {
        float x;
        float visualTop;
        float lifeRatio;
        String status;
        String lifeLabel;
        String superLabel;
        String damageLabel;
        /** 1 right after a hit, fading to 0. */
        float damageProgress;
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap hudArt;
    private final Rect artSource = new Rect();
    private final RectF artTarget = new RectF();

    /** Decode the ornamental atlas once; life, portraits and meters remain live. */
    void loadArt(Context context) {
        try (InputStream input = context.getAssets().open("ui/hud/energy-frame.png")) {
            hudArt = BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            android.util.Log.w("HudRenderer", "Energy artwork unavailable; using vector HUD", e);
        }
    }

    private void artwork(Canvas c, int l, int t, int r, int b,
                         float x, float y, float w, float h) {
        artSource.set(l, t, r, b);
        artTarget.set(x, y, x + w, y + h);
        c.drawBitmap(artePara(c), artSource, artTarget, imagePaint);
    }

    /** A moldura na placa de vídeo (feita na primeira vez que a tela é da GPU). */
    private Bitmap hudArtPlaca;

    /**
     * A moldura do HUD cobre boa parte do alto da tela em todo quadro: na tela da GPU vai uma
     * cópia na placa de vídeo (sem a cópia para a placa a cada vez que ela sai do cache).
     */
    private Bitmap artePara(Canvas c) {
        if (!c.isHardwareAccelerated() || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return hudArt;
        if (hudArtPlaca == null) {
            try {
                hudArtPlaca = hudArt.copy(Bitmap.Config.HARDWARE, false);
            } catch (RuntimeException e) {
                hudArtPlaca = null;
            }
            if (hudArtPlaca == null) hudArtPlaca = hudArt;
        }
        return hudArtPlaca;
    }

    private void reset() {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    final HudEffects effects = new HudEffects();
    void resetEffects() { effects.reset(); }

    void drawHud(Canvas c, State s, CharacterSelectRenderer portraits) {
        drawHudAt(c, s, portraits, System.nanoTime() / 1e9);
    }

    /** Same production renderer with a deterministic clock for native Canvas review. */
    void drawHudAt(Canvas c, State s, CharacterSelectRenderer portraits, double seconds) {
        effects.update(s, seconds);
        shinkaiDuration = s.shinkaiDuration();
        reset();
        FighterState active = s.active();
        // Sem parceiro (duelo 1x1), reserve() devolve o próprio titular: o HUD mostra a vaga vazia.
        FighterState reserve = s.reserve() == active ? null : s.reserve();

        if (hudArt == null) {
            drawTeam(c, active, reserve, null, false, s.tagReadyRatio(), portraits);
            drawTeam(c, s.opponent(), s.opponentReserve(), s.opponentReserveCharacter(), true, s.opponentTagReadyRatio(), portraits);
            drawPower(c, active, false);
            drawPower(c, s.opponent(), true);
        } else {
            drawArtTeam(c, active, reserve, null, false, s.tagReadyRatio(), portraits);
            drawArtTeam(c, s.opponent(), s.opponentReserve(), s.opponentReserveCharacter(), true, s.opponentTagReadyRatio(), portraits);
            artwork(c, 0, 0, 1672, 280, 0f, 0f, 1280f, 170f);
            // Entre as vidas: a barra do SHINKAI de quem está em campo (o assist foi para o círculo).
            drawShinkaiBar(c, active, false, 174f, 96f, 330f, 11f, shinkaiDuration);
            drawShinkaiBar(c, s.opponent(), true, 174f, 96f, 330f, 11f, shinkaiDuration);
            drawArtPower(c, active, false);
            drawArtPower(c, s.opponent(), true);
            text(c, "PLAYER 1", 220f, 65f, 15f, Color.WHITE);
            text(c, "PLAYER 2", 1060f, 65f, 15f, Color.WHITE);
            text(c, "TAG", 222f, 132f, 13f, Color.WHITE);
            text(c, s.opponentReserve() == null ? "TAG OFF" : "TAG", 1058f, 132f, 13f, 0xFFBCC4D8);
        }
        drawFrameEffects(c);
        // Batalha: 120 segundos; treino e cenas da história: ∞ (nunca um relógio inventado).
        if (hudArt == null) panel(c, 588f, 6f, 104f, 93f, 18f, 0xEF070B19, Color.WHITE, 2f);
        drawClock(c, s);
        text(c, s.battleActive() ? "BATALHA" : "TREINO", 640f, 87f, 12f, 0xFFB9C7E5);
        if (s.debugEnabled()) {
            text(c, s.versionLabel() + " • " + s.stateLabel(), 640f, 194f, 14f, Color.WHITE);
        }
        // No tutorial e nas lutas com conversa os botões de teste somem (o canto é dos quadros).
        if (!s.hideTestButtons()) drawTestButtons(c, s);
        drawToggle(c, s.pressed(Control.CHARACTER_SELECT), s.selectButtonLabel(),
            SELECT_BUTTON_LEFT, SELECT_BUTTON_TOP, SELECT_BUTTON_RIGHT, SELECT_BUTTON_BOTTOM, 17f);
    }

    /** Relógio da luta; nos últimos segundos fica vermelho e pulsa uma vez por segundo. */
    private void drawClock(Canvas c, State s) {
        String label = s.clockLabel();
        // Três algarismos (120 no começo) ficam um pouco menores para caber entre as barras.
        float size = label.length() >= 3 ? 48f : 58f;
        if (!s.clockUrgent()) {
            text(c, label, 640f, 70f, size, Color.WHITE);
            return;
        }
        // Pulso a cada segundo: cresce e volta, sem mudar o centro.
        float beat = (float)(effects.now - Math.floor(effects.now));
        float pulse = 1f + 0.18f * Math.max(0f, 1f - beat * 4f);
        c.save();
        c.translate(640f, 52f);
        c.scale(pulse, pulse);
        c.translate(-640f, -52f);
        text(c, label, 640f, 70f, size, RED);
        c.restore();
    }

    private void drawTestButtons(Canvas c, State s) {
        drawToggle(c, s.aiEnabled(), s.aiEnabled() ? "IA ON" : "IA OFF",
            AI_BUTTON_LEFT, AI_BUTTON_TOP, AI_BUTTON_RIGHT, AI_BUTTON_BOTTOM, 19f);
        drawToggle(c, s.debugEnabled(), s.debugEnabled() ? "DEBUG ON" : "DEBUG OFF",
            DEBUG_BUTTON_LEFT, DEBUG_BUTTON_TOP, DEBUG_BUTTON_RIGHT, DEBUG_BUTTON_BOTTOM, 16f);
        drawToggle(c, s.pressed(Control.HEAL_PLAYER), "VIDA P1",
            HEAL_PLAYER_LEFT, HEAL_BUTTON_TOP, HEAL_PLAYER_RIGHT, HEAL_BUTTON_BOTTOM, 14f);
        drawToggle(c, s.pressed(Control.HEAL_OPPONENT), "VIDA CPU",
            HEAL_OPPONENT_LEFT, HEAL_BUTTON_TOP, HEAL_OPPONENT_RIGHT, HEAL_BUTTON_BOTTOM, 14f);
        drawToggle(c, s.pressed(Control.TEAM_SELECT), "DUPLA " + s.teamLabel(),
            TEAM_BUTTON_LEFT, TEAM_BUTTON_TOP, TEAM_BUTTON_RIGHT, TEAM_BUTTON_BOTTOM, 14f);
        drawToggle(c, s.storyActive(), "HISTÓRIA",
            STORY_BUTTON_LEFT, STORY_BUTTON_TOP, STORY_BUTTON_RIGHT, STORY_BUTTON_BOTTOM, 16f);
        drawToggle(c, s.tutorialActive(), "TUTORIAL",
            TUTORIAL_BUTTON_LEFT, TUTORIAL_BUTTON_TOP, TUTORIAL_BUTTON_RIGHT, TUTORIAL_BUTTON_BOTTOM, 13f);
        if (s.animacaoLabel() != null) drawToggle(c, s.animacaoLigada(), s.animacaoLabel(),
            ANIM_BUTTON_LEFT, ANIM_BUTTON_TOP, ANIM_BUTTON_RIGHT, ANIM_BUTTON_BOTTOM, 13f);
        for (int i = 0; i < ControlsLayout.PERF_CONTROLS.length; i++) {
            String label = s.perfLabel(i);
            if (label == null) continue;
            float top = ControlsLayout.PERF_TOP[i];
            drawToggle(c, s.perfOn(i), label, ControlsLayout.PERF_LEFT, top, ControlsLayout.PERF_RIGHT,
                top + ControlsLayout.PERF_HEIGHTS[i], 13f);
        }
    }

    /** Só os botões (de teste e o do topo), para quando o HUD está desligado no teste de desempenho. */
    void drawButtonsOnly(Canvas c, State s) {
        if (!s.hideTestButtons()) drawTestButtons(c, s);
        drawToggle(c, s.pressed(Control.CHARACTER_SELECT), s.selectButtonLabel(),
            SELECT_BUTTON_LEFT, SELECT_BUTTON_TOP, SELECT_BUTTON_RIGHT, SELECT_BUTTON_BOTTOM, 17f);
    }


    private static final int BLUE = 0xFF22BFFF, RED = 0xFFFF354D;
    private final Path shape = new Path();
    private final RectF portraitBox = new RectF();
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Shader pointLifeGradient = new Degrade.Linear(0f, 63f, 0f, 89f,
        new int[]{0xFFFFFF9C, 0xFF7BFF22, 0xFF0ADF49}, null, Shader.TileMode.CLAMP);
    private final Shader reserveLifeGradient = new Degrade.Linear(0f, 119f, 0f, 136f,
        new int[]{0xFFFFFF9C, 0xFF7BFF22, 0xFF0ADF49}, null, Shader.TileMode.CLAMP);
    private static final String[] LEVELS = {"0", "1", "2", "3", "4", "5"};

    private void drawArtTeam(Canvas c, FighterState point, FighterState reserve,
                             CharacterDefinition absent, boolean right, float ready,
                             CharacterSelectRenderer portraits) {
        c.save();
        if (right) { c.translate(Arena.VW, 0f); c.scale(-1f, 1f); }
        // All coordinates are calibrated against the ornament atlas, not its bounding box.
        HudEffects.Meter hitFx = right ? effects.cpu : effects.point;
        float hurt = effects.fade(hitFx.hitAt, .32f);
        float shake = hurt * 5f * (float)Math.sin(effects.now * 95.0);
        portrait(c, portraits, point.character, 43f + shake, 24f, 119f, 86f, right ? RED : BLUE);
        if (hurt > 0f) {
            // Red flash on the face that just took the hit (same portrait outline).
            fxPaint.setColor(0xFFFF2A3A);
            fxPaint.setAlpha(Math.round(120 * hurt));
            c.drawPath(shape, fxPaint);
            fxPaint.setAlpha(255);
        }
        CharacterDefinition partner = reserve == null ? absent : reserve.character;
        if (partner != null) portrait(c, portraits, partner, 120f, 110f, 65f, 25f, right ? RED : BLUE);
        energyLife(c, point, 165f, 74f, 389f, 18f);
        if (reserve != null) energyLife(c, reserve, 259f, 119f, 182f, 10f);
        else panel(c, 259f, 119f, 182f, 10f, 6f, 0xFF161B28, 0xFF596072, 1f);
        paint.setColor(right ? RED : BLUE);
        c.drawCircle(554f, 115f, 8f, paint);
        paint.setColor(reserve != null && ready >= 1f ? Color.WHITE : 0xFF111723);
        c.drawCircle(554f, 115f, 4f, paint);
        if (reserve != null) assistRing(c, 554f, 115f, 13f, ready, right ? RED : BLUE);
        c.restore();
    }

    private void drawArtPower(Canvas c, FighterState fighter, boolean right) {
        c.save();
        if (right) { c.translate(Arena.VW, 0f); c.scale(-1f, 1f); }
        // Reuse the blue/red sides independently so the meter stays compact like the reference.
        if (right) {
            c.save(); c.translate(400f, 0f); c.scale(-1f, 1f);
            artwork(c, 922, 735, 1672, 941, 0f, 625f, 400f, 95f);
            c.restore();
        } else artwork(c, 0, 735, 750, 941, 0f, 625f, 400f, 95f);
        // Replace only the obsolete four inner cells. The outer energy frame stays.
        panel(c, 86, 675, 240, 23, 7, 0xFF070E20, right ? RED : BLUE, 2);
        int count = CombatConfig.MAX_METER / CombatConfig.METER_PER_BAR;
        float step = 225f / count, width = step - 5;
        HudEffects.Meter fx = right ? effects.cpu : effects.point;
        float bars = fighter.superBars();
        for (int i = 0; i < count; i++) {
            float x = 94 + i * step;
            panel(c, x, 679, width, 15, 5, 0xFF070E20, right ? RED : BLUE, 1.5f);
            c.save(); c.clipPath(shape);
            float fill = Arena.clamp(bars - i, 0, 1);
            paint.setColor(right ? RED : BLUE);
            c.drawRect(x, 679, x + width * fill, 694, paint);
            paint.setColor(0xBBFFFFFF);
            c.drawRect(x, 680, x + width * fill, 683, paint);
            float gain = effects.fade(fx.levelAt, HudEffects.LEVEL_FLASH);
            float spend = Math.max(effects.fade(fx.spendAt, HudEffects.SPEND_FLASH), effects.superBlink(fx));
            if (i >= fx.gainFrom && i < fx.gainTo && gain > 0) {
                fxPaint.setColor(Color.WHITE); fxPaint.setAlpha(Math.round(190 * gain));
                c.drawRect(x, 679, x + width, 694, fxPaint);
            }
            if (spend > 0) {
                float lo = Arena.clamp(fx.spentFrom / (float)CombatConfig.METER_PER_BAR - i, 0, 1);
                float hi = Arena.clamp(fx.spentTo / (float)CombatConfig.METER_PER_BAR - i, 0, 1);
                fxPaint.setColor(0xFFFFD698); fxPaint.setAlpha(Math.round(210 * spend));
                c.drawRect(x + width * lo, 679, x + width * hi, 694, fxPaint);
            }
            if (fill > 0f) {
                // Energy flowing through every charged cell (light cyan/pink band, never white-out).
                float flow = (float)((effects.now * .9 + i * .17) % 1.0);
                float fx0 = x + (width + 30f) * flow - 30f;
                fxPaint.setColor(right ? 0xFFFFC2CC : 0xFFBDF2FF);
                fxPaint.setAlpha(fill >= 1f ? 105 : 70);
                c.drawRect(Math.max(x, fx0), 679, Math.min(x + width * fill, fx0 + 22f), 694, fxPaint);
            }
            fxPaint.setAlpha(255); c.restore();
        }
        if (fighter.superMeter >= CombatConfig.MAX_METER)
            energyEdge(c, 87, 676, 238, 21, right ? RED : BLUE, 1);
        c.restore();
        int level = Math.max(0, Math.min(5, fighter.superMeter / CombatConfig.METER_PER_BAR));
        float pop = effects.fade(fx.levelAt, HudEffects.LEVEL_FLASH);
        float levelX = right ? Arena.VW - 51f : 51f;
        drawLevelFire(c, levelX, 684f, level, right, 1f);
        if (level > 0) {
            // Charged level: soft halo behind the digit, brighter when it just went up.
            fxPaint.setColor(right ? RED : BLUE);
            fxPaint.setAlpha(Math.round(70 + 120 * pop + (level == 5 ? 40 * (float)Math.sin(effects.now * 6) + 40 : 0)));
            c.drawCircle(levelX, 686f, 20f + 8f * pop, fxPaint);
            fxPaint.setAlpha(255);
        }
        text(c, LEVELS[level], levelX, 698f + 4f * pop, 38f * (1f + .35f * pop), pop > 0f ? 0xFFFFF3B0 : Color.WHITE);
        text(c, "SUPER", right ? Arena.VW - 348f : 348f, 702f, 17f, Color.WHITE);
    }

    /** Reused path and paint: the ornamental frame allocates no per-frame geometry. */
    private void panel(Canvas c, float x, float y, float w, float h, float bevel,
                       int fill, int stroke, float width) {
        shape.reset();
        shape.moveTo(x + bevel, y);
        shape.lineTo(x + w - bevel, y);
        shape.lineTo(x + w, y + h);
        shape.lineTo(x, y + h);
        shape.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        c.drawPath(shape, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(stroke);
        c.drawPath(shape, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void text(Canvas c, String value, float x, float y, float size, int color) {
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSkewX(-0.16f);
        paint.setTextSize(size);
        paint.setColor(0xFF050813);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        c.drawText(value, x, y, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawText(value, x, y, paint);
        paint.setTextSkewX(0f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawTeam(Canvas c, FighterState point, FighterState reserve,
                          CharacterDefinition absentPartner, boolean right, float ready,
                          CharacterSelectRenderer portraits) {
        int accent = right ? RED : BLUE;
        c.save();
        if (right) { c.translate(Arena.VW, 0f); c.scale(-1f, 1f); }
        // Angular energy strokes radiate from the portrait towards the center.
        for (int i = 0; i < 12; i++) {
            float y = 6f + i * 11f;
            paint.setStrokeWidth(i % 3 == 0 ? 7f : 3f);
            paint.setColor((i % 3 == 0 ? 0x99000000 : 0x55000000) | (accent & 0xFFFFFF));
            c.drawLine(5f, y - 23f, 157f + (i % 4) * 22f, y + 18f, paint);
            paint.setColor(0xBFFFFFFF);
            paint.setStrokeWidth(1.3f);
            c.drawLine(8f, y - 21f, 93f + (i % 4) * 13f, y + 5f, paint);
        }
        panel(c, 82f, 30f, 503f, 65f, 28f, 0xEF060B19, accent, 5f);
        panel(c, 165f, 31f, 393f, 27f, 15f, 0xE908101E, 0xFFDDE8FF, 1.5f);
        energyLife(c, point, 109f, 63f, 474f, 26f);
        portrait(c, portraits, point.character, 8f, 1f, 98f, 103f, accent);
        panel(c, 83f, 114f, 394f, 31f, 16f, 0xEB070D1B, accent, 3f);
        CharacterDefinition partner = reserve == null ? absentPartner : reserve.character;
        if (partner != null) portrait(c, portraits, partner, 88f, 114f, 74f, 36f, accent);
        if (reserve != null) {
            energyLife(c, reserve, 258f, 119f, 205f, 17f);
        } else {
            panel(c, 258f, 119f, 205f, 17f, 8f, 0xFF1A1F2B, 0xFF626A80, 1f);
        }
        paint.setColor(0xFF080D19);
        c.drawCircle(551f, 109f, 9f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(accent);
        paint.setStrokeWidth(2f);
        c.drawCircle(551f, 109f, 9f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(reserve != null && ready >= 1f ? Color.WHITE : 0xFF454B5A);
        c.drawCircle(551f, 109f, 4f, paint);
        if (reserve != null) assistRing(c, 551f, 109f, 14f, ready, accent);
        c.restore();
        drawShinkaiBar(c, point, right, 181f, 98f, 319f, 12f, shinkaiDuration);
        // Text stays readable when the frame is mirrored.
        float labelX = right ? Arena.VW - 353f : 353f;
        text(c, point.hudTitle, labelX, 51f, 16f, Color.WHITE);
        text(c, reserve == null ? "TAG OFF" : "TAG", right ? Arena.VW - 209f : 209f, 133f, 14f,
            reserve == null ? 0xFF8991A5 : Color.WHITE);
    }

    private void portrait(Canvas c, CharacterSelectRenderer portraits, CharacterDefinition character,
                          float x, float y, float w, float h, int accent) {
        panel(c, x, y, w, h, 12f, 0xEE0A1122, accent, 2f);
        Bitmap bitmap = portraits.hudPortrait(character);
        if (bitmap == null) return;
        c.save();
        c.clipPath(shape);
        // Center-crop the face close-up from the original Drive concept sheet.
        float scale = Math.max(w / bitmap.getWidth(), h / bitmap.getHeight());
        float drawW = bitmap.getWidth() * scale;
        float drawH = bitmap.getHeight() * scale;
        portraitBox.set(x + (w - drawW) / 2f, y + (h - drawH) / 2f,
            x + (w + drawW) / 2f, y + (h + drawH) / 2f);
        c.drawBitmap(bitmap, null, portraitBox, imagePaint);
        c.restore();
    }

    private void energyLife(Canvas c, FighterState fighter, float x, float y, float w, float h) {
        float life = Arena.clamp(fighter.life / (float)fighter.profile.maxLife, 0f, 1f);
        float recoverable = Arena.clamp(fighter.recoverableLife / (float)fighter.profile.maxLife, 0f, 1f - life);
        panel(c, x, y, w, h, 15f, 0xFF171E2B, Color.WHITE, 2f);
        c.save();
        c.clipPath(shape);
        if (recoverable > 0f) {
            paint.setColor(0xFFE63D54);
            c.drawRect(x + w * life, y, x + w * (life + recoverable), y + h, paint);
        }
        paint.setShader(y < 100f ? pointLifeGradient : reserveLifeGradient);
        c.drawRect(x, y, x + w * life, y + h, paint);
        paint.setShader(null);
        HudEffects.Meter fx = fighter == effects.point.fighter ? effects.point
            : fighter == effects.cpu.fighter ? effects.cpu : effects.reserve;
        if (life > 0f && life <= .25f) {
            // Danger: the remaining life burns red-orange and pulses.
            paint.setColor(0xFFFF2A1A);
            paint.setAlpha(Math.round(185 + 55 * (float)Math.sin(effects.now * 6.0)));
            c.drawRect(x, y, x + w * life, y + h, paint);
            paint.setAlpha(255);
        }
        paint.setColor(0x99FFFFFF);
        c.drawRect(x, y + 2f, x + w * life, y + 4f, paint);
        sheen(c, x, y, w * life, h, (float)(effects.now * .42 + (y < 100f ? 0 : .37)));
        if (hudArt == null) drawLifeEffects(c, fx, x, y, w, h);
        c.restore();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2f);
        c.drawPath(shape, paint);
        float flash = effects.fade(fx.hitAt, HudEffects.HIT_FLASH);
        float critical = fx.life > 0 && fx.life <= .25f ? .18f + .12f * (float)Math.sin(effects.now * 4) : 0;
        paint.setColor(flash > 0 ? 0xFFFFE5A0 : RED);
        paint.setAlpha(Math.round(255 * Math.max(flash, critical)));
        c.drawPath(shape, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Slanted light band sweeping across a filled region; {@code cycle} advances 1 per sweep. */
    private void sheen(Canvas c, float x, float y, float w, float h, float cycle) {
        if (w <= 2f) return;
        float t = cycle - (float)Math.floor(cycle);
        // Visible for the first 45% of each cycle, then a pause.
        if (t > .45f) return;
        float k = t / .45f;
        float band = Math.max(18f, h * 1.6f);
        float bx = x - band + (w + band * 2f) * k;
        c.save();
        c.clipRect(x, y, x + w, y + h);
        shine.reset();
        shine.moveTo(bx, y); shine.lineTo(bx + band, y);
        shine.lineTo(bx + band - h * .8f, y + h); shine.lineTo(bx - h * .8f, y + h); shine.close();
        fxPaint.setStyle(Paint.Style.FILL);
        fxPaint.setColor(Color.WHITE);
        fxPaint.setAlpha(Math.round(110 * (float)Math.sin(Math.PI * k)));
        c.drawPath(shine, fxPaint);
        fxPaint.setAlpha(255);
        c.restore();
    }

    private final Path shine = new Path();

    private static int blend(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t);
        int g = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t);
        int b = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t);
        return Color.argb(255, r, g, b);
    }

    /**
     * O assist no anel do círculo do TAG: enche até ficar pronto (aí o anel fica inteiro e o
     * miolo branco). Era a barra de 5 partes entre as vidas, que agora é do SHINKAI.
     */
    private void assistRing(Canvas c, float x, float y, float radius, float ready, int accent) {
        float k = Arena.clamp(ready, 0f, 1f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(0xFF1A2236);
        c.drawCircle(x, y, radius, paint);
        if (k > 0f) {
            paint.setColor(k >= 1f ? Color.WHITE : accent);
            c.drawArc(x - radius, y - radius, x + radius, y + radius, -90f, 360f * k, false, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * A barra do SHINKAI (entre a vida do titular e a do parceiro): enche na cor do tipo dele;
     * cheia, brilha e escreve SHINKAI; ativa, esvazia com o tempo e diz o tipo; usada no round,
     * fica apagada. Desenhada no lado dele (espelhada à direita), o texto sempre legível.
     */
    private void drawShinkaiBar(Canvas c, FighterState f, boolean right, float x, float y, float w, float h, int duration) {
        int color = f.shinkaiType.color;
        boolean active = f.inShinkai();
        boolean ready = f.shinkaiReady();
        float fill = active ? f.shinkaiFrames / (float)Math.max(1, duration)
            : f.shinkaiUsed ? 0f : f.shinkai / (float)CombatConfig.SHINKAI_MAX;
        double now = effects.now;
        c.save();
        if (right) { c.translate(Arena.VW, 0f); c.scale(-1f, 1f); }
        panel(c, x, y, w, h, Math.min(7f, h * 0.4f), 0xFF0A1020, f.shinkaiUsed && !active ? 0xFF4A5164 : 0xFFCED9F0, 1.3f);
        c.save();
        c.clipPath(shape);
        if (fill > 0f) {
            // Enchendo, a cor do tipo mais apagada; cheia ou ativa, viva e pulsando.
            float glow = ready || active ? 0.75f + 0.25f * (float)Math.sin(now * (ready ? 7.0 : 11.0)) : 0.55f;
            paint.setColor(blend(0xFF0A1020, color, glow));
            c.drawRect(x, y, x + w * fill, y + h, paint);
            paint.setColor(0x99FFFFFF);
            c.drawRect(x, y + 1f, x + w * fill, y + Math.max(2f, h * 0.25f), paint);
            if (ready || active) {
                // Energia correndo pela barra.
                float flow = (float)((now * (active ? 1.6 : 0.9)) % 1.0);
                float fx0 = x + (w + 60f) * flow - 60f;
                fxPaint.setColor(Color.WHITE);
                fxPaint.setAlpha(120);
                c.drawRect(Math.max(x, fx0), y, Math.min(x + w * fill, fx0 + 40f), y + h, fxPaint);
                fxPaint.setAlpha(255);
            }
        }
        c.restore();
        if (ready || active) energyEdge(c, x - 1f, y - 2f, w + 2f, h + 4f, color, ready ? 1f : 0.6f);
        c.restore();
        if (!ready && !active) return;
        float cx = right ? Arena.VW - (x + w / 2f) : x + w / 2f;
        float pop = ready ? 1f + 0.08f * (float)Math.sin(now * 7.0) : 1f;
        text(c, active ? "SHINKAI " + f.shinkaiType.label : "SHINKAI", cx, y + h + 3f, 15f * pop,
            active ? Color.WHITE : blend(color, Color.WHITE, 0.55f));
    }

    private void drawSegments(Canvas c, float bars, float x, float y, float w, float h, int accent) {
        float segment = (w - 20f) / 5f;
        for (int i = 0; i < 5; i++) {
            float left = x + i * (segment + 5f);
            panel(c, left, y, segment, h, Math.min(7f, h * 0.4f), 0xFF0A1020, 0xFFCED9F0, 1.3f);
            float fill = Arena.clamp(bars - i, 0f, 1f);
            c.save();
            c.clipPath(shape);
            paint.setColor(accent);
            c.drawRect(left, y, left + segment * fill, y + h, paint);
            paint.setColor(0xAAFFFFFF);
            c.drawRect(left, y + 1f, left + segment * fill, y + Math.max(2f, h * 0.25f), paint);
            c.restore();
        }
    }

    private void drawPower(Canvas c, FighterState fighter, boolean right) {
        int accent = right ? RED : BLUE;
        c.save();
        if (right) { c.translate(Arena.VW, 0f); c.scale(-1f, 1f); }
        panel(c, 21f, 696f, 400f, 21f, 19f, 0xEF070C1C, accent, 4f);
        drawSegments(c, fighter.superBars(), 86f, 699f, 246f, 14f, accent);
        drawSuperEffects(c, right ? effects.cpu : effects.point, right, 86f, 699f, 246f, 14f);
        paint.setColor(0xFF080E22);
        c.drawCircle(52f, 696f, 24f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(accent);
        c.drawCircle(52f, 696f, 24f, paint);
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.WHITE);
        c.drawCircle(52f, 696f, 19f, paint);
        paint.setStyle(Paint.Style.FILL);
        c.restore();
        int level = Math.max(0, Math.min(5, fighter.superMeter / CombatConfig.METER_PER_BAR));
        drawLevelFire(c, right ? Arena.VW - 52f : 52f, 694f, level, right, .85f);
        text(c, LEVELS[level], right ? Arena.VW - 52f : 52f, 707f, 31f, Color.WHITE);
        text(c, "SUPER", right ? Arena.VW - 377f : 377f, 706f, 18f, Color.WHITE);
    }

    private final Paint fxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Quadros do SHINKAI ativo (do State, lido a cada desenho do HUD). */
    private int shinkaiDuration = 480;
    private final Path flame = new Path();
    /** Cores do fogo do número do super, na cor da barra de cada lado: borda, meio e miolo. */
    private static final int[] FIRE_BLUE = {BLUE, 0xFF8FE6FF, 0xFFE8FCFF};
    private static final int[] FIRE_RED = {RED, 0xFFFF8C98, 0xFFFFE6E9};

    /**
     * Fogo atrás do número do super: quanto mais carregado, mais labaredas, mais altas e mais
     * claras, na cor da barra. 1 e 2: chama baixa; 3 (o Ultra liberado): fogo com miolo claro;
     * 5: labareda grande com centro branco e brasas subindo. Sem alocação.
     */
    private void drawLevelFire(Canvas c, float cx, float baseY, int level, boolean right, float scale) {
        if (level <= 0) return;
        float t = (float)effects.now;
        int tongues = 3 + level;
        boolean ultra = level >= CombatConfig.ULTRA_COST / CombatConfig.METER_PER_BAR;
        // Começa na borda de cima do anel do número (raio ~24) e sobe acima dele.
        float height = (30f + 11f * level) * scale;
        float width = (12f + 2f * level) * scale;
        float spread = (13f + 2.4f * level) * scale;
        fxPaint.setStyle(Paint.Style.FILL);
        // Três camadas, de fora para dentro: cada uma mais baixa, mais fina e mais quente.
        for (int layer = 0; layer < 3; layer++) {
            if (layer == 2 && !ultra) break;
            float k = 1f - layer * .28f;
            int color = (right ? FIRE_RED : FIRE_BLUE)[layer];
            fxPaint.setColor(color);
            fxPaint.setAlpha(layer == 0 ? (ultra ? 210 : 150 + 25 * level) : layer == 1 ? (ultra ? 220 : 110) : 235);
            for (int i = 0; i < tongues; i++) {
                float u = tongues == 1 ? 0f : i / (tongues - 1f) * 2f - 1f;
                // Labaredas do meio mais altas; cada uma tremula no seu ritmo.
                float flick = .78f + .22f * (float)Math.sin(t * (9f + i * 1.3f) + i * 2.1f + layer);
                float h = height * k * (1f - .45f * u * u) * flick;
                float w = width * k;
                float bx = cx + u * spread * k;
                float tip = bx + u * 6f * scale + (float)Math.sin(t * 7f + i * 1.7f) * w * .45f;
                // Gota de fogo com a base redonda, nascendo dentro do anel (mais embaixo nas
                // pontas do leque).
                float by = baseY + 8f * scale * Math.abs(u);
                flame.reset();
                flame.moveTo(bx, by + w * .3f);
                flame.cubicTo(bx - w * .95f, by, bx - w * .6f, by - h * .55f, tip, by - h);
                flame.cubicTo(bx + w * .6f, by - h * .55f, bx + w * .95f, by, bx, by + w * .3f);
                flame.close();
                c.drawPath(flame, fxPaint);
            }
        }
        if (level >= 5) {
            // No máximo: centro branco pulsando e brasas subindo.
            fxPaint.setColor(Color.WHITE);
            fxPaint.setAlpha(Math.round(120 + 60 * (float)Math.sin(t * 10f)));
            c.drawCircle(cx, baseY - 6f * scale, 9f * scale, fxPaint);
            fxPaint.setColor((right ? FIRE_RED : FIRE_BLUE)[2]);
            for (int i = 0; i < 7; i++) {
                float p = (t * .9f + i / 7f) % 1f;
                float ex = cx + (float)Math.sin(i * 2.4f + t * 3f) * spread * .9f;
                float ey = baseY - height * (.4f + 1.1f * p);
                fxPaint.setAlpha(Math.round(230 * (1f - p)));
                c.drawCircle(ex, ey, (2.6f - 1.6f * p) * scale, fxPaint);
            }
        }
        fxPaint.setAlpha(255);
    }

    private void drawLifeEffects(Canvas c, HudEffects.Meter fx, float x, float y, float w, float h) {
        // Amber lower stripe is the impact tail. Red recoverable health above stays visible.
        if (fx.trail > fx.life) {
            // Lost chunk: white right after the hit, cooling to amber while it drains.
            float hot = effects.fade(fx.hitAt, HudEffects.HIT_HOLD + .1f);
            fxPaint.setColor(blend(0xFFFF9A3C, 0xFFFFFFFF, hot));
            c.drawRect(x + w * fx.life, y + h * .35f, x + w * fx.trail, y + h, fxPaint);
        }
        float heal = effects.fade(fx.healAt, HudEffects.HEAL_FLASH);
        if (heal > 0) {
            fxPaint.setColor(0xFFBBFFD8); fxPaint.setAlpha(Math.round(180 * heal));
            c.drawRect(x + w * fx.healFrom, y, x + w * fx.healTo, y + h, fxPaint);
        }
        fxPaint.setAlpha(255);
    }

    private void drawSuperEffects(Canvas c, HudEffects.Meter fx, boolean right,
                                  float x, float y, float w, float h) {
        c.save(); c.clipRect(x, y, x + w, y + h);
        float gain = effects.fade(fx.levelAt, HudEffects.LEVEL_FLASH);
        float spend = Math.max(effects.fade(fx.spendAt, HudEffects.SPEND_FLASH), effects.superBlink(fx));
        fxPaint.setStyle(Paint.Style.FILL);
        if (gain > 0) {
            fxPaint.setColor(0xFFDFFFFF); fxPaint.setAlpha(Math.round(160 * gain));
            c.drawRect(x + w * fx.gainFrom * CombatConfig.METER_PER_BAR / CombatConfig.MAX_METER,
                y, x + w * fx.gainTo * CombatConfig.METER_PER_BAR / CombatConfig.MAX_METER, y + h, fxPaint);
        }
        if (spend > 0) {
            fxPaint.setColor(0xFFFFD698); fxPaint.setAlpha(Math.round(190 * spend));
            c.drawRect(x + w * fx.spentFrom / CombatConfig.MAX_METER, y,
                x + w * fx.spentTo / CombatConfig.MAX_METER, y + h, fxPaint);
        }
        fxPaint.setAlpha(255); c.restore();
        if (fx.superMeter >= CombatConfig.MAX_METER) energyEdge(c, x, y, w, h, right ? RED : BLUE, .65f);
    }

    private void energyEdge(Canvas c, float x, float y, float w, float h, int color, float strength) {
        fxPaint.setStyle(Paint.Style.STROKE); fxPaint.setStrokeWidth(2);
        fxPaint.setColor(color); fxPaint.setAlpha(Math.round(100 * strength));
        c.drawRect(x, y, x + w, y + h, fxPaint);
        // Fixed six deterministic sparks, contained in the frame; no particle allocation.
        for (int i = 0; i < 6; i++) {
            float t = (float)((effects.now * .24 + i / 6.0) % 1);
            float sx = x + 4 + t * (w - 16);
            float sy = i % 2 == 0 ? y : y + h;
            fxPaint.setAlpha((int)Math.round((60 + 100 * strength) * (.5 + .5 * Math.sin(effects.now * 5 + i))));
            c.drawLine(sx, sy, sx + 7, sy, fxPaint);
        }
        fxPaint.setAlpha(255); fxPaint.setStyle(Paint.Style.FILL);
    }

    private void drawFrameEffects(Canvas c) {
        boolean art = hudArt != null;
        for (int side = 0; side < 2; side++) {
            c.save(); if (side == 1) { c.translate(Arena.VW, 0); c.scale(-1, 1); }
            HudEffects.Meter fx = side == 0 ? effects.point : effects.cpu;
            if (art) {
                // The ornament overlays part of the original fill rectangle. Apply
                // visual life overlays in its visible interior after compositing it.
                c.save(); c.clipRect(174, 78, 546, 87);
                drawLifeEffects(c, fx, 165, 78, 389, 9); c.restore();
                if (side == 0 && effects.reserve.fighter != null) {
                    c.save(); c.clipRect(267, 120, 433, 127);
                    drawLifeEffects(c, effects.reserve, 259, 120, 182, 7); c.restore();
                }
            }
            float hit = effects.fade(fx.hitAt, HudEffects.HIT_FLASH);
            float critical = fx.life > 0 && fx.life <= .25f ? .18f + .12f * (float)Math.sin(effects.now * 4) : 0;
            energyEdge(c, art ? 165 : 109, art ? 73 : 62, art ? 389 : 474, art ? 20 : 28,
                hit > 0 ? 0xFFFFE5A0 : critical > 0 ? RED : side == 0 ? BLUE : RED,
                .10f + hit * .8f + critical);
            if (side == 0 && effects.assistEnabled) {
                drawTagWave(c, art);
                float ready = effects.fade(effects.readyAt, HudEffects.READY_FLASH);
                float use = effects.fade(effects.useAt, HudEffects.SPEND_FLASH);
                float x = art ? 174 : 181, y = art ? 96 : 98, w = art ? 330 : 319, h = art ? 11 : 12;
                if (ready > 0) {
                    drawAssistFlash(c, x, y, w, h, ready);
                }
                // Persistent, readable availability even after the one-shot flash ends.
                if (effects.assistRatio >= 1 && !effects.assistUsing) {
                    fxPaint.setStyle(Paint.Style.STROKE); fxPaint.setStrokeWidth(3);
                    fxPaint.setColor(0xFF84ECFF); fxPaint.setAlpha(200);
                    c.drawCircle(art ? 554 : 551, art ? 115 : 109, 11, fxPaint);
                    fxPaint.setAlpha(255); fxPaint.setStyle(Paint.Style.FILL);
                }
                if (ready > 0 || use > 0) {
                    energyEdge(c, x, y, w, h, use > 0 ? 0xFFFFFFFF : BLUE, Math.max(ready, use));
                    fxPaint.setStyle(Paint.Style.STROKE); fxPaint.setStrokeWidth(2);
                    fxPaint.setColor(BLUE); fxPaint.setAlpha(Math.round(210 * Math.max(ready, use)));
                    c.drawCircle(art ? 554 : 551, art ? 115 : 109, 9, fxPaint);
                    fxPaint.setAlpha(255); fxPaint.setStyle(Paint.Style.FILL);
                }
                if (effects.recharging) {
                    float bars = effects.assistRatio * 5;
                    int segment = Math.min(4, (int)bars);
                    float sw = (w - 20) / 5;
                    float left = x + segment * (sw + 5), front = left + sw * (bars - segment);
                    c.save(); c.clipRect(left, y, left + sw, y + h);
                    fxPaint.setColor(0xFFCDFAFF);
                    fxPaint.setAlpha((int)(90 + 35 * Math.sin(effects.now * 7)));
                    c.drawRect(Math.max(left, front - 7), y, front, y + h, fxPaint);
                    fxPaint.setAlpha(255); c.restore();
                }
            }
            c.restore();
        }
    }

    private void drawAssistFlash(Canvas c, float x, float y, float w, float h, float fade) {
        float sw = (w - 20) / 5;
        fxPaint.setColor(0xFFB9F8FF); fxPaint.setAlpha(Math.round(190 * fade));
        for (int i = 0; i < 5; i++) {
            float left = x + i * (sw + 5);
            c.drawRect(left + 5, y + 2, left + sw - 5, y + h - 2, fxPaint);
        }
        fxPaint.setAlpha(255);
    }

    /** A single wave travels inward to the new point portrait, without changing fill. */
    private void drawTagWave(Canvas c, boolean art) {
        float remaining = effects.fade(effects.tagAt, HudEffects.TAG_WAVE);
        if (remaining <= 0) return;
        float progress = 1 - remaining;
        float x = art ? 174 : 181, y = art ? 96 : 98;
        float w = art ? 330 : 319, h = art ? 11 : 12;
        float front = x + w * (1 - Math.min(1, progress / .72f));
        float sw = (w - 20) / 5;
        for (int i = 0; i < 5; i++) {
            float left = x + i * (sw + 5);
            c.save(); c.clipRect(left + 3, y + 1, left + sw - 3, y + h - 1);
            fxPaint.setColor(BLUE); fxPaint.setAlpha(Math.round(220 * remaining));
            c.drawRect(front - 18, y, front + 80, y + h, fxPaint);
            fxPaint.setColor(0xFFE4FFFF); fxPaint.setAlpha(Math.round(235 * remaining));
            c.drawRect(front - 3, y, front + 14, y + h, fxPaint);
            c.restore();
        }
        fxPaint.setAlpha(255);
        if (progress >= .55f) {
            float glow = Math.min(1, (progress - .55f) / .16f) * remaining * 2.5f;
            fxPaint.setStyle(Paint.Style.STROKE); fxPaint.setStrokeWidth(5);
            fxPaint.setColor(0xFF8AF3FF); fxPaint.setAlpha(Math.round(255 * Math.min(1, glow)));
            // Trace only the existing portrait frame; never bleach the face itself.
            float px = art ? 43 : 8, py = art ? 24 : 1;
            float pw = art ? 119 : 98, ph = art ? 86 : 103;
            shape.reset();shape.moveTo(px+12,py);shape.lineTo(px+pw-12,py);
            shape.lineTo(px+pw,py+ph);shape.lineTo(px,py+ph);shape.close();
            c.drawPath(shape,fxPaint);
            fxPaint.setAlpha(255);fxPaint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawToggle(Canvas c, boolean enabled, String label,
                            float left, float top, float right, float bottom, float textSize) {
        float w = right - left, h = bottom - top;
        panel(c, left, top, w, h, Math.min(12f, h * .3f), enabled ? 0xE6123A4E : 0xD60A1224,
            enabled ? 0xFF6FE3FF : 0xFF7B88A6, enabled ? 2.5f : 1.5f);
        paint.setColor(enabled ? 0xFF6FE3FF : BLUE);
        paint.setAlpha(enabled ? 255 : 170);
        c.drawRect(left + 3f, top + 5f, left + 7f, bottom - 5f, paint);
        paint.setAlpha(255);
        label(c, label, (left + right) * .5f + 3f, (top + bottom) * .5f, textSize, Color.WHITE);
    }

    /** Centered bold label with a dark outline, vertically centred on {@code cy}. */
    private void label(Canvas c, String value, float cx, float cy, float size, int color) {
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSkewX(-0.1f);
        paint.setTextSize(size);
        float y = cy - (paint.ascent() + paint.descent()) * .5f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        paint.setColor(0xE6050813);
        c.drawText(value, cx, y, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawText(value, cx, y, paint);
        paint.setTextSkewX(0f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /**
     * Painel da lição do tutorial: em que lição está, o nome, o que fazer, quantas vezes já
     * fez e o PULAR. Entre as lições, "MUITO BEM!"; no fim, "TREINO COMPLETO!".
     */
    void drawTutorial(Canvas c, TutorialDirector tutorial, Comandos.Esquema esquema) {
        if (!tutorial.active()) return;
        reset();
        if (tutorial.completing()) {
            drawBanner(c, "TREINO COMPLETO!", 255, 1.1f);
            return;
        }
        TutorialDirector.Lesson lesson = tutorial.lesson();
        if (lesson == null) return;
        float left = TUTORIAL_PANEL_LEFT, top = TUTORIAL_PANEL_TOP;
        float right = TUTORIAL_PANEL_RIGHT, bottom = TUTORIAL_PANEL_BOTTOM;
        float cx = (left + right) * 0.5f;
        panel(c, left, top, right - left, bottom - top, 14f, 0xE6120A04, 0xFFFFB04A, 2f);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(true);
        paint.setTextSize(14f);
        paint.setColor(0xFFFFC870);
        c.drawText("TREINO  " + (tutorial.lessonIndex() + 1) + "/" + tutorial.lessonCount(), left + 26f, top + 24f, paint);
        paint.setFakeBoldText(false);
        text(c, lesson.title, cx, top + 46f, 26f, Color.WHITE);
        // Os botões do texto no que o jogador está usando (toque, teclado ou controle).
        text(c, Comandos.texto(lesson.how, esquema), cx, top + 74f, 19f, 0xFFFFE6B8);
        // Quantas vezes: uma bolinha por repetição, cheia quando já fez.
        float pip = 7f, gap = 22f;
        float px = cx - (lesson.times - 1) * gap * 0.5f;
        for (int i = 0; i < lesson.times; i++) {
            boolean filled = i < tutorial.count();
            paint.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(filled ? 0xFF7BFF22 : 0xFFFFC870);
            c.drawCircle(px + i * gap, bottom - 10f, pip, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        drawToggle(c, false, "PULAR ▶▶", TUTORIAL_SKIP_LEFT, TUTORIAL_SKIP_TOP, TUTORIAL_SKIP_RIGHT,
            TUTORIAL_SKIP_BOTTOM, 14f);
        if (tutorial.praising()) {
            float k = Math.min(1f, tutorial.praiseFrame() / 8f);
            drawBanner(c, "MUITO BEM!", 255, 1.3f - 0.3f * k);
        }
    }

    private final Shader buttonBody = new Degrade.Radial(0f, -.35f, 1.2f,
        new int[]{0xCC3A4560, 0xBB141B2E, 0xAA05080F}, new float[]{0f, .6f, 1f}, Shader.TileMode.CLAMP);

    /** Round touch button: dark glassy body, coloured ring, bevel highlight and press glow. */
    private void button(Canvas c, float x, float y, float r, int ring, boolean pressed, boolean enabled) {
        c.save();
        c.translate(x, y);
        if (pressed && enabled) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(ring);
            paint.setAlpha(80);
            paint.setStrokeWidth(14f);
            c.drawCircle(0f, 0f, r + 5f, paint);
        }
        float rr = pressed ? r * .94f : r;
        paint.setStyle(Paint.Style.FILL);
        c.save();
        c.scale(rr, rr);
        paint.setShader(buttonBody);
        paint.setAlpha(enabled ? 255 : 140);
        c.drawCircle(0f, 0f, 1f, paint);
        paint.setShader(null);
        c.restore();
        if (pressed && enabled) {
            paint.setColor(ring);
            paint.setAlpha(150);
            c.drawCircle(0f, 0f, rr, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(enabled ? ring : 0xFF5A6070);
        paint.setAlpha(enabled ? 235 : 140);
        c.drawCircle(0f, 0f, rr - 2f, paint);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.WHITE);
        paint.setAlpha(enabled ? 120 : 40);
        c.drawArc(-rr + 8f, -rr + 8f, rr - 8f, rr - 8f, 200f, 130f, false, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
        c.restore();
    }

    private static final int L_RING = 0xFF45C6FF, M_RING = 0xFFFFC53A, H_RING = 0xFFFF4D5E;

    private final Shader bannerFill = new Degrade.Linear(0f, 168f, 0f, 214f,
        new int[]{0xFFFFFFFF, 0xFFFFE27A, 0xFFFF9A1F, 0xFFE0461A}, new float[]{0f, .35f, .7f, 1f},
        Shader.TileMode.CLAMP);
    private final Shader comboFill = new Degrade.Linear(0f, 250f, 0f, 302f,
        new int[]{0xFFFFFFFF, 0xFFFFEA8A, 0xFFFFA81E, 0xFFFF5A1A}, new float[]{0f, .3f, .7f, 1f},
        Shader.TileMode.CLAMP);

    /** Big centered call-out over the fight ("AGARRÃO!", "TECH!"). */
    void drawBanner(Canvas c, String text, int alpha, float scale) {
        if (text == null || alpha <= 0) return;
        reset();
        float cx = Arena.VW * 0.5f, cy = 210f;
        float a = alpha / 255f;
        // Slashed backing strip that widens in with the scale pop.
        float half = 330f * Math.min(1f, 2.2f - scale * 1.1f + .35f);
        shape.reset();
        shape.moveTo(cx - half + 24f, cy - 52f);
        shape.lineTo(cx + half + 24f, cy - 52f);
        shape.lineTo(cx + half - 24f, cy + 14f);
        shape.lineTo(cx - half - 24f, cy + 14f);
        shape.close();
        paint.setColor(Color.argb(Math.round(170 * a), 8, 6, 20));
        c.drawPath(shape, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(Color.argb(Math.round(230 * a), 255, 196, 70));
        c.drawLine(cx - half + 24f, cy - 52f, cx + half + 24f, cy - 52f, paint);
        c.drawLine(cx - half - 24f, cy + 14f, cx + half - 24f, cy + 14f, paint);
        paint.setStyle(Paint.Style.FILL);
        c.save();
        c.scale(scale, scale, cx, cy - 18f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(58f);
        paint.setTextSkewX(-0.22f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(14f);
        paint.setColor(Color.argb(Math.round(150 * a), 255, 70, 20));
        c.drawText(text, cx, cy, paint);
        paint.setStrokeWidth(8f);
        paint.setColor(Color.argb(alpha, 20, 8, 24));
        c.drawText(text, cx, cy, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(bannerFill);
        paint.setAlpha(alpha);
        c.drawText(text, cx, cy, paint);
        paint.setShader(null);
        c.restore();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setTextSkewX(0f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setAlpha(255);
    }

    /** Frames the result of a finished combo stays on screen. */
    static final int COMBO_LINGER_FRAMES = 60;
    private static final long COMBO_BUMP_NANOS = 120_000_000L;
    private final int[] comboHitsSeen = new int[2];
    private final long[] comboBumpAt = new long[2];

    /**
     * Combo counter of one ComboSession: hits, damage and current scaling. The left side
     * shows the player's combos, the right side the CPU's. Single hits are not combos.
     */
    void drawCombo(Canvas c, ComboSession active, ComboSession last, int framesSinceEnd, boolean leftSide) {
        ComboSession combo = active;
        int alpha = 255;
        if (combo == null && last != null && framesSinceEnd < COMBO_LINGER_FRAMES) {
            combo = last;
            alpha = Math.round(255f * (1f - framesSinceEnd / (float)COMBO_LINGER_FRAMES));
        }
        if (combo == null || combo.hitCount < 2) return;
        // Every new hit makes the counter jump, so a beam's fast hits read as a rising count.
        int side = leftSide ? 0 : 1;
        long now = System.nanoTime();
        if (combo.hitCount != comboHitsSeen[side]) {
            if (combo.hitCount > comboHitsSeen[side]) comboBumpAt[side] = now;
            comboHitsSeen[side] = combo.hitCount;
        }
        float bump = Math.max(0f, 1f - (now - comboBumpAt[side]) / (float)COMBO_BUMP_NANOS);
        reset();
        float a = alpha / 255f;
        float x = leftSide ? 40f : VW_RIGHT;
        Paint.Align align = leftSide ? Paint.Align.LEFT : Paint.Align.RIGHT;
        String count = comboCount(side, combo.hitCount);
        // Number: gradient fill, double outline, pops on every new hit.
        paint.setTextAlign(align);
        paint.setFakeBoldText(true);
        paint.setTextSkewX(-0.2f);
        float numberSize = 62f * (1f + 0.30f * bump);
        paint.setTextSize(numberSize);
        float numberWidth = paint.measureText(count);
        c.save();
        // Small shake on the hit that just landed.
        c.translate(bump > .5f ? (side == 0 ? 3f : -3f) * (bump - .5f) * 2f : 0f, 0f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(12f);
        paint.setColor(Color.argb(Math.round(140 * a), 255, 80, 20));
        c.drawText(count, x, 300f, paint);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.argb(alpha, 18, 8, 22));
        c.drawText(count, x, 300f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(comboFill);
        paint.setAlpha(alpha);
        c.drawText(count, x, 300f, paint);
        paint.setShader(null);
        if (bump > 0f) {
            paint.setColor(Color.argb(Math.round(200 * bump * a), 255, 255, 255));
            c.drawText(count, x, 300f, paint);
        }
        c.restore();
        // "HITS" tag next to the number.
        paint.setTextSize(24f);
        float hitsX = leftSide ? x + numberWidth + 8f : x - numberWidth - 18f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.argb(alpha, 18, 8, 22));
        c.drawText("HITS", hitsX, 296f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        c.drawText("HITS", hitsX, 296f, paint);
        // Damage and scaling on a slanted strip.
        String info = comboInfo(side, combo);
        paint.setTextSize(17f);
        paint.setTextSkewX(-0.12f);
        float infoWidth = paint.measureText(info) + 28f;
        float left = leftSide ? x - 14f : x - infoWidth + 14f;
        shape.reset();
        shape.moveTo(left + 10f, 309f); shape.lineTo(left + infoWidth + 10f, 309f);
        shape.lineTo(left + infoWidth - 6f, 333f); shape.lineTo(left - 6f, 333f); shape.close();
        paint.setColor(Color.argb(Math.round(190 * a), 8, 10, 24));
        c.drawPath(shape, paint);
        paint.setColor(Color.argb(alpha, 255, 170, 40));
        c.drawRect(leftSide ? left - 6f : left + infoWidth - 2f, 309f, leftSide ? left - 2f : left + infoWidth + 2f, 333f, paint);
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        c.drawText(info, leftSide ? x : x, 327f, paint);
        if (combo.airCombo) {
            paint.setColor(Color.argb(alpha, 140, 225, 255));
            c.drawText("JUGGLE " + combo.juggleCount, x, 354f, paint);
        }
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setTextSkewX(0f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private final int[] comboCountShown = {-1, -1}, comboDamageShown = {-1, -1}, comboScaleShown = {-1, -1};
    private final String[] comboCountText = {"", ""}, comboInfoText = {"", ""};

    /** Strings rebuilt only when the hit count changes, not every frame. */
    private String comboCount(int side, int hits) {
        if (comboCountShown[side] != hits) {
            comboCountShown[side] = hits;
            comboCountText[side] = String.valueOf(hits);
        }
        return comboCountText[side];
    }

    private String comboInfo(int side, ComboSession combo) {
        if (comboDamageShown[side] != combo.comboDamage || comboScaleShown[side] != combo.damageScale) {
            comboDamageShown[side] = combo.comboDamage;
            comboScaleShown[side] = combo.damageScale;
            comboInfoText[side] = "DANO " + combo.comboDamage + "  •  ESCALA " + combo.damageScale / 10 + "%";
        }
        return comboInfoText[side];
    }

    /** Right combo sits left of the training toggles column. */
    private static final float VW_RIGHT = 1060f;

    private void drawSuperMeter(
        Canvas c,
        FighterState fighter,
        float left,
        float top,
        float right,
        float bottom,
        boolean showText
    ) {
        float gap = 4f;
        float totalWidth = right - left;
        float segmentWidth = (totalWidth - gap * 4f) / 5f;

        for (int i = 0; i < 5; i++) {
            float segmentLeft = left + i * (segmentWidth + gap);
            float segmentRight = segmentLeft + segmentWidth;

            paint.setColor(Color.rgb(45, 53, 62));
            c.drawRoundRect(segmentLeft, top, segmentRight, bottom, 4f, 4f, paint);

            float fill = Arena.clamp(fighter.superBars() - i, 0f, 1f);
            if (fill > 0f) {
                paint.setColor(fighter.profile.color);
                c.drawRoundRect(segmentLeft, top, segmentLeft + segmentWidth * fill, bottom, 4f, 4f, paint);
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.4f);
            paint.setColor(Color.argb(210, 255, 255, 255));
            c.drawRoundRect(segmentLeft, top, segmentRight, bottom, 4f, 4f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        if (showText) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(12f);
            c.drawText(fighter.superHudLabel, left, bottom + 13f, paint);
        }
    }

    private void drawTagCooldown(Canvas c, State s, float left, float top, float right, float bottom) {
        float ratio = s.tagReadyRatio();

        paint.setColor(Color.rgb(45, 53, 62));
        c.drawRoundRect(left, top, right, bottom, 5f, 5f, paint);

        if (ratio > 0f) {
            paint.setColor(s.reserve().profile.color);
            c.drawRoundRect(left, top, left + (right - left) * ratio, bottom, 5f, 5f, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.argb(210, 255, 255, 255));
        c.drawRoundRect(left, top, right, bottom, 5f, 5f, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(12f);
        c.drawText(s.tagCooldownLabel(), left, bottom + 13f, paint);
    }

    private static int lifeColor(float ratio) {
        if (ratio > 0.55f) return Color.rgb(111, 223, 105);
        if (ratio > 0.25f) return Color.rgb(240, 190, 72);
        return Color.rgb(229, 82, 82);
    }

    private void drawLifeBar(
        Canvas c,
        FighterState fighter,
        float left,
        float top,
        float right,
        float bottom,
        boolean showText
    ) {
        float ratio = Arena.clamp(fighter.life / (float)fighter.profile.maxLife, 0f, 1f);
        float width = right - left;

        paint.setColor(Color.rgb(45, 53, 62));
        c.drawRoundRect(left, top, right, bottom, 8, 8, paint);

        // Recoverable life: the red stretch right after the life, which comes back off point.
        float recoverable = Arena.clamp(fighter.recoverableLife / (float)fighter.profile.maxLife, 0f, 1f - ratio);
        if (recoverable > 0f) {
            paint.setColor(Color.argb(200, 214, 52, 62));
            c.drawRoundRect(left + width * ratio - 8f, top, left + width * (ratio + recoverable), bottom, 8, 8, paint);
        }
        if (ratio > 0f) {
            paint.setColor(lifeColor(ratio));
            c.drawRoundRect(left, top, left + width * ratio, bottom, 8, 8, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(showText ? 2f : 1.5f);
        paint.setColor(Color.argb(210, 255, 255, 255));
        c.drawRoundRect(left, top, right, bottom, 8, 8, paint);
        paint.setStyle(Paint.Style.FILL);

        if (showText) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(15);
            c.drawText(fighter.lifeHudLabel, left + 6, bottom - 7, paint);
        }
    }

    /** Overhead panel drawn in world space above the opponent's head. */
    void drawOpponentPanel(Canvas c, OpponentPanel p) {
        reset();
        float barLeft = p.x - 125f;
        float barRight = p.x + 125f;
        float barTop = p.visualTop - 54f;
        float barBottom = barTop + 16f;

        paint.setColor(Color.argb(205, 12, 16, 28));
        c.drawRoundRect(barLeft - 8f, barTop - 28f, barRight + 8f, barBottom + 23f, 10f, 10f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(15f);
        c.drawText(p.status, p.x, barTop - 9f, paint);
        paint.setFakeBoldText(false);

        paint.setColor(Color.rgb(45, 53, 62));
        c.drawRoundRect(barLeft, barTop, barRight, barBottom, 6f, 6f, paint);

        if (p.lifeRatio > 0f) {
            paint.setColor(lifeColor(p.lifeRatio));
            c.drawRoundRect(barLeft, barTop, barLeft + (barRight - barLeft) * p.lifeRatio, barBottom, 6f, 6f, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.WHITE);
        c.drawRoundRect(barLeft, barTop, barRight, barBottom, 6f, 6f, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(12f);
        c.drawText(p.lifeLabel, p.x, barBottom + 16f, paint);
        paint.setTextSize(11f);
        c.drawText(p.superLabel, p.x, barBottom + 30f, paint);

        if (p.damageProgress > 0f) {
            paint.setColor(Color.WHITE);
            paint.setAlpha(Math.round(255f * p.damageProgress));
            paint.setFakeBoldText(true);
            paint.setTextSize(24f);
            c.drawText(p.damageLabel, p.x, p.visualTop - 72f - (1f - p.damageProgress) * 28f, paint);
            paint.setFakeBoldText(false);
            paint.setAlpha(255);
        }

        paint.setTextAlign(Paint.Align.LEFT);
    }

    void drawControls(Canvas c, State s) {
        reset();
        // D-pad: glassy disc, eight sectors, the active one lit in the player's blue.
        c.save();
        c.translate(DPAD_X, DPAD_Y);
        c.save();
        c.scale(DPAD_RADIUS, DPAD_RADIUS);
        paint.setShader(buttonBody);
        paint.setAlpha(210);
        c.drawCircle(0f, 0f, 1f, paint);
        paint.setShader(null);
        c.restore();
        paint.setAlpha(255);
        int direction = s.dpadDirection();
        if (direction != 0) {
            int directionIndex = direction - 1;
            float angle = (float)Math.toDegrees(Math.atan2(DPAD_UNIT_Y[directionIndex], DPAD_UNIT_X[directionIndex]));
            paint.setColor(BLUE);
            paint.setAlpha(120);
            c.drawArc(-DPAD_RADIUS + 4f, -DPAD_RADIUS + 4f, DPAD_RADIUS - 4f, DPAD_RADIUS - 4f,
                angle - 22.5f, 45f, true, paint);
            paint.setAlpha(255);
            paint.setColor(0xE6FFFFFF);
            c.drawCircle(DPAD_UNIT_X[directionIndex] * DPAD_RADIUS * 0.59f, DPAD_UNIT_Y[directionIndex] * DPAD_RADIUS * 0.59f,
                DPAD_RADIUS * 0.22f, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(0x40FFFFFF);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i + Math.PI / 8;
            c.drawLine((float)Math.cos(a) * (DPAD_DEADZONE + 4f), (float)Math.sin(a) * (DPAD_DEADZONE + 4f),
                (float)Math.cos(a) * (DPAD_RADIUS - 8f), (float)Math.sin(a) * (DPAD_RADIUS - 8f), paint);
        }
        paint.setStrokeWidth(4f);
        paint.setColor(0xD9A9D8FF);
        c.drawCircle(0f, 0f, DPAD_RADIUS - 2f, paint);
        paint.setStrokeWidth(2f);
        paint.setColor(0x66FFFFFF);
        c.drawArc(-DPAD_RADIUS + 10f, -DPAD_RADIUS + 10f, DPAD_RADIUS - 10f, DPAD_RADIUS - 10f, 200f, 130f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xD90A1222);
        c.drawCircle(0f, 0f, DPAD_DEADZONE, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(0x80A9D8FF);
        c.drawCircle(0f, 0f, DPAD_DEADZONE, paint);
        paint.setStyle(Paint.Style.FILL);
        c.restore();
        for (int i = 0; i < DPAD_LABELS.length; i++) {
            boolean lit = direction == i + 1;
            label(c, DPAD_LABELS[i], DPAD_X + DPAD_UNIT_X[i] * DPAD_RADIUS * 0.64f, DPAD_Y + DPAD_UNIT_Y[i] * DPAD_RADIUS * 0.64f,
                lit ? 31f : 28f, lit ? 0xFF0A1424 : 0xE6FFFFFF);
        }

        drawAttackButton(c, LIGHT_X, LIGHT_Y, "L", s.pressed(Control.LIGHT));
        drawAttackButton(c, MEDIUM_X, MEDIUM_Y, "M", s.pressed(Control.MEDIUM));
        drawAttackButton(c, HEAVY_X, HEAVY_Y, "H", s.pressed(Control.HEAVY));
        drawPairSpot(c, THROW_X, THROW_Y, "L+M", s.pressed(Control.THROW));
        drawPairSpot(c, PUSHBLOCK_X, PUSHBLOCK_Y, "M+H", s.pressed(Control.PUSHBLOCK));
        drawComboButton(c, s.pressed(Control.COMBO));
        drawTagButton(c, s);
        drawSuperButton(c, s);
        drawShinkaiButton(c, s);
    }

    /**
     * Botão SHINKAI: o anel mostra a barra enchendo; cheia, acende na cor do tipo; ativo, o anel
     * esvazia com os segundos; usado no round, apagado.
     */
    private void drawShinkaiButton(Canvas c, State s) {
        float x = SHINKAI_X, y = SHINKAI_Y, r = SHINKAI_RADIUS;
        FighterState f = s.active();
        int duration = s.shinkaiDuration();
        float left = f.inShinkai() ? f.shinkaiFrames / (float)Math.max(1, duration) : 0f;
        boolean ready = f.shinkaiReady();
        boolean pressed = s.pressed(Control.SHINKAI);
        int ring = f.shinkaiType.color;
        boolean lit = ready || left > 0f;
        button(c, x, y, r, ring, pressed || left > 0f, lit);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        if (left > 0f) {
            paint.setColor(0xFFFFFFFF);
            c.drawArc(x - r - 5f, y - r - 5f, x + r + 5f, y + r + 5f, -90f, 360f * left, false, paint);
        } else if (!ready && !f.shinkaiUsed && f.shinkai > 0) {
            paint.setColor(ring);
            c.drawArc(x - r - 5f, y - r - 5f, x + r + 5f, y + r + 5f, -90f,
                360f * f.shinkai / CombatConfig.SHINKAI_MAX, false, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        if (ready && !pressed) readyHalo(c, x, y, r, ring);
        label(c, "SHINKAI", x, y + (left > 0f || f.shinkaiUsed ? -7f : 0f), 13f, lit ? Color.WHITE : 0xFFB4BCCB);
        if (left > 0f) label(c, shinkaiLabel(f.shinkaiFrames / 60f), x, y + 11f, 10f, Color.WHITE);
        else if (f.shinkaiUsed) label(c, "USADO", x, y + 11f, 9f, 0xFFB4BCCB);
    }

    private int shinkaiTenthsShown = -1;
    private String shinkaiText = "";

    /** "3.4s": rebuilt only when the tenth changes, not every frame. */
    private String shinkaiLabel(float seconds) {
        int tenths = Math.max(0, Math.round(seconds * 10f));
        if (tenths != shinkaiTenthsShown) {
            shinkaiTenthsShown = tenths;
            shinkaiText = tenths / 10 + "." + tenths % 10 + "s";
        }
        return shinkaiText;
    }

    private void drawAttackButton(Canvas c, float x, float y, String label, boolean pressed) {
        int ring = "L".equals(label) ? L_RING : "M".equals(label) ? M_RING : H_RING;
        button(c, x, y, ATTACK_RADIUS, ring, pressed, true);
        label(c, label, x, y, pressed ? 39f : 40f, pressed ? Color.WHITE : 0xFFF2F6FF);
    }

    /** A spot between two buttons that presses both: L+M throw/tech, M+H pushblock. */
    private void drawPairSpot(Canvas c, float x, float y, String label, boolean pressed) {
        button(c, x, y, THROW_RADIUS, 0xFFFFD65C, pressed, true);
        label(c, label, x, y, 11f, Color.WHITE);
    }

    private void drawComboButton(Canvas c, boolean pressed) {
        button(c, COMBO_X, COMBO_Y, COMBO_RADIUS, 0xFFB98CFF, pressed, true);
        label(c, "COMBO", COMBO_X, COMBO_Y, 18f, Color.WHITE);
    }

    private void drawTagButton(Canvas c, State s) {
        boolean pressed = s.pressed(Control.TAG);
        boolean enabled = s.canTag();
        int reserveColor = s.reserve().profile.color | 0xFF000000;
        button(c, TAG_X, TAG_Y, TAG_RADIUS, reserveColor, pressed, enabled);
        if (enabled && !pressed) readyHalo(c, TAG_X, TAG_Y, TAG_RADIUS, reserveColor);
        else if (!enabled) cooldownArc(c, TAG_X, TAG_Y, TAG_RADIUS, reserveColor, s.tagReadyRatio());
        label(c, enabled ? s.tagButtonTitle() : s.tagButtonLabel(), TAG_X, TAG_Y, enabled ? 18f : 15f,
            enabled ? Color.WHITE : 0xFFB4BCCB);
    }

    /** O quanto a recarga já andou: um arco que fecha em volta do botão (vazio = acabou de usar). */
    private void cooldownArc(Canvas c, float x, float y, float r, int color, float ratio) {
        if (ratio <= 0f || ratio >= 1f) return;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(color);
        paint.setAlpha(170);
        c.drawArc(x - r - 6f, y - r - 6f, x + r + 6f, y + r + 6f, -90f, 360f * ratio, false, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Slowly rotating arc that tells a button is ready to use. */
    private void readyHalo(Canvas c, float x, float y, float r, int color) {
        float spin = (float)((effects.now * 140.0) % 360.0);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(color);
        paint.setAlpha(Math.round(150 + 80 * (float)Math.sin(effects.now * 5.0)));
        c.drawArc(x - r - 6f, y - r - 6f, x + r + 6f, y + r + 6f, spin, 70f, false, paint);
        c.drawArc(x - r - 6f, y - r - 6f, x + r + 6f, y + r + 6f, spin + 180f, 70f, false, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSuperButton(Canvas c, State s) {
        boolean pressed = s.pressed(Control.SUPER);
        boolean ultra = s.ultraReady();
        boolean enabled = ultra || s.canSuper();
        int ring = ultra ? 0xFFFF6AD5 : 0xFFFFD45A;
        button(c, SUPER_X, SUPER_Y, SUPER_RADIUS, ring, pressed, enabled);
        if (enabled && !pressed) readyHalo(c, SUPER_X, SUPER_Y, SUPER_RADIUS, ring);
        label(c, ultra ? "ULTRA" : "SUPER", SUPER_X, SUPER_Y - 8f, 18f, enabled ? Color.WHITE : 0xFFB4BCCB);
        label(c, s.active().superLevelHudLabel, SUPER_X, SUPER_Y + 15f, 13f, enabled ? ring : 0xFF9AA2B2);
    }
}
