package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Partículas dos poderes: fagulhas e estouro da bola de energia, fita dos objetos arremessados. */
public class ParticulasTest {
    private static final float DT = 1f / 60f;
    private static final List<Projectile> NADA = Collections.emptyList();

    private static Projectile projetil(String visual, int direcao) {
        return new Projectile(500f, 600f, 900f, 760f, 24f, 100, 0xFFF4B73B, 0, direcao, null, visual);
    }

    private static void voar(Particulas p, Projectile b, int quadros) {
        List<Projectile> lista = new ArrayList<>();
        lista.add(b);
        for (int i = 0; i < quadros; i++) {
            b.x += b.direction * b.speed * DT;
            p.passo(DT, lista, NADA);
        }
    }

    @Test public void theEnergyBallShedsSparksThatStayBehindIt() {
        for (int direcao : new int[] {1, -1}) {
            Particulas p = new Particulas();
            Projectile bola = projetil(null, direcao);
            voar(p, bola, 30);
            assertTrue("sparks in flight", p.vivas() >= 4);
            float[] faixa = p.faixaX();
            if (direcao > 0) assertTrue("behind the ball", faixa[1] < bola.x);
            else assertTrue("behind the ball", faixa[0] > bola.x);
            assertEquals("energy leaves no ribbon", 0, p.pontosDaFita(bola));
        }
    }

    @Test public void aThrownObjectLeavesARibbonAndNoSparks() {
        Particulas p = new Particulas();
        Projectile kunai = projetil("KUNAI", 1);
        voar(p, kunai, 30);
        assertEquals(0, p.vivas());
        assertTrue("a ribbon of several points", p.pontosDaFita(kunai) >= 6);
        assertFalse("its hit is the air burst, not an energy burst", p.acerto(kunai.x, kunai.y, 1, false, false));
        assertEquals(0, p.vivas());
        // Saiu de cena: a fita recolhe em poucos quadros.
        for (int i = 0; i < 12; i++) p.passo(DT, NADA, NADA);
        assertEquals(0, p.pontosDaFita(kunai));
    }

    @Test public void theBallBurstsForwardOnHitAndBackOnBlock() {
        for (boolean defendido : new boolean[] {false, true}) {
            Particulas p = new Particulas();
            Projectile bola = projetil("ONDA", 1);
            List<Projectile> lista = new ArrayList<>();
            lista.add(bola);
            p.passo(DT, lista, NADA);
            int antes = p.vivas();
            assertTrue(p.acerto(bola.x + 20f, bola.y, 1, defendido, false));
            int estouro = p.vivas() - antes;
            assertTrue("a burst of sparks: " + estouro, estouro >= 8);
            // O projétil some no mesmo quadro: quem já estourou não se desfaz de novo.
            p.passo(DT, NADA, NADA);
            assertTrue(p.vivas() <= antes + estouro);
            for (int i = 0; i < 6; i++) p.passo(DT, NADA, NADA);
            float[] faixa = p.faixaX();
            float meio = (faixa[0] + faixa[1]) * 0.5f;
            if (defendido) assertTrue("bounces back off the guard", meio < bola.x + 20f);
            else assertTrue("goes on through the victim", meio > bola.x + 20f);
        }
    }

    @Test public void aBallThatEndsInTheAirFallsApartAndEverythingDies() {
        Particulas p = new Particulas();
        Projectile bola = projetil("ONDA_CURTA", -1);
        voar(p, bola, 3);
        int antes = p.vivas();
        p.passo(DT, NADA, NADA);
        assertTrue("a small puff where it ended", p.vivas() > antes);
        for (int i = 0; i < 60; i++) p.passo(DT, NADA, NADA);
        assertEquals("nothing lingers", 0, p.vivas());
    }

    @Test public void onlyEnergyAndThrownObjectsAreFollowed() {
        assertEquals(Particulas.ENERGIA, Particulas.tipoDe(projetil(null, 1)));
        assertEquals(Particulas.ENERGIA, Particulas.tipoDe(projetil("PALMA", 1)));
        assertEquals(Particulas.OBJETO, Particulas.tipoDe(projetil("PEDRA", 1)));
        assertEquals(Particulas.OBJETO, Particulas.tipoDe(projetil("MACHADO", 1)));
        for (String v : new String[] {"SOMBRA", "CHICOTE", "FOGO", "CAIXA", "BALAO", "ESTOURO", "LUVA"}) {
            assertEquals(v, Particulas.NENHUM, Particulas.tipoDe(projetil(v, 1)));
        }
    }
}
