package com.gamelutagpt.ultra;

/**
 * Os 5 painéis fixos da "Página Final". Todo ultra usa a mesma estrutura;
 * o que muda são as imagens, os textos e os sons.
 */
public enum UltraSlot {
    OLHOS("olhos", "", "painel"),
    CARGA("carga", "VRUUUUM", "carga"),
    GOLPE("golpe", "KRAAK!", "impacto"),
    ATINGIDO("atingido", "TUMM!", "impacto"),
    FINAL("final", "KABUUUM!", "explosao");

    /** Nome do campo no ultra.json e nome padrão do arquivo de imagem. */
    public final String key;
    public final String defaultOnomatopoeia;
    public final String defaultSound;

    UltraSlot(String key, String defaultOnomatopoeia, String defaultSound) {
        this.key = key;
        this.defaultOnomatopoeia = defaultOnomatopoeia;
        this.defaultSound = defaultSound;
    }
}
