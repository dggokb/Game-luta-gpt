package com.gamelutagpt;

/** Energy or Super projectile in flight. */
final class Projectile {
    float x;
    final float y;
    final float startX;
    final float range;
    final float speed;
    final int damage;
    final int color;
    final int ownerIndex;
    final int direction;

    Projectile(
        float x,
        float y,
        float range,
        float speed,
        int damage,
        int color,
        int ownerIndex,
        int direction
    ) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.range = range;
        this.speed = speed;
        this.damage = damage;
        this.color = color;
        this.ownerIndex = ownerIndex;
        this.direction = direction;
    }
}
