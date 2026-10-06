package com.gamelutagpt.stage;

/**
 * Medidas do mundo da luta que o cenário precisa conhecer. O jogo passa os
 * próprios valores (Arena e CameraRig); o motor não depende do jogo.
 */
public final class StageWorld {
    public static final float VW = 1280f;
    public static final float VH = 720f;

    /** Y do chão no plano dos lutadores. */
    public final float groundY;
    public final float worldWidth;
    /** Câmera "de repouso": é nela que o stage.json descreve onde cada camada aparece. */
    public final float restZoom;
    public final float restCenterX;
    /** Altura na tela (0..VH) em que o chão aparece na câmera de repouso. */
    public final float restGroundScreenY;

    public StageWorld(float groundY, float worldWidth, float restZoom, float restCenterX, float restGroundScreenY) {
        this.groundY = groundY;
        this.worldWidth = worldWidth;
        this.restZoom = restZoom;
        this.restCenterX = restCenterX;
        this.restGroundScreenY = restGroundScreenY;
    }

    /** Topo do mundo visível na câmera de repouso. */
    public float restTop() {
        return groundY - restGroundScreenY / restZoom;
    }

    /** Esquerda do mundo visível na câmera de repouso. */
    public float restLeft() {
        return restCenterX - VW / (2f * restZoom);
    }
}
