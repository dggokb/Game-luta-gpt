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
        /**
         * Guard clips: the first frame is the held guard; the remaining frames are the
         * block reaction, stretched over the blockstun. progress is 0..1 of the blockstun.
         */
        float guardTime(boolean blocking,float progress) {
            if(!blocking||durations.length<2)return 0f;
            float hold=durations[0];
            return hold+Math.max(0f,Math.min(1f,progress))*(duration-hold-0.0001f);
        }
        /** Clip time that shows the given frame of the clip (its middle). */
        float timeOfFrame(int index) {
            int i=Math.max(0,Math.min(durations.length-1,index));
            float t=durations[i]*0.5f;
            for(int k=0;k<i;k++)t+=durations[k];
            return t;
        }
        /** Maps a gameplay clock of the given length onto this clip, keeping frame proportions. */
        float timeFor(float elapsed,float gameplayDuration) {
            if(gameplayDuration<=0f)return elapsed;
            return Math.max(0f,elapsed)*duration/gameplayDuration;
        }
    }
    /**
     * One input: its combat definition plus what the body shows. Gameplay timing comes
     * from the attack's frames; the animation, when present, is stretched to fit it. A move
     * without art keeps a declared body pose instead.
     */
    static final class Move {
        final String binding;
        final Animation animation;
        final String pose;
        final AttackDefinition attack;
        final int damage;
        /** Seconds, derived from the attack frames (animation stretch, AI, legacy checks). */
        final float totalTime,activeStart,activeEnd,reach,hitHeight;
        Move(String binding,Animation animation,String pose,AttackDefinition attack) {
            this.binding=binding;this.animation=animation;this.pose=pose;this.attack=attack;
            this.damage=attack.damage;
            this.totalTime=attack.totalFrames/(float)CombatConfig.FPS;
            this.activeStart=attack.startupFrames/(float)CombatConfig.FPS;
            this.activeEnd=(attack.startupFrames+attack.activeFrames)/(float)CombatConfig.FPS;
            this.reach=attack.reach;this.hitHeight=attack.hitHeight;
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
        /** The S/SUPER move that throws it; its stun data applies when the projectile hits. */
        final AttackDefinition attack;
        Projectile(AttackDefinition attack,float range,float speed,float spawnX,float spawnY,float crouchSpawnY,float airSpawnY) {
            this.attack=attack;this.damage=attack.damage;this.range=range;this.speed=speed;
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
        /** Which buffered press wins when several are valid (most important first). */
        final AttackDefinition.Strength[] inputPriority;
        Fighter(int color,int maxLife,String[] autoCombo,Projectile energy,
                int[] energyCommand,Projectile superAttack,Body body,AttackDefinition.Strength[] inputPriority) {
            this.color=color;this.maxLife=maxLife;
            this.autoCombo=autoCombo.clone();this.energy=energy;this.energyCommand=energyCommand.clone();
            this.superAttack=superAttack;this.body=body;
            this.inputPriority=inputPriority==null?CombatConfig.DEFAULT_PRIORITY.clone():inputPriority.clone();
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
        validateCombat();
    }
    /** Copy with different animations; used by tests and tooling. */
    CharacterDefinition withAnimations(String id,Map<String,Animation> animations) {
        return new CharacterDefinition(id,id,profile,artFacing,visualStandHeight,visualCrouchHeight,
            fighter,animations,moves,specialAnimations);
    }
    /** Combat definition of a binding, S or SUPER; null when the character lacks it. */
    AttackDefinition attack(String id) {
        if("S".equals(id))return fighter.energy==null?null:fighter.energy.attack;
        if("SUPER".equals(id))return fighter.superAttack==null?null:fighter.superAttack.attack;
        Move move=moves.get(id);
        return move==null?null:move.attack;
    }
    /**
     * Load-time validation of the combat data: every cancel route points to an existing
     * move of the same height (ground/air) and the auto-combo is a declared route.
     */
    private void validateCombat() {
        java.util.List<String> ids=new java.util.ArrayList<>(moves.keySet());
        if(fighter.energy!=null)ids.add("S");
        if(fighter.superAttack!=null)ids.add("SUPER");
        for(String id:ids) {
            AttackDefinition a=attack(id);
            if(!a.id.equals(id))throw new IllegalArgumentException(this.id+"/"+id+": attack id mismatch "+a.id);
            boolean air=id.startsWith("j");
            for(String target:a.cancelInto()) {
                if(AttackDefinition.JUMP.equals(target)) {
                    if(air)throw new IllegalArgumentException(this.id+"/"+id+": an air move cannot jump-cancel");
                    continue;
                }
                if(attack(target)==null)
                    throw new IllegalArgumentException(this.id+"/"+id+": cancelInto "+target+" does not exist");
                boolean special="S".equals(target)||"SUPER".equals(target);
                if(!special && target.startsWith("j")!=air)
                    throw new IllegalArgumentException(this.id+"/"+id+": cancelInto "+target+" changes ground/air");
            }
        }
        for(int i=0;i+1<fighter.autoCombo.length;i++) {
            if(!attack(fighter.autoCombo[i]).cancelsInto(fighter.autoCombo[i+1]))
                throw new IllegalArgumentException(this.id+": autoCombo "+fighter.autoCombo[i]+" -> "
                    +fighter.autoCombo[i+1]+" is not a declared cancel route");
        }
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
