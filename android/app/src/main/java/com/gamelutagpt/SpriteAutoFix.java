package com.gamelutagpt;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime auto-fixes produced by tools/sprites/autoestabilizar.py.
 * No destructive bitmap edits. Zero impact on hitboxes/gameplay. No-op unless
 * a valid, character-scoped asset explicitly supplies a correction.
 */
final class SpriteAutoFix {
    static final class Adjustment {
        final float size, dx, dy;
        Adjustment(float size,float dx,float dy) {
            this.size=size; this.dx=dx; this.dy=dy;
        }
    }
    private static final Adjustment NONE=new Adjustment(1f,0f,0f);
    private static final Map<String,Adjustment> values=new HashMap<>();
    /** Folhas com alguma correção: as outras (quase todas) nem montam a chave a cada desenho. */
    private static final java.util.Set<String> atlasComAjuste=new java.util.HashSet<>();
    private static boolean loaded;

    private static String key(String character,String atlas,int frame) {
        return character+"|"+atlas+"|"+frame;
    }

    static synchronized void load(Context context) {
        if(loaded) return;
        loaded=true;
        try(InputStream stream=context.getAssets().open("sprite-autofix.json")) {
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            byte[] bytes=new byte[8192];
            int n;
            while((n=stream.read(bytes))!=-1)buffer.write(bytes,0,n);
            replaceFromJson(new String(buffer.toByteArray(), "UTF-8"));
        } catch(Exception unused) {
            // No accepted corrections is a valid state. Existing gameplay
            // and sprites must remain identical in that situation.
        }
    }

    static synchronized void replaceFromJson(String json) throws Exception {
        JSONObject data=new JSONObject(json);
        if(data.getInt("schema")!=1) throw new IllegalArgumentException("schema");
        JSONArray entries=data.getJSONArray("corrections");
        Map<String,Adjustment> next=new HashMap<>();
        for(int i=0;i<entries.length();i++) {
            JSONObject entry=entries.getJSONObject(i);
            String character=entry.getString("character");
            String atlas=entry.getString("atlas");
            if(!character.matches("[a-z][a-z0-9_]{1,50}") ||
               !atlas.matches("[a-z][a-z0-9_]{1,90}"))
                throw new IllegalArgumentException("invalid character/atlas");
            JSONArray frames=entry.getJSONArray("frames");
            for(int j=0;j<frames.length();j++) {
                JSONArray row=frames.getJSONArray(j);
                if(row.length()!=4)throw new IllegalArgumentException("row");
                int frame=row.getInt(0);
                float scale=(float)row.getDouble(1);
                float dx=(float)row.getDouble(2);
                float dy=(float)row.getDouble(3);
                if(frame<0 || frame>5000 || !Float.isFinite(scale) ||
                   scale<.80f || scale>1.25f || !Float.isFinite(dx) ||
                   Math.abs(dx)>45f || !Float.isFinite(dy) || Math.abs(dy)>45f)
                    throw new IllegalArgumentException("unsafe sprite adjustment");
                String key=key(character,atlas,frame);
                if(next.put(key,new Adjustment(scale,dx,dy))!=null)
                    throw new IllegalArgumentException("duplicate correction");
            }
        }
        values.clear();
        values.putAll(next);
        atlasComAjuste.clear();
        for(String k:next.keySet()) {
            int a=k.indexOf('|'),b=k.lastIndexOf('|');
            atlasComAjuste.add(k.substring(a+1,b));
        }
    }

    static synchronized Adjustment get(String character,String atlas,int frame) {
        if(!atlasComAjuste.contains(atlas))return NONE;
        Adjustment result=values.get(key(character,atlas,frame));
        return result==null?NONE:result;
    }

    static synchronized int count() { return values.size(); }
    private SpriteAutoFix() {}
}
