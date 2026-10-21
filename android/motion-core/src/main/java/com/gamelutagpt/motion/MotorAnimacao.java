package com.gamelutagpt.motion;

/**
 * Motor de animação de um lutador: o movimento por cima dos quadros desenhados.
 *
 * A cada passo da luta (60 por segundo) o dono entrega uma {@link Leitura}; o motor percebe os
 * eventos (saiu do chão, aterrissou, caiu, virou, apanhou, defendeu, preparou e soltou o golpe,
 * largou e freou o dash) e responde com molas de forma fechada ({@link Canal}). A {@link Pose}
 * do quadro diz como desenhar: achatar/esticar com os pés parados, inclinação, arrasto, quique,
 * smear, a mistura curta com o quadro de antes, o rastro e as partículas do chão.
 *
 * O relógio do motor só anda fora do congelamento (hitstop, pausa do Super): o tranco de um
 * golpe começa quando a luta volta a andar, como no desenho animado. Tudo é conta do quadro do
 * relógio: o mesmo jogo dá a mesma animação.
 */
public final class MotorAnimacao {
    /** Quique do corpo caído: sobe ligeiro e desce sem passar do chão (amortecimento crítico). */
    private static final Mola QUIQUE = new Mola(3f, 1f);
    /** Maior inclinação pedida pelos alvos (dash, antecipação, arco), em graus. */
    private static final float MAX_ALVO = 10f;
    /** Velocidade horizontal (px/s) abaixo da qual o pulo conta como parado (sem arco). */
    private static final float VX_ARCO = 60f;

    public final Ajustes ajustes;
    /** Positivo achata (mais baixo e largo); negativo estica para cima. */
    private final Canal achatar = new Canal(Mola.PADRAO);
    /** Positivo alonga na horizontal (mais largo e baixo). */
    private final Canal esticar = new Canal(Mola.PADRAO);
    /** Graus; positivo para onde o lutador olha. */
    private final Canal inclinar = new Canal(Mola.PADRAO);
    /** Graus de cisalhamento (o arrasto); positivo leva a cabeça para onde o lutador olha. */
    private final Canal arrastar = new Canal(Mola.PADRAO);
    /** Fração da largura que a virada tira (só a largura: é um giro, não um achatar). */
    private final Canal virar = new Canal(Mola.RAPIDA);
    /** Pixels para cima do quique. */
    private final Canal subir = new Canal(Mola.PADRAO);
    private final Pose pose;

    private int relogio;
    private boolean primeiro = true;
    private boolean noChaoAntes = true, ativoAntes, preparandoAntes;
    private float vyAntes;
    private String clipeAntes;
    private int quadroAntes, olhandoAntes = 1;

    /** Dash em andamento (já com velocidade): para que lado (1 frente, -1 trás) e sentido no mundo. */
    private boolean correndo;
    private int ladoDash, sentidoDash;

    /** Quadros de smear que faltam (contados em passos: o smear aparece mesmo no hitstop). */
    private int smearRestante;

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
        arrastar.zerar();
        virar.zerar();
        subir.zerar();
        relogio = 0;
        primeiro = true;
        clipeAntes = clipeSaindo = null;
        transicaoInicio = Integer.MIN_VALUE;
        for (int i = 0; i < nascimento.length; i++) nascimento[i] = Integer.MIN_VALUE / 2;
        quadrosSemCopia = 0;
        correndo = false;
        smearRestante = 0;
        pose.neutralizar();
    }

    /** A pose do último passo. */
    public Pose pose() {
        return pose;
    }

    /** Um passo da luta. */
    public void passo(Leitura l) {
        pose.poeira = 0f;
        pose.deslize = 0;
        if (!ajustes.ligado) {
            pose.neutralizar();
            lembrar(l);
            return;
        }
        if (!l.congelado) relogio++;
        int q = relogio;
        float k = ajustes.intensidade;
        // Peso do personagem: o pesado deforma menos (forma) e inclina menos (ângulos).
        float peso = l.peso > 0f ? l.peso : 1f;
        float forma = k / peso, angulo = k / (float)Math.sqrt(peso);
        if (!primeiro) {
            eventos(l, q, k, forma, angulo, peso);
            transicao(l, q);
        }
        alvos(l, q, forma, angulo);
        smear(l, k);
        if (!l.congelado) rastro(l, q);
        montarPose(q);
        lembrar(l);
    }

    private void eventos(Leitura l, int q, float k, float forma, float angulo, float peso) {
        if (!noChaoAntes && l.noChao) {
            // Bater no chão corta o esticar do ar: o achatar sai inteiro.
            achatar.zerar();
            if (l.atingido && ajustes.quique) {
                achatar.degrau(q, ajustes.achatarQueda * forma, Mola.ELASTICA);
                subir.impulso(q, ajustes.alturaQuique * forma, QUIQUE);
            } else if (ajustes.pulo) {
                // Quanto mais rápido caía, mais achata (um pulinho achata pouco).
                float queda = clamp(Math.abs(vyAntes) / 1600f, 0.35f, 1f);
                achatar.degrau(q, ajustes.achatarAterrissagem * queda * forma, Mola.ELASTICA);
                // A queda do knockdown já tem a poeira dela no jogo.
                if (ajustes.poeira && !l.atingido) pose.poeira = Math.min(1f, ajustes.poeiraAterrissagem * queda * peso);
            }
        } else if (ajustes.pulo && noChaoAntes && !l.noChao && l.vy < 0f) {
            achatar.degrau(q, -ajustes.esticarDecolagem * forma, Mola.RAPIDA);
        }
        if (ajustes.virada && l.olhando != olhandoAntes) {
            // No quadro em que espelha o desenho sai estreito e abre de volta: lê como um giro.
            virar.degrau(q, ajustes.estreitarVirada * k, Mola.RAPIDA);
        }
        if (ajustes.impacto) {
            // Para que lado o empurrão leva, visto por quem apanha (sem direção: para trás).
            int lado = l.empurrao == 0 ? -1 : (l.empurrao * l.olhando > 0 ? 1 : -1);
            if (l.apanhou >= 0) {
                int f = peso(l.apanhou, ajustes.inclinacaoApanhando);
                inclinar.impulso(q, ajustes.inclinacaoApanhando[f] * lado * angulo, mola(f));
            } else if (l.defendeu) {
                inclinar.impulso(q, ajustes.inclinacaoDefendendo * lado * angulo, Mola.RAPIDA);
            }
        }
        dash(l, q, angulo);
        if (l.golpeAtivo && !ativoAntes) {
            int f = Math.max(0, l.forca);
            if (ajustes.estalo) {
                esticar.impulso(q, ajustes.esticarGolpe[peso(f, ajustes.esticarGolpe)] * forma, Mola.RAPIDA);
            }
            if (ajustes.continuidade) {
                // O corpo segue o golpe um pouco para a frente e assenta (pela mola do peso dele).
                int g = peso(f, ajustes.grausContinuidade);
                inclinar.impulso(q, ajustes.grausContinuidade[g] * angulo, mola(g));
            }
            if (ajustes.smear && f >= ajustes.forcaSmear) smearRestante = Math.max(1, ajustes.quadrosSmear);
        }
    }

    /** Largada e freada do dash: o arrasto do tronco e o pé deslizando. */
    private void dash(Leitura l, int q, float angulo) {
        if (l.dash && !correndo && l.vx != 0f) {
            // Largada (no primeiro passo com velocidade, para saber o lado).
            correndo = true;
            ladoDash = l.vx * l.olhando > 0f ? 1 : -1;
            sentidoDash = l.vx > 0f ? 1 : -1;
            if (ajustes.arrasto) arrastar.impulso(q, -0.5f * ajustes.grausArrasto * ladoDash * angulo, Mola.RAPIDA);
            if (ajustes.poeira && l.noChao) pose.deslize = sentidoDash;
        } else if (!l.dash && correndo) {
            correndo = false;
            // Freada no chão (o pulo que sai da corrida e o fim do air dash não freiam).
            if (l.noChao && noChaoAntes) {
                if (ajustes.arrasto) arrastar.impulso(q, ajustes.grausArrasto * ladoDash * angulo, Mola.ELASTICA);
                if (ajustes.poeira) pose.deslize = sentidoDash;
            }
        }
    }

    /** O que o corpo busca agora: inclinação do dash, da antecipação e do arco; abaixar e esticar no ar. */
    private void alvos(Leitura l, int q, float forma, float angulo) {
        float graus = 0f, baixo = 0f;
        if (ajustes.inclinacaoDash && l.dash) {
            float frente = l.vx * l.olhando;
            if (frente != 0f) graus += Math.signum(frente) * ajustes.grausDash * angulo;
        }
        boolean prepara = ajustes.antecipacao && l.preparando;
        if (prepara) {
            // Recua e abaixa um pouco na preparação: o golpe sai daí.
            graus -= ajustes.grausAntecipacao[peso(l.forca, ajustes.grausAntecipacao)] * angulo;
            baixo += ajustes.achatarAntecipacao * forma;
        }
        if (ajustes.ar && !l.noChao && !l.atingido) {
            baixo -= ajustes.esticarAr * clamp(Math.abs(l.vy) / ajustes.vyCheia, 0f, 1f) * forma;
            if (!l.golpe && Math.abs(l.vx) > VX_ARCO) {
                // Acompanha a curva: subindo, a cabeça vai para onde ele anda; descendo, volta.
                float arco = ajustes.grausArco * (l.embalo ? ajustes.arcoEmbalo : 1f);
                graus += arco * Math.signum(l.vx * l.olhando) * clamp(-l.vy / ajustes.vyCheia, -1f, 1f) * angulo;
            }
        }
        graus = clamp(graus, -MAX_ALVO, MAX_ALVO);
        // Saindo da preparação (o golpe sai seco) e ao trocar chão/ar, o corpo vai ligeiro ao alvo.
        boolean ligeiro = (preparandoAntes && !prepara) || l.noChao != noChaoAntes;
        if (ligeiro) {
            inclinar.alvo(q, graus, Mola.RAPIDA);
            achatar.alvo(q, baixo, Mola.RAPIDA);
        } else {
            inclinar.alvo(q, graus);
            achatar.alvo(q, baixo);
        }
    }

    private void smear(Leitura l, float k) {
        // Golpe cancelado (ou acabou): o smear some junto.
        if (!l.golpeAtivo) smearRestante = 0;
        if (smearRestante > 0) {
            pose.smear = smearRestante / (float)Math.max(1, ajustes.quadrosSmear);
            pose.smearEsticar = ajustes.esticarSmear * k;
            smearRestante--;
        } else {
            pose.smear = pose.smearEsticar = 0f;
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
        boolean golpe = ajustes.rastroNosGolpes && l.golpeAtivo && l.forca >= ajustes.forcaRastro;
        boolean deixa = ajustes.rastro && l.clipe != null && (l.dash || l.embalo || golpe);
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
        float a = achatar.valor(q), e = esticar.valor(q), v = virar.valor(q);
        float altura, largura;
        if (ajustes.volume) {
            // Área constante: o que o achatar tira da altura entra na largura, e o esticar na
            // horizontal tira da altura o que põe na largura.
            altura = (1f - a) / (1f + e);
            largura = 1f / altura;
        } else {
            altura = (1f - a) * (1f - 0.5f * e);
            largura = (1f + 0.6f * a) * (1f + e);
        }
        pose.escalaY = clamp(altura, 0.8f, 1.25f);
        pose.escalaX = clamp(largura * (1f - clamp(v, 0f, 0.6f)), 0.4f, 1.25f);
        pose.inclinacao = clamp(inclinar.valor(q), -15f, 15f);
        pose.arrasto = clamp(arrastar.valor(q), -8f, 8f);
        pose.deslocX = 0f;
        // Só para cima: o quique nunca enterra o corpo no chão.
        pose.deslocY = -Math.max(0f, subir.valor(q));
        achatar.limpar(q);
        esticar.limpar(q);
        inclinar.limpar(q);
        arrastar.limpar(q);
        virar.limpar(q);
        subir.limpar(q);

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
        preparandoAntes = ajustes.antecipacao && l.preparando;
        clipeAntes = l.clipe;
        quadroAntes = l.quadro;
        olhandoAntes = l.olhando;
    }

    /** A mola do peso {@code forca} (o fraco volta ligeiro, o forte balança devagar). */
    private Mola mola(int forca) {
        return ajustes.molaPorForca[peso(forca, ajustes.molaPorForca.length)];
    }

    private static int peso(int forca, float[] tabela) {
        return peso(forca, tabela.length);
    }

    private static int peso(int forca, int tamanho) {
        return Math.max(0, Math.min(tamanho - 1, forca));
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : v > max ? max : v;
    }
}
