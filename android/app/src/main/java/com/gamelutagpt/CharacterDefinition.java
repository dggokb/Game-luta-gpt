package com.gamelutagpt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable runtime data compiled from characters/<id>/character.json. */
final class CharacterDefinition {
    static final class Atlas {
        final String resource;
        final int width, height, rootX, rootY, columns, count;
        Atlas(String resource,int width,int height,int rootX,int rootY,int columns,int count) {
            this.resource=resource;this.width=width;this.height=height;
            this.rootX=rootX;this.rootY=rootY;this.columns=columns;this.count=count;
        }
    }
    static final class Animation {
        final String id;
        final Atlas atlas;
        private final int[] frames;
        private final float[] durations;
        final boolean loop;
        final float distancePerFrame, duration;
        Animation(String id,Atlas atlas,int[] frames,float[] durations,boolean loop,float distancePerFrame) {
            this.id=id;this.atlas=atlas;this.frames=frames.clone();this.durations=durations.clone();
            this.loop=loop;this.distancePerFrame=distancePerFrame;
            float sum=0;for(float d:durations)sum+=d;this.duration=sum;
        }
        int frame(float time,float distance) {
            if(distancePerFrame>0) return frames[(int)(Math.max(0,distance)/distancePerFrame)%frames.length];
            float t=Math.max(0,time);
            if(loop)t%=duration;
            for(int i=0;i<frames.length-1;i++) { if(t<durations[i])return frames[i];t-=durations[i]; }
            return frames[frames.length-1];
        }
    }
    static final class Move {
        final Animation animation;
        final int damage;
        final float activeStart,activeEnd,reach;
        Move(Animation animation,int damage,float activeStart,float activeEnd,float reach) {
            this.animation=animation;this.damage=damage;this.activeStart=activeStart;this.activeEnd=activeEnd;this.reach=reach;
        }
        boolean active(float elapsed) { return elapsed+0.000001f>=activeStart && elapsed<activeEnd; }
    }
    final String id,displayName;
    final CharacterVisualProfile profile;
    final Map<String,Animation> animations;
    final Map<String,Move> moves;
    CharacterDefinition(String id,String displayName,CharacterVisualProfile profile,
                        Map<String,Animation> animations,Map<String,Move> moves) {
        this.id=id;this.displayName=displayName;this.profile=profile;
        this.animations=Collections.unmodifiableMap(new LinkedHashMap<>(animations));
        this.moves=Collections.unmodifiableMap(new LinkedHashMap<>(moves));
    }
    Animation animation(String id) {
        Animation value=animations.get(id);
        if(value==null)throw new IllegalArgumentException("Unknown animation "+this.id+"/"+id);
        return value;
    }
}
