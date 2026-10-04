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

/** One atlas decode, explicit frame regions and fixed anatomical scale. */
final class SpriteFighterRenderer {
    final SpriteMotion motion = new SpriteMotion();
    private final Bitmap idleSheet;
    private final Bitmap movementSheet;
    private final Rect[] idleFrames = new Rect[8];
    // The authored sheet is not an exact grid: divide-by-four would cut limbs.
    static final int[][] REGIONS = {
        {34,22,336,363,190,358}, {368,20,565,363,465,358},
        {644,21,944,364,792,359}, {992,18,1181,363,1104,357},
        {31,378,322,695,183,689}, {378,375,560,695,465,689},
        {661,379,954,691,809,689}, {1008,373,1186,693,1120,687},
        {40,742,317,1001,180,995}, {377,788,570,1001,473,995},
        {677,691,873,984,800,995}, {991,696,1206,1001,1115,995},
        {29,1009,336,1234,195,1227}, {336,1004,695,1234,510,1227},
        {690,984,931,1234,802,1228}, {963,1032,1241,1238,1110,1231}
    };
    private final Rect[] movementFrames = new Rect[16];
    private final RectF destination = new RectF();
    private final Paint spritePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ColorFilter hitFlash = new PorterDuffColorFilter(Color.argb(150,255,255,255),PorterDuff.Mode.SRC_ATOP);
    private final ColorFilter blockFlash = new PorterDuffColorFilter(Color.rgb(205,240,255),PorterDuff.Mode.MULTIPLY);

    SpriteFighterRenderer(Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        idleSheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.idle, options);
        movementSheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.movement_astra, options);
        if (idleSheet == null || movementSheet == null) throw new IllegalStateException("Missing packaged fighter atlas");
        idleSheet.setDensity(Bitmap.DENSITY_NONE);
        movementSheet.setDensity(Bitmap.DENSITY_NONE);
        for (int i=0;i<8;i++) {
            int w=idleSheet.getWidth()/4,h=idleSheet.getHeight()/2;
            idleFrames[i]=new Rect((i%4)*w,(i/4)*h,(i%4+1)*w,(i/4+1)*h);
        }
        for (int i=0;i<16;i++) {
            int[] r=REGIONS[i];movementFrames[i]=new Rect(r[0],r[1],r[2],r[3]);
            if (r[2]>movementSheet.getWidth() || r[3]>movementSheet.getHeight()) throw new IllegalStateException("Invalid movement atlas bounds");
        }
    }

    void update(float dt, boolean grounded, boolean crouching, float velocityY, float travel,
                boolean forward, boolean dash, boolean backdash, boolean combat, boolean locked) {
        motion.update(dt,grounded,crouching,velocityY,travel,forward,dash,backdash,combat,locked);
    }

    void draw(Canvas canvas, Paint paint, float x, float baseY, boolean damageFlash, boolean guardFlash) {
        int index=motion.frame();
        Rect source;
        Bitmap sheet;
        if (motion.usesIdleSheet()) {
            sheet=idleSheet;source=idleFrames[index];
            float scale=205f / 151f;
            destination.set(x-source.width()*.5f*scale,baseY-153*scale,
                x+source.width()*.5f*scale,baseY+(source.height()-153)*scale);
        } else {
            sheet=movementSheet;source=movementFrames[index];int[] r=REGIONS[index];
            float scale=205f/332f;
            destination.set(x+(source.left-r[4])*scale,baseY+(source.top-r[5])*scale,
                x+(source.right-r[4])*scale,baseY+(source.bottom-r[5])*scale);
        }
        // Scene/HUD share a Paint and may leave alpha or a shader set. Sprite
        // opacity and sampling are independent from whichever layer drew last.
        spritePaint.setColorFilter(damageFlash?hitFlash:guardFlash?blockFlash:null);
        canvas.drawBitmap(sheet,source,destination,spritePaint);

    }
}
