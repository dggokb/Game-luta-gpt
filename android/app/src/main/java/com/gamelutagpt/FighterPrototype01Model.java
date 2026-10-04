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

    /** Shared by the GLES renderer and the mesh-export validation harness. */
    interface DrawTarget {
        void begin();
        void end();
        void draw(Mesh mesh, float[] model, float[] vp, float r, float g, float b, float a);
    }

    private static final int SKIN = Color.rgb(230, 169, 119);
    private static final int NAVY = Color.rgb(43, 65, 108);
    private static final int WHITE = Color.rgb(236, 234, 226);
    private static final int BLACK = Color.rgb(37, 37, 42);
    private static final int RED = Color.rgb(207, 65, 40);
    private static final int HAIR = Color.rgb(42, 34, 33);
    private static final int HAIR_LIGHT = Color.rgb(63, 48, 42);
    private final Mesh box = createBox();
    private final Mesh sphere = createSphere(10, 16);
    private final Mesh cone = createCone(5);
    // Cross sections follow the silhouette: shoulders, ribs, waist; hip, folds, cuff.
    private final Mesh torso = rings(new float[][]{{-30,23,12},{-24,28,15},{-7,26,14},{13,22,12},{29,21,11}}, 0, 360);
    private final Mesh shirt = rings(new float[][]{{-27,23.3f,13},{-23,28.3f,15.5f},{-7,26.3f,14.5f},{13,22.3f,12.5f},{29,21.3f,11.5f}}, 66, 114);
    private final Mesh panelLeft = rings(new float[][]{{-25,27.8f,15.2f},{-12,27.3f,15},{3,24.5f,13.8f},{24,22.2f,12.5f}}, 38, 64);
    private final Mesh panelRight = rings(new float[][]{{-25,27.8f,15.2f},{-12,27.3f,15},{3,24.5f,13.8f},{24,22.2f,12.5f}}, 116, 142);
    private final Mesh upperArm = rings(new float[][]{{-5,7,7},{1,11.5f,11},{10,12,11},{22,9,9},{35,6.5f,7}},0,360);
    private final Mesh forearm = rings(new float[][]{{-3,7,7},{5,9,8.5f},{14,8.5f,8},{30,5.5f,5.5f}},0,360);
    private final float[][] thighRings = {{-7,13,13},{3,17,15},{15,19,16},{32,16,14},{47,13,12}};
    private final float[][] shinRings = {{-5,13.5f,12.5f},{8,17,14},{23,16,13},{34,12,10},{42,7,7}};
    private final Mesh thigh = rings(thighRings,0,360);
    private final Mesh shin = rings(shinRings,0,360);
    private final Mesh thighStripe = rings(expand(thighRings,.35f),-30,36);
    private final Mesh shinStripe = rings(expand(shinRings,.35f),-30,36);
    private final Mesh face = rings(new float[][]{{-21,8,8},{-17,15,13},{-8,17,15},{3,15.5f,15},{12,11.5f,12.5f},{19,5,8}},0,360);
    private final Mesh hairCap = rings(new float[][]{{-27,5,6},{-23,14,13},{-16,18.5f,16},{-6,18,15}},0,360);
    private final Mesh tuft = tuft();
    private final float[] boneModel = new float[16];
    private final float[] local = new float[16];
    private final float[] model = new float[16];

    void draw(DrawTarget program, FighterPrototype01Rig rig, float[] root, float[] vp, boolean flash) {
        rig.buildMatrices();
        program.begin();
        int pelvis=FighterPrototype01Rig.PELVIS, spine=FighterPrototype01Rig.SPINE, head=FighterPrototype01Rig.HEAD;
        part(program,rig,root,vp,torso,spine,0,0,0,0,0,0,1,1,1,NAVY,flash);
        part(program,rig,root,vp,shirt,spine,0,0,0,0,0,0,1,1,1,BLACK,flash);
        part(program,rig,root,vp,panelLeft,spine,0,0,0,0,0,0,1,1,1,WHITE,flash);
        part(program,rig,root,vp,panelRight,spine,0,0,0,0,0,0,1,1,1,WHITE,flash);
        // Open standing collar, orange lining and pale zipper piping.
        for(int side=-1;side<=1;side+=2) {
            part(program,rig,root,vp,box,spine,side*14,-32,4,0,side*22,side*12,9,17,18,NAVY,flash);
            part(program,rig,root,vp,box,spine,side*13,-32,13,0,side*22,side*12,7,14,1.5f,RED,flash);
            part(program,rig,root,vp,box,spine,side*10,1,14.1f,0,side*8,side*-3,1.1f,52,1.4f,WHITE,flash);
        }
        part(program,rig,root,vp,sphere,pelvis,0,-1,0,0,0,0,48,20,29,BLACK,flash);
        for(int i=0;i<3;i++) part(program,rig,root,vp,box,pelvis,0,-5+i*4,14,0,0,0,39,1,1,Color.rgb(58,55,54),flash);
        part(program,rig,root,vp,sphere,pelvis,8,0,17,0,0,0,13,13,9,BLACK,flash);
        float sway=(float)Math.sin(rig.pose.ty[pelvis]*.5f)*3 + rig.pose.rz[spine]*.4f;
        for(int i=0;i<2;i++) {
            part(program,rig,root,vp,box,pelvis,7+i*10,22,16,8,0,-12+i*19+sway,7,41-i*6,2.5f,BLACK,flash);
            part(program,rig,root,vp,box,pelvis,11+i*5,40-i*5,17,8,0,-12+i*19+sway,7,9,2.8f,RED,flash);
        }
        drawArm(program,rig,root,vp,FighterPrototype01Rig.L_UPPER_ARM,FighterPrototype01Rig.L_FOREARM,FighterPrototype01Rig.L_HAND,flash);
        drawArm(program,rig,root,vp,FighterPrototype01Rig.R_UPPER_ARM,FighterPrototype01Rig.R_FOREARM,FighterPrototype01Rig.R_HAND,flash);
        drawLeg(program,rig,root,vp,FighterPrototype01Rig.L_THIGH,FighterPrototype01Rig.L_SHIN,FighterPrototype01Rig.L_FOOT,-1,flash);
        drawLeg(program,rig,root,vp,FighterPrototype01Rig.R_THIGH,FighterPrototype01Rig.R_SHIN,FighterPrototype01Rig.R_FOOT,1,flash);
        part(program,rig,root,vp,sphere,FighterPrototype01Rig.NECK,0,-4,0,0,0,0,18,29,18,SKIN,flash);
        part(program,rig,root,vp,face,head,0,0,0,0,0,0,1,1,1,SKIN,flash);
        for(int side=-1;side<=1;side+=2) {
            part(program,rig,root,vp,sphere,head,side*16,1,0,0,0,0,7,12,8,SKIN,flash);
            // Inset angular eyes, pupils and expressive eyebrows.
            part(program,rig,root,vp,sphere,head,side*7,-1,13.5f,0,side*19,side*-10,10,4.8f,3.8f,BLACK,flash);
            part(program,rig,root,vp,sphere,head,side*7,-1,14.5f,0,side*19,side*-10,8,3.1f,2.5f,WHITE,flash);
            part(program,rig,root,vp,sphere,head,side*6.2f,-.7f,16,0,0,0,2.8f,3.1f,1.5f,Color.rgb(49,37,30),flash);
            part(program,rig,root,vp,box,head,side*7,-5,14.5f,0,side*19,side*-14,10,1.8f,1.7f,HAIR,flash);
        }
        part(program,rig,root,vp,cone,head,0,7,14,90,0,0,4,7,6,SKIN,flash);
        part(program,rig,root,vp,box,head,0,11.7f,12.6f,0,0,-3,6,.7f,1,Color.rgb(103,62,48),flash);
        part(program,rig,root,vp,hairCap,head,0,0,-1,0,0,0,1,1,1,HAIR,flash);
        // Swept, asymmetric locks; every lock is a solid faceted mesh.
        for(int i=0;i<9;i++) {
            float angle=i*40f;
            double rad=Math.toRadians(angle);
            part(program,rig,root,vp,tuft,head,(float)Math.cos(rad)*12,-19-(i%3)*2,(float)Math.sin(rad)*10,
                15+(i%3)*12,angle,-25+(i%4)*12,13,12+(i%3)*2,12,i%3==0?HAIR_LIGHT:HAIR,flash);
        }
        for(int i=0;i<5;i++) {
            part(program,rig,root,vp,tuft,head,-13+i*6,-16+(i%2)*2,17,
                180,0,-24+i*6,10,13+(i%3)*2,7,i%2==0?HAIR:HAIR_LIGHT,flash);
        }
        program.end();
    }

    private void drawArm(DrawTarget p,FighterPrototype01Rig r,float[] root,float[] vp,int upper,int fore,int hand,boolean f) {
        part(p,r,root,vp,upperArm,upper,0,0,0,0,0,0,1,1,1,SKIN,f);
        part(p,r,root,vp,forearm,fore,0,0,0,0,0,0,1,1,1,SKIN,f);
        part(p,r,root,vp,sphere,fore,0,0,0,0,0,0,14,14,14,SKIN,f);
        part(p,r,root,vp,sphere,hand,0,5,0,0,0,0,16,22,17,BLACK,f);
        part(p,r,root,vp,box,hand,0,-2,0,0,0,0,15,6,15,BLACK,f);
        part(p,r,root,vp,box,hand,0,4,8,0,0,0,11,10,2,RED,f);
        part(p,r,root,vp,box,hand,0,-2,7.7f,0,0,0,10,3,2,RED,f);
        for(int i=0;i<4;i++) part(p,r,root,vp,sphere,hand,-5.1f+i*3.4f,12,4,0,0,0,3.5f,6,7,SKIN,f);
        part(p,r,root,vp,sphere,hand,7,6,3,0,0,-25,6,11,7,SKIN,f);
    }

    private void drawLeg(DrawTarget p,FighterPrototype01Rig r,float[] root,float[] vp,int upper,int lower,int foot,int side,boolean f) {
        part(p,r,root,vp,thigh,upper,0,0,0,0,0,0,1,1,1,WHITE,f);
        part(p,r,root,vp,shin,lower,0,0,0,0,0,0,1,1,1,WHITE,f);
        part(p,r,root,vp,thighStripe,upper,0,0,0,0,side==1?0:180,0,1,1,1,NAVY,f);
        part(p,r,root,vp,shinStripe,lower,0,0,0,0,side==1?0:180,0,1,1,1,NAVY,f);
        part(p,r,root,vp,sphere,foot,0,-5,1,0,0,0,16,19,19,BLACK,f);
        part(p,r,root,vp,sphere,foot,0,3,10,0,0,0,23,14,39,BLACK,f);
        part(p,r,root,vp,sphere,foot,0,0,10,0,0,0,22,14,38,WHITE,f);
        part(p,r,root,vp,sphere,foot,side*7,-2,3,0,0,0,8,14,24,NAVY,f);
        part(p,r,root,vp,box,foot,0,-7,8,0,0,0,10,5,9,RED,f);
        part(p,r,root,vp,box,foot,0,-3,15,-24,0,0,10,3,13,BLACK,f);
        for(int i=0;i<3;i++) part(p,r,root,vp,box,foot,0,-5+i*1.4f,11+i*3,-24,0,i%2==0?8:-8,9,1,1,WHITE,f);
    }

    private static float[][] expand(float[][] rows,float d) {
        float[][] result=new float[rows.length][3];
        for(int i=0;i<rows.length;i++) result[i]=new float[]{rows[i][0],rows[i][1]+d,rows[i][2]+d};
        return result;
    }

    /** Lathed cross sections; partial arcs form garment panels on the same surface. */
    private static Mesh rings(float[][] rows,float start,float end) {
        Builder b=new Builder();
        int steps=Math.max(2,Math.round((end-start)/22.5f));
        for(int j=0;j<rows.length-1;j++) for(int i=0;i<steps;i++) {
            double a=Math.toRadians(start+(end-start)*i/steps),c=Math.toRadians(start+(end-start)*(i+1)/steps);
            float[] v0={(float)Math.cos(a)*rows[j][1],rows[j][0],(float)Math.sin(a)*rows[j][2]};
            float[] v1={(float)Math.cos(c)*rows[j][1],rows[j][0],(float)Math.sin(c)*rows[j][2]};
            float[] v2={(float)Math.cos(c)*rows[j+1][1],rows[j+1][0],(float)Math.sin(c)*rows[j+1][2]};
            float[] v3={(float)Math.cos(a)*rows[j+1][1],rows[j+1][0],(float)Math.sin(a)*rows[j+1][2]};
            // Desired outward normal; use geometric slope so folds shade correctly.
            float nx=(v2[1]-v0[1])*(v1[2]-v0[2]), nz=-(v2[1]-v0[1])*(v1[0]-v0[0]);
            float ny=(v2[2]-v0[2])*(v1[0]-v0[0])-(v2[0]-v0[0])*(v1[2]-v0[2]);
            quad(b,v0[0],v0[1],v0[2],v1[0],v1[1],v1[2],v2[0],v2[1],v2[2],v3[0],v3[1],v3[2],nx,ny,nz);
        }
        if(end-start>359) for(int cap=0;cap<2;cap++) {
            float[] row=rows[cap==0?0:rows.length-1];
            for(int i=0;i<steps;i++) {
                double a=Math.PI*2*i/steps,c=Math.PI*2*(i+1)/steps;
                float ny=cap==0?-1:1;
                tri(b,0,row[0],0,(float)Math.cos(a)*row[1],row[0],(float)Math.sin(a)*row[2],(float)Math.cos(c)*row[1],row[0],(float)Math.sin(c)*row[2],0,ny,0,0,ny,0,0,ny,0);
            }
        }
        return b.mesh();
    }

    private static Mesh tuft() {
        Builder b=new Builder();
        float[][] v={{-.5f,0,0},{0,.12f,.5f},{.5f,0,0},{0,-.08f,-.4f},{.75f,-.8f,.18f}};
        for(int i=0;i<4;i++) {
            float[] a=v[i],c=v[(i+1)%4],d=v[4];
            float nx=(c[1]-a[1])*(d[2]-a[2])-(c[2]-a[2])*(d[1]-a[1]);
            float ny=(c[2]-a[2])*(d[0]-a[0])-(c[0]-a[0])*(d[2]-a[2]);
            float nz=(c[0]-a[0])*(d[1]-a[1])-(c[1]-a[1])*(d[0]-a[0]);
            tri(b,a[0],a[1],a[2],c[0],c[1],c[2],d[0],d[1],d[2],nx,ny,nz,nx,ny,nz,nx,ny,nz);
        }
        return b.mesh();
    }

    private void part(
        DrawTarget program,
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
                nx,-0.45f,nz, nx,-0.45f,nz, nx,-0.45f,nz);

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
        float nx=(by-ay)*(cz-az)-(bz-az)*(cy-ay);
        float ny=(bz-az)*(cx-ax)-(bx-ax)*(cz-az);
        float nz=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);
        if(nx*nx+ny*ny+nz*nz < 1e-12f) return;
        b.v(ax,ay,az,anx,any,anz);
        if(nx*(anx+bnx+cnx)+ny*(any+bny+cny)+nz*(anz+bnz+cnz)<0) {
            b.v(cx,cy,cz,cnx,cny,cnz); b.v(bx,by,bz,bnx,bny,bnz);
        } else { b.v(bx,by,bz,bnx,bny,bnz); b.v(cx,cy,cz,cnx,cny,cnz); }
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
            float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            nx/=length; ny/=length; nz/=length;
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
