package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Single owner of decoded atlases. Every renderer of a match shares one cache, so an
 * atlas used by several fighters (or renderers) is decoded exactly once.
 */
final class SpriteAtlasCache {
    private final Context context;
    private final Map<String,Bitmap> sheets=new HashMap<>();

    SpriteAtlasCache(Context context) { this.context=context; }

    /** Decodes every atlas of a character before gameplay; never called per frame. */
    void preload(CharacterDefinition definition) {
        for(CharacterDefinition.Animation animation:definition.animations.values()) load(animation.atlas);
    }

    Bitmap get(CharacterDefinition.Atlas atlas) {
        Bitmap bitmap=sheets.get(atlas.resource);
        if(bitmap==null)throw new IllegalStateException("Atlas not preloaded: "+atlas.resource);
        return bitmap;
    }

    int size() { return sheets.size(); }

    /** Frees every atlas the given characters do not use (team change: ~135 MB each). */
    void retainOnly(CharacterDefinition... characters) {
        Set<String> used=new HashSet<>();
        for(CharacterDefinition c:characters) for(CharacterDefinition.Animation a:c.animations.values()) used.add(a.atlas.resource);
        for(Iterator<Map.Entry<String,Bitmap>> it=sheets.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,Bitmap> e=it.next();
            if(!used.contains(e.getKey())) { e.getValue().recycle(); it.remove(); }
        }
    }

    private void load(CharacterDefinition.Atlas a) {
        Bitmap bitmap=sheets.get(a.resource);
        if(bitmap==null) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
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
