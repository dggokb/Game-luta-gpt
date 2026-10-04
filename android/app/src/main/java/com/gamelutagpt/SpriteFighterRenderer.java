package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

/** Prepacked atlases: each movement frame already matches its runtime scale. */
final class SpriteFighterRenderer {
    final SpriteMotion motion = new SpriteMotion();
    private final Bitmap idleSheet;
    private final Bitmap movementSheet;
    private final Bitmap jabSheet;
    private final Rect[] idleFrames = new Rect[8];
    private final Rect[] movementFrames = new Rect[16];
    private final Rect[] jabFrames = new Rect[3];
    private final RectF destination = new RectF();
    private final Paint spritePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ColorFilter hitFlash = new PorterDuffColorFilter(Color.argb(150,255,255,255),PorterDuff.Mode.SRC_ATOP);
    private final ColorFilter blockFlash = new PorterDuffColorFilter(Color.rgb(205,240,255),PorterDuff.Mode.MULTIPLY);

    SpriteFighterRenderer(Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        idleSheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.idle, options);
        movementSheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.movement_astra, options);
        jabSheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jab_light, options);
        if (idleSheet == null || movementSheet == null || jabSheet == null) {
            throw new IllegalStateException("Missing packaged fighter atlas");
        }
        idleSheet.setDensity(Bitmap.DENSITY_NONE);
        movementSheet.setDensity(Bitmap.DENSITY_NONE);
        jabSheet.setDensity(Bitmap.DENSITY_NONE);
        for (int i=0;i<8;i++) {
            int w=idleSheet.getWidth()/4,h=idleSheet.getHeight()/2;
            idleFrames[i]=new Rect((i%4)*w,(i/4)*h,(i%4+1)*w,(i/4+1)*h);
        }
        for (int i=0;i<16;i++) {
            int w=movementSheet.getWidth()/4,h=movementSheet.getHeight()/4;
            movementFrames[i]=new Rect((i%4)*w,(i/4)*h,(i%4+1)*w,(i/4+1)*h);
        }
        if (jabSheet.getWidth()!=768 || jabSheet.getHeight()!=256) {
            throw new IllegalStateException("Invalid light jab atlas dimensions");
        }
        for (int i=0;i<3;i++) {
            jabFrames[i]=new Rect(i*256,0,(i+1)*256,256);
        }
    }

    void update(float dt, boolean grounded, boolean crouching, float velocityY, float travel,
                boolean forward, boolean dash, boolean backdash, boolean lightJab,
                boolean combat, boolean locked) {
        motion.update(dt,grounded,crouching,velocityY,travel,forward,dash,backdash,lightJab,combat,locked);
    }

    void draw(Canvas canvas, Paint paint, float x, float baseY, boolean damageFlash, boolean guardFlash) {
        int index=motion.frame();
        Rect source;
        Bitmap sheet;
        if (motion.clip == SpriteMotion.Clip.LIGHT_JAB) {
            sheet=jabSheet;source=jabFrames[index];
            destination.set(x-128f,baseY-235f,x+128f,baseY+21f);
        } else if (motion.usesIdleSheet()) {
            sheet=idleSheet;source=idleFrames[index];
            float scale=205f / 151f;
            destination.set(x-source.width()*.5f*scale,baseY-153*scale,
                x+source.width()*.5f*scale,baseY+(source.height()-153)*scale);
        } else {
            sheet=movementSheet;source=movementFrames[index];
            destination.set(x-128f,baseY-235f,x+128f,baseY+21f);
        }
        // Scene/HUD share a Paint and may leave alpha or a shader set. Sprite
        // opacity and sampling are independent from whichever layer drew last.
        spritePaint.setColorFilter(damageFlash?hitFlash:guardFlash?blockFlash:null);
        canvas.drawBitmap(sheet,source,destination,spritePaint);

    }
}
