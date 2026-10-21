package com.gamelutagpt;

/** Simulation selects semantic states; character data owns every frame/timing. */
final class SpriteMotion {
    // Stable movement vocabulary (generated from the pack validator). Attack animation IDs are arbitrary manifest keys.
    static final class Clip {
        static final String IDLE=SpriteStates.IDLE, WALK_FORWARD=SpriteStates.WALK_FORWARD,
            WALK_BACK=SpriteStates.WALK_BACK, DASH=SpriteStates.DASH, BACKDASH=SpriteStates.BACKDASH,
            CROUCH=SpriteStates.CROUCH, RISE=SpriteStates.RISE, JUMP=SpriteStates.JUMP,
            FALL=SpriteStates.FALL, LAND=SpriteStates.LAND, COMBAT=SpriteStates.COMBAT,
            IDLE_2=SpriteStates.IDLE_2;
    }
    /** Voltas do IDLE antes do segundo idle (IDLE_2, opcional), que toca uma vez e volta. */
    static final int IDLE_LOOPS_BEFORE_IDLE_2=3;
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
        else next=idle();
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
    /**
     * Parado: o IDLE em loop e, quem tem IDLE_2, o segundo idle uma vez a cada 3 voltas
     * inteiras do IDLE (a troca cai no fim da volta, na guarda em que os dois começam).
     */
    private String idle() {
        if(!character.animations.containsKey(Clip.IDLE_2)) return Clip.IDLE;
        if(clip.equals(Clip.IDLE_2)) return time<character.animation(Clip.IDLE_2).duration ? Clip.IDLE_2 : Clip.IDLE;
        if(clip.equals(Clip.IDLE) && time>=IDLE_LOOPS_BEFORE_IDLE_2*character.animation(Clip.IDLE).duration) return Clip.IDLE_2;
        return Clip.IDLE;
    }
    int frame(){return character.animation(clip).frame(time,distance);}
}
