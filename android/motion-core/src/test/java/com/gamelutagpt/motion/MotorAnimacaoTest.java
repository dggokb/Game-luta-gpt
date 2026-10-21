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
        l.golpe = l.atingido = l.golpeAtivo = l.preparando = l.dash = l.embalo = l.congelado = false;
        l.preparandoPulo = false;
        l.forca = 0;
        l.peso = 1f;
        l.vida = 1f;
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
        assertTrue("mais baixo", p.escalaY < 1f - 0.5f * m.ajustes.achatarAterrissagem);
        assertTrue("mais largo", p.escalaX > 1f);
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
        assertTrue("mais alto", m.pose().escalaY > 1f + 0.5f * m.ajustes.esticarDecolagem);
        assertTrue("mais fino", m.pose().escalaX < 1f);
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
    public void asMolasDoCorpoVoltamSemBalancar() {
        // Firme: depois de aterrissar a altura volta ao normal sem passar para o outro lado.
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        Leitura no = parado("FALL");
        no.noChao = false;
        no.vy = 1600f;
        m.passo(no);
        for (int i = 0; i < 60; i++) {
            m.passo(parado(i == 0 ? "LAND" : "IDLE"));
            assertTrue("sem esticar de volta: " + m.pose().escalaY, m.pose().escalaY <= 1.002f);
        }
        // E o tronco que apanhou não vai para a frente depois de ir para trás.
        MotorAnimacao h = new MotorAnimacao();
        h.passo(parado("IDLE"));
        Leitura e = parado("HIT_STAND");
        e.apanhou = 3;
        e.empurrao = -1;
        h.passo(e);
        for (int i = 0; i < 60; i++) {
            h.passo(parado("HIT_STAND"));
            assertTrue("sem balanço: " + h.pose().inclinacao, h.pose().inclinacao < 0.3f);
        }
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
    public void comVolumeAchatarEEsticarMantemAArea() {
        Ajustes comVolume = new Ajustes();
        comVolume.volume = true;
        MotorAnimacao m = new MotorAnimacao(comVolume);
        m.passo(parado("IDLE"));
        Leitura no = parado("FALL");
        no.noChao = false;
        no.vy = 1600f;
        m.passo(no);
        for (int i = 0; i < 20; i++) {
            m.passo(parado(i == 0 ? "LAND" : "IDLE"));
            assertEquals("área do desenho", 1f, m.pose().escalaX * m.pose().escalaY, 2e-3f);
        }
    }

    @Test
    public void virarEstreitaSoALarguraEAbreDeVolta() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        Leitura virou = parado("IDLE");
        virou.olhando = -1;
        m.passo(virou);
        assertTrue("estreito no quadro da virada: " + m.pose().escalaX,
            m.pose().escalaX < 1f - 0.8f * m.ajustes.estreitarVirada);
        assertEquals("a altura não muda", 1f, m.pose().escalaY, 1e-6f);
        for (int i = 0; i < 40; i++) m.passo(virou);
        assertEquals(1f, m.pose().escalaX, 1e-3f);
    }

    @Test
    public void largadaEFreadaDoDashArrastamOTroncoESoltamPoeira() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        float menor = 0f;
        for (int i = 0; i < 12; i++) {
            Leitura d = parado("DASH");
            d.dash = true;
            d.vx = 620f;
            m.passo(d);
            if (i == 0) assertEquals("pé empurra o chão na largada", 1, m.pose().deslize);
            else assertEquals(0, m.pose().deslize);
            menor = Math.min(menor, m.pose().arrasto);
        }
        assertTrue("na largada o tronco fica para trás: " + menor, menor < -0.5f);
        m.passo(parado("IDLE"));
        assertEquals("o pé desliza na freada", 1, m.pose().deslize);
        float maior = 0f;
        for (int i = 0; i < 30; i++) {
            m.passo(parado("IDLE"));
            assertEquals(0, m.pose().deslize);
            maior = Math.max(maior, m.pose().arrasto);
        }
        assertTrue("na freada passa da conta: " + maior, maior > 2.5f);
        for (int i = 0; i < 120; i++) m.passo(parado("IDLE"));
        assertEquals(0f, m.pose().arrasto, 0.01f);
    }

    @Test
    public void backdashFreiaParaTrasEPuloDaCorridaNaoFreia() {
        MotorAnimacao m = new MotorAnimacao();
        for (int i = 0; i < 8; i++) {
            Leitura d = parado("BACKDASH");
            d.dash = true;
            d.vx = -500f;
            m.passo(d);
        }
        m.passo(parado("IDLE"));
        assertEquals("deslize para a esquerda (o sentido do backdash)", -1, m.pose().deslize);
        float menor = 0f;
        for (int i = 0; i < 30; i++) {
            m.passo(parado("IDLE"));
            menor = Math.min(menor, m.pose().arrasto);
        }
        assertTrue("o tronco passa para trás: " + menor, menor < -2.5f);

        MotorAnimacao pulo = new MotorAnimacao();
        for (int i = 0; i < 8; i++) {
            Leitura d = parado("DASH");
            d.dash = true;
            d.vx = 620f;
            pulo.passo(d);
        }
        Leitura sobe = parado("JUMP");
        sobe.noChao = false;
        sobe.vy = -1400f;
        sobe.vx = 540f;
        sobe.embalo = true;
        pulo.passo(sobe);
        assertEquals("sair pulando não é freada", 0, pulo.pose().deslize);
    }

    @Test
    public void aterrissarSoltaPoeiraEOCaidoQuicaSemEnterrar() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        Leitura no = parado("FALL");
        no.noChao = false;
        no.vy = 1600f;
        m.passo(no);
        assertEquals(0f, m.pose().poeira, 0f);
        m.passo(parado("LAND"));
        assertTrue(m.pose().poeira > 0f);
        m.passo(parado("IDLE"));
        assertEquals("só no passo da aterrissagem", 0f, m.pose().poeira, 0f);

        MotorAnimacao caido = new MotorAnimacao();
        Leitura voa = parado("HIT_AIR");
        voa.noChao = false;
        voa.atingido = true;
        voa.vy = 1400f;
        caido.passo(voa);
        caido.passo(voa);
        float maisAlto = 0f, achatou = 1f;
        for (int i = 0; i < 40; i++) {
            Leitura chao = parado("KNOCKDOWN");
            chao.atingido = true;
            caido.passo(chao);
            if (i == 0) assertEquals("a poeira da queda é do jogo", 0f, caido.pose().poeira, 0f);
            assertTrue("nunca abaixo do chão", caido.pose().deslocY <= 0f);
            maisAlto = Math.min(maisAlto, caido.pose().deslocY);
            achatou = Math.min(achatou, caido.pose().escalaY);
        }
        assertTrue("quicou: " + maisAlto, maisAlto < -0.5f * caido.ajustes.alturaQuique);
        assertTrue("achata mais que a aterrissagem comum: " + achatou,
            achatou < 1f - caido.ajustes.achatarAterrissagem);
    }

    @Test
    public void preparacaoRecuaEOGolpeSaiParaAFrente() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        for (int i = 0; i < 8; i++) {
            Leitura prep = parado("HEAVY_STRAIGHT");
            prep.golpe = prep.preparando = true;
            prep.forca = 2;
            m.passo(prep);
        }
        assertTrue("recuou: " + m.pose().inclinacao, m.pose().inclinacao < -1f);
        assertTrue("abaixou", m.pose().escalaY < 1f);
        float maior = -99f;
        for (int i = 0; i < 6; i++) {
            Leitura ativo = parado("HEAVY_STRAIGHT");
            ativo.golpe = ativo.golpeAtivo = true;
            ativo.forca = 2;
            m.passo(ativo);
            maior = Math.max(maior, m.pose().inclinacao);
        }
        assertTrue("o corpo segue o golpe para a frente em poucos quadros: " + maior, maior > 1f);
    }

    @Test
    public void smearSoNosGolpesFortesENosPrimeirosQuadros() {
        MotorAnimacao forte = new MotorAnimacao(), fraco = new MotorAnimacao();
        forte.passo(parado("IDLE"));
        fraco.passo(parado("IDLE"));
        int comSmear = 0;
        for (int i = 0; i < 6; i++) {
            Leitura h = parado("HEAVY_STRAIGHT");
            h.golpe = h.golpeAtivo = true;
            h.forca = 2;
            forte.passo(h);
            if (forte.pose().smear > 0f) {
                comSmear++;
                assertTrue(forte.pose().smearEsticar > 0f);
            }
            Leitura j = parado("LIGHT_JAB");
            j.golpe = j.golpeAtivo = true;
            fraco.passo(j);
            assertEquals(0f, fraco.pose().smear, 0f);
        }
        assertEquals(forte.ajustes.quadrosSmear, comSmear);
    }

    @Test
    public void golpeForteBalancaMaisDevagarQueOFraco() {
        assertTrue(quadroDoPico(3) > quadroDoPico(0));
    }

    private int quadroDoPico(int forca) {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        Leitura e = parado("HIT_STAND");
        e.apanhou = forca;
        e.empurrao = -1;
        m.passo(e);
        int quadro = 0;
        float pico = 0f;
        for (int i = 1; i < 40; i++) {
            m.passo(parado("HIT_STAND"));
            if (Math.abs(m.pose().inclinacao) > pico) {
                pico = Math.abs(m.pose().inclinacao);
                quadro = i;
            }
        }
        return quadro;
    }

    @Test
    public void noArEsticaComAVelocidadeEInclinaNoArco() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        float subindo = 0f, descendo = 0f, maisAlto = 1f;
        for (int i = 0; i < 60; i++) {
            Leitura ar = parado("JUMP");
            ar.noChao = false;
            ar.vx = 300f;
            ar.vy = -1500f + i * 50f;
            m.passo(ar);
            if (i == 12) subindo = m.pose().inclinacao;
            if (i == 59) descendo = m.pose().inclinacao;
            maisAlto = Math.max(maisAlto, m.pose().escalaY);
        }
        assertTrue("esticou: " + maisAlto, maisAlto > 1f + 0.5f * m.ajustes.esticarAr);
        assertTrue("subindo, para a frente: " + subindo, subindo > 1f);
        assertTrue("descendo, para trás: " + descendo, descendo < -1f);

        MotorAnimacao reto = new MotorAnimacao();
        reto.passo(parado("IDLE"));
        for (int i = 0; i < 30; i++) {
            Leitura ar = parado("JUMP");
            ar.noChao = false;
            ar.vy = -1500f + i * 50f;
            reto.passo(ar);
            assertEquals("pulo parado não inclina", 0f, reto.pose().inclinacao, 1e-6f);
        }
    }

    @Test
    public void personagemPesadoDeformaMenos() {
        assertTrue(menorAltura(2f) > menorAltura(1f));
        assertTrue(menorAltura(0.7f) < menorAltura(1f));
    }

    private float menorAltura(float peso) {
        MotorAnimacao m = new MotorAnimacao();
        Leitura no = parado("FALL");
        no.noChao = false;
        no.vy = 1600f;
        no.peso = peso;
        m.passo(no);
        m.passo(no);
        float menor = 1f;
        for (int i = 0; i < 20; i++) {
            Leitura chao = parado("LAND");
            chao.peso = peso;
            m.passo(chao);
            menor = Math.min(menor, m.pose().escalaY);
        }
        return menor;
    }

    @Test
    public void acertarTravaEErrarPassaDoPonto() {
        assertTrue("errar vai mais para a frente que acertar",
            passadaDepoisDoGolpe(false) > passadaDepoisDoGolpe(true) + 2f);
    }

    /** O maior "para a frente" de um H com 4 quadros ativos, acertando no primeiro ou no vazio. */
    private float passadaDepoisDoGolpe(boolean acerta) {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        float maior = -99f;
        for (int i = 0; i < 40; i++) {
            Leitura h = parado("HEAVY_STRAIGHT");
            h.golpe = i < 20;
            h.golpeAtivo = i < 4;
            h.forca = 2;
            h.acertou = acerta && i == 0;
            m.passo(h);
            maior = Math.max(maior, m.pose().inclinacao);
        }
        return maior;
    }

    @Test
    public void naCabecaVaiParaTrasNoCorpoDobraENasPernasArrasta() {
        float[] cabeca = reacao(Leitura.ALTO), corpo = reacao(Leitura.CORPO), pernas = reacao(Leitura.BAIXO);
        assertTrue("cabeça: tronco para trás", cabeca[0] < -3f);
        assertTrue("corpo: dobra para a frente " + corpo[0], corpo[0] > 2f);
        assertTrue("corpo: encolhe", corpo[2] < 1f - 0.5f * new Ajustes().dobrarCorpo);
        assertTrue("pernas: o tronco fica, os pés vão " + pernas[1], pernas[1] > 2f);
        assertEquals("pernas: sem girar", 0f, pernas[0], 0.5f);
    }

    /** Pico da inclinação, pico do arrasto e a menor altura depois de um golpe médio-forte. */
    private float[] reacao(int altura) {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        Leitura e = parado("HIT_STAND");
        e.atingido = true;
        e.apanhou = 2;
        e.empurrao = -1;
        e.alturaGolpe = altura;
        m.passo(e);
        float[] r = {0f, 0f, 1f};
        for (int i = 0; i < 40; i++) {
            Leitura h = parado("HIT_STAND");
            h.atingido = true;
            m.passo(h);
            if (Math.abs(m.pose().inclinacao) > Math.abs(r[0])) r[0] = m.pose().inclinacao;
            if (Math.abs(m.pose().arrasto) > Math.abs(r[1])) r[1] = m.pose().arrasto;
            r[2] = Math.min(r[2], m.pose().escalaY);
        }
        return r;
    }

    @Test
    public void golpesIguaisNaoReagemIgualMasOMesmoJogoSim() {
        float[] a = picosDeCincoGolpes(), b = picosDeCincoGolpes();
        float padrao = new Ajustes().inclinacaoApanhando[2];
        java.util.Set<Float> diferentes = new java.util.HashSet<>();
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i], 0f);
            assertTrue("perto do padrão: " + a[i], a[i] > padrao * 0.75f && a[i] < padrao * 1.25f);
            diferentes.add(Math.round(a[i] * 100f) / 100f);
        }
        assertTrue("variou", diferentes.size() >= 3);
    }

    private float[] picosDeCincoGolpes() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        float[] picos = new float[5];
        for (int g = 0; g < 5; g++) {
            Leitura e = parado("HIT_STAND");
            e.apanhou = 2;
            e.empurrao = -1;
            m.passo(e);
            for (int i = 0; i < 60; i++) {
                m.passo(parado("HIT_STAND"));
                picos[g] = Math.max(picos[g], -m.pose().inclinacao);
            }
        }
        return picos;
    }

    @Test
    public void comPoucaVidaCansaERespiraDevagar() {
        MotorAnimacao m = new MotorAnimacao();
        float menor = 2f, maior = 0f;
        for (int i = 0; i < 300; i++) {
            Leitura cansado = parado("IDLE");
            cansado.vida = 0.1f;
            m.passo(cansado);
            if (i > 150) {
                menor = Math.min(menor, m.pose().escalaY);
                maior = Math.max(maior, m.pose().escalaY);
            }
        }
        assertTrue("ombros caídos: " + m.pose().inclinacao, m.pose().inclinacao > 1f);
        assertTrue("idle mais lento: " + m.pose().ritmo, m.pose().ritmo < 0.85f);
        assertTrue("respira: " + menor + ".." + maior, maior - menor > 0.6f * m.ajustes.respiracao);
        for (int i = 0; i < 300; i++) m.passo(parado("IDLE"));
        assertTrue("vida de volta, pose neutra", m.pose().neutra());
        assertEquals(1f, m.pose().ritmo, 1e-3f);
    }

    @Test
    public void defenderGolpeForteArrastaOPeDuasVezes() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("DEFENSE_STAND"));
        Leitura d = parado("DEFENSE_STAND");
        d.defendeu = true;
        d.pesoDefendido = 2;
        d.empurrao = -1;
        m.passo(d);
        assertEquals(-1, m.pose().deslize);
        int depois = 0;
        for (int i = 0; i < 10; i++) {
            m.passo(parado("DEFENSE_STAND"));
            if (m.pose().deslize != 0) {
                depois++;
                assertEquals(-1, m.pose().deslize);
            }
        }
        assertEquals(1, depois);

        MotorAnimacao fraco = new MotorAnimacao();
        fraco.passo(parado("DEFENSE_STAND"));
        Leitura jab = parado("DEFENSE_STAND");
        jab.defendeu = true;
        jab.empurrao = -1;
        fraco.passo(jab);
        assertEquals("golpe fraco não arrasta", 0, fraco.pose().deslize);
    }

    @Test
    public void agachaParaPularEEsticaAoSair() {
        MotorAnimacao m = new MotorAnimacao();
        m.passo(parado("IDLE"));
        float menor = 1f;
        for (int i = 0; i < 3; i++) {
            Leitura agacha = parado("IDLE");
            agacha.preparandoPulo = true;
            m.passo(agacha);
            menor = Math.min(menor, m.pose().escalaY);
        }
        assertTrue("agachou: " + menor, menor < 1f - 0.5f * m.ajustes.agacharPulo);
        Leitura sai = parado("JUMP");
        sai.noChao = false;
        sai.vy = -1492f;
        m.passo(sai);
        m.passo(sai);
        assertTrue("esticou ao sair: " + m.pose().escalaY, m.pose().escalaY > 1f);
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
            assertEquals(a.pose().arrasto, b.pose().arrasto, 0f);
            assertEquals(a.pose().deslocY, b.pose().deslocY, 0f);
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
