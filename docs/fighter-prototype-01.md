# Fighter Prototype 01 — 3D production baseline

This document freezes the approved Fighter Prototype 01 concept as the technical source of truth for the standard fighter.

## Phase 1 — visual specification

- Adult male anime fighter, athletic build, readable fighting-game silhouette.
- Short spiky dark hair.
- Navy sleeveless vest with white side panels and red/orange collar accent.
- Black fitted undershirt.
- Loose white martial-arts pants with navy side accents.
- Black waist sash with red hanging accent.
- Black/red fingerless gloves.
- White/black/navy fighting shoes with red accent.
- Cel-shaded materials: hard light bands, dark outline, no photorealistic shading.

Reference proportions are normalized around a ~200 world-unit standing height so the model fits the current 2D combat metrics.

## Phase 2 — real 3D model

The runtime model is a true OpenGL ES mesh assembled from reusable low-poly primitives:
- sphere: head/hair mass
- cylinders: arms, legs, neck
- boxes: torso, vest panels, belt, gloves, shoes, facial details
- cones: hair spikes

The model is rendered in 3D with depth testing, back-face culling and a cel-shading shader. It is not drawn by Android Canvas.

## Phase 3 — standard rig

Bone hierarchy:
- ROOT
  - PELVIS
    - SPINE
      - CHEST
        - NECK
          - HEAD
        - L_UPPER_ARM
          - L_FOREARM
            - L_HAND
        - R_UPPER_ARM
          - R_FOREARM
            - R_HAND
    - L_THIGH
      - L_SHIN
        - L_FOOT
    - R_THIGH
      - R_SHIN
        - R_FOOT

This rig is the animation contract for future fighters.

## Phase 4 — baseline animation library

Implemented as procedural key poses blended from the live combat state:
- idle breathing
- forward/back walk cycle
- crouch
- jump / super-jump airborne pose
- high guard
- low guard
- standing L / M / H
- crouching 2L / 2M / 2H
- special / super casting pose
- knockdown root rotation remains driven by combat state

The combat engine still owns timing, physics, hitboxes, damage and cancel rules. The animator only converts those states into bone rotations.

## Phase 5 — integration

The existing Canvas GameView remains the gameplay/stage/HUD layer. Fighter Prototype 01 is rendered by a transparent OpenGL ES layer aligned to the exact same fight camera.

This preserves all mechanics while making the player model truly 3D. The old Canvas/pseudo-3D player renderer is bypassed when the OpenGL layer is active.
