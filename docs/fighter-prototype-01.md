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

The player is a real OpenGL ES mesh. Cross-section meshes define the tapered torso, muscular limbs, loose trousers and angular jaw. Partial surface arcs form vest panels and trouser stripes. Solid swept tufts form the hair; smaller meshes define collar lining, sash, fingerless gloves, eyes and shoes.

The procedural low-poly model is an interpretation of the supplied concept, not a final sculpt or a skinned imported GLB. Pieces follow 18 hierarchical bones. The renderer uses depth testing, consistently outward triangle winding, inverse-transpose normals for nonuniform scales, three light bands and a constant world-space outline. Mirrored facing also flips the culling convention.

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
- independent idle breathing
- forward/back walk cycle
- crouch
- jump / super-jump and descending pose
- high guard
- low guard
- standing L / M / H
- crouching 2L / 2M / 2H
- special / super casting pose
- knockdown root rotation remains driven by combat state

The combat engine still owns timing, physics, hitboxes, damage and cancel rules. The animator only converts those states into bone rotations.

## Phase 5 — integration

The existing Canvas GameView remains the gameplay/stage layer. A normal View draws HUD/controls above both surfaces. Fighter Prototype 01 is rendered by a transparent OpenGL ES layer aligned to the exact same fight camera.

This preserves all mechanics while making the player model truly 3D. The old Canvas/pseudo-3D player renderer is bypassed when the OpenGL layer is active.

## Astra v0.46 validation

The render state is copied atomically once per GL frame. Pose changes are smoothed using simulation time, then the support foot is planted. Combat owns hit timing; the visual rig never changes hitboxes. `Fighter3DTest` exports the actual meshes, matrices and shaders for `tools/render_mesh_review.py`, which renders the standard poses using EGL. This does not replace device testing.
