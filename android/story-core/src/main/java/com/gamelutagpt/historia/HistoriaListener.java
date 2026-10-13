package com.gamelutagpt.historia;

/** Como a {@link PaginaHistoria} conversa com o jogo. Chamado na thread do loop do jogo. */
public interface HistoriaListener {
    /**
     * O episódio chegou numa luta: o jogo monta as duplas, luta e depois chama
     * {@link PaginaHistoria#resultadoLuta(boolean)}.
     */
    void onHistoriaLuta(Episodio.Passo luta);

    /** Som da página ("pagina", "painel"); som que não existe é ignorado pelo jogo. */
    void onHistoriaSom(String id);

    /** O episódio terminou (inclusive pelo PULAR no último passo). */
    void onHistoriaFim(Episodio episodio);
}
