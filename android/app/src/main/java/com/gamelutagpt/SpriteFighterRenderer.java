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

/**
 * Renders pre-normalized fighter atlases.
 *
 * Registration and scale belong to the character profile, never to an animation
 * clip. Idle, movement and attacks therefore use the exact same transform.
 */
final class SpriteFighterRenderer {
    final SpriteMotion motion = new SpriteMotion();

    private final CharacterVisualProfile profile;
    private final Bitmap idleSheet;
    private final Bitmap movementSheet;
    private final Bitmap jabSheet;
    private final Bitmap mediumKickSheet;
    private final Bitmap heavyStraightSheet;
    private final Rect[] idleFrames = new Rect[8];
    private final Rect[] movementFrames = new Rect[16];
    private final Rect[] jabFrames = new Rect[3];
    private final Rect[] mediumKickFrames = new Rect[3];
    private final Rect[] heavyStraightFrames = new Rect[9];
    private final RectF destination = new RectF();
    private final Paint spritePaint =
        new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ColorFilter hitFlash =
        new PorterDuffColorFilter(
            Color.argb(150,255,255,255),
            PorterDuff.Mode.SRC_ATOP
        );
    private final ColorFilter blockFlash =
        new PorterDuffColorFilter(
            Color.rgb(205,240,255),
            PorterDuff.Mode.MULTIPLY
        );

    SpriteFighterRenderer(Context context) {
        this(context,CharacterVisualProfile.PLAYER_BASE);
    }

    SpriteFighterRenderer(
        Context context,
        CharacterVisualProfile profile
    ) {
        this.profile=profile;

        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inScaled=false;
        idleSheet=BitmapFactory.decodeResource(
            context.getResources(),
            R.drawable.player_base_idle,
            options
        );
        movementSheet=BitmapFactory.decodeResource(
            context.getResources(),
            R.drawable.player_base_movement,
            options
        );
        jabSheet=BitmapFactory.decodeResource(
            context.getResources(),
            R.drawable.player_base_jab,
            options
        );
        mediumKickSheet=BitmapFactory.decodeResource(
            context.getResources(),
            R.drawable.player_base_medium_kick,
            options
        );
        heavyStraightSheet=BitmapFactory.decodeResource(
            context.getResources(),
            R.drawable.player_base_heavy_straight,
            options
        );

        if (idleSheet==null || movementSheet==null || jabSheet==null || mediumKickSheet==null || heavyStraightSheet==null) {
            throw new IllegalStateException("Missing normalized fighter atlas");
        }

        idleSheet.setDensity(Bitmap.DENSITY_NONE);
        movementSheet.setDensity(Bitmap.DENSITY_NONE);
        jabSheet.setDensity(Bitmap.DENSITY_NONE);
        mediumKickSheet.setDensity(Bitmap.DENSITY_NONE);
        heavyStraightSheet.setDensity(Bitmap.DENSITY_NONE);

        requireSheet(idleSheet,4,2,"idle");
        requireSheet(movementSheet,4,4,"movement");
        requireSheet(jabSheet,3,1,"jab");
        if (
            mediumKickSheet.getWidth()!=
                GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_WIDTH *
                GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_COUNT ||
            mediumKickSheet.getHeight()!=
                GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_HEIGHT
        ) {
            throw new IllegalStateException("Invalid medium kick atlas");
        }
        if (
            heavyStraightSheet.getWidth()!=
                GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_WIDTH *
                GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_COUNT ||
            heavyStraightSheet.getHeight()!=
                GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_HEIGHT
        ) {
            throw new IllegalStateException("Invalid heavy straight atlas");
        }

        slice(idleFrames,4);
        slice(movementFrames,4);
        slice(jabFrames,3);
        for(int i=0;i<mediumKickFrames.length;i++) {
            int w=GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_WIDTH;
            int h=GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_HEIGHT;
            mediumKickFrames[i]=new Rect(i*w,0,(i+1)*w,h);
        }
        for(int i=0;i<heavyStraightFrames.length;i++) {
            int w=GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_WIDTH;
            int h=GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_HEIGHT;
            heavyStraightFrames[i]=new Rect(i*w,0,(i+1)*w,h);
        }
    }

    private void requireSheet(
        Bitmap sheet,
        int columns,
        int rows,
        String name
    ) {
        int expectedWidth=columns*profile.frameWidth;
        int expectedHeight=rows*profile.frameHeight;
        if (
            sheet.getWidth()!=expectedWidth ||
            sheet.getHeight()!=expectedHeight
        ) {
            throw new IllegalStateException(
                "Invalid "+name+" atlas for "+profile.id+
                ": expected "+expectedWidth+"x"+expectedHeight+
                " but got "+sheet.getWidth()+"x"+sheet.getHeight()
            );
        }
    }

    private void slice(Rect[] frames,int columns) {
        for (int i=0;i<frames.length;i++) {
            int column=i%columns;
            int row=i/columns;
            frames[i]=new Rect(
                column*profile.frameWidth,
                row*profile.frameHeight,
                (column+1)*profile.frameWidth,
                (row+1)*profile.frameHeight
            );
        }
    }

    void update(
        float dt,
        boolean grounded,
        boolean crouching,
        float velocityY,
        float travel,
        boolean forward,
        boolean dash,
        boolean backdash,
        boolean lightJab,
        boolean mediumKick,
        boolean heavyStraight,
        boolean combat,
        boolean locked
    ) {
        motion.update(
            dt,grounded,crouching,velocityY,travel,forward,dash,
            backdash,lightJab,mediumKick,heavyStraight,combat,locked
        );
    }

    void draw(
        Canvas canvas,
        Paint paint,
        float x,
        float baseY,
        boolean damageFlash,
        boolean guardFlash
    ) {
        int index=motion.frame();
        Rect source;
        Bitmap sheet;

        if (motion.clip==SpriteMotion.Clip.LIGHT_JAB) {
            sheet=jabSheet;
            source=jabFrames[index];
        } else if (motion.clip==SpriteMotion.Clip.MEDIUM_KICK) {
            sheet=mediumKickSheet;
            source=mediumKickFrames[index];
        } else if (motion.clip==SpriteMotion.Clip.HEAVY_STRAIGHT) {
            sheet=heavyStraightSheet;
            source=heavyStraightFrames[index];
        } else if (motion.usesIdleSheet()) {
            sheet=idleSheet;
            source=idleFrames[index];
        } else {
            sheet=movementSheet;
            source=movementFrames[index];
        }

        if (motion.clip==SpriteMotion.Clip.MEDIUM_KICK) {
            float left=
                x-GeneratedSpriteLayouts.MEDIUM_KICK_ROOT_X*profile.worldScale;
            float top=
                baseY-GeneratedSpriteLayouts.MEDIUM_KICK_ROOT_Y*profile.worldScale;
            destination.set(left,top,left+source.width()*profile.worldScale,top+source.height()*profile.worldScale);
        } else if (motion.clip==SpriteMotion.Clip.HEAVY_STRAIGHT) {
            float left=
                x-GeneratedSpriteLayouts.HEAVY_STRAIGHT_ROOT_X*profile.worldScale;
            float top=
                baseY-GeneratedSpriteLayouts.HEAVY_STRAIGHT_ROOT_Y*profile.worldScale;
            destination.set(left,top,left+source.width()*profile.worldScale,top+source.height()*profile.worldScale);
        } else {
            profile.place(destination,x,baseY);
        }

        spritePaint.setColorFilter(
            damageFlash ? hitFlash : guardFlash ? blockFlash : null
        );
        canvas.drawBitmap(sheet,source,destination,spritePaint);
    }

    CharacterVisualProfile visualProfile() {
        return profile;
    }
}
