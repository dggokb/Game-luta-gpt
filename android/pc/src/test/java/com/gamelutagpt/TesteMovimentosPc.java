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
 * agachado, golpes agachados, L, M, H, →+M, pulo e golpes no ar, andar para frente e para trás,
 * dash, backdash e o segundo idle.
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
        medirEscalas(lutador);
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
        // Pulo parado e os golpes no ar (↑ e o botão no alto; j2H segurando ↓).
        pulo("pulo", null, false);
        pulo("jL", PadInput.Button.LIGHT, false);
        pulo("jM", PadInput.Button.MEDIUM, false);
        pulo("jH", PadInput.Button.HEAVY, false);
        pulo("j2H", PadInput.Button.HEAVY, true);
        // S3 do p01: meia-lua para trás (↓↙←) + L, o chute giratório inteiro.
        trecho = "S3";
        pad.setDirection(3); passo(false); passo(false);
        pad.setDirection(4); passo(false); passo(false);
        pad.setDirection(5); passo(false);
        pad.press(PadInput.Button.LIGHT);
        for (int i = 0; i < 330; i++) { pad.setDirection(0); passo(i % 6 == 0); }
        solta(20);
        // S2 do p01 (→↓↘ + L, o punho ascendente) e S4 (↓↓ + L, o Aparar).
        comando("S2p", new int[]{1, 3, 2}, PadInput.Button.LIGHT, 90);
        comando("S4", new int[]{3, 0, 3}, PadInput.Button.LIGHT, 110);
        // S1 (↓↘→ + L), o S1 segurando o botão (carregado) e o Super: o projétil anda até o boneco.
        especial("S1", PadInput.Button.LIGHT, 0, 90);
        especial("S1c", PadInput.Button.LIGHT, 70, 120);
        jogo.engine.fighter(GameView.PLAYER).state.superMeter = CombatConfig.SUPER_COST;
        especial("Super", PadInput.Button.SUPER, -1, 150);
        // Ultra (↓ + SUPER com três barras): a abertura, a carga e o raio até o boneco.
        solta(40);
        jogo.engine.fighter(GameView.PLAYER).state.superMeter = CombatConfig.ULTRA_COST;
        trecho = "Ultra";
        pad.setDirection(3); passo(false);
        pad.press(PadInput.Button.SUPER);
        passo(false);   // o ↓ junto com o SUPER no mesmo quadro
        for (int i = 0; i < 60 * 14; i++) { pad.setDirection(0); passo(i % 8 == 0); }
        solta(30);
        // SHINKAI (a ativação do vídeo shinai) e o Ultra SHINKAI (↓ + SUPER no SHINKAI): o dragão azul.
        jogo.engine.fighter(GameView.PLAYER).state.shinkai = CombatConfig.SHINKAI_MAX;
        trecho = "Shinkai";
        pad.press(PadInput.Button.SHINKAI);
        for (int i = 0; i < 60 * 6; i++) { pad.setDirection(0); passo(i % 8 == 0); }
        jogo.engine.fighter(GameView.PLAYER).state.superMeter = CombatConfig.ULTRA_SHINKAI_COST;
        trecho = "UltraShinkai";
        pad.setDirection(3); passo(false);
        pad.press(PadInput.Button.SUPER);
        passo(false);
        for (int i = 0; i < 60 * 10; i++) { pad.setDirection(0); passo(i % 6 == 0); }
        solta(30);
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
        // Parado um tempo: o segundo idle (IDLE_2) entra a cada 3 voltas do idle.
        trecho = "idle2";
        for (int i = 0; i < 60 * 16; i++) { pad.setDirection(0); passo(i % 6 == 0); }
        System.out.println("quadros em " + saida);
        System.exit(0);
    }

    static void pulo(String nome, PadInput.Button botao, boolean baixo) throws Exception {
        trecho = nome;
        for (int i = 0; i < 80; i++) {
            pad.setDirection(i < 3 ? 7 : baixo && i >= 12 ? 3 : 0);
            if (botao != null && i == 14) pad.press(botao);
            passo(i % 2 == 0);
        }
        solta(20);
    }

    /** Meia-lua para a frente (↓↘→) e o botão; segura o botão {@code segura} quadros (carga). -1: só o botão. */
    static void especial(String nome, PadInput.Button botao, int segura, int quadros) throws Exception {
        trecho = nome;
        if (segura >= 0) {
            pad.setDirection(3); passo(false); passo(false);
            pad.setDirection(2); passo(false); passo(false);
            pad.setDirection(1); passo(false);
        }
        pad.press(botao);
        for (int i = 0; i < quadros; i++) {
            pad.setDirection(0);
            pad.setHolding(segura > 0 && i < segura);
            passo(i % 3 == 0);
        }
        pad.setHolding(false);
        solta(20);
    }

    /** Uma sequência de direções (2 quadros cada) e o botão; grava {@code quadros} quadros. */
    static void comando(String nome, int[] direcoes, PadInput.Button botao, int quadros) throws Exception {
        trecho = nome;
        for (int d : direcoes) { pad.setDirection(d); passo(false); passo(false); }
        pad.press(botao);
        for (int i = 0; i < quadros; i++) { pad.setDirection(0); passo(i % 3 == 0); }
        solta(30);
    }

    /**
     * Altura e largura do desenho (px da tela, pixels opacos) do 1º quadro de cada animação, numa
     * tela vazia: confere se tudo está na mesma escala (idle, pulo, golpes no ar, agachado).
     */
    static void medirEscalas(String lutador) throws Exception {
        SpriteFighterRenderer r = (SpriteFighterRenderer)campo("spriteFighterRenderer").get(jogo);
        for (String clip : new String[]{"IDLE", "JUMP", "FALL", "LAND", "JUMP_LIGHT", "JUMP_MEDIUM", "JUMP_HEAVY",
                "JUMP_HEAVY_DOWN", "CROUCH_IDLE", "SPECIAL_S2", "HEAVY_STRAIGHT"}) {
            if (!r.hasAnimation(clip)) continue;
            for (float t : new float[]{0f, 0.25f}) {
                Bitmap b = Bitmap.createBitmap(900, 900, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                r.motion.clip = clip;
                r.motion.time = t;
                r.draw(c, 450f, 800f, 1, false, false);
                java.awt.image.BufferedImage im = b.image();
                int x0 = 900, x1 = -1, y0 = 900, y1 = -1;
                for (int y = 0; y < 900; y++) for (int x = 0; x < 900; x++) {
                    if ((im.getRGB(x, y) >>> 24) > 128) { x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
                }
                System.out.println("ESCALA " + clip + " t=" + t + " altura " + (y1 - y0) + " largura " + (x1 - x0) + " pe " + y1);
            }
        }
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
