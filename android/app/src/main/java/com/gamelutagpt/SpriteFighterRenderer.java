package com.gamelutagpt;

import android.content.Context;
import android.graphics.*;

/** Generic atlas renderer. No attack IDs, resource IDs or per-clip branches. */
final class SpriteFighterRenderer {
    final SpriteMotion motion;
    private final SpriteAtlasCache atlases;
    private CharacterDefinition character;
    private final Rect source=new Rect();
    private final RectF destination=new RectF();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ColorFilter hitFlash=new PorterDuffColorFilter(Color.argb(150,255,255,255),PorterDuff.Mode.SRC_ATOP);
    private final ColorFilter blockFlash=new PorterDuffColorFilter(Color.rgb(205,240,255),PorterDuff.Mode.MULTIPLY);
    /** Base color filter when no flash is showing (the washed-out training opponent), or null. */
    private ColorFilter tint;

    /** Standalone renderer with its own cache (tools and tests). */
    SpriteFighterRenderer(Context context) { this(context,GeneratedCharacters.defaultCharacter()); }
    SpriteFighterRenderer(Context context,CharacterDefinition character) { this(new SpriteAtlasCache(context),character); }

    /** Match renderer: the owner preloads every pack it may show into the shared cache. */
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
     * Draws the current frame with its root at (x, baseY). {@code facing} is the world
     * direction the fighter looks at; mirroring follows the pack's declared art facing.
     */
    /** P01: continuous crouch scaling and a pixel-identical INTRO-to-IDLE handoff. */
    void draw(Canvas canvas,float x,float baseY,int facing,boolean damageFlash,boolean guardFlash) {
        CharacterDefinition.Animation anim=character.animation(motion.clip);
        int frame=motion.frame();
        spritePaint.setColorFilter(damageFlash?hitFlash:guardFlash?blockFlash:tint);
        boolean mirror=facing*character.artFacing<0;
        if(mirror){canvas.save();canvas.scale(-1f,1f,x,0f);}
        boolean p01="player_base".equals(character.id)||"p01_training".equals(character.id);
        float size=1f;
        if(p01 && SpriteStates.CROUCH.equals(motion.clip)) {
            size=1f-0.08f*clamp01(frame/7f);
        } else if(p01 && SpriteStates.RISE.equals(motion.clip)) {
            size=0.92f+0.08f*clamp01(frame/5f);
        }
        if(p01 && SpriteStates.INTRO.equals(motion.clip)) {
            // Last INTRO guard (421px) and first IDLE (450px) previously jumped.
            // Align the root gradually; end with the actual IDLE cell, not a rescale.
            float align=clamp01((frame-18f)/8f);
            float blend=clamp01((frame-22f)/4f);
            if(blend<1f) drawAtlas(canvas,anim.atlas,frame,x-6.72f*align,
                baseY,1f+0.065f*align,Math.round((1f-blend)*255f));
            if(blend>0f) {
                CharacterDefinition.Animation idle=character.animation(SpriteStates.IDLE);
                drawAtlas(canvas,idle.atlas,idle.frame(0f,0f),x,baseY,1f,Math.round(blend*255f));
            }
        } else {
            drawAtlas(canvas,anim.atlas,frame,x,baseY,size,255);
        }
        spritePaint.setAlpha(255);
        if(mirror)canvas.restore();
    }

    private void drawAtlas(Canvas canvas,CharacterDefinition.Atlas a,int frame,float x,
                           float baseY,float size,int alpha) {
        if(alpha<=0)return;
        int col=frame%a.columns,row=frame/a.columns;
        source.set(col*a.width,row*a.height,(col+1)*a.width,(row+1)*a.height);
        float scale=character.profile.worldScale/a.pixelScale*size;
        float left=x-a.rootX*scale,top=baseY-a.rootY*scale;
        destination.set(left,top,left+a.width*scale,top+a.height*scale);
        spritePaint.setAlpha(alpha);
        canvas.drawBitmap(atlases.get(a),source,destination,spritePaint);
    }
    private static float clamp01(float v){return Math.max(0f,Math.min(1f,v));}
    void setTint(ColorFilter tint){this.tint=tint;}
    /** Pale, cooler copy of the art: the same character as the opponent still reads apart. */
    static ColorFilter washedOut() {
        ColorMatrix m=new ColorMatrix();
        m.setSaturation(0.35f);
        m.postConcat(new ColorMatrix(new float[]{
            0.70f,0f,0f,0f,64f,
            0f,0.70f,0f,0f,70f,
            0f,0f,0.70f,0f,84f,
            0f,0f,0f,1f,0f}));
        return new ColorMatrixColorFilter(m);
    }
    boolean hasAnimation(String id){return character.animations.containsKey(id);}
    CharacterDefinition character(){return character;}
    CharacterVisualProfile visualProfile(){return character.profile;}
}
