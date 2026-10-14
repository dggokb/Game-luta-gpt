package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import java.util.List;
import java.util.Locale;

/**
 * DEBUG > AJUSTAR CAIXAS: escolhe o personagem e o item (corpo, golpe, magia, super) e ajusta
 * as caixas de combate sobre a animação, quadro a quadro, no tempo do golpe (60 quadros por
 * segundo). Verde: corpo que leva golpe; amarelo: empurrão; vermelho: acerto do golpe (cheio
 * nos quadros ativos); círculo: saída da magia. Cada toque salva sozinho no aparelho; SALVAR
 * NO DRIVE exporta para gravar no character.json (tools/sprites/caixas.py).
 */
final class TelaAjusteCaixas {
    private static final float ROOT_X = 300f, ROOT_Y = 600f, ZOOM = 1.7f, STAGE_RIGHT = 715f;
    private static final float HOLD = 0.5f;              // s parado no fim antes de repetir
    private static final float LIST_LEFT = 730f, LIST_RIGHT = 1268f, LIST_TOP = 84f, ROW_H = 40f;
    private static final int ROWS = 7;
    private static final float FIELDS_TOP = 372f, FIELD_H = 46f;
    private static final float[] SPEEDS = {.25f, .5f, 1f};
    private static final int GREEN = Color.argb(230, 80, 230, 120), YELLOW = Color.argb(220, 255, 214, 92),
        RED = Color.argb(230, 255, 60, 80), RED_FILL = Color.argb(110, 255, 60, 80);

    private final SpriteAtlasCache atlases;
    private final TelaAjusteTamanho.Host host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DashPathEffect dash = new DashPathEffect(new float[] {6f, 5f}, 0f);
    private boolean open, paused;
    private int character, selected, scroll, speed;
    private CharacterDefinition definition;
    private SpriteFighterRenderer renderer;
    private List<AjusteCaixas.Item> itens;
    /** Quadro do golpe (60 por segundo), ou tempo da animação do corpo. */
    private float frame, time;
    private String message = "";
    private float messageTime;

    TelaAjusteCaixas(SpriteAtlasCache atlases, TelaAjusteTamanho.Host host) {
        this.atlases = atlases;
        this.host = host;
    }

    boolean isOpen() { return open; }

    void open(String characterId) {
        character = 0;
        for (int i = 0; i < GeneratedCharacters.SELECTABLE.length; i++)
            if (GeneratedCharacters.SELECTABLE[i].equals(characterId)) character = i;
        open = true;
        show(character);
    }

    void close() {
        open = false;
        renderer = null;
        host.fechar();
    }

    private void show(int index) {
        character = (index + GeneratedCharacters.SELECTABLE.length) % GeneratedCharacters.SELECTABLE.length;
        definition = GeneratedCharacters.get(GeneratedCharacters.SELECTABLE[character]);
        host.manterAtlas(definition);
        renderer = new SpriteFighterRenderer(atlases, definition);
        itens = AjusteCaixas.itens(definition);
        selected = scroll = 0;
        restart();
    }

    private void select(int index) {
        selected = Math.max(0, Math.min(itens.size() - 1, index));
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + ROWS) scroll = selected - ROWS + 1;
        restart();
    }

    private void restart() { frame = time = 0f; }

    void say(String text) { message = text; messageTime = 4f; }

    private AjusteCaixas.Item item() { return itens.get(selected); }

    void update(float dt) {
        if (!open) return;
        messageTime = Math.max(0f, messageTime - dt);
        if (paused) return;
        float step = dt * SPEEDS[speed];
        time += step;
        AttackDefinition a = item().attack;
        if (a != null) {
            frame += step * CombatConfig.FPS;
            if (frame >= a.totalFrames + HOLD * CombatConfig.FPS) frame = 0f;
        }
    }

    private int frameIndex() {
        AttackDefinition a = item().attack;
        return a == null ? 0 : Math.min(a.totalFrames - 1, (int)frame);
    }

    void key(int keyCode) {
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_DPAD_UP: select(selected - 1); break;
            case android.view.KeyEvent.KEYCODE_DPAD_DOWN: select(selected + 1); break;
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT: stepFrame(-1); break;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: stepFrame(+1); break;
            case android.view.KeyEvent.KEYCODE_BUTTON_L1: show(character - 1); break;
            case android.view.KeyEvent.KEYCODE_BUTTON_R1: show(character + 1); break;
            case android.view.KeyEvent.KEYCODE_BACK:
            case android.view.KeyEvent.KEYCODE_BUTTON_B: close(); break;
            default: break;
        }
    }

    private void stepFrame(int delta) {
        AttackDefinition a = item().attack;
        paused = true;
        if (a != null) frame = Math.max(0, Math.min(a.totalFrames - 1, frameIndex() + delta));
    }

    // ------------------------------------------------------------ toque

    void tap(float x, float y) {
        if (!open) return;
        if (in(x, y, 20, 14, 70, 62)) { show(character - 1); return; }
        if (in(x, y, 330, 14, 380, 62)) { show(character + 1); return; }
        if (in(x, y, 1130, 14, 1268, 62)) { close(); return; }
        if (in(x, y, LIST_RIGHT - 60, LIST_TOP, LIST_RIGHT, LIST_TOP + ROW_H * 2)) { scroll(-ROWS / 2); return; }
        if (in(x, y, LIST_RIGHT - 60, LIST_TOP + ROW_H * (ROWS - 2), LIST_RIGHT, LIST_TOP + ROW_H * ROWS)) { scroll(ROWS / 2); return; }
        if (in(x, y, LIST_LEFT, LIST_TOP, LIST_RIGHT - 66, LIST_TOP + ROW_H * ROWS)) {
            int row = scroll + (int)((y - LIST_TOP) / ROW_H);
            if (row < itens.size()) select(row);
            return;
        }
        List<AjusteCaixas.Campo> campos = item().campos;
        for (int i = 0; i < campos.size(); i++) {
            float top = FIELDS_TOP + i * FIELD_H;
            if (y < top + 4 || y > top + FIELD_H - 4) continue;
            AjusteCaixas.Campo f = campos.get(i);
            if (in(x, y, 1000, top, 1050, top + FIELD_H)) AjusteCaixas.adjust(definition, f, -5);
            else if (in(x, y, 1056, top, 1100, top + FIELD_H)) AjusteCaixas.adjust(definition, f, -1);
            else if (in(x, y, 1106, top, 1150, top + FIELD_H)) AjusteCaixas.adjust(definition, f, +1);
            else if (in(x, y, 1156, top, 1206, top + FIELD_H)) AjusteCaixas.adjust(definition, f, +5);
            else if (in(x, y, 1212, top, 1268, top + FIELD_H)) AjusteCaixas.set(definition, f, f.base);
            return;
        }
        if (in(x, y, 730, 600, 980, 654)) { AjusteCaixas.reset(definition); say("Caixas do " + definition.displayName + " voltaram ao original"); return; }
        if (in(x, y, 988, 600, 1268, 654)) { host.salvar(); return; }
        if (in(x, y, 10, 660, 110, 706)) { paused = !paused; return; }
        if (in(x, y, 116, 660, 206, 706)) { speed = (speed + 1) % SPEEDS.length; return; }
        if (in(x, y, 212, 660, 322, 706)) { stepFrame(-1); return; }
        if (in(x, y, 328, 660, 438, 706)) { stepFrame(+1); return; }
        if (in(x, y, 444, 660, 600, 706)) {
            AttackDefinition a = item().attack;
            if (a != null) { frame = a.startupFrames; paused = true; }
        }
    }

    private void scroll(int rows) {
        scroll = Math.max(0, Math.min(Math.max(0, itens.size() - ROWS), scroll + rows));
    }

    private static boolean in(float x, float y, float l, float t, float r, float b) {
        return x >= l && x <= r && y >= t && y <= b;
    }

    // ------------------------------------------------------------ desenho

    void draw(Canvas c) {
        if (!open) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
        paint.setPathEffect(null);
        c.drawColor(Color.rgb(16, 20, 30));
        drawStage(c);
        drawHeader(c);
        drawList(c);
        drawFields(c);
        drawFooter(c);
        if (messageTime > 0f) {
            paint.setColor(Color.argb(230, 20, 90, 60));
            c.drawRoundRect(400, 84, 720, 124, 8, 8, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(15f);
            c.drawText(message, 410, 109, paint);
        }
    }

    private float v(String path) { return AjusteCaixas.value(definition, itens, path); }

    private void drawStage(Canvas c) {
        AjusteCaixas.Item it = item();
        paint.setColor(Color.rgb(24, 30, 44));
        c.drawRect(0, 70, STAGE_RIGHT, 650, paint);
        paint.setColor(Color.rgb(90, 100, 120));
        paint.setStrokeWidth(2f);
        c.drawLine(10, ROOT_Y, STAGE_RIGHT - 10, ROOT_Y, paint);
        AttackDefinition a = it.attack;
        int f = frameIndex();
        c.save();
        c.clipRect(0, 70, STAGE_RIGHT, 650);
        c.translate(ROOT_X, ROOT_Y);
        c.scale(ZOOM, ZOOM);
        CharacterDefinition.Animation anim = it.animation;
        if (anim != null) {
            float t;
            if (a != null) t = anim.timeFor(f / (float)CombatConfig.FPS, a.totalFrames / (float)CombatConfig.FPS);
            else if (it.tipo == AjusteCaixas.Item.Tipo.AGACHADO) t = Math.max(0f, anim.duration - .0001f);
            else t = anim.loop && anim.duration > 0f ? time % anim.duration : 0f;
            renderer.pose(anim.id, t, 0f);
            renderer.draw(c, 0f, 0f, 1, false, false);
        }
        boolean crouch = it.tipo == AjusteCaixas.Item.Tipo.AGACHADO || it.id.startsWith("2");
        float half = v("fighter.body.halfWidth");
        float height = v(crouch ? "fighter.body.crouchHeight" : "fighter.body.standHeight");
        box(c, GREEN, -half, -height, half, 0f, false);
        float push = v("fighter.body.pushHalfWidth");
        box(c, YELLOW, -push, -v("fighter.body.pushHeight"), push, 0f, false);
        if (it.tipo == AjusteCaixas.Item.Tipo.GOLPE) {
            float reach = AjusteCaixas.value(definition, it.campos.get(0));
            float hit = AjusteCaixas.value(definition, it.campos.get(1));
            boolean active = a.isActive(f);
            box(c, active ? RED_FILL : RED, 0f, -(hit + CombatRules.VERTICAL_SLACK), reach,
                -(hit - CombatRules.VERTICAL_SLACK), active ? false : true);
            if (active) box(c, RED, 0f, -(hit + CombatRules.VERTICAL_SLACK), reach, -(hit - CombatRules.VERTICAL_SLACK), false);
        } else if (it.tipo == AjusteCaixas.Item.Tipo.MAGIA || it.tipo == AjusteCaixas.Item.Tipo.SUPER) {
            boolean fired = a != null && f >= a.startupFrames;
            float radius = it.tipo == AjusteCaixas.Item.Tipo.SUPER ? 58f : 24f;
            float x = AjusteCaixas.value(definition, it.campos.get(0));
            spawn(c, x, AjusteCaixas.value(definition, it.campos.get(1)), radius, fired, 230);
            spawn(c, x, AjusteCaixas.value(definition, it.campos.get(2)), radius, false, 90);
        }
        c.restore();
        paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(13f);
        paint.setColor(Color.LTGRAY);
        c.drawText("verde: corpo que leva golpe   amarelo: empurrão   vermelho: acerto   círculo: saída da magia",
            12, 92, paint);
        if (a != null) {
            String phase = f < a.startupFrames ? "inicial" : a.isActive(f) ? "ATIVO (acerta)" : "recuperação";
            paint.setTextSize(16f);
            paint.setColor(a.isActive(f) ? Color.rgb(255, 120, 130) : Color.WHITE);
            c.drawText(String.format(Locale.US, "quadro %d/%d  %s   impacto no quadro %d", f + 1, a.totalFrames,
                phase, a.startupFrames + 1), 12, 116, paint);
        }
    }

    private void box(Canvas c, int color, float l, float t, float r, float b, boolean dashed) {
        paint.setColor(color);
        boolean fill = Color.alpha(color) < 150;
        paint.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
        paint.setStrokeWidth(2f / ZOOM);
        paint.setPathEffect(dashed ? dash : null);
        c.drawRect(l, t, r, b, paint);
        paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void spawn(Canvas c, float x, float y, float radius, boolean filled, int alpha) {
        paint.setColor(Color.argb(alpha, 255, 90, 200));
        paint.setStrokeWidth(2f / ZOOM);
        paint.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE);
        if (filled) paint.setColor(Color.argb(120, 255, 90, 200));
        c.drawCircle(x, -y, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        c.drawLine(x - 8, -y, x + 8, -y, paint);
        c.drawLine(x, -y - 8, x, -y + 8, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawHeader(Canvas c) {
        button(c, 20, 14, 70, 62, "◀", 26f, false);
        button(c, 330, 14, 380, 62, "▶", 26f, false);
        paint.setColor(Color.WHITE);
        paint.setTextSize(24f);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(definition.displayName.toUpperCase(), 200, 47, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.rgb(245, 225, 170));
        paint.setTextSize(22f);
        c.drawText("AJUSTAR CAIXAS", 420, 38, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(13f);
        paint.setColor(Color.LTGRAY);
        c.drawText("hitbox, hurtbox e empurrão; salva sozinho; SALVAR NO DRIVE leva para o jogo", 420, 58, paint);
        button(c, 1130, 14, 1268, 62, "VOLTAR", 18f, false);
    }

    private void drawList(Canvas c) {
        for (int i = 0; i < ROWS && scroll + i < itens.size(); i++) {
            AjusteCaixas.Item it = itens.get(scroll + i);
            float top = LIST_TOP + i * ROW_H;
            boolean changed = false;
            for (AjusteCaixas.Campo f : it.campos) changed |= AjusteCaixas.value(definition, f) != f.base;
            paint.setColor(scroll + i == selected ? Color.rgb(50, 90, 140) : Color.rgb(30, 38, 54));
            c.drawRoundRect(LIST_LEFT, top + 2, LIST_RIGHT - 66, top + ROW_H - 2, 6, 6, paint);
            paint.setColor(changed ? Color.rgb(255, 200, 80) : Color.WHITE);
            paint.setFakeBoldText(changed);
            paint.setTextSize(16f);
            c.drawText(it.label + (changed ? "  •" : ""), LIST_LEFT + 10, top + 26, paint);
            paint.setFakeBoldText(false);
        }
        button(c, LIST_RIGHT - 60, LIST_TOP + 2, LIST_RIGHT, LIST_TOP + ROW_H * 2 - 2, "▲", 22f, scroll == 0);
        button(c, LIST_RIGHT - 60, LIST_TOP + ROW_H * (ROWS - 2) + 2, LIST_RIGHT, LIST_TOP + ROW_H * ROWS - 2, "▼", 22f,
            scroll + ROWS >= itens.size());
        paint.setColor(Color.LTGRAY);
        paint.setTextSize(13f);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText((selected + 1) + "/" + itens.size(), LIST_RIGHT - 30, LIST_TOP + ROW_H * ROWS / 2f + 5, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawFields(Canvas c) {
        List<AjusteCaixas.Campo> campos = item().campos;
        for (int i = 0; i < campos.size(); i++) {
            AjusteCaixas.Campo f = campos.get(i);
            float top = FIELDS_TOP + i * FIELD_H;
            float value = AjusteCaixas.value(definition, f);
            boolean changed = value != f.base;
            paint.setColor(Color.rgb(24, 30, 44));
            c.drawRoundRect(LIST_LEFT, top + 2, LIST_RIGHT, top + FIELD_H - 2, 6, 6, paint);
            paint.setColor(Color.LTGRAY);
            paint.setTextSize(13f);
            c.drawText(f.label, LIST_LEFT + 10, top + 18, paint);
            paint.setColor(changed ? Color.rgb(255, 200, 80) : Color.WHITE);
            paint.setTextSize(18f);
            paint.setFakeBoldText(true);
            c.drawText(Math.round(value) + (changed ? "   (era " + Math.round(f.base) + ")" : ""), LIST_LEFT + 10, top + 39, paint);
            paint.setFakeBoldText(false);
            button(c, 1000, top + 5, 1050, top + FIELD_H - 5, "-10", 15f, false);
            button(c, 1056, top + 5, 1100, top + FIELD_H - 5, "-2", 15f, false);
            button(c, 1106, top + 5, 1150, top + FIELD_H - 5, "+2", 15f, false);
            button(c, 1156, top + 5, 1206, top + FIELD_H - 5, "+10", 15f, false);
            button(c, 1212, top + 5, 1268, top + FIELD_H - 5, "ORIG", 13f, !changed);
        }
    }

    private void drawFooter(Canvas c) {
        int changed = AjusteCaixas.changed(definition);
        button(c, 730, 600, 980, 654, "ZERAR " + definition.displayName.toUpperCase(), 16f, changed == 0);
        paint.setColor(Color.rgb(30, 110, 70));
        c.drawRoundRect(988, 600, 1268, 654, 8, 8, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText("SALVAR NO DRIVE", 1128, 626, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(12f);
        c.drawText("todos os personagens ajustados", 1128, 645, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        button(c, 10, 660, 110, 706, paused ? "TOCAR" : "PAUSA", 15f, false);
        button(c, 116, 660, 206, 706, "VEL " + (SPEEDS[speed] == 1f ? "1x" : SPEEDS[speed] == .5f ? "½x" : "¼x"), 15f, false);
        boolean attack = item().attack != null;
        button(c, 212, 660, 322, 706, "◀ QUADRO", 14f, !attack);
        button(c, 328, 660, 438, 706, "QUADRO ▶", 14f, !attack);
        button(c, 444, 660, 600, 706, "IR AO IMPACTO", 14f, !attack);
    }

    private void button(Canvas c, float l, float t, float r, float b, String text, float size, boolean dim) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(dim ? Color.rgb(36, 42, 54) : Color.rgb(65, 80, 100));
        c.drawRoundRect(l, t, r, b, 8, 8, paint);
        paint.setColor(dim ? Color.GRAY : Color.WHITE);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(text, (l + r) / 2f, (t + b) / 2f + size * .36f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}
