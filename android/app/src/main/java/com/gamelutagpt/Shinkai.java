package com.gamelutagpt;

/**
 * SHINKAI: o estado de cada lutador (substitui o Overdrive). A barra entre as vidas enche
 * apanhando, defendendo e batendo; cheia, o lutador ativa (botão SHINKAI ou SUPER + TAG) uma
 * vez por round, por {@link CombatConfig#shinkaiFrames} quadros. Todos ganham a vida vermelha de
 * volta; o resto vem do tipo dele. Veja docs/shinkai.md.
 *
 * <p>A divisão do elenco é um ponto de partida (mudar aqui é o bastante).
 */
final class Shinkai {
    enum Type {
        /** Corpo de aço: aguenta os golpes sem perder a ação e bate mais forte. */
        POWER("POWER", 0xFFFF7A1A),
        /** Mais rápido, um air dash a mais e o sumiço (→ → perto: aparece atrás do rival). */
        SPEED("SPEED", 0xFF35E0FF),
        /** A barra de Super enche sozinha e os projéteis saem maiores, mais rápidos e dois por vez. */
        ENERGY("ENERGY", 0xFFB45CFF),
        /** Cancels livres e o parry (→ bem na hora do golpe: não entra nada). */
        TECHNIQUE("TECHNIQUE", 0xFFFFE04A),
        /**
         * PROTÓTIPO do SHINKAI individual (só o Ryūga; docs/balanco/shinkai-ryuga.md): percepção.
         * Lê o golpe (defesa perfeita mais larga vira leitura: quem atacou fica aberto), esquiva
         * pelo backdash e o golpe preciso depois. Dura pelo desgaste do corpo, acaba quando ele
         * quer, e no limite pode romper (Ruptura).
         */
        PERCEPCAO("PERCEPÇÃO", 0xFF1E9CF0);   // o azul do dragão da liberação de poder

        final String label;
        /** Cor da aura, da barra e do aviso. */
        final int color;

        Type(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    /** A cor da Ruptura do protótipo PERCEPCAO (a aura mais quente). */
    static final int COR_RUPTURA = 0xFFFF5A2A;

    private Shinkai() {}

    /** O tipo do lutador; quem não está na lista (lutadores de treino, NPC) é TECHNIQUE. */
    static Type typeOf(CharacterDefinition character) {
        return typeOf(character.id);
    }

    static Type typeOf(String id) {
        if (prototipoDesligado) return typeOfAntigo(id);
        switch (id) {
            case "player_base": case "p01":
                return Type.PERCEPCAO;
            default:
                return typeOfAntigo(id);
        }
    }

    /** Para comparar (baterias): o Ryūga volta ao ENERGY do sistema antigo. */
    static boolean prototipoDesligado;

    static Type typeOfAntigo(String id) {
        switch (id) {
            case "p04": case "p09": case "p12": case "p13":
                return Type.POWER;
            case "p03": case "p05": case "p10": case "p16":
                return Type.SPEED;
            case "player_base": case "p01": case "p06": case "p08": case "p11":
                return Type.ENERGY;
            default:
                return Type.TECHNIQUE;
        }
    }
}
