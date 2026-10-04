# Movement atlas specification

Method: built-in image generation, transparent background, using the original branch's walk and idle sheets as identity references. Existing idle preserved; no new attack artwork.

Prompt: production 2D fighting-game movement sprite atlas, genuine transparent background. Preserve the existing adult fighter: black sleeveless vest with red piping and hood lining, white undershirt, loose black trousers with red accents, fingerless gloves, black/red/white shoes and shaggy black hair. Clean anime outlines and cel shading. Four columns and four rows, 16 full-body poses, facing right, consistent anatomy and scale, no text, grid, ground or shadows. First row: forward contact, passing, opposite contact, opposite passing, with visibly different leg silhouettes. Second row: backward steps, still looking right, alternating contact/passing phases. Third row: partial crouch, deep crouch guard, ascending jump with tucked knee, descending preparation. Fourth row: forward dash push-off, extended dash stride, backward hop facing right, compressed landing. Crouched figures must remain shorter, never enlarged to fill a cell. These are movement poses, not attacks.

The generated image's exact dimensions are 1254×1254. Its spacing is irregular, so the runtime uses explicit rectangles and anatomical pivots instead of dividing it into uniform cells. The original generated alpha channel is preserved. Normal movement scale is 205/332 world units per source pixel; each pose keeps the same scale, including crouch and dash.

The short four-frame gaits are intentionally a first movement pass. They can be replaced or expanded without modifying collision, attack timing or character profiles.
