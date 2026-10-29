package com.gamelutagpt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import com.gamelutagpt.stage.StagePack;
import com.gamelutagpt.stage.StageScene;
import com.gamelutagpt.stage.StageWorld;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Teste do desenho novo (fase B0): o cenário da luta e os quatro lutadores dela desenhados em
 * OpenGL ES 3, sem o Canvas do Android, com o contador de quadros. Serve para medir no celular
 * quanto o jeito novo ganha antes de trocar o desenho da luta inteira. Aberto pelo botão TESTE GL
 * do Modo Teste; os lutadores trocam de animação a cada segundo, como numa luta.
 */
public final class TesteGlActivity extends Activity {
    static final String CENARIO = "cenario", LUTADORES = "lutadores";

    private GLSurfaceView tela;
    private TextView contador;
    private volatile boolean comCenario = true, comLutadores = true, todasAnimacoes = true;

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Intent intent = getIntent();
        String cenario = intent.getStringExtra(CENARIO);
        String[] lutadores = intent.getStringArrayExtra(LUTADORES);
        if (cenario == null) cenario = "templo_lua";
        if (lutadores == null || lutadores.length == 0) lutadores = new String[] {"player_base", "p02", "p03", "p04"};

        tela = new GLSurfaceView(this);
        tela.setEGLContextClientVersion(3);
        tela.setPreserveEGLContextOnPause(true);
        tela.setRenderer(new Desenho(cenario, lutadores));
        tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        FrameLayout raiz = new FrameLayout(this);
        raiz.addView(tela);
        contador = new TextView(this);
        contador.setTextColor(Color.WHITE);
        contador.setBackgroundColor(0xB0000000);
        contador.setTextSize(13f);
        contador.setPadding(16, 8, 16, 8);
        FrameLayout.LayoutParams topo = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        raiz.addView(contador, topo);

        LinearLayout botoes = new LinearLayout(this);
        botoes.setOrientation(LinearLayout.HORIZONTAL);
        botoes.addView(botao("CENÁRIO", v -> { comCenario = !comCenario; marcar((TextView)v, comCenario); }));
        botoes.addView(botao("LUTADORES", v -> { comLutadores = !comLutadores; marcar((TextView)v, comLutadores); }));
        botoes.addView(botao("TODAS AS ANIMAÇÕES", v -> { todasAnimacoes = !todasAnimacoes; marcar((TextView)v, todasAnimacoes); }));
        botoes.addView(botao("FECHAR", v -> finish()));
        FrameLayout.LayoutParams baixo = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
        raiz.addView(botoes, baixo);
        setContentView(raiz);
    }

    private TextView botao(String nome, View.OnClickListener acao) {
        TextView b = new TextView(this);
        b.setText(nome);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15f);
        b.setPadding(28, 22, 28, 22);
        marcar(b, true);
        b.setOnClickListener(acao);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(8, 8, 8, 8);
        b.setLayoutParams(lp);
        return b;
    }

    private static void marcar(TextView b, boolean ligado) {
        b.setBackgroundColor(ligado ? 0xD02060B0 : 0xD0303030);
    }

    @Override protected void onResume() { super.onResume(); tela.onResume(); }
    @Override protected void onPause() { tela.onPause(); super.onPause(); }

    /** Imagens do cenário para o OpenGL: decodificadas na memória comum até subirem. */
    private final class Arquivos implements RenderAssets {
        @Override public String readText(String path) throws IOException {
            try (InputStream in = getAssets().open(path)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] b = new byte[4096];
                int n;
                while ((n = in.read(b)) != -1) out.write(b, 0, n);
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        }

        @Override public RenderImage loadImage(String path) throws IOException {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream in = getAssets().open(path)) {
                Bitmap b = BitmapFactory.decodeStream(in, null, o);
                if (b == null) throw new IOException("imagem: " + path);
                return new GlRenderCanvas.Imagem(b, true);
            }
        }
    }

    private final class Desenho implements GLSurfaceView.Renderer {
        private final String cenarioId;
        private final String[] ids;
        private final GlRenderCanvas canvas = new GlRenderCanvas();
        private StageScene cenario;
        private final List<CharacterDefinition> lutadores = new ArrayList<>();
        private final Map<String, GlRenderCanvas.Imagem> folhas = new HashMap<>();
        private final int[] caixa = new int[6];
        private long inicio, ultimo, janela;
        private int quadros;
        private double pior, somaMs;
        private String erro;
        private int maxTextura;

        Desenho(String cenarioId, String[] ids) {
            this.cenarioId = cenarioId;
            this.ids = ids;
        }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            try {
                canvas.criar();
                int[] max = new int[1];
                GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, max, 0);
                maxTextura = max[0];
                StagePack pack = StagePack.load(new Arquivos(), "stages/" + cenarioId);
                StageWorld mundo = new StageWorld(Arena.GROUND_Y, Arena.WORLD_WIDTH, CameraRig.CAMERA_ZOOM,
                    Arena.WORLD_WIDTH * 0.5f, CameraRig.CAMERA_GROUND_SCREEN_Y);
                cenario = new StageScene(pack, mundo);
                for (String id : ids) lutadores.add(GeneratedCharacters.get(id));
            } catch (Throwable t) {
                erro = t.toString();
            }
            inicio = ultimo = janela = System.nanoTime();
        }

        @Override public void onSurfaceChanged(GL10 gl, int largura, int altura) {
            canvas.tamanho(largura, altura);
        }

        /** A folha de uma animação; carrega (e sobe para a placa) na primeira vez. */
        private GlRenderCanvas.Imagem folha(CharacterDefinition.Atlas a) {
            GlRenderCanvas.Imagem im = folhas.get(a.resource);
            if (im != null) return im;
            int id = getResources().getIdentifier(a.resource, "drawable", getPackageName());
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            while (maxTextura > 0 && Math.max(a.sheetWidth, a.sheetHeight) / Math.max(1, o.inSampleSize) > maxTextura) {
                o.inSampleSize = Math.max(2, o.inSampleSize * 2);
            }
            Bitmap b = BitmapFactory.decodeResource(getResources(), id, o);
            im = new GlRenderCanvas.Imagem(b, false);
            folhas.put(a.resource, im);
            return im;
        }

        @Override public void onDrawFrame(GL10 gl) {
            long agora = System.nanoTime();
            float dt = (agora - ultimo) / 1e9f;
            ultimo = agora;
            float tempo = (agora - inicio) / 1e9f;
            canvas.comecar();
            if (erro == null) {
                try {
                    float zoom = CameraRig.CAMERA_ZOOM;
                    float esquerda = (Arena.WORLD_WIDTH - 1280f / zoom) * 0.5f + (float)Math.sin(tempo * 0.4f) * 120f;
                    float topo = Arena.GROUND_Y - CameraRig.CAMERA_GROUND_SCREEN_Y / zoom;
                    if (comCenario && cenario != null) {
                        cenario.update(Math.min(dt, 0.05f));
                        cenario.setCamera(zoom, esquerda, topo);
                        cenario.drawBackground(canvas);
                    }
                    if (comLutadores) desenharLutadores(tempo, zoom, esquerda, topo);
                    if (comCenario && cenario != null) cenario.drawForeground(canvas);
                } catch (Throwable t) {
                    erro = t.toString();
                }
            }
            canvas.terminar();
            medir(agora);
        }

        private void desenharLutadores(float tempo, float zoom, float esquerda, float topo) {
            canvas.save();
            canvas.scale(zoom, zoom, 0f, 0f);
            canvas.translate(-esquerda, -topo);
            float meio = Arena.WORLD_WIDTH * 0.5f;
            for (int i = 0; i < lutadores.size(); i++) {
                CharacterDefinition c = lutadores.get(i);
                List<CharacterDefinition.Animation> anims = new ArrayList<>(c.animations.values());
                // Troca de animação a cada segundo (como numa luta, várias folhas por personagem).
                CharacterDefinition.Animation anim = todasAnimacoes
                    ? anims.get((int)(tempo + i * 0.37f) % anims.size()) : c.animation(SpriteStates.IDLE);
                CharacterDefinition.Atlas a = anim.atlas;
                int quadro = anim.frame(tempo % Math.max(anim.duration, 0.01f), 0f);
                if (!a.frame(quadro, caixa)) continue;
                GlRenderCanvas.Imagem im = folha(a);
                float escalaFolha = im.width() / (float)a.sheetWidth;
                float escala = c.profile.worldScale / a.pixelScale;
                // Os dois titulares na frente, os parceiros atrás (como o assist entrando).
                float x = meio + (i % 2 == 0 ? -1f : 1f) * (i < 2 ? 220f : 420f);
                float esq = x - a.rootX * escala, cima = Arena.GROUND_Y - a.rootY * escala;
                canvas.drawImageRegion(im,
                    caixa[0] * escalaFolha, caixa[1] * escalaFolha,
                    (caixa[0] + caixa[2]) * escalaFolha, (caixa[1] + caixa[3]) * escalaFolha,
                    esq + caixa[4] * escala, cima + caixa[5] * escala,
                    esq + (caixa[4] + caixa[2]) * escala, cima + (caixa[5] + caixa[3]) * escala, 255);
            }
            canvas.restore();
        }

        private void medir(long agora) {
            quadros++;
            double ms = (System.nanoTime() - agora) / 1e6;
            somaMs += ms;
            pior = Math.max(pior, ms);
            if (agora - janela < 500_000_000L) return;
            double fps = quadros * 1e9 / (agora - janela);
            long mb = 0;
            for (GlRenderCanvas.Imagem im : folhas.values()) mb += im.bytes;
            final String texto = erro != null ? "ERRO: " + erro
                : String.format(java.util.Locale.ROOT,
                    "OpenGL  %.0f FPS  ·  desenho %.1f ms (pior %.1f)  ·  %d chamadas  ·  folhas %d MB (%d)  ·  %dx%d",
                    fps, somaMs / quadros, pior, canvas.chamadas, mb >> 20, folhas.size(),
                    tela.getWidth(), tela.getHeight());
            quadros = 0;
            somaMs = 0;
            pior = 0;
            janela = agora;
            runOnUiThread(() -> contador.setText(texto));
        }
    }
}
