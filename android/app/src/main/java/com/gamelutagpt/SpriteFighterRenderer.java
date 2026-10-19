package com.gamelutagpt;

import android.content.Context;
import android.graphics.*;

/** Generic atlas renderer. No attack IDs, resource IDs or per-clip branches. */
final class SpriteFighterRenderer {
    final SpriteMotion motion;
    private final SpriteAtlasCache atlases;
    private CharacterDefinition character;
    private final Rect source=new Rect();
    /** Caixa do quadro atual na folha (CharacterDefinition.Atlas#frame). */
    private final int[] box=new int[6];
    private final RectF destination=new RectF();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ColorFilter hitFlash=new PorterDuffColorFilter(Color.argb(150,255,255,255),PorterDuff.Mode.SRC_ATOP);
    private final ColorFilter blockFlash=new PorterDuffColorFilter(Color.rgb(205,240,255),PorterDuff.Mode.MULTIPLY);
    /** Base color filter when no flash is showing (the p06 shadow echo), or null. */
    private ColorFilter tint;

    /** Standalone renderer with its own cache (tools and tests). */
    SpriteFighterRenderer(Context context) { this(context,GeneratedCharacters.defaultCharacter()); }
    SpriteFighterRenderer(Context context,CharacterDefinition character) { this(new SpriteAtlasCache(context),character); }

    /** Match renderer: the owner preloads every pack it may show into the shared cache. */
    SpriteFighterRenderer(SpriteAtlasCache atlases,CharacterDefinition character) {
        this(atlases,character,true);
    }

    /**
     * {@code carregar} false: só monta o renderer; as folhas ficam para quem montar a luta
     * ({@link SpriteAtlasCache#preload}), que pode estar antecipando em segundo plano.
     */
    SpriteFighterRenderer(SpriteAtlasCache atlases,CharacterDefinition character,boolean carregar) {
        this.atlases=atlases;this.character=character;
        if(carregar)atlases.preload(character);
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
        update(dt,grounded,crouching,velocityY,travel,forward,dash,backdash,attackAnimation,attackElapsed,combat,locked,false);
    }
    void update(float dt,boolean grounded,boolean crouching,float velocityY,float travel,
                boolean forward,boolean dash,boolean backdash,String attackAnimation,
                float attackElapsed,boolean combat,boolean locked,boolean braking) {
        motion.update(dt,grounded,crouching,velocityY,travel,forward,dash,backdash,attackAnimation,attackElapsed,combat,locked,braking);
    }
    /** Transparência de tudo o que este renderer desenha (o reflexo no chão), 0..255. */
    private int alphaGlobal = 255;

    void setAlphaGlobal(int alpha) {alphaGlobal=Math.max(0,Math.min(255,alpha));}

    /** Filtro que vale por cima de tudo (a silhueta do impact frame), ou null. */
    private ColorFilter override;

    void setOverride(ColorFilter filter) {override=filter;}
    ColorFilter override() {return override;}

    /**
     * Draws the current frame with its root at (x, baseY). {@code facing} is the world
     * direction the fighter looks at; mirroring follows the pack's declared art facing.
     * Sprite artwork coordinates, not fighter hitboxes or simulation sizes.
     */
    void draw(Canvas canvas,float x,float baseY,int facing,boolean damageFlash,boolean guardFlash) {
        CharacterDefinition.Animation anim=character.animation(motion.clip);
        int frame=motion.frame();
        spritePaint.setColorFilter(override!=null?override:damageFlash?hitFlash:guardFlash?blockFlash:tint);
        boolean mirror=facing*character.artFacing<0;
        if(mirror) {canvas.save();canvas.scale(-1f,1f,x,0f);}
        // Todo personagem é desenhado igual: a folha no tamanho em que foi feita, em volta da raiz.
        drawAtlas(canvas,anim.atlas,frame,x,baseY,1f,1f,255);

        spritePaint.setAlpha(255);
        if(mirror)canvas.restore();
    }

    private void drawAtlas(Canvas canvas,CharacterDefinition.Atlas a,int frame,float x,
                           float baseY,float sizeX,float sizeY,int alpha) {
        if(alpha<=0||alphaGlobal<=0)return;
        if(!a.frame(frame,box))return;
        SpriteAtlasCache.Folha folha=atlases.get(character,a);
        // A caixa do quadro na folha (pixels da folha original) e onde ela fica na célula; com a
        // folha reduzida, a fonte é a mesma caixa no bitmap menor (arredondada para pixels
        // inteiros) e o destino é o que essa fonte cobre.
        float boxLeft=box[0],boxTop=box[1];
        source.set(Math.round(boxLeft*folha.escalaX),Math.round(boxTop*folha.escalaY),
            Math.round((boxLeft+box[2])*folha.escalaX),Math.round((boxTop+box[3])*folha.escalaY));
        float srcLeft=box[4]+source.left/folha.escalaX-boxLeft,srcTop=box[5]+source.top/folha.escalaY-boxTop;
        float srcRight=box[4]+source.right/folha.escalaX-boxLeft,srcBottom=box[5]+source.bottom/folha.escalaY-boxTop;
        float scale=character.profile.worldScale/a.pixelScale;
        // Accepted corrections are applied AFTER legacy calibrations and
        // debug tuning, so the engine really renders the corrected image.
        SpriteAutoFix.Adjustment fix=SpriteAutoFix.get(character.id,a.resource,frame);
        // DEBUG > AJUSTAR TAMANHOS: prévia em volta da raiz até o tamanho ser gravado na folha.
        float tuned=AjusteTamanho.scale(a);
        float widthScale=scale*sizeX*fix.size*tuned;
        float heightScale=scale*sizeY*fix.size*tuned;
        float left=x+fix.dx*scale-a.rootX*widthScale;
        float top=baseY+fix.dy*scale-a.rootY*heightScale;
        destination.set(left+srcLeft*widthScale,top+srcTop*heightScale,
            left+srcRight*widthScale,top+srcBottom*heightScale);
        spritePaint.setAlpha(alphaGlobal==255?alpha:alpha*alphaGlobal/255);
        canvas.drawBitmap(folha.bitmap,source,destination,spritePaint);
    }

    /** Um quadro de uma folha com a raiz em (x, baseY), sem calibração nem flash (ferramentas de conferência). */
    void drawAtlasFrame(Canvas canvas,CharacterDefinition.Atlas atlas,int frame,float x,float baseY) {
        spritePaint.setColorFilter(null);
        drawAtlas(canvas,atlas,frame,x,baseY,1f,1f,255);
    }

    void setTint(ColorFilter tint){this.tint=tint;}
    /** Sombra roxa e translúcida do próprio lutador (o eco de sombra da p06). */
    static ColorFilter shadowEcho() {
        return new ColorMatrixColorFilter(new ColorMatrix(new float[]{
            0.25f,0.10f,0.10f,0f,70f,
            0.05f,0.10f,0.05f,0f,20f,
            0.30f,0.15f,0.35f,0f,110f,
            0f,0f,0f,0.55f,0f}));
    }
    /** Mostra um clipe num instante dado, sem a simulação (tela de ajuste de tamanho). */
    void pose(String clip,float time,float distance){motion.clip=clip;motion.time=time;motion.distance=distance;}
    boolean hasAnimation(String id){return character.animations.containsKey(id);}
    CharacterDefinition character(){return character;}
    CharacterVisualProfile visualProfile(){return character.profile;}
}
