package com.gamelutagpt;

import static com.gamelutagpt.ControlsLayout.*;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Screen-space HUD and touch controls plus the opponent's overhead panel. It reads
 * everything through {@link State}; it never changes game state.
 */
final class HudRenderer {
    /** Read-only view of what the HUD shows. */
    interface State {
        FighterState active();
        FighterState reserve();
        String versionLabel();
        String stateLabel();
        int facing();
        boolean aiEnabled();
        boolean debugEnabled();
        /** The pair on the player's side, as "p01+p03". */
        String teamLabel();
        /** 0 while tagging, rising to 1 when the tag is ready. */
        float tagReadyRatio();
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

    private void reset() {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    void drawHud(Canvas c, State s) {
        reset();
        FighterState active = s.active();
        FighterState reserve = s.reserve();

        paint.setColor(Color.argb(185, 10, 15, 27));
        c.drawRoundRect(32, 28, 560, 234, 18, 18, paint);

        paint.setColor(active.profile.color);
        c.drawCircle(78, 74, 29, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.WHITE);
        c.drawCircle(78, 74, 29, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setFakeBoldText(true);
        c.drawText(active.hudTitle, 122, 58, paint);
        paint.setFakeBoldText(false);

        drawLifeBar(c, active, 122f, 70f, 525f, 98f, true);
        drawSuperMeter(c, active, 122f, 106f, 525f, 118f, true);
        drawTagCooldown(c, s, 122f, 139f, 525f, 149f);

        // Reserva: indicador menor com vida e Super próprios.
        paint.setColor(reserve.profile.color);
        c.drawCircle(78, 187, 14, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        c.drawText(reserve.reserveHudLabel, 105, 184, paint);
        paint.setFakeBoldText(false);

        drawLifeBar(c, reserve, 105f, 193f, 525f, 206f, false);
        drawSuperMeter(c, reserve, 105f, 213f, 525f, 223f, false);

        paint.setColor(Color.argb(180, 10, 15, 27));
        c.drawRoundRect(945, 28, 1248, 112, 18, 18, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        c.drawText(s.versionLabel(), 975, 59, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(16);

        c.drawText("Estado:", 975, 88, paint);
        c.drawText(s.stateLabel(), 1032, 88, paint);

        paint.setTextSize(14f);
        c.drawText(s.facing() > 0 ? "FACING: →" : "FACING: ←", 975, 106, paint);

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
        drawDemos(c, s);
    }

    /** One button per version demo, and the caption of the demo that plays. */
    private void drawDemos(Canvas c, State s) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(13f);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        float right = demoLeft(DemoDirector.VERSIONS.length - 1) + DEMO_WIDTH;
        c.drawText("DEMOS (toque de novo para parar)", (DEMO_LEFT + right) * 0.5f, DEMO_TOP - 8f, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < DemoDirector.VERSIONS.length; i++) {
            float left = demoLeft(i);
            drawToggle(c, s.demoPlaying() == i, DemoDirector.VERSIONS[i], left, DEMO_TOP, left + DEMO_WIDTH, DEMO_BOTTOM, 18f);
        }
        if (s.demoPlaying() < 0) return;
        float boxLeft = 250f, boxRight = 1030f, boxTop = 244f, boxBottom = 302f;
        paint.setColor(Color.argb(200, 10, 15, 27));
        c.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 14f, 14f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.rgb(74, 205, 232));
        c.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 14f, 14f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        float cx = (boxLeft + boxRight) * 0.5f;
        paint.setColor(Color.rgb(255, 226, 120));
        paint.setFakeBoldText(true);
        paint.setTextSize(19f);
        c.drawText(s.demoTitle(), cx, boxTop + 24f, paint);
        paint.setFakeBoldText(false);
        paint.setColor(Color.WHITE);
        paint.setTextSize(16f);
        c.drawText(s.demoCaption(), cx, boxTop + 47f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
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
