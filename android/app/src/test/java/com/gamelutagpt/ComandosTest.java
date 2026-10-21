package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;
import org.junit.Test;

/** Os botões do tutorial no toque, no teclado e em cada tipo de controle. */
public class ComandosTest {
    @Test
    public void controleReconhecidoPeloNome() {
        assertEquals(Comandos.Esquema.XBOX, Comandos.doControle("Xbox One Controller"));
        assertEquals(Comandos.Esquema.XBOX, Comandos.doControle(null));
        assertEquals(Comandos.Esquema.XBOX, Comandos.doControle("Generic USB Gamepad"));
        assertEquals(Comandos.Esquema.PLAYSTATION, Comandos.doControle("PS5 Controller"));
        assertEquals(Comandos.Esquema.PLAYSTATION, Comandos.doControle("Sony Interactive Entertainment Wireless Controller"));
        assertEquals(Comandos.Esquema.PLAYSTATION, Comandos.doControle("Wireless Controller"));
        assertEquals(Comandos.Esquema.NINTENDO, Comandos.doControle("Nintendo Switch Pro Controller"));
    }

    @Test
    public void botoesEmCadaEsquema() {
        String modelo = "Aperte {L}, {M}, {H}, {COMBO} e {SUPER}";
        assertEquals("Aperte L, M, H, COMBO e SUPER", Comandos.texto(modelo, Comandos.Esquema.TOQUE));
        assertEquals("Aperte [J], [K], [L], [U] e [I]", Comandos.texto(modelo, Comandos.Esquema.TECLADO));
        assertEquals("Aperte [X], [Y], [B], [A] e [LB]", Comandos.texto(modelo, Comandos.Esquema.XBOX));
        assertEquals("Aperte [□], [△], [○], [×] e [L1]", Comandos.texto(modelo, Comandos.Esquema.PLAYSTATION));
        assertEquals("Aperte [Y], [X], [A], [B] e [L]", Comandos.texto(modelo, Comandos.Esquema.NINTENDO));
        assertTrue(Comandos.texto("{AGARRAO}", Comandos.Esquema.TECLADO).startsWith("[H]"));
        assertTrue(Comandos.texto("{AGARRAO}", Comandos.Esquema.TOQUE).contains("o botão entre eles"));
    }

    @Test
    public void teclaOuBotaoDeControle() {
        assertTrue(Comandos.isBotaoDeControle(KeyEvent.KEYCODE_BUTTON_A));
        assertTrue(Comandos.isBotaoDeControle(KeyEvent.KEYCODE_BUTTON_START));
        assertFalse(Comandos.isBotaoDeControle(KeyEvent.KEYCODE_J));
        assertTrue(Comandos.isTeclaDeTeclado(KeyEvent.KEYCODE_J));
        assertTrue(Comandos.isTeclaDeTeclado(KeyEvent.KEYCODE_ESCAPE));
        // Setas valem para teclado e controle: não mudam o esquema.
        assertFalse(Comandos.isTeclaDeTeclado(KeyEvent.KEYCODE_DPAD_LEFT));
        assertFalse(Comandos.isTeclaDeTeclado(KeyEvent.KEYCODE_BUTTON_X));
    }

}
