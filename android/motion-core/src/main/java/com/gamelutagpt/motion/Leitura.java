package com.gamelutagpt.motion;

/**
 * O que o motor de animação lê de um lutador a cada passo da luta (60 por segundo). Quem usa
 * o motor preenche estes campos a partir do próprio estado (no jogo, do CombatFighter e do
 * clipe que o renderer escolheu) e chama {@link MotorAnimacao#passo}. Nada aqui depende do
 * jogo: o motor só vê números e o nome do clipe.
 */
public final class Leitura {
    /** Pés no chão. */
    public boolean noChao = true;
    /** Velocidade no mundo (px/s); y positivo para baixo. */
    public float vx, vy;
    /** Para onde o lutador olha no mundo: 1 direita, -1 esquerda. */
    public int olhando = 1;
    /** Posição da raiz (os pés) no mundo, para o rastro. */
    public float x, y;
    /** Clipe e quadro da folha que vão ser desenhados. */
    public String clipe;
    public int quadro;
    /** O clipe é um golpe (entra seco, sem transição: o tempo do golpe é sagrado). */
    public boolean golpe;
    /** Apanhando ou caído: as reações também entram secas, para o impacto ler. */
    public boolean atingido;
    /** O golpe está na janela ativa (pode acertar). */
    public boolean golpeAtivo;
    /** Peso do golpe próprio: 0 fraco, 1 médio, 2 forte, 3 especial/Super. */
    public int forca;
    /** Correndo ou no air dash (para a frente ou para trás). */
    public boolean dash;
    /** No ar com o embalo da corrida (o pulo que saiu do dash; o super pulo não conta). */
    public boolean embalo;
    /** Levou um golpe neste passo: o peso dele (0 a 3), ou -1. */
    public int apanhou = -1;
    /** Defendeu um golpe neste passo. */
    public boolean defendeu;
    /** Direção (no mundo) para onde o golpe recebido ou defendido empurra: 1 direita, -1 esquerda. */
    public int empurrao;
    /** Congelado no impacto (hitstop) ou na pausa do Super: o relógio do motor não anda. */
    public boolean congelado;

    /** Limpa os eventos de um passo (o resto é estado e o dono reescreve). */
    public void limparEventos() {
        apanhou = -1;
        defendeu = false;
    }
}
