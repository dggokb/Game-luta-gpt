package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * O telão da arena (semifinal e final): mostra a luta "ao vivo", de perto, como uma
 * transmissão, e depois de um golpe forte (Super, combo grande ou K.O.) passa o replay em
 * câmera lenta.
 *
 * <p>Leve de propósito: o telão não desenha a cena de novo. Ele redesenha só os sprites dos
 * lutadores, pequenos, por cima de um fundo liso. O replay não guarda imagens: a cada passo
 * guarda qual clipe e qual quadro cada lutador mostrava e onde ele estava (alguns números por
 * lutador), e no replay desenha esses quadros de novo.
 */
final class TelaoLuta {
    /** Lados guardados: 0 titular do jogador, 1 titular da CPU, 2 e 3 os parceiros em campo. */
    static final int LUGARES = 4;
    /** Passos guardados (4 s a 60 por segundo). */
    static final int CAPACIDADE = 240;
    /** O replay começa este tanto antes do golpe e termina este tanto depois. */
    static final int ANTES = 75, DEPOIS = 45;
    /** Velocidade do replay (câmera lenta). */
    static final float LENTO = 0.5f;
    /** Combo que vale replay. */
    static final int COMBO = 5;

    private final CharacterDefinition[][] quem = new CharacterDefinition[CAPACIDADE][LUGARES];
    private final String[][] clipe = new String[CAPACIDADE][LUGARES];
    private final int[][] quadro = new int[CAPACIDADE][LUGARES];
    private final float[][] x = new float[CAPACIDADE][LUGARES], y = new float[CAPACIDADE][LUGARES];
    private final int[][] lado = new int[CAPACIDADE][LUGARES];
    /** Passos já guardados desde o começo da luta. */
    private int gravados;

    /** Replay marcado: o golpe foi no passo {@code marco}; toca quando passar {@link #DEPOIS}. */
    private int marco = -1;
    /** Replay tocando: o passo (com fração) que aparece agora, e até onde vai. */
    private float tocando = -1f;
    private int fimDoReplay;

    // O que já foi visto, para cada golpe marcar só uma vez.
    private final AttackDefinition[] ultimoGolpe = new AttackDefinition[2];
    private final ComboSession[] ultimaSessao = new ComboSession[2];
    private final boolean[] caido = new boolean[2];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Luta nova: esquece o que passou (os lutadores de antes podem nem estar carregados). */
    void reset() {
        gravados = 0;
        marco = -1;
        tocando = -1f;
        ultimoGolpe[0] = ultimoGolpe[1] = null;
        ultimaSessao[0] = ultimaSessao[1] = null;
        caido[0] = caido[1] = false;
    }

    boolean replay() {
        return tocando >= 0f;
    }

    /** O lugar {@code i} neste passo (null = ninguém ali). */
    void gravar(int i, CharacterDefinition ch, String clip, int frame, float px, float py, int facing) {
        int k = gravados % CAPACIDADE;
        quem[k][i] = ch;
        clipe[k][i] = clip;
        quadro[k][i] = frame;
        x[k][i] = px;
        y[k][i] = py;
        lado[k][i] = facing;
    }

    /** Fecha o passo: vê se teve golpe que vale replay e anda o replay. */
    void passo(CombatEngine engine) {
        gravados++;
        if (tocando >= 0f) {
            tocando += LENTO;
            if (tocando >= fimDoReplay) tocando = -1f;
        } else if (marco >= 0 && gravados >= marco + DEPOIS) {
            // O começo não pode ser mais antigo que o que ainda está guardado (nem antes da luta).
            tocando = Math.max(0, Math.max(marco - ANTES, gravados - CAPACIDADE + 1));
            fimDoReplay = Math.min(marco + DEPOIS, gravados - 1);
            marco = -1;
        }
        for (int s = 0; s < 2; s++) {
            CombatFighter f = engine.fighter(s);
            boolean forte = false;
            if (f.attacking() && f.attack != ultimoGolpe[s] && f.attack.strength == AttackDefinition.Strength.SUPER) forte = true;
            ultimoGolpe[s] = f.attacking() ? f.attack : null;
            ComboSession sessao = engine.lastSession(s);
            if (sessao != null && sessao != ultimaSessao[s]) {
                ultimaSessao[s] = sessao;
                if (sessao.hitCount >= COMBO) forte = true;
            }
            if (f.ko() && !caido[s]) forte = true;
            caido[s] = f.ko();
            // Um replay de cada vez: o que acontece durante um fica sem.
            if (forte && marco < 0 && tocando < 0f) marco = gravados;
        }
    }

    /**
     * Desenha a tela do telão no retângulo (l, t, r, b). {@code leve}: sem os parceiros e sem as
     * linhas de TV (modo de efeitos leves).
     */
    void desenhar(Canvas c, SpriteFighterRenderer desenhista, float l, float t, float r, float b,
                  float chaoY, boolean leve, float tempo) {
        float w = r - l, h = b - t;
        if (w < 8f || h < 8f) return;
        c.save();
        c.clipRect(l, t, r, b);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setColor(replay() ? 0xFF1A1530 : 0xFF0C1428);
        c.drawRect(l, t, r, b, paint);
        // O chão da transmissão: uma faixa mais clara embaixo.
        paint.setColor(replay() ? 0xFF2A2140 : 0xFF16233F);
        c.drawRect(l, b - h * 0.16f, r, b, paint);
        int k = gravados == 0 ? -1 : replay() ? ((int)tocando) % CAPACIDADE : (gravados - 1) % CAPACIDADE;
        if (k >= 0 && quem[k][0] != null && quem[k][1] != null) {
            float cx = (x[k][0] + x[k][1]) * 0.5f;
            float alto = Math.max(quem[k][0].visualStandHeight, quem[k][1].visualStandHeight);
            float s = h * 0.66f / Math.max(120f, alto);
            float largura = Math.abs(x[k][0] - x[k][1]) + 260f;
            s = Math.min(s, w * 0.92f / largura);
            c.save();
            c.translate((l + r) * 0.5f, b - h * 0.08f);
            c.scale(s, s);
            c.translate(-cx, -chaoY);
            // Os parceiros atrás, os titulares na frente.
            if (!leve) {
                desenharLugar(c, desenhista, k, 2);
                desenharLugar(c, desenhista, k, 3);
            }
            desenharLugar(c, desenhista, k, 1);
            desenharLugar(c, desenhista, k, 0);
            c.restore();
        }
        if (!leve) {
            // Linhas de TV.
            paint.setColor(0x16FFFFFF);
            for (float yy = t + 1f; yy < b; yy += 4f) c.drawRect(l, yy, r, yy + 1f, paint);
        }
        // Selo no canto: AO VIVO (ponto vermelho piscando) ou REPLAY.
        float tam = Math.max(8f, h * 0.11f);
        paint.setTextSize(tam);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.LEFT);
        if (replay()) {
            paint.setColor(0xFFFFD54A);
            c.drawText("REPLAY", l + tam * 0.6f, t + tam * 1.3f, paint);
            paint.setColor(0xCCFFD54A);
            paint.setTextAlign(Paint.Align.RIGHT);
            c.drawText("▶▶ x0.5", r - tam * 0.6f, t + tam * 1.3f, paint);
        } else {
            if (((int)(tempo * 2f)) % 2 == 0) {
                paint.setColor(0xFFE53935);
                c.drawCircle(l + tam * 0.9f, t + tam * 0.95f, tam * 0.32f, paint);
            }
            paint.setColor(0xEEFFFFFF);
            c.drawText("AO VIVO", l + tam * 1.5f, t + tam * 1.3f, paint);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(false);
        c.restore();
    }

    private void desenharLugar(Canvas c, SpriteFighterRenderer desenhista, int k, int i) {
        CharacterDefinition ch = quem[k][i];
        if (ch == null || clipe[k][i] == null) return;
        desenhista.drawQuadro(c, ch, clipe[k][i], quadro[k][i], x[k][i], y[k][i], lado[k][i]);
    }
}
