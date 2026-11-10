# Stylized real-time VFX in a custom 2D engine (Android Java + OpenGL ES 3.0, Java2D fallback) and headless VFX authoring tools

Reading guide for the report writer:

- **Cited Findings** only contain things read in a source during this research. Each one says how it was read: *(direct)* = raw file/page text was downloaded and read; *(fetch summary)* = the page was read through a summarising fetch tool, so code and wording should be re-checked against the page before being quoted as verbatim; *(search summary)* = only a search-engine summary of the page was seen, lowest confidence.
- **Inferences** contain the recipes I assembled myself (GLSL ES 3.00, Java, Python). The Java ribbon builder and the Python texture generator were **compiled and run in this session** (results stated next to them). The GLSL was **not compiled** (no GLSL compiler in the research sandbox) and must be treated as untested.
- Nothing was tested on an Android device or GPU. No tool in the "Tools" section was installed or run here (neither Godot nor Blender is installed in the sandbox; `xvfb-run` and `ffmpeg` are).

---

## 1. Shader recipes in GLSL ES 3.0 (UV scroll, erosion/dissolve, gradient mapping, blending, distortion, stepped time)

### Takeaway
Every "hand-made looking" trail/spark/smoke look asked about reduces to one small fragment shader with 2 texture samples: scroll a grayscale shape, compare it (times a noise) against a time-driven threshold with `step`/`smoothstep`, colour the surviving value through a ramp, and output premultiplied colour so additive and normal blending share one blend state (`GL_ONE, GL_ONE_MINUS_SRC_ALPHA`). Sources agree on the maths; none of them is GLSL ES 3.00 for a custom engine, so the combined shader below is my adaptation and is uncompiled.

### Cited Findings

**Alpha erosion / dissolve**
- Alpha erosion = set a threshold on an alpha mask and animate it; pixels erode progressively instead of fading together, which is why it looks different from a fade. Hard version: `pixel.a = step(threshold, pixel.a);` (only 0 or 1, aliased). Soft version: `smoothstep(min, max, alpha)` gives a feathered edge. *(fetch summary)* — [VFXDoc: Alpha Erosion](https://vfxdoc.readthedocs.io/en/latest/shaders/alpha-erosion/)
- Soft-erosion pitfall: with feather > 0, erosion factors 0.0 and 1.0 no longer fully show / fully erase the mask. Fix: remap the erosion input to the wider range `[(0.0 - feather) ... (1.0 + feather)]`. *(fetch summary)* — [VFXDoc: Alpha Erosion](https://vfxdoc.readthedocs.io/en/latest/shaders/alpha-erosion/)
- Erosion maps "encode a mathematical progression, not color": they must be imported as linear (sRGB off), otherwise the erosion progresses non-linearly. *(fetch summary)* — [VFXDoc: Alpha Erosion](https://vfxdoc.readthedocs.io/en/latest/shaders/alpha-erosion/)
- Dithered erosion (Bayer pattern offset before the `step`) gives pseudo-transparency while every surviving pixel stays opaque, which avoids blended overdraw; the example given is large smoke in Strike Vector EX that caused frame drops when the camera passed through. *(fetch summary)* — [VFXDoc: Alpha Erosion](https://vfxdoc.readthedocs.io/en/latest/shaders/alpha-erosion/)
- A complete dissolve-with-glowing-edge fragment shader written for GLSL ES 3.00 (Shadertoy-style `mainImage`). The page was read through a summarising fetch, which reported the code as MIT-licensed by GameDev.net and restored the indentation, so re-check against the page before quoting. Key lines:
  ```glsl
  uniform float uAmount;    // 0..1 dissolve amount
  uniform float uEdgeWidth; // 0.02..0.25
  uniform vec3  uEdgeColor;
  float threshold = mix(-0.05, 1.05, uAmount);            // overshoots the noise range on purpose
  float keep = smoothstep(threshold, threshold + uEdgeWidth, field);
  float burn = keep - smoothstep(threshold + uEdgeWidth,
                                 threshold + uEdgeWidth * 2.4, field);
  float alpha = sprite.a * keep;                          // multiply, do not replace, the art's own alpha
  vec3 color = mix(sprite.rgb, uEdgeColor, clamp(burn, 0.0, 1.0));
  color += uEdgeColor * burn * 1.4;
  ```
  where `field` is a 2-octave value noise (`0.65*noise(uv*s) + 0.35*noise(uv*s*2.7+11.0)`). The explanation given: the threshold overshoots so amount 0 leaves the sprite intact and 1 removes it entirely; the glow is "the difference between two smoothsteps on nearby thresholds", so it follows the dissolve front and not the sprite outline. — [GameDev.net ShaderLab: Sprite dissolve effect](https://gamedev.net/shaderlab/14-sprite-dissolve-effect/)

**UV distortion with a flow/noise texture**
- Flow-map distortion: decode the flow vector from [0,1] to [-1,1], `uv' = uv - flow * progress` with `progress = frac(time + phaseOffset)`; hide the snap-back by blending two samples half a phase apart with triangle weights `w = 1 - abs(1 - 2*progress)` (the two weights always sum to 1). A low-frequency noise added to time de-synchronises the pulse across the surface. Reference function (HLSL, trivially portable; read via summarising fetch):
  ```hlsl
  float3 FlowUVW (float2 uv, float2 flowVector, float2 jump,
                  float flowOffset, float tiling, float time, bool flowB) {
      float phaseOffset = flowB ? 0.5 : 0;
      float progress = frac(time + phaseOffset);
      float3 uvw;
      uvw.xy = uv - flowVector * (progress + flowOffset);
      uvw.xy *= tiling;
      uvw.xy += phaseOffset;
      uvw.xy += (time - progress) * jump;
      uvw.z = 1 - abs(1 - 2 * progress);
      return uvw;
  }
  ```
  — [Catlike Coding: Texture Distortion](https://catlikecoding.com/unity/tutorials/flow/texture-distortion/)

**Gradient mapping / palette lookup**
- Android's own texture guide recommends "color tinting": store a grayscale texture (less memory than RGB) and colourise in the shader; an alternative is an RGB mask whose channels select colours. *(direct)* — [Android Developers: Textures](https://developer.android.com/games/optimize/textures)
- Gradient-map shaders in Godot/Defold tutorials all do the same three steps: compute a gray value (luma dot product), use it as the U coordinate of a ramp texture, keep the source alpha. *(search summary)* — [GDQuest: Gradient map](https://www.gdquest.com/library/shader_gradient_map); [Defold forum: Gradient map shader sample](https://forum.defold.com/t/gradient-map-shader-sample-project/81188)
- Ramp texture pitfalls reported by artists: set the ramp's wrap mode to clamp (repeat gives dark/bright artifacts at the ends); do not import the grayscale lookup source as sRGB, the colour ramp itself may be sRGB. *(search summary)* — [Polycount discussion](https://polycount.com/discussion/comment/1709224)
- A shipping open-source 2D fighting-game engine does exactly a 1D palette lookup in its sprite shader: `c = COMPAT_TEXTURE(pal, vec2(palUV[0]+palUV[2]*c.r*0.9966, palUV[1]));` (index in the red channel, palettes packed in a palette atlas addressed by `palUV`), followed by per-draw "PalFX" (`hue`, `neg`, `gray`, `add`, `mult`, `alpha` uniforms). *(direct)* — [Ikemen GO: src/shaders/sprite_lib.frag.glsl](https://github.com/ikemen-engine/Ikemen-GO/blob/master/src/shaders/sprite_lib.frag.glsl)

**Additive vs premultiplied alpha**
- With premultiplied output the blend state is `glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);` and the three common effect blend modes become a texture/shader decision, not a state change: "Additive - Set alpha to zero. Alpha Blend - Multiply the color by the alpha value. Multiply - Limited to greyscale multiply." *(direct)* — [dtrebilco/PreMulAlpha README](https://github.com/dtrebilco/PreMulAlpha)
- Same source on usage: "Additive - Used in sparks, fire, lights, glowing objects etc. Alpha Blend - Used in leaves, fences, smoke etc."; premultiplied alpha removes the dark fringes that `(alpha, 1-alpha)` blending shows around soft edges and fixes colour bleeding in mipmap generation; it is lossy on colour but "in general, it should be the default blending mode". *(direct)* — [dtrebilco/PreMulAlpha README](https://github.com/dtrebilco/PreMulAlpha)
- `GL_ONE, GL_ONE_MINUS_SRC_ALPHA` is correct only if the shader output is premultiplied; `GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA` is for un-multiplied output. *(search summary)* — [Khronos forum: blend problem](https://community.khronos.org/t/blend-problem/62428)

**This project's existing filter (checked in the repository, grep only)**
- `android/app/src/main/java/com/gamelutagpt/Filtro.java` documents its 4x5 matrix as the Android `ColorMatrix` convention: offsets on a 0–255 scale, applied to the colour *without* premultiplied alpha. The renderer already issues `canvas.drawVertices(Canvas.VertexMode.TRIANGLE_STRIP, ...)` with a `BitmapShader` paint in `AndroidRenderCanvas.java` (line 267), and the desktop shim `android/pc/src/main/java/android/graphics/Canvas.java` only supports `drawVertices` for `TRIANGLE_STRIP` with texture coordinates and a `BitmapShader`. — local files, no URL

### Inferences

**(a) One combined effect shader, GLSL ES 3.00 — my adaptation of the sources above, NOT compiled or run.**

```glsl
#version 300 es
// vertex
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;      // ribbon: u = 0 tail .. 1 head, v = 0 outer edge .. 1 inner edge
uniform mat4 uMvp;
out vec2 vUv;
void main() { vUv = aUv; gl_Position = uMvp * vec4(aPos, 0.0, 1.0); }
```

```glsl
#version 300 es
precision mediump float;
// Packed effect texture, linear (NOT sRGB), REPEAT on S, CLAMP on T for ribbons:
//   R = shape / intensity (streaks, spark body, smoke puff)   G = tileable erosion noise   B = spare mask
uniform sampler2D uTex;
uniform sampler2D uRamp;        // optional 256x1 RGB ramp, CLAMP_TO_EDGE, LINEAR
uniform float uUseRamp;         // 1.0 = sample uRamp, 0.0 = use the three colours below
uniform vec3  uColDark, uColMid, uColHot;   // per-character palette
uniform float uTime;            // seconds since the effect started (keep small; restart per effect)
uniform float uFps;             // 12.0 = "on twos" (of 24), 8.0 = "on threes"; 0.0 = smooth
uniform vec2  uScroll;          // shape UV scroll, in UV units per second (x = along the ribbon)
uniform vec2  uNoiseScroll;     // erosion-noise scroll
uniform vec2  uNoiseTiling;     // e.g. (2.0, 1.0)
uniform float uDistort;         // 0.0 .. ~0.08
uniform float uErode;           // 0 = intact .. 1 = gone (drive from effect age on the CPU)
uniform float uTailBias;        // extra erosion at the tail, 0 .. 1
uniform float uFeather;         // 0.0 = hard cel edge (1 px AA), up to ~0.15 = soft
uniform float uRimWidth;        // 0.0 = no rim, ~0.05 = thin bright rim at the erosion front
uniform float uHead, uTail;     // visible window along u; ribbon is drawn only where uTail <= u <= uHead
uniform float uAdditive;        // 0.0 = normal "over", 1.0 = additive, in-between allowed
uniform float uAlpha;           // per-draw alpha
in vec2 vUv;
out vec4 fragColor;

void main() {
    // stepped time: hold each pose for 1/uFps seconds (anime "on twos / threes")
    float t = uFps > 0.0 ? floor(uTime * uFps) / uFps : uTime;

    // sample 1: scrolling noise (erosion + distortion source)
    float n = texture(uTex, vUv * uNoiseTiling + uNoiseScroll * t).g;

    // sample 2: scrolling, distorted shape
    vec2 uv = vUv + (n - 0.5) * uDistort * vec2(1.0, 0.6);
    float shape = texture(uTex, uv + uScroll * t).r;

    // position inside the visible window: 0 at the tail end, 1 at the head
    float along = clamp((vUv.x - uTail) / max(uHead - uTail, 1e-4), 0.0, 1.0);
    float inside = step(uTail, vUv.x) * step(vUv.x, uHead);

    // erosion: field vs threshold. The threshold is remapped to [-f, 1+f] so 0 and 1 are exact.
    float field = shape * mix(0.55, 1.0, n);
    float e  = clamp(uErode + (1.0 - along) * uTailBias, 0.0, 1.0);
    float f  = max(uFeather, fwidth(field));           // uFeather = 0 -> one-pixel antialiased hard edge
    float th = mix(-f, 1.0 + f, e);
    float keep = smoothstep(th - f, th + f, field);
    float rim  = uRimWidth > 0.0
               ? keep * (1.0 - smoothstep(th + f, th + f + uRimWidth, field))
               : 0.0;

    // gradient map: gray -> ramp texture or dark/mid/hot colours
    float g = clamp(shape + rim, 0.0, 1.0);
    vec3 col3 = mix(mix(uColDark, uColMid, clamp(g * 2.0, 0.0, 1.0)),
                    uColHot, clamp(g * 2.0 - 1.0, 0.0, 1.0));
    vec3 col  = mix(col3, texture(uRamp, vec2(g, 0.5)).rgb, uUseRamp);
    col = mix(col, uColHot, rim);

    // premultiplied output; alpha -> 0 turns the same blend state into additive
    float a = keep * inside * uAlpha;
    fragColor = vec4(col * a, a * (1.0 - uAdditive));
}
// GL state: glEnable(GL_BLEND); glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
```

Notes on the recipe (my reasoning, not from a source):
- Hard edge vs soft edge is only `uFeather` (0 vs ~0.1). The optional rim is the "two nearby smoothsteps" idea from the GameDev.net shader.
- Stepped time: `floor(t * fps) / fps` is the whole trick; it must be applied to *everything that animates* (scroll, erosion amount, head position) or the effect looks half-smooth. Quantising `uErode`/`uHead` is easiest on the CPU: `float q = (float) Math.floor(age * fps) / fps;` before computing them. Motion of the character sprite itself should stay tied to the animation frames it already has.
- If `uRamp` is unused, bind any texture or build a second program without it; dynamic branching on a uniform is cheap but not free on old GPUs.
- `fwidth` is core in GLSL ES 3.00 (no extension needed) — from general knowledge of the spec, not re-verified in this session. Likewise that `highp float` is available in ES 3.0 fragment shaders; use `highp` for `uTime`-driven UV maths if banding/jitter appears with large time values, or wrap `uScroll * t` with `fract` on the CPU.
- Use additive (`uAdditive = 1`) for sparks, flashes, energy cores; use normal "over" for smoke, dust and for trails that must stay visible on bright stages (additive washes out to white on a light background). A common mix is `uAdditive ≈ 0.5–0.7` for slash trails: bright but still occluding. This matches the usage list in the PreMulAlpha README; the 0.5–0.7 figure is my suggestion, not sourced.

**(b) What the existing 4x5 colour matrix can already do without any new shader.** Because the matrix works on un-premultiplied colour with 0–255 offsets (per `Filtro.java`), two of the requested looks are just matrices applied to a grayscale effect texture whose erosion field is in the red channel:
- Two-colour gradient map: `R' = (hot.r - dark.r) * R + dark.r` (same for G, B, all driven from the input R column). That is a straight dark→hot ramp, distinct per character, at zero shader cost.
- Linear erosion: `A' = k * R + (-threshold * k * 255)` with `k = 1 / feather`. After the clamp to 0..255 this is `clamp((field - threshold) / feather)`, i.e. the soft erosion with a linear instead of smoothstep ramp; `feather = 0.02` gives a visually hard edge.
- Limits: a three-colour ramp, a rim at the erosion front, scrolling a second noise layer, and distortion need the dedicated shader above. This assumes the GL path clamps the matrix result and applies it before alpha is multiplied in; I only grepped the files, I did not read the GL shader, so this must be checked.

**(c) Java2D fallback (no shaders).** From general Java2D knowledge, not from a source read in this session:
- `java.awt.AlphaComposite` only offers Porter-Duff rules, so there is no built-in additive blend. Options: a custom `Composite` (slow), or bake additive effects as premultiplied "over" sprites with boosted brightness and accept the difference.
- Erosion and gradient mapping can be done per pixel in software once per frame-step into a cached `BufferedImage` (`TYPE_INT_ARGB_PRE`): for each pixel `a = clamp((field - th) / feather)`, colour from a 256-entry `int[]` LUT. Because time is stepped (8–12 distinct steps per effect), the result of every step can be cached, so the cost is paid once per effect variant.
- The desktop shim already maps `drawVertices(TRIANGLE_STRIP)` with a `BitmapShader`; keeping the trail as a triangle strip with a pre-eroded, pre-coloured texture per time step keeps both paths on the same geometry code.

### Gaps
- I did not find and read a primary graphics-programmer article (Zucconi, Cyanilux, Ronja, Minions Art, Book of Shaders, Inigo Quilez) for each recipe; the erosion, flow and dissolve material comes from VFXDoc, Catlike Coding and GameDev.net ShaderLab instead. The suggested authors were not fetched.
- No source was read that states the stepped-time formula or documents "on twos/threes" for effects in a specific game. Search summaries about Guilty Gear Xrd (GDC 2015, Junya C. Motomura) confirm deliberate limited animation / frame cutting for characters ([GDC Vault](https://gdcvault.com/play/1022031/GuiltyGearXrd-s-Art-Style-The), [slides PDF](https://www.ggxrd.com/Motomura_Junya_GuiltyGearXrd.pdf)) but the talk text itself was not retrieved, so nothing about its effects pipeline is confirmed here.
- The GLSL above is uncompiled. It needs a compile on a real ES 3.0 driver (and ideally one Mali and one Adreno device) before being trusted.

---

## 2. Trail mesh generation (ribbon along a polyline/spline, normals, width profile, UVs, self-intersection, head/tail animation)

### Takeaway
The standard pipeline is: clean the points, smooth them (Chaikin or centripetal Catmull-Rom), resample at even arc length, extrude along the per-point normal with a width profile, map U to arc length. On a densely resampled smooth curve plain normals are enough (no miter maths); the one real hazard is the inner edge folding over on tight bends, which is solved by clamping width to the local radius of curvature. Because each attack's path is known ahead of time, the mesh can be built once per attack and animated purely with shader uniforms.

### Cited Findings
- mattdesl's libGDX "finger swipe" (Fruit-Ninja style) pipeline: capture input → simplify (drop points closer than a minimum distance, radial-distance pass) → smooth with 2 iterations of Chaikin (each pair P0,P1 emits points at 25 % and 75 %; first and last kept) → extrude to a triangle strip using the perpendicular `(-y, x)` of the normalised segment direction → taper thickness with distance from the head → anti-alias by giving centre vertices weight 1 and edge vertices weight 0 and sampling a small falloff texture with `(weight, 0)`. Suggested optimisations: own Mesh + ShaderProgram instead of immediate mode, fewer smoothing iterations, batch many swipes in one `GL_TRIANGLES` call. *(fetch summary)* — [lwjgl-basics wiki: LibGDX Finger Swipe](https://github.com/mattdesl/lwjgl-basics/wiki/LibGDX-Finger-Swipe)
- The actual extrusion code from that tutorial (Java). It builds two strips (one per side, `mult` = +1 / -1) that share the centre line, tapers linearly, and adds pointed end caps: *(direct)*
  ```java
  for (int i=1; i<input.size-1; i++) {
      Vector2 p = input.get(i);
      Vector2 p2 = input.get(i+1);
      perp.set(p).sub(p2).nor();            //get direction and normalize it
      perp.set(-perp.y, perp.x);            //get perpendicular
      float thick = thickness * (1f-((i)/(float)(input.size)));
      perp.mul(thick/2f);                   //move outward by thickness
      perp.mul(mult);                       //decide on which side we are using
      tristrip.add(new Vector2(p.x+perp.x, p.y+perp.y));
      texcoord.add(new Vector2(0f, 0f));    //0.0 -> end, transparent
      tristrip.add(new Vector2(p.x, p.y));  //add the center point
      texcoord.add(new Vector2(1f, 0f));    //1.0 -> center, opaque
  }
  ```
  — [gist mattdesl/5030570: SwipeTriStrip.java](https://gist.github.com/mattdesl/5030570)
- Miter joins for polylines: tangent = normalize(dirA + dirB); miter = perpendicular of the tangent; miter length = `halfThick / dot(miter, normalOfSegmentA)`. *(direct)*
  ```js
  module.exports.computeMiter = function computeMiter(tangent, miter, lineA, lineB, halfThick) {
      add(tangent, lineA, lineB)
      normalize(tangent, tangent)
      set(miter, -tangent[1], tangent[0])
      set(tmp, -lineA[1], lineA[0])
      return halfThick / dot(miter, tmp)
  }
  ```
  — [mattdesl/polyline-miter-util index.js](https://github.com/mattdesl/polyline-miter-util/blob/master/index.js); used per point by [mattdesl/polyline-normals index.js](https://github.com/mattdesl/polyline-normals/blob/master/index.js)
- Godot's Line2D builder shows the production answers to the hard cases: it falls back from a sharp (miter) joint to a bevel when the miter would be too long (`sharp_limit`, comment: "Fallback on bevel if sharp angle is too high (because it would produce very long miters)"); when inner edges of two segments do not intersect ("segments are too sharp or they overlap") it also switches to bevel; UV along the line is either `current_distance / (width * tile_aspect)` (tiled) or `current_distance / total_distance` (stretched); width comes from a curve sampled at `current_distance / total_distance`. *(direct)* — [godotengine/godot scene/2d/line_builder.cpp](https://github.com/godotengine/godot/blob/master/scene/2d/line_builder.cpp)
- PixiJS rope mesh (2 vertices per point, indexed triangles): perpendicular from the previous to the next point (`perpY = -(next.x - last.x); perpX = next.y - last.y`, normalised, times half width); U either accumulates `distance / (width * textureScale)` (tiling by arc length) or is `i / (total - 1)` (stretched); V is 0 and 1. *(direct)* — [pixijs RopeGeometry.ts](https://github.com/pixijs/pixijs/blob/dev/src/scene/mesh-simple/RopeGeometry.ts)
- Centripetal Catmull-Rom (knot spacing `t(i+1) = |P(i+1) - P(i)|^alpha + t(i)` with alpha = 0.5; alpha = 0 is uniform, 1 is chordal): the curve passes through its control points and, for the centripetal variant, "loops and self-intersections do not occur within a curve segment" and "cusps will never occur within a curve segment". The page gives the Barry-Goldman pyramid (A1..A3, B1, B2, C) and reference code in Python, C# and C++. *(fetch summary)* — [Wikipedia: Centripetal Catmull–Rom spline](https://en.wikipedia.org/wiki/Centripetal_Catmull%E2%80%93Rom_spline)
- Open-source trail plugins list the same feature set as the target: variable width via curve, colour via gradient, stretched or tiled texture, Chaikin smoothing, per-point lifetime (Godot Trail System, MIT per its README); and "Dewiggle and clip overlaps features for improved trail appearance", colour ramps and width curves (GPUTrail). *(direct, READMEs)* — [OBKF/Godot-Trail-System](https://github.com/OBKF/Godot-Trail-System); [celyk/GPUTrail](https://github.com/celyk/GPUTrail)

### Inferences

**Verified Java implementation (written for these notes; compiled with `javac` and run in this session).** Test: 7 control points on arcs of radius 15, 40 and 120 px over 200°, a zig-zag and an inward spiral; ribbon 90 px wide grown to either side; resampled at 6 px. Result for all 10 cases: spacing uniform (5.78–5.79 px on the r = 40 case) and **0 flipped triangles**. Before adding the neighbour min-filter on widths, the r = 40 inward case produced 1 flipped triangle, so that step is needed. The test checks triangle winding (local fold-over), not whether a spiral overlaps itself globally.

```java
/** Ribbon (GL_TRIANGLE_STRIP) along a polyline. Plain Java. */
public final class RibbonBuilder {
    /** Centripetal Catmull-Rom (Barry-Goldman form, alpha = 0.5). Evaluates between p1 and p2, s in [0,1]. */
    static void catmullRom(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                           float s, float[] out) {
        float t0 = 0f;
        float t1 = t0 + knot(x0, y0, x1, y1);
        float t2 = t1 + knot(x1, y1, x2, y2);
        float t3 = t2 + knot(x2, y2, x3, y3);
        float t = t1 + (t2 - t1) * s;
        float a1x = lerp(x0, x1, (t - t0) / (t1 - t0)), a1y = lerp(y0, y1, (t - t0) / (t1 - t0));
        float a2x = lerp(x1, x2, (t - t1) / (t2 - t1)), a2y = lerp(y1, y2, (t - t1) / (t2 - t1));
        float a3x = lerp(x2, x3, (t - t2) / (t3 - t2)), a3y = lerp(y2, y3, (t - t2) / (t3 - t2));
        float b1x = lerp(a1x, a2x, (t - t0) / (t2 - t0)), b1y = lerp(a1y, a2y, (t - t0) / (t2 - t0));
        float b2x = lerp(a2x, a3x, (t - t1) / (t3 - t1)), b2y = lerp(a2y, a3y, (t - t1) / (t3 - t1));
        out[0] = lerp(b1x, b2x, (t - t1) / (t2 - t1));
        out[1] = lerp(b1y, b2y, (t - t1) / (t2 - t1));
    }
    private static float knot(float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        return Math.max(1e-4f, (float) Math.pow(dx * dx + dy * dy, 0.25));   // |d|^0.5, never 0
    }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Step 1: smooth the per-frame tip positions. in = x,y pairs (n points). Returns x,y pairs. */
    public static float[] subdivide(float[] in, int n, int perSegment) {
        float[] out = new float[((n - 1) * perSegment + 1) * 2];
        float[] p = new float[2];
        int o = 0;
        for (int i = 0; i < n - 1; i++) {
            float x1 = in[2 * i], y1 = in[2 * i + 1], x2 = in[2 * i + 2], y2 = in[2 * i + 3];
            // phantom end points: mirror the neighbour so the curve starts/ends tangent to the first/last segment
            float x0 = i > 0 ? in[2 * i - 2] : 2 * x1 - x2, y0 = i > 0 ? in[2 * i - 1] : 2 * y1 - y2;
            float x3 = i < n - 2 ? in[2 * i + 4] : 2 * x2 - x1, y3 = i < n - 2 ? in[2 * i + 5] : 2 * y2 - y1;
            for (int k = 0; k < perSegment; k++) {
                catmullRom(x0, y0, x1, y1, x2, y2, x3, y3, k / (float) perSegment, p);
                out[o++] = p[0]; out[o++] = p[1];
            }
        }
        out[o++] = in[2 * n - 2]; out[o] = in[2 * n - 1];
        return out;
    }

    /** Step 2: resample at constant arc-length spacing so UVs and width are uniform. Returns x,y pairs. */
    public static float[] resample(float[] pts, int n, float spacing) {
        float total = 0;
        for (int i = 1; i < n; i++) total += dist(pts, i - 1, i);
        int m = Math.max(2, (int) Math.ceil(total / spacing) + 1);
        float[] out = new float[m * 2];
        float step = total / (m - 1), acc = 0, target = 0;
        int seg = 1, o = 0;
        for (int j = 0; j < m; j++, target = j * step) {
            while (seg < n - 1 && acc + dist(pts, seg - 1, seg) < target) { acc += dist(pts, seg - 1, seg); seg++; }
            float d = dist(pts, seg - 1, seg);
            float f = d > 1e-6f ? Math.min(1f, (target - acc) / d) : 0f;
            out[o++] = lerp(pts[2 * seg - 2], pts[2 * seg], f);
            out[o++] = lerp(pts[2 * seg - 1], pts[2 * seg + 1], f);
        }
        return out;
    }
    private static float dist(float[] p, int a, int b) {
        return (float) Math.hypot(p[2 * b] - p[2 * a], p[2 * b + 1] - p[2 * a + 1]);
    }

    /**
     * Step 3: extrude. pts = evenly spaced x,y pairs (n points), index 0 = tail (oldest), n-1 = head (newest).
     * Writes 2 vertices per point as x,y,u,v (8 floats per point) for GL_TRIANGLE_STRIP. Returns vertex count.
     * u = normalised arc length (0 tail .. 1 head), v = 0 on the outer (tip) edge, 1 on the inner edge.
     * The tip path itself is the outer edge; the ribbon grows towards 'side' (+1 = left of travel, -1 = right).
     * Width is clamped to the local radius of curvature when the ribbon grows towards the inside of the bend,
     * which is what prevents the inner edge from folding over itself on tight curves.
     */
    public static int build(float[] pts, int n, float width, float side, float[] out) {
        float[] w = new float[n];                                  // hoist to a field to avoid per-frame garbage
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            w[i] = width * widthProfile(u);
            if (i > 0 && i < n - 1) {                              // curvature from 3 consecutive samples
                float ax = pts[2 * i] - pts[2 * i - 2], ay = pts[2 * i + 1] - pts[2 * i - 1];
                float bx = pts[2 * i + 2] - pts[2 * i], by = pts[2 * i + 3] - pts[2 * i + 1];
                float la = (float) Math.hypot(ax, ay), lb = (float) Math.hypot(bx, by);
                float cross = ax * by - ay * bx;                   // >0: turning left
                if (la > 1e-6f && lb > 1e-6f && cross * side > 0) { // ribbon grows towards the centre of the bend
                    float sin = Math.abs(cross) / (la * lb);
                    float radius = 0.5f * (la + lb) / Math.max(1e-4f, sin);
                    w[i] = Math.min(w[i], radius * CLAMP);
                }
            }
        }
        int o = 0;
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 1), b = Math.min(n - 1, i + 1);
            float tx = pts[2 * b] - pts[2 * a], ty = pts[2 * b + 1] - pts[2 * a + 1];
            float len = (float) Math.hypot(tx, ty);
            if (len < 1e-6f) { tx = 1; ty = 0; } else { tx /= len; ty /= len; }
            float nx = -ty * side, ny = tx * side;                 // unit normal on the chosen side
            float wi = w[i];                                       // min over +-2 neighbours: no sudden width jumps
            for (int k = Math.max(1, i - 2); k <= Math.min(n - 2, i + 2); k++) wi = Math.min(wi, w[k]);
            float u = i / (float) (n - 1);
            out[o++] = pts[2 * i];            out[o++] = pts[2 * i + 1];            out[o++] = u; out[o++] = 0f;
            out[o++] = pts[2 * i] + nx * wi;  out[o++] = pts[2 * i + 1] + ny * wi;  out[o++] = u; out[o++] = 1f;
        }
        return n * 2;
    }
    static float CLAMP = 0.9f;

    /** Width profile along the trail: point at the tail, bulge near the head, small round-off at the very head. */
    static float widthProfile(float u) {
        float tail = (float) Math.pow(u, 0.6);                     // 0 at tail -> 1
        float head = smooth(1f, 0.9f, u);                          // 1 -> 0 over the last 10 %
        return tail * (0.25f + 0.75f * head);
    }
    static float smooth(float e0, float e1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0)));
        return t * t * (3f - 2f * t);
    }
}
```

Design notes (my reasoning):
- **Clean first.** Remove consecutive points closer than ~2 px before the spline (the `knot` guard avoids a division by zero, but duplicate points still make ugly tangents). mattdesl's pipeline does the same simplification first.
- **Why resample.** After even arc-length resampling every joint turns by a small angle, so plain normals behave like miters and the miter-length blow-up handled by Godot's `sharp_limit` cannot occur. Spacing of 4–8 px at game resolution gives 20–60 points per swing: 40–120 vertices.
- **Seams.** A single strip with U from arc length has no seams. Seams appear only if the trail is built from separate quads per animation frame; avoid that.
- **Outer edge = tip path.** Putting the tracked fist/foot tip on the outer edge (v = 0) and growing the ribbon to one side keeps the crisp leading edge exactly on the limb. `side` should be chosen once per attack from the overall turning direction (sign of the summed cross products) so the ribbon grows towards the body/inside of the arc; for a straight jab use a centred ribbon instead (offset both ways by half the width).
- **Head advances, tail erodes — without rebuilding the mesh.** Build the full ribbon once per attack (the path per attack is known at load time) and animate with the uniforms of the shader in section 1: `uHead = easeOut(age / swingTime)` (0→1 during the active frames), `uTail = easeIn((age - delay) / fadeTime)`, `uErode` rising after the hit, `uTailBias ≈ 0.4` so the tail always erodes first. With stepped time, compute them from `floor(age * fps) / fps`.
- **Alternative when the path is not known in advance** (e.g. free-moving projectile): keep a ring buffer of recent points with birth times, drop points older than a lifetime, rebuild the strip each frame (this is the Godot Trail System / PixiJS rope model).
- If the game already tracks two points per frame (e.g. fist and elbow, or blade tip and base), build the strip directly between the two smoothed paths; it cannot fold and needs no curvature clamp. That is the classic weapon-trail construction; no source was read for it in this session.

### Gaps
- No source was read on the specific look decisions of anime slash trails (how much bulge at the head, typical lifetime in frames). The width profile above is a starting point, not a sourced standard.
- The Effekseer runtime's ribbon/track renderer (which implements spline subdivision for ribbons) could not be located: my guessed file paths returned 404 and the repository tree could not be listed through the proxy.
- The ribbon builder was verified only numerically (winding), not visually and not on device.

---

## 3. Channel-packed effect textures (layout, resolution, memory, mipmaps, compression, procedural authoring)

### Takeaway
Pack unrelated grayscale layers (shape, erosion noise, mask) into the channels of one linear, non-sRGB texture; keep effect textures small (they are grayscale and get coloured in the shader) and, for small 2D effect textures, prefer uncompressed RGB8/RGBA8 over ETC2/ASTC, because block compression assumes correlated channels and packed data is uncorrelated by design. Tileable noise and streak textures are easy to generate in NumPy; the generator below was run and its seams measured.

### Cited Findings
- "Pack texture channels": combine single-channel textures into one multi-channel texture; this "reduces memory usage and decreases the number of texture sampler operations performed by the fragment shader". "When packing, assign data with the most detail to the green channel" because hardware usually gives green more bits. "Make sure that your packed textures are set to a linear RGB color space and not sRGB." *(direct)* — [Android Developers: Textures](https://developer.android.com/games/optimize/textures)
- Mipmaps add 33 % memory; "If a texture is drawn at a fixed distance from the camera, generating mipmaps is an unnecessary use of memory." Bilinear is "generally the best balance"; trilinear "requires significantly more memory bandwidth"; "Texture filtering may account for up to half of the total GPU energy consumption". *(direct)* — [Android Developers: Textures](https://developer.android.com/games/optimize/textures)
- Compression support: "ETC2 is supported by over 90% of active Android devices. Very old devices which do not support the OpenGL ES 3.0 API are unable to use ETC2"; ETC2 adds 8-bit and punch-through alpha, sRGB variants and one/two-channel R11/RG11; "ASTC is supported by over 75% of active Android devices", has configurable block sizes and is "generally the best primary option, with ETC2 being a fallback". *(direct)* — [Android Developers: Textures](https://developer.android.com/games/optimize/textures)
- Arm on packing non-colour data with ASTC (RGBM case): a channel that "is not directly storing a color" is "likely to be poorly correlated with the RGB channels"; ASTC can handle that with a second weight plane, its most expensive encoding option; the encoder can weight a channel more heavily (`-ch 1 1 1 5`); ETC2+EAC errors look more like dither speckle while ASTC showed block artifacts on that data. *(search summary)* — [Arm: High quality RGBM texture compression with ASTC](https://developer.arm.com/community/arm-community-blogs/b/mobile-graphics-and-gaming-blog/posts/high-quality-rgbm-texture-compression-with-astc)
- Erosion maps must be linear (sRGB off) or the erosion timing becomes non-linear. *(fetch summary)* — [VFXDoc: Alpha Erosion](https://vfxdoc.readthedocs.io/en/latest/shaders/alpha-erosion/)
- Flipbooks/effect textures "are often black and white, with color supplied by the particle system or shader"; noise and erosion maps are layered on top to break repetition. *(fetch summary)* — [VFX Apprentice: What are flipbooks](https://www.vfxapprentice.com/blog/what-are-flipbooks-in-games)
- Ready-made tileable Perlin/fractal noise for NumPy: `generate_perlin_noise_2d(shape, res, tileable=(True, True))` and `generate_fractal_noise_2d(shape, res, octaves, persistence, lacunarity, tileable)`; `shape` must be a multiple of `res` (and of `lacunarity^(octaves-1)*res` for fractal). Install: `pip3 install git+https://github.com/pvigier/perlin-numpy`. *(direct, README)* — [pvigier/perlin-numpy](https://github.com/pvigier/perlin-numpy)

### Inferences

**Recommended layout (my synthesis):**

| Channel | Content | Sampled with | Notes |
|---|---|---|---|
| R | Shape / intensity (brush streaks for trails, spark body, smoke puff). Doubles as the gradient-map input. | scrolling UV | brightest = hottest colour |
| G | Tileable erosion noise (fBm or Worley). Most detail goes here per the Android guide. | separately scrolling UV | must tile in the scroll direction |
| B | Static mask or gradient (edge falloff, radial gradient for sparks). Leave empty for ribbons where `u`/`v` give gradients for free. | static UV | costs a third sample if used |
| A | Unused → use RGB8, or a second noise | | avoid if not needed |

- **Sizes and memory (arithmetic, uncompressed):** 256×64 RGB8 trail texture = 49 152 bytes (≈48 KB); 256×256 RGB8 = 192 KB; 512×512 RGBA8 = 1 MB; 1024×1024 RGBA8 = 4 MB (+33 % with mipmaps per the Android guide). A handful of 256-class packed textures plus one 256×1 ramp per character costs far less than one 1024² coloured flipbook.
- **Compression decision:** for packed effect textures at 256-class sizes, ship uncompressed (PNG on disk, RGB8/RGBA8 in memory). Reasons: the saving is tens of kilobytes, while block artifacts are amplified by the erosion threshold (a hard `step` turns a small block error into a visible stair on the erosion edge — my reasoning from how thresholding works, not a sourced claim). If compression is wanted for large coloured flipbooks: ETC2 is guaranteed on ES 3.0 devices; ASTC needs a runtime extension check and an ETC2 fallback. Standard bit rates (from general knowledge, not re-verified here): ETC2 RGB 4 bpp, ETC2 RGBA8 (EAC alpha) 8 bpp, ASTC 4×4 8 bpp, 6×6 3.56 bpp, 8×8 2 bpp.
- **Mipmaps:** not needed for a fixed-zoom 2D fighter (Android guide: fixed distance → mipmaps are wasted memory). Add them only if effects are drawn much smaller than authored (camera zoom-out) and shimmer appears.
- **Wrap/filter:** `GL_REPEAT` on the scroll axis, `GL_CLAMP_TO_EDGE` across a ribbon; `GL_LINEAR`. Ramp textures: `GL_CLAMP_TO_EDGE` (polycount pitfall above). Non-power-of-two sizes are legal with repeat and mipmaps in ES 3.0 (general knowledge), but power-of-two sizes remain the safe choice.

**Verified Python/NumPy generator (written for these notes; run in this session with NumPy 2.5.3 and Pillow 12.3.0).** It produced a 256×64 packed PNG. Seam test printed: fBm mean difference across the wrap edge 0.0009 vs 0.0011 between adjacent interior columns; Worley 0.0123 vs 0.0123 — i.e. the wrap seam is indistinguishable from any other column step.

```python
import numpy as np
from PIL import Image

def tileable_value_noise(size, cells, rng):
    """Periodic value noise: random lattice of cells x cells, wrapped with modulo -> tiles seamlessly."""
    g = rng.random((cells, cells)).astype(np.float32)
    t = np.arange(size, dtype=np.float32) * cells / size
    i0 = np.floor(t).astype(int); f = t - i0
    f = f * f * (3 - 2 * f)                      # smoothstep fade
    i1 = (i0 + 1) % cells; i0 %= cells
    fx, fy = f[None, :], f[:, None]
    a = g[i0][:, i0]; b = g[i0][:, i1]; c = g[i1][:, i0]; d = g[i1][:, i1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy

def tileable_fbm(size, cells=4, octaves=4, seed=0):
    rng = np.random.default_rng(seed)
    out = np.zeros((size, size), np.float32); amp = 1.0; tot = 0.0
    for o in range(octaves):
        out += amp * tileable_value_noise(size, cells * 2 ** o, rng); tot += amp; amp *= 0.5
    return out / tot

def tileable_worley(size, npts=24, seed=0):
    """Tileable cellular (Worley) F1 distance: wrap the distance with min(d, 1-d) per axis."""
    rng = np.random.default_rng(seed)
    pts = rng.random((npts, 2)).astype(np.float32)
    u = (np.arange(size, dtype=np.float32) + 0.5) / size
    X, Y = np.meshgrid(u, u)
    d = np.full((size, size), 9.0, np.float32)
    for px, py in pts:
        dx = np.abs(X - px); dx = np.minimum(dx, 1 - dx)
        dy = np.abs(Y - py); dy = np.minimum(dy, 1 - dy)
        d = np.minimum(d, np.sqrt(dx * dx + dy * dy))
    return d / d.max()

def streak_texture(w, h, seed=0):
    """Horizontal brush streaks for a slash ribbon: U = along the trail (tiles), V = across."""
    n = tileable_fbm(max(w, h), cells=4, octaves=3, seed=seed)[:h, :w]
    rng = np.random.default_rng(seed + 1)
    rows = rng.random(h).astype(np.float32)                      # each row = one bristle
    k = np.exp(-0.5 * (np.arange(-4, 5) / 1.5) ** 2); k /= k.sum()
    rows = np.convolve(np.r_[rows[-4:], rows, rows[:4]], k, mode="valid")
    rows = (rows - rows.min()) / (rows.max() - rows.min())
    v = (np.arange(h, dtype=np.float32) + 0.5) / h
    profile = np.sin(np.pi * v) ** 0.6                           # fade to 0 at both edges
    streak = rows[:, None] * 0.7 + n * 0.3
    return np.clip(streak * profile[:, None], 0, 1)

if __name__ == "__main__":
    W, H = 256, 64
    shape = streak_texture(W, H, seed=3)                           # R: shape / intensity
    erode = tileable_fbm(256, cells=4, octaves=4, seed=7)[:H, :W]  # G: erosion noise (tiles along U)
    u = (np.arange(W, dtype=np.float32) + 0.5) / W
    grad = u[None, :] * np.ones((H, 1), np.float32)                # B: tail(0) -> head(1) gradient
    rgb = np.dstack([shape, erode, grad])
    Image.fromarray((rgb * 255 + 0.5).astype(np.uint8), "RGB").save("slash_packed.png")
```

Caveats on the generator: the streak layer came out with range 0.05–0.72 in the test run, so normalise it (`shape / shape.max()`) before packing or the hottest ramp colour is never reached; the script proves tiling and that the code runs, it does **not** prove the result looks good — that needs eyes on the rendered trail.

**Procedural vs hand-painted vs AI-generated (my assessment):** noise, gradients, ramps and streak bases are best generated in code (exactly tileable, linear, reproducible, parameterised per character by seed and ramp). Shapes that carry style — spark silhouettes, impact bursts, smoke puff outlines — are where a drawn or generated *grayscale* key frame matters; they then get their animation from erosion and their colour from the ramp instead of from extra drawn frames. That division is what lets one authored shape serve many characters.

### Gaps
- No source was found with resolution recommendations specific to 2D fighting-game effects on mobile; the size table is arithmetic plus the generic Android guidance.
- The Arm ASTC finding is from a search summary of an article about RGBM/HDR data, not about VFX masks; its transfer to packed effect masks is an inference.
- I did not find sourced guidance on AI-generated effect textures (quality, tiling, licensing).
- Simplex noise was not covered; the generator uses value-noise fBm and Worley only.

---

## 4. Flipbook best practices (atlas layout, blending vs stepping, premultiplied alpha, trimming, frame count/resolution)

### Takeaway
Stylized games keep flipbooks small — commonly capped around 4×4 = 16 frames, grayscale, coloured and eroded in the shader — and for an anime look frames should step, not cross-blend. Store them premultiplied to avoid dark fringes, and leave padding around each cell.

### Cited Findings
- Frames sit in a grid read left-to-right, top-to-bottom; grids may be uneven (the article's slash example is 8×2 = 16 frames). "Stylized games typically cap grids at a 4x4 (16 frames) layout" to keep file size down. *(fetch summary)* — [VFX Apprentice: What are flipbooks in games](https://www.vfxapprentice.com/blog/what-are-flipbooks-in-games)
- Resolution guidance from the same article: common sizes 128–4096 px square, 4096 "extreme and rare"; top-down games often use 512×512; "sidescrollers with larger FX may need at least 1024x1024"; a 3×3 flipbook in 512×512 gives about 170 px per frame. *(fetch summary)* — [VFX Apprentice](https://www.vfxapprentice.com/blog/what-are-flipbooks-in-games)
- Workflow: stylized flipbooks are often hand-drawn (Adobe Animate, Toon Boom Harmony, Krita, Photoshop); keep clear space around each frame so the engine does not crop edges; shaders add scale, UV scrolling, alpha erosion and colour shifts on top of the ordered frames. *(fetch summary)* — [VFX Apprentice](https://www.vfxapprentice.com/blog/what-are-flipbooks-in-games)
- Frame blending with motion vectors is the technique for *realistic* smoke/explosions: one practitioner example went from 11×11 (121 frames) to 6×6 with vectors; a "4K sheet with 64 frames" is described as standard for explosions; blending shows artifacts when consecutive frames differ too much. These are forum opinions about realistic 3D-game effects. *(search summary)* — [realtimevfx: Motion vectors for animated textures](https://realtimevfx.com/t/motion-vectors-for-animated-textures-how-do-you-generate-them/575)
- Premultiplied alpha removes the dark outline produced by `(alpha, 1-alpha)` blending on soft-edged sprites and fixes colour bleeding in mipmaps. *(direct)* — [dtrebilco/PreMulAlpha](https://github.com/dtrebilco/PreMulAlpha)
- Atlases reduce draw calls: "Using the same texture for different objects is one factor for combining them into a single draw call." *(direct)* — [Android Developers: Textures](https://developer.android.com/games/optimize/textures)

### Inferences
- **Hard stepping, not blending.** Cross-fading two drawn frames produces a ghosted double image that reads as "video", the opposite of limited animation. Use integer frame selection: `frame = min(floor(age * fps), count - 1)` with fps 12–15. Frame blending only makes sense for soft smoke, and even there the erosion shader gives a smoother fade for free. (Reasoning from the sources above; no source states this rule for anime-style 2D.)
- **Frame budget for hit sparks on mobile (suggestion, not sourced):** 6–10 frames at 12–15 fps (0.4–0.7 s), cells of 128–192 px for normal hits, 256 px for heavy hits, grayscale. Example: 8 frames × 128 px in a 512×256 sheet = 128 KB as R8, 512 KB as RGBA8. Per-character colour comes from the ramp, so one sheet serves every character; distinctness comes from ramp + erosion noise + scale/rotation jitter + which 2–3 sheets are layered.
- **Shader for a stepped flipbook cell (uncompiled):**
  ```glsl
  uniform vec2  uGrid;      // (cols, rows)
  uniform float uFrame;     // integer-valued float, computed on the CPU
  vec2 cell = vec2(mod(uFrame, uGrid.x), floor(uFrame / uGrid.x));
  vec2 uv   = (cell + vUv) / uGrid;     // vUv in 0..1 across the quad
  ```
  Then feed `texture(uTex, uv).r` into the erosion/ramp code of section 1.
- **Padding and trimming.** With `GL_LINEAR`, neighbouring cells bleed at cell borders: keep ≥2 px of empty (premultiplied zero) border per cell, or inset the UVs by half a texel. Trimming each frame to its bounding box and storing per-frame offsets saves fill-rate (fewer transparent pixels rasterised) more than memory; for 8-frame sparks a uniform grid is simpler and the saving is small — trim only large, mostly-empty effects (long smoke, ground dust).
- **Premultiply at load.** If PNGs are stored straight-alpha, multiply RGB by A once at load (or use `Bitmap`'s default premultiplied state on Android, which already holds premultiplied pixels — general Android knowledge, not re-verified) and blend with `GL_ONE, GL_ONE_MINUS_SRC_ALPHA`.

### Gaps
- No source with measured numbers for hit-spark frame counts/resolutions in shipped 2D fighters was found; the budget above is a suggestion.
- No source was read on atlas trimming for effects specifically.
- The VFX Apprentice article does not cover frame blending or channel packing (confirmed by the fetch), so those points rest on other, weaker sources.

---

## 5. Performance on mid/low-end Android GPUs (overdraw, batching, shader cost)

### Takeaway
The cost of 2D effects on mobile is almost entirely blended fill-rate: big additive quads stacked on top of each other. Vendor material confirms the mechanism (blending defeats the hidden-pixel optimisations of tile-based GPUs) but I found no authoritative numeric overdraw budget; keep effects small in screen area, few layers deep, and cheap per pixel, and measure on real devices.

### Cited Findings
- On Mali, blending disables early depth/stencil testing and Forward Pixel Kill, the main mechanisms that remove hidden-pixel work; translucency must be drawn back-to-front and therefore generates overdraw by design. The Mali Graphics Debugger has an overdraw map mode ("the whiter the area ... the more overdraw"). *(search summary)* — [Arm community: subpasses, alpha blending, FPK, early ZS](https://community.arm.com/support-forums/f/graphics-gaming-and-vr-forum/54421/subpasses-alpha-blending-fpk-early-zs); [Arm: Mali Graphics Debugger overdraw features](https://community.arm.com/arm-community-blogs/b/graphics-gaming-and-vr-blog/posts/new-mali-graphics-debugger-v1-2-features-improve-fragment-performance)
- "Minimizing the number of draw calls of a game frame is an important element in achieving optimal rendering performance ... each draw call incurs CPU overhead as it is processed by the graphics driver"; sharing one atlas texture is a precondition for merging draws. *(direct)* — [Android Developers: Textures](https://developer.android.com/games/optimize/textures)
- "Texture filtering may account for up to half of the total GPU energy consumption"; bilinear is the recommended default, trilinear costs significantly more bandwidth. *(direct)* — [Android Developers: Textures](https://developer.android.com/games/optimize/textures)
- Premultiplied alpha lets additive, alpha-blended and (grayscale) multiply effects share one blend state, so they can be batched in one draw call; the author presents this as important for frame-rate-sensitive mobile VR. *(direct)* — [dtrebilco/PreMulAlpha](https://github.com/dtrebilco/PreMulAlpha)
- Dithered (opaque, alpha-tested) erosion is used specifically to avoid blended overdraw for large smoke. *(fetch summary)* — [VFXDoc: Alpha Erosion](https://vfxdoc.readthedocs.io/en/latest/shaders/alpha-erosion/)
- Generic mobile guidance found only in forum/blog form: full-screen particle effects overdraw every pixel several times and should be avoided; use fewer, smaller particles with less overlap and simple shaders. No number given. *(search summary)* — [Unity discussions: overdraw / fill rate on mobile](https://discussions.unity.com/t/why-is-overdraw-fill-rate-such-an-issue-on-mobile-dev/459136)

### Inferences
- **Budget by screen coverage, not by particle count.** A workable self-imposed rule (my suggestion, unsourced): total blended effect area per frame ≤ about 1–1.5 screens' worth of pixels, with the heaviest moment (screen-filling super flash) being a single quad, not a stack. A ribbon trail covers a few percent of the screen; a hit spark of 256 px on a 1080p-class target is ~3 % per layer.
- **Shader cost.** The section-1 shader is 2 texture samples and no dependent branching — cheap. Costs to watch: a third sample (mask), `uRamp` (a third/fourth sample; the 3-uniform-colour ramp avoids it), and dependent texture reads (distortion makes sample 2 depend on sample 1; fine on ES 3.0-class GPUs but it is the first thing to drop on a low tier).
- **Tight geometry beats shader tricks.** The ribbon mesh only covers the trail's own area, whereas a flipbook quad for a slash covers its whole bounding box, most of it transparent. For large arcs the ribbon rasterises far fewer wasted pixels.
- **Batching in this engine.** Group effect draws by (program, texture); with premultiplied output the blend state never changes. Per-effect parameters that differ (erode, colours) prevent merging into one draw unless moved into vertex attributes; at fighting-game counts (a dozen effects on screen) separate draws are fine — draw calls matter in the hundreds, not the tens (my judgement, unmeasured).
- **Render-scale option.** If a device is fill-rate bound, rendering effects to a half-resolution offscreen target and compositing once is the classic fix for soft smoke; it blurs hard cel edges, so it suits smoke/dust only. No source was read for this in the session.
- **Low tier switch.** Expose a quality flag: no distortion, no rim, hard-step erosion via the existing colour matrix (section 1b), fewer smoke layers.

### Gaps
- **No numeric overdraw limit from Arm or Qualcomm was found.** The Arm Mali best-practices page on blending is JavaScript-rendered and returned no text to the fetch tool; no Qualcomm Adreno guidance was retrieved at all. Any "2.5× overdraw" style figure would be unsourced.
- Nothing was measured on a device. The project should profile with the vendor tools (Arm Performance Studio / Snapdragon Profiler) or Android GPU Inspector on its lowest target phone.

---

## 6. Tools: what can export effect assets headless from the command line on Linux

### Takeaway
None of the dedicated VFX authoring tools examined has a confirmed headless sprite-sheet recorder: Effekseer's CLI exists but only converts project files to runtime binaries/glTF, and its recorder is a GUI feature. The realistic automated routes on Linux are (1) plain code (Python/NumPy/Pillow, already working in this sandbox), (2) Blender in background mode (headless GPU rendering on Linux via EGL since 3.4), and (3) Godot's Movie Maker under a virtual display (`xvfb-run`), because Godot's own `--headless` flag uses a dummy renderer that produces no images. Effekseer effects can be played natively on Android through a native (JNI) runtime, but the ready-made Java bindings target libGDX.

### Cited Findings

**Effekseer**
- License: MIT ("The MIT License (MIT) Copyright (c) 2011 Effekseer Project"). *(direct)* — [effekseer/Effekseer LICENSE](https://github.com/effekseer/Effekseer/blob/master/LICENSE)
- The editor **does have command-line arguments**, read from its entry point: `-cui` (sets `gui = false`), `-in <file>`, `-o <file>` (re-save project), `-e <file>` (export), `-f <format>` (`gltf`, `glb`, otherwise the binary runtime format), `-m <magnification>`, `--materialcache`. In `Exec`, export does exactly one of: glTF export, GLB export, or `Binary.Exporter().Export(Core.Root, magnification)` written to the `-e` path. **There is no code path there that records frames or sprite sheets.** *(direct)* — [Effekseer Dev/Editor/Effekseer/Program.cs](https://github.com/effekseer/Effekseer/blob/master/Dev/Editor/Effekseer/Program.cs)
- The recorder is documented as an editor (GUI) feature: it outputs the effect as a sprite sheet, GIF, AVI etc.; "Export as a single image" arranges frames from upper-left to lower-right with "X Count" columns. *(search summary)* — [Effekseer tool reference: Record](https://effekseer.github.io/Help_Tool/en/ToolReference/record.html)
- Runtime platforms listed by the project: DirectX 9/11/12, Metal, Vulkan, OpenGL, WebGL, Unity, Godot, Unreal, DX Library, Cocos2d-x. Master is the 1.8 development branch ("Please don't use master branch for production"), with 1.7x/1.6x/1.5x branches. *(direct)* — [effekseer/Effekseer README](https://github.com/effekseer/Effekseer)
- Official multi-language bindings: "Effekseer for C#, Java and Python", built with SWIG + CMake; "The default build produces C#, Java and Python native libraries"; "the bindings use OpenGL and Windows DX9/DX11"; Java uses JNI (`EffekseerNativeForJava`); usage section points to libGDX examples. **Android is not mentioned in the README.** *(direct)* — [effekseer/EffekseerForMultiLanguages README](https://github.com/effekseer/EffekseerForMultiLanguages)
- GDXseer: Kotlin library for libGDX that renders Effekseer effects; based on Effekseer 1.70e and libGDX 1.12.1; supported platforms include "Android (OpenGLES)" with prebuilt `.so` for arm64-v8a, armeabi-v7a, x86, x86_64; manager constructed with `OpenGLDeviceType.OpenGLES3`; Linux listed as a future platform; Apache License 2.0. *(direct)* — [niraj-rayalla/GDXseer](https://github.com/niraj-rayalla/GDXseer); its predecessor: [SrJohnathan/gdx-effekseer](https://github.com/SrJohnathan/gdx-effekseer)
- jMonkeyEngine binding built on EffekseerForMultiLanguages supports only Windows 64-bit and Linux 64-bit; a forum reply says Android would need compiling the native library yourself and "won't necessarily work out of the box". *(search summary)* — [riccardobl/jme-effekseerNative](https://github.com/riccardobl/jme-effekseerNative); [jMonkeyEngine forum thread](https://hub.jmonkeyengine.org/t/effekseer-native-for-jme-wip/43673)

**Godot**
- `--write-movie <file>` runs the engine so a movie is written ("usually with .avi or .png extension"); `--fixed-fps` is forced and sets the movie FPS; `--quit-after N` sets the number of frames; `--resolution WxH` sets output size. Example: `godot --path /path/to/your_project --write-movie output.avi --fixed-fps 30`. *(direct)* — [Godot docs: Command line tutorial](https://docs.godotengine.org/en/stable/tutorials/editor/command_line_tutorial.html); [Godot docs: Creating movies](https://docs.godotengine.org/en/stable/tutorials/animation/creating_movies.html)
- PNG output is an image sequence; files get an 8-digit suffix (`folder/example00000000.png`, ...). "Transparency is supported, but the root viewport **must** have its `transparent_bg` property set to `true`" (project setting Rendering > Transparent Background). "the window size is clamped by your display's resolution". *(direct)* — [Godot docs source: creating_movies.rst](https://github.com/godotengine/godot-docs/blob/master/tutorials/animation/creating_movies.rst)
- `--headless` is defined as "Enable headless mode (`--display-driver headless --audio-driver Dummy`). Useful for servers and with `--script`." *(direct)* — [Godot docs source: command_line_tutorial.rst](https://github.com/godotengine/godot-docs/blob/master/tutorials/editor/command_line_tutorial.rst)
- A user report on Godot 4: with `--headless` the rendering server is "Dummy", so `get_viewport().get_texture().get_image()` returns no image; the post has no answers. *(fetch summary)* — [Godot forum: "Godot 4's --headless is not the same as Godot 3's --no-window"](https://forum.godotengine.org/150510/godot-headless-not-same-godot-window-cant-render-the-backend)
- One third-party doc claims `--write-movie` crashes under `--headless`. Single secondary source, unverified. *(search summary)* — [docs.summerengine.com: headless](https://docs.summerengine.com/automation/headless)

**Blender**
- Blender 3.4 (released 7 Dec 2022) added headless OpenGL rendering on Linux: the commit "Introduce headless OpenGL rendering on Linux" falls back to a surfaceless EGL context (`EGL_MESA_platform_surfaceless`) when no display can be initialised in background mode; "The headless backend only works on Linux as EGL is not used on Mac or Windows." Before that, EEVEE needed a display even with `-b` while Cycles did not. *(search summary; the commit page itself returned HTTP 403 to the fetch tool)* — [Blender commit 3195a38](https://projects.blender.org/blender/blender/commit/3195a38); [Blender devtalk: EEVEE background rendering](https://devtalk.blender.org/t/blender-2-8-unable-to-open-a-display-by-the-rendering-on-the-background-eevee/1436); [fostips: Blender 3.4 released](https://fostips.com/?p=8930)
- Older workaround widely reported: run under Xvfb. *(search summary)* — [Blender devtalk thread](https://devtalk.blender.org/t/blender2-82-xvfb-virtual-screen-rendering-with-strange-texture-results-eevee/12750)

**Unity**
- `-batchmode -nographics`: "Unity doesn't initialize a graphics device" — intended for automation on machines without a GPU, i.e. not for rendering; users report black frames when rendering with `-nographics`; Xvfb + `-force-opengl` is a reported, unverified workaround. *(search summary)* — [Unity manual: command-line arguments](https://docs.unity3d.com/Manual/PlayerCommandLineArguments.html)

**Pixel FX Designer (CodeManu)**
- GUI tool for Windows and macOS that renders pixel-art effects to PNG sprite sheets or GIFs; the developer stated no Linux version is planned; users have run it under Wine with problems. No command-line export found. *(search summary)* — [itch.io: Pixel FX Designer](https://codemanu.itch.io/particle-fx-designer); [itch.io thread: Linux version](https://itch.io/t/455092/linux-version)

**EmberGen (JangaFX)**
- Search found only GUI flipbook-export threads and a Linux crash-on-launch report; no documentation of a command-line or headless export. *(search summary)* — [JangaFX forum: EmberGen crashes on launch on Linux](https://forums.jangafx.com/t/embergen-crashes-on-launch-on-linux/2293)

**Aseprite (sheet packing, not effect authoring)**
- Has a documented CLI with `--batch` and `--sheet` for sprite-sheet export. *(search summary)* — [Aseprite CLI docs](https://www.aseprite.org/cli/)

**This sandbox (checked directly)**
- Installed: `python3` with NumPy 2.5.3 and Pillow 12.3.0, `ffmpeg`, `Xvfb`/`xvfb-run`, `javac`/`java`. Not installed: `blender`, `godot`, `astcenc`, `etc2comp`, `glslangValidator`. — local check, no URL

### Summary table (status of each claim as stated above)

| Tool | License | Truly headless on Linux? | Output | Native Java/Android playback? |
|---|---|---|---|---|
| Python + NumPy + Pillow (own generators) | open source | **Yes — ran in this session** | PNG, packed channels, sheets | n/a (assets only) |
| Effekseer editor | MIT | CLI converts `.efkproj/.efkefc` → runtime binary / glTF only. Sprite-sheet recording: **GUI only as far as found**. Whether `-cui` runs with no display on Linux: **not confirmed** | `.efk` binary, glTF/GLB (CLI); sheet/GIF/AVI (GUI) | Yes in principle: C++ runtime with OpenGL/GLES renderer via JNI. Ready-made: GDXseer (libGDX, Android GLES3, Apache-2.0). No drop-in for a non-libGDX engine found |
| Godot 4 Movie Maker | MIT (not re-verified here) | `--headless` = dummy renderer → no images (user report). Expected to work under `xvfb-run` with software GL: **not tested** | PNG sequence (alpha via `transparent_bg`), AVI | No (Godot is its own engine) |
| Blender (`-b -P script.py`) | GPL (not re-verified here) | Cycles: yes historically. EEVEE/Workbench: yes on Linux since 3.4 via EGL per release coverage; **Grease Pencil headless not confirmed** | PNG sequence with alpha | No |
| Unity batch mode | proprietary | `-nographics` does not render; needs GPU or virtual display | — | Not available in this game |
| Unreal (`-RenderOffscreen`) | proprietary | **Not researched successfully** | — | Not available in this game |
| Houdini (`hython`) | proprietary | **Not researched successfully** | — | No |
| EmberGen | proprietary | No CLI/headless export found | flipbooks (GUI) | No |
| Pixel FX Designer | proprietary | No (Windows/macOS GUI, no Linux) | PNG sheets, GIF | No |
| Aseprite CLI | proprietary (not re-verified) | `--batch` documented | sheets from existing frames | n/a |

### Inferences
- **Effekseer as an authoring tool for this project is a poor fit for full automation**: effects are authored interactively and the only confirmed way to get frames out is the GUI recorder. A scripted route would be to drive the *runtime* instead of the editor — e.g. the Python binding from EffekseerForMultiLanguages rendering into an offscreen GL framebuffer and reading pixels back — but nobody was found documenting that, and it needs a GL context (EGL/OSMesa or Xvfb). Treat as an experiment, not a known-good path.
- **Effekseer as a runtime on Android** is technically available (GDXseer proves the GLES3 native runtime builds and runs on Android ABIs), but GDXseer is bound to libGDX's asset loading and GL wrapper. For this custom engine it would mean: build the Effekseer C++ runtime + GLES renderer with the NDK, write a small JNI layer (the SWIG wrapper in EffekseerForMultiLanguages is the template), and share the GL context/state with the existing renderer. It would not cover the Java2D desktop path at all, which then needs baked frames anyway. Given that requirement, shader-driven effects + baked sheets are the lower-risk choice.
- **Godot route, if wanted**: `xvfb-run -s "-screen 0 1280x720x24" godot --path proj --write-movie out/frame.png --fixed-fps 12 --quit-after 16 --resolution 512x512` with `transparent_bg` enabled, then pack the PNG sequence with Pillow/ffmpeg. Under Xvfb the renderer is software GL (llvmpipe), which should be adequate for 2D particles/shaders at sprite-sheet resolutions but is slow and may differ from GPU output; "the window size is clamped by your display's resolution" means the virtual screen must be at least the output size. **Untested here.**
- **Blender route, if wanted**: `blender -b scene.blend -P render_fx.py` on Blender ≥ 3.4 with a script that sets film transparent, resolution and frame range and calls `bpy.ops.render.render(animation=True)`. Works for mesh/shader-based stylized effects; Grease Pencil in background mode needs a trial before relying on it.
- **Most practical pipeline for 50–60 effects** (my recommendation from the above): generate packed textures and ramps in Python (deterministic, runs headless today), drive variety at runtime with the shader parameters (ramp, noise seed/scroll, erosion timing, stepped fps, width profile), and bake to flipbooks with the same Python code only for the Java2D path and for effects whose shapes must be drawn.

### Gaps
- **Effekseer `-cui` on a display-less Linux box**: `app.Initialize(gui)` is called with `gui = false`, but I did not read what it initialises, and did not run it. Unknown whether it needs a display or native GUI libraries.
- **Godot `--write-movie` under Xvfb / with `--headless`**: no primary source (GitHub issue or docs) confirming either behaviour was retrieved; GitHub issue search was blocked by the sandbox proxy.
- **Blender**: the primary commit page returned 403; the 3.4 headless facts come from search summaries of the commit message and news coverage. Grease Pencil headless rendering: no source found.
- **Unreal batch/offscreen rendering and Houdini `hython` (including licence terms for Apprentice/Indie renders)**: the combined search returned only Unity results; nothing confirmed.
- **"FX Tool"-type sprite effect generators and open-source libraries for procedural slash/trail/spark sprite generation**: none found. I do not know of a maintained open-source library whose purpose is generating slash/spark sprite sheets from parameters.
- **Licences of EffekseerForMultiLanguages** (the `LICENSE` URL on master returned 404) and of Godot/Blender/Aseprite were not re-verified in this session.
- `.efkefc` format details were not researched beyond the CLI accepting an input file and exporting the runtime binary.

---

## 7. Open-source code worth studying (trails, hit effects, fighting-game sprite shaders)

### Takeaway
Good, readable reference code exists for each *building block* (ribbon extrusion, joints, arc-length UVs, palette-lookup sprite shader, premultiplied batching), but I did not find an open-source 2D game specifically known for anime-style slash trails or hit sparks whose effect code could be pointed to file-by-file.

### Cited Findings
- **Ikemen GO** — open-source fighting-game engine (Go, MUGEN-compatible; MIT per search summary). Its sprite fragment shader library shows a production 2D-fighter shader: indexed-palette lookup through a palette texture, per-draw `add`/`mult`/`alpha`/`gray`/`hue`/`neg` (PalFX), time uniforms (`iTime`, `sTime`), extra textures `tex1`/`tex2` and a `param[16]` array for custom shaders that `#include` the library. Existence and contents verified. *(direct)* — [src/shaders/sprite_lib.frag.glsl](https://github.com/ikemen-engine/Ikemen-GO/blob/master/src/shaders/sprite_lib.frag.glsl); [src/shaders/sprite.frag.glsl](https://github.com/ikemen-engine/Ikemen-GO/blob/master/src/shaders/sprite.frag.glsl); [repository](https://github.com/ikemen-engine/Ikemen-GO)
- **Godot Line2D builder** — joints (sharp/bevel/round) with the long-miter fallback, caps, width curve, gradient, tiled vs stretched UVs. *(direct)* — [scene/2d/line_builder.cpp](https://github.com/godotengine/godot/blob/master/scene/2d/line_builder.cpp)
- **PixiJS rope mesh** — the minimal 2D ribbon: two vertices per point, arc-length or stretched U. *(direct)* — [src/scene/mesh-simple/RopeGeometry.ts](https://github.com/pixijs/pixijs/blob/dev/src/scene/mesh-simple/RopeGeometry.ts)
- **mattdesl swipe trail (libGDX, Java)** — full tutorial and extrusion class; closest to the target language. *(direct for the gist; fetch summary for the wiki)* — [SwipeTriStrip.java gist](https://gist.github.com/mattdesl/5030570); [tutorial](https://github.com/mattdesl/lwjgl-basics/wiki/LibGDX-Finger-Swipe); [mattdesl/gdx-swiper repo](https://github.com/mattdesl/gdx-swiper) (individual file paths in the repo not verified)
- **mattdesl polyline utilities (JS)** — miter maths in ~30 lines. *(direct)* — [polyline-miter-util](https://github.com/mattdesl/polyline-miter-util/blob/master/index.js); [polyline-normals](https://github.com/mattdesl/polyline-normals/blob/master/index.js)
- **Godot trail plugins** — [OBKF/Godot-Trail-System](https://github.com/OBKF/Godot-Trail-System) (MIT per README; Chaikin smoothing, width curve, gradient, lifetime); [celyk/GPUTrail](https://github.com/celyk/GPUTrail) (GPU trail for Godot 4, "dewiggle" and "clip overlaps"). *(direct, READMEs only; source files not read)*
- **dtrebilco/PreMulAlpha** — demo and write-up for batching additive/alpha/multiply effects with one premultiplied blend state. *(direct)* — [repository](https://github.com/dtrebilco/PreMulAlpha)
- **Castagne** — fighting-game engine layer on Godot; licence allows commercial use with attribution. Its effects code was not inspected. *(search summary)* — [panthavma/castagne](https://github.com/panthavma/castagne)
- **GDXseer / EffekseerForMultiLanguages** — reference for binding the Effekseer runtime to Java on Android GLES3. *(direct, READMEs)* — [GDXseer](https://github.com/niraj-rayalla/GDXseer); [EffekseerForMultiLanguages](https://github.com/effekseer/EffekseerForMultiLanguages)

### Inferences
- Ikemen GO is the most relevant single codebase: a real 2D fighter renderer where hit sparks are flipbook sprites recoloured by palette and PalFX — the same architecture as this project (flipbooks + per-draw alpha + colour transform), extended with a palette texture. It confirms that palette/ramp lookup in the sprite shader is the conventional way to get per-character colour variants from shared art. It is not a source for ribbon trails (MUGEN-style engines use sprite afterimages rather than mesh trails — general knowledge of MUGEN, not checked in the code).
- For the trail itself, the combination "mattdesl tutorial for the pipeline + Godot line builder for the edge cases" covers everything the Java implementation in section 2 needs.

### Gaps
- No open-source 2D game or engine with *well-regarded anime-style* slash trails / hit effects was identified with links to its effect source files. Searches surfaced engines (Ikemen GO, Castagne) but no effect-specific code.
- The Godot trail plugins' source files and Castagne's effects were not read; only READMEs/search summaries.
- Effekseer's own ribbon/track renderer source (likely the best reference for spline-subdivided ribbons with UV modes) could not be located in this session.
- Several repository listings could not be retrieved because the GitHub API was not reachable from the sandbox for those repositories; file paths marked "not verified" should be opened before being cited.
