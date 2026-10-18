package com.gamelutagpt;

/**
 * Nível da CPU (1 a 8). A dificuldade vem de quanto ela demora para reagir, de quanto ela
 * acerta a defesa, do tamanho dos combos e de quanto ela pune e tira do ar. Nunca de dano maior
 * nem de ler o botão do jogador: tudo o que ela reage passa pela {@link CpuPerception}, que só
 * mostra a luta de {@link #reactionFrames} quadros atrás.
 */
final class CpuLevel {
    /**
     * Reação humana mais rápida que a CPU pode ter: 13 quadros (~217 ms a 60 fps), o tempo de um
     * jogador muito bom reagindo a algo que ele espera. Nenhum nível reage antes disso.
     */
    static final int HUMAN_REACTION_FRAMES = 13;
    static final int MIN = 1, MAX = 8, DEFAULT = 5;

    final int number;
    /** Quadros entre o rival mostrar algo e a CPU poder reagir (nunca abaixo do humano). */
    final int reactionFrames;
    /** Atraso extra sorteado a cada reação (0 a este valor): ninguém reage sempre igual. */
    final int reactionJitter;
    /**
     * Colada no rival, segura a defesa agachada por hábito (sem reagir a nada): o jab é rápido
     * demais para defender reagindo. Baixo de propósito (o que dá graça é a luta, não a defesa):
     * a janela é curta e acaba logo depois da pressão, e na saída da defesa ela pune.
     */
    final double guardAnticipation;
    /** Viu o golpe (ou o pulo) chegando a tempo: chance de defender. */
    final double reactGuard;
    /** Defendendo, acerta a altura (agachada contra golpe baixo, em pé contra pulo). */
    final double guardHeight;
    /** Golpes do caderno de combos que ela sabe fazer seguidos. */
    final int comboDepth;
    /** Viu que acertou: chance de seguir para o lançador, o especial ou o Super. */
    final double confirm;
    /** Chance de errar o próximo golpe do combo (o combo cai). */
    final double drop;
    /** Viu o rival errar um golpe ao alcance: chance de punir. */
    final double punish;
    /** Viu o rival pular para cima dela: chance de tirar do ar com o 2H. */
    final double antiAir;
    /** Viu um projétil vindo: chance de pular por cima (senão defende). */
    final double jumpProjectile;
    /** Esperava um agarrão (rival colado): chance de apertar o tech. */
    final double techGuess;
    final double airTech;
    /** Intervalo entre decisões no neutro, em segundos. */
    final float decisionMin, decisionMax;
    /** Escala das chances do kit da batalha (assist, DHC, Overdrive, troca). */
    final double kitScale;

    private CpuLevel(int number, int reactionFrames, int reactionJitter, double guardAnticipation,
                     double reactGuard, double guardHeight, int comboDepth, double confirm, double drop,
                     double punish, double antiAir, double jumpProjectile, double techGuess, double airTech,
                     float decisionMin, float decisionMax, double kitScale) {
        if (reactionFrames < HUMAN_REACTION_FRAMES) {
            throw new IllegalArgumentException("nível " + number + ": reação abaixo da humana");
        }
        this.number = number; this.reactionFrames = reactionFrames; this.reactionJitter = reactionJitter;
        this.guardAnticipation = guardAnticipation; this.reactGuard = reactGuard; this.guardHeight = guardHeight;
        this.comboDepth = comboDepth; this.confirm = confirm; this.drop = drop; this.punish = punish;
        this.antiAir = antiAir; this.jumpProjectile = jumpProjectile; this.techGuess = techGuess;
        this.airTech = airTech; this.decisionMin = decisionMin; this.decisionMax = decisionMax;
        this.kitScale = kitScale;
    }

    //                                 reação  hábito defesa altura combo confirma cai  pune  anti  pula  tech  air  decisão     kit
    private static final CpuLevel[] LEVELS = {
        new CpuLevel(1, 30, 10, 0.05, 0.10, 0.50,  1, 0.00, 0.50, 0.00, 0.00, 0.05, 0.05, 0.20, 0.35f, 0.60f, 0.0),
        new CpuLevel(2, 26,  9, 0.10, 0.25, 0.60,  2, 0.10, 0.40, 0.10, 0.10, 0.10, 0.10, 0.35, 0.30f, 0.50f, 0.2),
        new CpuLevel(3, 22,  8, 0.15, 0.40, 0.70,  3, 0.25, 0.30, 0.25, 0.20, 0.15, 0.15, 0.45, 0.25f, 0.45f, 0.4),
        new CpuLevel(4, 19,  7, 0.20, 0.55, 0.78,  4, 0.40, 0.20, 0.40, 0.35, 0.25, 0.20, 0.55, 0.20f, 0.40f, 0.6),
        new CpuLevel(5, 16,  6, 0.25, 0.65, 0.85,  6, 0.55, 0.12, 0.55, 0.50, 0.35, 0.25, 0.65, 0.16f, 0.34f, 0.8),
        new CpuLevel(6, 15,  5, 0.30, 0.75, 0.90,  8, 0.70, 0.08, 0.68, 0.62, 0.45, 0.30, 0.75, 0.14f, 0.30f, 1.0),
        new CpuLevel(7, 14,  4, 0.33, 0.85, 0.94, 10, 0.85, 0.04, 0.80, 0.75, 0.55, 0.35, 0.85, 0.12f, 0.26f, 1.1),
        new CpuLevel(8, 13,  3, 0.35, 0.92, 0.97, 12, 0.95, 0.02, 0.90, 0.85, 0.65, 0.40, 0.92, 0.12f, 0.22f, 1.2),
    };

    /**
     * Chance de tentar a defesa perfeita (apertar a defesa em cima da hora) no que ela decidiu
     * defender: nada até o 4, depois sobe com o nível.
     */
    double justDefense() {
        return number <= 4 ? 0.0 : 0.08 * (number - 4);
    }

    /**
     * Chance de apostar no combo breaker a cada golpe novo do combo que está levando (com barra):
     * nada até o 3, depois sobe com o nível. A aposta é no tempo: só quebra se acertar a janela.
     */
    double comboBreaker() {
        return number <= 3 ? 0.0 : 0.12 * (number - 3);
    }

    static CpuLevel of(int number) {
        return LEVELS[Math.max(MIN, Math.min(MAX, number)) - 1];
    }

    /** Níveis abaixo de 1 ou acima de 8 viram o mais próximo. */
    static int clamp(int number) {
        return Math.max(MIN, Math.min(MAX, number));
    }
}
