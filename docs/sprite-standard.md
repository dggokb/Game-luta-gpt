# Sprite production standard

Scale belongs to the **character**, not to an animation clip.

Each fighter owns one visual profile: frame width/height, root registration point
and one global world scale. Every Idle, walk, attack, jump and reaction for that
fighter is normalized to that profile before entering the game.

Different fighters may use different frame sizes, roots and world scales. A large
fighter can therefore truly be larger than the base fighter.

## Player base

- frame: 256 x 256
- root: X=128, Y=238
- worldScale: 1.0
- transparent background
- one pose per cell

Current sheets:
- player_base_idle.png: 4 x 2
- player_base_movement.png: 4 x 4
- player_base_jab.png: 3 x 1

## Pipeline

1. Create the pose from the character model sheet.
2. Register the frame against that character's root.
3. Normalize anatomy against that character's own landmarks.
4. Export into that character's frame geometry.
5. Preview the animation.
6. Validate root drift and on-model proportions.
7. Integrate with the character profile scale unchanged.

Bounding-box size is not the scale reference. A punch may widen the frame, a crouch
may shorten it and a jump may move the body away from the root. Scale is judged by
the character's anatomy and registration.

Hitboxes and hurtboxes remain separate gameplay data.
