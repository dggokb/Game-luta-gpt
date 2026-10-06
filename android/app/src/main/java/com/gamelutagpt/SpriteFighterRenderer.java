package com.gamelutagpt;

import android.content.Context;
import android.graphics.*;
import java.util.HashMap;
import java.util.Map;

/** Generic atlas renderer. No attack IDs, resource IDs or per-clip branches. */
final class SpriteFighterRenderer {
    final SpriteMotion motion;
    private final Context context;
    private CharacterDefinition character;
    private CharacterVisualProfile profile;
    private final Map<String,Bitmap> sheets=new HashMap<>();
    private final Rect source=new Rect();
    private final RectF destination=new RectF();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ColorFilter hitFlash=new PorterDuffColorFilter(Color.argb(150,255,255,255),PorterDuff.Mode.SRC_ATOP);
    private final ColorFilter blockFlash=new PorterDuffColorFilter(Color.rgb(205,240,255),PorterDuff.Mode.MULTIPLY);
    SpriteFighterRenderer(Context context) { this(context,GeneratedCharacters.defaultCharacter()); }
    SpriteFighterRenderer(Context context,CharacterDefinition character) {
        this.context=context;this.character=character;this.profile=character.profile;
        motion=new SpriteMotion(character);preload(character);
        // Team assets decode once, before gameplay; tag never decodes on a frame.
        for(String id:GeneratedCharacters.TEAM)preload(GeneratedCharacters.get(id));
    }
    SpriteFighterRenderer(Context context,CharacterVisualProfile profile) {
        this(context);this.profile=profile;
    }
    void setCharacter(String id) {
        CharacterDefinition next=GeneratedCharacters.get(id);
        if(next==character)return;
        preload(next);character=next;profile=next.profile;motion.setCharacter(next);
    }
    private void preload(CharacterDefinition definition) {
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        for(CharacterDefinition.Animation animation:definition.animations.values()) {
            CharacterDefinition.Atlas a=animation.atlas;
            Bitmap bitmap=sheets.get(a.resource);
            if(bitmap==null) {
                int id=context.getResources().getIdentifier(a.resource,"drawable",context.getPackageName());
                if(id==0)throw new IllegalStateException("Missing atlas "+a.resource);
                bitmap=BitmapFactory.decodeResource(context.getResources(),id,options);
                if(bitmap==null)throw new IllegalStateException("Cannot decode "+a.resource);
                bitmap.setDensity(Bitmap.DENSITY_NONE);sheets.put(a.resource,bitmap);
            }
            if(bitmap.getWidth()!=a.columns*a.width || bitmap.getHeight()!=((a.count+a.columns-1)/a.columns)*a.height)
                throw new IllegalStateException("Invalid atlas dimensions: "+a.resource);
        }
    }
    void update(float dt,boolean grounded,boolean crouching,float velocityY,float travel,
                boolean forward,boolean dash,boolean backdash,String attackAnimation,
                float attackElapsed,boolean combat,boolean locked) {
        motion.update(dt,grounded,crouching,velocityY,travel,forward,dash,backdash,attackAnimation,attackElapsed,combat,locked);
    }
    void draw(Canvas canvas,Paint ignored,float x,float baseY,boolean damageFlash,boolean guardFlash) {
        CharacterDefinition.Atlas a=character.animation(motion.clip).atlas;
        int frame=motion.frame(),col=frame%a.columns,row=frame/a.columns;
        source.set(col*a.width,row*a.height,(col+1)*a.width,(row+1)*a.height);
        float scale=profile.worldScale,left=x-a.rootX*scale,top=baseY-a.rootY*scale;
        destination.set(left,top,left+a.width*scale,top+a.height*scale);
        spritePaint.setColorFilter(damageFlash?hitFlash:guardFlash?blockFlash:null);
        canvas.drawBitmap(sheets.get(a.resource),source,destination,spritePaint);
    }
    boolean hasAnimation(String id){return character.animations.containsKey(id);}
    CharacterVisualProfile visualProfile(){return profile;}
}
