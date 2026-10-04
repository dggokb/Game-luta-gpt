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

The GitHub workflow runs the importer before the Android tests and checks that the
generated PNG, report and Java layout metadata are identical to the committed
outputs. A stale or manually altered generated asset therefore fails the build.

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
