package com.gamelutagpt;

/** Energy or Super projectile in flight; owned and moved by the CombatEngine. */
final class Projectile {
    float x;
    /** Position before the last advance, for the swept hit test. */
    float previousX;
    final float y;
    final float startX;
    final float range;
    final float speed;
    final float radius;
    final int damage;
    final int color;
    final int ownerIndex;
    final int direction;
    /** S or SUPER definition of the thrower: stun, hitstop and pushback of the hit. */
    final AttackDefinition attack;
    /** Desenho do S1 (nome de ProjetilVisual.Tipo), ou null para a bola de energia padrão. */
    final String visual;
    /** O chicote já esticou e volta para a mão: não acerta mais. */
    boolean returning;

    Projectile(
        float x,
        float y,
        float range,
        float speed,
        float radius,
        int damage,
        int color,
        int ownerIndex,
        int direction,
        AttackDefinition attack
    ) {
        this(x, y, range, speed, radius, damage, color, ownerIndex, direction, attack, null);
    }

    Projectile(
        float x,
        float y,
        float range,
        float speed,
        float radius,
        int damage,
        int color,
        int ownerIndex,
        int direction,
        AttackDefinition attack,
        String visual
    ) {
        this.x = x;
        this.previousX = x;
        this.y = y;
        this.startX = x;
        this.range = range;
        this.speed = speed;
        this.radius = radius;
        this.damage = damage;
        this.color = color;
        this.ownerIndex = ownerIndex;
        this.direction = direction;
        this.attack = attack;
        this.visual = visual;
    }

    /** Volta para a mão ao chegar no alcance (ou ao acertar), em vez de sumir. */
    boolean retracts() { return "CHICOTE".equals(visual); }
}
