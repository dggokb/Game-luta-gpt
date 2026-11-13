package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.gamelutagpt.historia.Campanha;
import com.gamelutagpt.historia.CinematicaLuta;
import com.gamelutagpt.historia.PaginaHistoria;
import com.gamelutagpt.pc.DadosDoJogo;
import com.gamelutagpt.render.RenderAssets;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Queue;
import javax.imageio.ImageIO;

/**
 * O teste dos QTE na luta do convite do Episódio 2 (Ryūga × Yin-lu, ou Kōga × Brucy na campanha
 * do Kōga), no jogo de verdade do PC: abre o episódio na campanha, passa as páginas, deixa o
 * rival com 20% da vida e joga a cena (o primeiro QTE erra de propósito, para ver o contra-ataque
 * e o pedido de novo). Os QTE vão pelo caminho do jogo: toques na fila da tela e teclas do
 * teclado. Grava quadros em PNG.
 *
 * <pre>java ... com.gamelutagpt.TesteQteConvite saida/ [ryuga|koga]</pre>
 */
public final class TesteQteConvite {
    private TesteQteConvite() {}

    static GameView jogo;
    static Method avancar, desenhar;
    static Bitmap tela;
    static Canvas canvas;
    static File saida;
    static int quadro;

    public static void main(String[] args) throws Exception {
        saida = new File(args.length > 0 ? args[0] : "build/qte");
        String irmao = args.length > 1 ? args[1] : "ryuga";
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
        canvas = new Canvas(tela);

        // Episódio 2 na campanha do irmão.
        metodo("loadTemporada").invoke(jogo);
        Campanha campanha = Campanha.load((RenderAssets)campo("ultraAssets").get(jogo), 7L);
        campanha.restaurar(irmao, -1);
        PaginaHistoria historia = (PaginaHistoria)campo("historia").get(jogo);
        campo("storyMode").set(jogo, true);
        historia.setBotaoMenu(true);
        metodo("startStoryAt", int.class).invoke(jogo, 2);
        historia.setCampanha(campanha);
        metodo("playStoryEpisode", int.class).invoke(jogo, 2);

        // As páginas antes da luta: um toque a cada meio segundo.
        int limite = 60 * 120;
        while (!historia.waitingFight() && limite-- > 0) {
            passo(false);
            if (quadro % 30 == 0 && historia.coversScreen()) historia.tap(900f, 400f);
        }
        System.out.println("luta: " + historia.passo().rotulo + " " + jogo.engine.fighter(GameView.PLAYER).state.character.id + " × " + jogo.engine.fighter(GameView.OPPONENT).state.character.id);
        CinematicaLuta cena = (CinematicaLuta)campo("cinematica").get(jogo);
        if (!cena.armada()) throw new IllegalStateException("a cinemática não armou");

        // A luta: depois da intro, o rival leva um golpe que o deixa com 19%.
        limite = 60 * 60;
        boolean bateu = false;
        while (!cena.rodando() && limite-- > 0) {
            passo(quadro % 20 == 0);
            if (!bateu && (int)campo("introFrame").get(jogo) < 0 && !((BattleFlow)campo("flow").get(jogo)).holdsFighters() && quadro % 120 == 0) {
                FighterState rival = jogo.engine.fighter(GameView.OPPONENT).state;
                FighterState jogador = jogo.engine.fighter(GameView.PLAYER).state;
                // Esperando a cena, só o rival (o lado do gatilho) tem o último ponto seguro.
                System.out.println("antes da cena, último ponto seguro: rival " + (rival.lifeFloor > 0) +
                    ", jogador " + (jogador.lifeFloor > 0));
                rival.takeDamage(rival.life - Math.round(rival.profile.maxLife * 0.19f), 0);
                bateu = true;
                System.out.println("rival com 19%");
            }
        }
        System.out.println("cena começou no quadro " + quadro);

        // A cena: tinta, página (pula com um toque), tinta, QTE.
        int qtes = 0, noQte = 0, toques = 0;
        boolean pedia = false;
        limite = 60 * 150;
        while (!cena.acabou() && limite-- > 0) {
            boolean pede = cena.qtePedindo();
            if (pede && !pedia) {
                if (cena.qteErros() == 0) qtes++;
                noQte = 0;
                toques = 0;
                System.out.printf(Locale.ROOT, "QTE %d (erros %d) no quadro %d, vida do rival %.0f%%, do jogador %.0f%%%n", qtes,
                    cena.qteErros(), quadro, vida(jogo.engine.fighter(GameView.OPPONENT)), vida(jogo.engine.fighter(GameView.PLAYER)));
            }
            pedia = pede;
            if (pede) {
                noQte++;
                CinematicaLuta.TipoQte tipo = cena.qteTipo();
                if (qtes == 1 && cena.qteErros() == 0) {
                    // Deixa o tempo acabar: o rival contra-ataca e o QTE pede de novo.
                } else if (tipo == CinematicaLuta.TipoQte.TOQUE && noQte == 20) {
                    toque(MotionEvent.ACTION_DOWN, 300f, 300f);
                    toque(MotionEvent.ACTION_UP, 300f, 300f);
                } else if (tipo == CinematicaLuta.TipoQte.MARTELAR && noQte % 5 == 0) {
                    // Martelar no teclado (J).
                    tecla(KeyEvent.KEYCODE_J);
                    toques++;
                } else if (tipo == CinematicaLuta.TipoQte.DESLIZAR && noQte == 25) {
                    float dx = cena.qteDirecao()[0], dy = cena.qteDirecao()[1];
                    toque(MotionEvent.ACTION_DOWN, 640f, 420f);
                    toque(MotionEvent.ACTION_MOVE, 640f + dx * 60f, 422f + dy * 60f);
                    toque(MotionEvent.ACTION_MOVE, 640f + dx * 140f, 425f + dy * 140f);
                    toque(MotionEvent.ACTION_UP, 640f + dx * 140f, 425f + dy * 140f);
                } else if (tipo == CinematicaLuta.TipoQte.TEMPO && noQte == Math.round(cena.qteSegundos() * 60f)) {
                    toque(MotionEvent.ACTION_DOWN, 640f, 420f);
                    toque(MotionEvent.ACTION_UP, 640f, 420f);
                }
            } else if (cena.paginaNaTela() && cena.pegaToque() && quadro % 40 == 0) {
                // A página: um toque por vez, quadro a quadro.
                toque(MotionEvent.ACTION_DOWN, 640f, 360f);
                toque(MotionEvent.ACTION_UP, 640f, 360f);
            }
            passo(quadro % CADENCIA == 0 || pede && noQte == 12);
        }
        System.out.printf(Locale.ROOT, "cena acabou no quadro %d: rival %.0f%% (KO %b), jogador %.0f%%%n", quadro,
            vida(jogo.engine.fighter(GameView.OPPONENT)), jogo.engine.fighter(GameView.OPPONENT).ko(), vida(jogo.engine.fighter(GameView.PLAYER)));
        // O resultado da luta.
        for (int i = 0; i < 60 * 6; i++) passo(i % 30 == 0);
        System.out.println("história esperando a luta: " + historia.waitingFight());
        System.exit(0);
    }

    /** De quantos em quantos quadros a cena é gravada (-Dgameluta.cadencia; 2 = vídeo a 30 qps). */
    static final int CADENCIA = Integer.getInteger("gameluta.cadencia", 6);

    static float vida(CombatFighter f) {
        return 100f * f.state.life / f.state.profile.maxLife;
    }

    static void passo(boolean gravar) throws Exception {
        avancar.invoke(jogo, CombatConfig.DT);
        quadro++;
        if (!gravar) return;
        // Um Canvas novo a cada quadro: nada do quadro anterior fica no estado dele.
        canvas = new Canvas(tela);
        canvas.drawColor(0xFF000000);
        desenhar.invoke(jogo, canvas);
        ImageIO.write(tela.image(), "png", new File(saida, String.format(Locale.ROOT, "q%05d.png", quadro)));
    }

    @SuppressWarnings("unchecked")
    static void toque(int acao, float x, float y) throws Exception {
        ((Queue<MotionEvent>)campo("pendingInput").get(jogo)).offer(MotionEvent.obtain(0L, 0L, acao, x, y, 0));
    }

    static void tecla(int codigo) {
        jogo.onKeyDown(codigo, new KeyEvent(KeyEvent.ACTION_DOWN, codigo));
        jogo.onKeyUp(codigo, new KeyEvent(KeyEvent.ACTION_UP, codigo));
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
