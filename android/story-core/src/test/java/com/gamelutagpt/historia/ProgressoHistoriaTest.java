package com.gamelutagpt.historia;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ProgressoHistoriaTest {
    private static final float DT = 1f / 60f;

    private static List<Torneio.Dupla> duplas() throws IOException {
        return Torneio.duplasFromJson(new String(Files.readAllBytes(
            new File("../app/src/main/assets", Torneio.ARQUIVO).toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void saveNovoComecaNoPrologoSoComOPrologoLiberado() {
        ProgressoHistoria p = ProgressoHistoria.fromJson("", 42L);
        assertFalse(p.temSave());
        assertEquals(0, p.atual());
        assertTrue(p.liberado(0));
        assertFalse(p.liberado(1));
        assertEquals(42L, p.semente());
        assertFalse("save estragado vira save novo", ProgressoHistoria.fromJson("{lixo", 7L).temSave());
    }

    @Test
    public void saveDeAntesDoSobAMesmaLuaParadoNaFinalJogaOBonus() throws IOException {
        // Versão 3: terminou a Semifinal (ep06) e ia para A Final, que era o ep07.
        ProgressoHistoria p = ProgressoHistoria.fromJson("{\"versao\":3,\"semente\":\"3\",\"atual\":7,\"concluidos\":7,"
            + "\"fotos\":{\"7\":{\"irmao\":\"koga\",\"disputadas\":2}}}", 9L);
        assertEquals("joga o bônus", 7, p.atual());
        assertEquals(2, p.campanhaNoInicio(7, duplas()).disputadas());
        assertEquals("A Final começa igual", 2, p.campanhaNoInicio(8, duplas()).disputadas());
        // Quem já tinha terminado a temporada continua com ela terminada.
        ProgressoHistoria fim = ProgressoHistoria.fromJson("{\"versao\":3,\"semente\":\"3\",\"atual\":9,\"concluidos\":9,"
            + "\"fotos\":{}}", 9L);
        assertEquals(10, fim.atual());
        assertEquals(10, fim.concluidos());
        assertEquals("gravado de novo não anda mais", 7, ProgressoHistoria.fromJson(p.toJson(), 9L).atual());
    }

    @Test
    public void osIrmaosJuntosContraAgentesDiferentesEmCadaOnda() throws IOException {
        Campanha c = new Campanha(duplas(), 21L);
        c.escolher(Torneio.KOGA);
        Episodio ep = Episodio.fromJson("{\"id\":\"t\",\"titulo\":\"T\",\"passos\":["
            + "{\"tipo\":\"luta\",\"dupla\":\"irmaos\",\"cpu\":\"agentes\",\"ondas\":3}]}");
        Episodio.Passo luta = ep.passos.get(0);
        assertEquals(3, luta.ondas);
        java.util.Set<String> vistos = new java.util.HashSet<>();
        for (int onda = 0; onda < 3; onda++) {
            c.onda(onda);
            String[][] lados = c.lados(luta);
            assertArrayEquals("o escolhido de titular", new String[] {"p03", "player_base"}, lados[0]);
            for (String agente : lados[1]) {
                assertTrue("sem repetir: " + agente, vistos.add(agente));
                for (Torneio.Dupla d : duplas()) {
                    if (java.util.Arrays.asList(d.lutadores).contains(agente)) {
                        assertNull("nem os irmãos nem os parceiros", d.irmao);
                        assertTrue("só quem tem golpes", d.pronta);
                    }
                }
            }
        }
        c.restaurar(Torneio.RYUGA, -1);
        assertEquals("restaurar volta à primeira onda", 0, c.onda());
        assertArrayEquals(new String[] {"player_base", "p03"}, c.lados(luta)[0]);
        assertEquals("sem ondas é uma luta", 1, Episodio.fromJson("{\"id\":\"t\",\"titulo\":\"T\",\"passos\":["
            + "{\"tipo\":\"luta\",\"dupla\":\"jogador\",\"cpu\":\"rival\"}]}").passos.get(0).ondas);
        for (String ruim : new String[] {"\"dupla\":\"agentes\",\"cpu\":\"rival\"", "\"dupla\":\"jogador\",\"cpu\":\"irmaos\"",
                "\"dupla\":\"irmaos\",\"cpu\":\"agentes\",\"ondas\":0"}) {
            try {
                Episodio.fromJson("{\"id\":\"t\",\"titulo\":\"T\",\"passos\":[{\"tipo\":\"luta\"," + ruim + "}]}");
                org.junit.Assert.fail("devia recusar " + ruim);
            } catch (IllegalArgumentException esperado) {
                // ok
            }
        }
    }

    private static String chave(Torneio t) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.vagas(); i++) sb.append(t.participante(0, i).id).append(' ');
        return sb.toString();
    }

    @Test
    public void jogarDeNovoAteOSorteioSorteiaOutroTorneio() throws IOException {
        // Como na temporada: desafio no porto no ep03 e o sorteio no ep04.
        final int porto = 3, sorteio = 4;
        List<Torneio.Dupla> duplas = duplas();
        java.util.Random sorte = new java.util.Random(11L);
        java.util.Set<String> chaves = new java.util.HashSet<>();
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            ProgressoHistoria p = new ProgressoHistoria(77L);
            Campanha c = new Campanha(duplas, 77L);
            c.escolher(irmao);
            for (int ep = 1; ep <= sorteio; ep++) p.comecou(ep, c);
            String portoAntes = p.campanhaNoInicio(sorteio, duplas).adversarioPorto().id;

            assertFalse("depois do sorteio a chave fica", p.novoSorteio(sorteio + 1, sorteio, porto, duplas, sorte));
            assertEquals(77L, p.semente());
            for (int i = 0; i < 20; i++) {
                // O próprio sorteio: outra chave, mas o adversário do porto (já enfrentado) é o mesmo.
                assertTrue(p.novoSorteio(sorteio, sorteio, porto, duplas, sorte));
                Campanha nova = p.campanhaNoInicio(sorteio, duplas);
                assertEquals(irmao, nova.irmao());
                assertEquals(portoAntes, nova.adversarioPorto().id);
                chaves.add(chave(nova.torneio()));
            }
            long antes = p.semente();
            assertTrue("antes do porto vale qualquer chave", p.novoSorteio(porto, sorteio, porto, duplas, sorte));
            assertTrue(antes != p.semente());
            assertTrue(p.novoSorteio(0, sorteio, porto, duplas, sorte));
            assertEquals("a semente nova fica no save", p.semente(), ProgressoHistoria.fromJson(p.toJson(), 1L).semente());
        }
        assertTrue("sai chave diferente: " + chaves.size(), chaves.size() > 4);
        ProgressoHistoria semSorteio = new ProgressoHistoria(3L);
        assertFalse("temporada sem sorteio", semSorteio.novoSorteio(0, -1, -1, duplas, sorte));
    }

    @Test
    public void terminarLiberaOProximoEContinuarApontaParaEle() throws IOException {
        ProgressoHistoria p = new ProgressoHistoria(5L);
        Campanha c = new Campanha(duplas(), 5L);
        p.comecou(0, c);
        p.terminou(0, c);
        assertTrue(p.temSave());
        assertEquals(1, p.atual());
        assertTrue(p.liberado(1));
        assertFalse("o que não fez não pode", p.liberado(2));
        assertTrue(p.concluido(0));
        assertFalse(p.concluido(1));
        // Voltar a um capítulo antigo não tranca os que já foram liberados.
        p.terminou(1, c);
        p.terminou(2, c);
        p.comecou(0, c);
        assertEquals(0, p.atual());
        assertEquals(3, p.concluidos());
        assertTrue(p.liberado(3));
        assertFalse(p.liberado(4));
    }

    @Test
    public void gravaELeAMesmaCampanhaComAMesmaChave() throws IOException {
        ProgressoHistoria p = new ProgressoHistoria(-1234567890123L);
        Campanha c = new Campanha(duplas(), p.semente());
        p.comecou(1, c);
        c.escolher(Torneio.KOGA);
        p.terminou(1, c);                 // ep02 começa com o Kōga e a chave ainda sem sorteio
        Torneio t = c.torneio();
        t.registrar(true);                // quartas
        p.terminou(2, c);                 // ep03 começa depois das quartas
        String json = p.toJson();

        ProgressoHistoria lido = ProgressoHistoria.fromJson(json, 1L);
        assertEquals(p.semente(), lido.semente());
        assertEquals(3, lido.atual());
        assertEquals(3, lido.concluidos());

        Campanha ep01 = lido.campanhaNoInicio(1, duplas());
        assertFalse("no começo do ep01 ainda não escolheu", ep01.escolheu());
        assertNull(ep01.torneioSorteado());

        Campanha ep02 = lido.campanhaNoInicio(2, duplas());
        assertEquals(Torneio.KOGA, ep02.irmaoEscolhido());
        assertEquals(-1, ep02.disputadas());

        Campanha ep03 = lido.campanhaNoInicio(3, duplas());
        assertEquals(1, ep03.disputadas());
        Torneio refeito = ep03.torneio();
        for (int r = 0; r <= 1; r++) {
            for (int i = 0; i < (Torneio.VAGAS >> r); i++) {
                assertEquals("a chave refeita é a mesma", t.participante(r, i).id, refeito.participante(r, i).id);
            }
        }
        assertEquals(t.emboscada().id, refeito.emboscada().id);
    }

    @Test
    public void saveNaoPassaDoFimDaTemporada() {
        ProgressoHistoria p = ProgressoHistoria.fromJson("{\"semente\":\"3\",\"atual\":12,\"concluidos\":12}", 1L);
        p.limitar(7);
        assertEquals(7, p.atual());
        assertEquals(7, p.concluidos());
    }

    private static List<MenuCapitulos.Capitulo> capitulos(int n) {
        List<MenuCapitulos.Capitulo> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new MenuCapitulos.Capitulo(i, "Cap " + i));
        return out;
    }

    private static float[] centro(MenuCapitulos m, int item) {
        float[] b = m.caixa(item);
        return new float[] {(b[0] + b[2]) * 0.5f, (b[1] + b[3]) * 0.5f};
    }

    @Test
    public void capituloTrancadoNaoAbreELiberadoAbre() {
        ProgressoHistoria p = new ProgressoHistoria(1L);
        p.terminou(0, null);
        p.terminou(1, null);
        p.comecou(2, null);
        MenuCapitulos m = new MenuCapitulos();
        m.abrir(null, capitulos(7), p);
        assertEquals("o cursor começa no CONTINUAR", MenuCapitulos.CONTINUAR, m.cursor());
        for (int i = 0; i < 30; i++) m.update(DT);
        m.render(new PaginaHistoriaTest.NullCanvas());
        float[] trancado = centro(m, 5);
        assertNull(m.tap(trancado[0], trancado[1]));
        assertTrue(m.isOpen());
        m.render(new PaginaHistoriaTest.NullCanvas());
        float[] feito = centro(m, 1);
        assertEquals(Integer.valueOf(1), m.tap(feito[0], feito[1]));
        assertFalse(m.isOpen());

        m.abrir(null, capitulos(7), p);
        for (int i = 0; i < 30; i++) m.update(DT);
        float[] cont = centro(m, MenuCapitulos.CONTINUAR);
        assertEquals(Integer.valueOf(MenuCapitulos.CONTINUAR), m.tap(cont[0], cont[1]));
    }

    @Test
    public void controleAndaPelosCapitulosEVolta() {
        ProgressoHistoria p = new ProgressoHistoria(1L);
        for (int i = 0; i < 7; i++) p.terminou(i, null);
        MenuCapitulos m = new MenuCapitulos();
        m.abrir(null, capitulos(7), p);
        assertFalse("temporada terminada: sem continuar", m.podeContinuar());
        for (int i = 0; i < 30; i++) m.update(DT);
        m.render(new PaginaHistoriaTest.NullCanvas());
        int inicio = m.cursor();
        m.mover(0, -1);
        m.mover(-1, 0);
        m.mover(-1, 0);
        m.mover(-1, 0);
        assertEquals(0, m.cursor());
        m.mover(1, 0);
        assertEquals(1, m.cursor());
        m.mover(0, 1);
        assertTrue("desce para a segunda fileira", m.cursor() >= 4);
        assertEquals(Integer.valueOf(m.cursor()), m.confirmar());
        m.abrir(null, capitulos(7), p);
        assertEquals(inicio, m.cursor());
        assertSame(MenuCapitulos.VOLTAR, m.voltar());
    }
}
