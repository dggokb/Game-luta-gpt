package com.gamelutagpt;

import com.gamelutagpt.motion.Canal;
import com.gamelutagpt.motion.Mola;

/**
 * Peso dos golpes na tela (só apresentação; o motor não lê nada daqui): um soco de câmera
 * no ponto do impacto, o cenário escurecendo por alguns quadros nos golpes fortes (os
 * "impact frames" do Garou e do FighterZ), e no KO um clarão branco com câmera lenta.
 * Anda no relógio de 60 Hz da luta.
 */
final class ImpactFeel {
    /** Zoom extra por tamanho de golpe (LIGHT, MEDIUM, HEAVY, SPECIAL), punish counter e KO. */
    static final float[] ZOOM = {0f, 0.012f, 0.025f, 0.05f};
    static final float PUNISH_ZOOM = 0.07f, KO_ZOOM = 0.09f;
    /**
     * Mola da volta do zoom (motion-core): o soco entra seco e volta firme ao enquadramento
     * normal, sem balançar.
     */
    static final Mola ZOOM_SPRING = new Mola(3.2f, 0.75f);
    /**
     * Tranco da tela no sentido do golpe (pixels da tela) por tamanho (LIGHT, MEDIUM, HEAVY,
     * SPECIAL): vai e volta por mola, firme.
     */
    static final float[] KICK = {0f, 4f, 9f, 14f};
    static final Mola KICK_SPRING = new Mola(4.5f, 0.75f);
    /** Cenário escurecido (alfa 0..255) e quantos quadros dura, por tamanho de golpe. */
    static final int[] DARK = {0, 0, 70, 100};
    static final int[] DARK_FRAMES = {0, 0, 3, 4};
    static final int PUNISH_DARK = 140, PUNISH_DARK_FRAMES = 6;
    /** KO: clarão branco (alfa) e a câmera lenta (quadros da luta e a velocidade deles). */
    static final int KO_FLASH = 210;
    static final int KO_SLOW_FRAMES = 42;
    static final float KO_SLOW_SCALE = 0.35f;
    /** Golpe final do Ultra: câmera lenta mais curta que a do KO. */
    static final int ULTRA_SLOW_FRAMES = 30;
    static final float ULTRA_SLOW_SCALE = 0.4f;

    /**
     * Impact frame (a técnica dos animes): por 2 quadros a cena vira silhueta, com linhas de
     * velocidade saindo do impacto, e 1 quadro de cores invertidas antes de voltar ao normal.
     * RED: fundo vermelho e silhuetas pretas com contorno branco (punish counter, fim de combo
     * longo). VOID: tudo preto e silhuetas brancas (KO, Super e Ultra).
     */
    static final int NONE = 0, RED = 1, VOID = 2;
    static final int STYLE_FRAMES = 2, NEGATIVE_FRAMES = 1;
    /** Quadros do impact frame forte (a pancada que fecha o golpe): o dobro e mais um. */
    static final int STRONG_STYLE_FRAMES = 5;
    /** Quadros sem outro impact frame do mesmo peso (a tela não fica piscando num combo). */
    static final int IMPACT_COOLDOWN = 30;

    /**
     * Câmera de cinema (FighterZ): no lançador, no golpe que joga o rival no chão/parede e no
     * KO, a câmera corta para um close inclinado no impacto, com o fundo escurecendo e a luta
     * em câmera lenta, e volta com corte seco. O zoom total é CINEMA_ZOOM × CINEMA_OVERSCAN; a
     * sobra do overscan cobre os cantos que a inclinação descobre.
     */
    static final float CINEMA_ZOOM = 1.45f, CINEMA_OVERSCAN = 1.25f, CINEMA_TILT = 8f;
    /** Close mais aberto para o H que joga na parede (~1,5× no total, em vez de ~1,8×). */
    static final float CINEMA_ZOOM_WALL = 1.2f;
    static final int CINEMA_FRAMES = 12;
    static final float CINEMA_SCALE = 0.45f;
    static final int CINEMA_DARK_FROM = 60, CINEMA_DARK_TO = 170;
    /** Entre dois closes: num combo só o primeiro golpe grande ganha a câmera. */
    static final int CINEMA_COOLDOWN = 90;

    /** Zoom extra do soco de câmera, no relógio de passos da luta. */
    private final Canal zoom = new Canal(ZOOM_SPRING);
    /** Tranco da tela no sentido do golpe (pixels da tela). */
    private final Canal kick = new Canal(KICK_SPRING);
    /** Passos entre um tranco e o próximo (o tempo de ele ir e voltar). */
    static final int KICK_GAP = 20;
    private int lastKick = -KICK_GAP;
    private int frame;
    private float focusX, focusY;
    private int dark, darkFrames, darkTotal;
    private float flash;
    private int slowFrames;
    private float slowScale = KO_SLOW_SCALE;
    private int impactStyle = NONE, impactLeft, impactCooldown, impactSeed;
    private float impactX, impactY;
    private int cinemaFrames, cinemaCooldown;
    private float cinemaTilt, cinemaZoom = CINEMA_ZOOM;

    void clear() {
        zoom.zerar();
        kick.zerar();
        frame = 0;
        lastKick = -KICK_GAP;
        dark = darkFrames = darkTotal = 0;
        flash = 0f;
        slowFrames = 0;
        impactStyle = NONE;
        impactLeft = impactCooldown = 0;
        cinemaFrames = cinemaCooldown = 0;
    }

    /**
     * Close de cinema no ponto (x, y). {@code direction}: para onde o golpe empurra (+1 direita);
     * a câmera inclina a favor dele. O KO passa por cima do intervalo.
     */
    void cinema(float x, float y, int direction, boolean knockout) {
        cinema(x, y, direction, knockout, CINEMA_ZOOM);
    }

    /** Close com o zoom pedido (antes do overscan): {@link #CINEMA_ZOOM} ou {@link #CINEMA_ZOOM_WALL}. */
    void cinema(float x, float y, int direction, boolean knockout, float zoomLevel) {
        if (cinemaCooldown > 0 && !knockout) return;
        cinemaZoom = zoomLevel;
        cinemaFrames = CINEMA_FRAMES;
        cinemaCooldown = CINEMA_COOLDOWN;
        cinemaTilt = (direction >= 0 ? 1f : -1f) * CINEMA_TILT;
        focusX = x;
        focusY = y;
    }

    boolean cinema() { return cinemaFrames > 0; }

    /** Inclinação da câmera em graus (0 fora do close). */
    float tilt() { return cinemaFrames > 0 ? cinemaTilt : 0f; }

    /**
     * Pede um impact frame no ponto (x, y) do mundo. Durante o intervalo, só um estilo mais
     * forte (VOID sobre RED) toma o lugar.
     */
    void impactFrame(int style, float x, float y) {
        impactFrame(style, x, y, false);
    }

    /** {@code every}: ignora o intervalo (cada pancada do Super ganha o seu, a torta e a marreta). */
    void impactFrame(int style, float x, float y, boolean every) {
        impactFrame(style, x, y, every, false);
    }

    /** {@code strong}: o impact frame dura {@link #STRONG_STYLE_FRAMES} (a pancada que fecha o golpe). */
    void impactFrame(int style, float x, float y, boolean every, boolean strong) {
        if (style == NONE) return;
        if (!every && !strong && impactCooldown > 0 && style <= impactStyle) return;
        impactStyle = style;
        // +1: o passo da luta que pediu ainda conta um quadro antes de desenhar.
        impactLeft = (strong ? STRONG_STYLE_FRAMES : STYLE_FRAMES) + NEGATIVE_FRAMES + 1;
        impactCooldown = IMPACT_COOLDOWN;
        impactX = x;
        impactY = y;
        impactSeed++;
    }

    /**
     * Um golpe que acertou (defesas não chamam). {@code size} é a classe do {@link ImpactEffects};
     * {@code punish}: punish counter; {@code ko}: o golpe que nocauteou.
     */
    void onHit(float x, float y, int size, boolean punish, boolean ko) {
        size = Math.max(ImpactEffects.LIGHT, Math.min(ImpactEffects.SPECIAL, size));
        float kick = ko ? KO_ZOOM : punish ? PUNISH_ZOOM : ZOOM[size];
        if (kick > 0f && kick >= zoom.valor(frame)) {
            // O soco novo toma o lugar do que sobrava (entra seco no tamanho dele).
            zoom.zerar();
            zoom.degrau(frame, kick, ZOOM_SPRING);
            focusX = x;
            focusY = y;
        }
        int alpha = punish || ko ? PUNISH_DARK : DARK[size];
        int frames = punish || ko ? PUNISH_DARK_FRAMES : DARK_FRAMES[size];
        if (alpha > 0 && alpha >= darkAlpha()) {
            dark = alpha;
            darkFrames = darkTotal = frames;
        }
        if (ko) {
            flash = KO_FLASH;
            slow(KO_SLOW_FRAMES, KO_SLOW_SCALE);
        }
    }

    /** O tranco da tela de um golpe que acertou: {@code direction} é para onde ele empurra (+1 direita). */
    void kick(int direction, int size) {
        size = Math.max(ImpactEffects.LIGHT, Math.min(ImpactEffects.SPECIAL, size));
        // Um tranco por vez: outro só depois que o anterior voltou (sem chacoalhar num combo).
        if (KICK[size] > 0f && frame - lastKick >= KICK_GAP) {
            kick.impulso(frame, (direction >= 0 ? 1f : -1f) * KICK[size], KICK_SPRING);
            lastKick = frame;
        }
    }

    /** Deslocamento da tela agora (pixels da tela; positivo para a direita). */
    float kickX() { return kick.valor(frame); }

    /** Câmera lenta por {@code frames} quadros da luta a {@code scale}; a mais longa vale. */
    void slow(int frames, float scale) {
        if (frames < slowFrames) return;
        slowFrames = frames;
        slowScale = scale;
    }

    /** Um passo da luta. */
    void step() {
        zoom.limpar(++frame);
        kick.limpar(frame);
        if (darkFrames > 0) darkFrames--;
        flash *= 0.88f;
        if (flash < 2f) flash = 0f;
        if (slowFrames > 0) slowFrames--;
        if (impactLeft > 0) impactLeft--;
        if (impactCooldown > 0) impactCooldown--;
        if (cinemaCooldown > 0) cinemaCooldown--;
        if (cinemaFrames > 0 && --cinemaFrames == 0) zoom.zerar();  // corte seco de volta
    }

    /** Estilo do impact frame que aparece agora ({@link #NONE} fora dele e no quadro negativo). */
    int impactStyle() { return impactLeft > NEGATIVE_FRAMES ? impactStyle : NONE; }

    /** O quadro de cores invertidas logo depois da silhueta. */
    boolean negative() { return impactLeft > 0 && impactLeft <= NEGATIVE_FRAMES; }

    float impactX() { return impactX; }
    float impactY() { return impactY; }
    /** Muda a cada impact frame: as linhas de velocidade não saem sempre iguais. */
    int impactSeed() { return impactSeed; }

    /** Multiplicador do zoom da câmera (1 = normal). */
    float zoom() { return cinemaFrames > 0 ? cinemaZoom : 1f + zoom.valor(frame); }

    /** Ponto do mundo que o soco de câmera segura no mesmo lugar da tela. */
    float focusX() { return focusX; }
    float focusY() { return focusY; }

    /** Alfa do escurecimento do cenário agora (atrás dos lutadores). */
    int darkAlpha() {
        if (cinemaFrames > 0) {
            float k = 1f - cinemaFrames / (float)CINEMA_FRAMES;
            return Math.round(CINEMA_DARK_FROM + (CINEMA_DARK_TO - CINEMA_DARK_FROM) * k);
        }
        if (darkFrames <= 0 || darkTotal <= 0) return 0;
        return Math.round(dark * darkFrames / (float)darkTotal);
    }

    /** Alfa do clarão branco do KO (na frente de tudo no mundo). */
    int flashAlpha() { return Math.round(flash); }

    /** Velocidade do relógio da luta: abaixo de 1 na câmera lenta do KO. */
    float timeScale() {
        float scale = slowFrames > 0 ? slowScale : 1f;
        return cinemaFrames > 0 ? Math.min(scale, CINEMA_SCALE) : scale;
    }

    /** Quadros que o golpe leva para voltar à guarda depois de conectar (a "volta seca"). */
    static final int SNAP_BACK_FRAMES = 3;

    /**
     * Tempo do clipe de um golpe normal no quadro {@code attackFrame}. Até o fim da janela
     * ativa, o mesmo de sempre; depois, se o golpe conectou (acerto ou defesa), a volta do
     * desenho acontece em {@link #SNAP_BACK_FRAMES} quadros e o resto da recuperação fica na
     * última pose (a guarda). Golpe que errou volta devagar, como antes: o erro continua
     * parecendo arriscado. Golpes de vários acertos ficam como estão. Só o desenho muda.
     */
    static float snapBackTime(CharacterDefinition.Move move, int attackFrame, boolean connected, float step) {
        float plain = move.animationTime((attackFrame + 0.5f) * step);
        AttackDefinition a = move.attack;
        int activeEnd = a.startupFrames + a.activeFrames;
        if (!connected || a.maxHits > 1 || attackFrame < activeEnd || move.animation == null) return plain;
        float from = move.animationTime(activeEnd * step);
        float end = move.animation.duration - 0.0001f;
        float k = Math.min(1f, (attackFrame - activeEnd + 1) / (float)SNAP_BACK_FRAMES);
        return Math.max(Math.min(plain, end), Math.min(end, from + (end - from) * k));
    }

    /**
     * Tremida de quem apanhou durante o congelamento do acerto (hitstop), alternando de lado a
     * cada quadro; menor na defesa. 0 fora dele.
     */
    static float hitShake(CombatFighter f, int frame) {
        if (f.hitstop <= 0) return 0f;
        float amplitude;
        if (f.framesSinceHit == 0) amplitude = Math.min(9f, 2.5f + f.hitstop * 0.7f);
        else if (f.framesSinceBlock == 0) amplitude = 2.5f;
        else return 0f;
        return (frame % 2 == 0 ? 1f : -1f) * amplitude;
    }
}
