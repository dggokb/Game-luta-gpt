package com.gamelutagpt;

/**
 * Armadilha no chão (a caixa surpresa da p16): o projétil cai e fica parado; quando o pavio
 * acaba (o tempo depende do botão do especial) ela abre e a luva salta. A luva é um projétil
 * parado na frente da caixa durante os quadros ativos, para acertar pelo mesmo caminho dos
 * projéteis (defesa, pausa, combo); criada e contada pelo CombatEngine.
 */
final class Armadilha {
    final float x;
    final int ownerIndex;
    final int direction;
    final int color;
    final int damage;
    final AttackDefinition attack;
    final CharacterDefinition.Projectile data;
    /** Quadros até abrir (na mina, até armar); depois, quadros desde que abriu. */
    int fuse;
    int opened;
    /** Mina: quadros armada esperando o rival, e se ele já a disparou. */
    int waited;
    boolean triggered;

    Armadilha(float x, int ownerIndex, int direction, int color, int damage, AttackDefinition attack,
              CharacterDefinition.Projectile data, int fuse) {
        this.x = x;
        this.ownerIndex = ownerIndex;
        this.direction = direction;
        this.color = color;
        this.damage = damage;
        this.attack = attack;
        this.data = data;
        this.fuse = fuse;
    }

    boolean open() { return data.trapMine > 0 ? triggered : fuse <= 0; }

    /** Mina armada e fechada, esperando o rival. */
    boolean armed() { return data.trapMine > 0 && fuse <= 0 && !triggered; }

    /** Quanto da abertura já passou (0 fechada, 1 terminou). */
    float progress() { return open() ? opened / (float)Math.max(1, data.trapDuration) : 0f; }
}
