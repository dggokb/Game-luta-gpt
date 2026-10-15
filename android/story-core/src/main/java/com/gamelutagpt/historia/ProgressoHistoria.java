package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import java.util.List;
import java.util.Map;
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

    private final long semente;
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
        Campanha c = new Campanha(duplas, semente);
        Foto f = fotos.get(episodio);
        if (f != null) c.restaurar(f.irmao, f.disputadas);
        return c;
    }

    // ------------------------------------------------------------ gravação

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"versao\":1,\"semente\":\"").append(semente).append("\",\"atual\":").append(atual)
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
            return p;
        } catch (RuntimeException ex) {
            return new ProgressoHistoria(sementeNova);
        }
    }

    /** Não deixa o save apontar para além da temporada (episódios removidos numa versão nova). */
    public void limitar(int episodios) {
        atual = Math.min(atual, episodios);
        concluidos = Math.min(concluidos, episodios);
    }
}
