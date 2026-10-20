package com.gamelutagpt;

/** Simulation selects semantic states; character data owns every frame/timing. */
final class SpriteMotion {
    // Stable movement vocabulary (generated from the pack validator). Attack animation IDs are arbitrary manifest keys.
    static final class Clip {
        static final String IDLE=SpriteStates.IDLE, WALK_FORWARD=SpriteStates.WALK_FORWARD,
            WALK_BACK=SpriteStates.WALK_BACK, DASH=SpriteStates.DASH, BACKDASH=SpriteStates.BACKDASH,
            CROUCH=SpriteStates.CROUCH, RISE=SpriteStates.RISE, JUMP=SpriteStates.JUMP,
            FALL=SpriteStates.FALL, LAND=SpriteStates.LAND, COMBAT=SpriteStates.COMBAT;
    }
    /** O IDLE2 (se o pacote tiver) toca uma vez a cada tantas voltas do IDLE. */
    static final int IDLE_LOOPS_BEFORE_IDLE2=2;
    String clip=Clip.IDLE;
    float time,distance;
    private boolean wasGrounded=true,wasCrouching;
    private CharacterDefinition character;
    SpriteMotion() { this(GeneratedCharacters.defaultCharacter()); }
    SpriteMotion(CharacterDefinition character) { this.character=character; }
    void setCharacter(CharacterDefinition next) {
        character=next;clip=Clip.IDLE;time=distance=0;wasGrounded=true;wasCrouching=false;
    }
    void update(float dt,boolean grounded,boolean crouching,float velocityY,float travel,
                boolean forward,boolean dash,boolean backdash,String attackAnimation,
                float attackElapsed,boolean combat,boolean locked) {
        dt=Math.max(0,Math.min(.1f,dt));
        String next;
        if(attackAnimation!=null) next=attackAnimation;
        else if(!grounded) next=velocityY < -35 ? Clip.JUMP : Clip.FALL;
        else if(crouching) next=Clip.CROUCH;
        else if(combat || locked) next=Clip.COMBAT;
        else if(!wasGrounded || (clip.equals(Clip.LAND) && time<character.animation(Clip.LAND).duration)) next=Clip.LAND;
        else if(backdash && Math.abs(travel)>.001f) next=Clip.BACKDASH;
        else if(Math.abs(travel)>.001f) next=dash && forward ? Clip.DASH : forward ? Clip.WALK_FORWARD : Clip.WALK_BACK;
        else if(clip.equals(Clip.CROUCH) || (clip.equals(Clip.RISE) && time<character.animation(Clip.RISE).duration)) next=Clip.RISE;
        else next=Clip.IDLE;
        if(next.equals(Clip.IDLE)) next=idleVariant(dt);
        wasGrounded=grounded;
        if(!next.equals(clip)){
            clip=next;time=distance=0;
            // Still holding down after a crouching attack (or block): stay crouched instead
            // of standing up and crouching again.
            if(next.equals(Clip.CROUCH) && wasCrouching) time=character.animation(Clip.CROUCH).duration;
        }
        wasCrouching=crouching;
        // Attacks use the combat clock, including a repeated attack of the same type.
        time=attackAnimation!=null ? Math.max(0,attackElapsed) : time+dt;
        distance+=Math.abs(travel);
    }
    /** IDLE parado: a cada duas voltas dele, uma vez o IDLE2; depois volta ao IDLE. */
    private String idleVariant(float dt){
        CharacterDefinition.Animation idle2=character.animations.get(SpriteStates.IDLE2);
        if(idle2==null || character.animation(Clip.IDLE).duration<=0) return Clip.IDLE;
        if(clip.equals(SpriteStates.IDLE2)) return time+dt<idle2.duration ? SpriteStates.IDLE2 : Clip.IDLE;
        if(clip.equals(Clip.IDLE) && time+dt>=IDLE_LOOPS_BEFORE_IDLE2*character.animation(Clip.IDLE).duration)
            return SpriteStates.IDLE2;
        return Clip.IDLE;
    }
    int frame(){return character.animation(clip).frame(time,distance);}
}
