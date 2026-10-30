package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import java.util.Locale;

/**
 * Tela "CARREGANDO" com o anel girando (azul do dragão, vermelho do tigre): aparece sempre
 * que o jogo decodifica arte pesada (abrir o app, duplas novas, lutas da história), para
 * nunca parecer travado. O mesmo desenho no APK e no PC.
 */
public final class TelaCarregando {
    private static final int BLUE = 0xFF24AAFF;
    private static final int RED = 0xFFFF3149;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ring = new RectF();
    private final Shader background = new Degrade.Linear(0f, 0f, 0f, Arena.VH,
        0xFF0B163F, 0xFF04060F, Shader.TileMode.CLAMP);
    private final String text = texto(Locale.getDefault().getLanguage());

    /** "CARREGANDO" no idioma do aparelho (os mesmos três da história). */
    static String texto(String idioma) {
        if ("en".equals(idioma)) return "LOADING";
        if ("es".equals(idioma)) return "CARGANDO";
        return "CARREGANDO";
    }

    /** Desenha em tela cheia; {@code ms} = há quanto tempo está carregando (anima o anel). */
    public void draw(Canvas c, long ms) {
        float sx = c.getWidth() / Arena.VW, sy = c.getHeight() / Arena.VH;
        c.save();
        c.scale(sx, sy);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(background);
        c.drawRect(0f, 0f, Arena.VW, Arena.VH, paint);
        paint.setShader(null);

        float cx = Arena.VW * 0.5f, cy = Arena.VH * 0.44f, r = 46f;
        float turn = (ms % 1200L) / 1200f * 360f;
        ring.set(cx - r, cy - r, cx + r, cy + r);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(9f);
        paint.setColor(0x33FFFFFF);
        c.drawCircle(cx, cy, r, paint);
        paint.setColor(BLUE);
        c.drawArc(ring, turn, 110f, false, paint);
        paint.setColor(RED);
        c.drawArc(ring, turn + 180f, 110f, false, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setColor(0xFFFFFFFF);
        paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        paint.setTextSize(40f);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(text, cx, cy + r + 70f, paint);
        // Pontinhos acendendo um por vez.
        int lit = (int)((ms / 350L) % 4L);
        for (int i = 0; i < 3; i++) {
            paint.setColor(i < lit ? 0xFFFFFFFF : 0x40FFFFFF);
            c.drawCircle(cx - 18f + i * 18f, cy + r + 100f, 5f, paint);
        }
        paint.setTypeface(null);
        paint.setTextAlign(Paint.Align.LEFT);
        c.restore();
    }
}
