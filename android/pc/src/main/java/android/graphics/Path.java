package android.graphics;

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** Caminho vetorial sobre {@link Path2D}. */
public class Path {
    public enum Direction { CW, CCW }

    public enum FillType { WINDING, EVEN_ODD, INVERSE_WINDING, INVERSE_EVEN_ODD }

    final Path2D.Float shape = new Path2D.Float(Path2D.WIND_NON_ZERO);
    private FillType fillType = FillType.WINDING;

    public Path() {}

    public Path(Path src) { set(src); }

    public void set(Path src) {
        shape.reset();
        shape.append(src.shape, false);
        setFillType(src.fillType);
    }

    public void reset() {
        shape.reset();
        setFillType(FillType.WINDING);
    }

    public void rewind() { reset(); }

    public boolean isEmpty() { return shape.getCurrentPoint() == null; }

    public FillType getFillType() { return fillType; }

    public void setFillType(FillType ft) {
        fillType = ft;
        shape.setWindingRule(ft == FillType.EVEN_ODD || ft == FillType.INVERSE_EVEN_ODD
            ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
    }

    public void moveTo(float x, float y) { shape.moveTo(x, y); }

    public void rMoveTo(float dx, float dy) { Point2D p = last(); shape.moveTo(p.getX() + dx, p.getY() + dy); }

    public void lineTo(float x, float y) { ensureStart(); shape.lineTo(x, y); }

    public void rLineTo(float dx, float dy) { Point2D p = last(); ensureStart(); shape.lineTo(p.getX() + dx, p.getY() + dy); }

    public void quadTo(float x1, float y1, float x2, float y2) { ensureStart(); shape.quadTo(x1, y1, x2, y2); }

    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        Point2D p = last();
        ensureStart();
        shape.quadTo(p.getX() + dx1, p.getY() + dy1, p.getX() + dx2, p.getY() + dy2);
    }

    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        ensureStart();
        shape.curveTo(x1, y1, x2, y2, x3, y3);
    }

    public void rCubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        Point2D p = last();
        ensureStart();
        shape.curveTo(p.getX() + x1, p.getY() + y1, p.getX() + x2, p.getY() + y2, p.getX() + x3, p.getY() + y3);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo) {
        arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle) { arcTo(oval, startAngle, sweepAngle, false); }

    public void arcTo(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean forceMoveTo) {
        // Android mede o ângulo no sentido horário (y para baixo); o Arc2D, no anti-horário.
        Arc2D arc = new Arc2D.Float(left, top, right - left, bottom - top, -startAngle, -sweepAngle, Arc2D.OPEN);
        shape.append(arc, !forceMoveTo && shape.getCurrentPoint() != null);
    }

    public void addArc(RectF oval, float startAngle, float sweepAngle) {
        addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle);
    }

    public void addArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        shape.append(new Arc2D.Float(left, top, right - left, bottom - top, -startAngle, -sweepAngle, Arc2D.OPEN), false);
    }

    public void close() { if (shape.getCurrentPoint() != null) shape.closePath(); }

    public void addRect(float left, float top, float right, float bottom, Direction dir) {
        shape.append(new Rectangle2D.Float(Math.min(left, right), Math.min(top, bottom), Math.abs(right - left), Math.abs(bottom - top)), false);
    }

    public void addRect(RectF rect, Direction dir) { addRect(rect.left, rect.top, rect.right, rect.bottom, dir); }

    public void addOval(float left, float top, float right, float bottom, Direction dir) {
        shape.append(new Ellipse2D.Float(left, top, right - left, bottom - top), false);
    }

    public void addOval(RectF oval, Direction dir) { addOval(oval.left, oval.top, oval.right, oval.bottom, dir); }

    public void addCircle(float x, float y, float radius, Direction dir) {
        shape.append(new Ellipse2D.Float(x - radius, y - radius, radius * 2f, radius * 2f), false);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float rx, float ry, Direction dir) {
        shape.append(roundRect(left, top, right, bottom, rx, ry), false);
    }

    public void addRoundRect(RectF rect, float rx, float ry, Direction dir) {
        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, dir);
    }

    /** Cantos {x0, y0, x1, y1, ...} em sentido horário a partir do canto superior esquerdo. */
    public void addRoundRect(float left, float top, float right, float bottom, float[] radii, Direction dir) {
        Path2D.Float p = new Path2D.Float();
        float tlx = radii[0], tly = radii[1], trx = radii[2], try_ = radii[3];
        float brx = radii[4], bry = radii[5], blx = radii[6], bly = radii[7];
        final float k = 0.5522848f;
        p.moveTo(left + tlx, top);
        p.lineTo(right - trx, top);
        p.curveTo(right - trx + trx * k, top, right, top + try_ - try_ * k, right, top + try_);
        p.lineTo(right, bottom - bry);
        p.curveTo(right, bottom - bry + bry * k, right - brx + brx * k, bottom, right - brx, bottom);
        p.lineTo(left + blx, bottom);
        p.curveTo(left + blx - blx * k, bottom, left, bottom - bly + bly * k, left, bottom - bly);
        p.lineTo(left, top + tly);
        p.curveTo(left, top + tly - tly * k, left + tlx - tlx * k, top, left + tlx, top);
        p.closePath();
        shape.append(p, false);
    }

    public void addRoundRect(RectF rect, float[] radii, Direction dir) {
        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radii, dir);
    }

    public void addPath(Path src) { shape.append(src.shape, false); }

    public void addPath(Path src, float dx, float dy) {
        shape.append(src.shape.createTransformedShape(AffineTransform.getTranslateInstance(dx, dy)), false);
    }

    public void offset(float dx, float dy) { shape.transform(AffineTransform.getTranslateInstance(dx, dy)); }

    public void transform(Matrix matrix) { shape.transform(matrix.toAffine()); }

    public void computeBounds(RectF bounds, boolean exact) {
        Rectangle2D b = shape.getBounds2D();
        bounds.set((float) b.getMinX(), (float) b.getMinY(), (float) b.getMaxX(), (float) b.getMaxY());
    }

    static RoundRectangle2D roundRect(float left, float top, float right, float bottom, float rx, float ry) {
        float w = right - left, h = bottom - top;
        float ax = Math.max(0f, Math.min(rx, w / 2f)), ay = Math.max(0f, Math.min(ry, h / 2f));
        return new RoundRectangle2D.Float(left, top, w, h, ax * 2f, ay * 2f);
    }

    private Point2D last() {
        Point2D p = shape.getCurrentPoint();
        return p == null ? new Point2D.Float() : p;
    }

    private void ensureStart() {
        if (shape.getCurrentPoint() == null) shape.moveTo(0f, 0f);
    }
}
