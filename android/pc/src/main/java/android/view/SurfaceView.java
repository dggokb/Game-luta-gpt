package android.view;

import android.content.Context;
import android.graphics.Canvas;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * No PC a "superfície" é a janela: quem a hospeda entrega um {@link Target} que fornece o
 * Canvas de cada quadro e mostra o quadro pronto.
 */
public class SurfaceView extends View {
    /** Extensão do PC: o destino real dos quadros (janela ou imagem fora da tela). */
    public interface Target {
        Canvas beginFrame();
        void endFrame(Canvas canvas);
        /** Tamanho fixo do buffer (0 = o da tela): o jogo desenha menor e quem mostra amplia. */
        default void setTamanhoFixo(int width, int height) {}
        /** Tamanho do quadro desenhado pela placa de vídeo (0 = sem OpenGL). */
        default int larguraGl() { return 0; }
        default int alturaGl() { return 0; }
        /** O quadro desenhado fora do Canvas (OpenGL), pronto: quem mostra o leva à tela. */
        default void mostrarPronto(java.awt.image.BufferedImage imagem) {}
    }

    private final Holder holder = new Holder();

    public SurfaceView(Context context) { super(context); }

    public SurfaceHolder getHolder() { return holder; }

    /** Extensão do PC: liga a superfície ao destino e avisa o jogo (surfaceCreated). */
    public void attach(Target target, int width, int height) {
        setSize(width, height);
        holder.target = target;
        target.setTamanhoFixo(holder.fixedWidth, holder.fixedHeight);
        holder.surface.setValid(true);
        for (SurfaceHolder.Callback c : holder.callbacks) c.surfaceCreated(holder);
        for (SurfaceHolder.Callback c : holder.callbacks) c.surfaceChanged(holder, 1, width, height);
    }

    /** Extensão do PC: o destino dos quadros agora (null sem janela). */
    public Target alvo() { return holder.target; }

    /** Extensão do PC: nova dimensão da janela. */
    public void resize(int width, int height) {
        setSize(width, height);
        for (SurfaceHolder.Callback c : holder.callbacks) c.surfaceChanged(holder, 1, width, height);
    }

    /** Extensão do PC: a janela fechou (surfaceDestroyed para o loop do jogo). */
    public void detach() {
        holder.surface.setValid(false);
        for (SurfaceHolder.Callback c : holder.callbacks) c.surfaceDestroyed(holder);
        holder.target = null;
    }

    private static final class Holder implements SurfaceHolder {
        final List<Callback> callbacks = new CopyOnWriteArrayList<>();
        final Surface surface = new Surface();
        volatile Target target;

        @Override public void addCallback(Callback callback) { callbacks.add(callback); }

        @Override public void removeCallback(Callback callback) { callbacks.remove(callback); }

        @Override public Surface getSurface() { return surface; }

        @Override public Canvas lockCanvas() {
            Target t = target;
            return t == null ? null : t.beginFrame();
        }

        @Override public Canvas lockHardwareCanvas() { return lockCanvas(); }

        @Override public void unlockCanvasAndPost(Canvas canvas) {
            Target t = target;
            if (t != null && canvas != null) t.endFrame(canvas);
        }

        private volatile int fixedWidth, fixedHeight;

        @Override public void setFixedSize(int width, int height) {
            fixedWidth = width;
            fixedHeight = height;
            Target t = target;
            if (t != null) t.setTamanhoFixo(width, height);
        }

        @Override public void setSizeFromLayout() { setFixedSize(0, 0); }
    }
}
