package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Tela real de seleção TAG: painel central simétrico, 2 equipes independentes,
 * previews grandes e pequeno preview do Templo da Lua. Nada é um screenshot clicável.
 * Reaproveita o primeiro quadro IDLE dos sprites instalados (sem decodificar atlases inteiros).
 */
final class CharacterSelectRenderer {
    private static final int BLUE = 0xFF26A8FF;
    private static final int RED = 0xFFFF4151;
    private static final int WHITE = 0xFFF3F5FF;
    private final Context context;
    private final Map<String, Bitmap> portraits = new HashMap<>();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint img = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF box = new RectF();
    private final Rect src = new Rect();
    private final Path path = new Path();

    CharacterSelectRenderer(Context context) { this.context = context; }

    void draw(Canvas canvas, Paint unused, CharacterSelect select) {
        backdrop(canvas);
        drawHero(canvas, select, 0);
        drawHero(canvas, select, 1);
        drawHeading(canvas, select, 0);
        drawHeading(canvas, select, 1);
        drawGrid(canvas, select);
        drawTeams(canvas, select, 0);
        drawTeams(canvas, select, 1);
        drawStagePreview(canvas);
        drawBottomBar(canvas, select);
    }

    private void backdrop(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0f, 0f, 0f, Arena.VH,
            0xFF08142F, 0xFF070814, Shader.TileMode.CLAMP));
        c.drawColor(Color.BLACK);
        c.drawRect(0f, 0f, Arena.VW, Arena.VH, p);
        p.setShader(null);
        p.setColor(0x40278BFF);
        c.drawCircle(245f, 177f, 254f, p);
        p.setColor(0x40FF243E);
        c.drawCircle(1035f, 177f, 254f, p);
        p.setColor(0x40D0DAFF);
        c.drawCircle(640f, 61f, 57f, p);
        p.setColor(0x30081022);
        c.drawCircle(652f, 54f, 58f, p);
        for (int i = 0; i < 9; i++) {
            float x = i * 153f - 17f;
            p.setColor(0x44000713);
            c.drawRect(x, 0f, x + 22f, 658f, p);
            p.setColor(0x323F5481);
            c.drawRect(x + 17f, 0f, x + 20f, 658f, p);
        }
        for (int i = 0; i < 11; i++) {
            float x = 42f + i * 124f;
            p.setColor(0x778099BF);
            c.drawCircle(x, 80f + (i % 3) * 15f, 1.8f, p);
        }
        // duas auréolas em oposição: dragão azul e tigre vermelho no conceito
        drawAura(c, 235f, 215f, BLUE);
        drawAura(c, 1045f, 215f, RED);
        p.setColor(0xB7000711);
        c.drawRect(0f, 638f, 1280f, 720f, p);
        line(c, 0, 656, 1280, 656, 0xFF586A87, 2f);
    }

    private void drawAura(Canvas c, float cx, float cy, int color) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.4f);
        for (int i = 0; i < 4; i++) {
            p.setColor(withAlpha(color, 66 - i * 11));
            float rad = 154f + i * 19f;
            c.drawArc(cx - rad, cy - rad, cx + rad, cy + rad, 195f, 205f, false, p);
            c.drawArc(cx - rad, cy - rad, cx + rad, cy + rad, 17f, 131f, false, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawHero(Canvas c, CharacterSelect s, int side) {
        boolean left = side == 0;
        float x = left ? 12f : 947f;
        float w = 322f;
        int accent = left ? BLUE : RED;
        p.setShader(new LinearGradient(x, 190f, x + w, 450f,
            withAlpha(accent, 45), 0x00222233, Shader.TileMode.CLAMP));
        c.drawRect(x, 128f, x + w, 559f, p);
        p.setShader(null);
        Bitmap b = portrait(GeneratedCharacters.get(s.pick(side, 0)));
        if (b != null) {
            float scale = Math.min(360f / b.getWidth(), 430f / b.getHeight());
            float width = b.getWidth() * scale, height = b.getHeight() * scale;
            float centerX = left ? 178f : 1104f;
            float bottom = 552f;
            box.set(centerX - width / 2f, bottom - height, centerX + width / 2f, bottom);
            img.setAlpha(245);
            c.drawBitmap(b, null, box, img);
        }
        p.setColor(withAlpha(accent, 242));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        if (left) c.drawLine(0f, 365f, 0f, 651f, p);
        else c.drawLine(1279f, 365f, 1279f, 651f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawHeading(Canvas c, CharacterSelect s, int side) {
        boolean left = side == 0;
        int accent = left ? BLUE : RED;
        float x = left ? 12f : 1088f;
        p.setColor(withAlpha(accent, 205));
        c.drawRoundRect(x, 18f, x + 181f, 81f, 5f, 5f, p);
        p.setColor(0xFF090E22);
        c.drawRoundRect(x + 4f, 22f, x + 177f, 77f, 4f, 4f, p);
        text(c, left ? "P1" : "P2", left ? x + 90f : x + 91f, 66f, 48f,
             WHITE, Paint.Align.CENTER, true);
        String name = GeneratedCharacters.get(s.pick(side, 0)).displayName.toUpperCase();
        text(c, name, left ? 21f : 1257f, 113f, 22f, WHITE,
             left ? Paint.Align.LEFT : Paint.Align.RIGHT, true);
    }

    private void drawGrid(Canvas c, CharacterSelect s) {
        // Contorno rígido e simétrico: perspectiva baixa sem inclinação lateral.
        p.setColor(0xF00A142B);
        c.drawRoundRect(370f, 143f, 910f, 519f, 10f, 10f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.5f);
        p.setColor(0xFF778FBC);
        c.drawRoundRect(370f, 143f, 910f, 519f, 10f, 10f, p);
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < CharacterSelect.COLUMNS * CharacterSelect.ROWS; i++)
            drawTile(c, s, i);
    }

    private void drawTile(Canvas c, CharacterSelect s, int index) {
        float l = s.cardLeft(index), t = s.cardTop(index);
        float w = s.cardWidth(), h = s.cardHeight();
        boolean locked = index >= s.ids.length;
        String id = locked ? null : s.ids[index];
        box.set(l, t, l + w, t + h);
        p.setColor(locked ? 0xFF0B1020 : 0xFF18243C);
        c.drawRect(box, p);
        if (!locked) {
            Bitmap b = portrait(GeneratedCharacters.get(id));
            if (b != null) {
                float scale = Math.max((w - 8f) / b.getWidth(), (h - 11f) / b.getHeight());
                // Centralizar o rosto/corpo com recorte dentro de cada carta.
                float width = b.getWidth() * scale, height = b.getHeight() * scale;
                c.save();
                c.clipRect(l + 2f, t + 2f, l + w - 2f, t + h - 2f);
                img.setAlpha(255);
                c.drawBitmap(b, null, new RectF(l + (w - width) / 2f,
                    t + h - height + 12f, l + (w + width) / 2f, t + h + 12f), img);
                c.restore();
            }
            p.setShader(new LinearGradient(l, t + h - 31f, l, t + h,
                0x00000000, 0xE6080C1D, Shader.TileMode.CLAMP));
            c.drawRect(l, t + h - 35f, l + w, t + h, p);
            p.setShader(null);
            String name = GeneratedCharacters.get(id).displayName;
            if (name.length() > 11) name = name.substring(0, 11);
            text(c, name, l + w / 2f, t + h - 7f, 14f, WHITE, Paint.Align.CENTER, true);
            int p1 = s.order(0, id), p2 = s.order(1, id);
            if (p1 > 0) selectorCorner(c, l, t, w, h, BLUE, "P1 " + p1, true);
            if (p2 > 0) selectorCorner(c, l, t, w, h, RED, "P2 " + p2, false);
            int focusedOrder = s.order(s.focusSide(), id);
            if (focusedOrder == s.focusSlot() + 1) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(4f);
                p.setColor(s.focusSide() == 0 ? BLUE : RED);
                c.drawRect(l + 1, t + 1, l + w - 1, t + h - 1, p);
                p.setStyle(Paint.Style.FILL);
            }
        } else {
            // Slots futuros ficam visivelmente trancados, sem cartões fictícios.
            text(c, "◇", l + w / 2f, t + 69f, 44f, 0xFF75809A, Paint.Align.CENTER, false);
            text(c, "BLOQUEADO", l + w / 2f, t + 96f, 13f,
                0xFF7C87A4, Paint.Align.CENTER, false);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(0xFF4A6186);
        c.drawRect(box, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void selectorCorner(Canvas c, float l, float t, float w, float h,
                                int color, String label, boolean left) {
        float bx = left ? l + 2f : l + w - 48f;
        p.setColor(color);
        c.drawRect(bx, t + 2f, bx + 46f, t + 23f, p);
        text(c, label, bx + 23f, t + 18f, 13f, WHITE, Paint.Align.CENTER, true);
    }

    private void drawTeams(Canvas c, CharacterSelect s, int side) {
        int accent = side == 0 ? BLUE : RED;
        float x = s.slotLeft(side), y = CharacterSelect.TEAM_TOP;
        boolean focusedSide = s.focusSide() == side;
        text(c, "TEAM", x + CharacterSelect.TEAM_W / 2f, y - 14f, 22f,
             WHITE, Paint.Align.CENTER, true);
        for (int slot = 0; slot < 2; slot++) {
            float top = s.slotTop(slot);
            box.set(x, top, x + CharacterSelect.TEAM_W, top + CharacterSelect.SLOT_H);
            p.setColor(0xEE090E22);
            c.drawRoundRect(box, 8f, 8f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(focusedSide && s.focusSlot() == slot ? 4f : 2f);
            p.setColor(focusedSide && s.focusSlot() == slot ? accent : withAlpha(accent, 130));
            c.drawRoundRect(box, 8f, 8f, p);
            p.setStyle(Paint.Style.FILL);
            text(c, String.valueOf(slot + 1), x + 18f, top + 47f, 27f,
                 WHITE, Paint.Align.CENTER, true);
            Bitmap b = portrait(GeneratedCharacters.get(s.pick(side, slot)));
            if (b != null) {
                float scale = Math.min(62f / b.getWidth(), 67f / b.getHeight());
                float w = b.getWidth() * scale, h = b.getHeight() * scale;
                img.setAlpha(255);
                c.drawBitmap(b, null, new RectF(x + 39f, top + CharacterSelect.SLOT_H - 5f - h,
                     x + 39f + w, top + CharacterSelect.SLOT_H - 5f), img);
            }
            String label = GeneratedCharacters.get(s.pick(side, slot)).displayName;
            if (label.length() > 14) label = label.substring(0, 14);
            text(c, label, x + 155f, top + 36f, 17f, WHITE, Paint.Align.CENTER, true);
            text(c, slot == 0 ? "TITULAR" : "TAG", x + 154f, top + 60f, 15f,
                 accent, Paint.Align.CENTER, true);
        }
    }

    private void drawStagePreview(Canvas c) {
        float l = 352f, t = 527f, r = 928f, b = 640f;
        box.set(l, t, r, b);
        p.setShader(new LinearGradient(l, t, l, b, 0xFF19386D, 0xFF090C24, Shader.TileMode.CLAMP));
        c.drawRoundRect(box, 7f, 7f, p);
        p.setShader(null);
        p.setColor(0xFFE5E9FF);
        c.drawCircle(641f, 556f, 23f, p);
        p.setColor(0x6EFFFFFF);
        c.drawCircle(641f, 556f, 30f, p);
        // silhuetas de telhados orientais, pátio e pilares do Templo da Lua
        p.setColor(0xFF020B19);
        for (int j = 0; j < 5; j++) {
            float x = 391 + j * 107f, roof = 576 + (j % 2) * 6f;
            path.reset();path.moveTo(x - 38f, roof + 18f);
            path.lineTo(x + 4f, roof - 12f);path.lineTo(x + 45f, roof + 18f);
            path.close();c.drawPath(path, p);
            c.drawRect(x - 29f, roof + 17f, x + 38f, 625f, p);
        }
        p.setColor(0xFF49638E);
        c.drawRect(l + 8f, 624f, r - 8f, 628f, p);
        p.setColor(0xE6070B1B);
        c.drawRect(l + 2f, 603f, r - 2f, 638f, p);
        text(c, CharacterSelect.STAGE, 640f, 628f, 21f, WHITE, Paint.Align.CENTER, true);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.5f);
        p.setColor(0xFF5F93D2); c.drawRoundRect(box, 7f, 7f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawBottomBar(Canvas c, CharacterSelect s) {
        drawButton(c, CharacterSelect.BACK_LEFT, CharacterSelect.BACK_TOP,
             CharacterSelect.BACK_RIGHT, CharacterSelect.BACK_BOTTOM,
             0xFF7B89A6, "VOLTAR", true);
        drawButton(c, CharacterSelect.FIGHT_LEFT, CharacterSelect.FIGHT_TOP,
             CharacterSelect.FIGHT_RIGHT, CharacterSelect.FIGHT_BOTTOM,
             s.ready() ? 0xFF2DC7F7 : 0xFF6A7488, "LUTAR", s.ready());
        drawButton(c, CharacterSelect.RANDOM_LEFT, CharacterSelect.RANDOM_TOP,
             CharacterSelect.RANDOM_RIGHT, CharacterSelect.RANDOM_BOTTOM,
             0xFFA1ABCB, "ALEATÓRIO", true);
        text(c, "TOQUE NA EQUIPE PARA ESCOLHER O TITULAR OU TAG",
             640f, 140f, 16f, 0xFFB4C4E5, Paint.Align.CENTER, false);
    }

    private void drawButton(Canvas c, float l, float t, float r, float b, int accent,
                            String name, boolean active) {
        p.setColor(active ? withAlpha(accent, 56) : 0x80222935);
        c.drawRoundRect(l, t, r, b, 8f, 8f, p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2f);
        p.setColor(accent);
        c.drawRoundRect(l, t, r, b, 8f, 8f, p);
        p.setStyle(Paint.Style.FILL);
        text(c, name, (l + r) / 2f, (t + b) / 2f + 8f, 23f,
             active ? WHITE : 0xFFAAB2C4, Paint.Align.CENTER, true);
    }

    private void text(Canvas c, String value, float x, float y, float size, int color,
                      Paint.Align align, boolean bold) {
        p.setShader(null);
        p.setColor(color);
        p.setTextSize(size);
        p.setTextAlign(align);
        p.setTypeface(bold ? android.graphics.Typeface.create("sans-serif-condensed", 1) :
                android.graphics.Typeface.create("sans-serif", 0));
        p.setStyle(Paint.Style.FILL);
        c.drawText(value, x, y, p);
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float stroke) {
        p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);
        c.drawLine(x1, y1, x2, y2, p);
        p.setStyle(Paint.Style.FILL);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private Bitmap portrait(CharacterDefinition character) {
        if (portraits.containsKey(character.id)) return portraits.get(character.id);
        Bitmap cell = null;
        CharacterDefinition.Animation idle = character.animations.get(SpriteStates.IDLE);
        if (idle != null) {
            CharacterDefinition.Atlas a = idle.atlas;
            int frame = idle.frame(0f, 0f);
            int id = context.getResources().getIdentifier(a.resource, "drawable", context.getPackageName());
            if (id != 0) {
                try (InputStream in = context.getResources().openRawResource(id)) {
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(in, false);
                    int x = frame % a.columns * a.width, y = frame / a.columns * a.height;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inScaled = false;
                    cell = decoder.decodeRegion(new Rect(x, y, x + a.width, y + a.height), options);
                    decoder.recycle();
                    cell = crop(cell);
                } catch (IOException | RuntimeException ex) {
                    Log.w("GameLuta", "retrato de " + character.id + " indisponivel", ex);
                }
            }
        }
        portraits.put(character.id, cell);
        return cell;
    }

    private static Bitmap crop(Bitmap cell) {
        if (cell == null) return null;
        int w = cell.getWidth(), h = cell.getHeight();
        int[] pixels = new int[w * h];
        cell.getPixels(pixels, 0, w, 0, 0, w, h);
        int l = w, t = h, r = -1, b = -1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if ((pixels[y * w + x] >>> 24) > 16) {
                l = Math.min(l, x);r = Math.max(r, x);t = Math.min(t, y);b = Math.max(b, y);
            }
        }
        if (r < l) return cell;
        Bitmap out = Bitmap.createBitmap(cell, l, t, r - l + 1, b - t + 1);
        cell.recycle();
        return out;
    }
}
