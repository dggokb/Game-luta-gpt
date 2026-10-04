package com.gamelutagpt;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private Fighter3DView fighter3DView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        hideSystemUi();

        Fighter3DState fighter3DState = new Fighter3DState();
        GameView gameView = new GameView(this, fighter3DState);
        fighter3DView = new Fighter3DView(this, fighter3DState);

        // The GL layer is visually on top, so forward all multi-touch input to
        // the unchanged gameplay SurfaceView.
        fighter3DView.setOnTouchListener(
            (view, event) -> gameView.onTouchEvent(event)
        );

        FrameLayout root = new FrameLayout(this);
        root.addView(
            gameView,
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        );
        root.addView(
            fighter3DView,
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        );

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fighter3DView != null) {
            fighter3DView.onResume();
        }
        hideSystemUi();
    }

    @Override
    protected void onPause() {
        if (fighter3DView != null) {
            fighter3DView.onPause();
        }
        super.onPause();
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
