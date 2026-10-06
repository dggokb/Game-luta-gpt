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
        /** Maps a gameplay clock of the given length onto this clip, keeping frame proportions. */
        float timeFor(float elapsed,float gameplayDuration) {
            if(gameplayDuration<=0f)return elapsed;
            return Math.max(0f,elapsed)*duration/gameplayDuration;
        }
    }
    /**
     * Frame data of one input. Gameplay timing (totalTime/active window) belongs to the
     * move; the animation, when present, is stretched to fit it. A move without art keeps
     * a declared body pose instead.
     */
    static final class Move {
        final String binding;
        final Animation animation;
        final String pose;
        final int damage;
        final float totalTime,activeStart,activeEnd,reach,hitHeight;
        Move(String binding,Animation animation,String pose,int damage,float totalTime,
             float activeStart,float activeEnd,float reach,float hitHeight) {
            this.binding=binding;this.animation=animation;this.pose=pose;this.damage=damage;
            this.totalTime=totalTime;this.activeStart=activeStart;this.activeEnd=activeEnd;
            this.reach=reach;this.hitHeight=hitHeight;
        }
        boolean active(float elapsed) { return elapsed+0.000001f>=activeStart && elapsed<activeEnd; }
        /** Window where a defender sees the strike coming (used by anticipated guard). */
        boolean threatening(float elapsed) {
            float lead=0.16f*totalTime;
            return elapsed>=activeStart-lead && elapsed<=activeEnd+lead;
        }
        float animationTime(float elapsed) { return animation==null?0f:animation.timeFor(elapsed,totalTime); }
    }
    static final class Projectile {
        final int damage;
        final float range,speed;
        /** Launch point: forward offset from the root and heights above the ground. */
        final float spawnX,spawnY,crouchSpawnY,airSpawnY;
        Projectile(int damage,float range,float speed,float spawnX,float spawnY,float crouchSpawnY,float airSpawnY) {
            this.damage=damage;this.range=range;this.speed=speed;
            this.spawnX=spawnX;this.spawnY=spawnY;this.crouchSpawnY=crouchSpawnY;this.airSpawnY=airSpawnY;
        }
        float spawnHeight(boolean crouching,boolean airborne) {
            return crouching ? crouchSpawnY : airborne ? airSpawnY : spawnY;
        }
    }
    /** Gameplay hurtbox in world units, independent of the PNG canvas. */
    static final class Body {
        final float halfWidth,standHeight,crouchHeight;
        /** Pushbox: keeps two bodies from overlapping; low enough to be jumped over. */
        final float pushHalfWidth,pushHeight;
        Body(float halfWidth,float standHeight,float crouchHeight,float pushHalfWidth,float pushHeight) {
            this.halfWidth=halfWidth;this.standHeight=standHeight;this.crouchHeight=crouchHeight;
            this.pushHalfWidth=pushHalfWidth;this.pushHeight=pushHeight;
        }
        float height(boolean crouching) { return crouching?crouchHeight:standHeight; }
    }
    /** Fighter rules that used to live in GameView.FighterProfile. */
    static final class Fighter {
        final int color,maxLife;
        final String[] autoCombo;
        final Projectile energy,superAttack;
        final int[] energyCommand;
        final Body body;
        Fighter(int color,int maxLife,String[] autoCombo,Projectile energy,
                int[] energyCommand,Projectile superAttack,Body body) {
            this.color=color;this.maxLife=maxLife;
            this.autoCombo=autoCombo.clone();this.energy=energy;this.energyCommand=energyCommand.clone();
            this.superAttack=superAttack;this.body=body;
        }
        boolean hasEnergyAttack() { return energy!=null; }
        boolean hasSuperAttack() { return superAttack!=null; }
    }
    final String id,displayName;
    final CharacterVisualProfile profile;
    /** +1 when the art faces right, -1 when it faces left. */
    final int artFacing;
    /** Opaque height above the root in world units, measured by the importer. */
    final float visualStandHeight,visualCrouchHeight;
    final Fighter fighter;
    final Map<String,Animation> animations;
    final Map<String,Move> moves;
    final Map<String,Animation> specialAnimations;
    CharacterDefinition(String id,String displayName,CharacterVisualProfile profile,int artFacing,
                        float visualStandHeight,float visualCrouchHeight,Fighter fighter,
                        Map<String,Animation> animations,Map<String,Move> moves,
                        Map<String,Animation> specialAnimations) {
        this.id=id;this.displayName=displayName;this.profile=profile;this.artFacing=artFacing;
        this.visualStandHeight=visualStandHeight;this.visualCrouchHeight=visualCrouchHeight;this.fighter=fighter;
        this.animations=Collections.unmodifiableMap(new LinkedHashMap<>(animations));
        this.moves=Collections.unmodifiableMap(new LinkedHashMap<>(moves));
        this.specialAnimations=Collections.unmodifiableMap(new LinkedHashMap<>(specialAnimations));
    }
    /** Copy with different animations; used by tests and tooling. */
    CharacterDefinition withAnimations(String id,Map<String,Animation> animations) {
        return new CharacterDefinition(id,id,profile,artFacing,visualStandHeight,visualCrouchHeight,
            fighter,animations,moves,specialAnimations);
    }
    Animation animation(String id) {
        Animation value=animations.get(id);
        if(value==null)throw new IllegalArgumentException("Unknown animation "+this.id+"/"+id);
        return value;
    }
    /**
     * Resolves the move for an input. Airborne attacks use the jL/jM/jH bindings; every
     * binding is mandatory in the pack, so this never falls back silently.
     */
    Move move(String type,boolean airborne) {
        String binding=airborne ? "j"+(type.startsWith("2")?type.substring(1):type) : type;
        return moves.get(binding);
    }
}
