package com.gamelutagpt;

/**
 * Fight camera: zooms out to keep both fighters in frame, keeps the ground near the
 * same screen height and follows a Super Jump vertically. Pure math, unit-testable.
 */
final class CameraRig {
    static final float CAMERA_ZOOM = 1.24f;
    static final float CAMERA_MIN_ZOOM = 0.78f;
    static final float CAMERA_FIGHTER_MARGIN_X = 520f;
    static final float CAMERA_GROUND_SCREEN_Y = 552f;
    static final float CAMERA_TOP_MARGIN_SCREEN = 64f;
    static final float CAMERA_BOTTOM_MARGIN_SCREEN = 45f;
    static final float GROUND_CAMERA_TOP = 72f;

    float x = 700f;
    float top = GROUND_CAMERA_TOP;
    float zoom = CAMERA_ZOOM;

    /**
     * @param highestFighterTop world y of the highest opaque pixel of either fighter
     * @param superJumpActive while true, height does not widen the framing
     */
    void update(float dt, float playerX, float opponentX, float highestFighterTop, boolean superJumpActive) {
        float separationX = Math.abs(playerX - opponentX);
        float requiredWorldWidth =
            separationX + CAMERA_FIGHTER_MARGIN_X;

        float targetZoomX =
            Arena.VW / Math.max(Arena.VW / CAMERA_ZOOM, requiredWorldWidth);

        float heightAboveGround =
            Math.max(1f, Arena.GROUND_Y - highestFighterTop);

        // Tenta reservar margem visual acima do lutador mais alto.
        float usableVerticalScreen =
            CAMERA_GROUND_SCREEN_Y - CAMERA_TOP_MARGIN_SCREEN;
        float targetZoomY =
            usableVerticalScreen / heightAboveGround;

        // No Super Jump, a altura nao abre o enquadramento:
        // a camera sobe/desce junto com o lutador e o zoom continua
        // respondendo somente a separacao horizontal.
        float targetZoom = Arena.clamp(
            superJumpActive
                ? targetZoomX
                : Math.min(targetZoomX, targetZoomY),
            CAMERA_MIN_ZOOM,
            CAMERA_ZOOM
        );

        float zoomFollow =
            1f - (float)Math.pow(0.0025f, dt);
        zoom +=
            (targetZoom - zoom) * zoomFollow;

        float visibleWorldWidth = Arena.VW / zoom;
        float halfVisible = visibleWorldWidth * 0.5f;

        float fightCenterX = (playerX + opponentX) * 0.5f;
        float targetCameraX = Arena.clamp(
            fightCenterX,
            halfVisible,
            Arena.WORLD_WIDTH - halfVisible
        );

        float horizontalFollow =
            1f - (float)Math.pow(0.0015f, dt);
        x +=
            (targetCameraX - x) * horizontalFollow;

        // Mantém o chão praticamente na mesma altura da tela enquanto possível.
        float baseTop =
            Arena.GROUND_Y - CAMERA_GROUND_SCREEN_Y / zoom;
        float topMarginWorld =
            CAMERA_TOP_MARGIN_SCREEN / zoom;

        float targetCameraTop = baseTop;
        if (
            highestFighterTop <
            targetCameraTop + topMarginWorld
        ) {
            targetCameraTop =
                highestFighterTop - topMarginWorld;
        }

        // Fora do Super Jump, preserva uma faixa do chao na tela.
        // Durante o Super Jump essa trava e removida para a camera poder
        // acompanhar verticalmente sem precisar afastar o zoom.
        if (!superJumpActive) {
            float lowestAllowedTop =
                Arena.GROUND_Y -
                (Arena.VH - CAMERA_BOTTOM_MARGIN_SCREEN) / zoom;
            targetCameraTop = Math.max(
                targetCameraTop,
                lowestAllowedTop
            );
        }
        targetCameraTop = Arena.clamp(
            targetCameraTop,
            Arena.WORLD_TOP,
            GROUND_CAMERA_TOP
        );

        float verticalFollow =
            1f - (float)Math.pow(0.0007f, dt);
        top +=
            (targetCameraTop - top) * verticalFollow;
    }
}
