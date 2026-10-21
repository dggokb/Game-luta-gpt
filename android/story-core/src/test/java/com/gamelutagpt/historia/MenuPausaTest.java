package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import org.junit.Test;

/** A pausa (o que sobrou do story-core na versão leve). */
public class MenuPausaTest {
    private static final float DT = 1f / 60f;

    @Test
    public void pausaContinuaOuVoltaAoMenu() {
        MenuPausa p = new MenuPausa();
        p.abrir(MenuPausa.AVISO);
        assertEquals(MenuPausa.Opcao.CONTINUAR, p.cursor());
        float[] menu = MenuPausa.CAIXAS[7];
        assertNull("o toque que abriu a pausa não escolhe", p.tap((menu[0] + menu[2]) * 0.5f, (menu[1] + menu[3]) * 0.5f));
        for (int i = 0; i < 20; i++) p.update(DT);
        assertNull(p.tap(640f, 675f));
        assertTrue(p.isOpen());
        p.render(new NullCanvas());
        assertEquals(MenuPausa.Opcao.MENU, p.tap((menu[0] + menu[2]) * 0.5f, (menu[1] + menu[3]) * 0.5f));
        assertFalse(p.isOpen());

        p.abrir(null);
        assertEquals(MenuPausa.Opcao.CONTINUAR, p.voltar());
        assertFalse(p.isOpen());

        p.abrir(null);
        for (int i = 0; i < 20; i++) p.update(DT);
        for (int i = 0; i < 7; i++) p.mover(1);
        assertEquals(MenuPausa.Opcao.MENU, p.confirmar());
        assertNull(p.confirmar());
    }

    @Test
    public void pausaLigaEDesligaOContadorDeFps() {
        MenuPausa p = new MenuPausa();
        p.abrir(null);
        for (int i = 0; i < 20; i++) p.update(DT);
        assertTrue("ligado de começo", p.fps());
        for (int i = 0; i < 6; i++) p.mover(1);
        assertEquals(MenuPausa.Opcao.FPS, p.cursor());
        assertNull("troca e a pausa continua", p.confirmar());
        assertFalse(p.fps());
        assertTrue(p.ajustar(1));
        assertTrue(p.fps());
        float[] caixa = MenuPausa.CAIXAS[6];
        assertNull(p.tap((caixa[0] + caixa[2]) * 0.5f, (caixa[1] + caixa[3]) * 0.5f));
        assertFalse(p.fps());
        assertTrue(p.isOpen());
        p.render(new NullCanvas());
    }

    @Test
    public void pausaTrocaOsGraficosSemFechar() {
        MenuPausa p = new MenuPausa();
        final int[] nivel = {0};
        final String[] nomes = {"AUTO", "ALTO", "MÉDIO", "LEVE"};
        p.setGraficos(new MenuPausa.Graficos() {
            @Override public String rotulo(java.util.function.UnaryOperator<String> traduzir) { return traduzir.apply(nomes[nivel[0]]); }
            @Override public void mudar(int direcao) { nivel[0] = ((nivel[0] + (direcao > 0 ? 1 : -1)) % 4 + 4) % 4; }
        });
        p.abrir(null);
        for (int i = 0; i < 20; i++) p.update(DT);
        for (int i = 0; i < 5; i++) p.mover(1);
        assertEquals(MenuPausa.Opcao.GRAFICOS, p.cursor());
        assertNull("A troca o nível e a pausa continua", p.confirmar());
        assertEquals(1, nivel[0]);
        assertTrue(p.ajustar(-1));
        assertEquals(0, nivel[0]);
        float[] caixa = MenuPausa.CAIXAS[5];
        assertNull(p.tap(caixa[2] - 10f, (caixa[1] + caixa[3]) * 0.5f));
        assertEquals(1, nivel[0]);
        assertTrue(p.isOpen());
        p.render(new NullCanvas());
    }

    private static TelaComandos.Ficha ficha(String nome) {
        return new TelaComandos.Ficha(nome, java.util.Arrays.asList(
            new TelaComandos.Linha("Onda", "↓ ↘ → + L/M/H", false),
            new TelaComandos.Linha("Mergulho", "↓ ↘ → + L/M/H", true)));
    }

    @Test
    public void pausaMostraOsComandosDeCadaPersonagem() {
        MenuPausa p = new MenuPausa();
        p.comandos().setFichas(java.util.Arrays.asList(ficha("p01"), ficha("p02"), ficha("p03")), 1);
        p.abrir(null);
        for (int i = 0; i < 20; i++) p.update(DT);
        p.mover(1);
        p.mover(1);
        p.mover(1);
        p.mover(1);
        assertEquals(MenuPausa.Opcao.COMANDOS, p.cursor());
        assertNull("COMANDOS não fecha a pausa", p.confirmar());
        assertTrue(p.isOpen());
        assertTrue(p.comandos().isOpen());
        assertEquals("começa no personagem do jogador", "p02", p.comandos().ficha().personagem);
        p.mover(1);
        assertEquals("▲▼ não andam na lista", MenuPausa.Opcao.COMANDOS, p.cursor());
        assertTrue(p.ajustar(1));
        assertEquals("p03", p.comandos().ficha().personagem);
        p.ajustar(1);
        assertEquals("dá a volta", "p01", p.comandos().ficha().personagem);
        p.ajustar(-1);
        assertEquals("p03", p.comandos().ficha().personagem);
        p.render(new NullCanvas());
        assertNull("voltar sai da lista e fica na pausa", p.voltar());
        assertTrue(p.isOpen());
        assertFalse(p.comandos().isOpen());

        // Pelo toque: a faixa COMANDOS abre, as setas trocam e VOLTAR fecha.
        float[] caixa = MenuPausa.CAIXAS[4];
        assertNull(p.tap((caixa[0] + caixa[2]) * 0.5f, (caixa[1] + caixa[3]) * 0.5f));
        assertTrue(p.comandos().isOpen());
        for (int i = 0; i < 20; i++) p.update(DT);
        float[] anterior = TelaComandos.ANTERIOR;
        assertNull(p.tap((anterior[0] + anterior[2]) * 0.5f, (anterior[1] + anterior[3]) * 0.5f));
        assertEquals("p02", p.comandos().ficha().personagem);
        float[] voltar = TelaComandos.BOTAO_VOLTAR;
        assertNull(p.tap((voltar[0] + voltar[2]) * 0.5f, (voltar[1] + voltar[3]) * 0.5f));
        assertFalse(p.comandos().isOpen());
        assertTrue("o toque do VOLTAR não escolhe nada na pausa", p.isOpen());
        assertNull(p.tap((caixa[0] + caixa[2]) * 0.5f, (caixa[1] + caixa[3]) * 0.5f));
        assertFalse(p.comandos().isOpen());

        p.fechar();
        p.abrir(null);
        assertFalse("reabrir a pausa não abre a lista", p.comandos().isOpen());
    }

    @Test
    public void pausaMudaAVelocidadeSemFechar() {
        MenuPausa p = new MenuPausa();
        assertEquals(MenuPausa.VELOCIDADE_PADRAO, p.velocidade(), 1e-6f);
        p.abrir(null);
        for (int i = 0; i < 20; i++) p.update(DT);
        assertFalse("◀ ▶ fora da VELOCIDADE não mudam nada", p.ajustar(1));
        p.mover(1);
        assertEquals(MenuPausa.Opcao.VELOCIDADE, p.cursor());
        assertEquals("o padrão é a velocidade normal", 1f, p.velocidade(), 1e-6f);
        assertTrue(p.ajustar(-1));
        assertEquals(0.9f, p.velocidade(), 1e-6f);
        p.ajustar(1);
        p.ajustar(1);
        assertEquals("para na ponta", 1f, p.velocidade(), 1e-6f);
        assertNull("A na VELOCIDADE não fecha a pausa", p.confirmar());
        assertTrue(p.isOpen());
        assertEquals("e dá a volta", 0.5f, p.velocidade(), 1e-6f);
        p.render(new NullCanvas());

        float[] caixa = MenuPausa.CAIXAS[1];
        float cy = (caixa[1] + caixa[3]) * 0.5f;
        assertNull(p.tap(caixa[2] - 20f, cy));
        assertEquals("metade direita: mais rápido", 0.6f, p.velocidade(), 1e-6f);
        assertNull(p.tap(caixa[0] + 20f, cy));
        assertEquals("metade esquerda: mais devagar", 0.5f, p.velocidade(), 1e-6f);
        assertTrue(p.isOpen());

        p.setVelocidade(0.77f);
        assertEquals("salva fora da lista vira a mais próxima", 0.8f, p.velocidade(), 1e-6f);
        p.abrir(null);
        assertEquals("reabrir não volta ao padrão", 0.8f, p.velocidade(), 1e-6f);
    }

    @Test
    public void pausaLigaEDesligaOImpactoSemFechar() {
        MenuPausa p = new MenuPausa();
        assertTrue("vem ligado", p.impacto());
        p.abrir(null);
        for (int i = 0; i < 20; i++) p.update(DT);
        p.mover(1);
        p.mover(1);
        assertEquals(MenuPausa.Opcao.IMPACTO, p.cursor());
        assertNull("A no IMPACTO não fecha a pausa", p.confirmar());
        assertFalse(p.impacto());
        assertTrue(p.ajustar(1));
        assertTrue("◀ ▶ também trocam", p.impacto());
        float[] caixa = MenuPausa.CAIXAS[2];
        assertNull(p.tap((caixa[0] + caixa[2]) * 0.5f, (caixa[1] + caixa[3]) * 0.5f));
        assertFalse("pelo toque", p.impacto());
        assertTrue(p.isOpen());
        p.render(new NullCanvas());
        p.setImpacto(true);
        assertTrue(p.impacto());
    }

    @Test
    public void pausaMudaONivelDaCpuSemFechar() {
        MenuPausa p = new MenuPausa();
        assertEquals("o padrão é o 5", 5, p.nivelCpu());
        p.abrir(null);
        for (int i = 0; i < 20; i++) p.update(DT);
        p.mover(1);
        p.mover(1);
        p.mover(1);
        assertEquals(MenuPausa.Opcao.NIVEL_CPU, p.cursor());
        assertTrue(p.ajustar(1));
        assertEquals(6, p.nivelCpu());
        p.ajustar(1);
        p.ajustar(1);
        p.ajustar(1);
        assertEquals("para no 8", 8, p.nivelCpu());
        assertNull("A no NÍVEL não fecha a pausa", p.confirmar());
        assertEquals("e dá a volta", 1, p.nivelCpu());
        p.ajustar(-1);
        assertEquals("para no 1", 1, p.nivelCpu());
        float[] caixa = MenuPausa.CAIXAS[3];
        float cy = (caixa[1] + caixa[3]) * 0.5f;
        assertNull(p.tap(caixa[2] - 20f, cy));
        assertEquals("metade direita: sobe", 2, p.nivelCpu());
        assertNull(p.tap(caixa[0] + 20f, cy));
        assertEquals("metade esquerda: desce", 1, p.nivelCpu());
        assertTrue(p.isOpen());
        p.render(new NullCanvas());
        p.setNivelCpu(12);
        assertEquals("salvo fora da faixa vira o mais próximo", 8, p.nivelCpu());
    }

    static final class NullCanvas implements RenderCanvas {
        @Override public void save() {}
        @Override public void restore() {}
        @Override public void translate(float dx, float dy) {}
        @Override public void scale(float sx, float sy, float px, float py) {}
        @Override public void rotate(float degrees, float px, float py) {}
        @Override public void clipPolygon(float[] xy) {}
        @Override public void fillRect(float l, float t, float r, float b, int argb) {}
        @Override public void fillPolygon(float[] xy, int argb) {}
        @Override public void strokePolygon(float[] xy, float width, int argb) {}
        @Override public void fillCircle(float cx, float cy, float radius, int argb) {}
        @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) {}
        @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {}
        @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {}
        @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                              float l, float t, float r, float b, int alpha) {}
        @Override public void fillRadialGlow(float cx, float cy, float radius, int argbCenter) {}
        @Override public void fillVerticalGradient(float l, float t, float r, float b, int top, int bottom) {}
        @Override public void fillOval(float l, float t, float r, float b, int argb) {}
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke,
                                       float strokeWidth, boolean italic) {}
        @Override public void beginInvert() {}
        @Override public void endInvert() {}
    }
}
