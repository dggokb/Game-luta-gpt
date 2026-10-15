package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class MenuInicialTest {
    private static final float DT = 1f / 60f;

    /** Lê os textos de verdade do jogo; imagens não abrem (o menu escreve o nome do jogo). */
    private static final class GameAssets implements RenderAssets {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File("../app/src/main/assets", path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException("sem " + path); }
    }

    private static MenuInicial.Opcao run(MenuInicial m, float seconds) {
        MenuInicial.Opcao escolha = null;
        for (float t = 0f; t < seconds; t += DT) {
            MenuInicial.Opcao o = m.update(DT);
            if (o != null) {
                assertNull("a escolha sai uma vez só", escolha);
                escolha = o;
            }
        }
        return escolha;
    }

    private static float[] centro(int opcao) {
        float[] c = MenuInicial.CAIXAS[opcao];
        return new float[] {(c[0] + c[2]) * 0.5f, (c[1] + c[3]) * 0.5f};
    }

    @Test
    public void toqueNaOpcaoEscolheDepoisDoEscurecer() {
        MenuInicial m = new MenuInicial(new GameAssets());
        m.abrir(null);
        assertTrue(m.isOpen());
        float[] teste = centro(1);
        m.tap(teste[0], teste[1]); // cedo demais: o toque que abriu o menu não vale
        assertNull(run(m, MenuInicial.TAP_DELAY + 0.05f));
        m.tap(640f, 60f); // no logo: nada
        assertNull(run(m, 0.1f));
        m.tap(teste[0], teste[1]);
        assertEquals(MenuInicial.Opcao.TESTE, m.cursor());
        assertTrue(m.isOpen());
        assertEquals(MenuInicial.Opcao.TESTE, run(m, MenuInicial.FLASH + MenuInicial.FADE_OUT + 0.1f));
        assertFalse(m.isOpen());
    }

    @Test
    public void controleMoveOCursorEConfirma() {
        MenuInicial m = new MenuInicial(new GameAssets());
        m.abrir(null);
        assertEquals(MenuInicial.Opcao.HISTORIA, m.cursor());
        m.mover(1);
        assertEquals(MenuInicial.Opcao.TESTE, m.cursor());
        m.mover(1); // dá a volta
        assertEquals(MenuInicial.Opcao.HISTORIA, m.cursor());
        m.mover(-1);
        assertEquals(MenuInicial.Opcao.TESTE, m.cursor());
        m.mover(-1);
        run(m, MenuInicial.TAP_DELAY + 0.05f);
        m.confirmar();
        m.mover(1); // depois de escolher, o cursor não anda
        assertEquals(MenuInicial.Opcao.HISTORIA, m.cursor());
        assertEquals(MenuInicial.Opcao.HISTORIA, run(m, 1f));
    }

    @Test
    public void desenhaComESemTemporadaEmOutroIdioma() throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        assertTrue(temporada.setIdioma(assets, "en"));
        assertEquals("STORY MODE", temporada.t(MenuInicial.HISTORIA));
        MenuInicial m = new MenuInicial(assets);
        m.setVersao("v1");
        for (Temporada t : new Temporada[] {temporada, null}) {
            m.abrir(t);
            for (int i = 0; i < 120; i++) {
                m.update(DT);
                m.render(new PaginaHistoriaTest.NullCanvas());
            }
            m.confirmar();
            for (int i = 0; i < 60 && m.isOpen(); i++) {
                m.update(DT);
                m.render(new PaginaHistoriaTest.NullCanvas());
            }
            assertFalse(m.isOpen());
        }
    }

    @Test
    public void pausaContinuaOuVoltaAoMenu() {
        MenuPausa p = new MenuPausa();
        p.abrir(null, true);
        assertEquals(MenuPausa.Opcao.CONTINUAR, p.cursor());
        float[] menu = MenuPausa.CAIXAS[2];
        assertNull("o toque que abriu a pausa não escolhe", p.tap((menu[0] + menu[2]) * 0.5f, (menu[1] + menu[3]) * 0.5f));
        for (int i = 0; i < 20; i++) p.update(DT);
        assertNull(p.tap(640f, 650f));
        assertTrue(p.isOpen());
        p.render(new PaginaHistoriaTest.NullCanvas());
        assertEquals(MenuPausa.Opcao.MENU, p.tap((menu[0] + menu[2]) * 0.5f, (menu[1] + menu[3]) * 0.5f));
        assertFalse(p.isOpen());

        p.abrir(null, false);
        assertEquals(MenuPausa.Opcao.CONTINUAR, p.voltar());
        assertFalse(p.isOpen());

        p.abrir(null, false);
        for (int i = 0; i < 20; i++) p.update(DT);
        p.mover(1);
        p.mover(1);
        assertEquals(MenuPausa.Opcao.MENU, p.confirmar());
        assertNull(p.confirmar());
    }

    @Test
    public void pausaMudaAVelocidadeSemFechar() {
        MenuPausa p = new MenuPausa();
        assertEquals(MenuPausa.VELOCIDADE_PADRAO, p.velocidade(), 1e-6f);
        p.abrir(null, false);
        for (int i = 0; i < 20; i++) p.update(DT);
        assertFalse("◀ ▶ fora da VELOCIDADE não mudam nada", p.ajustar(1));
        p.mover(1);
        assertEquals(MenuPausa.Opcao.VELOCIDADE, p.cursor());
        assertTrue(p.ajustar(1));
        assertEquals(0.9f, p.velocidade(), 1e-6f);
        p.ajustar(1);
        p.ajustar(1);
        assertEquals("para na ponta", 1f, p.velocidade(), 1e-6f);
        assertNull("A na VELOCIDADE não fecha a pausa", p.confirmar());
        assertTrue(p.isOpen());
        assertEquals("e dá a volta", 0.5f, p.velocidade(), 1e-6f);
        p.render(new PaginaHistoriaTest.NullCanvas());

        float[] caixa = MenuPausa.CAIXAS[1];
        float cy = (caixa[1] + caixa[3]) * 0.5f;
        assertNull(p.tap(caixa[2] - 20f, cy));
        assertEquals("metade direita: mais rápido", 0.6f, p.velocidade(), 1e-6f);
        assertNull(p.tap(caixa[0] + 20f, cy));
        assertEquals("metade esquerda: mais devagar", 0.5f, p.velocidade(), 1e-6f);
        assertTrue(p.isOpen());

        p.setVelocidade(0.77f);
        assertEquals("salva fora da lista vira a mais próxima", 0.8f, p.velocidade(), 1e-6f);
        p.abrir(null, false);
        assertEquals("reabrir não volta ao padrão", 0.8f, p.velocidade(), 1e-6f);
    }
}
