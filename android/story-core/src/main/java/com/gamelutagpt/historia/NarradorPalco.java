package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;

/**
 * O narrador em pessoa, no cenário: fica atrás dos lutadores (na profundidade
 * {@link #PROFUNDIDADE} do palco, na frente das paredes e atrás dos objetos do chão), anda
 * junto com a luta de um lado para o outro, vira para a ação e gesticula com o microfone
 * enquanto a {@link Narracao} fala.
 *
 * <p>É um <b>placeholder</b> animado até existirem os sprites dele: um boneco de tinta com as
 * cores do concept (terno preto, colete vermelho, dourado, microfone), articulado por
 * código (pernas e braços com duas partes, a ponta indo até um alvo).
 *
 * <ul>
 *   <li>Lado: fica de fora da dupla, do lado com mais espaço na tela; só troca de lado
 *       quando o seu aperta (e corre por trás dos lutadores até o outro).</li>
 *   <li>Calado: microfone no peito, a outra mão na cintura, respirando, o peso trocando de
 *       perna.</li>
 *   <li>Comentário ({@code normal}): microfone na boca, a mão aberta acenando para a luta.</li>
 *   <li>{@code empolgado}: o braço lá em cima; numa fala grande ele dá um pulo.</li>
 *   <li>{@code serio}: inclinado para a frente, apontando para a luta.</li>
 * </ul>
 *
 * <p>Coordenadas: {@link #alvo} e {@link #x()} no mundo do palco naquela profundidade; o
 * desenho ({@link #render}) recebe onde ficam os pés na tela e a escala (pixels por unidade).
 * Java puro, sem Android.
 */
public final class NarradorPalco {
    /** Profundidade no palco (a mesma régua do stage.json; os lutadores ficam no 0). */
    public static final float PROFUNDIDADE = 200f;
    /** Altura do boneco em pé (unidades do mundo): o desenho tem 210 e cresce {@link #TAMANHO}. */
    static final float TAMANHO = 1.3f, ALTURA = 210f * TAMANHO;
    /** Distância de fora da dupla até onde ele fica. */
    static final float AFASTA = 250f;
    /** Margem da borda da tela. */
    static final float BORDA = 80f;
    /**
     * Espaço do lado dele abaixo do qual pensa em trocar (já não cabe na distância certa da
     * dupla) e quanto o outro lado precisa ter a mais.
     */
    static final float APERTADO = AFASTA + BORDA - 40f, FOLGA = 140f;
    /** Velocidade andando e correndo (unidades por segundo); corre se o alvo está longe. */
    static final float ANDA = 150f, CORRE = 420f, LONGE = 230f;
    static final float PULO = 300f, GRAVIDADE = 1500f;

    // Paleta do concept.
    private static final int INK = 0xFF14100E;
    private static final int TERNO = 0xFF2E2623, TERNO_LUZ = 0xFF4A3E37, TERNO_FUNDO = 0xFF1C1715;
    private static final int VERMELHO = 0xFF7E1F1D, VERMELHO_LUZ = 0xFFA3302A;
    private static final int DOURADO = 0xFFC89B5A;
    private static final int PELE = 0xFFC98559, PELE_SOMBRA = 0xFFA4653F;
    private static final int CABELO = 0xFF2B1A12, SAPATO = 0xFF0C0A09;
    private static final int MIC = 0xFF161210, MIC_TELA = 0xFF4A4642;

    // Esqueleto (unidades, pés no 0, para cima é negativo, olhando para +x).
    private static final float QUADRIL = -100f, OMBRO = -163f, CABECA = -190f, RAIO_CABECA = 15f;
    private static final float COXA = 50f, CANELA = 48f, BRACO = 40f, ANTEBRACO = 38f;

    private float x = Float.NaN, vx, alvoX, olharX;
    private float y, vy;
    private int lado;
    private float facing = 1f, facingVisto = 1f;
    private float time, passo;
    private String texto;
    private String expressao;
    private int prioridade;
    private boolean boca;
    /** Pose atual, suavizada (cada parte vai até a pose pedida aos poucos). */
    private float micNaBoca, gesto, bracoAlto, aponta, inclina;
    private final float[] junta = new float[4];

    /** Luta nova: aparece direto no lugar na primeira chamada de {@link #alvo}. */
    public void reset() {
        x = Float.NaN;
        vx = vy = y = 0f;
        lado = 0;
        texto = expressao = null;
        prioridade = 0;
        micNaBoca = gesto = bracoAlto = aponta = inclina = 0f;
    }

    /**
     * Onde ficar: {@code a} e {@code b} são os lutadores e {@code esq}/{@code dir} as bordas
     * da tela, tudo no mundo do palco na profundidade dele.
     */
    public void alvo(float a, float b, float esq, float dir) {
        float lo = Math.min(a, b), hi = Math.max(a, b);
        float espacoEsq = lo - esq, espacoDir = dir - hi;
        if (lado == 0) {
            lado = espacoEsq > espacoDir ? -1 : 1;
        } else {
            float meu = lado < 0 ? espacoEsq : espacoDir, outro = lado < 0 ? espacoDir : espacoEsq;
            if (meu < APERTADO && outro > meu + FOLGA) lado = -lado;
        }
        float limiteEsq = esq + BORDA, limiteDir = dir - BORDA;
        alvoX = lado < 0 ? Math.max(limiteEsq, lo - AFASTA) : Math.min(limiteDir, hi + AFASTA);
        olharX = (lo + hi) * 0.5f;
        if (Float.isNaN(x)) {
            x = alvoX;
            facing = facingVisto = olharX >= x ? 1f : -1f;
        }
    }

    /**
     * Um passo: anda até o alvo e acompanha a fala. {@code texto} é a fala na tela (null:
     * calado; uma fala nova, de prioridade 3, faz ele pular), {@code boca} se ainda digita.
     */
    public void update(float dt, String texto, String expressao, int prioridade, boolean boca) {
        time += dt;
        if (texto != null && !texto.equals(this.texto) && prioridade >= 3 && y >= 0f) vy = -PULO;
        this.texto = texto;
        this.expressao = expressao;
        this.prioridade = prioridade;
        this.boca = boca;
        if (Float.isNaN(x)) return;

        // Anda (ou corre) até o alvo, freando perto dele.
        float falta = alvoX - x;
        float maxima = Math.abs(falta) > LONGE || Math.abs(vx) > ANDA * 1.5f && Math.abs(falta) > 40f ? CORRE : ANDA;
        float quer = Math.signum(falta) * Math.min(maxima, Math.abs(falta) * 4f);
        if (Math.abs(falta) < 4f) quer = 0f;
        vx += (quer - vx) * Math.min(1f, dt * 8f);
        x += vx * dt;
        passo += Math.abs(vx) * dt * (float)(Math.PI * 2 / (correndo() ? 150f : 95f));
        if (Math.abs(vx) < 8f) passo += (Math.round(passo / Math.PI) * (float)Math.PI - passo) * Math.min(1f, dt * 10f);

        // Pulo.
        if (vy != 0f || y < 0f) {
            vy += GRAVIDADE * dt;
            y += vy * dt;
            if (y >= 0f) {
                y = 0f;
                vy = 0f;
            }
        }

        // Vira: correndo, para onde vai; senão, para a luta.
        float quero = correndo() ? Math.signum(vx) : (olharX >= x ? 1f : -1f);
        if (Math.abs(olharX - x) < 12f && !correndo()) quero = facing;
        facing = quero;
        facingVisto += (facing - facingVisto) * Math.min(1f, dt * 12f);

        // Pose pedida pela fala.
        boolean fala = texto != null;
        boolean emp = fala && "empolgado".equals(expressao), serio = fala && "serio".equals(expressao);
        float k = Math.min(1f, dt * 9f);
        micNaBoca += ((fala ? 1f : 0f) - micNaBoca) * k;
        gesto += ((fala && !emp && !serio ? 1f : 0f) - gesto) * k;
        bracoAlto += ((emp ? 1f : 0f) - bracoAlto) * k;
        aponta += ((serio ? 1f : 0f) - aponta) * k;
        inclina += ((serio ? 1f : correndo() ? 0.8f : 0f) - inclina) * Math.min(1f, dt * 6f);
    }

    public float x() {
        return x;
    }

    public int lado() {
        return lado;
    }

    /** Para onde olha: 1 direita, -1 esquerda. */
    public float facing() {
        return facing;
    }

    public boolean correndo() {
        return Math.abs(vx) > ANDA * 1.2f;
    }

    public boolean andando() {
        return Math.abs(vx) > 8f;
    }

    public boolean noAr() {
        return y < 0f;
    }

    public boolean apontando() {
        return aponta > 0.5f;
    }

    public boolean bracoLevantado() {
        return bracoAlto > 0.5f;
    }

    public boolean microfoneNaBoca() {
        return micNaBoca > 0.5f;
    }

    // ------------------------------------------------------------ desenho

    /** Desenha com os pés em ({@code pesX}, {@code pesY}) da tela e {@code escala} px/unidade. */
    public void render(RenderCanvas c, float pesX, float pesY, float escala) {
        if (Float.isNaN(x)) return;
        escala *= TAMANHO;
        // Canhão de luz do palco em cima dele (mais forte quando fala) e a sombra no chão.
        float luz = texto != null ? 1f : 0.65f;
        float topo = pesY - 520f * escala;
        for (int i = 0; i < 2; i++) {
            float base = (i == 0 ? 78f : 52f) * escala, cima = (i == 0 ? 26f : 14f) * escala;
            int alfa = Math.round((i == 0 ? 26 : 30) * luz);
            c.fillPolygon(new float[] {pesX - cima, topo, pesX + cima, topo, pesX + base, pesY, pesX - base, pesY},
                (alfa << 24) | 0xFFF0C8);
        }
        c.fillRadialGlow(pesX, pesY - 70f * escala, 120f * escala, (Math.round(64 * luz) << 24) | 0xFFD89A);
        c.fillOval(pesX - 80f * escala, pesY - 9f * escala, pesX + 80f * escala, pesY + 9f * escala,
            (Math.round(70 * luz) << 24) | 0xFFE6B4);
        float sombra = 1f + y / 200f;
        c.fillOval(pesX - 34f * escala * sombra, pesY - 5f * escala, pesX + 34f * escala * sombra, pesY + 5f * escala,
            0x77000000);

        float vira = Math.copySign(Math.max(0.2f, Math.abs(facingVisto)), facingVisto);
        c.save();
        c.translate(pesX, pesY);
        c.scale(escala * vira, escala, 0f, 0f);

        float respira = (float)Math.sin(time * 2.2f) * 1.2f;
        float falaBob = texto != null ? (float)Math.abs(Math.sin(time * 9f)) * -2.5f * (prioridade >= 2 ? 1.4f : 1f) : 0f;
        boolean anda = andando();
        float passada = anda ? (correndo() ? 26f : 17f) : 0f;
        float sobe = anda ? -Math.abs((float)Math.cos(passo)) * (correndo() ? 6f : 3f) : 0f;
        float peso = anda ? 0f : (float)Math.sin(time * 0.9f) * 4f;
        float quadrilY = QUADRIL + y + sobe + respira * 0.5f + falaBob * 0.5f;
        float quadrilX = peso;

        // Pernas (a de trás mais escura), com o pé no chão ou levantando no passo.
        float pe1 = (float)Math.sin(passo) * passada, pe2 = -pe1;
        float lev1 = anda ? Math.max(0f, (float)Math.cos(passo)) * (correndo() ? 16f : 9f) : 0f;
        float lev2 = anda ? Math.max(0f, -(float)Math.cos(passo)) * (correndo() ? 16f : 9f) : 0f;
        float aberto = anda ? 4f : 14f;
        perna(c, quadrilX - 5f, quadrilY, -aberto + pe2, y - lev2, TERNO_FUNDO);
        perna(c, quadrilX + 5f, quadrilY, aberto + pe1, y - lev1, TERNO);

        // Corpo de cima, inclinado a partir do quadril.
        c.save();
        float graus = inclina * 9f + (bracoAlto > 0f ? -4f * bracoAlto : 0f);
        c.rotate(graus, quadrilX, quadrilY);
        float dy = quadrilY - QUADRIL + respira * 0.5f + falaBob * 0.5f;
        float ox = quadrilX;

        // Braço livre (atrás do corpo): cintura, aceno, lá em cima ou apontando.
        float omb2x = ox - 7f, omb2y = OMBRO + dy;
        float cinturaX = ox - 20f, cinturaY = QUADRIL - 8f + dy;
        float aceno = (float)Math.sin(time * 6f);
        float acenoX = ox + 34f + aceno * 6f, acenoY = OMBRO + 8f + dy + aceno * 8f;
        float altoX = ox + 10f, altoY = OMBRO - 70f + dy + (float)Math.sin(time * 10f) * 6f * bracoAlto;
        float apontaX = ox + 74f, apontaY = OMBRO - 4f + dy;
        float m2x = mistura(cinturaX, acenoX, altoX, apontaX), m2y = mistura(cinturaY, acenoY, altoY, apontaY);
        if (anda && micNaBoca < 0.5f) {
            // Andando calado: o braço balança.
            m2x = ox - 6f - (float)Math.sin(passo) * (correndo() ? 22f : 12f);
            m2y = QUADRIL - 12f + dy;
        }
        float cotoveloLado = bracoAlto > 0.5f ? -1f : 1f;
        braco(c, omb2x, omb2y, m2x, m2y, cotoveloLado, TERNO_FUNDO, aponta > 0.5f, gesto > 0.5f || bracoAlto > 0.5f);

        tronco(c, ox, dy);
        cabeca(c, ox + 2f, CABECA + dy);

        // Braço do microfone (na frente): do peito até a boca.
        float omb1x = ox + 8f, omb1y = OMBRO + 1f + dy;
        float peitoX = ox + 16f, peitoY = OMBRO + 30f + dy;
        float bocaX = ox + 18f, bocaY = CABECA + 12f + dy;
        if (bracoAlto > 0f) bocaY -= 3f * bracoAlto;
        float m1x = peitoX + (bocaX - peitoX) * micNaBoca, m1y = peitoY + (bocaY - peitoY) * micNaBoca;
        if (anda && micNaBoca < 0.5f) {
            m1x = ox + 12f + (float)Math.sin(passo) * (correndo() ? 10f : 5f);
            m1y = OMBRO + 34f + dy;
        }
        braco(c, omb1x, omb1y, m1x, m1y, 1f, TERNO, false, false);
        microfone(c, m1x, m1y);
        if (boca && texto != null) ondas(c, ox + 24f, CABECA + 6f + dy);
        c.restore();

        c.restore();
    }

    /** Pose do braço livre: cintura, aceno, lá em cima, apontando (pelo peso de cada um). */
    private float mistura(float cintura, float aceno, float alto, float aponta) {
        float resto = Math.max(0f, 1f - gesto - bracoAlto - this.aponta);
        return cintura * resto + aceno * gesto + alto * bracoAlto + aponta * this.aponta;
    }

    private void perna(RenderCanvas c, float hx, float hy, float px, float py, int cor) {
        ik(hx, hy, px, py - 9f, COXA, CANELA, -1f, junta);
        float jx = junta[0], jy = junta[1], tx = junta[2], ty = junta[3];
        membro(c, hx, hy, jx, jy, 17f, cor);
        membro(c, jx, jy, tx, ty, 14f, cor);
        // Sapato.
        float[] sapato = {tx - 8f, ty + 2f, tx - 6f, ty - 4f, tx + 10f, ty - 3f, tx + 17f, ty + 5f, tx + 17f, ty + 9f, tx - 8f, ty + 9f};
        c.fillPolygon(sapato, SAPATO);
        c.strokePolygon(sapato, 2f, INK);
        c.drawLine(tx - 2f, ty - 1f, tx + 9f, ty, 1.5f, 0x88FFFFFF);
    }

    private void braco(RenderCanvas c, float sx, float sy, float hx, float hy, float lado, int cor,
                       boolean dedo, boolean aberta) {
        ik(sx, sy, hx, hy, BRACO, ANTEBRACO, lado, junta);
        float jx = junta[0], jy = junta[1], tx = junta[2], ty = junta[3];
        membro(c, sx, sy, jx, jy, 13f, cor);
        membro(c, jx, jy, tx, ty, 11f, cor);
        // Punho dourado e a mão.
        float ux = tx - jx, uy = ty - jy;
        float n = (float)Math.sqrt(ux * ux + uy * uy);
        if (n < 1e-3f) n = 1f;
        ux /= n;
        uy /= n;
        c.drawLine(tx - ux * 4f, ty - uy * 4f, tx - ux * 1f, ty - uy * 1f, 11f, DOURADO);
        c.fillCircle(tx + ux * 4f, ty + uy * 4f, 6f, INK);
        c.fillCircle(tx + ux * 4f, ty + uy * 4f, 4.6f, PELE);
        if (dedo) {
            c.drawLine(tx + ux * 7f, ty + uy * 7f, tx + ux * 16f, ty + uy * 16f, 4.2f, INK);
            c.drawLine(tx + ux * 7f, ty + uy * 7f, tx + ux * 15.5f, ty + uy * 15.5f, 2.4f, PELE);
        } else if (aberta) {
            for (int i = -1; i <= 1; i++) {
                float ax = ux - uy * i * 0.45f, ay = uy + ux * i * 0.45f;
                c.drawLine(tx + ux * 6f, ty + uy * 6f, tx + ux * 6f + ax * 7f, ty + uy * 6f + ay * 7f, 3.6f, INK);
                c.drawLine(tx + ux * 6f, ty + uy * 6f, tx + ux * 6f + ax * 6.5f, ty + uy * 6f + ay * 6.5f, 2f, PELE);
            }
        }
    }

    private static void membro(RenderCanvas c, float x1, float y1, float x2, float y2, float largura, int cor) {
        c.drawLine(x1, y1, x2, y2, largura + 3.5f, INK);
        c.drawLine(x1, y1, x2, y2, largura, cor);
    }

    private void tronco(RenderCanvas c, float ox, float dy) {
        float q = QUADRIL + dy, o = OMBRO + dy;
        // Paletó com as abas abertas atrás.
        float[] paleto = {ox - 22f, o + 2f, ox + 22f, o + 2f, ox + 20f, q + 6f, ox + 24f, q + 34f, ox + 8f, q + 30f,
            ox - 6f, q + 30f, ox - 26f, q + 36f, ox - 21f, q + 6f};
        c.fillPolygon(paleto, TERNO);
        c.strokePolygon(paleto, 2.5f, INK);
        // Forro vermelho aparecendo nas abas.
        c.fillPolygon(new float[] {ox - 21f, q + 8f, ox - 26f, q + 34f, ox - 14f, q + 30f}, VERMELHO);
        // Colete vermelho em V, com os botões.
        float[] colete = {ox - 9f, o + 6f, ox + 13f, o + 6f, ox + 13f, q + 2f, ox + 2f, q + 8f, ox - 9f, q + 2f};
        c.fillPolygon(colete, VERMELHO);
        c.drawLine(ox + 6f, o + 14f, ox + 6f, q - 4f, 2f, VERMELHO_LUZ);
        for (int i = 0; i < 3; i++) c.fillCircle(ox + 2f, o + 24f + i * 12f, 1.6f, DOURADO);
        // Camisa preta, gravata vermelha.
        c.fillPolygon(new float[] {ox - 4f, o + 2f, ox + 8f, o + 2f, ox + 2f, o + 18f}, INK);
        c.fillPolygon(new float[] {ox + 1f, o + 4f, ox + 4f, o + 4f, ox + 5f, o + 30f, ox + 2.5f, o + 34f, ox, o + 30f}, VERMELHO_LUZ);
        // Lapelas com o friso dourado e o broche com a corrente.
        c.drawLine(ox - 10f, o + 3f, ox - 6f, o + 44f, 2.2f, DOURADO);
        c.drawLine(ox + 15f, o + 3f, ox + 13f, o + 44f, 2.2f, DOURADO);
        c.fillCircle(ox + 15f, o + 16f, 2.6f, DOURADO);
        c.drawLine(ox + 15f, o + 18f, ox + 12f, o + 30f, 1.2f, DOURADO);
        c.drawLine(ox - 22f, o + 6f, ox - 20f, q + 4f, 1.4f, TERNO_LUZ);
        // Ombros.
        c.fillCircle(ox - 16f, o + 6f, 9f, TERNO);
        c.fillCircle(ox + 16f, o + 6f, 9f, TERNO);
    }

    private void cabeca(RenderCanvas c, float hx, float hy) {
        boolean emp = texto != null && "empolgado".equals(expressao);
        boolean serio = texto != null && "serio".equals(expressao);
        c.save();
        c.rotate(emp ? -10f : serio ? 6f : 0f, hx, hy + 12f);
        // Pescoço e colarinho.
        c.drawLine(hx - 1f, hy + 10f, hx - 1f, hy + 22f, 12f, PELE_SOMBRA);
        // Rosto: cabeça e queixo.
        c.fillCircle(hx, hy, RAIO_CABECA + 2f, INK);
        c.fillCircle(hx, hy, RAIO_CABECA, PELE);
        float[] queixo = {hx - 10f, hy + 4f, hx + 14f, hy + 2f, hx + 16f, hy + 9f, hx + 8f, hy + 17f, hx - 4f, hy + 14f};
        c.fillPolygon(queixo, PELE);
        c.strokePolygon(new float[] {hx + 14f, hy + 2f, hx + 16f, hy + 9f, hx + 8f, hy + 17f, hx - 4f, hy + 14f}, 2f, INK);
        // Cavanhaque.
        c.fillPolygon(new float[] {hx + 4f, hy + 13f, hx + 12f, hy + 12f, hx + 9f, hy + 19f, hx + 4f, hy + 17f}, CABELO);
        // Orelha.
        c.fillCircle(hx - 6f, hy + 2f, 3.6f, PELE_SOMBRA);
        // Cabelo bagunçado para trás.
        float[] cabelo = {hx - 17f, hy + 6f, hx - 21f, hy - 4f, hx - 16f, hy - 9f, hx - 18f, hy - 17f, hx - 8f, hy - 16f,
            hx - 7f, hy - 23f, hx + 3f, hy - 18f, hx + 8f, hy - 24f, hx + 12f, hy - 15f, hx + 20f, hy - 14f, hx + 15f, hy - 6f,
            hx + 6f, hy - 9f, hx - 4f, hy - 6f, hx - 10f, hy + 2f};
        c.fillPolygon(cabelo, CABELO);
        c.strokePolygon(cabelo, 1.8f, INK);
        c.drawLine(hx - 6f, hy - 15f, hx + 6f, hy - 13f, 1.4f, 0xFF6B4026);
        // Olho e sobrancelha (franzida no sério e no grito).
        float brava = serio || emp ? 3f : 0f;
        c.drawLine(hx + 5f, hy - 6f - brava * 0.3f, hx + 13f, hy - 4f + brava * 0.7f, 2.2f, INK);
        c.fillCircle(hx + 10f, hy - 1f, 1.8f, INK);
        // Boca: aberta enquanto fala, sorriso de canto calado.
        boolean abre = boca && texto != null && ((int)(time * 9f) % 2 == 0 || emp);
        if (abre) {
            float alto = emp ? 6f : 4f;
            c.fillOval(hx + 6f, hy + 6f, hx + 15f, hy + 7f + alto, 0xFF5A1414);
            c.drawLine(hx + 7f, hy + 6.5f, hx + 14f, hy + 6.5f, 1.4f, 0xFFF4EDE4);
        } else if (serio) {
            c.drawLine(hx + 7f, hy + 8f, hx + 14f, hy + 8f, 1.6f, INK);
        } else {
            c.drawLine(hx + 6f, hy + 7f, hx + 14f, hy + 5f, 1.8f, INK);
        }
        c.restore();
    }

    private static void microfone(RenderCanvas c, float hx, float hy) {
        // Cabo preto com anel dourado; a tela do microfone para cima, perto da boca.
        float tx = hx + 3f, ty = hy - 10f;
        c.drawLine(hx - 1f, hy + 6f, tx, ty, 6.5f, INK);
        c.drawLine(hx - 1f, hy + 6f, tx, ty, 4.5f, MIC);
        c.drawLine(hx + 0.4f, hy - 2f, hx + 1.4f, hy - 5f, 4.5f, DOURADO);
        c.fillCircle(tx + 0.5f, ty - 3f, 5.2f, INK);
        c.fillCircle(tx + 0.5f, ty - 3f, 4f, MIC_TELA);
        c.fillCircle(tx - 0.8f, ty - 4.4f, 1.3f, 0x99FFFFFF);
    }

    /** Riscos de som saindo da boca enquanto fala. */
    private void ondas(RenderCanvas c, float bx, float by) {
        float t = (time * 3f) % 1f;
        for (int i = 0; i < 3; i++) {
            float d = 6f + ((t + i / 3f) % 1f) * 16f;
            int alfa = (int)(200 * (1f - (d - 6f) / 16f));
            float ang = (i - 1) * 0.5f;
            float cx = (float)Math.cos(ang), sy = (float)Math.sin(ang);
            c.drawLine(bx + cx * d, by + sy * d, bx + cx * (d + 5f), by + sy * (d + 5f), 2.2f, (alfa << 24) | 0xFFFFFF);
        }
    }

    /**
     * Duas partes ({@code a}, {@code b}) da raiz até a ponta, dobrando para {@code lado}.
     * Escreve a junta em {@code out[0..1]} e a ponta (encurtada se não alcança) em {@code out[2..3]}.
     */
    static void ik(float rx, float ry, float tx, float ty, float a, float b, float lado, float[] out) {
        float dx = tx - rx, dy = ty - ry;
        float d = (float)Math.sqrt(dx * dx + dy * dy);
        float max = a + b - 0.01f;
        if (d > max) {
            tx = rx + dx / d * max;
            ty = ry + dy / d * max;
            dx = tx - rx;
            dy = ty - ry;
            d = max;
        }
        d = Math.max(d, 1e-3f);
        float cos = Math.max(-1f, Math.min(1f, (a * a + d * d - b * b) / (2f * a * d)));
        double ang = Math.atan2(dy, dx) + lado * Math.acos(cos);
        out[0] = rx + (float)Math.cos(ang) * a;
        out[1] = ry + (float)Math.sin(ang) * a;
        out[2] = tx;
        out[3] = ty;
    }
}
