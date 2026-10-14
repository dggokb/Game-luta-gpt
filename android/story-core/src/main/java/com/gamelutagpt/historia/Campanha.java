package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderAssets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * O que o jogador já decidiu numa partida do modo história: com qual irmão joga (escolhido
 * depois do Prólogo) e a chave do torneio (sorteada no passo "sorteio" ou, se ele foi pulado,
 * na primeira luta do torneio). Vive enquanto a história toca; ainda não é salvo.
 */
public final class Campanha {
    private final List<Torneio.Dupla> duplas;
    private final Random random;
    private String irmao;
    private Torneio torneio;

    public Campanha(List<Torneio.Dupla> duplas, Random random) {
        this.duplas = duplas == null ? Collections.<Torneio.Dupla>emptyList() : duplas;
        this.random = random;
    }

    /** Campanha nova com as duplas de {@code historia/torneio/torneio.json}. */
    public static Campanha load(RenderAssets assets, Random random) throws IOException {
        return new Campanha(Torneio.duplasFromJson(assets.readText(Torneio.ARQUIVO)), random);
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
        if (torneio == null && temTorneio()) torneio = Torneio.sortear(duplas, irmao(), random);
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
        if (luta.rotulo != null || !luta.torneio) return luta.rotulo;
        Torneio t = torneio();
        return t != null ? t.rodadaAtual() : null;
    }

    /** O passo vale para o irmão escolhido (passos com {@code "irmao"} só tocam para ele). */
    public boolean vale(Episodio.Passo passo) {
        return passo.irmao == null || passo.irmao.equals(irmao());
    }
}
