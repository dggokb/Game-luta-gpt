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
    // P01 visual normalization measured from the actual alpha bounds of each
    // atlas frame. These values are not estimates of the canvas width.
    private static final float[] P01_CROUCH = {
        0.974f,0.965f,0.929f,0.924f,0.918f,0.907f,0.886f,0.878f
    };
    private static final float[] P01_RISE_X = {
        0.886f,0.904f,0.921f,0.926f,0.929f,0.929f
    };
    private static final float[] P01_RISE_Y = {
        0.885f,0.890f,0.920f,0.960f,1.000f,1.035f
    };
    private static final float[] P01_INTRO_X = {
        1.000f,1.000f,0.947f,0.947f,0.947f,0.947f,0.947f
    };
    private static final float[] P01_INTRO_Y = {
        1.000f,1.000f,1.072f,1.072f,1.069f,1.067f,1.067f
    };

    /** Sprite artwork coordinates, not fighter hitboxes or simulation sizes. */
    void draw(Canvas canvas,float x,float baseY,int facing,boolean damageFlash,boolean guardFlash) {
        CharacterDefinition.Animation anim=character.animation(motion.clip);
        int frame=motion.frame();
        spritePaint.setColorFilter(damageFlash?hitFlash:guardFlash?blockFlash:tint);
        boolean mirror=facing*character.artFacing<0;
        if(mirror) {canvas.save();canvas.scale(-1f,1f,x,0f);}
        boolean p01="player_base".equals(character.id)||"p01_training".equals(character.id);

        if(p01 && SpriteStates.CROUCH.equals(motion.clip)) {
            // Widening of the painted figure across CROUCH 0..7 is compensated.
            // Result: occupied width stays ~302px in authored sprite coordinates.
            float k=P01_CROUCH[Math.min(frame,P01_CROUCH.length-1)];
            drawAtlas(canvas,anim.atlas,frame,x,baseY,k,k,255);
        } else if(p01 && SpriteStates.RISE.equals(motion.clip)) {
            int i=Math.min(frame,P01_RISE_X.length-1);
            float fade=clamp01((i-3f)/2f);
            if(fade<1f)drawAtlas(canvas,anim.atlas,frame,x,baseY,
                P01_RISE_X[i],P01_RISE_Y[i],Math.round(255*(1f-fade)));
            if(fade>0f)drawP01Idle(canvas,x,baseY,Math.round(255*fade));
        } else if(p01 && SpriteStates.INTRO.equals(motion.clip)) {
            // Pose after frame 21 is 319px wide/420px tall versus the IDLE
            // 302px/448px. Correct axes independently before the handoff.
            // The last INTRO frame displays the ACTUAL IDLE bitmap, unchanged.
            float fade=clamp01((frame-22f)/4f);
            if(fade<1f) {
                int i=Math.max(0,Math.min(6,frame-20));
                float ox=frame>=22 ? -3.8f : 0f;
                float oy=frame>=22 ? -3.0f : 0f;
                drawAtlas(canvas,anim.atlas,frame,x+ox,baseY+oy,
                    P01_INTRO_X[i],P01_INTRO_Y[i],Math.round(255*(1f-fade)));
            }
            if(fade>0f) drawP01Idle(canvas,x,baseY,Math.round(255*fade));
        } else {
            drawAtlas(canvas,anim.atlas,frame,x,baseY,1f,1f,255);
        }

        spritePaint.setAlpha(255);
        if(mirror)canvas.restore();
    }

    private void drawP01Idle(Canvas canvas,float x,float baseY,int alpha) {
        CharacterDefinition.Animation idle=character.animation(SpriteStates.IDLE);
        drawAtlas(canvas,idle.atlas,idle.frame(0f,0f),x,baseY,1f,1f,alpha);
    }

    private void drawAtlas(Canvas canvas,CharacterDefinition.Atlas a,int frame,float x,
                           float baseY,float sizeX,float sizeY,int alpha) {
        if(alpha<=0)return;
        int col=frame%a.columns,row=frame/a.columns;
        source.set(col*a.width,row*a.height,(col+1)*a.width,(row+1)*a.height);
        float scale=character.profile.worldScale/a.pixelScale;
        float widthScale=scale*sizeX,heightScale=scale*sizeY;
        float left=x-a.rootX*widthScale,top=baseY-a.rootY*heightScale;
        destination.set(left,top,left+a.width*widthScale,top+a.height*heightScale);
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
