package com.gamelutagpt;

import android.content.Context;
import android.graphics.*;
import com.gamelutagpt.motion.Pose;

/** Generic atlas renderer. No attack IDs, resource IDs or per-clip branches. */
final class SpriteFighterRenderer {
    final SpriteMotion motion;
    /** Motor de animação (motion-core): o movimento por cima dos quadros. */
    final AnimacaoLutador animacao = new AnimacaoLutador();
    private final SpriteAtlasCache atlases;
    private CharacterDefinition character;
    private final Rect source=new Rect();
    /** Caixa do quadro atual na folha (CharacterDefinition.Atlas#frame). */
    private final int[] box=new int[6];
    private final RectF destination=new RectF();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ColorFilter hitFlash=new PorterDuffColorFilter(Color.argb(150,255,255,255),PorterDuff.Mode.SRC_ATOP);
    private final ColorFilter blockFlash=new PorterDuffColorFilter(Color.rgb(205,240,255),PorterDuff.Mode.MULTIPLY);
    /** Rastro do motor de animação: o lutador clareado e azulado. */
    private final ColorFilter rastro=new ColorMatrixColorFilter(new ColorMatrix(new float[]{
        0.62f,0f,0f,0f,32f,
        0f,0.72f,0f,0f,58f,
        0f,0f,1f,0f,112f,
        0f,0f,0f,1f,0f}));
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
        atlases.preload(next);character=next;motion.setCharacter(next);animacao.zerar();
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
        0.9545f,0.9457f,0.9104f,0.9055f,0.8996f,0.8889f,0.8683f,0.8604f
    };
    private static final float[] P01_RISE_X = {
        0.8683f,0.8859f,0.9026f,0.9075f,0.9104f,0.9104f
    };
    private static final float[] P01_RISE_Y = {
        0.8673f,0.8722f,0.9016f,0.9408f,0.9800f,1.0143f
    };
    // Original atlas silhouettes change proportions across frames 20..26.
    // Align the final silhouette with idle's occupied pixels (not canvas width).
    // Idle is 2% smaller to match the final intro pose more closely.
    private static final float P01_IDLE_SCALE = 0.980f;
    // DASH atlas is ~538px wide in its broadest pose (idle is only 302px).
    // Trim the exaggerated drawn size without affecting travel or hitboxes.
    private static final float P01_DASH_X = 0.760f;
    private static final float P01_DASH_Y = 0.940f;
    // Standing part of the victory uses approximately the same on-screen
    // figure height as the first intro frame (before the action).
    private static final float P01_VICTORY_SCALE = 0.940f;
    // During the supplied victory sheet the fighter walks in during frames
    // 0..6. The clip starts at 7; its planted feet are offset ~47 atlas
    // pixels from the declared atlas root, so re-anchor them at world x.
    private static final float P01_VICTORY_FOOT_SHIFT = 24.0f;
    private static final float[] P01_INTRO_X = {
        0.965f,0.950f,0.940f,0.935f,0.931f,0.928f,0.928f
    };
    private static final float[] P01_INTRO_Y = {
        0.970f,1.010f,1.052f,1.050f,1.047f,1.045f,1.045f
    };
    private static final float[] P01_INTRO_OFFSET_X = {
        -12.9f,-10.3f,-10.7f,-11.0f,-11.3f,-11.6f,-11.6f
    };
    private static final float[] P01_INTRO_OFFSET_Y = {
        2.1f,0.0f,-4.1f,-4.1f,-3.4f,-3.14f,-3.14f
    };

    /** Transparência de tudo o que este renderer desenha (o reflexo no chão), 0..255. */
    private int alphaGlobal = 255;

    void setAlphaGlobal(int alpha) {alphaGlobal=Math.max(0,Math.min(255,alpha));}

    /** Filtro que vale por cima de tudo (a silhueta do impact frame), ou null. */
    private ColorFilter override;

    void setOverride(ColorFilter filter) {override=filter;}
    ColorFilter override() {return override;}

    /** Sprite artwork coordinates, not fighter hitboxes or simulation sizes. */
    void draw(Canvas canvas,float x,float baseY,int facing,boolean damageFlash,boolean guardFlash) {
        spritePaint.setColorFilter(override!=null?override:damageFlash?hitFlash:guardFlash?blockFlash:tint);
        Pose pose=animacao.pose();
        canvas.save();
        if(facing*character.artFacing<0) canvas.scale(-1f,1f,x,0f);
        aplicarPose(canvas,pose,x,baseY);
        drawSmear(canvas,pose,x,baseY);
        drawClip(canvas,motion.clip,motion.frame(),x,baseY,255);
        // Transição: o quadro do clipe de antes por cima, sumindo em poucos quadros.
        if(pose.clipeAnterior!=null && pose.alfaAnterior>0f && hasAnimation(pose.clipeAnterior)) {
            drawClip(canvas,pose.clipeAnterior,pose.quadroAnterior,x,baseY,Math.round(255*pose.alfaAnterior));
        }
        spritePaint.setAlpha(255);
        canvas.restore();
    }

    /**
     * Rastro do motor de animação: cópias dos quadros recentes, translúcidas e tingidas, onde o
     * lutador estava (desenhe antes do lutador, sem as transformações dele).
     */
    void drawRastro(Canvas canvas) {
        Pose pose=animacao.pose();
        if(pose.fantasmas==0)return;
        spritePaint.setColorFilter(rastro);
        for(int i=0;i<pose.fantasmas;i++) {
            Pose.Fantasma f=pose.rastro[i];
            if(f.alfa<=0f||!hasAnimation(f.clipe))continue;
            canvas.save();
            if(f.olhando*character.artFacing<0) canvas.scale(-1f,1f,f.x,0f);
            drawClip(canvas,f.clipe,f.quadro,f.x,f.y,Math.round(255*f.alfa));
            canvas.restore();
        }
        spritePaint.setAlpha(255);
        spritePaint.setColorFilter(tint);
    }

    /**
     * Achatar/esticar, inclinação e arrasto em volta da raiz (os pés), no espaço da arte (já
     * espelhada).
     */
    private void aplicarPose(Canvas canvas,Pose pose,float x,float baseY) {
        if(pose.neutra())return;
        // Na arte, "para a frente" é o lado para onde ela foi desenhada olhando.
        float frente=character.artFacing;
        if(pose.deslocX!=0f||pose.deslocY!=0f) canvas.translate(pose.deslocX*frente,pose.deslocY);
        if(pose.inclinacao!=0f) canvas.rotate(pose.inclinacao*frente,x,baseY);
        if(pose.arrasto!=0f) {
            // Cisalhamento com os pés parados: quanto mais alto, mais anda (y acima dos pés é negativo).
            canvas.translate(x,baseY);
            canvas.skew(-frente*(float)Math.tan(Math.toRadians(pose.arrasto)),0f);
            canvas.translate(-x,-baseY);
        }
        if(pose.escalaX!=1f||pose.escalaY!=1f) canvas.scale(pose.escalaX,pose.escalaY,x,baseY);
    }

    /**
     * Smear do golpe forte: duas cópias do quadro esticadas na horizontal em volta dos pés, por
     * baixo do desenho. Só a ponta que passa do corpo aparece (o punho, o pé), translúcida.
     */
    private void drawSmear(Canvas canvas,Pose pose,float x,float baseY) {
        if(pose.smear<=0f||pose.smearEsticar<=0f||override!=null)return;
        for(int i=2;i>=1;i--) {
            canvas.save();
            canvas.scale(1f+pose.smearEsticar*i/2f,1f,x,baseY);
            drawClip(canvas,motion.clip,motion.frame(),x,baseY,Math.round(255*pose.smear*(i==2?0.25f:0.45f)));
            canvas.restore();
        }
    }

    /** Um quadro de um clipe com as calibrações do p01 (sem espelhar nem a pose). */
    private void drawClip(Canvas canvas,String clip,int frame,float x,float baseY,int alpha) {
        CharacterDefinition.Animation anim=character.animation(clip);
        boolean p01="player_base".equals(character.id)||"p01_training".equals(character.id);

        if(p01 && SpriteStates.IDLE.equals(clip)) {
            drawAtlas(canvas,anim.atlas,frame,x,baseY,
                P01_IDLE_SCALE,P01_IDLE_SCALE,alpha);
        } else if(p01 && SpriteStates.DASH.equals(clip)) {
            // The running pose can extend its limbs, but never enlarges
            // the fighter relative to the canonical idle body.
            drawP01Calibrated(canvas,anim,frame,x,baseY,alpha);
        } else if(p01 && SpriteStates.VICTORY.equals(clip)) {
            // Per-frame center and feet stay on the same world root, even
            // when the original victory video shifts the painted body.
            drawP01Calibrated(canvas,anim,frame,x,baseY,alpha);
        } else if(p01 && SpriteStates.CROUCH.equals(clip)) {
            // Widening of the painted figure across CROUCH 0..7 is compensated.
            // Result: occupied width stays ~302px in authored sprite coordinates.
            float k=P01_CROUCH[Math.min(frame,P01_CROUCH.length-1)];
            drawAtlas(canvas,anim.atlas,frame,x,baseY,k,k,alpha);
        } else if(p01 && SpriteStates.RISE.equals(clip)) {
            int i=Math.min(frame,P01_RISE_X.length-1);
            float fade=clamp01((i-3f)/2f);
            if(fade<1f)drawAtlas(canvas,anim.atlas,frame,x,baseY,
                P01_RISE_X[i],P01_RISE_Y[i],Math.round(alpha*(1f-fade)));
            if(fade>0f)drawP01Idle(canvas,x,baseY,Math.round(alpha*fade));
        } else if(p01 && SpriteStates.INTRO.equals(clip)) {
            // The intro's natural silhouette changes from narrow to a
            // fighting stance: normalize every frame, then crossfade to idle.
            float fade=clamp01((frame-22f)/4f);
            if(fade<1f) drawP01Calibrated(canvas,anim,frame,x,baseY,
                Math.round(alpha*(1f-fade)));
            if(fade>0f) drawP01Idle(canvas,x,baseY,Math.round(alpha*fade));
        } else if(p01) {
            // Every other P01 state, including all attacks, hit, air, guard,
            // recovery, dash, throw, victory and taunt, uses the same master.
            drawP01Calibrated(canvas,anim,frame,x,baseY,alpha);
        } else {
            drawAtlas(canvas,anim.atlas,frame,x,baseY,1f,1f,alpha);
        }
    }

    private void drawP01Calibrated(Canvas canvas,CharacterDefinition.Animation anim,
                                    int frame,float x,float baseY,int alpha) {
        float[] k=P01SpriteCalibration.get(anim.atlas.resource,frame);
        drawAtlas(canvas,anim.atlas,frame,x+k[2],baseY+k[3],k[0],k[1],alpha);
    }

    private void drawP01Idle(Canvas canvas,float x,float baseY,int alpha) {
        CharacterDefinition.Animation idle=character.animation(SpriteStates.IDLE);
        drawAtlas(canvas,idle.atlas,idle.frame(0f,0f),x,baseY,P01_IDLE_SCALE,P01_IDLE_SCALE,alpha);
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

    private static float clamp01(float v){return Math.max(0f,Math.min(1f,v));}
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
