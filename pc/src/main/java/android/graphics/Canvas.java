package android.graphics;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.Deque;

public final class Canvas {
    private static final class State {
        final AffineTransform transform;
        final Shape clip;
        final Composite composite;
        State(Graphics2D g){
            transform=g.getTransform();
            clip=g.getClip();
            composite=g.getComposite();
        }
    }

    private final Graphics2D graphics;
    private final int width,height;
    private final Deque<State> states=new ArrayDeque<>();

    public Canvas(Graphics2D graphics,int width,int height){
        this.graphics=graphics;this.width=width;this.height=height;
    }

    public int getWidth(){return width;}
    public int getHeight(){return height;}

    public int save(){states.push(new State(graphics));return states.size();}
    public int saveLayer(RectF bounds,Paint paint){return save();}
    public int saveLayerAlpha(float left,float top,float right,float bottom,int alpha){
        int count=save();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0f,Math.min(1f,alpha/255f))));
        return count;
    }
    public void restoreToCount(int count){
        while(!states.isEmpty() && states.size()>=count)restore();
    }

    public void restore(){
        if(states.isEmpty())return;
        State state=states.pop();
        graphics.setTransform(state.transform);
        graphics.setClip(state.clip);
        graphics.setComposite(state.composite);
    }

    public void scale(float sx,float sy){graphics.scale(sx,sy);}
    public void scale(float sx,float sy,float px,float py){
        graphics.translate(px,py);graphics.scale(sx,sy);graphics.translate(-px,-py);
    }
    public void translate(float dx,float dy){graphics.translate(dx,dy);}
    public void rotate(float degrees,float px,float py){graphics.rotate(Math.toRadians(degrees),px,py);}
    public void clipPath(Path path){ if(path!=null)graphics.clip(path.value); }

    public void drawRect(float left,float top,float right,float bottom,Paint paint){
        paint.apply(graphics);
        java.awt.geom.Rectangle2D.Float shape=new java.awt.geom.Rectangle2D.Float(left,top,right-left,bottom-top);
        drawShape(shape,paint);
    }

    public void drawRoundRect(float left,float top,float right,float bottom,float rx,float ry,Paint paint){
        paint.apply(graphics);
        RoundRectangle2D.Float shape=new RoundRectangle2D.Float(left,top,right-left,bottom-top,rx*2f,ry*2f);
        drawShape(shape,paint);
    }

    public void drawOval(float left,float top,float right,float bottom,Paint paint){
        paint.apply(graphics);
        Ellipse2D.Float shape=new Ellipse2D.Float(left,top,right-left,bottom-top);
        drawShape(shape,paint);
    }
    public void drawOval(RectF rect,Paint paint){
        if(rect!=null)drawOval(rect.left,rect.top,rect.right,rect.bottom,paint);
    }

    public void drawCircle(float cx,float cy,float radius,Paint paint){
        drawOval(cx-radius,cy-radius,cx+radius,cy+radius,paint);
    }

    public void drawArc(float left,float top,float right,float bottom,float startAngle,float sweepAngle,boolean useCenter,Paint paint){
        paint.apply(graphics);
        int type=useCenter?Arc2D.PIE:Arc2D.OPEN;
        Arc2D.Float shape=new Arc2D.Float(left,top,right-left,bottom-top,startAngle,sweepAngle,type);
        drawShape(shape,paint);
    }

    public void drawLine(float x1,float y1,float x2,float y2,Paint paint){
        paint.apply(graphics);
        graphics.draw(new java.awt.geom.Line2D.Float(x1,y1,x2,y2));
    }

    public void drawPath(Path path,Paint paint){
        paint.apply(graphics);
        drawShape(path.value,paint);
    }

    public void drawText(String text,float x,float y,Paint paint){
        if(text==null)return;
        paint.apply(graphics);
        FontMetrics metrics=graphics.getFontMetrics();
        float drawX=x;
        float textWidth=metrics.stringWidth(text)*paint.textScaleX();
        if(paint.textAlign()==Paint.Align.CENTER)drawX-=textWidth/2f;
        else if(paint.textAlign()==Paint.Align.RIGHT)drawX-=textWidth;

        AffineTransform old=graphics.getTransform();
        if(paint.textScaleX()!=1f||paint.textSkewX()!=0f){
            graphics.translate(drawX,y);
            graphics.shear(-paint.textSkewX(),0);
            graphics.scale(paint.textScaleX(),1);
            graphics.drawString(text,0,0);
            graphics.setTransform(old);
        }else{
            graphics.drawString(text,drawX,y);
        }
    }

    public void drawBitmap(Bitmap bitmap,Rect source,RectF destination,Paint paint){
        if(bitmap==null||destination==null)return;
        paint.apply(graphics);
        Composite previous=graphics.getComposite();
        graphics.setComposite(AlphaComposite.SrcOver);
        int sl=0,st=0,sr=bitmap.getWidth(),sb=bitmap.getHeight();
        if(source!=null){sl=source.left;st=source.top;sr=source.right;sb=source.bottom;}
        graphics.drawImage(bitmap.image,
            Math.round(destination.left),Math.round(destination.top),
            Math.round(destination.right),Math.round(destination.bottom),
            sl,st,sr,sb,null);
        graphics.setComposite(previous);
    }

    private void drawShape(Shape shape,Paint paint){
        if(paint.style()==Paint.Style.STROKE)graphics.draw(shape);
        else graphics.fill(shape);
    }

    public void dispose(){graphics.dispose();}
}
