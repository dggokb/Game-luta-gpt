package com.gamelutagpt.motion;

/**
 * Motor de animação de um lutador: o movimento por cima dos quadros desenhados.
 *
 * A cada passo da luta (60 por segundo) o dono entrega uma {@link Leitura}; o motor percebe os
 * eventos (saiu do chão, aterrissou, apanhou, defendeu, o golpe ficou ativo, trocou de clipe,
 * está correndo) e responde com molas de forma fechada ({@link Canal}). A {@link Pose} do
 * quadro diz como desenhar: achatar/esticar com os pés parados, inclinação, a mistura curta
 * com o quadro de antes e o rastro.
 *
 * O relógio do motor só anda fora do congelamento (hitstop, pausa do Super): o tranco de um
 * golpe começa quando a luta volta a andar, como no desenho animado. Tudo é conta do quadro do
 * relógio: o mesmo jogo dá a mesma animação.
 */
public final class MotorAnimacao {
    public final Ajustes ajustes;
    /** Positivo achata (mais baixo e largo); negativo estica para cima. */
    private final Canal achatar = new Canal(Mola.PADRAO);
    /** Positivo alonga na horizontal (mais largo e baixo). */
    private final Canal esticar = new Canal(Mola.PADRAO);
    /** Graus; positivo para onde o lutador olha. */
    private final Canal inclinar = new Canal(Mola.PADRAO);
    private final Pose pose;

    private int relogio;
    private boolean primeiro = true;
    private boolean noChaoAntes = true, ativoAntes;
    private float vyAntes;
    private String clipeAntes;
    private int quadroAntes, olhandoAntes = 1;

    private String clipeSaindo;
    private int quadroSaindo, transicaoInicio = Integer.MIN_VALUE;

    /** Rastro: fila circular de cópias e quando cada uma nasceu (no relógio do motor). */
    private final Pose.Fantasma[] copias;
    private final int[] nascimento;
    private int proximaCopia, quadrosSemCopia;

    public MotorAnimacao() {
        this(new Ajustes());
    }

    public MotorAnimacao(Ajustes ajustes) {
        this.ajustes = ajustes;
        int max = Math.max(1, ajustes.copiasRastro);
        pose = new Pose(max);
        copias = new Pose.Fantasma[max];
        nascimento = new int[max];
        for (int i = 0; i < max; i++) {
            copias[i] = new Pose.Fantasma();
            nascimento[i] = Integer.MIN_VALUE / 2;
        }
    }

    /** Volta ao repouso (round novo, troca de personagem). */
    public void zerar() {
        achatar.zerar();
        esticar.zerar();
        inclinar.zerar();
        relogio = 0;
        primeiro = true;
        clipeAntes = clipeSaindo = null;
        transicaoInicio = Integer.MIN_VALUE;
        for (int i = 0; i < nascimento.length; i++) nascimento[i] = Integer.MIN_VALUE / 2;
        quadrosSemCopia = 0;
        pose.neutralizar();
    }

    /** A pose do último passo. */
    public Pose pose() {
        return pose;
    }

    /** Um passo da luta. */
    public void passo(Leitura l) {
        if (!ajustes.ligado) {
            pose.neutralizar();
            lembrar(l);
            return;
        }
        if (!l.congelado) relogio++;
        int q = relogio;
        float k = ajustes.intensidade;
        if (!primeiro) {
            eventos(l, q, k);
            transicao(l, q);
        }
        float alvo = 0f;
        if (ajustes.inclinacaoDash && l.dash) {
            float frente = l.vx * l.olhando;
            if (frente != 0f) alvo = Math.signum(frente) * ajustes.grausDash * k;
        }
        inclinar.alvo(q, alvo);
        if (!l.congelado) rastro(l, q);
        montarPose(q);
        lembrar(l);
    }

    private void eventos(Leitura l, int q, float k) {
        if (ajustes.pulo) {
            if (!noChaoAntes && l.noChao) {
                // Quanto mais rápido caía, mais achata (um pulinho achata pouco).
                float peso = clamp(Math.abs(vyAntes) / 1600f, 0.35f, 1f);
                achatar.degrau(q, ajustes.achatarAterrissagem * peso * k, Mola.ELASTICA);
            } else if (noChaoAntes && !l.noChao && l.vy < 0f) {
                achatar.degrau(q, -ajustes.esticarDecolagem * k, Mola.RAPIDA);
            }
        }
        if (ajustes.impacto) {
            // Para que lado o empurrão leva, visto por quem apanha (sem direção: para trás).
            int lado = l.empurrao == 0 ? -1 : (l.empurrao * l.olhando > 0 ? 1 : -1);
            if (l.apanhou >= 0) {
                float graus = ajustes.inclinacaoApanhando[peso(l.apanhou, ajustes.inclinacaoApanhando)];
                inclinar.impulso(q, graus * lado * k, Mola.ELASTICA);
            } else if (l.defendeu) {
                inclinar.impulso(q, ajustes.inclinacaoDefendendo * lado * k, Mola.RAPIDA);
            }
        }
        if (ajustes.estalo && l.golpeAtivo && !ativoAntes) {
            esticar.impulso(q, ajustes.esticarGolpe[peso(l.forca, ajustes.esticarGolpe)] * k, Mola.RAPIDA);
        }
    }

    private void transicao(Leitura l, int q) {
        boolean trocou = clipeAntes != null && l.clipe != null && !l.clipe.equals(clipeAntes);
        if (!trocou) return;
        // Golpes e reações entram secos; virar de lado também (o quadro de antes estaria espelhado),
        // e sair do chão ou aterrissar (as poses mudam demais: viraria dupla exposição).
        if (ajustes.transicao && !l.golpe && !l.atingido && l.olhando == olhandoAntes && l.noChao == noChaoAntes) {
            clipeSaindo = clipeAntes;
            quadroSaindo = quadroAntes;
            transicaoInicio = q;
        } else {
            transicaoInicio = Integer.MIN_VALUE;
        }
    }

    private void rastro(Leitura l, int q) {
        boolean deixa = ajustes.rastro && l.clipe != null &&
            ((l.golpeAtivo && l.forca >= ajustes.forcaRastro) || l.dash);
        if (!deixa) {
            quadrosSemCopia = 0;
            return;
        }
        if (quadrosSemCopia++ % Math.max(1, ajustes.intervaloRastro) != 0) return;
        Pose.Fantasma f = copias[proximaCopia];
        f.clipe = l.clipe;
        f.quadro = l.quadro;
        f.x = l.x;
        f.y = l.y;
        f.olhando = l.olhando;
        nascimento[proximaCopia] = q;
        proximaCopia = (proximaCopia + 1) % copias.length;
    }

    private void montarPose(int q) {
        float a = achatar.valor(q), e = esticar.valor(q);
        pose.escalaY = clamp((1f - a) * (1f - 0.5f * e), 0.8f, 1.2f);
        pose.escalaX = clamp((1f + 0.6f * a) * (1f + e), 0.8f, 1.2f);
        pose.inclinacao = clamp(inclinar.valor(q), -15f, 15f);
        pose.deslocX = pose.deslocY = 0f;
        achatar.limpar(q);
        esticar.limpar(q);
        inclinar.limpar(q);

        int n = q - transicaoInicio;
        if (clipeSaindo != null && n >= 0 && n < ajustes.quadrosTransicao) {
            pose.clipeAnterior = clipeSaindo;
            pose.quadroAnterior = quadroSaindo;
            pose.alfaAnterior = ajustes.alfaTransicao * (1f - n / (float)ajustes.quadrosTransicao);
        } else {
            pose.clipeAnterior = null;
            pose.alfaAnterior = 0f;
        }

        // Do mais velho para o mais novo (o desenho põe o mais novo por cima).
        pose.fantasmas = 0;
        for (int i = 0; i < copias.length; i++) {
            int j = (proximaCopia + i) % copias.length;
            int idade = q - nascimento[j];
            if (idade <= 0 || idade > ajustes.vidaRastro) continue;
            Pose.Fantasma de = copias[j], para = pose.rastro[pose.fantasmas++];
            para.clipe = de.clipe;
            para.quadro = de.quadro;
            para.x = de.x;
            para.y = de.y;
            para.olhando = de.olhando;
            para.alfa = ajustes.alfaRastro * (1f - idade / (float)(ajustes.vidaRastro + 1));
        }
    }

    private void lembrar(Leitura l) {
        primeiro = false;
        noChaoAntes = l.noChao;
        vyAntes = l.vy;
        ativoAntes = l.golpeAtivo;
        clipeAntes = l.clipe;
        quadroAntes = l.quadro;
        olhandoAntes = l.olhando;
    }

    private static int peso(int forca, float[] tabela) {
        return Math.max(0, Math.min(tabela.length - 1, forca));
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : v > max ? max : v;
    }
}
