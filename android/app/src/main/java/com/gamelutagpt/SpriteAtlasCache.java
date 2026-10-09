package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Decode sprite atlases on demand, not on the Activity's UI startup thread.
 *
 * Older builds eagerly decoded BOTH full characters (~hundreds of MiB) before
 * showing any screen. Some Android phones terminate the process on launch.
 * The LRU only evicts after unlockCanvasAndPost: a hardware Canvas may still
 * hold Bitmap references until the frame is submitted.
 *
 * Compression/PNG pixels are unchanged; the original resolution is preferred.
 * On actual allocation failure only, retry a half-size bitmap so the game
 * can remain playable. Geometry is scaled by SpriteFighterRenderer.
 */
final class SpriteAtlasCache {
    private static final String LOG_TAG="SpriteAtlasCache";
    private static final long MIB=1024L*1024L;
    private final Context context;
    private final LinkedHashMap<String,Bitmap> sheets=
        new LinkedHashMap<>(24,.75f,true);
    private final long budgetBytes;

    SpriteAtlasCache(Context context) {
        this.context=context;
        long heapLimit=Runtime.getRuntime().maxMemory();
        // Per-frame safe budget for textures on a mobile heap. A PC renderer
        // can retain full resolution independently; this policy is Android-only.
        budgetBytes=Math.max(32L*MIB,Math.min(96L*MIB,heapLimit/3));
        Log.i(LOG_TAG,"lazy atlas mode, heap "+(heapLimit/MIB)+
            " MiB, cache budget "+(budgetBytes/MIB)+" MiB");
    }

    /**
     * Does NOT decode all animation textures. Called when switching fighter
     * packs; textures will be loaded when the actual pose is first drawn.
     */
    void preload(CharacterDefinition definition) {
        if(definition==null)throw new IllegalArgumentException("Missing fighter");
    }

    Bitmap get(CharacterDefinition.Atlas atlas) {
        Bitmap image=sheets.get(atlas.resource);
        if(image==null) {
            image=decode(atlas);
            sheets.put(atlas.resource,image);
            Log.d(LOG_TAG,"loaded "+atlas.resource+" "+image.getWidth()+"x"+
                image.getHeight()+" cache="+(decodedBytes()/MIB)+" MiB");
        }
        return image;
    }

    int size() { return sheets.size(); }
    long decodedBytes() {
        long bytes=0;
        for(Bitmap bitmap:sheets.values()) bytes+=bitmap.getAllocationByteCount();
        return bytes;
    }
    long budgetBytes() { return budgetBytes; }

    /** Call AFTER posting the canvas; never recycle an in-flight bitmap. */
    void trimAfterFrame() {
        if(decodedBytes()<=budgetBytes || sheets.size()<2)return;
        long used=decodedBytes();
        Iterator<Map.Entry<String,Bitmap>> it=sheets.entrySet().iterator();
        while(used>budgetBytes && sheets.size()>1 && it.hasNext()) {
            Map.Entry<String,Bitmap> e=it.next();
            Bitmap b=e.getValue();
            used-=b.getAllocationByteCount();
            it.remove();
            b.recycle();
        }
    }

    /** Team change: drop the retired character's art after last frame posted. */
    void retainOnly(CharacterDefinition... characters) {
        Set<String> used=new HashSet<>();
        for(CharacterDefinition c:characters)
            if(c!=null)for(CharacterDefinition.Animation a:c.animations.values())
                used.add(a.atlas.resource);
        for(Iterator<Map.Entry<String,Bitmap>> it=sheets.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,Bitmap> e=it.next();
            if(!used.contains(e.getKey())) {
                e.getValue().recycle();it.remove();
            }
        }
    }

    void release() {
        for(Bitmap b:sheets.values())b.recycle();
        sheets.clear();
    }

    private Bitmap decode(CharacterDefinition.Atlas atlas) {
        int id=context.getResources().getIdentifier(
            atlas.resource,"drawable",context.getPackageName());
        if(id==0)throw new IllegalStateException("Missing atlas "+atlas.resource);
        try {
            return load(atlas,id,1);
        } catch(OutOfMemoryError oom) {
            // Rare fallback; keeping the app launchable is more important than
            // loading another full-res atlas into an exhausted mobile heap.
            Log.e(LOG_TAG,"OOM decoding "+atlas.resource+" at full size. Retrying at 1/2",oom);
            release();
            System.gc();
            return load(atlas,id,2);
        }
    }

    private Bitmap load(CharacterDefinition.Atlas a,int id,int sample) {
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inScaled=false;
        options.inSampleSize=sample;
        Bitmap result=BitmapFactory.decodeResource(context.getResources(),id,options);
        if(result==null)throw new IllegalStateException("Cannot decode "+a.resource);
        result.setDensity(Bitmap.DENSITY_NONE);
        int expectedW=a.columns*a.width;
        int expectedH=((a.count+a.columns-1)/a.columns)*a.height;
        if(Math.abs(result.getWidth()*sample-expectedW)>sample ||
                Math.abs(result.getHeight()*sample-expectedH)>sample) {
            result.recycle();
            throw new IllegalStateException("Invalid atlas dimensions: "+a.resource);
        }
        return result;
    }
}
