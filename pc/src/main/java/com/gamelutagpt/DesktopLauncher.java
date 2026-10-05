package com.gamelutagpt;

import android.content.Context;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

public final class DesktopLauncher {
    private DesktopLauncher() {}

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            Context context = new Context();
            GameView gameView = new GameView(context);
            gameView.setPreferredSize(new Dimension(1280, 720));

            JFrame frame = new JFrame("Game Luta Sprite GPT - PC");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setContentPane(gameView);
            frame.pack();
            frame.setMinimumSize(new Dimension(960, 540));
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    gameView.pauseGame();
                    frame.dispose();
                    System.exit(0);
                }
            });
            frame.setVisible(true);
            gameView.requestFocusInWindow();
        });
    }
}
