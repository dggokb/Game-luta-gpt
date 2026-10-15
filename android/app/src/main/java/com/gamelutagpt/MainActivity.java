package com.gamelutagpt;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private GameView gameView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        hideSystemUi();
        // Montar o jogo decodifica a arte inicial (alguns segundos): primeiro aparece a tela de
        // carregamento, e o GameView é criado logo depois que ela foi desenhada.
        setContentView(new Carregando(this, () -> {
            if (isFinishing() || gameView != null) return;
            gameView = new GameView(this);
            setContentView(gameView);
        }));
    }

    /** A mesma TelaCarregando do jogo, enquanto o GameView não existe. */
    private static final class Carregando extends View {
        private final TelaCarregando tela = new TelaCarregando();
        private final long since = SystemClock.uptimeMillis();
        private Runnable depoisDoPrimeiroQuadro;

        Carregando(Context context, Runnable depoisDoPrimeiroQuadro) {
            super(context);
            this.depoisDoPrimeiroQuadro = depoisDoPrimeiroQuadro;
        }

        @Override protected void onDraw(Canvas canvas) {
            tela.draw(canvas, SystemClock.uptimeMillis() - since);
            Runnable r = depoisDoPrimeiroQuadro;
            if (r != null) {
                depoisDoPrimeiroQuadro = null;
                // Depois deste quadro ir para a tela.
                post(r);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView!=null) gameView.resumeGame();
        hideSystemUi();
    }

    @Override protected void onPause() {
        if (gameView!=null) gameView.pauseGame();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (gameView!=null) gameView.release();
        super.onDestroy();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
}
