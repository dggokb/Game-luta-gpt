package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.Locale;

/**
 * Combat debug view (DEBUG button). World space: hurtboxes (green), pushboxes (yellow),
 * active hitboxes (red), extra move hurtboxes (blue) and projectile radii. Screen space:
 * engine frame, the V2 state of each fighter, buffer, combo and the frame data table of
 * the player's character with advantage on hit/block, so links are calibrated from data.
 */
final class DebugOverlay {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final String[] BINDINGS = {"L", "M", "H", "2L", "2M", "2H", "jL", "jM", "jH", "S", "SUPER"};

    void drawWorld(Canvas c, CombatEngine engine) {
        paint.setStrokeWidth(2f);
        for (int i = 0; i < 2; i++) {
            CombatFighter f = engine.fighter(i);
            CharacterDefinition.Body body = f.body();
            float hurtTop = f.hurtTop();
            stroke(c, Color.argb(220, 80, 230, 120), f.x - body.halfWidth, hurtTop, f.x + body.halfWidth, f.y);
            stroke(c, Color.argb(200, 255, 214, 92), f.x - body.pushHalfWidth, f.y - body.pushHeight,
                f.x + body.pushHalfWidth, f.y);
            if (!f.attacking()) continue;
            int frame = Math.max(0, f.attackFrame);
            AttackDefinition a = f.attack;
            for (int b = 0; b < a.hurtboxCount(); b++) {
                AttackDefinition.Box box = a.hurtbox(b);
                if (box.activeAt(frame)) drawBox(c, f, box, Color.argb(200, 90, 160, 255), false);
            }
            if (a.kind != AttackDefinition.Kind.NORMAL || !a.isActive(frame)) continue;
            for (int b = 0; b < a.hitboxCount(); b++) {
                AttackDefinition.Box box = a.hitbox(b);
                boolean used = (f.hitboxMask & (1L << b)) != 0;
                if (box.activeAt(frame)) drawBox(c, f, box, used ? Color.argb(120, 160, 60, 60) : Color.argb(150, 255, 60, 80), true);
            }
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(220, 255, 60, 80));
        for (Projectile p : engine.energyProjectiles) c.drawCircle(p.x, p.y, p.radius, paint);
        for (Projectile p : engine.superProjectiles) c.drawCircle(p.x, p.y, p.radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBox(Canvas c, CombatFighter f, AttackDefinition.Box box, int color, boolean fill) {
        float left = f.facing > 0 ? f.x + box.x0 : f.x - box.x1;
        float right = f.facing > 0 ? f.x + box.x1 : f.x - box.x0;
        if (fill) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            c.drawRect(left, f.y - box.y1, right, f.y - box.y0, paint);
        } else {
            stroke(c, color, left, f.y - box.y1, right, f.y - box.y0);
        }
    }

    private void stroke(Canvas c, int color, float left, float top, float right, float bottom) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        c.drawRect(left, top, right, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    void drawPanel(Canvas c, CombatEngine engine) {
        float left = 572f, top = 28f, right = 936f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 8, 12, 22));
        c.drawRoundRect(left, top, right, 420f, 12f, 12f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(13f);
        paint.setFakeBoldText(true);
        float x = left + 10f, y = top + 18f, line = 15f;
        c.drawText(String.format(Locale.US, "FRAME %d  •  %d FPS fixo%s", engine.frame(), CombatConfig.FPS,
            engine.superFreezeActive() ? "  •  SUPER FREEZE" : ""), x, y, paint);
        paint.setFakeBoldText(false);
        y += line;
        for (int i = 0; i < 2; i++) {
            CombatFighter f = engine.fighter(i);
            paint.setColor(i == 0 ? Color.rgb(255, 214, 92) : Color.rgb(255, 120, 130));
            c.drawText((i == 0 ? "P1 " : "CPU ") + fighterLine(f), x, y, paint);
            y += line;
            paint.setColor(Color.argb(220, 210, 220, 235));
            c.drawText("   buffer " + f.buffer.entries() + "  guard " + f.anticipatedGuard
                + "  x " + Math.round(f.x) + "  y " + Math.round(f.y), x, y, paint);
            y += line;
        }
        for (int defender = 1; defender >= 0; defender--) {
            ComboSession s = engine.session(defender);
            if (s == null) continue;
            paint.setColor(Color.rgb(140, 230, 255));
            c.drawText(String.format(Locale.US, "COMBO %s: %d hits  dano %d  escala %d%%  decay -%d  juggle %d  %df",
                defender == 1 ? "P1" : "CPU", s.hitCount, s.comboDamage, s.damageScale / 10, s.hitstunDecay,
                s.juggleCount, s.comboDuration), x, y, paint);
            y += line;
        }
        y += 4f;
        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        CharacterDefinition character = engine.fighter(0).character();
        c.drawText("FRAME DATA " + character.displayName + "  (s/a/r  hit  block  → rotas)", x, y, paint);
        paint.setFakeBoldText(false);
        y += line;
        paint.setTextSize(12f);
        for (String id : BINDINGS) {
            AttackDefinition a = character.attack(id);
            if (a == null) continue;
            String advantage = a.kind == AttackDefinition.Kind.NORMAL
                ? String.format(Locale.US, "%+d  %+d", a.advantageOnHit(), a.advantageOnBlock())
                : "proj";
            paint.setColor(Color.argb(230, 225, 230, 240));
            c.drawText(String.format(Locale.US, "%-5s %2d/%d/%-2d  %s  → %s", id, a.startupFrames, a.activeFrames,
                a.recoveryFrames, advantage, String.join(" ", a.cancelInto())), x, y, paint);
            y += 14f;
        }
    }

    private static String fighterLine(CombatFighter f) {
        StringBuilder b = new StringBuilder(f.stateName());
        if (f.attacking()) {
            b.append(' ').append(f.attack.id).append(" f").append(Math.max(0, f.attackFrame) + 1)
                .append('/').append(f.attack.totalFrames).append(' ').append(f.outcome.name());
            AttackDefinition.Window w = CancelSystem.windowFor(f);
            if (w != null) b.append(CancelSystem.windowOpen(f) ? " CANCEL" : " janela " + w);
        }
        if (f.stunLeft > 0) b.append("  stun ").append(f.stunLeft).append('/').append(f.stunTotal);
        if (f.hitstop > 0) b.append("  hitstop ").append(f.hitstop);
        return b.toString();
    }
}
