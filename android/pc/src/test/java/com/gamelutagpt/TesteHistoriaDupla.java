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
import com.gamelutagpt.historia.Torneio;
import com.gamelutagpt.pc.DadosDoJogo;
import com.gamelutagpt.render.RenderAssets;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Queue;
import javax.imageio.ImageIO;

/**
 * A história de uma dupla adversária no torneio da história, no jogo de verdade do PC: sorteia
 * uma chave em que a dupla pedida é a das quartas, entra na luta, derruba o titular dela (a cena
 * não pode começar: o parceiro está de pé), leva o último de pé a 20% e joga a cena, tocando as
 * páginas e acertando os QTE. Grava um PNG a cada 15 quadros.
 *
 * <pre>gradle :pc:testeHistoriaDupla -Pdupla=p09 [-Pirmao=koga]</pre>
 */
public final class TesteHistoriaDupla {
    private TesteHistoriaDupla() {}

    static GameView jogo;
    static Method avancar, desenhar;
    static Bitmap tela;
    static File saida;
    static int quadro;

    public static void main(String[] args) throws Exception {
        saida = new File(args[0]);
        String irmao = args[1], dupla = args[2];
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
        Campanha campanha = null;
        for (long semente = 1; semente < 500 && campanha == null; semente++) {
            Campanha c = Campanha.load(assets, semente);
            c.restaurar(irmao, -1);
            Torneio t = c.torneio();
            if (t != null && t.adversario() != null && dupla.equals(t.adversario().id)) campanha = c;
        }
        if (campanha == null) throw new IllegalStateException("nenhuma chave com " + dupla + " nas quartas");
        PaginaHistoria historia = (PaginaHistoria)campo("historia").get(jogo);
        campo("storyMode").set(jogo, true);
        historia.setCampanha(campanha);
        Temporada temporada = (Temporada)campo("temporada").get(jogo);
        Episodio so = Episodio.fromJson("{\"id\": \"ep04\", \"passos\": [{\"tipo\": \"luta\", \"torneio\": true, " +
            "\"cenario\": \"arena_costeira\"}, {\"tipo\": \"proximo\", \"titulo\": \"fim\"}]}");
        historia.start(temporada, so);
        for (int i = 0; i < 600 && !historia.waitingFight(); i++) passo(false);
        if (!historia.waitingFight()) throw new IllegalStateException("a história não chegou na luta");
        System.out.println("luta: " + jogo.engine.fighter(GameView.PLAYER).state.character.id + " × " +
            jogo.engine.fighter(GameView.OPPONENT).state.character.id);
        CinematicaLuta cena = (CinematicaLuta)campo("cinematica").get(jogo);
        if (!cena.armada()) throw new IllegalStateException("a cinemática da dupla não armou");

        // Depois da intro: o titular do rival cai de vez (a cena espera o parceiro).
        int limite = 60 * 60;
        while (((int)campo("introFrame").get(jogo) >= 0 || ((BattleFlow)campo("flow").get(jogo)).holdsFighters()) && limite-- > 0) {
            passo(quadro % 30 == 0);
        }
        FighterState titular = jogo.engine.fighter(GameView.OPPONENT).state;
        titular.takeDamage(titular.life, 0);
        System.out.println("titular " + titular.character.id + " caiu (vida " + titular.life + ")");
        limite = 60 * 15;
        while (jogo.engine.fighter(GameView.OPPONENT).state == titular && limite-- > 0) {
            passo(quadro % 15 == 0);
            if (cena.rodando()) throw new IllegalStateException("a cena começou com o parceiro ainda de pé");
        }
        FighterState ultimo = jogo.engine.fighter(GameView.OPPONENT).state;
        System.out.println("último de pé: " + ultimo.character.id + " no quadro " + quadro);
        for (int i = 0; i < 90; i++) passo(i % 15 == 0);
        ultimo.takeDamage(ultimo.life - Math.round(ultimo.profile.maxLife * 0.2f), 0);
        limite = 60 * 20;
        while (!cena.rodando() && limite-- > 0) passo(quadro % 15 == 0);
        if (!cena.rodando()) throw new IllegalStateException("a cena não começou");
        System.out.println("cena começou no quadro " + quadro);

        int noQte = 0, toques = 0;
        boolean pedia = false;
        limite = 60 * 240;
        while (!cena.acabou() && limite-- > 0) {
            boolean pede = cena.qtePedindo();
            if (pede && !pedia) {
                noQte = 0;
                System.out.println("QTE " + cena.qteTipo() + " no quadro " + quadro);
            }
            pedia = pede;
            if (pede) {
                noQte++;
                CinematicaLuta.TipoQte tipo = cena.qteTipo();
                boolean toca = tipo == CinematicaLuta.TipoQte.MARTELAR ? noQte % 5 == 0
                    : tipo == CinematicaLuta.TipoQte.TEMPO ? noQte == Math.round(cena.qteSegundos() * 60f) : noQte == 20;
                if (toca && tipo == CinematicaLuta.TipoQte.DESLIZAR) {
                    toque(MotionEvent.ACTION_DOWN, 500f, 420f);
                    toque(MotionEvent.ACTION_MOVE, 640f, 420f);
                    toque(MotionEvent.ACTION_UP, 780f, 420f);
                } else if (toca) {
                    toque(MotionEvent.ACTION_DOWN, 640f, 420f);
                    toque(MotionEvent.ACTION_UP, 640f, 420f);
                }
            } else if (cena.pegaToque() && quadro % 50 == 0) {
                // As páginas: um toque a cada quadro do mangá.
                toque(MotionEvent.ACTION_DOWN, 640f, 360f);
                toque(MotionEvent.ACTION_UP, 640f, 360f);
                toques++;
            }
            passo(quadro % 15 == 0);
        }
        System.out.println("cena acabou no quadro " + quadro + " (" + toques + " toques nas páginas)" +
            (cena.acabou() ? "" : " — NÃO ACABOU"));
        boolean acabou = cena.acabou();
        for (int i = 0; i < 60 * 4; i++) passo(i % 30 == 0);
        System.out.println("vida do rival depois: " + jogo.engine.fighter(GameView.OPPONENT).state.life);
        System.exit(acabou ? 0 : 1);
    }

    static void passo(boolean gravar) throws Exception {
        avancar.invoke(jogo, CombatConfig.DT);
        quadro++;
        if (!gravar) return;
        Canvas canvas = new Canvas(tela);
        canvas.drawColor(0xFF000000);
        desenhar.invoke(jogo, canvas);
        ImageIO.write(tela.image(), "png", new File(saida, String.format(Locale.ROOT, "q%05d.png", quadro)));
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
