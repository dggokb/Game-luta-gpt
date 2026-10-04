package com.gamelutagpt;

import android.opengl.GLES20;
import android.opengl.Matrix;

final class Toon3DProgram {
    private static final String VERTEX =
        "uniform mat4 uMvp;" +
        "uniform mat4 uModel;" +
        "uniform float uOutlineScale;" +
        "attribute vec3 aPosition;" +
        "attribute vec3 aNormal;" +
        "varying vec3 vNormal;" +
        "void main() {" +
        "  vec3 p = aPosition * uOutlineScale;" +
        "  gl_Position = uMvp * vec4(p, 1.0);" +
        "  vNormal = mat3(uModel) * aNormal;" +
        "}";

    private static final String FRAGMENT =
        "precision mediump float;" +
        "uniform vec4 uColor;" +
        "uniform float uOutline;" +
        "varying vec3 vNormal;" +
        "void main() {" +
        "  if (uOutline > 0.5) {" +
        "    gl_FragColor = vec4(0.055, 0.060, 0.075, 1.0);" +
        "    return;" +
        "  }" +
        "  vec3 n = normalize(vNormal);" +
        "  vec3 l = normalize(vec3(-0.35, -0.72, 0.60));" +
        "  float d = max(dot(n, l), 0.0);" +
        "  float band = d > 0.72 ? 1.10 : (d > 0.30 ? 0.82 : 0.56);" +
        "  vec3 lit = clamp(uColor.rgb * band, 0.0, 1.0);" +
        "  gl_FragColor = vec4(lit, uColor.a);" +
        "}";

    final int program;
    final int aPosition;
    final int aNormal;
    private final int uMvp;
    private final int uModel;
    private final int uColor;
    private final int uOutlineScale;
    private final int uOutline;

    private final float[] mvp = new float[16];

    Toon3DProgram() {
        int vertex = compile(GLES20.GL_VERTEX_SHADER, VERTEX);
        int fragment = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertex);
        GLES20.glAttachShader(program, fragment);
        GLES20.glLinkProgram(program);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new IllegalStateException("3D shader link failed: " + log);
        }

        GLES20.glDeleteShader(vertex);
        GLES20.glDeleteShader(fragment);

        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aNormal = GLES20.glGetAttribLocation(program, "aNormal");
        uMvp = GLES20.glGetUniformLocation(program, "uMvp");
        uModel = GLES20.glGetUniformLocation(program, "uModel");
        uColor = GLES20.glGetUniformLocation(program, "uColor");
        uOutlineScale = GLES20.glGetUniformLocation(program, "uOutlineScale");
        uOutline = GLES20.glGetUniformLocation(program, "uOutline");
    }

    void begin() {
        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glEnableVertexAttribArray(aNormal);
    }

    void end() {
        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aNormal);
    }

    void draw(
        FighterPrototype01Model.Mesh mesh,
        float[] model,
        float[] viewProjection,
        float r,
        float g,
        float b,
        float a
    ) {
        Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0);

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
        GLES20.glUniform4f(uColor, r, g, b, a);

        // Expanded back faces create the anime outline.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_FRONT);
        GLES20.glUniform1f(uOutline, 1f);
        GLES20.glUniform1f(uOutlineScale, 1.075f);
        mesh.draw(this);

        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glUniform1f(uOutline, 0f);
        GLES20.glUniform1f(uOutlineScale, 1f);
        mesh.draw(this);
    }

    private static int compile(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException("3D shader compile failed: " + log);
        }
        return shader;
    }
}
