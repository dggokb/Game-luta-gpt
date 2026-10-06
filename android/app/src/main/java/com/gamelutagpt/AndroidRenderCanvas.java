package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;

/** Implementação do {@link RenderCanvas} em cima do Canvas do Android. */
final class AndroidRenderCanvas implements RenderCanvas {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint invertPaint = new Paint();
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final Rect srcRect = new Rect();
    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Canvas canvas;

    AndroidRenderCanvas() {
        invertPaint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        })));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    /** Define o Canvas do quadro atual, já escalado para 1280x720. */
    void begin(Canvas target) {
        canvas = target;
    }

    @Override
    public void save() {
        canvas.save();
    }

    @Override
    public void restore() {
        canvas.restore();
    }

    @Override
    public void translate(float dx, float dy) {
        canvas.translate(dx, dy);
    }

    @Override
    public void scale(float sx, float sy, float px, float py) {
        canvas.scale(sx, sy, px, py);
    }

    @Override
    public void rotate(float degrees, float px, float py) {
        canvas.rotate(degrees, px, py);
    }

    @Override
    public void clipPolygon(float[] xy) {
        canvas.clipPath(polygon(xy));
    }

    @Override
    public void fillRect(float left, float top, float right, float bottom, int argb) {
        fill(argb);
        canvas.drawRect(left, top, right, bottom, paint);
    }

    @Override
    public void fillPolygon(float[] xy, int argb) {
        fill(argb);
        canvas.drawPath(polygon(xy), paint);
    }

    @Override
    public void strokePolygon(float[] xy, float width, int argb) {
        stroke(argb, width, Paint.Cap.BUTT, Paint.Join.MITER);
        canvas.drawPath(polygon(xy), paint);
    }

    @Override
    public void fillCircle(float cx, float cy, float radius, int argb) {
        fill(argb);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    @Override
    public void strokeCircle(float cx, float cy, float radius, float width, int argb) {
        stroke(argb, width, Paint.Cap.BUTT, Paint.Join.MITER);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {
        stroke(argb, width, Paint.Cap.ROUND, Paint.Join.ROUND);
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    @Override
    public void drawImage(RenderImage image, float left, float top, float right, float bottom, int alpha) {
        imagePaint.setAlpha(alpha);
        rect.set(left, top, right, bottom);
        canvas.drawBitmap(((AndroidRenderAssets.AndroidRenderImage)image).bitmap, null, rect, imagePaint);
    }

    @Override
    public void drawImageRegion(
        RenderImage image,
        float srcLeft,
        float srcTop,
        float srcRight,
        float srcBottom,
        float left,
        float top,
        float right,
        float bottom,
        int alpha
    ) {
        imagePaint.setAlpha(alpha);
        srcRect.set(Math.round(srcLeft), Math.round(srcTop), Math.round(srcRight), Math.round(srcBottom));
        if (srcRect.width() <= 0 || srcRect.height() <= 0) return;
        rect.set(left, top, right, bottom);
        canvas.drawBitmap(((AndroidRenderAssets.AndroidRenderImage)image).bitmap, srcRect, rect, imagePaint);
    }

    @Override
    public void fillRadialGlow(float cx, float cy, float radius, int argbCenter) {
        if (radius <= 0f) return;
        gradientPaint.setShader(new RadialGradient(
            cx, cy, radius, argbCenter, argbCenter & 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, gradientPaint);
        gradientPaint.setShader(null);
    }

    @Override
    public void fillVerticalGradient(float left, float top, float right, float bottom, int argbTop, int argbBottom) {
        if (bottom <= top) return;
        gradientPaint.setShader(new LinearGradient(0f, top, 0f, bottom, argbTop, argbBottom, Shader.TileMode.CLAMP));
        canvas.drawRect(left, top, right, bottom, gradientPaint);
        gradientPaint.setShader(null);
    }

    @Override
    public void fillOval(float left, float top, float right, float bottom, int argb) {
        fill(argb);
        rect.set(left, top, right, bottom);
        canvas.drawOval(rect, paint);
    }

    @Override
    public void drawText(
        String text,
        float x,
        float y,
        float size,
        int fillArgb,
        int strokeArgb,
        float strokeWidth,
        boolean italic
    ) {
        paint.setTextSize(size);
        paint.setTextSkewX(italic ? -0.22f : 0f);
        if (strokeWidth > 0f) {
            stroke(strokeArgb, strokeWidth, Paint.Cap.ROUND, Paint.Join.ROUND);
            canvas.drawText(text, x, y, paint);
        }
        fill(fillArgb);
        canvas.drawText(text, x, y, paint);
        paint.setTextSkewX(0f);
    }

    @Override
    public void beginInvert() {
        canvas.saveLayer(null, invertPaint);
    }

    @Override
    public void endInvert() {
        canvas.restore();
    }

    private void fill(int argb) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(argb);
    }

    private void stroke(int argb, float width, Paint.Cap cap, Paint.Join join) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(argb);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(cap);
        paint.setStrokeJoin(join);
    }

    private Path polygon(float[] xy) {
        path.reset();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i + 1 < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        path.close();
        return path;
    }
}
