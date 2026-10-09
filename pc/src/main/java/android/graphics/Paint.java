package android.graphics;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Paint {
    public static final int ANTI_ALIAS_FLAG=1;
    public static final int FILTER_BITMAP_FLAG=2;

    public enum Style { FILL, STROKE }
    public enum Align { LEFT, CENTER, RIGHT }
    public enum Cap { BUTT, ROUND, SQUARE }
    public enum Join { MITER, ROUND, BEVEL }

    private int color=Color.rgb(255,255,255);
    private int alpha=255;
    private Style style=Style.FILL;
    private Align textAlign=Align.LEFT;
    private Cap strokeCap=Cap.BUTT;
    private Join strokeJoin=Join.MITER;
    private float strokeWidth=1f;
    private float textSize=16f;
    private boolean fakeBold;
    private float textSkewX;
    private float textScaleX=1f;
    private Shader shader;
    private ColorFilter colorFilter;

    public Paint(){}
    public Paint(int flags){}

    public void setColor(int color){this.color=color;}
    public void setAlpha(int alpha){this.alpha=Math.max(0,Math.min(255,alpha));}
    public void setStyle(Style style){this.style=style;}
    public void setStrokeWidth(float strokeWidth){this.strokeWidth=strokeWidth;}
    public void setTextSize(float textSize){this.textSize=textSize;}
    public void setTextAlign(Align textAlign){this.textAlign=textAlign;}
    public void setFakeBoldText(boolean fakeBold){this.fakeBold=fakeBold;}
    public void setTextSkewX(float textSkewX){this.textSkewX=textSkewX;}
    public void setTextScaleX(float textScaleX){this.textScaleX=textScaleX;}
    public void setStrokeCap(Cap strokeCap){this.strokeCap=strokeCap;}
    public void setStrokeJoin(Join strokeJoin){this.strokeJoin=strokeJoin;}
    public void setShader(Shader shader){this.shader=shader;}
    public void setColorFilter(ColorFilter colorFilter){this.colorFilter=colorFilter;}
    public void setTypeface(Typeface value){setFakeBoldText(value==Typeface.DEFAULT_BOLD);}

    public float ascent(){return -0.80f*textSize;}
    public float descent(){return 0.20f*textSize;}

    int alpha(){return alpha;}
    Style style(){return style;}
    Align textAlign(){return textAlign;}
    float textScaleX(){return textScaleX;}
    float textSkewX(){return textSkewX;}
    ColorFilter colorFilter(){return colorFilter;}

    Font font(){
        return new Font(Font.SANS_SERIF,fakeBold?Font.BOLD:Font.PLAIN,Math.max(1,Math.round(textSize)));
    }

    void apply(Graphics2D g){
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int sourceAlpha=Color.alpha(color);
        int actualAlpha=sourceAlpha*alpha/255;
        java.awt.Color awtColor=new java.awt.Color(Color.red(color),Color.green(color),Color.blue(color),actualAlpha);

        if(shader instanceof LinearGradient gradient){
            java.awt.Color c0=new java.awt.Color(Color.red(gradient.color0),Color.green(gradient.color0),Color.blue(gradient.color0),Color.alpha(gradient.color0)*alpha/255);
            java.awt.Color c1=new java.awt.Color(Color.red(gradient.color1),Color.green(gradient.color1),Color.blue(gradient.color1),Color.alpha(gradient.color1)*alpha/255);
            g.setPaint(new GradientPaint(gradient.x0,gradient.y0,c0,gradient.x1,gradient.y1,c1,false));
        }else if(shader instanceof RadialGradient radial) {
            java.awt.Color center=new java.awt.Color(Color.red(radial.centerColor),Color.green(radial.centerColor),Color.blue(radial.centerColor),Color.alpha(radial.centerColor)*alpha/255);
            java.awt.Color edge=new java.awt.Color(Color.red(radial.edgeColor),Color.green(radial.edgeColor),Color.blue(radial.edgeColor),Color.alpha(radial.edgeColor)*alpha/255);
            g.setPaint(new java.awt.RadialGradientPaint(radial.cx,radial.cy,Math.max(1f,radial.radius),new float[]{0f,1f},new java.awt.Color[]{center,edge}));
        }else{
            g.setPaint(awtColor);
        }

        int cap=switch(strokeCap){
            case ROUND -> BasicStroke.CAP_ROUND;
            case SQUARE -> BasicStroke.CAP_SQUARE;
            default -> BasicStroke.CAP_BUTT;
        };
        int join=switch(strokeJoin){
            case ROUND -> BasicStroke.JOIN_ROUND;
            case BEVEL -> BasicStroke.JOIN_BEVEL;
            default -> BasicStroke.JOIN_MITER;
        };
        g.setStroke(new BasicStroke(Math.max(0.1f,strokeWidth),cap,join));
        g.setFont(font());
    }
}
