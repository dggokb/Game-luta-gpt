package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.gamelutagpt.pc.DadosDoJogo;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Joga os movimentos e os golpes de um lutador no jogo de verdade do PC, pelo controle (o pad do
 * jogo), contra um boneco parado, e grava os quadros de cada um numa pasta: idle, agachar e ficar
 * agachado, golpes agachados, L, M, H, →+M, andar para frente e para trás, dash e backdash.
 *
 * <pre>gradle :pc:testeMovimentos -Plutador=ryuga_8 [-Prival=koga_8]</pre>
 */
public final class TesteMovimentosPc {
    private TesteMovimentosPc() {}

    static GameView jogo;
    static Method avancar, desenhar;
    static PadInput pad;
    static Bitmap tela;
    static File saida;
    static int quadro;
    static String trecho = "inicio";

    public static void main(String[] args) throws Exception {
        saida = new File(args[0]);
        String lutador = args[1], rival = args[2];
        saida.mkdirs();
        Context ctx = DadosDoJogo.localizar().contexto(null);
        jogo = new GameView(ctx);
        jogo.setSize(1280, 720);
        jogo.setTouchControlsVisible(false);
        avancar = metodo("advanceSimulation", float.class);
        desenhar = metodo("renderFrame", Canvas.class);
        metodo("onSizeChanged", int.class, int.class, int.class, int.class).invoke(jogo, 1280, 720, 0, 0);
        campo("activityActive").set(jogo, true);
        tela = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
        pad = (PadInput)campo("pad").get(jogo);
        metodo("selectTeamsNow", String.class, String.class, String.class, String.class, boolean.class)
            .invoke(jogo, lutador, rival, rival, lutador, true);
        metodo("setOpponentAiEnabled", boolean.class).invoke(jogo, false);
        // Depois da intro e do LUTEM!.
        for (int i = 0; i < 60 * 12 && ((int)campo("introFrame").get(jogo) >= 0 || ((BattleFlow)campo("flow").get(jogo)).holdsFighters()); i++) passo(false);
        for (int i = 0; i < 30; i++) passo(false);

        trecho("idle", 0, null, 40);
        trecho("agachar", 3, null, 70);
        solta(20);
        trecho("2L", 3, PadInput.Button.LIGHT, 45);
        trecho("2M", 3, PadInput.Button.MEDIUM, 45);
        trecho("2H", 3, PadInput.Button.HEAVY, 55);
        solta(25);
        trecho("L", 0, PadInput.Button.LIGHT, 35);
        trecho("M", 0, PadInput.Button.MEDIUM, 40);
        trecho("H", 0, PadInput.Button.HEAVY, 50);
        trecho("6M", 1, PadInput.Button.MEDIUM, 50);
        solta(20);
        trecho("tras", 5, null, 70);
        trecho("frente", 1, null, 70);
        solta(20);
        // Dash e backdash: dois toques.
        trecho = "dash";
        toques(1);
        for (int i = 0; i < 40; i++) { pad.setDirection(1); passo(i % 2 == 0); }
        solta(30);
        trecho = "backdash";
        toques(5);
        for (int i = 0; i < 45; i++) { pad.setDirection(0); passo(i % 2 == 0); }
        System.out.println("quadros em " + saida);
        System.exit(0);
    }

    static void toques(int dir) throws Exception {
        pad.setDirection(dir); passo(true); passo(true);
        pad.setDirection(0); passo(true); passo(true);
        pad.setDirection(dir); passo(true); passo(true);
    }

    static void trecho(String nome, int direcao, PadInput.Button botao, int quadros) throws Exception {
        trecho = nome;
        for (int i = 0; i < quadros; i++) {
            pad.setDirection(direcao);
            if (botao != null && i == 6) pad.press(botao);
            passo(i % 2 == 0);
        }
    }

    static void solta(int quadros) throws Exception {
        pad.setDirection(0);
        for (int i = 0; i < quadros; i++) passo(false);
    }

    static void passo(boolean gravar) throws Exception {
        avancar.invoke(jogo, CombatConfig.DT);
        quadro++;
        if (!gravar) return;
        Canvas canvas = new Canvas(tela);
        canvas.drawColor(0xFF000000);
        desenhar.invoke(jogo, canvas);
        ImageIO.write(tela.image(), "png", new File(saida, String.format(Locale.ROOT, "%s_%05d.png", trecho, quadro)));
    }

    static Method metodo(String nome, Class<?>... tipos) throws Exception {
        Method m = GameView.class.getDeclaredMethod(nome, tipos);
        m.setAccessible(true);
        return m;
    }

    static Field campo(String nome) throws Exception {
        Field f = GameView.class.getDeclaredField(nome);
        f.setAccessible(true);
        return f;
    }
}
