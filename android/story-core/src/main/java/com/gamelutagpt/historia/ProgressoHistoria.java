package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * O save do modo história: até onde o jogador chegou e como estava a campanha no começo de cada
 * episódio. O jogo grava no começo e no fim de cada episódio; quem sai no meio volta ao começo
 * daquele episódio, com a campanha como ela estava ali.
 *
 * <ul>
 *   <li>{@link #atual()}: o episódio para CONTINUAR (índice em {@link Temporada#episodios});
 *       igual ao total quando a temporada foi terminada.</li>
 *   <li>{@link #concluidos()}: quantos episódios já foram terminados (o maior alcançado).
 *       Os capítulos liberados são esses e o seguinte; os outros ficam trancados.</li>
 *   <li>A campanha guarda a semente da chave e, por episódio, o irmão escolhido e as rodadas
 *       já disputadas: com isso a chave é refeita igual ({@link Campanha#restaurar}).</li>
 *   <li>Escolher de novo um capítulo até o do sorteio troca a semente: outro torneio
 *       ({@link #novoSorteio}). O CONTINUAR mantém a chave.</li>
 * </ul>
 *
 * Java puro: o jogo guarda o texto de {@link #toJson()} (no Android, em SharedPreferences).
 */
public final class ProgressoHistoria {
    /** Como a campanha estava no começo de um episódio. */
    private static final class Foto {
        final String irmao;
        final int disputadas;

        Foto(String irmao, int disputadas) {
            this.irmao = irmao;
            this.disputadas = disputadas;
        }
    }

    /**
     * Versão do save. A 1 é de antes do ep02 "O Parceiro" (o torneio começava no ep02); a 2,
     * de antes do ep03 "A Chegada" (o torneio começava no ep03).
     */
    static final int VERSAO = 3;
    /** Onde o ep02 "O Parceiro" entrou: nos saves da versão 1, daqui em diante os índices andam um. */
    static final int EPISODIO_NOVO_V2 = 2;
    /** Onde o ep03 "A Chegada" entrou: nos saves até a versão 2, daqui em diante os índices andam um. */
    static final int EPISODIO_NOVO_V3 = 3;

    private long semente;
    private int atual;
    private int concluidos;
    private final Map<Integer, Foto> fotos = new TreeMap<>();

    public ProgressoHistoria(long semente) {
        this.semente = semente;
    }

    public long semente() {
        return semente;
    }

    /** O episódio para continuar. */
    public int atual() {
        return atual;
    }

    /** Quantos episódios já foram terminados (o mais longe que o jogador chegou). */
    public int concluidos() {
        return concluidos;
    }

    /** Já jogou alguma coisa (senão o MODO HISTÓRIA começa direto no Prólogo). */
    public boolean temSave() {
        return atual > 0 || concluidos > 0;
    }

    /** O capítulo pode ser escolhido: os já terminados e o seguinte a eles. */
    public boolean liberado(int episodio) {
        return episodio >= 0 && episodio <= concluidos;
    }

    public boolean concluido(int episodio) {
        return episodio < concluidos;
    }

    /** O episódio começou: é dele que o CONTINUAR parte, com a campanha deste momento. */
    public void comecou(int episodio, Campanha campanha) {
        atual = episodio;
        fotos.put(episodio, foto(campanha));
    }

    /** O episódio terminou: o próximo fica liberado e vira o ponto de continuar. */
    public void terminou(int episodio, Campanha campanha) {
        concluidos = Math.max(concluidos, episodio + 1);
        atual = episodio + 1;
        fotos.put(episodio + 1, foto(campanha));
    }

    private static Foto foto(Campanha campanha) {
        if (campanha == null) return new Foto(null, -1);
        return new Foto(campanha.irmaoEscolhido(), campanha.disputadas());
    }

    /**
     * A campanha como estava no começo do episódio (mesma semente, irmão e rodadas). Sem foto
     * desse episódio (o Prólogo, ou um save antigo), a campanha começa do zero.
     */
    public Campanha campanhaNoInicio(int episodio, List<Torneio.Dupla> duplas) {
        return campanhaCom(semente, episodio, duplas);
    }

    private Campanha campanhaCom(long semente, int episodio, List<Torneio.Dupla> duplas) {
        Campanha c = new Campanha(duplas, semente);
        Foto f = fotos.get(episodio);
        if (f != null) c.restaurar(f.irmao, f.disputadas);
        return c;
    }

    /** Quantas sementes {@link #novoSorteio} tenta para achar uma com o mesmo adversário no porto. */
    static final int TENTATIVAS_SORTEIO = 400;

    /**
     * O jogador escolheu jogar de novo a partir do capítulo {@code episodio}: se ele vem antes do
     * sorteio da chave, ou é o próprio sorteio, a semente troca e sai outro torneio.
     *
     * <p>O adversário do desafio no porto também sai da semente (é do lado do rival na chave que
     * vai ser sorteada). Se o porto já ficou para trás ({@code episodio > porto}), a semente nova
     * precisa dar o mesmo adversário de lá; senão ele poderia reaparecer no caminho do jogador.
     * Sem nenhuma em {@link #TENTATIVAS_SORTEIO}, fica a chave de antes.
     *
     * @param sorteio índice do episódio com o passo "sorteio" (-1: não há)
     * @param porto índice do episódio com o desafio no porto (-1: não há)
     * @return se a semente mudou
     */
    public boolean novoSorteio(int episodio, int sorteio, int porto, List<Torneio.Dupla> duplas, Random sorte) {
        if (sorteio < 0 || episodio > sorteio) return false;
        if (porto < 0 || episodio <= porto) {
            semente = sorte.nextLong();
            return true;
        }
        Torneio.Dupla antes = campanhaNoInicio(episodio, duplas).adversarioPorto();
        for (int i = 0; i < TENTATIVAS_SORTEIO; i++) {
            long nova = sorte.nextLong();
            Campanha c = campanhaCom(nova, episodio, duplas);
            Torneio.Dupla depois = c.adversarioPorto();
            if (antes == null ? depois == null : depois != null && antes.id.equals(depois.id)) {
                semente = nova;
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ gravação

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"versao\":").append(VERSAO).append(",\"semente\":\"").append(semente).append("\",\"atual\":").append(atual)
            .append(",\"concluidos\":").append(concluidos).append(",\"fotos\":{");
        boolean primeira = true;
        for (Map.Entry<Integer, Foto> e : fotos.entrySet()) {
            if (!primeira) sb.append(',');
            primeira = false;
            sb.append('"').append(e.getKey()).append("\":{\"irmao\":")
                .append(e.getValue().irmao == null ? "null" : "\"" + e.getValue().irmao + "\"")
                .append(",\"disputadas\":").append(e.getValue().disputadas).append('}');
        }
        return sb.append("}}").toString();
    }

    /** Lê o save; texto vazio ou estragado vira um save novo com {@code sementeNova}. */
    public static ProgressoHistoria fromJson(String json, long sementeNova) {
        if (json == null || json.trim().isEmpty()) return new ProgressoHistoria(sementeNova);
        try {
            Map<String, Object> map = Episodio.asMap(MiniJson.parse(json));
            ProgressoHistoria p = new ProgressoHistoria(Long.parseLong(Episodio.string(map, "semente", "0")));
            p.atual = Math.max(0, Episodio.number(map, "atual", 0).intValue());
            p.concluidos = Math.max(0, Episodio.number(map, "concluidos", 0).intValue());
            Object fotos = map.get("fotos");
            if (fotos != null) {
                for (Map.Entry<String, Object> e : Episodio.asMap(fotos).entrySet()) {
                    Map<String, Object> f = Episodio.asMap(e.getValue());
                    Object irmao = f.get("irmao");
                    p.fotos.put(Integer.parseInt(e.getKey()), new Foto(irmao == null ? null : String.valueOf(irmao),
                        Episodio.number(f, "disputadas", -1).intValue()));
                }
            }
            int versao = Episodio.number(map, "versao", 1).intValue();
            if (versao < 2) p.inserirEpisodio(EPISODIO_NOVO_V2);
            if (versao < 3) p.inserirEpisodio(EPISODIO_NOVO_V3);
            return p;
        } catch (RuntimeException ex) {
            return new ProgressoHistoria(sementeNova);
        }
    }

    /**
     * Save de antes de um episódio novo entrar no meio da temporada, na posição {@code novo}:
     * daí em diante os episódios eram um antes. Sem isso, a campanha restaurada já viria com
     * rodadas adiantadas (a emboscada some). Quem parou no começo do episódio que agora vem
     * depois do novo joga o novo (a campanha no começo dos dois é a mesma); quem já tinha
     * passado dele continua no mesmo episódio de antes.
     *
     * <ul>
     *   <li>Versão 1: o ep02 "O Parceiro" ainda não existia ({@link #EPISODIO_NOVO_V2}).</li>
     *   <li>Versão 2: o ep03 "A Chegada" ainda não existia ({@link #EPISODIO_NOVO_V3}).</li>
     * </ul>
     */
    private void inserirEpisodio(int novo) {
        Map<Integer, Foto> antigas = new TreeMap<>(fotos);
        fotos.clear();
        for (Map.Entry<Integer, Foto> e : antigas.entrySet()) {
            int ep = e.getKey();
            if (ep < novo) {
                fotos.put(ep, e.getValue());
            } else {
                fotos.put(ep + 1, e.getValue());
                // O começo do episódio novo é igual ao começo do que veio depois dele.
                if (ep == novo) fotos.put(ep, e.getValue());
            }
        }
        if (atual > novo) atual++;
        if (concluidos > novo) concluidos++;
    }

    /** Não deixa o save apontar para além da temporada (episódios removidos numa versão nova). */
    public void limitar(int episodios) {
        atual = Math.min(atual, episodios);
        concluidos = Math.min(concluidos, episodios);
    }
}
