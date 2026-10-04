package com.gamelutagpt;

/**
 * Procedural baseline animation library for the standard rig.
 *
 * The combat engine owns timing; this animator only maps live gameplay state to
 * reusable bone rotations. This keeps animation replacement independent from
 * hitboxes and frame rules.
 */
final class FighterPrototype01Animator {
    private final FighterPrototype01Rig.Pose previous = new FighterPrototype01Rig.Pose();
    private float lastTime = -1f;

    void apply(Fighter3DState state, FighterPrototype01Rig rig) {
        FighterPrototype01Rig.Pose p = rig.pose;
        p.reset();

        float walk = (float)Math.sin(state.walkTime);
        float breathe = (float)Math.sin(state.animationTime * 2.6f);
        float attack = clamp01(state.attackPhase);
        attack = attack * attack * (3f - 2f * attack);

        // Bring the rear forearm in front of the vest, rather than through the torso.
        p.tz[FighterPrototype01Rig.L_UPPER_ARM] = 17f;
        p.rx[FighterPrototype01Rig.L_UPPER_ARM] = 32f;
        // Base fighting stance.
        p.ry[FighterPrototype01Rig.ROOT] = 7f;
        p.rz[FighterPrototype01Rig.PELVIS] = 2f;
        p.rz[FighterPrototype01Rig.SPINE] = -2f;
        p.ry[FighterPrototype01Rig.CHEST] = 8f;

        p.rz[FighterPrototype01Rig.L_UPPER_ARM] = -48f;
        p.rz[FighterPrototype01Rig.L_FOREARM] = -118f;
        p.rz[FighterPrototype01Rig.R_UPPER_ARM] = -42f;
        p.rz[FighterPrototype01Rig.R_FOREARM] = -135f;

        p.rz[FighterPrototype01Rig.L_THIGH] = 14f;
        p.rz[FighterPrototype01Rig.L_SHIN] = -7f;
        p.rz[FighterPrototype01Rig.R_THIGH] = -15f;
        p.rz[FighterPrototype01Rig.R_SHIN] = 7f;

        // Idle breathing remains deliberately subtle.
        p.ty[FighterPrototype01Rig.PELVIS] += breathe * 1.4f;
        p.rx[FighterPrototype01Rig.CHEST] += breathe * 1.2f;
        p.rz[FighterPrototype01Rig.L_FOREARM] += breathe * 2.5f;
        p.rz[FighterPrototype01Rig.R_FOREARM] -= breathe * 2.5f;

        boolean walking =
            !state.crouching &&
            !state.airborne &&
            Math.abs(state.walkTime) > 0.001f;

        if (walking) {
            p.ty[FighterPrototype01Rig.PELVIS] += Math.abs(walk) * 2.5f;

            p.rz[FighterPrototype01Rig.L_THIGH] += walk * 18f;
            p.rz[FighterPrototype01Rig.R_THIGH] -= walk * 18f;
            p.rz[FighterPrototype01Rig.L_SHIN] -= walk * 8f;
            p.rz[FighterPrototype01Rig.R_SHIN] += walk * 8f;

            p.rz[FighterPrototype01Rig.L_UPPER_ARM] -= walk * 10f;
            p.rz[FighterPrototype01Rig.R_UPPER_ARM] += walk * 10f;
        }

        if (walking && state.dashing) {
            p.rz[FighterPrototype01Rig.SPINE] -= 9f;
            p.rz[FighterPrototype01Rig.L_THIGH] += walk * 12f;
            p.rz[FighterPrototype01Rig.R_THIGH] -= walk * 12f;
        }
        if (state.crouching) {
            applyCrouch(p);
        }

        if (state.airborne) {
            applyAirPose(p, state.superJumping);
            if (state.verticalSpeed > 0f) {
                float falling=clamp01(state.verticalSpeed/500f);
                p.rz[FighterPrototype01Rig.L_THIGH]=lerp(p.rz[FighterPrototype01Rig.L_THIGH],12,falling);
                p.rz[FighterPrototype01Rig.R_THIGH]=lerp(p.rz[FighterPrototype01Rig.R_THIGH],-18,falling);
                p.rz[FighterPrototype01Rig.L_SHIN]=lerp(p.rz[FighterPrototype01Rig.L_SHIN],-12,falling);
                p.rz[FighterPrototype01Rig.R_SHIN]=lerp(p.rz[FighterPrototype01Rig.R_SHIN],20,falling);
            }
        }

        if (state.guardPose == Fighter3DState.GUARD_HIGH) {
            applyHighGuard(p);
        } else if (state.guardPose == Fighter3DState.GUARD_LOW) {
            applyLowGuard(p);
        }

        if (state.superPose) {
            applySuperPose(p);
        }

        String type = state.attackType;
        if (type != null && !type.isEmpty()) {
            applyAttack(p, type, attack);
        }

        if (state.hitFlash) applyHitRecoil(p);
        float dt=state.animationTime-lastTime;
        float blend=lastTime<0 || dt<=0 || dt>.2f ? 1f : 1f-(float)Math.exp(-28f*dt);
        for(int i=0;i<FighterPrototype01Rig.BONE_COUNT;i++) {
            p.tx[i]=previous.tx[i]=lerp(previous.tx[i],p.tx[i],blend);
            p.ty[i]=previous.ty[i]=lerp(previous.ty[i],p.ty[i],blend);
            p.tz[i]=previous.tz[i]=lerp(previous.tz[i],p.tz[i],blend);
            p.rx[i]=previous.rx[i]=lerp(previous.rx[i],p.rx[i],blend);
            p.ry[i]=previous.ry[i]=lerp(previous.ry[i],p.ry[i],blend);
            p.rz[i]=previous.rz[i]=lerp(previous.rz[i],p.rz[i],blend);
        }
        lastTime=state.animationTime;
        if (!state.airborne && state.knockdownAngle==0f) {
            rig.buildMatrices();
            float support=Math.max(rig.matrix(FighterPrototype01Rig.L_FOOT)[13],rig.matrix(FighterPrototype01Rig.R_FOOT)[13]);
            p.ty[FighterPrototype01Rig.ROOT]-=support+9f;
        }
    }

    private void applyCrouch(FighterPrototype01Rig.Pose p) {
        p.ty[FighterPrototype01Rig.PELVIS] += 28f;
        p.rz[FighterPrototype01Rig.SPINE] = -7f;
        p.rz[FighterPrototype01Rig.CHEST] = -4f;

        p.rz[FighterPrototype01Rig.L_THIGH] = 42f;
        p.rz[FighterPrototype01Rig.L_SHIN] = -72f;
        p.rz[FighterPrototype01Rig.R_THIGH] = -49f;
        p.rz[FighterPrototype01Rig.R_SHIN] = 76f;

        p.rz[FighterPrototype01Rig.L_UPPER_ARM] = -55f;
        p.rz[FighterPrototype01Rig.L_FOREARM] = -110f;
        p.rz[FighterPrototype01Rig.R_UPPER_ARM] = -50f;
        p.rz[FighterPrototype01Rig.R_FOREARM] = -115f;
    }

    private void applyAirPose(
        FighterPrototype01Rig.Pose p,
        boolean superJump
    ) {
        p.rz[FighterPrototype01Rig.SPINE] = superJump ? -7f : -3f;

        p.rz[FighterPrototype01Rig.L_THIGH] = superJump ? 33f : 27f;
        p.rz[FighterPrototype01Rig.L_SHIN] = superJump ? -58f : -42f;
        p.rz[FighterPrototype01Rig.R_THIGH] = superJump ? -40f : -31f;
        p.rz[FighterPrototype01Rig.R_SHIN] = superJump ? 62f : 46f;

        p.rz[FighterPrototype01Rig.L_UPPER_ARM] = superJump ? 72f : 51f;
        p.rz[FighterPrototype01Rig.L_FOREARM] = superJump ? 26f : 65f;
        p.rz[FighterPrototype01Rig.R_UPPER_ARM] = superJump ? -85f : -60f;
        p.rz[FighterPrototype01Rig.R_FOREARM] = superJump ? -22f : -67f;
    }

    private void applyHighGuard(FighterPrototype01Rig.Pose p) {
        p.rz[FighterPrototype01Rig.CHEST] = -5f;

        p.rz[FighterPrototype01Rig.L_UPPER_ARM] = -70f;
        p.rz[FighterPrototype01Rig.L_FOREARM] = -108f;

        p.rz[FighterPrototype01Rig.R_UPPER_ARM] = -58f;
        p.rz[FighterPrototype01Rig.R_FOREARM] = -135f;
    }

    private void applyLowGuard(FighterPrototype01Rig.Pose p) {
        if (p.ty[FighterPrototype01Rig.PELVIS] < 20f) applyCrouch(p);

        p.rz[FighterPrototype01Rig.L_UPPER_ARM] = -55f;
        p.rz[FighterPrototype01Rig.L_FOREARM] = -95f;

        p.rz[FighterPrototype01Rig.R_UPPER_ARM] = -50f;
        p.rz[FighterPrototype01Rig.R_FOREARM] = -115f;
    }

    private void applySuperPose(FighterPrototype01Rig.Pose p) {
        p.rz[FighterPrototype01Rig.SPINE] = 0f;
        p.ry[FighterPrototype01Rig.CHEST] = -14f;

        p.rz[FighterPrototype01Rig.L_UPPER_ARM] = 96f;
        p.rz[FighterPrototype01Rig.L_FOREARM] = -10f;

        p.rz[FighterPrototype01Rig.R_UPPER_ARM] = -96f;
        p.rz[FighterPrototype01Rig.R_FOREARM] = 10f;

        p.rz[FighterPrototype01Rig.L_THIGH] = 20f;
        p.rz[FighterPrototype01Rig.R_THIGH] = -21f;
    }

    private void applyAttack(
        FighterPrototype01Rig.Pose p,
        String type,
        float a
    ) {
        // attackPhase() already returns a rise/fall curve; using it directly
        // gives anticipation/extension/recovery without owning gameplay time.
        if ("L".equals(type)) {
            p.rz[FighterPrototype01Rig.CHEST] = -8f * a;
            p.ry[FighterPrototype01Rig.CHEST] = -16f * a;

            p.rz[FighterPrototype01Rig.R_UPPER_ARM] =
                lerp(-42f, -90f, a);
            p.rz[FighterPrototype01Rig.R_FOREARM] =
                lerp(-135f, 0f, a);

            p.rz[FighterPrototype01Rig.L_UPPER_ARM] =
                lerp(-48f, -55f, a);
        } else if ("M".equals(type)) {
            p.rz[FighterPrototype01Rig.CHEST] = 7f * a;
            p.rz[FighterPrototype01Rig.R_THIGH] =
                lerp(-15f, -88f, a);
            p.rz[FighterPrototype01Rig.R_SHIN] =
                lerp(7f, 7f, a);
            p.rz[FighterPrototype01Rig.L_UPPER_ARM] =
                lerp(-48f, -55f, a);
            p.rz[FighterPrototype01Rig.R_UPPER_ARM] =
                lerp(-42f, -24f, a);
        } else if ("H".equals(type)) {
            p.rz[FighterPrototype01Rig.CHEST] = -13f * a;
            p.ry[FighterPrototype01Rig.CHEST] = -22f * a;

            p.rz[FighterPrototype01Rig.R_UPPER_ARM] =
                lerp(-42f, -108f, a);
            p.rz[FighterPrototype01Rig.R_FOREARM] =
                lerp(-135f, 27f, a);

            p.rz[FighterPrototype01Rig.L_UPPER_ARM] =
                lerp(-48f, -55f, a);
        } else if ("2L".equals(type)) {
            if (p.ty[FighterPrototype01Rig.PELVIS] < 20f) applyCrouch(p);
            p.rz[FighterPrototype01Rig.R_UPPER_ARM] =
                lerp(-62f, -88f, a);
            p.rz[FighterPrototype01Rig.R_FOREARM] =
                lerp(-74f, 0f, a);
        } else if ("2M".equals(type)) {
            if (p.ty[FighterPrototype01Rig.PELVIS] < 20f) applyCrouch(p);
            p.rz[FighterPrototype01Rig.R_THIGH] =
                lerp(-49f, -104f, a);
            p.rz[FighterPrototype01Rig.R_SHIN] =
                lerp(76f, -4f, a);
            p.rz[FighterPrototype01Rig.CHEST] = 9f * a;
        } else if ("2H".equals(type)) {
            if (p.ty[FighterPrototype01Rig.PELVIS] < 20f) applyCrouch(p);
            p.ty[FighterPrototype01Rig.PELVIS] -= 9f * a;
            p.rz[FighterPrototype01Rig.CHEST] = -12f * a;

            p.rz[FighterPrototype01Rig.R_UPPER_ARM] =
                lerp(-62f, -163f, a);
            p.rz[FighterPrototype01Rig.R_FOREARM] =
                lerp(-74f, 5f, a);
        } else if ("S".equals(type) || "SUPER".equals(type)) {
            p.ry[FighterPrototype01Rig.CHEST] = -20f * a;

            p.rz[FighterPrototype01Rig.R_UPPER_ARM] =
                lerp(-42f, -89f, a);
            p.rz[FighterPrototype01Rig.R_FOREARM] =
                lerp(-135f, 0f, a);

            p.rz[FighterPrototype01Rig.L_UPPER_ARM] =
                lerp(-48f, -76f, a);
            p.rz[FighterPrototype01Rig.L_FOREARM] =
                lerp(-118f, -8f, a);
        }
    }

    private void applyHitRecoil(FighterPrototype01Rig.Pose p) {
        p.rz[FighterPrototype01Rig.SPINE] = 8f;
        p.rz[FighterPrototype01Rig.CHEST] = 12f;
        p.rz[FighterPrototype01Rig.NECK] = -9f;

        p.rz[FighterPrototype01Rig.L_UPPER_ARM] = 18f;
        p.rz[FighterPrototype01Rig.L_FOREARM] = 42f;
        p.rz[FighterPrototype01Rig.R_UPPER_ARM] = -21f;
        p.rz[FighterPrototype01Rig.R_FOREARM] = -39f;
    }

    private float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
