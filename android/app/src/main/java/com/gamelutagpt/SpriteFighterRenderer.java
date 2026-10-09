package com.gamelutagpt;

import android.content.Context;
import android.graphics.*;

/** Generic atlas renderer. Crossfade occurs only in graphics, never in physics. */
final class SpriteFighterRenderer {
    final SpriteMotion motion;
    private final SpriteAtlasCache atlases;
    private CharacterDefinition character;
    private final Rect source=new Rect();
    private final RectF destination=new RectF();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ColorFilter hitFlash=new PorterDuffColorFilter(Color.argb(150,255,255,255),PorterDuff.Mode.SRC_ATOP);
    private final ColorFilter blockFlash=new PorterDuffColorFilter(Color.rgb(205,240,255),PorterDuff.Mode.MULTIPLY);
    private ColorFilter tint;

    SpriteFighterRenderer(Context context) { this(context,GeneratedCharacters.defaultCharacter()); }
    SpriteFighterRenderer(Context context,CharacterDefinition character) { this(new SpriteAtlasCache(context),character); }
    SpriteFighterRenderer(SpriteAtlasCache atlases,CharacterDefinition character) {
        this.atlases=atlases;this.character=character;
        atlases.preload(character);
        motion=new SpriteMotion(character);
    }
    void setCharacter(String id) {
        CharacterDefinition next=GeneratedCharacters.get(id);
        if(next==character)return;
        atlases.preload(next);character=next;motion.setCharacter(next);
    }
    void update(float dt,boolean grounded,boolean crouching,float velocityY,float travel,
                boolean forward,boolean dash,boolean backdash,String attackAnimation,
                float attackElapsed,boolean combat,boolean locked) {
        motion.update(dt,grounded,crouching,velocityY,travel,forward,dash,backdash,attackAnimation,attackElapsed,combat,locked);
    }
    /**
     * Blend old pose at the same character root with the new pose for a few
     * frames. The engine already picked the new combat state this tick; the
     * blend does not affect attacks, hurtboxes, movement, hitstop or recovery.
     *
     * Alpha uses a complementary mix, never draws either pose twice opaque.
     */
    void draw(Canvas canvas,float x,float baseY,int facing,boolean damageFlash,boolean guardFlash) {
        spritePaint.setColorFilter(damageFlash?hitFlash:guardFlash?blockFlash:tint);
        boolean mirror=facing*character.artFacing<0;
        if(mirror){canvas.save();canvas.scale(-1f,1f,x,0f);}
        float outgoing=motion.outgoingAlpha();
        if(outgoing>0f && motion.previousClip!=null) {
            CharacterDefinition.Animation old=character.animations.get(motion.previousClip);
            if(old!=null) drawFrame(canvas,old.atlas,motion.previousFrame,x,baseY,outgoing);
            drawFrame(canvas,character.animation(motion.clip).atlas,motion.frame(),x,baseY,1f-outgoing);
        } else {
            drawFrame(canvas,character.animation(motion.clip).atlas,motion.frame(),x,baseY,1f);
        }
        spritePaint.setAlpha(255);
        if(mirror)canvas.restore();
    }

    private void drawFrame(Canvas canvas,CharacterDefinition.Atlas a,int frame,
                           float x,float baseY,float alpha) {
        int col=frame%a.columns,row=frame/a.columns;
        // Native on most phones. Only actual OOM invokes the reduced-size
        // fallback; scale source coordinates, never the world hitbox/feet root.
        Bitmap bitmap=atlases.get(a);
        int rawW=a.columns*a.width;
        int rawH=((a.count+a.columns-1)/a.columns)*a.height;
        float ratioX=bitmap.getWidth()/(float)rawW;
        float ratioY=bitmap.getHeight()/(float)rawH;
        source.set(Math.round(col*a.width*ratioX),Math.round(row*a.height*ratioY),
            Math.round((col+1)*a.width*ratioX),Math.round((row+1)*a.height*ratioY));
        float scale=character.profile.worldScale,left=x-a.rootX*scale,top=baseY-a.rootY*scale;
        destination.set(left,top,left+a.width*scale,top+a.height*scale);
        spritePaint.setAlpha(Math.max(0,Math.min(255,Math.round(alpha*255f))));
        canvas.drawBitmap(bitmap,source,destination,spritePaint);
    }

    void setTint(ColorFilter tint){this.tint=tint;}
    static ColorFilter washedOut() {
        ColorMatrix m=new ColorMatrix();
        m.setSaturation(0.35f);
        m.postConcat(new ColorMatrix(new float[]{
            0.70f,0f,0f,0f,64f,
            0f,0.70f,0f,0f,70f,
            0f,0f,0f,1f,0f}));
        return new ColorMatrixColorFilter(m);
    }
    boolean hasAnimation(String id){return character.animations.containsKey(id);}
    CharacterDefinition character(){return character;}
    CharacterVisualProfile visualProfile(){return character.profile;}
}
