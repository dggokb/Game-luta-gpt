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
        String versionLabel();
        String stateLabel();
        int facing();
        boolean aiEnabled();
        boolean debugEnabled();
        /** The pair on the player's side, as "p01+p03". */
        String teamLabel();
        /** 0 while tagging, rising to 1 when the tag is ready. */
        float tagReadyRatio();
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
        /** Overdrive left (1 just turned on, 0 when off). */
        float overdriveRatio();
        float overdriveSeconds();
        /** The Overdrive of this round has not been used yet. */
        boolean overdriveReady();
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
        c.drawBitmap(hudArt, artSource, artTarget, imagePaint);
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
        reset();
        FighterState active = s.active();
        FighterState reserve = s.reserve();

        if (hudArt == null) {
            drawTeam(c, active, reserve, null, false, s.tagReadyRatio(), portraits);
            drawTeam(c, s.opponent(), null, s.opponentReserveCharacter(), true, 0f, portraits);
            drawPower(c, active, false);
            drawPower(c, s.opponent(), true);
        } else {
            drawArtTeam(c, active, reserve, null, false, s.tagReadyRatio(), portraits);
            drawArtTeam(c, s.opponent(), null, s.opponentReserveCharacter(), true, 0f, portraits);
            artwork(c, 0, 0, 1672, 280, 0f, 0f, 1280f, 170f);
            drawSegments(c, Arena.clamp(s.tagReadyRatio(), 0f, 1f) * 5f, 174f, 96f, 330f, 11f, BLUE);
            c.save(); c.translate(Arena.VW, 0f); c.scale(-1f, 1f);
            // Training CPU has no playable reserve or assist system.
            drawSegments(c, 0f, 174f, 96f, 330f, 11f, 0xFF596072);
            c.restore();
            drawArtPower(c, active, false);
            drawArtPower(c, s.opponent(), true);
            text(c, "PLAYER 1", 220f, 65f, 15f, Color.WHITE);
            text(c, "PLAYER 2", 1060f, 65f, 15f, Color.WHITE);
            text(c, "TAG", 222f, 132f, 13f, Color.WHITE);
            text(c, "TAG OFF", 1058f, 132f, 13f, 0xFFBCC4D8);
        }
        drawFrameEffects(c);
        // Training has no round clock: never display a fabricated countdown.
        if (hudArt == null) panel(c, 588f, 6f, 104f, 93f, 18f, 0xEF070B19, Color.WHITE, 2f);
        text(c, "∞", 640f, 70f, 58f, Color.WHITE);
        text(c, "TREINO", 640f, 87f, 12f, 0xFFB9C7E5);
        if (s.debugEnabled()) {
            text(c, s.versionLabel() + " • " + s.stateLabel(), 640f, 194f, 14f, Color.WHITE);
        }
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
        drawToggle(c, s.pressed(Control.CHARACTER_SELECT), "◀ SELEÇÃO",
            SELECT_BUTTON_LEFT, SELECT_BUTTON_TOP, SELECT_BUTTON_RIGHT, SELECT_BUTTON_BOTTOM, 17f);
    }


    private static final int BLUE = 0xFF22BFFF, RED = 0xFFFF354D;
    private final Path shape = new Path();
    private final RectF portraitBox = new RectF();
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Shader pointLifeGradient = new LinearGradient(0f, 63f, 0f, 89f,
        new int[]{0xFFFFFF9C, 0xFF7BFF22, 0xFF0ADF49}, null, Shader.TileMode.CLAMP);
    private final Shader reserveLifeGradient = new LinearGradient(0f, 119f, 0f, 136f,
        new int[]{0xFFFFFF9C, 0xFF7BFF22, 0xFF0ADF49}, null, Shader.TileMode.CLAMP);
    private static final String[] LEVELS = {"0", "1", "2", "3", "4", "5"};

    private void drawArtTeam(Canvas c, FighterState point, FighterState reserve,
                             CharacterDefinition absent, boolean right, float ready,
                             CharacterSelectRenderer portraits) {
        c.save();
        if (right) { c.translate(Arena.VW, 0f); c.scale(-1f, 1f); }
        // All coordinates are calibrated against the ornament atlas, not its bounding box.
        portrait(c, portraits, point.character, 43f, 24f, 119f, 86f, right ? RED : BLUE);
        CharacterDefinition partner = reserve == null ? absent : reserve.character;
        if (partner != null) portrait(c, portraits, partner, 120f, 110f, 65f, 25f, right ? RED : BLUE);
        energyLife(c, point, 165f, 74f, 389f, 18f);
        if (reserve != null) energyLife(c, reserve, 259f, 119f, 182f, 10f);
        else panel(c, 259f, 119f, 182f, 10f, 6f, 0xFF161B28, 0xFF596072, 1f);
        paint.setColor(right ? RED : BLUE);
        c.drawCircle(554f, 115f, 8f, paint);
        paint.setColor(reserve != null && ready >= 1f ? Color.WHITE : 0xFF111723);
        c.drawCircle(554f, 115f, 4f, paint);
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
        float bars = Arena.clamp(fighter.superMeter / (float)CombatConfig.MAX_METER, 0f, 1f) * 4f;
        for (int i = 0; i < 4; i++) {
            float x = 94f + i * 57f;
            panel(c, x, 679f, 51f, 15f, 8f, 0xFF070E20, right ? RED : BLUE, 1.5f);
            c.save(); c.clipPath(shape);
            paint.setColor(right ? RED : BLUE);
            c.drawRect(x, 679f, x + 51f * Arena.clamp(bars - i, 0f, 1f), 694f, paint);
            paint.setColor(0xDDFFFFFF);
            c.drawRect(x, 680f, x + 51f * Arena.clamp(bars - i, 0f, 1f), 683f, paint);
            c.restore();
        }
        drawSuperEffects(c, right ? effects.cpu : effects.point, right, 94f, 679f, 222f, 15f);
        c.restore();
        int level = Math.max(0, Math.min(5, fighter.superMeter / CombatConfig.METER_PER_BAR));
        text(c, LEVELS[level], right ? Arena.VW - 51f : 51f, 698f, 38f, Color.WHITE);
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
        drawSegments(c, Arena.clamp(ready, 0f, 1f) * 5f, 181f, 98f, 319f, 12f, accent);
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
        c.restore();
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
        paint.setColor(0x99FFFFFF);
        c.drawRect(x, y + 2f, x + w * life, y + 4f, paint);
        HudEffects.Meter fx = fighter == effects.point.fighter ? effects.point
            : fighter == effects.cpu.fighter ? effects.cpu : effects.reserve;
        drawLifeEffects(c, fx, x, y, w, h);
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
        text(c, LEVELS[level], right ? Arena.VW - 52f : 52f, 707f, 31f, Color.WHITE);
        text(c, "SUPER", right ? Arena.VW - 377f : 377f, 706f, 18f, Color.WHITE);
    }

    private final Paint fxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private void drawLifeEffects(Canvas c, HudEffects.Meter fx, float x, float y, float w, float h) {
        // Amber lower stripe is the impact tail. Red recoverable health above stays visible.
        if (fx.trail > fx.life) {
            fxPaint.setColor(0xFFFFCC64);
            c.drawRect(x + w * fx.life, y + h * .65f, x + w * fx.trail, y + h, fxPaint);
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
        float spend = effects.fade(fx.spendAt, HudEffects.SPEND_FLASH);
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
            float hit = effects.fade(fx.hitAt, HudEffects.HIT_FLASH);
            float critical = fx.life > 0 && fx.life <= .25f ? .18f + .12f * (float)Math.sin(effects.now * 4) : 0;
            energyEdge(c, art ? 165 : 109, art ? 73 : 62, art ? 389 : 474, art ? 20 : 28,
                hit > 0 ? 0xFFFFE5A0 : critical > 0 ? RED : side == 0 ? BLUE : RED,
                .10f + hit * .8f + critical);
            if (side == 0 && effects.assistEnabled) {
                float ready = effects.fade(effects.readyAt, HudEffects.READY_FLASH);
                float use = effects.fade(effects.useAt, HudEffects.SPEND_FLASH);
                float x = art ? 174 : 181, y = art ? 96 : 98, w = art ? 330 : 319, h = art ? 11 : 12;
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

    private void drawToggle(Canvas c, boolean enabled, String label,
                            float left, float top, float right, float bottom, float textSize) {
        paint.setColor(enabled ? Color.rgb(74, 205, 232) : Color.argb(180, 45, 53, 62));
        c.drawRoundRect(left, top, right, bottom, 12f, 12f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(Color.WHITE);
        c.drawRoundRect(left, top, right, bottom, 12f, 12f, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(enabled ? Color.rgb(18, 35, 48) : Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(textSize);
        float cy = (top + bottom) * 0.5f;
        c.drawText(label, (left + right) * 0.5f, cy - (paint.ascent() + paint.descent()) * 0.5f, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** Big centered call-out over the fight ("AGARRÃO!", "TECH!"). */
    void drawBanner(Canvas c, String text, int alpha, float scale) {
        if (text == null || alpha <= 0) return;
        reset();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(54f * scale);
        paint.setTextSkewX(-0.18f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
        paint.setColor(Color.argb(alpha, 20, 12, 30));
        c.drawText(text, Arena.VW * 0.5f, 210f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(alpha, 255, 226, 120));
        c.drawText(text, Arena.VW * 0.5f, 210f, paint);
        paint.setTextSkewX(0f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
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
        float x = leftSide ? 40f : VW_RIGHT;
        paint.setTextAlign(leftSide ? Paint.Align.LEFT : Paint.Align.RIGHT);
        paint.setFakeBoldText(true);
        paint.setTextSize(46f * (1f + 0.32f * bump));
        paint.setColor(Color.argb(alpha, 255, 214 + Math.round(41f * bump), 92 + Math.round(163f * bump)));
        c.drawText(combo.hitCount + " HITS", x, 300f, paint);
        paint.setTextSize(18f);
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        c.drawText("DANO " + combo.comboDamage + "  •  ESCALA " + combo.damageScale / 10 + "%", x, 326f, paint);
        if (combo.airCombo) {
            c.drawText("JUGGLE " + combo.juggleCount, x, 348f, paint);
        }
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private static final float VW_RIGHT = Arena.VW - 40f;

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
        paint.setColor(Color.argb(105, 7, 13, 26));
        c.drawCircle(DPAD_X, DPAD_Y, DPAD_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.argb(215, 255, 255, 255));
        c.drawCircle(DPAD_X, DPAD_Y, DPAD_RADIUS, paint);

        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(90, 255, 255, 255));
        for (int i = 0; i < DPAD_LABELS.length; i++) {
            float x = DPAD_X + DPAD_UNIT_X[i] * DPAD_RADIUS;
            float y = DPAD_Y + DPAD_UNIT_Y[i] * DPAD_RADIUS;
            c.drawLine(DPAD_X, DPAD_Y, x, y, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        int direction = s.dpadDirection();
        if (direction != 0) {
            int directionIndex = direction - 1;
            float hx = DPAD_X + DPAD_UNIT_X[directionIndex] * 72f;
            float hy = DPAD_Y + DPAD_UNIT_Y[directionIndex] * 72f;
            paint.setColor(Color.argb(165, 255, 255, 255));
            c.drawCircle(hx, hy, 31f, paint);
        }

        paint.setColor(Color.argb(175, 10, 18, 32));
        c.drawCircle(DPAD_X, DPAD_Y, DPAD_DEADZONE, paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(27f);

        float textCenterOffset = -(paint.ascent() + paint.descent()) / 2f;
        for (int i = 0; i < DPAD_LABELS.length; i++) {
            float tx = DPAD_X + DPAD_UNIT_X[i] * 78f;
            float ty = DPAD_Y + DPAD_UNIT_Y[i] * 78f + textCenterOffset;
            c.drawText(DPAD_LABELS[i], tx, ty, paint);
        }

        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);

        drawAttackButton(c, LIGHT_X, LIGHT_Y, "L", s.pressed(Control.LIGHT));
        drawAttackButton(c, MEDIUM_X, MEDIUM_Y, "M", s.pressed(Control.MEDIUM));
        drawAttackButton(c, HEAVY_X, HEAVY_Y, "H", s.pressed(Control.HEAVY));
        drawPairSpot(c, THROW_X, THROW_Y, "L+M", s.pressed(Control.THROW));
        drawPairSpot(c, PUSHBLOCK_X, PUSHBLOCK_Y, "M+H", s.pressed(Control.PUSHBLOCK));
        drawComboButton(c, s.pressed(Control.COMBO));
        drawTagButton(c, s);
        drawSuperButton(c, s);
        drawOverdriveButton(c, s);
    }

    /** OD: lit while unused this round, a draining ring while on, dim once spent. */
    private void drawOverdriveButton(Canvas c, State s) {
        float x = OVERDRIVE_X, y = OVERDRIVE_Y, r = OVERDRIVE_RADIUS;
        float left = s.overdriveRatio();
        boolean ready = s.overdriveReady();
        boolean pressed = s.pressed(Control.OVERDRIVE);
        paint.setColor(left > 0f ? Color.argb(210, 150, 30, 10)
            : ready ? (pressed ? Color.rgb(255, 120, 40) : Color.argb(185, 90, 24, 12))
            : Color.argb(90, 55, 60, 68));
        c.drawCircle(x, y, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(ready || left > 0f ? Color.argb(235, 255, 140, 50) : Color.argb(120, 180, 180, 180));
        if (left > 0f) {
            c.drawArc(x - r, y - r, x + r, y + r, -90f, 360f * left, false, paint);
        } else {
            c.drawCircle(x, y, r, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ready || left > 0f ? Color.WHITE : Color.argb(150, 220, 220, 220));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(17f);
        c.drawText("OD", x, y + (left > 0f || !ready ? -2f : 6f), paint);
        paint.setTextSize(10f);
        if (left > 0f) c.drawText(overdriveLabel(s.overdriveSeconds()), x, y + 13f, paint);
        else if (!ready) c.drawText("USADO", x, y + 13f, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private int overdriveTenthsShown = -1;
    private String overdriveText = "";

    /** "3.4s": rebuilt only when the tenth changes, not every frame. */
    private String overdriveLabel(float seconds) {
        int tenths = Math.max(0, Math.round(seconds * 10f));
        if (tenths != overdriveTenthsShown) {
            overdriveTenthsShown = tenths;
            overdriveText = tenths / 10 + "." + tenths % 10 + "s";
        }
        return overdriveText;
    }

    private void drawAttackButton(Canvas c, float x, float y, String label, boolean pressed) {
        paint.setColor(pressed ? Color.argb(195, 255, 255, 255) : Color.argb(120, 7, 13, 26));
        c.drawCircle(x, y, ATTACK_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.argb(220, 255, 255, 255));
        c.drawCircle(x, y, ATTACK_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(pressed ? Color.rgb(25, 35, 48) : Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(31f);
        paint.setFakeBoldText(true);
        float textY = y - (paint.ascent() + paint.descent()) / 2f;
        c.drawText(label, x, textY, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** A spot between two buttons that presses both: L+M throw/tech, M+H pushblock. */
    private void drawPairSpot(Canvas c, float x, float y, String label, boolean pressed) {
        paint.setColor(pressed ? Color.argb(220, 255, 255, 255) : Color.argb(170, 7, 13, 26));
        c.drawCircle(x, y, THROW_RADIUS, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(225, 255, 214, 92));
        c.drawCircle(x, y, THROW_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pressed ? Color.rgb(25, 35, 48) : Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11f);
        paint.setFakeBoldText(true);
        float textY = y - (paint.ascent() + paint.descent()) / 2f;
        c.drawText(label, x, textY, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawComboButton(Canvas c, boolean pressed) {
        paint.setColor(pressed ? Color.argb(205, 255, 255, 255) : Color.argb(135, 7, 13, 26));
        c.drawCircle(COMBO_X, COMBO_Y, COMBO_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.argb(225, 255, 255, 255));
        c.drawCircle(COMBO_X, COMBO_Y, COMBO_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(pressed ? Color.rgb(25, 35, 48) : Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        float textY = COMBO_Y - (paint.ascent() + paint.descent()) / 2f;
        c.drawText("COMBO", COMBO_X, textY, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawTagButton(Canvas c, State s) {
        boolean pressed = s.pressed(Control.TAG);
        boolean enabled = s.canTag();
        int reserveColor = s.reserve().profile.color;

        if (!enabled) {
            paint.setColor(Color.argb(95, 55, 60, 68));
        } else {
            paint.setColor(pressed ? reserveColor : Color.argb(145, 7, 13, 26));
        }
        c.drawCircle(TAG_X, TAG_Y, TAG_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(enabled ? reserveColor : Color.argb(120, 180, 180, 180));
        c.drawCircle(TAG_X, TAG_Y, TAG_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(enabled ? Color.WHITE : Color.argb(155, 220, 220, 220));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(enabled ? 17f : 14f);
        paint.setFakeBoldText(true);
        float textY = TAG_Y - (paint.ascent() + paint.descent()) / 2f;
        c.drawText(enabled ? s.tagButtonTitle() : s.tagButtonLabel(), TAG_X, textY, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSuperButton(Canvas c, State s) {
        boolean pressed = s.pressed(Control.SUPER);
        boolean ultra = s.ultraReady();
        boolean enabled = ultra || s.canSuper();

        paint.setColor(
            enabled
                ? (pressed ? s.active().profile.color : Color.argb(170, 42, 16, 68))
                : Color.argb(90, 55, 60, 68)
        );
        c.drawCircle(SUPER_X, SUPER_Y, SUPER_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(enabled ? Color.argb(235, 255, 220, 90) : Color.argb(120, 180, 180, 180));
        c.drawCircle(SUPER_X, SUPER_Y, SUPER_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(enabled ? Color.WHITE : Color.argb(150, 220, 220, 220));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(15f);
        paint.setFakeBoldText(true);
        c.drawText(ultra ? "ULTRA" : "SUPER", SUPER_X, SUPER_Y - 4f, paint);
        paint.setTextSize(11f);
        c.drawText(s.active().superLevelHudLabel, SUPER_X, SUPER_Y + 14f, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}
