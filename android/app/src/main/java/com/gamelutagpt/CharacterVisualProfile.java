package com.gamelutagpt;

import android.graphics.RectF;

/**
 * Visual registration for one fighter: the canonical authoring cell (frame size and root)
 * and the world scale every clip of the fighter is drawn at.
 *
 * Each atlas keeps its own packed cell and root (wide attacks, tall jumps); the renderer
 * places a frame by that atlas root, scaled by {@link #worldScale}. Clips never change
 * scale at runtime.
 */
final class CharacterVisualProfile {
    final String id;
    final int frameWidth;
    final int frameHeight;
    final float rootX;
    final float rootY;
    final float worldScale;

    CharacterVisualProfile(
        String id,
        int frameWidth,
        int frameHeight,
        float rootX,
        float rootY,
        float worldScale
    ) {
        if (frameWidth <= 0 || frameHeight <= 0 || worldScale <= 0f) {
            throw new IllegalArgumentException("Invalid visual profile");
        }
        this.id=id;
        this.frameWidth=frameWidth;
        this.frameHeight=frameHeight;
        this.rootX=rootX;
        this.rootY=rootY;
        this.worldScale=worldScale;
    }

    void place(RectF out,float worldX,float baseY) {
        float scale=worldScale;
        float left=worldX-rootX*scale;
        float top=baseY-rootY*scale;
        out.set(
            left,
            top,
            left+frameWidth*scale,
            top+frameHeight*scale
        );
    }
}
