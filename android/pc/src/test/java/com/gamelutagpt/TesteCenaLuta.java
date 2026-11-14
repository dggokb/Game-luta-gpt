package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import com.gamelutagpt.historia.Campanha;
import com.gamelutagpt.historia.CinematicaLuta;
import com.gamelutagpt.historia.Episodio;
import com.gamelutagpt.historia.PaginaHistoria;
import com.gamelutagpt.historia.Temporada;
import com.gamelutagpt.pc.DadosDoJogo;
import com.gamelutagpt.render.RenderAssets;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Queue;
import javax.imageio.ImageIO;

/**
 * Joga a cinemática de uma luta da história no jogo de verdade do PC, direto na luta (sem as
 * páginas antes): abre o passo de luta {@code marca} do episódio na campanha do irmão, tira o
 * lutador do jogador até a vida do gatilho e toca a cena, acertando todos os QTE pelo caminho do
 * jogo (toques na fila da tela). Grava um PNG a cada 6 quadros (10 por segundo).
 *
 * <pre>gradle :pc:testeCena -Pepisodio=ep00 -Pmarca=briga-irmaos [-Pirmao=koga] [-Pvida=0.49]</pre>
 *
 * Serve para conferir as cenas em vídeo ({@code video} no roteiro): os dois lutadores somem e
 * o vídeo toca no chão, no meio deles.
 */
public final class TesteCenaLuta {
    private TesteCenaLuta() {}

    static GameView jogo;
    static Method avancar, desenhar;
    static Bitmap tela;
    static File saida;
    static int quadro;

    public static void main(String[] args) throws Exception {
        saida = new File(args[0]);
        String irmao = args[1], episodio = args[2], marca = args[3];
        float vida = Float.parseFloat(args[4]);
        saida.mkdirs();
        Context ctx = DadosDoJogo.localizar().contexto(null);
        jogo = new GameView(ctx);
        jogo.setSize(1280, 720);
        jogo.setTouchControlsVisible(true);
        avancar = metodo("advanceSimulation", float.class);
        desenhar = metodo("renderFrame", Canvas.class);
        metodo("onSizeChanged", int.class, int.class, int.class, int.class).invoke(jogo, 1280, 720, 0, 0);
        campo("activityActive").set(jogo, true);
        tela = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);

        metodo("loadTemporada").invoke(jogo);
        RenderAssets assets = (RenderAssets)campo("ultraAssets").get(jogo);
        Campanha campanha = Campanha.load(assets, 7L);
        campanha.restaurar(irmao, -1);
        PaginaHistoria historia = (PaginaHistoria)campo("historia").get(jogo);
        campo("storyMode").set(jogo, true);
        historia.setCampanha(campanha);
        Temporada temporada = (Temporada)campo("temporada").get(jogo);
        // Um episódio só com a luta pedida (a linha dela no episodio.json, que tem um passo por
        // linha), para a história esperar a luta como no jogo: o gatilho da cena só olha a luta
        // que a história está esperando.
        String passo = null;
        for (String linha : assets.readText(Temporada.pasta(episodio) + "/episodio.json").split("\n")) {
            if (linha.contains("\"marca\": \"" + marca + "\"") && (!linha.contains("\"irmao\"") || linha.contains("\"irmao\": \"" + irmao + "\""))) {
                passo = linha.trim().replaceAll(",$", "");
            }
        }
        if (passo == null) throw new IllegalStateException("sem a luta " + marca + " em " + episodio);
        Episodio so = Episodio.fromJson("{\"id\": \"" + episodio + "\", \"passos\": [" + passo + ", {\"tipo\": \"proximo\", \"titulo\": \"fim\"}]}");
        historia.start(temporada, so);
        for (int i = 0; i < 600 && !historia.waitingFight(); i++) passo(false);
        if (!historia.waitingFight()) throw new IllegalStateException("a história não chegou na luta");
        System.out.println("luta: " + jogo.engine.fighter(GameView.PLAYER).state.character.id + " × " +
            jogo.engine.fighter(GameView.OPPONENT).state.character.id);
        CinematicaLuta cena = (CinematicaLuta)campo("cinematica").get(jogo);
        if (!cena.armada()) throw new IllegalStateException("a cinemática não armou");

        // Depois da intro, o jogador fica com a vida do gatilho.
        int limite = 60 * 60;
        boolean bateu = false;
        while (!cena.rodando() && limite-- > 0) {
            passo(quadro % 30 == 0 && !SO_ANIMACAO);
            if (!bateu && (int)campo("introFrame").get(jogo) < 0 && !((BattleFlow)campo("flow").get(jogo)).holdsFighters() && quadro % 120 == 0) {
                FighterState f = jogo.engine.fighter(GameView.PLAYER).state;
                f.takeDamage(f.life - Math.round(f.profile.maxLife * vida), 0);
                bateu = true;
            }
        }
        System.out.println("cena começou no quadro " + quadro);

        String video = null;
        int noQte = 0, qtes = 0;
        boolean pedia = false;
        limite = 60 * 120;
        while (!cena.acabou() && limite-- > 0) {
            boolean pede = cena.qtePedindo();
            if (pede && !pedia) {
                noQte = 0;
                if (cena.qteErros() == 0) qtes++;
                System.out.println("QTE " + cena.qteTipo() + " no quadro " + quadro + (cena.qteErros() > 0 ? " (de novo)" : ""));
            }
            pedia = pede;
            if (pede) {
                noQte++;
                CinematicaLuta.TipoQte tipo = cena.qteTipo();
                boolean toca = tipo == CinematicaLuta.TipoQte.MARTELAR ? noQte % 5 == 0
                    : tipo == CinematicaLuta.TipoQte.TEMPO ? noQte == Math.round(cena.qteSegundos() * 60f) : noQte == 20;
                // -PerraPrimeiro: o primeiro QTE fica sem resposta (o erro e o pedido de novo).
                if (ERRA_PRIMEIRO && qtes == 1 && cena.qteErros() == 0) toca = false;
                if (toca) {
                    toque(MotionEvent.ACTION_DOWN, 640f, 420f);
                    toque(MotionEvent.ACTION_UP, 640f, 420f);
                }
            }
            String agora = cena.video();
            if (agora != null && !agora.equals(video)) System.out.println("vídeo " + agora + " no quadro " + quadro);
            video = agora;
            passo((quadro % CADENCIA == 0 || pede && noQte == 12) && (!SO_ANIMACAO || cena.animando()));
        }
        System.out.println("cena acabou no quadro " + quadro);
        for (int i = 0; i < 60 * 3; i++) passo(i % 30 == 0 && !SO_ANIMACAO);
        System.exit(0);
    }

    /** Grava um quadro a cada tantos (-Pcadencia, padrão 6). */
    static final int CADENCIA = Integer.getInteger("gameluta.cadencia", 6);
    /** Grava só a animação manual da cena (-PsoAnimacao=true): o vídeo de conferência dela. */
    static final boolean SO_ANIMACAO = Boolean.getBoolean("gameluta.soAnimacao");
    /** Grava em JPG (-Pjpg=true): um décimo do PNG. */
    static final boolean JPG = Boolean.getBoolean("gameluta.jpg");
    static final boolean ERRA_PRIMEIRO = Boolean.getBoolean("gameluta.erraPrimeiro");

    static void passo(boolean gravar) throws Exception {
        avancar.invoke(jogo, CombatConfig.DT);
        quadro++;
        if (!gravar) return;
        Canvas canvas = new Canvas(tela);
        canvas.drawColor(0xFF000000);
        desenhar.invoke(jogo, canvas);
        if (JPG) {
            java.awt.image.BufferedImage rgb = new java.awt.image.BufferedImage(tela.image().getWidth(),
                tela.image().getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            rgb.getGraphics().drawImage(tela.image(), 0, 0, null);
            ImageIO.write(rgb, "jpg", new File(saida, String.format(Locale.ROOT, "q%05d.jpg", quadro)));
        } else {
            ImageIO.write(tela.image(), "png", new File(saida, String.format(Locale.ROOT, "q%05d.png", quadro)));
        }
    }

    @SuppressWarnings("unchecked")
    static void toque(int acao, float x, float y) throws Exception {
        ((Queue<MotionEvent>)campo("pendingInput").get(jogo)).offer(MotionEvent.obtain(0L, 0L, acao, x, y, 0));
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
