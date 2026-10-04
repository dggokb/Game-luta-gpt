package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Fighter Prototype 01
 *
 * Rig visual dedicado ao personagem aprovado no concept sheet.
 * O renderer nao decide gameplay. Ele recebe um FighterAnimationState e
 * converte esse estado em pose do corpo/roupa/cabeca.
 */
final class FighterPrototype01Renderer {
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

    private final Joint hip = new Joint(0f, -61f);
    private final Joint chest = new Joint(-2f, -126f);
    private final Joint head = new Joint(1f, -177f);

    private final Joint rearShoulder = new Joint(-26f, -130f);
    private final Joint rearElbow = new Joint(-42f, -96f);
    private final Joint rearHand = new Joint(-22f, -73f);

    private final Joint frontShoulder = new Joint(27f, -128f);
    private final Joint frontElbow = new Joint(43f, -94f);
    private final Joint frontHand = new Joint(30f, -71f);

    private final Joint rearKnee = new Joint(-20f, -32f);
    private final Joint rearFoot = new Joint(-34f, 0f);
    private final Joint frontKnee = new Joint(22f, -31f);
    private final Joint frontFoot = new Joint(38f, 0f);

    void draw(
        Canvas c,
        Paint paint,
        float x,
        float baseY,
        FighterAnimationState state,
        float stateTime,
        float actionPhase,
        boolean hitFlash
    ) {
        buildPose(state, stateTime, clamp01(actionPhase));

        c.save();
        c.translate(x, baseY);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(68, 0, 0, 0));
        c.drawOval(-54f, -8f, 55f, 11f, paint);

        drawBackLeg(c, paint, rearKnee, rearFoot);
        drawBackArm(c, paint, rearShoulder, rearElbow, rearHand);
        drawBody(c, paint);
        drawFrontLeg(c, paint, frontKnee, frontFoot);
        drawFrontArm(c, paint, frontShoulder, frontElbow, frontHand);
        drawHead(c, paint);

        if (hitFlash) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(112, 255, 255, 255));
            c.drawOval(-57f, -214f, 67f, 9f, paint);
        }

        c.restore();
    }

    private void buildPose(
        FighterAnimationState state,
        float time,
        float phase
    ) {
        resetNeutralPose(time);

        switch (state) {
            case IDLE:
                applyIdle(time);
                break;
            case WALK_FORWARD:
                applyWalk(time, 1f);
                break;
            case WALK_BACKWARD:
                applyWalk(time, -1f);
                break;
            case DASH_FORWARD:
                applyDash(false, time);
                break;
            case BACKDASH:
                applyDash(true, time);
                break;
            case CROUCH:
                applyCrouch();
                break;
            case JUMP_RISE:
                applyJump(false, true);
                break;
            case JUMP_FALL:
                applyJump(false, false);
                break;
            case SUPER_JUMP_RISE:
                applyJump(true, true);
                break;
            case SUPER_JUMP_FALL:
                applyJump(true, false);
                break;
            case ATTACK_L:
                applyAttackPose("L", phase, false, false);
                break;
            case ATTACK_M:
                applyAttackPose("M", phase, false, false);
                break;
            case ATTACK_H:
                applyAttackPose("H", phase, false, false);
                break;
            case ATTACK_2L:
                applyCrouch();
                applyAttackPose("2L", phase, true, false);
                break;
            case ATTACK_2M:
                applyCrouch();
                applyAttackPose("2M", phase, true, false);
                break;
            case ATTACK_2H:
                applyCrouch();
                applyAttackPose("2H", phase, true, false);
                break;
            case AIR_ATTACK_L:
                applyJump(false, phase < 0.5f);
                applyAttackPose("L", phase, false, true);
                break;
            case AIR_ATTACK_M:
                applyJump(false, phase < 0.5f);
                applyAttackPose("M", phase, false, true);
                break;
            case AIR_ATTACK_H:
                applyJump(true, phase < 0.5f);
                applyAttackPose("H", phase, false, true);
                break;
            case SPECIAL:
                applySpecial(phase, false);
                break;
            case AIR_SPECIAL:
                applyJump(false, false);
                applySpecial(phase, true);
                break;
            case SUPER:
                applySuper(time, phase);
                break;
            case GUARD_HIGH:
                applyGuardHigh();
                break;
            case GUARD_LOW:
                applyCrouch();
                applyGuardLow();
                break;
            case HIT:
                applyHit(time);
                break;
            case KNOCKDOWN_FALL:
                applyKnockdownFall(phase);
                break;
            case KNOCKDOWN_DOWN:
                applyKnockdownDown();
                break;
            case GET_UP:
                applyGetUp(phase);
                break;
            case TAG:
                applyTag(time);
                break;
        }
    }

    private void resetNeutralPose(float time) {
        float breathe = (float)Math.sin(time * 3.2f) * 1.5f;

        hip.set(0f, -61f + breathe);
        chest.set(-2f, -126f + breathe);
        head.set(1f, -177f + breathe);

        rearShoulder.set(-26f, -130f + breathe);
        rearElbow.set(-42f, -96f + breathe);
        rearHand.set(-22f, -73f + breathe);

        frontShoulder.set(27f, -128f + breathe);
        frontElbow.set(43f, -94f + breathe);
        frontHand.set(30f, -71f + breathe);

        rearKnee.set(-20f, -32f);
        rearFoot.set(-34f, 0f);
        frontKnee.set(22f, -31f);
        frontFoot.set(38f, 0f);
    }

    private void applyIdle(float time) {
        float sway = (float)Math.sin(time * 2.2f);
        chest.x = -3f + sway * 1.8f;
        head.x = 1f + sway * 0.8f;
        frontHand.y -= 2f * sway;
        rearHand.y += 2f * sway;
    }

    private void applyWalk(float time, float direction) {
        float cycle = (float)Math.sin(time * 9.5f) * direction;
        float lift = Math.abs((float)Math.sin(time * 9.5f));

        chest.x += 3f * cycle;
        head.x += 2f * cycle;

        rearKnee.set(-20f + 18f * cycle, -31f - 5f * lift);
        rearFoot.set(-35f + 27f * cycle, -2f - 8f * Math.max(0f, cycle));
        frontKnee.set(22f - 18f * cycle, -31f - 5f * lift);
        frontFoot.set(39f - 27f * cycle, -2f - 8f * Math.max(0f, -cycle));

        rearElbow.x -= 11f * cycle;
        rearHand.x -= 14f * cycle;
        frontElbow.x += 11f * cycle;
        frontHand.x += 14f * cycle;
    }

    private void applyDash(boolean backward, float time) {
        float dir = backward ? -1f : 1f;
        chest.x += 13f * dir;
        head.x += 11f * dir;
        hip.x += 6f * dir;

        rearFoot.x -= 17f * dir;
        frontFoot.x += 21f * dir;

        rearElbow.x -= 16f * dir;
        rearHand.x -= 26f * dir;
        frontElbow.x -= 8f * dir;
        frontHand.x -= 16f * dir;

        float pulse = (float)Math.sin(time * 18f) * 2f;
        head.y += pulse;
    }

    private void applyCrouch() {
        hip.set(-2f, -41f);
        chest.set(7f, -94f);
        head.set(11f, -139f);

        rearShoulder.set(-17f, -98f);
        frontShoulder.set(32f, -93f);
        rearElbow.set(-36f, -67f);
        rearHand.set(-18f, -48f);
        frontElbow.set(49f, -63f);
        frontHand.set(33f, -44f);

        rearKnee.set(-39f, -26f);
        rearFoot.set(-53f, 0f);
        frontKnee.set(35f, -20f);
        frontFoot.set(57f, 0f);
    }

    private void applyJump(boolean superJump, boolean rising) {
        float compact = rising ? 1f : 0.55f;
        float spread = superJump ? 1.25f : 1f;

        hip.x = rising ? -3f : 4f;
        chest.x = rising ? -6f : 5f;
        head.x = rising ? -7f : 5f;

        rearKnee.set(-24f * spread, -35f - 6f * compact);
        rearFoot.set(-46f * spread, -13f - 11f * compact);
        frontKnee.set(32f * spread, -42f + 7f * compact);
        frontFoot.set(51f * spread, -21f + 10f * compact);

        rearElbow.set(-43f, -104f);
        rearHand.set(-25f, -82f);
        frontElbow.set(48f, -101f);
        frontHand.set(34f, -78f);

        if (superJump) {
            rearElbow.x -= 10f;
            frontElbow.x += 10f;
            rearFoot.x -= 8f;
            frontFoot.x += 8f;
        }

        if (!rising) {
            rearHand.y += 7f;
            frontHand.y += 7f;
            rearFoot.y += 7f;
            frontFoot.y += 5f;
        }
    }

    private void applyGuardHigh() {
        chest.x = -6f;
        head.x = -3f;
        rearElbow.set(-43f, -130f);
        rearHand.set(-15f, -159f);
        frontElbow.set(49f, -126f);
        frontHand.set(23f, -162f);
        rearFoot.x = -40f;
        frontFoot.x = 44f;
    }

    private void applyGuardLow() {
        rearElbow.set(-40f, -68f);
        rearHand.set(-20f, -51f);
        frontElbow.set(50f, -63f);
        frontHand.set(30f, -45f);
        rearFoot.x = -55f;
        frontFoot.x = 60f;
    }

    private void applyHit(float time) {
        float recoil = Math.min(1f, time * 10f);
        chest.x = -13f * recoil;
        head.x = -18f * recoil;
        hip.x = -5f * recoil;

        rearElbow.set(-58f, -111f);
        rearHand.set(-76f, -83f);
        frontElbow.set(25f, -104f);
        frontHand.set(7f, -76f);

        rearFoot.x = -42f;
        frontFoot.x = 43f;
    }

    private void applyKnockdownFall(float phase) {
        float t = clamp01(phase);
        chest.x -= 12f * t;
        head.x -= 20f * t;
        rearElbow.set(-55f, -93f);
        rearHand.set(-72f, -58f);
        frontElbow.set(29f, -76f);
        frontHand.set(46f, -49f);
    }

    private void applyKnockdownDown() {
        rearElbow.set(-47f, -88f);
        rearHand.set(-61f, -60f);
        frontElbow.set(39f, -84f);
        frontHand.set(58f, -58f);
        rearKnee.x = -31f;
        frontKnee.x = 35f;
    }

    private void applyGetUp(float phase) {
        float t = clamp01(phase);
        applyCrouch();
        hip.y -= 18f * t;
        chest.y -= 25f * t;
        head.y -= 34f * t;
        rearKnee.x += 13f * t;
        frontKnee.x -= 8f * t;
    }

    private void applyTag(float time) {
        float pulse = (float)Math.sin(time * 9f) * 3f;
        rearElbow.set(-51f, -123f + pulse);
        rearHand.set(-75f, -103f + pulse);
        frontElbow.set(56f, -121f - pulse);
        frontHand.set(82f, -100f - pulse);
        rearFoot.x = -43f;
        frontFoot.x = 48f;
    }

    private void applySpecial(float phase, boolean airborne) {
        if (airborne) {
            rearKnee.set(-27f, -35f);
            rearFoot.set(-45f, -12f);
            frontKnee.set(31f, -39f);
            frontFoot.set(50f, -17f);
        }

        float snap = (float)Math.sin(phase * Math.PI);
        chest.x = 6f * snap;
        rearElbow.set(19f, -108f);
        rearHand.set(48f + 22f * snap, -98f);
        frontElbow.set(51f, -105f);
        frontHand.set(78f + 37f * snap, -98f);
    }

    private void applySuper(float time, float phase) {
        float pulse = 1f + 0.07f * (float)Math.sin(time * 24f);
        chest.x = -5f;
        rearElbow.set(-53f, -126f);
        rearHand.set(-82f * pulse, -105f);
        frontElbow.set(59f, -124f);
        frontHand.set(91f * pulse, -102f);
        rearFoot.x = -45f;
        frontFoot.x = 51f;

        if (phase > 0.55f) {
            frontHand.x += 22f * phase;
            rearHand.x += 17f * phase;
        }
    }

    private void applyAttackPose(
        String type,
        float p,
        boolean crouching,
        boolean airborne
    ) {
        float snap = (float)Math.sin(p * Math.PI);

        if ("L".equals(type)) {
            chest.x = 4f * snap;
            frontElbow.set(50f + 34f * snap, -109f);
            frontHand.set(51f + 82f * snap, -108f);
            rearHand.set(-14f, -92f);

            if (airborne) {
                frontHand.y += 9f;
                frontElbow.y += 7f;
            }
        } else if ("M".equals(type)) {
            chest.x = -5f * snap;
            frontKnee.set(31f + 42f * snap, -45f - 12f * snap);
            frontFoot.set(49f + 106f * snap, -31f - 28f * snap);
            frontHand.set(24f, -91f);
            rearHand.set(-25f, -85f);

            if (airborne) {
                frontFoot.y -= 10f;
                rearFoot.y -= 6f;
            }
        } else if ("H".equals(type)) {
            chest.x = 8f * snap;
            frontElbow.set(52f + 42f * snap, -87f + 20f * snap);
            frontHand.set(54f + 104f * snap, -79f + 29f * snap);
            rearElbow.set(-46f, -114f);
            rearHand.set(-27f, -98f);

            if (airborne) {
                frontHand.y += 18f * snap;
                frontFoot.x += 18f * snap;
            }
        } else if ("2L".equals(type)) {
            frontElbow.set(48f + 31f * snap, -63f);
            frontHand.set(50f + 72f * snap, -56f);
        } else if ("2M".equals(type)) {
            frontKnee.set(40f + 38f * snap, -20f);
            frontFoot.set(58f + 115f * snap, -5f);
        } else if ("2H".equals(type)) {
            chest.x = 8f * snap;
            frontElbow.set(53f + 32f * snap, -81f - 32f * snap);
            frontHand.set(59f + 83f * snap, -69f - 63f * snap);
            rearHand.set(-18f, -62f);
        }
    }

    private void drawBackLeg(Canvas c, Paint p, Joint knee, Joint foot) {
        drawLimb(c, p, hip.x - 14f, hip.y + 7f, knee.x, knee.y,
            24f, OUTLINE, WHITE_DARK);
        drawLimb(c, p, knee.x, knee.y, foot.x, foot.y - 8f,
            21f, OUTLINE, WHITE_DARK);
        drawShoe(c, p, foot.x, foot.y, false);
    }

    private void drawFrontLeg(Canvas c, Paint p, Joint knee, Joint foot) {
        drawLimb(c, p, hip.x + 15f, hip.y + 7f, knee.x, knee.y,
            28f, OUTLINE, WHITE);
        drawLimb(c, p, knee.x, knee.y, foot.x, foot.y - 8f,
            23f, OUTLINE, WHITE);
        drawPantsShadowStripe(c, p, knee, foot);
        drawShoe(c, p, foot.x, foot.y, true);
    }

    private void drawBackArm(Canvas c, Paint p, Joint shoulder, Joint elbow, Joint hand) {
        drawLimb(c, p, shoulder.x, shoulder.y, elbow.x, elbow.y,
            19f, OUTLINE, SKIN_DARK);
        drawLimb(c, p, elbow.x, elbow.y, hand.x, hand.y,
            17f, OUTLINE, SKIN_DARK);
        drawGlove(c, p, hand.x, hand.y, false);
    }

    private void drawFrontArm(Canvas c, Paint p, Joint shoulder, Joint elbow, Joint hand) {
        drawLimb(c, p, shoulder.x, shoulder.y, elbow.x, elbow.y,
            21f, OUTLINE, SKIN);
        drawLimb(c, p, elbow.x, elbow.y, hand.x, hand.y,
            19f, OUTLINE, SKIN);
        drawArmHighlight(c, p, shoulder, elbow);
        drawGlove(c, p, hand.x, hand.y, true);
    }

    private void drawBody(Canvas c, Paint p) {
        drawTorsoPanel(c, p,
            hip.x - 31f, hip.y + 5f,
            hip.x + 32f, hip.y - 11f,
            BLACK, OUTLINE);

        path.reset();
        path.moveTo(chest.x - 30f, chest.y + 3f);
        path.lineTo(chest.x + 31f, chest.y + 3f);
        path.lineTo(hip.x + 26f, hip.y - 8f);
        path.lineTo(hip.x - 26f, hip.y - 8f);
        path.close();
        fillOutline(c, p, path, BLACK, OUTLINE, 3.2f);

        path.reset();
        path.moveTo(chest.x - 39f, chest.y - 10f);
        path.lineTo(chest.x - 11f, chest.y - 4f);
        path.lineTo(hip.x - 9f, hip.y - 8f);
        path.lineTo(hip.x - 32f, hip.y - 7f);
        path.close();
        fillOutline(c, p, path, NAVY, OUTLINE, 3.2f);

        path.reset();
        path.moveTo(chest.x + 11f, chest.y - 4f);
        path.lineTo(chest.x + 40f, chest.y - 9f);
        path.lineTo(hip.x + 32f, hip.y - 7f);
        path.lineTo(hip.x + 9f, hip.y - 8f);
        path.close();
        fillOutline(c, p, path, NAVY, OUTLINE, 3.2f);

        p.setStyle(Paint.Style.FILL);
        p.setColor(WHITE);
        c.drawRoundRect(
            chest.x - 31f, chest.y + 3f,
            chest.x - 20f, hip.y - 16f,
            4f, 4f, p
        );
        c.drawRoundRect(
            chest.x + 20f, chest.y + 3f,
            chest.x + 31f, hip.y - 16f,
            4f, 4f, p
        );

        p.setColor(RED);
        path.reset();
        path.moveTo(chest.x + 16f, chest.y - 10f);
        path.lineTo(chest.x + 34f, chest.y - 20f);
        path.lineTo(chest.x + 29f, chest.y + 3f);
        path.close();
        c.drawPath(path, p);

        p.setColor(BLACK);
        c.drawRect(hip.x - 27f, hip.y - 3f, hip.x + 30f, hip.y + 9f, p);

        p.setColor(RED);
        path.reset();
        path.moveTo(hip.x + 16f, hip.y + 7f);
        path.lineTo(hip.x + 27f, hip.y + 8f);
        path.lineTo(hip.x + 38f, hip.y + 47f);
        path.lineTo(hip.x + 26f, hip.y + 42f);
        path.close();
        c.drawPath(path, p);
    }

    private void drawHead(Canvas c, Paint p) {
        drawLimb(c, p, chest.x, chest.y - 6f, head.x, head.y + 28f,
            16f, OUTLINE, SKIN_DARK);

        p.setStyle(Paint.Style.FILL);
        p.setColor(SKIN_DARK);
        c.drawOval(head.x - 27f, head.y - 27f, head.x + 31f, head.y + 32f, p);

        p.setColor(SKIN);
        c.drawOval(head.x - 24f, head.y - 29f, head.x + 27f, head.y + 28f, p);

        p.setColor(SKIN_LIGHT);
        c.drawOval(head.x - 16f, head.y - 22f, head.x + 6f, head.y + 5f, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.2f);
        p.setColor(OUTLINE);
        c.drawOval(head.x - 24f, head.y - 29f, head.x + 27f, head.y + 28f, p);

        p.setStrokeWidth(3f);
        c.drawLine(head.x - 14f, head.y - 6f, head.x - 3f, head.y - 9f, p);
        c.drawLine(head.x + 7f, head.y - 9f, head.x + 18f, head.y - 6f, p);

        p.setStrokeWidth(2.2f);
        c.drawLine(head.x - 11f, head.y, head.x - 2f, head.y, p);
        c.drawLine(head.x + 8f, head.y, head.x + 17f, head.y, p);
        c.drawLine(head.x - 1f, head.y + 13f, head.x + 9f, head.y + 13f, p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(HAIR);
        c.drawOval(head.x - 29f, head.y - 40f, head.x + 29f, head.y - 5f, p);

        for (int i = 0; i < 9; i++) {
            float sx = head.x - 27f + i * 6.5f;
            float tipX = sx + (i - 4) * 2.5f;
            float tipY = head.y - 55f - (i % 4) * 4f;

            path.reset();
            path.moveTo(sx - 8f, head.y - 21f);
            path.lineTo(tipX, tipY);
            path.lineTo(sx + 9f, head.y - 19f);
            path.close();
            c.drawPath(path, p);
        }

        p.setColor(HAIR_LIGHT);
        path.reset();
        path.moveTo(head.x - 19f, head.y - 33f);
        path.lineTo(head.x - 5f, head.y - 49f);
        path.lineTo(head.x + 8f, head.y - 32f);
        path.close();
        c.drawPath(path, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.2f);
        p.setColor(OUTLINE);
        c.drawOval(head.x - 29f, head.y - 40f, head.x + 29f, head.y - 5f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawArmHighlight(Canvas c, Paint p, Joint a, Joint b) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(6f);
        p.setColor(SKIN_LIGHT);
        c.drawLine(a.x + 4f, a.y - 2f, b.x + 4f, b.y - 3f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawPantsShadowStripe(Canvas c, Paint p, Joint knee, Joint foot) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(7f);
        p.setColor(NAVY);
        c.drawLine(knee.x + 8f, knee.y, foot.x + 8f, foot.y - 9f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawGlove(Canvas c, Paint p, float x, float y, boolean front) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(OUTLINE);
        c.drawCircle(x, y, 13f, p);
        p.setColor(front ? BLACK_LIGHT : BLACK);
        c.drawCircle(x, y, 10f, p);
        p.setColor(front ? RED_LIGHT : RED);
        c.drawRect(x - 11f, y + 5f, x + 11f, y + 11f, p);
    }

    private void drawShoe(Canvas c, Paint p, float x, float y, boolean front) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(OUTLINE);
        c.drawRoundRect(x - 18f, y - 15f, x + 22f, y + 6f, 7f, 7f, p);
        p.setColor(front ? WHITE : WHITE_DARK);
        c.drawRoundRect(x - 15f, y - 12f, x + 19f, y + 3f, 6f, 6f, p);
        p.setColor(NAVY_DARK);
        c.drawRect(x - 10f, y - 8f, x + 7f, y - 4f, p);
        p.setColor(RED);
        c.drawRect(x + 8f, y - 9f, x + 14f, y + 2f, p);
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
