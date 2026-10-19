package com.gamelutagpt;

/** Simulation selects semantic states; character data owns every frame/timing. */
final class SpriteMotion {
    // Stable movement vocabulary (generated from the pack validator). Attack animation IDs are arbitrary manifest keys.
    static final class Clip {
        static final String IDLE=SpriteStates.IDLE, WALK_FORWARD=SpriteStates.WALK_FORWARD,
            WALK_BACK=SpriteStates.WALK_BACK, DASH=SpriteStates.DASH, BACKDASH=SpriteStates.BACKDASH,
            CROUCH=SpriteStates.CROUCH, RISE=SpriteStates.RISE, JUMP=SpriteStates.JUMP,
            FALL=SpriteStates.FALL, LAND=SpriteStates.LAND, COMBAT=SpriteStates.COMBAT,
            DASH_START=SpriteStates.DASH_START, DASH_STOP=SpriteStates.DASH_STOP;
    }
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
        update(dt,grounded,crouching,velocityY,travel,forward,dash,backdash,attackAnimation,attackElapsed,combat,locked,false);
    }
    /** {@code braking}: the body still slides after the run was released (it plays DASH_STOP). */
    void update(float dt,boolean grounded,boolean crouching,float velocityY,float travel,
                boolean forward,boolean dash,boolean backdash,String attackAnimation,
                float attackElapsed,boolean combat,boolean locked,boolean braking) {
        dt=Math.max(0,Math.min(.1f,dt));
        String next;
        if(attackAnimation!=null) next=attackAnimation;
        else if(!grounded) next=velocityY < -35 ? Clip.JUMP : Clip.FALL;
        else if(crouching) next=Clip.CROUCH;
        else if(combat || locked) next=Clip.COMBAT;
        else if(!wasGrounded || (clip.equals(Clip.LAND) && time<character.animation(Clip.LAND).duration)) next=Clip.LAND;
        else if(backdash && Math.abs(travel)>.001f) next=Clip.BACKDASH;
        else if(braking && character.animations.containsKey(Clip.DASH_STOP)) next=Clip.DASH_STOP;
        else if(Math.abs(travel)>.001f) next=dash && forward ? dashClip() : forward ? Clip.WALK_FORWARD : Clip.WALK_BACK;
        else if(clip.equals(Clip.CROUCH) || (clip.equals(Clip.RISE) && time<character.animation(Clip.RISE).duration)) next=Clip.RISE;
        else if(stopping()) next=Clip.DASH_STOP;
        else next=Clip.IDLE;
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
    /** Corrida com arrancada (DASH_START, uma vez) antes do loop, quando o personagem tem. */
    private String dashClip() {
        if(clip.equals(Clip.DASH)) return Clip.DASH;
        if(clip.equals(Clip.DASH_START))
            return character.animation(Clip.DASH_START).finished(time,distance) ? Clip.DASH : Clip.DASH_START;
        return character.animations.containsKey(Clip.DASH_START) ? Clip.DASH_START : Clip.DASH;
    }
    /** Parou de correr: a freada (DASH_STOP) toca até o fim antes da guarda; qualquer ação a corta. */
    private boolean stopping() {
        if(!character.animations.containsKey(Clip.DASH_STOP)) return false;
        if(clip.equals(Clip.DASH_STOP)) return !character.animation(Clip.DASH_STOP).finished(time,distance);
        return clip.equals(Clip.DASH) || clip.equals(Clip.DASH_START);
    }
    int frame(){return character.animation(clip).frame(time,distance);}
}
