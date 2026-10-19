package com.gamelutagpt;

import android.content.SharedPreferences;

/**
 * Quanto o aparelho desenha por quadro. Em aparelho fraco o que pesa é encher a tela (o cenário
 * cobre a tela várias vezes por quadro, camada sobre camada), então os níveis abaixo do ALTO
 * desenham menos pixels (a tela é renderizada menor e ampliada de graça pelo sistema/janela) e o
 * LEVE ainda tira os efeitos que só enfeitam (reflexos, neblina, pétalas, brilhos das luzes).
 *
 * <p>No AUTO o jogo mede a luta: se ela não sustenta {@link #FPS_MINIMO} quadros por segundo
 * por {@link #JANELAS_RUINS} janelas seguidas, desce um nível. O nível aprendido fica salvo, e o
 * próximo uso já começa nele. Escolher AUTO de novo na pausa recomeça do ALTO.
 */
final class QualidadeGrafica {
    /** AUTO decide sozinho; os outros fixam o nível. A ordem é a do ciclo na pausa. */
    enum Modo { AUTO, ALTO, MEDIO, LEVE }

    static final int ALTO = 0, MEDIO = 1, LEVE = 2;
    /** Fração da resolução da tela em que cada nível desenha. */
    static final float[] ESCALA = {1f, 0.8f, 0.6f};
    /** Rótulos para a pausa (traduzidos pela temporada). */
    static final String[] NOMES = {"ALTO", "MÉDIO", "LEVE"};
    static final String AUTO = "AUTO";
    /** Abaixo disso a luta está pesada demais para o nível atual. */
    static final float FPS_MINIMO = 50f;
    static final int JANELAS_RUINS = 2;
    /** Folgado (quase os 60) por tantas janelas seguidas (~30 s de luta): o AUTO sobe um nível. */
    static final float FPS_FOLGA = 56f;
    static final int JANELAS_PARA_SUBIR = 15;
    /** Só conta janela em que quase todo quadro foi luta (não menu, carregamento ou pausa). */
    static final float FRACAO_LUTA = 0.8f;

    private static final String CHAVE_MODO = "graficosModo", CHAVE_NIVEL_AUTO = "graficosNivelAuto";

    private final SharedPreferences prefs;
    private Modo modo = Modo.AUTO;
    /** O nível que o AUTO aprendeu para este aparelho. */
    private int nivelAuto = ALTO;
    private int ruins, boas, janelas, subiuNaJanela = -1;
    private boolean subidaFalhou;

    QualidadeGrafica(SharedPreferences prefs) {
        this.prefs = prefs;
        if (prefs != null) {
            try {
                modo = Modo.valueOf(prefs.getString(CHAVE_MODO, Modo.AUTO.name()));
            } catch (IllegalArgumentException e) {
                modo = Modo.AUTO;
            }
            nivelAuto = Math.max(ALTO, Math.min(LEVE, prefs.getInt(CHAVE_NIVEL_AUTO, ALTO)));
        }
        // PC: -Dgameluta.graficos=ALTO|MEDIO|LEVE|AUTO fixa o modo (para medir e comparar).
        String fixo = System.getProperty("gameluta.graficos");
        if (fixo != null) {
            try {
                modo = Modo.valueOf(fixo.toUpperCase(java.util.Locale.ROOT));
                if (modo == Modo.AUTO) nivelAuto = ALTO;
            } catch (IllegalArgumentException e) {
                // valor desconhecido: fica o salvo
            }
        }
    }

    Modo modo() { return modo; }

    /** O nível em uso agora. */
    int nivel() {
        switch (modo) {
            case ALTO: return ALTO;
            case MEDIO: return MEDIO;
            case LEVE: return LEVE;
            default: return nivelAuto;
        }
    }

    float escala() { return ESCALA[nivel()]; }

    /**
     * Teto da resolução em que o jogo desenha: a arte é feita para 1280×720, e acima de 1080
     * linhas não ganha nada na tela do celular, só custa (uma tela 1440p desenha 1,8× os pixels
     * de uma 1080p; 1600p, 2,2×). Celular bom tem tela grande e ficava "pesado" por isso.
     */
    static final int ALTURA_MAXIMA = 1080;

    /** Fração da tela em que desenhar: a do nível, aplicada sobre o teto de {@link #ALTURA_MAXIMA}. */
    static float escalaDaTela(float escalaDoNivel, int alturaDaTela) {
        float teto = alturaDaTela > ALTURA_MAXIMA ? ALTURA_MAXIMA / (float)alturaDaTela : 1f;
        return teto * escalaDoNivel;
    }

    /** Os efeitos que só enfeitam ficam de fora. */
    boolean efeitosLeves() { return nivel() == LEVE; }

    /** Próximo modo (AUTO → ALTO → MÉDIO → LEVE → AUTO). Voltar ao AUTO recomeça do ALTO. */
    void proximoModo(int direcao) {
        int n = Modo.values().length;
        modo = Modo.values()[((modo.ordinal() + (direcao >= 0 ? 1 : -1)) % n + n) % n];
        if (modo == Modo.AUTO) nivelAuto = ALTO;
        ruins = boas = 0;
        subiuNaJanela = -1;
        subidaFalhou = false;
        salvar();
    }

    /** "AUTO (MÉDIO)" ou o nível fixo, para a pausa. {@code t} traduz. */
    String rotulo(java.util.function.UnaryOperator<String> t) {
        String nivel = t.apply(NOMES[nivel()]);
        return modo == Modo.AUTO ? t.apply(AUTO) + " (" + nivel + ")" : nivel;
    }

    /**
     * Uma janela de medida fechou ({@link PerfilQuadros}). No AUTO, luta abaixo de
     * {@link #FPS_MINIMO} por {@link #JANELAS_RUINS} janelas seguidas desce um nível.
     * @return true se o nível mudou
     */
    boolean janela(float fps, float fracaoLuta) {
        if (modo != Modo.AUTO || fracaoLuta < FRACAO_LUTA) return false;
        janelas++;
        if (fps >= FPS_MINIMO) {
            ruins = 0;
            // Folgado por um tempo num nível abaixo do ALTO: tenta subir (o nível guardado podia
            // ser de um aperto passageiro ou de uma versão mais pesada, e ficava para sempre).
            boas = fps >= FPS_FOLGA ? boas + 1 : 0;
            if (boas >= JANELAS_PARA_SUBIR && nivelAuto > ALTO && !subidaFalhou) {
                nivelAuto--;
                boas = 0;
                subiuNaJanela = janelas;
                salvar();
                return true;
            }
            return false;
        }
        boas = 0;
        if (++ruins < JANELAS_RUINS || nivelAuto >= LEVE) return false;
        // Subiu e logo não aguentou: não sobe de novo nesta sessão (sem ficar indo e voltando).
        if (subiuNaJanela >= 0 && janelas - subiuNaJanela <= JANELAS_PARA_SUBIR) subidaFalhou = true;
        nivelAuto++;
        ruins = 0;
        salvar();
        return true;
    }

    private void salvar() {
        if (prefs == null) return;
        prefs.edit().putString(CHAVE_MODO, modo.name()).putInt(CHAVE_NIVEL_AUTO, nivelAuto).apply();
    }
}
