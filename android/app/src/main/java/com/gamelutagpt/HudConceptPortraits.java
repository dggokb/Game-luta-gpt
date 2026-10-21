package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** HUD faces from the author's Drive concepts, independent of combat sprite atlases. */
final class HudConceptPortraits {
    private final Context context;
    private final Map<String, Bitmap> cache = new HashMap<>();

    HudConceptPortraits(Context context) { this.context = context; }

    static String source(String id) {
        if ("player_base".equals(id)) return "ui/hud/concepts/p01.webp";
        if (id.matches("p0[2-9]|p1[0-2]|p15")) return "ui/hud/concepts/" + id + ".webp";
        return null;
    }

    /** Pixel regions of the authored face close-ups; full source sheets are preserved. */
    private static int[] face(String id) {
        switch (id) {
            case "player_base": return new int[]{1170, 30, 1430, 340};
            case "p02": return new int[]{1147, 10, 1438, 274};
            case "p03": return new int[]{1149, 14, 1435, 342};
            case "p04": return new int[]{1244, 24, 1525, 330};
            case "p05": return new int[]{8, 755, 289, 1065};
            case "p06": return new int[]{868, 638, 1184, 1015};
            case "p07": return new int[]{18, 685, 357, 1005};
            case "p08": return new int[]{12, 645, 307, 1070};
            case "p09": return new int[]{18, 758, 372, 1014};
            case "p10": return new int[]{13, 770, 368, 1058};
            case "p11": return new int[]{870, 419, 1145, 747};
            case "p12": return new int[]{1169, 15, 1435, 332};
            case "p15": return new int[]{905, 513, 1163, 835};
            default: return null;
        }
    }

    Bitmap get(String id) {
        if (cache.containsKey(id)) return cache.get(id);
        Bitmap result = null;
        String source = source(id);
        int[] rect = face(id);
        if (source != null && rect != null) {
            try (InputStream input = context.getAssets().open(source)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;
                Bitmap sheet = BitmapFactory.decodeStream(input, null, options);
                if (sheet != null) {
                    try { result = Bitmap.createBitmap(sheet, rect[0], rect[1], rect[2]-rect[0], rect[3]-rect[1]); }
                    finally { if (result != sheet) sheet.recycle(); }
                }
            } catch (IOException | RuntimeException e) {
                Log.w("HudConceptPortraits", "Concept portrait unavailable: " + id, e);
            }
        }
        cache.put(id, result);
        return result;
    }
}
