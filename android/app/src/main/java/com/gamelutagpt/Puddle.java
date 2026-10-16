package com.gamelutagpt;

/**
 * Poça de veneno no chão (a seringa da p11 ao cair): quem pisa nela sem estar envenenado
 * fica envenenado. Dura alguns segundos; criada e contada pelo CombatEngine.
 */
final class Puddle {
    final float x;
    final float halfWidth;
    final int ownerIndex;
    final int color;
    final int totalFrames;
    int framesLeft;
    final int poisonDamage;
    final int poisonFrames;

    Puddle(float x, float halfWidth, int ownerIndex, int color, int frames, int poisonDamage, int poisonFrames) {
        this.x = x;
        this.halfWidth = halfWidth;
        this.ownerIndex = ownerIndex;
        this.color = color;
        this.totalFrames = frames;
        this.framesLeft = frames;
        this.poisonDamage = poisonDamage;
        this.poisonFrames = poisonFrames;
    }

    /** Quanto da vida da poça ainda resta (1 recém-criada, 0 sumindo). */
    float life() { return framesLeft / (float)Math.max(1, totalFrames); }
}
