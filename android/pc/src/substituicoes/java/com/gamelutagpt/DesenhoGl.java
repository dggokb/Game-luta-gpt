package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.SurfaceView;
import com.gamelutagpt.render.RenderCanvas;

/** No PC não há o desenho em OpenGL: a luta sai inteira no Canvas, como sempre. */
final class DesenhoGl {
    interface Ouvinte {
        void superficieGl(boolean pronta);
    }

    static DesenhoGl noQuadro() { return null; }

    static void soltarDepois(Bitmap folha) {}

    static RenderCanvas comoRenderCanvas(Canvas c) { return null; }

    DesenhoGl(SurfaceView view, SurfaceView jogo, Ouvinte ouvinte) {}

    void tamanho(int largura, int altura) {}
    boolean disponivel() { return false; }
    void desligar(Throwable motivo) {}
    String motivoFalha() { return null; }
    void tentarDeNovo() {}
    boolean contextoRecriado() { return false; }
    void soltarThread() {}
    void liberar() {}
    Canvas comecarQuadro() { return null; }
    void terminarQuadro() {}
    void semMundo() {}
    void manutencao() {}
    void sprite(Canvas c, Bitmap folha, Rect fonte, RectF destino, int alpha, Filtro filtro) {}
    void aquecer(com.gamelutagpt.stage.StageScene cenario) {}
    boolean preSubir(com.gamelutagpt.render.RenderImage imagem) { return false; }
    int subir(SpriteAtlasCache atlases, long orcamentoNs) { return 0; }
    String resumo() { return ""; }
}
