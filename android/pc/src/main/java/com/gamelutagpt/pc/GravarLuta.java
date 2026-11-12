package com.gamelutagpt.pc;

import android.view.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Roda o jogo sem janela, entra numa luta do MODO TESTE e grava quadros seguidos de um roteiro:
 * serve para ver um golpe, um poder ou um efeito no jogo de verdade sem celular e sem tela.
 *
 * <pre>
 * gradle :pc:gravarLuta -Proteiro="espera:1 grava:90:esp especial:J fim" [-Pdupla=p05,p09,player_base,p04]
 *     →  pc/build/gravar-luta/esp-000.png ...
 * </pre>
 *
 * Passos do roteiro, na ordem (um por argumento):
 * <ul>
 *   <li>{@code espera:1.5} — segundos de jogo;</li>
 *   <li>{@code tecla:J} — aperta e solta (J K L = L M H; U combo; I Super; O parceiro; H agarrão;
 *       P SHINKAI; W A S D direções; F3 liga/desliga a CPU);</li>
 *   <li>{@code segura:D} / {@code solta:D} — andar, agachar, defender;</li>
 *   <li>{@code especial:J} — ↓ ↘ → e o botão (o especial de comando, olhando para a direita);</li>
 *   <li>{@code super:3000} — a barra de Super dos dois times (1000 por barra);</li>
 *   <li>{@code grava:90:nome} — começa a gravar 90 quadros seguidos enquanto o roteiro continua;
 *       {@code fim} espera a gravação acabar;</li>
 *   <li>{@code rota:L,M,M,S3,W90,C200,S2} — aperta o combo quadro a quadro, como o executor dos
 *       testes (RoteiroCombo); o rival fica parado;</li>
 *   <li>{@code foto:nome} — um quadro só.</li>
 * </ul>
 * Os botões de teste do canto ficam fora da gravação ({@code -Dgameluta.botoes=true} mostra). A
 * luta começa com o jogador em x=1020 e o rival em x=1580; para um golpe de perto acertar, ande
 * antes ({@code segura:D espera:1.3 solta:D}). Os quadros saem pelo desenho do PC (Java2D); o do
 * celular é OpenGL ({@code -Dgameluta.opengl=true} com uma tela, mesmo virtual).
 */
public final class GravarLuta {
    private GravarLuta() {}

    private static Thread gravando;

    public static void main(String[] a) throws Exception {
        if (a.length < 3) {
            System.err.println("uso: GravarLuta <pasta> <LxA> passo [passo ...]");
            System.exit(2);
        }
        File saida = new File(a[0]);
        if (!saida.isDirectory() && !saida.mkdirs()) throw new IOException("não criou " + saida);
        String[] r = a[1].toLowerCase(Locale.ROOT).split("x");
        DadosDoJogo dados = DadosDoJogo.localizar();
        CapturaPc c = new CapturaPc(dados.contexto(null), Integer.parseInt(r[0]), Integer.parseInt(r[1]));
        c.comecar();
        c.esperar(1.5);
        // MODO TESTE (o último item do menu: cima dá a volta até ele), a seleção e a luta.
        c.tecla(KeyEvent.KEYCODE_W);
        c.tecla(KeyEvent.KEYCODE_J);
        c.esperar(1.5);
        for (int i = 0; i < 4; i++) {
            c.tecla(KeyEvent.KEYCODE_J);
            c.esperar(0.3);
        }
        c.tecla(KeyEvent.KEYCODE_ENTER);
        // A carga das folhas e a entrada dos lutadores: espera a luta valer (-Dgameluta.carga: o limite, em s).
        esperarLuta(c);
        String dupla = System.getProperty("gameluta.dupla");
        if (dupla != null) {
            String[] d = dupla.split(",");
            c.jogo().escolherDuplas(d[0], d[1], d[2], d[3]);
            c.esperar(1.0);
            esperarLuta(c);
        }
        c.jogo().mostrarBotoesDeTeste(Boolean.getBoolean("gameluta.botoes"));
        List<String> nomes = new ArrayList<>();
        List<BufferedImage> quadros = new ArrayList<>();
        for (int i = 2; i < a.length; i++) {
            String[] p = a[i].split(":");
            switch (p[0]) {
                case "espera":
                    c.esperar(Double.parseDouble(p[1]));
                    break;
                case "tecla":
                    apertar(c, tecla(p[1]));
                    break;
                case "segura":
                    c.segurar(tecla(p[1]));
                    break;
                case "solta":
                    c.soltar(tecla(p[1]));
                    break;
                case "especial": {
                    // ↓ ↘ → + botão, cada direção por alguns quadros (o comando lê por onde o direcional passou).
                    int botao = tecla(p[1]);
                    c.segurar(KeyEvent.KEYCODE_S);
                    Thread.sleep(70);
                    c.segurar(KeyEvent.KEYCODE_D);
                    Thread.sleep(70);
                    c.soltar(KeyEvent.KEYCODE_S);
                    Thread.sleep(70);
                    c.segurar(botao);
                    Thread.sleep(70);
                    c.soltar(botao);
                    c.soltar(KeyEvent.KEYCODE_D);
                    break;
                }
                case "rota":
                    // O combo do teste, quadro a quadro (RoteiroCombo): rota:L,M,M,S3,W90,C200,S2
                    c.jogo().apertarRoteiro(p[1]);
                    break;
                case "super":
                    c.jogo().encherSuper(Integer.parseInt(p[1]));
                    c.esperar(0.1);
                    break;
                case "grava": {
                    int n = Integer.parseInt(p[1]);
                    String nome = p[2];
                    esperarGravacao();
                    gravando = new Thread(() -> {
                        try {
                            for (int k = 0; k < n; k++) {
                                BufferedImage q = c.quadro();
                                synchronized (quadros) {
                                    quadros.add(q);
                                    nomes.add(String.format(Locale.ROOT, "%s-%03d", nome, k));
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }, "Gravacao");
                    gravando.start();
                    break;
                }
                case "fim":
                    esperarGravacao();
                    break;
                case "foto": {
                    esperarGravacao();
                    BufferedImage q = c.quadro();
                    synchronized (quadros) {
                        quadros.add(q);
                        nomes.add(p[1]);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("passo desconhecido: " + a[i]);
            }
        }
        esperarGravacao();
        c.parar();
        for (int i = 0; i < quadros.size(); i++) ImageIO.write(quadros.get(i), "png", new File(saida, nomes.get(i) + ".png"));
        System.out.println("gravou " + quadros.size() + " quadros em " + saida);
        System.exit(0);
    }

    /** Espera a luta carregar e a entrada acabar, e mais um instante para os dois pararem em guarda. */
    private static void esperarLuta(CapturaPc c) throws InterruptedException {
        long limite = System.currentTimeMillis() + Math.round(1000 * Double.parseDouble(System.getProperty("gameluta.carga", "600")));
        c.esperar(1.0);
        while (!c.jogo().lutaPronta()) {
            if (System.currentTimeMillis() > limite) throw new IllegalStateException("a luta não começou");
            c.esperar(0.25);
        }
        c.esperar(0.5);
    }

    private static void esperarGravacao() throws InterruptedException {
        if (gravando != null) gravando.join();
        gravando = null;
    }

    /** Aperta por 100 ms: um toque mais curto às vezes cai entre dois quadros do jogo. */
    private static void apertar(CapturaPc c, int codigo) throws InterruptedException {
        c.segurar(codigo);
        Thread.sleep(100);
        c.soltar(codigo);
        Thread.sleep(50);
    }

    private static int tecla(String nome) throws ReflectiveOperationException {
        return KeyEvent.class.getField("KEYCODE_" + nome.toUpperCase(Locale.ROOT)).getInt(null);
    }
}
