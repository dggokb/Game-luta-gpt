package com.gamelutagpt;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Real low-poly 3D mesh for the approved Fighter Prototype 01 concept.
 *
 * Mesh pieces are attached to the standard rig. They can later be replaced by
 * authored GLB/mesh assets without changing animation or combat contracts.
 */
final class FighterPrototype01Model {
    static final class Mesh {
        private final FloatBuffer positions;
        private final FloatBuffer normals;
        private final int vertexCount;

        Mesh(float[] p, float[] n) {
            positions = ByteBuffer
                .allocateDirect(p.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
            positions.put(p).position(0);

            normals = ByteBuffer
                .allocateDirect(n.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
            normals.put(n).position(0);

            vertexCount = p.length / 3;
        }

        void draw(Toon3DProgram program) {
            positions.position(0);
            normals.position(0);

            GLES20.glVertexAttribPointer(
                program.aPosition,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                positions
            );
            GLES20.glVertexAttribPointer(
                program.aNormal,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                normals
            );
            GLES20.glDrawArrays(
                GLES20.GL_TRIANGLES,
                0,
                vertexCount
            );
        }
    }

    private static final int SKIN = Color.rgb(215, 151, 106);
    private static final int NAVY = Color.rgb(37, 65, 112);
    private static final int WHITE = Color.rgb(226, 229, 229);
    private static final int BLACK = Color.rgb(37, 38, 43);
    private static final int RED = Color.rgb(197, 58, 38);
    private static final int HAIR = Color.rgb(34, 30, 33);
    private static final int SHOE_DARK = Color.rgb(31, 34, 39);

    private final Mesh box = createBox();
    private final Mesh cylinder = createCylinder(12);
    private final Mesh sphere = createSphere(9, 14);
    private final Mesh cone = createCone(10);

    private final float[] boneModel = new float[16];
    private final float[] local = new float[16];
    private final float[] model = new float[16];

    void draw(
        Toon3DProgram program,
        FighterPrototype01Rig rig,
        float[] root,
        float[] viewProjection,
        boolean hitFlash
    ) {
        rig.buildMatrices();
        program.begin();

        // Back leg first.
        drawPantsLeg(
            program, rig, root, viewProjection,
            FighterPrototype01Rig.L_THIGH,
            FighterPrototype01Rig.L_SHIN,
            FighterPrototype01Rig.L_FOOT,
            -1f,
            hitFlash
        );

        // Back arm.
        drawArm(
            program, rig, root, viewProjection,
            FighterPrototype01Rig.L_UPPER_ARM,
            FighterPrototype01Rig.L_FOREARM,
            FighterPrototype01Rig.L_HAND,
            hitFlash
        );

        drawTorso(program, rig, root, viewProjection, hitFlash);
        drawHead(program, rig, root, viewProjection, hitFlash);

        // Front leg.
        drawPantsLeg(
            program, rig, root, viewProjection,
            FighterPrototype01Rig.R_THIGH,
            FighterPrototype01Rig.R_SHIN,
            FighterPrototype01Rig.R_FOOT,
            1f,
            hitFlash
        );

        // Front arm last for clear fighting-game silhouette.
        drawArm(
            program, rig, root, viewProjection,
            FighterPrototype01Rig.R_UPPER_ARM,
            FighterPrototype01Rig.R_FOREARM,
            FighterPrototype01Rig.R_HAND,
            hitFlash
        );

        program.end();
    }

    private void drawTorso(
        Toon3DProgram program,
        FighterPrototype01Rig rig,
        float[] root,
        float[] vp,
        boolean flash
    ) {
        // Black fitted undershirt.
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.SPINE,
            0f, 0f, 0f,
            0f, 0f, 0f,
            50f, 64f, 25f,
            BLACK, flash
        );

        // Navy sleeveless vest, split so the black shirt stays visible.
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.SPINE,
            -20f, 0f, 15f,
            0f, 0f, -2f,
            19f, 66f, 8f,
            NAVY, flash
        );
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.SPINE,
            20f, 0f, 15f,
            0f, 0f, 2f,
            19f, 66f, 8f,
            NAVY, flash
        );

        // White concept panels.
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.SPINE,
            -10f, 2f, 20f,
            0f, 0f, 0f,
            6f, 55f, 3f,
            WHITE, flash
        );
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.SPINE,
            10f, 2f, 20f,
            0f, 0f, 0f,
            6f, 55f, 3f,
            WHITE, flash
        );

        // Red/orange high-collar accents.
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.CHEST,
            -10f, 8f, 16f,
            0f, 0f, -24f,
            7f, 24f, 5f,
            RED, flash
        );
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.CHEST,
            10f, 8f, 16f,
            0f, 0f, 24f,
            7f, 24f, 5f,
            RED, flash
        );

        // Waist / sash.
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.PELVIS,
            0f, 0f, 0f,
            0f, 0f, 0f,
            61f, 11f, 31f,
            BLACK, flash
        );
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.PELVIS,
            19f, 23f, 2f,
            0f, 0f, -10f,
            10f, 43f, 5f,
            RED, flash
        );
    }

    private void drawArm(
        Toon3DProgram program,
        FighterPrototype01Rig rig,
        float[] root,
        float[] vp,
        int upper,
        int forearm,
        int hand,
        boolean flash
    ) {
        part(
            program, rig, root, vp,
            cylinder, upper,
            0f, 0f, 0f,
            0f, 0f, 0f,
            18f, 38f, 18f,
            SKIN, flash
        );
        part(
            program, rig, root, vp,
            cylinder, forearm,
            0f, 0f, 0f,
            0f, 0f, 0f,
            16f, 34f, 16f,
            SKIN, flash
        );

        // Fingerless glove: black body + red wrist band.
        part(
            program, rig, root, vp,
            box, hand,
            0f, 4f, 0f,
            0f, 0f, 0f,
            20f, 18f, 20f,
            BLACK, flash
        );
        part(
            program, rig, root, vp,
            box, hand,
            0f, 15f, 0f,
            0f, 0f, 0f,
            22f, 7f, 22f,
            RED, flash
        );
    }

    private void drawPantsLeg(
        Toon3DProgram program,
        FighterPrototype01Rig rig,
        float[] root,
        float[] vp,
        int thigh,
        int shin,
        int foot,
        float side,
        boolean flash
    ) {
        part(
            program, rig, root, vp,
            cylinder, thigh,
            0f, 0f, 0f,
            0f, 0f, 0f,
            29f, 39f, 28f,
            WHITE, flash
        );
        part(
            program, rig, root, vp,
            cylinder, shin,
            0f, 0f, 0f,
            0f, 0f, 0f,
            25f, 38f, 24f,
            WHITE, flash
        );

        // Navy outer stripe from the approved concept.
        part(
            program, rig, root, vp,
            box, thigh,
            side * 10f, 18f, 11f,
            0f, 0f, 0f,
            6f, 35f, 3f,
            NAVY, flash
        );
        part(
            program, rig, root, vp,
            box, shin,
            side * 9f, 17f, 10f,
            0f, 0f, 0f,
            5f, 32f, 3f,
            NAVY, flash
        );

        // High-top fighting shoe.
        part(
            program, rig, root, vp,
            box, foot,
            side * 5f, 5f, 9f,
            0f, 0f, side * -4f,
            32f, 16f, 43f,
            WHITE, flash
        );
        part(
            program, rig, root, vp,
            box, foot,
            side * 4f, 3f, 25f,
            0f, 0f, 0f,
            19f, 8f, 4f,
            SHOE_DARK, flash
        );
        part(
            program, rig, root, vp,
            box, foot,
            side * 11f, 3f, 28f,
            0f, 0f, 0f,
            6f, 9f, 4f,
            RED, flash
        );
    }

    private void drawHead(
        Toon3DProgram program,
        FighterPrototype01Rig rig,
        float[] root,
        float[] vp,
        boolean flash
    ) {
        // Neck points upward from the neck bone.
        part(
            program, rig, root, vp,
            cylinder, FighterPrototype01Rig.NECK,
            0f, 0f, 0f,
            0f, 0f, 180f,
            13f, 23f, 13f,
            SKIN, flash
        );

        // Anime face.
        part(
            program, rig, root, vp,
            sphere, FighterPrototype01Rig.HEAD,
            0f, 0f, 0f,
            0f, 0f, 0f,
            48f, 55f, 45f,
            SKIN, flash
        );

        // Hair mass.
        part(
            program, rig, root, vp,
            sphere, FighterPrototype01Rig.HEAD,
            0f, -16f, -1f,
            0f, 0f, 0f,
            53f, 36f, 49f,
            HAIR, flash
        );

        // Spiky silhouette.
        for (int i = 0; i < 7; i++) {
            float x = -22f + i * 7.2f;
            float z = ((i % 2) == 0) ? 3f : -4f;
            float angle = -30f + i * 10f;
            part(
                program, rig, root, vp,
                cone, FighterPrototype01Rig.HEAD,
                x, -26f - (i % 3) * 2f, z,
                0f, 0f, angle,
                13f, 27f + (i % 3) * 4f, 13f,
                HAIR, flash
            );
        }

        // Eyes / brows — small 3D geometry, no texture dependency.
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.HEAD,
            -8f, -3f, 23f,
            0f, 0f, -6f,
            9f, 2.2f, 1.8f,
            BLACK, false
        );
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.HEAD,
            8f, -3f, 23f,
            0f, 0f, 6f,
            9f, 2.2f, 1.8f,
            BLACK, false
        );
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.HEAD,
            -8f, -10f, 22.5f,
            0f, 0f, -10f,
            10f, 2.4f, 1.8f,
            HAIR, false
        );
        part(
            program, rig, root, vp,
            box, FighterPrototype01Rig.HEAD,
            8f, -10f, 22.5f,
            0f, 0f, 10f,
            10f, 2.4f, 1.8f,
            HAIR, false
        );
    }

    private void part(
        Toon3DProgram program,
        FighterPrototype01Rig rig,
        float[] root,
        float[] vp,
        Mesh mesh,
        int bone,
        float tx,
        float ty,
        float tz,
        float rx,
        float ry,
        float rz,
        float sx,
        float sy,
        float sz,
        int color,
        boolean flash
    ) {
        Matrix.multiplyMM(
            boneModel,
            0,
            root,
            0,
            rig.matrix(bone),
            0
        );

        Matrix.setIdentityM(local, 0);
        Matrix.translateM(local, 0, tx, ty, tz);
        if (rz != 0f) Matrix.rotateM(local, 0, rz, 0f, 0f, 1f);
        if (rx != 0f) Matrix.rotateM(local, 0, rx, 1f, 0f, 0f);
        if (ry != 0f) Matrix.rotateM(local, 0, ry, 0f, 1f, 0f);
        Matrix.scaleM(local, 0, sx, sy, sz);

        Matrix.multiplyMM(
            model,
            0,
            boneModel,
            0,
            local,
            0
        );

        int actual = flash ? Color.WHITE : color;
        program.draw(
            mesh,
            model,
            vp,
            Color.red(actual) / 255f,
            Color.green(actual) / 255f,
            Color.blue(actual) / 255f,
            Color.alpha(actual) / 255f
        );
    }

    private static Mesh createBox() {
        Builder b = new Builder();

        // Front / back.
        quad(b, -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f, 0f,0f,1f);
        quad(b, 0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0f,0f,-1f);

        // Left / right.
        quad(b, -0.5f,-0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f, -1f,0f,0f);
        quad(b, 0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f, 1f,0f,0f);

        // Top / bottom.
        quad(b, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, -0.5f,-0.5f,0.5f, 0f,-1f,0f);
        quad(b, -0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0f,1f,0f);

        return b.mesh();
    }

    private static Mesh createCylinder(int segments) {
        Builder b = new Builder();
        for (int i = 0; i < segments; i++) {
            float a0 = (float)(Math.PI * 2.0 * i / segments);
            float a1 = (float)(Math.PI * 2.0 * (i + 1) / segments);
            float x0 = (float)Math.cos(a0) * 0.5f;
            float z0 = (float)Math.sin(a0) * 0.5f;
            float x1 = (float)Math.cos(a1) * 0.5f;
            float z1 = (float)Math.sin(a1) * 0.5f;

            tri(b,
                x0,0f,z0, x1,0f,z1, x1,1f,z1,
                x0,0f,z0, x1,0f,z1, x1,0f,z1);
            tri(b,
                x0,0f,z0, x1,1f,z1, x0,1f,z0,
                x0,0f,z0, x1,0f,z1, x0,0f,z0);

            tri(b,
                0f,0f,0f, x1,0f,z1, x0,0f,z0,
                0f,-1f,0f, 0f,-1f,0f, 0f,-1f,0f);
            tri(b,
                0f,1f,0f, x0,1f,z0, x1,1f,z1,
                0f,1f,0f, 0f,1f,0f, 0f,1f,0f);
        }
        return b.mesh();
    }

    private static Mesh createSphere(int stacks, int slices) {
        Builder b = new Builder();
        for (int y = 0; y < stacks; y++) {
            float v0 = (float)y / stacks;
            float v1 = (float)(y + 1) / stacks;
            float phi0 = (float)(-Math.PI * 0.5 + Math.PI * v0);
            float phi1 = (float)(-Math.PI * 0.5 + Math.PI * v1);

            for (int x = 0; x < slices; x++) {
                float u0 = (float)x / slices;
                float u1 = (float)(x + 1) / slices;
                float th0 = (float)(Math.PI * 2.0 * u0);
                float th1 = (float)(Math.PI * 2.0 * u1);

                float[] p00 = spherePoint(phi0, th0);
                float[] p10 = spherePoint(phi0, th1);
                float[] p11 = spherePoint(phi1, th1);
                float[] p01 = spherePoint(phi1, th0);

                tri(b,
                    p00[0],p00[1],p00[2],
                    p10[0],p10[1],p10[2],
                    p11[0],p11[1],p11[2],
                    p00[0]*2f,p00[1]*2f,p00[2]*2f,
                    p10[0]*2f,p10[1]*2f,p10[2]*2f,
                    p11[0]*2f,p11[1]*2f,p11[2]*2f);

                tri(b,
                    p00[0],p00[1],p00[2],
                    p11[0],p11[1],p11[2],
                    p01[0],p01[1],p01[2],
                    p00[0]*2f,p00[1]*2f,p00[2]*2f,
                    p11[0]*2f,p11[1]*2f,p11[2]*2f,
                    p01[0]*2f,p01[1]*2f,p01[2]*2f);
            }
        }
        return b.mesh();
    }

    private static Mesh createCone(int segments) {
        Builder b = new Builder();
        for (int i = 0; i < segments; i++) {
            float a0 = (float)(Math.PI * 2.0 * i / segments);
            float a1 = (float)(Math.PI * 2.0 * (i + 1) / segments);
            float x0 = (float)Math.cos(a0) * 0.5f;
            float z0 = (float)Math.sin(a0) * 0.5f;
            float x1 = (float)Math.cos(a1) * 0.5f;
            float z1 = (float)Math.sin(a1) * 0.5f;

            float nx = (float)Math.cos((a0 + a1) * 0.5f);
            float nz = (float)Math.sin((a0 + a1) * 0.5f);

            tri(b,
                x0,0f,z0, x1,0f,z1, 0f,-1f,0f,
                nx,0.45f,nz, nx,0.45f,nz, nx,0.45f,nz);

            tri(b,
                0f,0f,0f, x1,0f,z1, x0,0f,z0,
                0f,1f,0f, 0f,1f,0f, 0f,1f,0f);
        }
        return b.mesh();
    }

    private static float[] spherePoint(float phi, float theta) {
        float cp = (float)Math.cos(phi);
        return new float[] {
            cp * (float)Math.cos(theta) * 0.5f,
            (float)Math.sin(phi) * 0.5f,
            cp * (float)Math.sin(theta) * 0.5f
        };
    }

    private static void quad(
        Builder b,
        float ax,float ay,float az,
        float bx,float by,float bz,
        float cx,float cy,float cz,
        float dx,float dy,float dz,
        float nx,float ny,float nz
    ) {
        tri(b,
            ax,ay,az, bx,by,bz, cx,cy,cz,
            nx,ny,nz, nx,ny,nz, nx,ny,nz);
        tri(b,
            ax,ay,az, cx,cy,cz, dx,dy,dz,
            nx,ny,nz, nx,ny,nz, nx,ny,nz);
    }

    private static void tri(
        Builder b,
        float ax,float ay,float az,
        float bx,float by,float bz,
        float cx,float cy,float cz,
        float anx,float any,float anz,
        float bnx,float bny,float bnz,
        float cnx,float cny,float cnz
    ) {
        b.v(ax,ay,az,anx,any,anz);
        b.v(bx,by,bz,bnx,bny,bnz);
        b.v(cx,cy,cz,cnx,cny,cnz);
    }

    private static final class Builder {
        private float[] p = new float[1024];
        private float[] n = new float[1024];
        private int pc = 0;
        private int nc = 0;

        void v(
            float x,float y,float z,
            float nx,float ny,float nz
        ) {
            ensure(3);
            p[pc++] = x;
            p[pc++] = y;
            p[pc++] = z;
            n[nc++] = nx;
            n[nc++] = ny;
            n[nc++] = nz;
        }

        private void ensure(int add) {
            if (pc + add <= p.length) return;
            int next = p.length * 2;
            float[] np = new float[next];
            float[] nn = new float[next];
            System.arraycopy(p, 0, np, 0, pc);
            System.arraycopy(n, 0, nn, 0, nc);
            p = np;
            n = nn;
        }

        Mesh mesh() {
            float[] fp = new float[pc];
            float[] fn = new float[nc];
            System.arraycopy(p, 0, fp, 0, pc);
            System.arraycopy(n, 0, fn, 0, nc);
            return new Mesh(fp, fn);
        }
    }
}
