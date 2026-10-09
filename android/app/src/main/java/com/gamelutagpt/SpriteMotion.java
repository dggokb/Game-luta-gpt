package com.gamelutagpt;

/**
 * Simulation selects semantic states; character manifests own frames and timing.
 * Every state transition has an optional visual-only outgoing pose. The prior
 * sprite frame is frozen, never fed back into game logic.
 */
final class SpriteMotion {
    static final class Clip {
        static final String IDLE=SpriteStates.IDLE, WALK_FORWARD=SpriteStates.WALK_FORWARD,
            WALK_BACK=SpriteStates.WALK_BACK, DASH=SpriteStates.DASH, BACKDASH=SpriteStates.BACKDASH,
            CROUCH=SpriteStates.CROUCH, RISE=SpriteStates.RISE, JUMP=SpriteStates.JUMP,
            FALL=SpriteStates.FALL, LAND=SpriteStates.LAND, COMBAT=SpriteStates.COMBAT;
    }
    String clip=Clip.IDLE;
    float time,distance;
    /** Public-to-package so the renderer can draw the previously visible pose. */
    String previousClip;
    int previousFrame;
    private float fadeTime,fadeDuration;
    private boolean wasGrounded=true,wasCrouching;
    private CharacterDefinition character;
    private float takeoffSpeed;
    SpriteMotion() { this(GeneratedCharacters.defaultCharacter()); }
    SpriteMotion(CharacterDefinition character) { this.character=character; }
    void setCharacter(CharacterDefinition next) {
        character=next;clip=Clip.IDLE;time=distance=takeoffSpeed=0;
        wasGrounded=true;wasCrouching=false;
        previousClip=null;fadeTime=fadeDuration=0;
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
        wasGrounded=grounded;
        if(!next.equals(clip)){
            float duration=SpriteTransitionPolicy.duration(character,clip,next);
            if(duration>0) {
                previousClip=clip;
                previousFrame=frame(); // last actual pose, including attack elapsed
                fadeDuration=duration;
                fadeTime=duration;
            } else {
                previousClip=null;
                fadeDuration=fadeTime=0;
            }
            clip=next;time=distance=0;
            if (next.equals(Clip.JUMP)) takeoffSpeed=Math.max(0f,-velocityY);
            if(next.equals(Clip.CROUCH) && wasCrouching) time=character.animation(Clip.CROUCH).duration;
        }
        wasCrouching=crouching;
        time=attackAnimation!=null ? Math.max(0,attackElapsed) : time+dt;
        if (next.equals(Clip.JUMP) && attackAnimation==null)
            time=SpriteAnimationSync.ascentClock(time,velocityY,takeoffSpeed,
                                                 character.animation(Clip.JUMP).duration);
        distance=SpriteAnimationSync.travelDistance(distance,travel);
        if(fadeTime>0) {
            fadeTime=Math.max(0,fadeTime-dt);
            if(fadeTime<=0) previousClip=null;
        }
    }
    int frame(){return character.animation(clip).frame(time,distance);}
    float outgoingAlpha() {
        if(previousClip==null || fadeDuration<=0) return 0f;
        float x=Math.max(0f,Math.min(1f,fadeTime/fadeDuration));
        return x*x; // smoothly accelerates into the new pose.
    }
}
