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

    DesenhoGl(SurfaceView view, SurfaceView jogo, Ouvinte ouvinte) {}

    void tamanho(int largura, int altura) {}
    boolean disponivel() { return false; }
    boolean contextoRecriado() { return false; }
    void soltarThread() {}
    void liberar() {}
    boolean comecarQuadro(int largura, int altura) { return false; }
    void terminarQuadro() {}
    void semMundo() {}
    void manutencao() {}
    RenderCanvas mundo(Canvas c) { throw new UnsupportedOperationException(); }
    void sprite(Canvas c, Bitmap folha, Rect fonte, RectF destino, int alpha, Filtro filtro) {}
    void recorte(Canvas c, float l, float t, float r, float b) {}
    void semRecorte() {}
    void fillTriangles(Canvas c, float[] xy, int n, int argb) {}
    void matrizNoCanvas(Canvas c) {}
    int subir(SpriteAtlasCache atlases, long orcamentoNs) { return 0; }
    String resumo() { return ""; }
}
