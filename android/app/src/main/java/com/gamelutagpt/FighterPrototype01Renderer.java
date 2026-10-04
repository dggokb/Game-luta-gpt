package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Fighter Prototype 01
 *
 * Articulated cel-shaded test character based on the approved concept sheet.
 * Combat still lives in GameView; this class only converts combat state into
 * readable character poses. It is intentionally rig-like so standard motions
 * can be refined joint by joint without rewriting gameplay.
 */
final class FighterPrototype01Renderer {
    static final int GUARD_NONE = 0;
    static final int GUARD_HIGH = 1;
    static final int GUARD_LOW = 2;

    private static final int OUTLINE = Color.rgb(22, 24, 30);
    private static final int SKIN = Color.rgb(214, 151, 108);
    private static final int SKIN_LIGHT = Color.rgb(239, 181, 134);
    private static final int SKIN_DARK = Color.rgb(154, 96, 72);
    private static final int HAIR = Color.rgb(35, 31, 33);
    private static final int HAIR_LIGHT = Color.rgb(66, 55, 51);
    private static final int NAVY = Color.rgb(35, 63, 111);
    private static final int NAVY_DARK = Color.rgb(24, 40, 72);
    private static final int WHITE = Color.rgb(229, 231, 229);
    private static final int WHITE_DARK = Color.rgb(164, 171, 179);
    private static final int BLACK = Color.rgb(35, 36, 40);
    private static final int BLACK_LIGHT = Color.rgb(61, 62, 68);
    private static final int RED = Color.rgb(191, 55, 38);
    private static final int RED_LIGHT = Color.rgb(224, 79, 50);

    private final Path path = new Path();

    private static final class Joint {
        float x;
        float y;
        Joint(float x, float y) {
            this.x = x;
            this.y = y;
        }
        void set(float nx, float ny) {
            x = nx;
            y = ny;
        }
    }

    private final Joint hip = new Joint(0f, -56f);
    private final Joint chest = new Joint(0f, -118f);
    private final Joint head = new Joint(1f, -164f);

    private final Joint rearShoulder = new Joint(-21f, -121f);
    private final Joint rearElbow = new Joint(-34f, -92f);
    private final Joint rearHand = new Joint(-20f, -72f);

    private final Joint frontShoulder = new Joint(23f, -119f);
    private final Joint frontElbow = new Joint(38f, -91f);
    private final Joint frontHand = new Joint(27f, -69f);

    private final Joint rearKnee = new Joint(-18f, -30f);
    private final Joint rearFoot = new Joint(-30f, 0f);
    private final Joint frontKnee = new Joint(20f, -29f);
    private final Joint frontFoot = new Joint(34f, 0f);

    void draw(
        Canvas c,
        Paint paint,
        float x,
        float baseY,
        int stateColor,
        boolean crouching,
        boolean airborne,
        float motionTime,
        String attackType,
        float attackPhase,
        int guardPose,
        boolean superPose,
        boolean hitFlash
    ) {
        buildPose(
            crouching,
            airborne,
            motionTime,
            attackType,
            attackPhase,
            guardPose,
            superPose
        );

        c.save();
        c.translate(x, baseY);

        // Ground shadow belongs to the character so its footprint follows him.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(72, 0, 0, 0));
        c.drawOval(-48f, -7f, 49f, 10f, paint);

        drawBackLeg(c, paint, rearKnee, rearFoot);
        drawBackArm(c, paint, rearShoulder, rearElbow, rearHand);
        drawBody(c, paint);
        drawFrontLeg(c, paint, frontKnee, frontFoot);
        drawFrontArm(c, paint, frontShoulder, frontElbow, frontHand);
        drawHead(c, paint);

        if (hitFlash) {
            paint.setColor(Color.argb(105, 255, 255, 255));
            c.drawOval(-48f, -196f, 62f, 8f, paint);
        } else if (stateColor == Color.rgb(205, 240, 255)) {
            paint.setColor(Color.argb(52, 185, 230, 255));
            c.drawOval(-49f, -195f, 61f, 8f, paint);
        }

        c.restore();
    }

    private void buildPose(
        boolean crouching,
        boolean airborne,
        float motionTime,
        String attackType,
        float attackPhase,
        int guardPose,
        boolean superPose
    ) {
        float walk = (!crouching && !airborne)
            ? (float)Math.sin(motionTime)
            : 0f;
        float breathe = (float)Math.sin(motionTime * 0.45f) * 1.8f;

        hip.set(0f, -56f + breathe);
        chest.set(-2f, -117f + breathe);
        head.set(0f, -162f + breathe);

        rearShoulder.set(-23f, -121f + breathe);
        frontShoulder.set(24f, -119f + breathe);

        rearElbow.set(-36f - 8f * walk, -91f + 4f * walk + breathe);
        rearHand.set(-18f - 8f * walk, -72f + 3f * walk + breathe);
        frontElbow.set(39f + 8f * walk, -89f - 4f * walk + breathe);
        frontHand.set(26f + 8f * walk, -69f - 3f * walk + breathe);

        rearKnee.set(-17f + 15f * walk, -31f);
        rearFoot.set(-31f + 20f * walk, 0f);
        frontKnee.set(19f - 15f * walk, -30f);
        frontFoot.set(35f - 20f * walk, 0f);

        if (airborne) {
            hip.y -= 1f;
            chest.y -= 2f;
            head.y -= 3f;

            rearKnee.set(-22f, -30f);
            rearFoot.set(-38f, -13f);
            frontKnee.set(29f, -38f);
            frontFoot.set(45f, -20f);

            rearElbow.set(-38f, -97f);
            rearHand.set(-22f, -76f);
            frontElbow.set(42f, -95f);
            frontHand.set(30f, -74f);
        }

        if (crouching) {
            hip.set(-2f, -38f);
            chest.set(6f, -87f);
            head.set(10f, -126f);

            rearShoulder.set(-14f, -91f);
            frontShoulder.set(29f, -86f);
            rearElbow.set(-31f, -63f);
            rearHand.set(-17f, -47f);
            frontElbow.set(44f, -58f);
            frontHand.set(30f, -42f);

            rearKnee.set(-34f, -25f);
            rearFoot.set(-47f, 0f);
            frontKnee.set(31f, -19f);
            frontFoot.set(51f, 0f);
        }

        if (guardPose == GUARD_HIGH) {
            rearElbow.set(-38f, chest.y - 5f);
            rearHand.set(-15f, head.y + 18f);
            frontElbow.set(43f, chest.y - 3f);
            frontHand.set(22f, head.y + 14f);
            chest.x = -4f;
        } else if (guardPose == GUARD_LOW) {
            hip.set(-1f, -41f);
            chest.set(7f, -88f);
            head.set(10f, -128f);
            rearShoulder.set(-14f, -91f);
            frontShoulder.set(29f, -87f);
            rearElbow.set(-35f, -59f);
            rearHand.set(-19f, -45f);
            frontElbow.set(45f, -55f);
            frontHand.set(29f, -39f);
            rearKnee.set(-35f, -25f);
            rearFoot.set(-49f, 0f);
            frontKnee.set(34f, -20f);
            frontFoot.set(53f, 0f);
        } else if (superPose) {
            chest.x = -5f;
            rearElbow.set(-47f, -118f);
            rearHand.set(-75f, -101f);
            frontElbow.set(52f, -116f);
            frontHand.set(82f, -98f);
            rearFoot.x = -40f;
            frontFoot.x = 45f;
        }

        if (attackType != null && !attackType.isEmpty()) {
            applyAttackPose(attackType, clamp01(attackPhase), crouching);
        }
    }

    private void applyAttackPose(String type, float p, boolean crouching) {
        // The current combat engine supplies a 0..1 attack phase. For this
        // prototype, joints create readable anticipation/extension/recovery.
        float snap = (float)Math.sin(p * Math.PI);

        if ("L".equals(type)) {
            chest.x = 3f * snap;
            frontElbow.set(48f + 31f * snap, -103f);
            frontHand.set(48f + 71f * snap, -103f);
            rearHand.set(-12f, -86f);
        } else if ("M".equals(type)) {
            chest.x = -4f * snap;
            frontKnee.set(29f + 38f * snap, -42f - 11f * snap);
            frontFoot.set(46f + 92f * snap, -29f - 25f * snap);
            frontHand.set(22f, -84f);
            rearHand.set(-23f, -79f);
        } else if ("H".equals(type)) {
            chest.x = 7f * snap;
            frontElbow.set(47f + 38f * snap, -83f + 18f * snap);
            frontHand.set(47f + 90f * snap, -76f + 26f * snap);
            rearElbow.set(-41f, -107f);
            rearHand.set(-24f, -91f);
        } else if ("2L".equals(type)) {
            frontElbow.set(43f + 28f * snap, -58f);
            frontHand.set(45f + 63f * snap, -52f);
        } else if ("2M".equals(type)) {
            frontKnee.set(37f + 34f * snap, -19f);
            frontFoot.set(53f + 102f * snap, -5f);
        } else if ("2H".equals(type)) {
            chest.x = 7f * snap;
            frontElbow.set(49f + 29f * snap, -75f - 29f * snap);
            frontHand.set(55f + 73f * snap, -65f - 55f * snap);
            rearHand.set(-17f, -57f);
        } else if ("S".equals(type) || "SUPER".equals(type)) {
            rearElbow.set(17f, -101f);
            rearHand.set(43f + 21f * snap, -91f);
            frontElbow.set(47f, -98f);
            frontHand.set(72f + 34f * snap, -91f);
            chest.x = 5f * snap;
        }
    }

    private void drawBackLeg(Canvas c, Paint p, Joint knee, Joint foot) {
        drawLimb(c, p, hip.x - 12f, hip.y + 6f, knee.x, knee.y,
            22f, OUTLINE, WHITE_DARK);
        drawLimb(c, p, knee.x, knee.y, foot.x, foot.y - 7f,
            19f, OUTLINE, WHITE_DARK);
        drawShoe(c, p, foot.x, foot.y, false);
    }

    private void drawFrontLeg(Canvas c, Paint p, Joint knee, Joint foot) {
        drawLimb(c, p, hip.x + 13f, hip.y + 6f, knee.x, knee.y,
            25f, OUTLINE, WHITE);
        drawLimb(c, p, knee.x, knee.y, foot.x, foot.y - 7f,
            21f, OUTLINE, WHITE);
        drawPantsShadowStripe(c, p, knee, foot);
        drawShoe(c, p, foot.x, foot.y, true);
    }

    private void drawBackArm(Canvas c, Paint p, Joint shoulder, Joint elbow, Joint hand) {
        drawLimb(c, p, shoulder.x, shoulder.y, elbow.x, elbow.y,
            17f, OUTLINE, SKIN_DARK);
        drawLimb(c, p, elbow.x, elbow.y, hand.x, hand.y,
            15f, OUTLINE, SKIN_DARK);
        drawGlove(c, p, hand.x, hand.y, false);
    }

    private void drawFrontArm(Canvas c, Paint p, Joint shoulder, Joint elbow, Joint hand) {
        drawLimb(c, p, shoulder.x, shoulder.y, elbow.x, elbow.y,
            19f, OUTLINE, SKIN);
        drawLimb(c, p, elbow.x, elbow.y, hand.x, hand.y,
            17f, OUTLINE, SKIN);
        drawArmHighlight(c, p, shoulder, elbow);
        drawGlove(c, p, hand.x, hand.y, true);
    }

    private void drawBody(Canvas c, Paint p) {
        // Waist / black sash.
        drawTorsoPanel(c, p,
            hip.x - 29f, hip.y + 5f,
            hip.x + 30f, hip.y - 10f,
            BLACK, OUTLINE);

        // Black undershirt.
        path.reset();
        path.moveTo(chest.x - 28f, chest.y + 2f);
        path.lineTo(chest.x + 29f, chest.y + 2f);
        path.lineTo(hip.x + 24f, hip.y - 7f);
        path.lineTo(hip.x - 24f, hip.y - 7f);
        path.close();
        fillOutline(c, p, path, BLACK, OUTLINE, 3f);

        // Navy sleeveless vest - two separate panels so black shirt remains visible.
        path.reset();
        path.moveTo(chest.x - 36f, chest.y - 8f);
        path.lineTo(chest.x - 10f, chest.y - 3f);
        path.lineTo(hip.x - 8f, hip.y - 7f);
        path.lineTo(hip.x - 30f, hip.y - 6f);
        path.close();
        fillOutline(c, p, path, NAVY, OUTLINE, 3f);

        path.reset();
        path.moveTo(chest.x + 10f, chest.y - 3f);
        path.lineTo(chest.x + 37f, chest.y - 7f);
        path.lineTo(hip.x + 30f, hip.y - 6f);
        path.lineTo(hip.x + 8f, hip.y - 7f);
        path.close();
        fillOutline(c, p, path, NAVY, OUTLINE, 3f);

        // White vest side panels from concept.
        p.setStyle(Paint.Style.FILL);
        p.setColor(WHITE);
        c.drawRoundRect(
            chest.x - 29f, chest.y + 2f,
            chest.x - 19f, hip.y - 14f,
            4f, 4f, p
        );
        c.drawRoundRect(
            chest.x + 19f, chest.y + 2f,
            chest.x + 29f, hip.y - 14f,
            4f, 4f, p
        );

        // Red/orange collar accent.
        p.setColor(RED);
        path.reset();
        path.moveTo(chest.x + 15f, chest.y - 8f);
        path.lineTo(chest.x + 31f, chest.y - 17f);
        path.lineTo(chest.x + 27f, chest.y + 2f);
        path.close();
        c.drawPath(path, p);

        // Waist sash tails.
        p.setColor(BLACK);
        c.drawRect(hip.x - 25f, hip.y - 2f, hip.x + 28f, hip.y + 8f, p);
        p.setColor(RED);
        path.reset();
        path.moveTo(hip.x + 15f, hip.y + 6f);
        path.lineTo(hip.x + 25f, hip.y + 7f);
        path.lineTo(hip.x + 34f, hip.y + 43f);
        path.lineTo(hip.x + 24f, hip.y + 38f);
        path.close();
        c.drawPath(path, p);
    }

    private void drawHead(Canvas c, Paint p) {
        // Neck.
        drawLimb(c, p, chest.x, chest.y - 5f, head.x, head.y + 25f,
            15f, OUTLINE, SKIN_DARK);

        // Face.
        p.setStyle(Paint.Style.FILL);
        p.setColor(SKIN_DARK);
        c.drawOval(head.x - 24f, head.y - 24f, head.x + 29f, head.y + 30f, p);
        p.setColor(SKIN);
        c.drawOval(head.x - 21f, head.y - 26f, head.x + 24f, head.y + 26f, p);
        p.setColor(SKIN_LIGHT);
        c.drawOval(head.x - 14f, head.y - 20f, head.x + 5f, head.y + 4f, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(OUTLINE);
        c.drawOval(head.x - 21f, head.y - 26f, head.x + 24f, head.y + 26f, p);

        // Brows / focused anime eyes.
        p.setStrokeWidth(3f);
        c.drawLine(head.x - 12f, head.y - 5f, head.x - 2f, head.y - 8f, p);
        c.drawLine(head.x + 6f, head.y - 8f, head.x + 16f, head.y - 5f, p);
        p.setStrokeWidth(2f);
        c.drawLine(head.x - 10f, head.y, head.x - 2f, head.y, p);
        c.drawLine(head.x + 7f, head.y, head.x + 15f, head.y, p);

        // Hair mass + spikes.
        p.setStyle(Paint.Style.FILL);
        p.setColor(HAIR);
        c.drawOval(head.x - 25f, head.y - 35f, head.x + 25f, head.y - 4f, p);
        for (int i = 0; i < 7; i++) {
            float sx = head.x - 22f + i * 7f;
            float tipX = sx + (i - 3) * 2.2f;
            float tipY = head.y - 48f - (i % 3) * 5f;
            path.reset();
            path.moveTo(sx - 7f, head.y - 19f);
            path.lineTo(tipX, tipY);
            path.lineTo(sx + 8f, head.y - 17f);
            path.close();
            c.drawPath(path, p);
        }

        p.setColor(HAIR_LIGHT);
        path.reset();
        path.moveTo(head.x - 17f, head.y - 29f);
        path.lineTo(head.x - 4f, head.y - 43f);
        path.lineTo(head.x + 7f, head.y - 28f);
        path.close();
        c.drawPath(path, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(OUTLINE);
        c.drawOval(head.x - 25f, head.y - 35f, head.x + 25f, head.y - 4f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawArmHighlight(Canvas c, Paint p, Joint a, Joint b) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(6f);
        p.setColor(SKIN_LIGHT);
        c.drawLine(
            a.x + 3f, a.y - 1f,
            b.x + 3f, b.y - 2f,
            p
        );
        p.setStyle(Paint.Style.FILL);
    }

    private void drawPantsShadowStripe(Canvas c, Paint p, Joint knee, Joint foot) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(6f);
        p.setColor(NAVY);
        c.drawLine(knee.x + 7f, knee.y, foot.x + 7f, foot.y - 8f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawGlove(Canvas c, Paint p, float x, float y, boolean front) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(OUTLINE);
        c.drawCircle(x, y, 12f, p);
        p.setColor(front ? BLACK_LIGHT : BLACK);
        c.drawCircle(x, y, 9f, p);
        p.setColor(front ? RED_LIGHT : RED);
        c.drawRect(x - 10f, y + 5f, x + 10f, y + 10f, p);
    }

    private void drawShoe(Canvas c, Paint p, float x, float y, boolean front) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(OUTLINE);
        c.drawRoundRect(x - 16f, y - 14f, x + 20f, y + 5f, 7f, 7f, p);
        p.setColor(front ? WHITE : WHITE_DARK);
        c.drawRoundRect(x - 13f, y - 11f, x + 17f, y + 2f, 6f, 6f, p);
        p.setColor(NAVY_DARK);
        c.drawRect(x - 9f, y - 7f, x + 6f, y - 3f, p);
        p.setColor(RED);
        c.drawRect(x + 7f, y - 8f, x + 13f, y + 1f, p);
    }

    private void drawLimb(
        Canvas c,
        Paint p,
        float x1,
        float y1,
        float x2,
        float y2,
        float width,
        int outline,
        int fill
    ) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);

        p.setStrokeWidth(width + 6f);
        p.setColor(outline);
        c.drawLine(x1, y1, x2, y2, p);

        p.setStrokeWidth(width);
        p.setColor(fill);
        c.drawLine(x1, y1, x2, y2, p);

        // Hard cel shade on lower/right side.
        p.setStrokeWidth(Math.max(4f, width * 0.25f));
        p.setColor(shade(fill, 0.70f));
        c.drawLine(x1 + 3f, y1 + 2f, x2 + 3f, y2 + 2f, p);

        p.setStyle(Paint.Style.FILL);
    }

    private void drawTorsoPanel(
        Canvas c,
        Paint p,
        float left,
        float top,
        float right,
        float bottom,
        int fill,
        int outline
    ) {
        float l = Math.min(left, right);
        float r = Math.max(left, right);
        float t = Math.min(top, bottom);
        float b = Math.max(top, bottom);

        p.setStyle(Paint.Style.FILL);
        p.setColor(outline);
        c.drawRoundRect(l - 3f, t - 3f, r + 3f, b + 3f, 7f, 7f, p);
        p.setColor(fill);
        c.drawRoundRect(l, t, r, b, 6f, 6f, p);
    }

    private void fillOutline(
        Canvas c,
        Paint p,
        Path shape,
        int fill,
        int outline,
        float stroke
    ) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        c.drawPath(shape, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(stroke);
        p.setColor(outline);
        c.drawPath(shape, p);
        p.setStyle(Paint.Style.FILL);
    }

    private int shade(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, Math.max(0, Math.round(Color.red(color) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(Color.green(color) * factor)));
        int b = Math.min(255, Math.max(0, Math.round(Color.blue(color) * factor)));
        return Color.argb(a, r, g, b);
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
