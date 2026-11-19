package com.gamelutagpt;

import java.util.HashMap;
import java.util.Map;

/**
 * O agarrão de cada lutador com personalidade (o dono, 18/11: "agarrões também têm que ter
 * personalidade"). Sem cena, o agarrão é o genérico do motor: o rival cai na hora e escorrega para a
 * frente. Com cena, a fase de jogar dura o tempo do vídeo do personagem, o rival segue o desenho por
 * pontos marcados (preso, no ar girando, cravado no chão), o dano entra no impacto e ele fica deitado
 * onde caiu. Os quadros são do motor (60 por segundo), contados do começo da fase de jogar
 * (THROW_EXECUTE); a animação THROW_TOSS é esticada sobre {@link #quadros}.
 */
final class CenaDoAgarrao {
    /** Poses do rival: preso pela roupa, no ar (girando), deitado. */
    static final int PRESO = 0, NO_AR = 1, DEITADO = 2;
    /** No 1º ponto, "frente" igual a isto: a distância em que ele foi agarrado. */
    static final float ONDE_PEGOU = -1f;

    /** Duração da fase de jogar, o quadro do dano (o impacto) e o quadro em que ele vira derrubado. */
    final int quadros, acerto, solta;
    /**
     * Pontos do caminho: {quadro, frente (px à frente de quem agarra), altura (px do chão aos pés),
     * giro (graus; positivo leva a cabeça para longe de quem agarra), pose}. Entre dois pontos a posição
     * e o giro andam em linha; a pose é a do ponto anterior.
     */
    final float[][] pontos;

    CenaDoAgarrao(int quadros, int acerto, int solta, float[][] pontos) {
        this.quadros = quadros;
        this.acerto = acerto;
        this.solta = solta;
        this.pontos = pontos;
    }

    /** {frente, altura, giro, pose} no quadro {@code q}; {@code pegou} é a distância de quando agarrou. */
    float[] em(int q, float pegou) {
        float[] a = pontos[0], b = a;
        for (float[] p : pontos) {
            if (p[0] <= q) a = p;
            if (p[0] >= q) { b = p; break; }
            b = p;
        }
        float k = b[0] > a[0] ? (q - a[0]) / (b[0] - a[0]) : 0f;
        k = Math.max(0f, Math.min(1f, k));
        float fa = a[1] == ONDE_PEGOU ? pegou : a[1], fb = b[1] == ONDE_PEGOU ? pegou : b[1];
        int pose = (int) a[4];
        float giro = pose == DEITADO ? 0f : a[3] + (b[3] - a[3]) * k;
        return new float[] {fa + (fb - fa) * k, a[2] + (b[2] - a[2]) * k, giro, pose};
    }

    private static final Map<String, CenaDoAgarrao> CENAS = new HashMap<>();

    static {
        float P = PRESO, A = NO_AR, D = DEITADO, G = ONDE_PEGOU;
        // Ryūga: puxa pela gola, se curva e passa o rival por cima do ombro; ele cai de costas na frente.
        CENAS.put("player_base", new CenaDoAgarrao(40, 20, 30, new float[][] {
            {0, G, 0, 0, P}, {6, 30, 50, -40, A}, {13, 5, 150, -120, A}, {20, 140, 0, 0, D}, {40, 140, 0, 0, D}}));
        // Yin-lu: torce o braço, carrega no quadril e projeta por cima dele até o chão na frente.
        CENAS.put("p02", new CenaDoAgarrao(63, 28, 42, new float[][] {
            {0, G, 0, 0, P}, {10, 35, 20, 0, P}, {18, 10, 110, -70, A}, {24, 30, 140, -150, A},
            {28, 150, 0, 0, D}, {63, 150, 0, 0, D}}));
        // Natisuko: agarra a gola, varre a perna (ele cai de costas aos pés dela) e crava o soco para baixo.
        CENAS.put("p15", new CenaDoAgarrao(48, 23, 30, new float[][] {
            {0, G, 0, 0, P}, {8, 55, 0, 15, P}, {13, 70, 50, 60, A}, {18, 85, 0, 0, D}, {48, 85, 0, 0, D}}));
        // Kōga (19/11, o dono: "jogar o adversário longe, tipo o agarrão tradicional do Ryu"): puxa pela
        // gola, arremessa com os dois braços para cima e para a frente; o rival voa alto, girando, e cai
        // de costas longe.
        CENAS.put("p03", new CenaDoAgarrao(60, 46, 48, new float[][] {
            {0, G, 0, 0, P}, {12, 35, 20, 0, P}, {19, 60, 90, -40, A}, {28, 170, 190, -150, A},
            {38, 290, 120, -260, A}, {46, 350, 0, 0, D}, {60, 350, 0, 0, D}}));
        // Bonnie: número de circo: levanta o rival acima da cabeça, segura para a plateia e crava na frente.
        CENAS.put("p16", new CenaDoAgarrao(66, 32, 45, new float[][] {
            {0, G, 0, 0, P}, {4, 40, 30, 0, P}, {9, 15, 150, -90, A}, {25, 15, 160, -90, A},
            {29, 80, 120, -150, A}, {32, 150, 0, 0, D}, {66, 150, 0, 0, D}}));
    }

    /** A cena do agarrão do lutador, ou null (o agarrão genérico). */
    static CenaDoAgarrao de(CharacterDefinition c) {
        return c == null ? null : CENAS.get(c.id);
    }
}
