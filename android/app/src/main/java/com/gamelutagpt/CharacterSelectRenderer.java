package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws {@link CharacterSelect}: a card per character with the first IDLE frame. Only that
 * one cell is decoded per character (a whole pack is ~135 MB), once, and kept.
 */
final class CharacterSelectRenderer {
    private final Context context;
    private final Map<String, Bitmap> portraits = new HashMap<>();
    private final RectF rect = new RectF();
    private final Rect source = new Rect();
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    CharacterSelectRenderer(Context context) { this.context = context; }

    void draw(Canvas c, Paint p, CharacterSelect s) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(235, 14, 16, 26));
        c.drawRect(0, 0, Arena.VW, Arena.VH, p);

        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setColor(Color.WHITE);
        p.setTextSize(44f);
        c.drawText("ESCOLHA A DUPLA", Arena.VW / 2f, 90f, p);
        p.setFakeBoldText(false);
        p.setTextSize(20f);
        p.setColor(Color.rgb(190, 196, 214));
        c.drawText("1º começa lutando · 2º entra no TAG · toque de novo para desmarcar", Arena.VW / 2f, 125f, p);

        for (int i = 0; i < s.ids.length; i++) drawCard(c, p, s, i);

        boolean ready = s.ready();
        rect.set(CharacterSelect.FIGHT_LEFT, CharacterSelect.FIGHT_TOP, CharacterSelect.FIGHT_RIGHT, CharacterSelect.FIGHT_BOTTOM);
        p.setColor(ready ? Color.rgb(232, 72, 60) : Color.rgb(70, 72, 84));
        c.drawRoundRect(rect, 16f, 16f, p);
        p.setColor(ready ? Color.WHITE : Color.rgb(150, 152, 164));
        p.setTextSize(34f);
        p.setFakeBoldText(true);
        c.drawText("LUTAR", rect.centerX(), rect.centerY() + 12f, p);

        rect.set(CharacterSelect.BACK_LEFT, CharacterSelect.BACK_TOP, CharacterSelect.BACK_RIGHT, CharacterSelect.BACK_BOTTOM);
        p.setColor(Color.rgb(54, 58, 74));
        c.drawRoundRect(rect, 12f, 12f, p);
        p.setColor(Color.WHITE);
        p.setTextSize(22f);
        c.drawText("VOLTAR", rect.centerX(), rect.centerY() + 8f, p);
        p.setFakeBoldText(false);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCard(Canvas c, Paint p, CharacterSelect s, int i) {
        String id = s.ids[i];
        CharacterDefinition character = GeneratedCharacters.get(id);
        int order = s.order(id);
        float left = s.cardLeft(i), top = CharacterSelect.CARD_TOP;
        rect.set(left, top, left + s.cardWidth(), top + CharacterSelect.CARD_H);

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(32, 35, 50));
        c.drawRoundRect(rect, 14f, 14f, p);
        Bitmap portrait = portrait(character);
        if (portrait != null) {
            // Feet near the bottom, the whole guard inside the card.
            float scale = Math.min((s.cardWidth() - 16f) / portrait.getWidth(),
                (CharacterSelect.CARD_H - 70f) / portrait.getHeight());
            float w = portrait.getWidth() * scale, h = portrait.getHeight() * scale;
            float x = rect.centerX() - w / 2f, y = rect.bottom - 50f - h;
            bitmapPaint.setAlpha(order > 0 || !s.ready() ? 255 : 110);
            source.set(0, 0, portrait.getWidth(), portrait.getHeight());
            c.drawBitmap(portrait, source, new RectF(x, y, x + w, y + h), bitmapPaint);
        }
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(22f);
        p.setFakeBoldText(true);
        c.drawText(character.displayName, rect.centerX(), rect.bottom - 18f, p);
        p.setFakeBoldText(false);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(order > 0 ? 6f : 2f);
        p.setColor(order > 0 ? 0xFF000000 | character.fighter.color : Color.rgb(80, 84, 104));
        c.drawRoundRect(rect, 14f, 14f, p);
        p.setStyle(Paint.Style.FILL);
        if (order > 0) {
            p.setColor(0xFF000000 | character.fighter.color);
            c.drawCircle(rect.left + 26f, rect.top + 26f, 20f, p);
            p.setColor(Color.BLACK);
            p.setTextSize(26f);
            p.setFakeBoldText(true);
            c.drawText(String.valueOf(order), rect.left + 26f, rect.top + 35f, p);
            p.setFakeBoldText(false);
        }
        p.setTextAlign(Paint.Align.LEFT);
    }

    /** First IDLE cell cropped to its opaque box; null when it cannot be read. */
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
                    Log.w("GameLuta", "retrato de " + character.id + " indisponível", ex);
                    cell = null;
                }
            }
        }
        portraits.put(character.id, cell);
        return cell;
    }

    private static Bitmap crop(Bitmap cell) {
        if (cell == null) return null;
        int w = cell.getWidth(), h = cell.getHeight();
        int[] px = new int[w * h];
        cell.getPixels(px, 0, w, 0, 0, w, h);
        int l = w, t = h, r = -1, b = -1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if ((px[y * w + x] >>> 24) > 16) { l = Math.min(l, x); r = Math.max(r, x); t = Math.min(t, y); b = Math.max(b, y); }
        }
        if (r < l) return cell;
        Bitmap out = Bitmap.createBitmap(cell, l, t, r - l + 1, b - t + 1);
        if (out != cell) cell.recycle();
        return out;
    }
}
