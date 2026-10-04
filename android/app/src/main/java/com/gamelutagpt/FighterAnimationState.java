package com.gamelutagpt;

/**
 * Estados visuais do Fighter Prototype 01.
 *
 * A simulacao de combate continua no GameView. Este enum e a ponte entre
 * gameplay e animacao: cada estado pode ganhar poses/frames proprios sem
 * alterar a regra do golpe.
 */
enum FighterAnimationState {
    IDLE,
    WALK_FORWARD,
    WALK_BACKWARD,
    DASH_FORWARD,
    BACKDASH,
    CROUCH,

    JUMP_RISE,
    JUMP_FALL,
    SUPER_JUMP_RISE,
    SUPER_JUMP_FALL,

    ATTACK_L,
    ATTACK_M,
    ATTACK_H,
    ATTACK_2L,
    ATTACK_2M,
    ATTACK_2H,

    AIR_ATTACK_L,
    AIR_ATTACK_M,
    AIR_ATTACK_H,
    SPECIAL,
    AIR_SPECIAL,
    SUPER,

    GUARD_HIGH,
    GUARD_LOW,
    HIT,

    KNOCKDOWN_FALL,
    KNOCKDOWN_DOWN,
    GET_UP,

    TAG
}
