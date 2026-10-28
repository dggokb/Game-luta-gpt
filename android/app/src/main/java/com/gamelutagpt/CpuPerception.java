package com.gamelutagpt;

import java.util.List;

/**
 * O que a CPU sabe do rival: só o que aparece na tela (posição, animação, pulo, projéteis) e
 * só depois do tempo de reação. Guarda os últimos quadros e entrega o de {@code delay} quadros
 * atrás; nunca o botão que o jogador apertou nem o quadro atual. Pure Java.
 */
final class CpuPerception {
    static final int HISTORY = 64;

    /** O rival como ele aparecia num quadro. */
    static final class Snapshot {
        int frame;
        float x, y, vx, vy;
        boolean grounded, ko;
        /** Agachado (na tela): o golpe alto pega. */
        boolean crouching;
        CombatFighter.Status status;
        /** Golpe na tela (null = nenhum) e em que quadro dele. */
        AttackDefinition attack;
        int attackFrame;
        CombatFighter.Outcome outcome;
        /** Muda a cada golpe novo do rival; e a cada pulo novo. */
        int attackSerial, jumpSerial;
        /** O rival está com a guarda quebrada (aberto); a contagem muda a cada quebra nova. */
        boolean guardBroken;
        int guardBreakSerial;
        /** Projétil do rival vindo na direção da CPU: quanto falta para chegar (MAX_VALUE = nenhum). */
        float projectileGap = Float.MAX_VALUE;
        float projectileSpeed;
        boolean projectileLow, projectileSuper;
        int projectileSerial;
        /** Onde a bola estava (centro), para onde vai (+1/-1) e o raio dela. */
        float projectileX, projectileRadius;
        int projectileDirection;

        /**
         * Quanto falta da frente da bola até o centro de quem está em {@code selfX} agora,
         * {@code elapsed} quadros depois do que ela viu (a bola seguiu andando). A própria
         * posição a CPU sabe na hora; só a bola é prevista. MAX_VALUE sem bola.
         */
        float projectileGapNow(float selfX, int elapsed) {
            if (projectileGap == Float.MAX_VALUE) return Float.MAX_VALUE;
            float x = projectileX + projectileDirection * projectileSpeed * CombatConfig.DT * elapsed;
            return (selfX - x) * projectileDirection - projectileRadius;
        }

        boolean attacking() { return attack != null; }

        /** O golpe na tela ainda vai bater (preparação ou ativo). */
        boolean threatening() {
            return attack != null && attackFrame < attack.startupFrames + attack.activeFrames;
        }

        /** Quadros de recuperação que ainda faltavam ao golpe neste quadro (0 = acabando). */
        int recoveryLeft() {
            return attack == null ? 0 : Math.max(0, attack.totalFrames - attackFrame);
        }
    }

    private final Snapshot[] ring = new Snapshot[HISTORY];
    private int count, last = -1, lastFrame = Integer.MIN_VALUE;
    private int attackSerial, jumpSerial, projectileSerial, guardBreakSerial;
    private Projectile lastProjectile;

    CpuPerception() {
        for (int i = 0; i < HISTORY; i++) ring[i] = new Snapshot();
    }

    void reset() {
        count = 0;
        last = -1;
        lastFrame = Integer.MIN_VALUE;
        lastProjectile = null;
    }

    /** Anota o quadro atual do rival de {@code self} (uma vez por quadro do motor). */
    void record(CombatEngine engine, int self) {
        int frame = engine.frame();
        if (frame == lastFrame) return;
        if (frame < lastFrame) reset();   // luta nova (outro motor): o que viu antes não vale
        lastFrame = frame;
        CombatFighter me = engine.fighter(self);
        CombatFighter t = engine.fighter(1 - self);
        Snapshot prev = count > 0 ? ring[last] : null;
        last = (last + 1) % HISTORY;
        count = Math.min(HISTORY, count + 1);
        Snapshot s = ring[last];
        s.frame = frame;
        s.x = t.x; s.y = t.y; s.vx = t.vx; s.vy = t.vy;
        s.grounded = t.grounded;
        s.crouching = t.grounded && t.crouching;
        s.ko = t.ko();
        s.status = t.status;
        s.attack = t.attacking() ? t.attack : null;
        s.attackFrame = t.attackFrame;
        s.outcome = t.outcome;
        if (s.attack != null && (prev == null || prev.attack != s.attack || prev.attackFrame > s.attackFrame)) {
            attackSerial++;
        }
        if (!s.grounded && prev != null && prev.grounded) jumpSerial++;
        s.attackSerial = attackSerial;
        s.jumpSerial = jumpSerial;
        s.guardBroken = t.guardBroken;
        if (s.guardBroken && (prev == null || !prev.guardBroken)) guardBreakSerial++;
        s.guardBreakSerial = guardBreakSerial;
        nearestProjectile(engine, me, t.index, s);
    }

    /**
     * Quanto o corpo do desenho de {@code me} passa da meia largura do corpo do lado {@code lado}
     * (+1 direita, -1 esquerda do mundo); 0 sem caixas medidas.
     */
    static float alemDaCaixa(CombatFighter me, int lado) {
        float[] corpo = CombatEngine.corpoAtual(me);
        if (corpo == null) return 0f;
        boolean frente = lado == me.facing;
        float longe = 0f;
        for (int k = 0; k + 3 < corpo.length; k += 4) longe = Math.max(longe, frente ? corpo[k + 1] : -corpo[k]);
        return Math.max(0f, longe - me.body().halfWidth);
    }

    private void nearestProjectile(CombatEngine engine, CombatFighter me, int owner, Snapshot s) {
        s.projectileGap = Float.MAX_VALUE;
        Projectile best = null;
        float bestAlem = 0f;
        for (List<Projectile> list : java.util.Arrays.asList(engine.energyProjectiles, engine.superProjectiles)) {
            for (Projectile p : list) {
                if (p.ownerIndex != owner || p.returning) continue;
                // Até o corpo do desenho (a base aberta, a guarda): o que passa da caixa antiga
                // encosta antes, e os prazos de pular e defender contam até ele.
                float alem = alemDaCaixa(me, -p.direction);
                float gap = (me.x - p.x) * p.direction - p.radius - alem;
                if (gap < -40f || gap >= s.projectileGap) continue;
                s.projectileGap = gap;
                best = p;
                bestAlem = alem;
            }
        }
        if (best != null) {
            if (best != lastProjectile) projectileSerial++;
            s.projectileSpeed = best.speed;
            s.projectileX = best.x;
            s.projectileDirection = best.direction;
            // O raio leva junto o corpo além da caixa: a previsão (projectileGapNow) conta até ele.
            s.projectileRadius = best.radius + bestAlem;
            s.projectileLow = best.attack.low;
            s.projectileSuper = best.superKind();
        }
        lastProjectile = best;
        s.projectileSerial = projectileSerial;
    }

    /**
     * O rival de {@code delay} quadros atrás, ou null se a luta ainda não durou isso (a CPU
     * não sabe nada do que ainda não viu).
     */
    Snapshot seen(int delay) {
        if (count == 0 || delay >= count) return null;
        int index = ((last - delay) % HISTORY + HISTORY) % HISTORY;
        return ring[index];
    }

    /** O quadro mais antigo que ela guarda (null se nenhum). */
    Snapshot oldest() {
        if (count == 0) return null;
        return ring[((last - count + 1) % HISTORY + HISTORY) % HISTORY];
    }

    /** Quadro mais recente anotado. */
    int lastFrame() { return lastFrame; }
}
