package com.gamelutagpt;

/**
 * Personalidade da CPU, pelo estilo de combo do lutador (fighter.comboStyle, de
 * tools/sprites/mecanicas.json): a distância que ela gosta, o quanto ela avança, solta
 * projétil, agarra, pula e espera defendendo.
 */
final class CpuStyle {
    final String name;
    /** Distância que ela procura no neutro (anda até ela, ou recua se o rival chega mais perto). */
    final float preferredDistance;
    /** Vontade de avançar: dash de longe e menos recuo colado. */
    final double aggression;
    /** Multiplica a vontade de soltar o projétil. */
    final double zoning;
    /** Multiplica a vontade de agarrar colado. */
    final double throwing;
    /** Multiplica a vontade de pular. */
    final double jumping;
    /** Multiplica o hábito de esperar defendendo colado. */
    final double patience;

    private CpuStyle(String name, float preferredDistance, double aggression, double zoning, double throwing,
                     double jumping, double patience) {
        this.name = name; this.preferredDistance = preferredDistance; this.aggression = aggression;
        this.zoning = zoning; this.throwing = throwing; this.jumping = jumping; this.patience = patience;
    }

    static final CpuStyle EQUILIBRADO = new CpuStyle("equilibrado", 260f, 0.50, 1.0, 1.0, 1.0, 1.1);
    private static final CpuStyle[] STYLES = {
        EQUILIBRADO,
        new CpuStyle("pressao", 120f, 0.85, 0.5, 1.4, 1.0, 0.8),
        new CpuStyle("chutes", 180f, 0.65, 0.7, 1.0, 1.2, 0.9),
        new CpuStyle("aereo", 220f, 0.70, 0.8, 1.0, 2.0, 0.8),
        new CpuStyle("alcance", 420f, 0.25, 1.8, 0.6, 0.7, 1.2),
        new CpuStyle("tanque", 100f, 0.60, 0.4, 2.2, 0.6, 1.0),
        new CpuStyle("imprevisivel", 160f, 0.75, 0.8, 1.2, 1.4, 0.6),
    };

    static CpuStyle of(String name) {
        for (CpuStyle s : STYLES) if (s.name.equals(name)) return s;
        return EQUILIBRADO;
    }

    static CpuStyle of(CharacterDefinition c) {
        return c == null ? EQUILIBRADO : of(c.fighter.comboStyle);
    }

    /** O imprevisível troca de plano de repente: às vezes faz o oposto do que o estilo pede. */
    boolean erratic() { return "imprevisivel".equals(name); }
}
