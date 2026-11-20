package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.gamelutagpt.historia.Campanha;
import com.gamelutagpt.historia.Episodio;
import com.gamelutagpt.historia.PaginaHistoria;
import com.gamelutagpt.historia.Temporada;
import com.gamelutagpt.pc.DadosDoJogo;
import com.gamelutagpt.render.RenderAssets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * Grava em vídeo (MP4, 30 por segundo) a luta da Semifinal do modo história, a de verdade (o
 * passo de luta do ep06, com a Bolsa de Apostas), com a CPU jogando dos dois lados: o vídeo
 * mostra a placa, as fichas, o Apostador falando e o castigo de quem esfria.
 *
 * <pre>gradle :pc:gravarSemifinal [-Pirmao=koga] [-Psegundos=75] [-Psaida=arquivo.mp4]</pre>
 *
 * A campanha começa com as quartas vencidas (o adversário é o da Semifinal na chave). Os quadros
 * vão direto para o ffmpeg (precisa estar no PATH).
 */
public final class GravarSemifinal {
    private GravarSemifinal() {}

    static GameView jogo;
    static Method avancar, desenhar;
    static Bitmap tela;

    public static void main(String[] args) throws Exception {
        File saida = new File(args[0]);
        String irmao = args[1];
        int segundos = Integer.parseInt(args[2]);
        int largura = 1280, altura = 720;
        Context ctx = DadosDoJogo.localizar().contexto(null);
        jogo = new GameView(ctx);
        jogo.setSize(largura, altura);
        jogo.setTouchControlsVisible(false);
        avancar = metodo("advanceSimulation", float.class);
        desenhar = metodo("renderFrame", Canvas.class);
        metodo("onSizeChanged", int.class, int.class, int.class, int.class).invoke(jogo, largura, altura, 0, 0);
        campo("activityActive").set(jogo, true);
        tela = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888);

        metodo("loadTemporada").invoke(jogo);
        RenderAssets assets = (RenderAssets)campo("ultraAssets").get(jogo);
        Campanha campanha = Campanha.load(assets, Long.getLong("gameluta.semente", 7L));
        // As quartas já vencidas: a próxima luta da chave é a Semifinal.
        campanha.restaurar(irmao, 1);
        PaginaHistoria historia = (PaginaHistoria)campo("historia").get(jogo);
        campo("storyMode").set(jogo, true);
        historia.setCampanha(campanha);
        Temporada temporada = (Temporada)campo("temporada").get(jogo);
        String passo = null;
        for (String linha : assets.readText(Temporada.pasta("ep06") + "/episodio.json").split("\n")) {
            if (linha.contains("\"aposta\": true")) passo = linha.trim().replaceAll(",$", "");
        }
        if (passo == null) throw new IllegalStateException("ep06 sem a luta da aposta");
        Episodio so = Episodio.fromJson("{\"id\": \"ep06\", \"passos\": [" + passo + ", {\"tipo\": \"proximo\", \"titulo\": \"fim\"}]}");
        historia.start(temporada, so);
        for (int i = 0; i < 1200 && !historia.waitingFight(); i++) avancar.invoke(jogo, CombatConfig.DT);
        if (!historia.waitingFight()) throw new IllegalStateException("a história não chegou na luta");
        System.out.println("semifinal: " + jogo.engine.fighter(GameView.PLAYER).state.character.id + " × " +
            jogo.engine.fighter(GameView.OPPONENT).state.character.id);

        // A CPU joga pelo jogador.
        AiController cpu = new AiController(new OpponentAi(new Random(7)), new Random(77));
        cpu.setEnabled(true);
        cpu.setFullKit(true);
        jogo.cpuNoJogador = cpu;

        Process ffmpeg = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "bgra",
            "-s", largura + "x" + altura, "-r", "30", "-i", "-", "-c:v", "libx264", "-preset", "medium", "-crf", "20",
            "-pix_fmt", "yuv420p", "-movflags", "+faststart", saida.getAbsolutePath()).inheritIO()
            .redirectInput(ProcessBuilder.Redirect.PIPE).start();
        OutputStream video = ffmpeg.getOutputStream();
        byte[] bytes = new byte[largura * altura * 4];
        int[] px = new int[largura * altura];
        BattleFlow flow = (BattleFlow)campo("flow").get(jogo);
        int quadros = 0, depoisDoFim = -1, limite = segundos * 60;
        while (quadros < limite && depoisDoFim != 0) {
            avancar.invoke(jogo, CombatConfig.DT);
            quadros++;
            if (depoisDoFim > 0) depoisDoFim--;
            else if (depoisDoFim < 0 && flow.over()) depoisDoFim = 4 * 60;
            if (quadros % 2 != 0) continue;
            Canvas canvas = new Canvas(tela);
            canvas.drawColor(0xFF000000);
            desenhar.invoke(jogo, canvas);
            BufferedImage img = tela.image();
            img.getRGB(0, 0, largura, altura, px, 0, largura);
            for (int i = 0, j = 0; i < px.length; i++, j += 4) {
                int c = px[i];
                bytes[j] = (byte)c;
                bytes[j + 1] = (byte)(c >> 8);
                bytes[j + 2] = (byte)(c >> 16);
                bytes[j + 3] = (byte)0xFF;
            }
            video.write(bytes);
            if (quadros % 600 == 0) System.out.println((quadros / 60) + " s");
        }
        video.close();
        int fim = ffmpeg.waitFor();
        System.out.println("vídeo: " + saida + " (" + quadros / 60 + " s, ffmpeg " + fim + ")");
        System.exit(fim);
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
