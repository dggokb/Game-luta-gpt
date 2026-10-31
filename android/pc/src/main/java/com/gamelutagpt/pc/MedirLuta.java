package com.gamelutagpt.pc;

import android.view.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;

/**
 * O teste de desempenho automático (o mesmo do botão TESTE do celular) no PC, sem tela: entra
 * numa luta, roda os 30 s roteirizados e escreve o relatório. Serve para medir cada mudança sem
 * depender de captura de tela do aparelho.
 *
 * <pre>gradle :pc:medirLuta [-Presolucao=1920x1080]   →   pc/build/medir-luta/relatorio.txt + PNGs</pre>
 */
public final class MedirLuta {
    private MedirLuta() {}

    public static void main(String[] args) throws Exception {
        File saida = new File(args.length > 0 ? args[0] : "build/medir-luta");
        if (!saida.isDirectory() && !saida.mkdirs()) throw new IOException("não criou " + saida);
        int largura = 1280, altura = 720;
        if (args.length > 1) {
            String[] r = args[1].toLowerCase(Locale.ROOT).split("x");
            largura = Integer.parseInt(r[0]);
            altura = Integer.parseInt(r[1]);
        }
        DadosDoJogo dados = DadosDoJogo.localizar();
        CapturaPc c = new CapturaPc(dados.contexto(null), largura, altura);
        c.comecar();
        c.esperar(1.5);
        // MODO TESTE → seleção de duplas → P1, TAG, P2, TAG e confirmar.
        c.tecla(KeyEvent.KEYCODE_W);
        c.tecla(KeyEvent.KEYCODE_J);
        c.esperar(1.5);
        for (int i = 0; i < 4; i++) {
            c.tecla(KeyEvent.KEYCODE_J);
            c.esperar(0.3);
        }
        c.tecla(KeyEvent.KEYCODE_ENTER);
        // Intro, READY e GO.
        c.esperar(6.0);
        // -Dgameluta.dupla=p15,p16,p13,p14: as duplas da luta (jogador, parceiro, rival, parceiro).
        String dupla = System.getProperty("gameluta.dupla");
        if (dupla != null) {
            String[] d = dupla.split(",");
            c.jogo().escolherDuplas(d[0], d[1], d[2], d[3]);
            c.esperar(1.0);
            long carregando = System.currentTimeMillis() + 180_000;
            while (!c.jogo().iniciarTesteDesempenho()) {
                if (System.currentTimeMillis() > carregando) {
                    // Sem o exit, as threads do jogo seguram o processo aberto.
                    System.err.println("a dupla não carregou");
                    System.exit(2);
                }
                c.esperar(0.5);
            }
            // O pedido acima já valeu: espera a intro passar e pede de novo (o roteiro conta da luta).
            c.esperar(0.1);
        }
        long limite = System.currentTimeMillis() + 20_000;
        while (!c.jogo().iniciarTesteDesempenho()) {
            if (System.currentTimeMillis() > limite) throw new IllegalStateException("a luta não começou");
            c.esperar(0.5);
        }
        // Algumas fotos no caminho (SHINKAI aos 3 s, Super aos 10 s, Ultra aos 17 s).
        double[] fotos = {4.0, 10.5, 17.5, 18.5, 19.5, 20.5, 21.5, 24.0};
        String[] nomes = {"shinkai", "super", "ultra", "ultra-2", "ultra-3", "ultra-4", "ultra-5", "shinkai-rival"};
        double passado = 0;
        for (int i = 0; i < fotos.length; i++) {
            c.esperar(fotos[i] - passado);
            passado = fotos[i];
            CapturaPc.salvar(c.quadro(), saida, String.format(Locale.ROOT, "%02d-%s", i + 1, nomes[i]));
        }
        // O teste conta passos de luta (as pausas do Super e do Ultra esticam o tempo real).
        limite = System.currentTimeMillis() + 120_000;
        while (c.jogo().testeRodando()) {
            if (System.currentTimeMillis() > limite) throw new IllegalStateException("o teste não terminou");
            c.esperar(0.5);
        }
        String[] relatorio = c.jogo().relatorioTeste();
        c.esperar(0.3);
        CapturaPc.salvar(c.quadro(), saida, "99-relatorio");
        c.parar();
        String cabeca = String.format(Locale.ROOT, "PC sem tela %dx%d", largura, altura);
        String[] linhas = new String[relatorio.length + 1];
        linhas[0] = cabeca;
        System.arraycopy(relatorio, 0, linhas, 1, relatorio.length);
        Files.write(new File(saida, "relatorio.txt").toPath(), Arrays.asList(linhas));
        for (String l : linhas) System.out.println(l);
        System.exit(0);
    }
}
