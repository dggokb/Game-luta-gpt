# Sprite production standard

Scale belongs to the **character**, never to an individual animation.

Each fighter owns a visual profile with its own base frame, preferred registration
root, world scale and standing reference height. Large and small fighters therefore
keep their intended size differences.

## Player base

- base frame: 256 x 256
- preferred root: X=128, Y=238
- worldScale: 1.0
- standing visual reference height: 228 px
- transparent background

The base frame is not a hard maximum canvas size. A long kick, weapon or effect may
need a wider clip canvas. The fighter is never shrunk just to fit a fixed rectangle.

## Import pipeline

New generated sprite sheets are not sliced by equal-width guesses.

`tools/sprites/import_sprites.py`:

1. detects each frame from transparent separation in the source sheet, including horizontal strips and multi-row grids;
2. verifies the expected frame count;
3. detects the grounded registration point from the supporting foot/feet;
4. applies one character-relative normalization scale to the whole clip;
5. calculates the required transparent canvas from the actual pose extents;
6. preserves the preferred character root whenever it fits;
7. rounds canvas size to predictable allocation steps;
8. validates minimum margins on all four sides;
9. validates root drift after resampling;
10. refuses low-resolution sources that would require forbidden upscaling;
11. generates the normalized PNG, machine-readable report, debug preview and Java
    layout constants consumed by the renderer.

A failure in any required check exits non-zero and blocks CI.

The GitHub workflow runs the importer before the Android tests and checks the
medium-kick and heavy-straight PNGs, their reports and the generated Java layout
against committed outputs. These are the two clips currently covered by regeneration.
Idle, movement and Jab use normalized assets but do not yet have source/config/report
coverage in this importer. Do not describe all sprites as reproducibly imported.

## Clip geometry

Runtime scale is still the character's single `worldScale`.

A clip can have its own **canvas geometry**:

- frameWidth / frameHeight
- local rootX / rootY

Those values describe transparent space and registration only. They do not resize
the fighter.

The medium kick is the first asset using the importer. Its source sheet visually
looked like three equal cells, but the extended kick crossed the naive cell boundary.
The importer detected the three actual opaque components instead, registered their
feet independently and produced:

- 3 frames
- 384 x 256 per frame
- root X=128, Y=238
- worldScale=1.0
- at least 8 px transparent safety margin on every side
- no clipping

## Source/config/generated separation

Source art:
`art/sprites/source/`

Character profiles:
`tools/sprites/profiles/`

Clip configurations:
`tools/sprites/clips/`

Validation reports:
`tools/sprites/reports/`

Generated Android assets:
`android/app/src/main/res/drawable-nodpi/`

Generated runtime layout metadata:
`GeneratedSpriteLayouts.java`

## Rule for future sprites

The production flow is:

source art -> importer -> automatic geometry/root calculation -> validation ->
debug preview -> generated asset/layout -> Android tests -> APK.

Do not manually choose a crop width, resize one animation in the renderer, or make
a pose fit by changing its scale. If an asset does not satisfy the character
profile, the importer must reject it before integration.

Hitboxes and hurtboxes remain separate gameplay data and must never be inferred from
opaque pixels.

## Review baseline and idle quality

Reviewed against Sprite GPT v0.51, commit
`ef3416d8d216a2c7cc54d823e95882ce0bad7c72`, integrated into Sprite Astra v0.52.
The v0.48 high-resolution idle restoration is included: `player_base_idle.png`
is a 1024×512 atlas with eight 256×256 cells. Runtime uses the same character
transform as movement, rather than enlarging the old 120×155 idle frames.
Keep the high-resolution master; never recover detail by enlarging the legacy
compressed idle. The normalized idle was visually reviewed alongside movement.


## Multi-row sprite sheets

For larger animations the importer also supports `grid-alpha-components`.
It detects occupied X and Y regions from alpha, combines them in row-major order
and validates the resulting frame count before normalization.

The standing Heavy straight punch is the first 9-frame case:

- source layout: 3 x 3;
- detected frames: 9;
- generated runtime sheet: 9 frames in a horizontal strip;
- generated frame: 320 x 256;
- generated local root: X=136, Y=244;
- worldScale remains 1.0;
- minimum required transparent margin: 8 px;
- no runtime per-animation scale correction.

This proves that source-sheet arrangement is an import concern only. Runtime clips
consume generated frame geometry and do not depend on how the source art was laid out.

## Actual limits of the current importer

The two segmentation modes find occupied projections separated by fully transparent
gaps, not arbitrary two-dimensional connected components. Grid segmentation uses
global X intervals shared by all rows. Staggered rows, detached effects, shadows or
touching poses can merge or split candidates; a correct frame count alone does not
prove correct grouping. Inspect the source rectangles in the technical preview.

Only `ground-feet` is implemented. It estimates the root from the lowest alpha band,
and silently falls back to the bounding-box center when no valid foot interval is
found. Root tolerance proves consistency of that heuristic after resampling, not
anatomical correctness. It must not be used as an automatic root for airborne
poses, cloth/effects below the feet, or intentional world-space root motion.

One import scale is calculated from median bounding-box height of
`scaleReferenceFrames`. This is safe only for comparable standing reference poses
without effects changing their height. Normalizing crouch/jump/extended-arm poses
to standing height would change anatomy even though runtime worldScale stays 1.0.

Alpha threshold is 10: margin checks ignore alpha ≤10. A PASS therefore does not
prove that every faint pixel or translucent effect was preserved. `maxUpscale=1.0`
rejects insufficient dimensions, but does not detect blur or compression already
present in a large source. Review the final rendered image at gameplay scale.

## Recommended next improvements (not implemented in v0.52)

1. Migrate idle, movement and Jab masters into `art/sprites/source/`, with configs,
   reports and generated layouts. Then generate the CI output list from all clip
   configs so new clips cannot be omitted from a hard-coded comparison list.
2. Detect X intervals per row; add explicit expected row counts/order and reject
   ambiguous grouping. Allow reviewed source landmark metadata for unusual poses,
   while keeping crop/root/scale corrections out of the renderer.
3. Use an authored standing reference or anatomical landmarks for scale. Add root
   confidence checks and fail when feet are missing; support a distinct reviewed
   root mode for airborne clips. Do not shift sprites to erase intentional motion.
4. Reject source silhouettes cut off at the image boundary before normalization.
   Check cell containment before compositing, and report faint alpha outside the
   validated silhouette separately so thresholding cannot hide clipped effects.
5. Pin Pillow and record source/config/profile hashes and importer version in the
   report. Generate character profile values from one source; they are currently
   duplicated between JSON and `CharacterVisualProfile.java`. Validate reference
   indices, frame counts, unique IDs/Java names and finite positive profile values.
6. Add negative importer cases: staggered rows, detached effects, wrong count,
   missing feet, invalid references, transparent source, upscale and canvas limits.
   Keep production-renderer tests for both facing directions, zoom and transitions.
7. Synchronize attack visual timing and active hit windows from gameplay metadata.
   Current melee damage begins at 72% of attack duration, while L/M visual recovery
   starts at 0.100/0.170 seconds (damage starts at 0.1152/0.1872 seconds). Test a real
   hit at each phase instead of only checking ordered animation frames.
8. Treat emulator startup/resume as a lifecycle smoke test. Approval of visual
   quality and input latency also needs moving footage, multi-touch/release tests,
   frame-time measurements and a physical Android run. A 120 Hz fixed physics step
   does not guarantee 8.3 ms input latency; input and drawing share a 60 FPS loop.

These recommendations are documentation of remaining work, not claims of new
runtime behavior. Keep hitboxes/hurtboxes independent from sprite alpha.
