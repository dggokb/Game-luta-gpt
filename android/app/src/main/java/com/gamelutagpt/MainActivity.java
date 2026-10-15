package com.gamelutagpt;

import android.app.Activity;
import android.os.Bundle;
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
        // O GameView nasce leve e decodifica a arte inicial na thread "Carregando", com a tela
        // de carregamento na frente: a thread da UI fica livre para os toques (sem ANR).
        gameView = new GameView(this, true);
        setContentView(gameView);
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
