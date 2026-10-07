package com.gamelutagpt.ultra;

/** Resultado do toque no tempo certo durante o painel final. */
public enum UltraGrade {
    NENHUM(0f, ""),
    ERROU(0f, "ERROU..."),
    BOM(0.10f, "BOM!"),
    PERFEITO(0.25f, "PERFEITO!");

    /** Bônus aplicado sobre o dano da detonação final e do raio. */
    public final float finalBonus;
    public final String label;

    UltraGrade(float finalBonus, String label) {
        this.finalBonus = finalBonus;
        this.label = label;
    }
}
