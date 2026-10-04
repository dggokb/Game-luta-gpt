package com.gamelutagpt;

import android.opengl.Matrix;

/**
 * Standard reusable humanoid rig for Fighter Prototype 01.
 *
 * Rotations are local Euler angles in degrees. Translation offsets are only
 * used for procedural pose adjustments; rest offsets define the skeleton.
 */
final class FighterPrototype01Rig {
    static final int ROOT = 0;
    static final int PELVIS = 1;
    static final int SPINE = 2;
    static final int CHEST = 3;
    static final int NECK = 4;
    static final int HEAD = 5;

    static final int L_UPPER_ARM = 6;
    static final int L_FOREARM = 7;
    static final int L_HAND = 8;
    static final int R_UPPER_ARM = 9;
    static final int R_FOREARM = 10;
    static final int R_HAND = 11;

    static final int L_THIGH = 12;
    static final int L_SHIN = 13;
    static final int L_FOOT = 14;
    static final int R_THIGH = 15;
    static final int R_SHIN = 16;
    static final int R_FOOT = 17;

    static final int BONE_COUNT = 18;

    private static final int[] PARENT = {
        -1,
        ROOT,
        PELVIS,
        SPINE,
        CHEST,
        NECK,
        CHEST,
        L_UPPER_ARM,
        L_FOREARM,
        CHEST,
        R_UPPER_ARM,
        R_FOREARM,
        PELVIS,
        L_THIGH,
        L_SHIN,
        PELVIS,
        R_THIGH,
        R_SHIN
    };

    private static final float[] REST_X = {
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        -27f,
        0f,
        0f,
        27f,
        0f,
        0f,
        -15f,
        0f,
        0f,
        15f,
        0f,
        0f
    };

    private static final float[] REST_Y = {
        0f,
        -98f,
        -28f,
        -28f,
        -12f,
        -23f,
        2f,
        35f,
        30f,
        2f,
        35f,
        30f,
        4f,
        47f,
        42f,
        4f,
        47f,
        42f
    };

    private static final float[] REST_Z = {
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        -7f,
        0f,
        0f,
        8f,
        0f,
        0f,
        -7f,
        0f,
        0f,
        8f,
        0f,
        0f
    };

    static final class Pose {
        final float[] tx = new float[BONE_COUNT];
        final float[] ty = new float[BONE_COUNT];
        final float[] tz = new float[BONE_COUNT];
        final float[] rx = new float[BONE_COUNT];
        final float[] ry = new float[BONE_COUNT];
        final float[] rz = new float[BONE_COUNT];

        void reset() {
            for (int i = 0; i < BONE_COUNT; i++) {
                tx[i] = 0f;
                ty[i] = 0f;
                tz[i] = 0f;
                rx[i] = 0f;
                ry[i] = 0f;
                rz[i] = 0f;
            }
        }
    }

    final Pose pose = new Pose();

    private final float[][] world = new float[BONE_COUNT][16];
    private final float[] local = new float[16];
    private final float[] temp = new float[16];

    void buildMatrices() {
        for (int i = 0; i < BONE_COUNT; i++) {
            Matrix.setIdentityM(local, 0);
            Matrix.translateM(
                local,
                0,
                REST_X[i] + pose.tx[i],
                REST_Y[i] + pose.ty[i],
                REST_Z[i] + pose.tz[i]
            );

            if (pose.rz[i] != 0f) {
                Matrix.rotateM(local, 0, pose.rz[i], 0f, 0f, 1f);
            }
            if (pose.rx[i] != 0f) {
                Matrix.rotateM(local, 0, pose.rx[i], 1f, 0f, 0f);
            }
            if (pose.ry[i] != 0f) {
                Matrix.rotateM(local, 0, pose.ry[i], 0f, 1f, 0f);
            }

            int parent = PARENT[i];
            if (parent < 0) {
                System.arraycopy(local, 0, world[i], 0, 16);
            } else {
                Matrix.multiplyMM(temp, 0, world[parent], 0, local, 0);
                System.arraycopy(temp, 0, world[i], 0, 16);
            }
        }
    }

    float[] matrix(int bone) {
        return world[bone];
    }
}
