package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.gamelutagpt.pc.DadosDoJogo;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;

/**
 * Revisão do motor de animação: o mesmo roteiro de luta (pulo, dash, L > M > H no CPU,
 * backdash, pulo para a frente, pulo saído da corrida, super pulo, pulo por cima do CPU,
 * 2H, andar, o cansaço com pouca vida e três especiais: S1 (energia), S2 e S4) jogado quadro a quadro, sem tela, com o motor desligado e
 * ligado. Os quadros saem em PNG para comparar lado a lado (e virar vídeo com o ffmpeg).
 *
 * <pre>gradle :pc:revisarAnimacao   →   pc/build/revisao-animacao/{desligado,ligado}/0000.png</pre>
 */
public final class RevisaoAnimacao {
    /** Quadros do roteiro (60 por segundo). */
    static final int QUADROS = 1140;
    /** Tamanho dos PNGs (metade da tela virtual). */
    static final int LARGURA = 960, ALTURA = 540;

    public static void main(String[] args) throws Exception {
        File saida = new File(args.length > 0 ? args[0] : "build/revisao-animacao");
        Context contexto = DadosDoJogo.localizar().contexto(null);
        // -Dmodos=desligado,ligado (padrão): uma pasta por modo, na ordem.
        for (String modo : System.getProperty("modos", "desligado,ligado").split(",")) {
            AnimacaoLutador.AJUSTES.ligado = modo.startsWith("ligado");
            AnimacaoLutador.AJUSTES.intensidade = 1f;
            jogar(contexto, new File(saida, modo));
        }
        System.out.println("Revisão do motor de animação em " + saida.getAbsolutePath());
    }

    private static void jogar(Context contexto, File pasta) throws Exception {
        if (!pasta.isDirectory() && !pasta.mkdirs()) throw new IOException("não criou " + pasta);
        GameView jogo = new GameView(contexto);
        campo(jogo, "introFrame").setInt(jogo, -1);
        // Sem o controle de toque por cima dos lutadores.
        jogo.setTouchControlsVisible(false);
        Method avancar = metodo("advanceSimulation", float.class);
        Method desenhar = metodo("renderFrame", Canvas.class);
        Bitmap tela = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
        BufferedImage menor = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_INT_RGB);
        CombatFighter p = jogo.engine.fighter(GameView.PLAYER), cpu = jogo.engine.fighter(GameView.OPPONENT);
        StringBuilder estado = new StringBuilder(), poses = new StringBuilder();
        Field renderer = campo(jogo, "spriteFighterRenderer");
        for (int q = 0; q < QUADROS; q++) {
            roteiro(jogo.pad, q, p, cpu);
            avancar.invoke(jogo, CombatConfig.DT);
            // O estado da luta em texto: com o motor ligado ou não, tem de sair igual.
            estado.append(String.format(java.util.Locale.ROOT, "%d %s %.1f %.1f %s %.1f %.1f%n", q,
                p.status, p.x, p.y, cpu.status, cpu.x, cpu.y));
            // Como o loop do jogo: um Canvas novo por quadro, sobre a tela limpa (estado do Java2D
            // que sobrasse de um quadro passava para o seguinte).
            // A pose do motor de quem joga e o tranco da câmera (para achar balanço sem olhar quadro a quadro).
            com.gamelutagpt.motion.Pose pose = ((SpriteFighterRenderer)renderer.get(jogo)).animacao.pose();
            poses.append(String.format(java.util.Locale.ROOT, "%d %s %.2f %.2f %.3f %.3f%n", q, p.status,
                pose.inclinacao, pose.arrasto, pose.escalaX, pose.escalaY));
            Canvas canvas = new Canvas(tela);
            canvas.drawColor(0xFF000000);
            desenhar.invoke(jogo, canvas);
            Graphics2D g = menor.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(tela.image(), 0, 0, LARGURA, ALTURA, null);
            g.dispose();
            ImageIO.write(menor, "png", new File(pasta, String.format("%04d.png", q)));
        }
        java.nio.file.Files.write(new File(pasta, "estado.txt").toPath(),
            estado.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.write(new File(pasta, "pose.txt").toPath(),
            poses.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** O que o jogador aperta em cada quadro (direções da tela: 1 →, 3 ↓, 5 ←, 7 ↑, 8 ↗). */
    private static void roteiro(PadInput pad, int q, CombatFighter p, CombatFighter cpu) {
        if (q == 0) colocar(p, cpu, 420f, 700f);
        // Pulo parado: decola e aterrissa.
        if (q == 20) pad.setDirection(7);
        if (q == 23) pad.setDirection(0);
        // Dash para a frente (→ →) e para.
        if (q == 90) colocar(p, cpu, 300f, 900f);
        if (q == 92) pad.setDirection(1);
        if (q == 95) pad.setDirection(0);
        if (q == 98) pad.setDirection(1);
        if (q == 118) pad.setDirection(0);
        // L > M > H colado no CPU (ele apanha).
        if (q == 150) colocar(p, cpu, 520f, 640f);
        if (q == 152) pad.press(PadInput.Button.LIGHT);
        if (q == 168) pad.press(PadInput.Button.MEDIUM);
        if (q == 186) pad.press(PadInput.Button.HEAVY);
        // Backdash (← ←).
        if (q == 250) colocar(p, cpu, 640f, 900f);
        if (q == 252) pad.setDirection(5);
        if (q == 255) pad.setDirection(0);
        if (q == 258) pad.setDirection(5);
        if (q == 262) pad.setDirection(0);
        // Pulo para a frente e aterrissagem.
        if (q == 300) colocar(p, cpu, 400f, 900f);
        if (q == 302) pad.setDirection(8);
        if (q == 305) pad.setDirection(0);
        // Pulo saído da corrida (→ → e ↗): leva o embalo e o rastro.
        if (q == 380) colocar(p, cpu, 250f, 1100f);
        if (q == 382) pad.setDirection(1);
        if (q == 385) pad.setDirection(0);
        if (q == 388) pad.setDirection(1);
        if (q == 398) pad.setDirection(8);
        if (q == 402) pad.setDirection(0);
        // Super pulo (↓ e ↑): sem rastro.
        if (q == 470) colocar(p, cpu, 400f, 1000f);
        if (q == 472) pad.setDirection(3);
        if (q == 476) pad.setDirection(7);
        if (q == 480) pad.setDirection(0);
        // Pulo por cima do CPU (cross-up), depois de o super pulo aterrissar: os dois viram de lado.
        if (q == 590) colocar(p, cpu, 620f, 700f);
        if (q == 592) pad.setDirection(8);
        if (q == 595) pad.setDirection(0);
        // 2H colado: o lançador joga o CPU para cima.
        if (q == 660) colocar(p, cpu, 600f, 670f);
        if (q == 662) pad.setDirection(3);
        if (q == 664) pad.press(PadInput.Button.HEAVY);
        if (q == 680) pad.setDirection(0);
        // Andando para a frente (os pés no chão).
        if (q == 780) colocar(p, cpu, 300f, 1100f);
        if (q == 782) pad.setDirection(1);
        if (q == 832) pad.setDirection(0);
        // Pouca vida: parado e cansado.
        if (q == 840) {
            colocar(p, cpu, 500f, 900f);
            p.state.life = p.state.profile.maxLife / 10;
        }
        // Especiais (direções da tela): S1 energia ↓↘→ + M, S2 →↓↘ + H, S4 ↓↙← + H.
        if (q == 940) colocar(p, cpu, 500f, 760f);
        if (q == 942) pad.setDirection(3);
        if (q == 944) pad.setDirection(2);
        if (q == 946) { pad.setDirection(1); pad.press(PadInput.Button.MEDIUM); }
        if (q == 950) pad.setDirection(0);
        if (q == 1010) colocar(p, cpu, 500f, 640f);
        if (q == 1012) pad.setDirection(1);
        if (q == 1014) pad.setDirection(3);
        if (q == 1016) { pad.setDirection(2); pad.press(PadInput.Button.HEAVY); }
        if (q == 1020) pad.setDirection(0);
        if (q == 1070) colocar(p, cpu, 500f, 680f);
        if (q == 1072) pad.setDirection(3);
        if (q == 1074) pad.setDirection(4);
        if (q == 1076) { pad.setDirection(5); pad.press(PadInput.Button.HEAVY); }
        if (q == 1080) pad.setDirection(0);
    }

    /** Põe os dois no lugar e de frente um para o outro (entre as cenas). */
    private static void colocar(CombatFighter p, CombatFighter cpu, float xp, float xc) {
        p.x = xp;
        cpu.x = xc;
        // Vida cheia a cada cena: o roteiro nunca termina num K.O.
        p.state.restoreLife();
        cpu.state.restoreLife();
    }

    private static Field campo(GameView jogo, String nome) throws Exception {
        Field f = GameView.class.getDeclaredField(nome);
        f.setAccessible(true);
        return f;
    }

    private static Method metodo(String nome, Class<?>... tipos) throws Exception {
        Method m = GameView.class.getDeclaredMethod(nome, tipos);
        m.setAccessible(true);
        return m;
    }
}
