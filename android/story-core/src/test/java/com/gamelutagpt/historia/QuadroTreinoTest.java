package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.junit.Test;

/** Quadros entre as lições do tutorial: o arquivo do jogo vale e o quadro anda e sai sozinho. */
public class QuadroTreinoTest {
    private static final File ASSETS = new File("../app/src/main/assets");
    private static final float DT = 1f / 60f;

    private static final RenderAssets FILES = new RenderAssets() {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
    };

    @Test
    public void quadrosDoJogoValem() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        Map<String, Episodio.Painel> quadros = QuadroTreino.fromJson(FILES.readText(QuadroTreino.ARQUIVO));
        assertTrue(quadros.containsKey("andar"));
        assertTrue(quadros.containsKey("fim"));
        for (Map.Entry<String, Episodio.Painel> e : quadros.entrySet()) {
            assertFalse(e.getKey() + " sem balão", e.getValue().baloes.isEmpty());
            for (Episodio.Balao b : e.getValue().baloes) {
                assertTrue(e.getKey() + ": fala de alguém fora do elenco: " + b.quem, temporada.elenco.containsKey(b.quem));
            }
            for (String who : e.getValue().personagens) {
                assertTrue(e.getKey() + ": fora do elenco: " + who, temporada.elenco.containsKey(who));
            }
        }
    }

    @Test
    public void toqueTerminaODigitarPassaOBalaoEFecha() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        QuadroTreino q = new QuadroTreino(FILES);
        q.load();
        assertFalse("quadro que não existe não aparece", q.start(temporada, "nao-existe"));
        assertFalse(q.active());

        assertTrue(q.start(temporada, "combo"));
        PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
        for (int i = 0; i < 40; i++) {
            q.update(DT);
            q.render(canvas);
        }
        q.tap(); // termina de digitar o 1º balão
        q.tap(); // 2º balão
        q.tap(); // termina de digitar
        q.tap(); // fecha
        for (int i = 0; i < 30; i++) q.update(DT);
        assertFalse(q.active());

        // Sem tocar, os balões passam sozinhos e o quadro sai.
        assertTrue(q.start(temporada, "fim"));
        int frames = 0;
        while (q.active() && frames++ < 60 * 30) {
            q.update(DT);
            q.render(canvas);
        }
        assertFalse("o quadro não pode prender o treino", q.active());
        assertTrue(frames > 60 * 3);
    }
}
