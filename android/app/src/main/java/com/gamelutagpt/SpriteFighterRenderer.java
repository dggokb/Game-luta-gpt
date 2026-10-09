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
    void draw(Canvas canvas,float x,float baseY,int facing,boolean damageFlash,boolean guardFlash) {
        spritePaint.setColorFilter(damageFlash?hitFlash:guardFlash?blockFlash:tint);
        boolean mirror=facing*character.artFacing<0;
        if(mirror){canvas.save();canvas.scale(-1f,1f,x,0f);}
        // Always render the authoritative current state first; the old pose is
        // visual-only and fades out. Hit and block responses are never delayed.
        drawPose(canvas,motion.clip,motion.frame(),x,baseY,255);
        float fade=motion.outgoingAlpha();
        if(fade>0f && motion.previousClip!=null && !damageFlash && !guardFlash
                && character.animations.containsKey(motion.previousClip)) {
            drawPose(canvas,motion.previousClip,motion.previousFrame,x,baseY,
                     Math.round(255f*fade));
        }
        spritePaint.setAlpha(255);
        if(mirror)canvas.restore();
    }
    private void drawPose(Canvas canvas,String clip,int frame,float x,float baseY,int alpha) {
        CharacterDefinition.Atlas a=character.animation(clip).atlas;
        int count=Math.max(1,a.count);
        frame=Math.max(0,Math.min(count-1,frame));
        int col=frame%a.columns,row=frame/a.columns;
        source.set(col*a.width,row*a.height,(col+1)*a.width,(row+1)*a.height);
        // Retain the latest sprite pixel-scale normalization (critical for P05/P06).
        float scale=character.profile.worldScale/a.pixelScale;
        destination.set(x-a.rootX*scale,baseY-a.rootY*scale,
            x-a.rootX*scale+a.width*scale,baseY-a.rootY*scale+a.height*scale);
        spritePaint.setAlpha(alpha);
        canvas.drawBitmap(atlases.get(a),source,destination,spritePaint);
    }
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
