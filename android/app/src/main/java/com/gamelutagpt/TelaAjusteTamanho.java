package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.List;

/**
 * DEBUG > AJUSTAR TAMANHOS: escolhe o personagem, a folha de sprite (as animações que a
 * usam) e ajusta o tamanho no olho. A animação toca em loop com o IDLE em transparência
 * atrás, raiz no mesmo ponto, como na troca de animação no jogo. Cada toque salva sozinho
 * no aparelho; SALVAR NO DRIVE exporta para gravar nas folhas (tools/sprites/regua.py).
 */
final class TelaAjusteTamanho {
    /** O que a tela pede ao jogo. */
    interface Host {
        /** Mantém decodificadas só as folhas da luta e deste personagem. */
        void manterAtlas(CharacterDefinition extra);
        void salvar();
        void fechar();
    }

    private static final float VW = Arena.VW, VH = Arena.VH;
    private static final float ZOOM = 1.85f, ROOT_X = 360f, ROOT_Y = 640f;
    private static final float HOLD = 0.45f;            // s parado no fim de um golpe antes de repetir
    private static final float WALK_SPEED = 300f;       // px/s, CombatConfig: andar baseado em distância
    private static final float LIST_LEFT = 730f, LIST_RIGHT = 1268f, LIST_TOP = 84f, ROW_H = 42f;
    private static final int ROWS = 10;
    private static final float[] SPEEDS = {1f, .5f, .25f};

    private final SpriteAtlasCache atlases;
    private final Host host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private boolean open;
    private int character;
    private CharacterDefinition definition;
    private SpriteFighterRenderer renderer;
    private List<AjusteTamanho.Folha> folhas;
    private int selected, scroll, speed;
    private boolean ghost = true, paused;
    private float time, distance;
    private String message = "";
    private float messageTime;

    TelaAjusteTamanho(SpriteAtlasCache atlases, Host host) {
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
        folhas = AjusteTamanho.folhas(definition);
        selected = scroll = 0;
        restart();
    }

    private void select(int index) {
        selected = Math.max(0, Math.min(folhas.size() - 1, index));
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + ROWS) scroll = selected - ROWS + 1;
        restart();
    }

    private void restart() { time = distance = 0f; }

    void say(String text) { message = text; messageTime = 4f; }

    void update(float dt) {
        if (!open) return;
        messageTime = Math.max(0f, messageTime - dt);
        if (paused) return;
        float step = dt * SPEEDS[speed];
        time += step;
        distance += WALK_SPEED * step;
    }

    /** Animação mostrada para a folha escolhida: a primeira que a usa. */
    private CharacterDefinition.Animation animation() {
        return definition.animations.get(folhas.get(selected).animacoes.get(0));
    }

    /** Teclas do controle: ↑↓ escolhe, ←→ ajusta 1%, B/voltar fecha. */
    void key(int keyCode) {
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_DPAD_UP: select(selected - 1); break;
            case android.view.KeyEvent.KEYCODE_DPAD_DOWN: select(selected + 1); break;
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT: AjusteTamanho.adjust(folhas.get(selected).atlas, -1); break;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: AjusteTamanho.adjust(folhas.get(selected).atlas, +1); break;
            case android.view.KeyEvent.KEYCODE_BUTTON_L1: show(character - 1); break;
            case android.view.KeyEvent.KEYCODE_BUTTON_R1: show(character + 1); break;
            case android.view.KeyEvent.KEYCODE_BACK:
            case android.view.KeyEvent.KEYCODE_BUTTON_B: close(); break;
            default: break;
        }
    }

    // ------------------------------------------------------------ toque

    void tap(float x, float y) {
        if (!open) return;
        CharacterDefinition.Atlas atlas = folhas.get(selected).atlas;
        if (in(x, y, 20, 14, 70, 62)) { show(character - 1); return; }
        if (in(x, y, 330, 14, 380, 62)) { show(character + 1); return; }
        if (in(x, y, 1130, 14, 1268, 62)) { close(); return; }
        // Lista: setas e linhas.
        if (in(x, y, LIST_RIGHT - 60, LIST_TOP - 2, LIST_RIGHT, LIST_TOP + ROW_H * 2)) { scroll(-ROWS / 2); return; }
        if (in(x, y, LIST_RIGHT - 60, LIST_TOP + ROW_H * (ROWS - 2), LIST_RIGHT, LIST_TOP + ROW_H * ROWS)) { scroll(ROWS / 2); return; }
        if (in(x, y, LIST_LEFT, LIST_TOP, LIST_RIGHT - 66, LIST_TOP + ROW_H * ROWS)) {
            int row = scroll + (int)((y - LIST_TOP) / ROW_H);
            if (row < folhas.size()) select(row);
            return;
        }
        // Ajuste da folha escolhida.
        float by = 532f;
        if (in(x, y, 730, by, 800, by + 54)) { AjusteTamanho.adjust(atlas, -5); return; }
        if (in(x, y, 808, by, 878, by + 54)) { AjusteTamanho.adjust(atlas, -1); return; }
        if (in(x, y, 1006, by, 1076, by + 54)) { AjusteTamanho.adjust(atlas, +1); return; }
        if (in(x, y, 1084, by, 1154, by + 54)) { AjusteTamanho.adjust(atlas, +5); return; }
        if (in(x, y, 1162, by, 1268, by + 54)) { AjusteTamanho.set(atlas, 1f); return; }
        // Rodapé.
        if (in(x, y, 730, 610, 980, 664)) { AjusteTamanho.reset(definition); say("Tamanhos do " + definition.displayName + " voltaram a 100%"); return; }
        if (in(x, y, 988, 610, 1268, 664)) { host.salvar(); return; }
        // Reprodução.
        if (in(x, y, 20, 660, 140, 706)) { paused = !paused; return; }
        if (in(x, y, 150, 660, 270, 706)) { speed = (speed + 1) % SPEEDS.length; return; }
        if (in(x, y, 280, 660, 460, 706)) { ghost = !ghost; return; }
        if (in(x, y, 470, 660, 590, 706)) { restart(); }
    }

    private void scroll(int rows) {
        scroll = Math.max(0, Math.min(Math.max(0, folhas.size() - ROWS), scroll + rows));
    }

    private static boolean in(float x, float y, float l, float t, float rr, float b) {
        return x >= l && x <= rr && y >= t && y <= b;
    }

    // ------------------------------------------------------------ desenho

    void draw(Canvas c) {
        if (!open) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
        c.drawColor(Color.rgb(16, 20, 30));
        drawStage(c);
        drawHeader(c);
        drawList(c);
        drawAdjust(c);
        drawFooter(c);
        if (messageTime > 0f) {
            paint.setColor(Color.argb(230, 20, 90, 60));
            c.drawRoundRect(400, 84, 720, 124, 8, 8, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(15f);
            fit(c, message, 410, 109, 300);
        }
    }

    private void drawStage(Canvas c) {
        paint.setColor(Color.rgb(24, 30, 44));
        c.drawRect(0, 70, 715, 650, paint);
        paint.setColor(Color.rgb(90, 100, 120));
        paint.setStrokeWidth(2f);
        c.drawLine(10, ROOT_Y, 705, ROOT_Y, paint);
        CharacterDefinition.Animation anim = animation();
        c.save();
        c.clipRect(0, 70, 715, 650);
        c.translate(ROOT_X, ROOT_Y);
        c.scale(ZOOM, ZOOM);
        if (ghost) {
            renderer.pose(SpriteStates.IDLE, 0f, 0f);
            int layer = c.saveLayerAlpha(-ROOT_X / ZOOM, -ROOT_Y / ZOOM, (715 - ROOT_X) / ZOOM, (650 - ROOT_Y) / ZOOM, 95);
            renderer.draw(c, 0f, 0f, 1, false, false);
            c.restoreToCount(layer);
        }
        float t = time;
        if (anim.distancePerFrame <= 0f) {
            float cycle = anim.loop ? anim.duration : anim.duration + HOLD;
            t = cycle > 0f ? time % cycle : 0f;
            if (!anim.loop) t = Math.min(t, Math.max(0f, anim.duration - .0001f));
        }
        renderer.pose(anim.id, t, distance);
        renderer.draw(c, 0f, 0f, 1, false, false);
        c.restore();
        paint.setColor(Color.LTGRAY);
        paint.setTextSize(14f);
        c.drawText(ghost ? "IDLE em transparência atrás, raiz no mesmo ponto" : "IDLE atrás desligado", 20, 96, paint);
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
        c.drawText("AJUSTAR TAMANHOS", 420, 38, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(13f);
        paint.setColor(Color.LTGRAY);
        c.drawText("salva sozinho no aparelho; SALVAR NO DRIVE leva para o jogo", 420, 58, paint);
        button(c, 1130, 14, 1268, 62, "VOLTAR", 18f, false);
    }

    private void drawList(Canvas c) {
        for (int i = 0; i < ROWS && scroll + i < folhas.size(); i++) {
            AjusteTamanho.Folha f = folhas.get(scroll + i);
            float top = LIST_TOP + i * ROW_H;
            boolean sel = scroll + i == selected;
            int pct = AjusteTamanho.percent(f.atlas);
            paint.setColor(sel ? Color.rgb(50, 90, 140) : Color.rgb(30, 38, 54));
            c.drawRoundRect(LIST_LEFT, top + 2, LIST_RIGHT - 66, top + ROW_H - 2, 6, 6, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(16f);
            fit(c, f.nome(), LIST_LEFT + 10, top + 27, LIST_RIGHT - 66 - LIST_LEFT - 90);
            paint.setColor(pct == 100 ? Color.LTGRAY : Color.rgb(255, 200, 80));
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setFakeBoldText(pct != 100);
            c.drawText(pct + "%", LIST_RIGHT - 76, top + 27, paint);
            paint.setFakeBoldText(false);
            paint.setTextAlign(Paint.Align.LEFT);
        }
        boolean up = scroll > 0, down = scroll + ROWS < folhas.size();
        button(c, LIST_RIGHT - 60, LIST_TOP + 2, LIST_RIGHT, LIST_TOP + ROW_H * 2 - 2, "▲", 22f, !up);
        button(c, LIST_RIGHT - 60, LIST_TOP + ROW_H * (ROWS - 2) + 2, LIST_RIGHT, LIST_TOP + ROW_H * ROWS - 2, "▼", 22f, !down);
        paint.setColor(Color.LTGRAY);
        paint.setTextSize(13f);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText((selected + 1) + "/" + folhas.size(), LIST_RIGHT - 30, LIST_TOP + ROW_H * ROWS / 2f + 5, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawAdjust(Canvas c) {
        float by = 532f;
        button(c, 730, by, 800, by + 54, "-5", 22f, false);
        button(c, 808, by, 878, by + 54, "-1", 22f, false);
        paint.setColor(Color.rgb(10, 14, 22));
        c.drawRoundRect(886, by, 998, by + 54, 8, 8, paint);
        int pct = AjusteTamanho.percent(folhas.get(selected).atlas);
        paint.setColor(pct == 100 ? Color.WHITE : Color.rgb(255, 200, 80));
        paint.setTextSize(28f);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(pct + "%", 942, by + 38, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
        button(c, 1006, by, 1076, by + 54, "+1", 22f, false);
        button(c, 1084, by, 1154, by + 54, "+5", 22f, false);
        button(c, 1162, by, 1268, by + 54, "100%", 18f, pct == 100);
    }

    private void drawFooter(Canvas c) {
        int changed = AjusteTamanho.changed(definition);
        button(c, 730, 610, 980, 664, "ZERAR " + definition.displayName.toUpperCase(), 16f, changed == 0);
        paint.setColor(Color.rgb(30, 110, 70));
        c.drawRoundRect(988, 610, 1268, 664, 8, 8, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText("SALVAR NO DRIVE", 1128, 636, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(12f);
        c.drawText("todos os personagens ajustados", 1128, 655, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        button(c, 20, 660, 140, 706, paused ? "TOCAR" : "PAUSA", 15f, false);
        button(c, 150, 660, 270, 706, "VEL " + (SPEEDS[speed] == 1f ? "1x" : SPEEDS[speed] == .5f ? "½x" : "¼x"), 15f, false);
        button(c, 280, 660, 460, 706, ghost ? "IDLE ATRÁS: SIM" : "IDLE ATRÁS: NÃO", 15f, false);
        button(c, 470, 660, 590, 706, "DO INÍCIO", 15f, false);
    }

    private void button(Canvas c, float l, float t, float rr, float b, String text, float size, boolean dim) {
        paint.setColor(dim ? Color.rgb(36, 42, 54) : Color.rgb(65, 80, 100));
        c.drawRoundRect(l, t, rr, b, 8, 8, paint);
        paint.setColor(dim ? Color.GRAY : Color.WHITE);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(text, (l + rr) / 2f, (t + b) / 2f + size * .36f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** Texto cortado com reticências para caber na largura. */
    private void fit(Canvas c, String text, float x, float y, float width) {
        if (paint.measureText(text) <= width) { c.drawText(text, x, y, paint); return; }
        int n = paint.breakText(text, true, width - paint.measureText("…"), null);
        c.drawText(text.substring(0, Math.max(0, n)) + "…", x, y, paint);
    }
}
