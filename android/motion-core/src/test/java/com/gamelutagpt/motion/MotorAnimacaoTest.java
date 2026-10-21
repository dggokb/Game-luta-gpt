package com.gamelutagpt.motion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MotorAnimacaoTest {
    private final Leitura l = new Leitura();

    private Leitura parado(String clipe) {
        l.limparEventos();
        l.noChao = true;
        l.vx = l.vy = 0f;
        l.olhando = 1;
        l.clipe = clipe;
        l.quadro = 0;
        l.golpe = l.atingido = l.golpeAtivo = l.dash = l.embalo = l.congelado = false;
        l.forca = 0;
        return l;
    }

    @Test
    public void paradoAPoseENeutra() {
        MotorAnimacao m = new MotorAnimacao();
        for (int i = 0; i < 30; i++) m.passo(parado("IDLE"));
        assertTrue(m.pose().neutra());
        assertNull(m.pose().clipeAnterior);
        assertEquals(0, m.pose().fantasmas);
    }

    @Test
    public void aterrissarAchataEVoltaAoNormal() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        // Caindo rápido, depois toca o chão.
        Leitura no = parado("FALL");
        no.noChao = false;
        no.vy = 1500f;
        m.passo(no);
        m.passo(parado("LAND"));
        Pose p = m.pose();
        assertTrue("mais baixo", p.escalaY < 0.95f);
        assertTrue("mais largo", p.escalaX > 1.03f);
        for (int i = 0; i < 120; i++) m.passo(parado("IDLE"));
        assertEquals(1f, m.pose().escalaY, 1e-3f);
        assertEquals(1f, m.pose().escalaX, 1e-3f);
    }

    @Test
    public void sairDoChaoEstica() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        Leitura sobe = parado("JUMP");
        sobe.noChao = false;
        sobe.vy = -900f;
        m.passo(sobe);
        assertTrue("mais alto", m.pose().escalaY > 1.03f);
        assertTrue("mais fino", m.pose().escalaX < 0.98f);
    }

    @Test
    public void apanharInclinaParaOLadoDoEmpurraoSoDepoisDoCongelamento() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        // Olhando para a direita, empurrado para a esquerda (para trás).
        Leitura golpe = parado("HIT_STAND");
        golpe.atingido = true;
        golpe.apanhou = 2;
        golpe.empurrao = -1;
        golpe.congelado = true;
        m.passo(golpe);
        // Durante o hitstop a pose fica parada (o tremor do impacto é do jogo).
        for (int i = 0; i < 6; i++) {
            Leitura preso = parado("HIT_STAND");
            preso.atingido = true;
            preso.congelado = true;
            m.passo(preso);
            assertEquals(0f, m.pose().inclinacao, 1e-6f);
        }
        float menor = 0f;
        for (int i = 0; i < 20; i++) {
            Leitura solto = parado("HIT_STAND");
            solto.atingido = true;
            m.passo(solto);
            menor = Math.min(menor, m.pose().inclinacao);
        }
        assertTrue("tronco foi para trás: " + menor, menor < -4f);
    }

    @Test
    public void defenderEmpurraMenosQueApanhar() {
        float apanhando = picoDeInclinacao(false), defendendo = picoDeInclinacao(true);
        assertTrue(Math.abs(defendendo) < Math.abs(apanhando));
        assertTrue(defendendo < 0f);
    }

    private float picoDeInclinacao(boolean defende) {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        Leitura e = parado(defende ? "DEFENSE_STAND" : "HIT_STAND");
        if (defende) e.defendeu = true;
        else e.apanhou = 1;
        e.empurrao = -1;
        m.passo(e);
        float pico = 0f;
        for (int i = 0; i < 30; i++) {
            m.passo(parado(defende ? "DEFENSE_STAND" : "HIT_STAND"));
            if (Math.abs(m.pose().inclinacao) > Math.abs(pico)) pico = m.pose().inclinacao;
        }
        return pico;
    }

    @Test
    public void correndoInclinaParaAFrenteEVoltaAoParar() {
        MotorAnimacao m = new MotorAnimacao();
        for (int i = 0; i < 40; i++) {
            Leitura corre = parado("DASH");
            corre.dash = true;
            corre.vx = -620f;
            corre.olhando = -1;
            m.passo(corre);
        }
        assertEquals("olhando para a esquerda e correndo para lá: para a frente",
            m.ajustes.grausDash, m.pose().inclinacao, 0.2f);
        for (int i = 0; i < 120; i++) {
            Leitura para = parado("IDLE");
            para.olhando = -1;
            m.passo(para);
        }
        assertEquals(0f, m.pose().inclinacao, 0.01f);
    }

    @Test
    public void trocarDeClipeMisturaOQuadroDeAntesMasGolpeEntraSeco() {
        MotorAnimacao m = new MotorAnimacao();
        Leitura andando = parado("WALK_FORWARD");
        andando.quadro = 5;
        m.passo(andando);
        m.passo(parado("IDLE"));
        assertEquals("WALK_FORWARD", m.pose().clipeAnterior);
        assertEquals(5, m.pose().quadroAnterior);
        float alfa = m.pose().alfaAnterior;
        assertEquals(m.ajustes.alfaTransicao, alfa, 1e-6f);
        m.passo(parado("IDLE"));
        assertTrue("some aos poucos", m.pose().alfaAnterior < alfa && m.pose().alfaAnterior > 0f);
        for (int i = 0; i < m.ajustes.quadrosTransicao; i++) m.passo(parado("IDLE"));
        assertNull(m.pose().clipeAnterior);

        Leitura golpe = parado("LIGHT_JAB");
        golpe.golpe = true;
        m.passo(golpe);
        assertNull("golpe entra seco", m.pose().clipeAnterior);
    }

    @Test
    public void viradoDeLadoNaoMistura() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("WALK_BACK"));
        Leitura virou = parado("IDLE");
        virou.olhando = -1;
        m.passo(virou);
        assertNull(m.pose().clipeAnterior);
    }

    @Test
    public void dashDeixaRastroQueSome() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        for (int i = 0; i < 6; i++) {
            Leitura d = parado("DASH");
            d.dash = true;
            d.vx = 620f;
            d.quadro = i;
            d.x = 100f + i * 10f;
            m.passo(d);
        }
        Pose p = m.pose();
        assertTrue("tem cópias", p.fantasmas >= 2);
        for (int i = 1; i < p.fantasmas; i++) {
            assertTrue("a mais nova por último e mais forte", p.rastro[i].alfa > p.rastro[i - 1].alfa);
            assertTrue(p.rastro[i].x > p.rastro[i - 1].x);
        }
        for (int i = 0; i < 20; i++) m.passo(parado("IDLE"));
        assertEquals(0, m.pose().fantasmas);
    }

    @Test
    public void puloComEmbaloDaCorridaDeixaRastroEPuloComumNao() {
        MotorAnimacao comEmbalo = new MotorAnimacao(), comum = new MotorAnimacao();
        for (int i = 0; i < 6; i++) {
            Leitura a = parado("JUMP");
            a.noChao = false;
            a.embalo = true;
            comEmbalo.passo(a);
            Leitura b = parado("JUMP");
            b.noChao = false;
            comum.passo(b);
        }
        assertTrue(comEmbalo.pose().fantasmas >= 2);
        assertEquals(0, comum.pose().fantasmas);
    }

    @Test
    public void golpeSoDeixaRastroSeLigarNosGolpes() {
        MotorAnimacao padrao = new MotorAnimacao();
        Ajustes nosGolpes = new Ajustes();
        nosGolpes.rastroNosGolpes = true;
        MotorAnimacao ligado = new MotorAnimacao(nosGolpes);
        for (int i = 0; i < 6; i++) {
            Leitura h = parado("HEAVY_STRAIGHT");
            h.golpe = h.golpeAtivo = true;
            h.forca = 2;
            padrao.passo(h);
            Leitura h2 = parado("HEAVY_STRAIGHT");
            h2.golpe = h2.golpeAtivo = true;
            h2.forca = 2;
            ligado.passo(h2);
        }
        assertEquals("padrão: golpe não deixa rastro", 0, padrao.pose().fantasmas);
        assertTrue(ligado.pose().fantasmas >= 2);
    }

    @Test
    public void desligadoNadaMudaEOMesmoJogoDaAMesmaAnimacao() {
        MotorAnimacao desligado = new MotorAnimacao(Ajustes.desligado());
        MotorAnimacao a = new MotorAnimacao(), b = new MotorAnimacao();
        for (int i = 0; i < 300; i++) {
            Leitura x = roteiro(i);
            desligado.passo(x);
            assertTrue(desligado.pose().neutra());
            assertNull(desligado.pose().clipeAnterior);
            a.passo(roteiro(i));
            b.passo(roteiro(i));
            assertEquals(a.pose().escalaX, b.pose().escalaX, 0f);
            assertEquals(a.pose().escalaY, b.pose().escalaY, 0f);
            assertEquals(a.pose().inclinacao, b.pose().inclinacao, 0f);
            assertEquals(a.pose().alfaAnterior, b.pose().alfaAnterior, 0f);
        }
    }

    @Test
    public void zerarVoltaAoRepouso() {
        MotorAnimacao m = new MotorAnimacao();
        for (int i = 0; i < 50; i++) m.passo(roteiro(i));
        m.zerar();
        m.passo(parado("IDLE"));
        assertTrue(m.pose().neutra());
        assertFalse(m.pose().fantasmas > 0);
    }

    /** Uma luta inventada: pula, corre, apanha, bate. */
    private static Leitura roteiro(int i) {
        Leitura r = new Leitura();
        int fase = i % 60;
        r.noChao = fase < 20 || fase > 45;
        r.vy = r.noChao ? 0f : (fase < 33 ? -800f : 900f);
        r.dash = fase > 48 && fase < 56;
        r.vx = r.dash ? 620f : 0f;
        r.clipe = r.noChao ? (r.dash ? "DASH" : "IDLE") : "JUMP";
        r.quadro = fase % 8;
        if (fase == 10) {
            r.apanhou = i % 4;
            r.empurrao = -1;
        }
        r.congelado = fase > 10 && fase < 15;
        r.golpeAtivo = fase > 56;
        r.forca = 2;
        return r;
    }
}
