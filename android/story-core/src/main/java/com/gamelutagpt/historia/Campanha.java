package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderAssets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * O que o jogador já decidiu numa partida do modo história: com qual irmão joga (escolhido
 * depois do Prólogo) e a chave do torneio (sorteada no passo "sorteio" ou, se ele foi pulado,
 * na primeira luta do torneio).
 *
 * A sorte da chave vem de uma semente: a mesma semente, o mesmo irmão e o mesmo número de
 * rodadas disputadas refazem exatamente a mesma chave. É assim que o save
 * ({@link ProgressoHistoria}) guarda a campanha sem guardar a chave inteira.
 */
public final class Campanha {
    private final List<Torneio.Dupla> duplas;
    private final long semente;
    private String irmao;
    private Torneio torneio;

    public Campanha(List<Torneio.Dupla> duplas, long semente) {
        this.duplas = duplas == null ? Collections.<Torneio.Dupla>emptyList() : duplas;
        this.semente = semente;
    }

    /** Campanha nova com as duplas de {@code historia/torneio/torneio.json}. */
    public static Campanha load(RenderAssets assets, long semente) throws IOException {
        return new Campanha(Torneio.duplasFromJson(assets.readText(Torneio.ARQUIVO)), semente);
    }

    public long semente() {
        return semente;
    }

    /** O irmão escolhido, ou null antes da escolha. */
    public String irmaoEscolhido() {
        return irmao;
    }

    /** Rodadas do torneio já disputadas, ou -1 se a chave ainda não foi sorteada. */
    public int disputadas() {
        return torneio == null ? -1 : torneio.disputadas();
    }

    /**
     * Volta ao ponto salvo: o irmão escolhido (ou null) e quantas rodadas da chave já foram
     * disputadas (-1 = chave ainda não sorteada). O jogador venceu todas (a derrota repete a luta).
     */
    public void restaurar(String irmao, int disputadas) {
        this.irmao = null;
        torneio = null;
        if (irmao != null) escolher(irmao);
        if (disputadas < 0 || !temTorneio()) return;
        Torneio t = torneio();
        for (int i = 0; i < disputadas && !t.acabou(); i++) t.registrar(true);
    }

    /** O irmão do jogador ({@link Torneio#RYUGA} ou {@link Torneio#KOGA}); Ryūga até a escolha. */
    public String irmao() {
        return irmao != null ? irmao : Torneio.RYUGA;
    }

    public boolean escolheu() {
        return irmao != null;
    }

    /** A escolha depois do Prólogo. Trocar de irmão desfaz a chave (o sorteio depende do lado). */
    public void escolher(String irmao) {
        String novo = Torneio.KOGA.equals(irmao) ? Torneio.KOGA : Torneio.RYUGA;
        if (!novo.equals(this.irmao)) torneio = null;
        this.irmao = novo;
    }

    /** O rival: o outro irmão. */
    public String rival() {
        return Torneio.KOGA.equals(irmao()) ? Torneio.RYUGA : Torneio.KOGA;
    }

    public boolean temTorneio() {
        return !duplas.isEmpty();
    }

    /** A chave, sorteada na primeira vez que é pedida; null sem {@code torneio.json}. */
    public Torneio torneio() {
        // Random novo com a semente a cada sorteio: a mesma chave sempre que for refeita.
        if (torneio == null && temTorneio()) torneio = Torneio.sortear(duplas, irmao(), new Random(semente));
        return torneio;
    }

    /** A chave só se já foi sorteada (o desenho não sorteia sozinho). */
    public Torneio torneioSorteado() {
        return torneio;
    }

    /** A dupla fixa do irmão {@code quem}, ou null. */
    public Torneio.Dupla duplaDo(String quem) {
        for (Torneio.Dupla d : duplas) {
            if (quem.equals(d.irmao)) return d;
        }
        return null;
    }

    /**
     * As duplas da luta: {jogador, CPU}, cada uma {titular, parceiro}. Resolve
     * {@code "dupla": "jogador"} / {@code "cpu": "rival"} pelo irmão escolhido e
     * {@code "torneio": true} pelo próximo adversário da chave. Null: a luta usa as duplas que já
     * estão no jogo (ou a referência não pôde ser resolvida).
     */
    public String[][] lados(Episodio.Passo luta) {
        String[] jogador;
        String[] cpu;
        if (luta.duelo) {
            // 1x1: o capitão da dupla do jogador contra o próprio parceiro.
            Torneio.Dupla dupla = duplaDo(irmao());
            if (dupla == null) return null;
            return new String[][] {{dupla.lutadores[0]}, {dupla.lutadores[1]}};
        }
        if (luta.emboscada) {
            // Fora da arena: a dupla que perdeu a outra quarta do lado do jogador.
            Torneio t = torneioSorteado();
            Torneio.Dupla emboscada = t != null ? t.emboscada() : null;
            if (emboscada == null) return null;
            return new String[][] {t.jogador().lutadores.clone(), emboscada.lutadores.clone()};
        }
        if (luta.torneio) {
            Torneio t = torneio();
            Torneio.Dupla adversario = t != null ? t.adversario() : null;
            if (adversario == null) return null;
            jogador = t.jogador().lutadores;
            cpu = adversario.lutadores;
        } else {
            jogador = resolver(luta.duplaRef, luta.dupla);
            cpu = resolver(luta.cpuRef, luta.cpu);
        }
        return jogador != null && cpu != null ? new String[][] {jogador.clone(), cpu.clone()} : null;
    }

    private String[] resolver(String ref, String[] fixo) {
        if (ref == null) return fixo;
        Torneio.Dupla d = duplaDo("jogador".equals(ref) ? irmao() : rival());
        return d != null ? d.lutadores : null;
    }

    /** Rótulo da luta: o do passo ou, numa luta do torneio, o nome da rodada. */
    public String rotulo(Episodio.Passo luta) {
        if (luta.rotulo != null) return luta.rotulo;
        if (luta.duelo) return DUELO;
        if (luta.emboscada) return EMBOSCADA;
        if (!luta.torneio) return null;
        Torneio t = torneio();
        return t != null ? t.rodadaAtual() : null;
    }

    /** Rótulo padrão do duelo 1x1. */
    public static final String DUELO = "DUELO FINAL";

    /** Rótulo padrão da emboscada fora da arena. */
    public static final String EMBOSCADA = "EMBOSCADA";

    /**
     * O passo vale para esta partida: passos com {@code "irmao"} só para quem escolheu esse
     * irmão; a emboscada, só depois que o jogador passou das quartas; o duelo, só para a dupla
     * campeã.
     */
    public boolean vale(Episodio.Passo passo) {
        if (passo.irmao != null && !passo.irmao.equals(irmao())) return false;
        if (passo.emboscada) {
            Torneio t = torneioSorteado();
            if (t == null || t.emboscada() == null) return false;
        }
        if (passo.duelo) {
            Torneio t = torneioSorteado();
            if (t == null || t.campeao() == null || t.campeao() != t.jogador()) return false;
        }
        return true;
    }
}
