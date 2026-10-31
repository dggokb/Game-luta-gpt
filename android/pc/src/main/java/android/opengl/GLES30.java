package android.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

/**
 * PC: o OpenGL ES 3 do Android por cima do OpenGL 3.3 do computador (LWJGL). Só o que o desenho
 * da luta usa ({@code GlRenderCanvas}); os números das constantes são os mesmos nos dois.
 * Os shaders "#version 300 es" viram "#version 330 core" (a mesma linguagem para o que eles usam).
 */
public final class GLES30 {
    private GLES30() {}

    public static final int GL_TEXTURE_2D = 0x0DE1, GL_TEXTURE0 = 0x84C0, GL_TEXTURE1 = 0x84C1, GL_TEXTURE2 = 0x84C2;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801, GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_WRAP_S = 0x2802, GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_LINEAR = 0x2601, GL_CLAMP_TO_EDGE = 0x812F, GL_REPEAT = 0x2901;
    public static final int GL_RGBA = 0x1908, GL_UNSIGNED_BYTE = 0x1401, GL_FLOAT = 0x1406;
    public static final int GL_ARRAY_BUFFER = 0x8892, GL_STREAM_DRAW = 0x88E0;
    public static final int GL_VERTEX_SHADER = 0x8B31, GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_COMPILE_STATUS = 0x8B81, GL_LINK_STATUS = 0x8B82;
    public static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000, GL_STENCIL_BUFFER_BIT = 0x0400;
    public static final int GL_BLEND = 0x0BE2, GL_STENCIL_TEST = 0x0B90;
    public static final int GL_ZERO = 0, GL_ONE = 1, GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_KEEP = 0x1E00, GL_INVERT = 0x150A, GL_INCR_WRAP = 0x8507, GL_DECR_WRAP = 0x8508;
    public static final int GL_ALWAYS = 0x0207, GL_NOTEQUAL = 0x0205;
    public static final int GL_FRONT = 0x0404, GL_BACK = 0x0405;

    // ---- Texturas ----

    public static void glGenTextures(int n, int[] ids, int offset) {
        for (int i = 0; i < n; i++) ids[offset + i] = GL11.glGenTextures();
    }

    public static void glDeleteTextures(int n, int[] ids, int offset) {
        for (int i = 0; i < n; i++) GL11.glDeleteTextures(ids[offset + i]);
    }

    public static void glBindTexture(int target, int id) { GL11.glBindTexture(target, id); }

    public static void glActiveTexture(int unidade) { GL13.glActiveTexture(unidade); }

    public static void glTexParameteri(int target, int pname, int param) { GL11.glTexParameteri(target, pname, param); }

    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, Buffer pixels) {
        // O formato sem tamanho (GL_RGBA) do ES vira o de 8 bits por canal.
        int interno = internalformat == GL_RGBA ? GL11.GL_RGBA8 : internalformat;
        GL11.nglTexImage2D(target, level, interno, width, height, border, format, type, endereco(pixels));
    }

    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height,
                                       int format, int type, Buffer pixels) {
        GL11.nglTexSubImage2D(target, level, x, y, width, height, format, type, endereco(pixels));
    }

    // ---- Buffers e vértices ----

    public static void glGenBuffers(int n, int[] ids, int offset) {
        for (int i = 0; i < n; i++) ids[offset + i] = GL15.glGenBuffers();
    }

    public static void glGenVertexArrays(int n, int[] ids, int offset) {
        for (int i = 0; i < n; i++) ids[offset + i] = GL30.glGenVertexArrays();
    }

    public static void glBindVertexArray(int id) { GL30.glBindVertexArray(id); }

    public static void glBindBuffer(int target, int id) { GL15.glBindBuffer(target, id); }

    /** Envio dos vértices de um buffer que não é direto (o do Android aceita; o LWJGL não). */
    private static ByteBuffer envio;

    public static void glBufferData(int target, int size, Buffer data, int usage) {
        if (data == null || data.isDirect()) {
            GL15.nglBufferData(target, size, endereco(data), usage);
            return;
        }
        if (envio == null || envio.capacity() < size) {
            if (envio != null) MemoryUtil.memFree(envio);
            envio = MemoryUtil.memAlloc(Math.max(size, envio == null ? 1 << 20 : envio.capacity() * 2));
        }
        envio.clear();
        if (data instanceof FloatBuffer) {
            FloatBuffer f = (FloatBuffer) data;
            envio.asFloatBuffer().put(f.array(), f.arrayOffset() + f.position(), size / 4);
        } else if (data instanceof IntBuffer) {
            IntBuffer f = (IntBuffer) data;
            envio.asIntBuffer().put(f.array(), f.arrayOffset() + f.position(), size / 4);
        } else {
            ByteBuffer f = (ByteBuffer) data;
            envio.put(f.array(), f.arrayOffset() + f.position(), size);
            envio.clear();
        }
        GL15.nglBufferData(target, size, MemoryUtil.memAddress(envio), usage);
    }

    public static void glEnableVertexAttribArray(int i) { GL20.glEnableVertexAttribArray(i); }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset);
    }

    public static void glDrawArrays(int mode, int first, int count) { GL11.glDrawArrays(mode, first, count); }

    // ---- Shaders ----

    public static int glCreateProgram() { return GL20.glCreateProgram(); }

    public static int glCreateShader(int tipo) { return GL20.glCreateShader(tipo); }

    public static void glShaderSource(int shader, String fonte) {
        GL20.glShaderSource(shader, fonte.replace("#version 300 es", "#version 330 core"));
    }

    public static void glCompileShader(int shader) { GL20.glCompileShader(shader); }

    public static void glAttachShader(int programa, int shader) { GL20.glAttachShader(programa, shader); }

    public static void glLinkProgram(int programa) { GL20.glLinkProgram(programa); }

    public static void glUseProgram(int programa) { GL20.glUseProgram(programa); }

    public static void glGetShaderiv(int shader, int pname, int[] params, int offset) {
        params[offset] = GL20.glGetShaderi(shader, pname);
    }

    public static void glGetProgramiv(int programa, int pname, int[] params, int offset) {
        params[offset] = GL20.glGetProgrami(programa, pname);
    }

    public static String glGetShaderInfoLog(int shader) { return GL20.glGetShaderInfoLog(shader); }

    public static String glGetProgramInfoLog(int programa) { return GL20.glGetProgramInfoLog(programa); }

    public static int glGetUniformLocation(int programa, String nome) { return GL20.glGetUniformLocation(programa, nome); }

    public static void glUniform1i(int local, int v) { GL20.glUniform1i(local, v); }

    public static void glUniform2f(int local, float x, float y) { GL20.glUniform2f(local, x, y); }

    public static void glUniform4f(int local, float x, float y, float z, float w) { GL20.glUniform4f(local, x, y, z, w); }

    public static void glUniform3fv(int local, int count, float[] v, int offset) {
        GL20.glUniform3fv(local, Arrays.copyOfRange(v, offset, offset + count * 3));
    }

    public static void glUniformMatrix4fv(int local, int count, boolean transpose, float[] v, int offset) {
        GL20.glUniformMatrix4fv(local, transpose, Arrays.copyOfRange(v, offset, offset + count * 16));
    }

    // ---- Estado ----

    public static void glGetIntegerv(int pname, int[] params, int offset) {
        // GL_GPU_DISJOINT_EXT: o OpenGL do computador não tem (o tempo nunca é interrompido).
        params[offset] = pname == 0x8FBB ? 0 : GL11.glGetInteger(pname);
    }

    // ---- Consultas (o tempo da placa de vídeo: GL_TIME_ELAPSED, do OpenGL 3.3) ----

    public static final int GL_EXTENSIONS = 0x1F03, GL_QUERY_RESULT = 0x8866, GL_QUERY_RESULT_AVAILABLE = 0x8867;

    /** No OpenGL 3.3 o tempo da placa é do núcleo: aparece aqui como a extensão do celular. */
    public static String glGetString(int nome) {
        if (nome == GL_EXTENSIONS) return "GL_EXT_disjoint_timer_query";
        return GL11.glGetString(nome);
    }

    public static void glGenQueries(int n, int[] ids, int offset) {
        for (int i = 0; i < n; i++) ids[offset + i] = GL15.glGenQueries();
    }

    public static void glBeginQuery(int alvo, int id) { GL15.glBeginQuery(alvo, id); }

    public static void glEndQuery(int alvo) { GL15.glEndQuery(alvo); }

    public static void glGetQueryObjectuiv(int id, int pname, int[] params, int offset) {
        params[offset] = GL15.glGetQueryObjecti(id, pname);
    }

    public static void glViewport(int x, int y, int w, int h) { GL11.glViewport(x, y, w, h); }

    public static void glClearColor(float r, float g, float b, float a) { GL11.glClearColor(r, g, b, a); }

    public static void glClearStencil(int s) { GL11.glClearStencil(s); }

    public static void glClear(int mascara) { GL11.glClear(mascara); }

    public static void glEnable(int cap) { GL11.glEnable(cap); }

    public static void glDisable(int cap) { GL11.glDisable(cap); }

    public static void glBlendFunc(int src, int dst) { GL11.glBlendFunc(src, dst); }

    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) { GL11.glColorMask(r, g, b, a); }

    public static void glStencilMask(int m) { GL11.glStencilMask(m); }

    public static void glStencilFunc(int func, int ref, int mask) { GL11.glStencilFunc(func, ref, mask); }

    public static void glStencilOp(int fail, int zfail, int zpass) { GL11.glStencilOp(fail, zfail, zpass); }

    public static void glStencilOpSeparate(int face, int fail, int zfail, int zpass) {
        GL20.glStencilOpSeparate(face, fail, zfail, zpass);
    }

    /** Endereço do buffer direto a partir da posição dele (0 = sem dados). */
    private static long endereco(Buffer b) {
        if (b == null) return 0L;
        if (!b.isDirect()) throw new IllegalArgumentException("buffer não direto");
        return MemoryUtil.memAddress(b);
    }

}
