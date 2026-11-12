# Shading model and cost for lit painted sprites (single pass, GLES 3.0) without blow-out

Reading status legend: **[full]** = page/PDF text read in full or in the relevant section (PDFs extracted locally with pdftotext); **[section]** = only the relevant section read; **[snippet]** = search-result summary only, not verified on the page.

Sources actually read:
- Mitchell, Francke, Eng 2007, "Illustrative Rendering in Team Fortress 2" (NPAR) — https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf **[full, sections 5–7]**
- Mitchell, McTaggart, Green 2006, "Shading in Valve's Source Engine" (SIGGRAPH course) — https://steamcdn-a.akamaihd.net/apps/valve/2006/SIGGRAPH06_Course_ShadingInValvesSourceEngine.pdf **[section 8.4]**
- Karis 2013, "Real Shading in Unreal Engine 4" notes — https://cdn2.unrealengine.com/Resources/files/2013SiggraphPresentationsNotes-26915738.pdf **[section on falloff]**
- Lagarde & de Rousiers 2014, "Moving Frostbite to PBR" v3.2 course notes — https://seblagarde.files.wordpress.com/2015/07/course_notes_moving_frostbite_to_pbr_v32.pdf **[section on punctual light attenuation]**
- Sloan 2008, "Stupid Spherical Harmonics (SH) Tricks" — https://www.ppsloan.org/publications/StupidSH36.pdf **[section "Extracting Conventional Lights from SH"]**
- W3C Compositing and Blending Level 1 — https://www.w3.org/TR/compositing-1/ **[full up to char 100000; blend-mode section read]**
- Pegtop soft light page — http://www.pegtop.net/delphi/articles/blendmodes/softlight.htm **[full]**
- 64.github.io "Tone Mapping" — https://64.github.io/tonemapping/ **[full]**
- Franzke 2014, "Dynamic 2D Character Lighting" (Game Developer) — https://www.gamedeveloper.com/programming/dynamic-2d-character-lighting **[full]**
- Gooch et al. 1998, SIGGRAPH paper web version — https://users.cs.northwestern.edu/~ago820/SIG98/paper/node6.html **[full, but the equation is an image]**
- Unity URP Asset manual 14.0 — https://docs.unity3d.com/Packages/com.unity.render-pipelines.universal@14.0/manual/universalrp-asset.html **[full]**
- Unity Toon Shader 0.9, "Shading Step and Feather" — https://docs.unity3d.com/Packages/com.unity.toonshader@0.9/manual/ShadingStepAndFeather.html **[full]**
- 4Gamer, GUILTY GEAR Xrd -SIGN- graphics article, part 2 (2014) — https://www.4gamer.net/games/216/G021678/20140714079/ **[full, saved copy from an earlier session]**

Blocked or unreadable: the ARM Mali best-practices page (https://developer.arm.com/documentation/101897/latest/Shader-code/Minimize-precision redirects to support.arm.com, which returned only a title and no body text). The lilToon shadow docs URL I guessed (lilxyzw.github.io/lilToon/en_US/settings/shadow.html) returned 404. A search for the lilToon docs found nothing usable. The godotshaders "2D cast shadow" page served the code as truncated base64.

---

## 1. Combine rules and brightness ceilings (classic sum, half-Lambert, warped ramps, ambient cube, hemisphere, Gooch, toon shadow colour)

### Takeaway
The production reference is Valve's: `albedo × [ambient(n) + Σ c_i · w(0.5·N·L + 0.5)]`. The sum is not clamped; TF2 even allows a 0..2 overbright output. Valve keeps it from blowing out in two ways. Only the 2 strongest lights get per-pixel diffuse, and every other light is folded into a 6-colour ambient cube. The art-controlled ramp has a top value only slightly above mid-grey (×2 = about 1.0–1.2). For this game the useful parts are: a ramp ceiling the artist controls, a cool-coloured shadow region instead of black, half-Lambert so shading wraps, and folding extra lights into ambient.

### Cited Findings
- **TF2 Equation 1, verbatim structure:** `k_d [ a(n̂) + Σ_{i=1..L} c_i w( (α (n̂·l̂_i) + β)^γ ) ]`. Here "k_d is the albedo of the object sampled from a texture map, n̂·l̂_i is a traditional unclamped Lambertian term", and "α, β and γ are a scale, bias and exponent". "a() is a function which evaluates a directional ambient term as a function of the per-pixel normal n̂". "w() is a warping function which maps a scalar in the range of 0..1 to an RGB color." — [TF2 2007](https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf)
- **Half Lambert values:** Since Half-Life (1998), Valve applies "a scale by 0.5, bias by 0.5 and square to diffuse lighting terms … (α = 0.5, β = 0.5 and γ = 2)". The purpose is "to prevent characters from losing a sense of shape on the back side". In TF2, "we leave α and β at 0.5 but set the exponent γ to 1 since we can express any shaping … in the warping function w()." — [TF2 2007](https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf)
- Valve 2006 on Half Lambert: "This is a completely non-physical technique but the perceptual benefit is enormous given the trivial implementation cost." — [Valve 2006 §8.4.4](https://steamcdn-a.akamaihd.net/apps/valve/2006/SIGGRAPH06_Course_ShadingInValvesSourceEngine.pdf)
- **Ramp ceiling and coloured shadow (TF2 warp texture):** "the rightmost value in the texture is not white but is, rather, only slightly brighter than mid-gray. This is done because there is a multiplication by 2 in the pixel shader after the lookup … the output is in the 0..2 range." The texture "has essentially three regions: a grayscale gradient on the right, a cool gradient on the left and a small reddish terminator region in the middle … illustrated shadows often tend toward cool colors, not black, and … there is often a slight reddening at the terminator." The warp is "evaluated with a lookup into the artist-generated 1D texture". — [TF2 2007](https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf)
- **TF2 Equation 2 (view-dependent terms, rim):** `Σ_{i=1..L} [ c_i k_s max( f_s (v̂·r̂_i)^{k_spec}, f_r k_r (v̂·r̂_i)^{k_rim} ) ] + (n̂·û) f_r k_r a(v̂)`. Definitions from the paper: "û is a world-space up vector"; "k_rim is a constant exponent which controls the breadth of the rim highlights"; "f_r is another Fresnel term used to mask rim highlights (typically just (1 − (n̂·v̂))^4)"; "k_r is a rim mask texture"; "a(v̂) is an evaluation of the ambient cube using a ray from the eye through the pixel". The (n̂·û) term is "clamped to be positive", so the dedicated rim adds light "only for upward facing normals … to exploit the human instinct to assume that lighting tends to come from above." — [TF2 2007](https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf)
- TF2 also notes that minification of normal maps "tends to smooth out diffuse lighting". It warns that "slowly-varying tangent frames can result in overly-broad Fresnel terms, leading to rim highlights that are more obtrusive than we would like." — [TF2 2007](https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf)
- **Ambient cube, verbatim HLSL:**
  ```hlsl
  float3 AmbientLight( const float3 worldNormal )
  {
        float3 nSquared = worldNormal * worldNormal;
        int3 isNegative = ( worldNormal < 0.0 );
        float3 linearColor;
        linearColor = nSquared.x * cAmbientCube[isNegative.x]   +
                      nSquared.y * cAmbientCube[isNegative.y+2] +
                      nSquared.z * cAmbientCube[isNegative.z+4];
        return linearColor;
  }
  ```
  The paper gives its cost as "(6 vertex shader instructions or 11 pixel shader instructions)". "the six required colors can be loaded into shader constants." — [Valve 2006 §8.4.1](https://steamcdn-a.akamaihd.net/apps/valve/2006/SIGGRAPH06_Course_ShadingInValvesSourceEngine.pdf)
- **Light budget and folding:** "On ps_2_0 video cards (20% of our users) we compute diffuse illumination for up to two local lights. Any nearby local lights beyond the two most significant lights are added into the ambient cube on the fly." On ps_2_b/ps_3_0, Source "also computes Phong terms for the two most significant local lights." — [Valve 2006 §8.4.3](https://steamcdn-a.akamaihd.net/apps/valve/2006/SIGGRAPH06_Course_ShadingInValvesSourceEngine.pdf)
- Sloan notes that the ambient cube is "more efficient to evaluate compared to SH, having 6 basis functions with only 3 being non-zero at any point". It is not rotation-invariant, while SH is (shown in his comparison figure). — [Sloan](https://www.ppsloan.org/publications/StupidSH36.pdf)
- **Hemisphere lighting (orange-book style GLSL):** `float costheta = dot(tnorm, lightVec); float a = 0.5 + 0.5 * costheta; gl_FrontColor = mix(GroundColor, SkyColor, a);`. The Unity/Cg form is `float w = 0.5 * (1.0 + dot(upDirection, normalDirection));`. — [yaldex OpenGL Shading Language ch12](https://mail.yaldex.com/open-gl/ch12lev1sec1.html) **[snippet]**; [Wikibooks GLSL/Unity Skylight](https://en.wikibooks.org/wiki/GLSL_Programming/Unity/Diffuse_Reflection_of_Skylight) **[snippet]**. Hargreaves: "use the y component of the vertex normal to interpolate between them" — [Hargreaves radiosity notes](https://shawnhargreaves.com/radiosity_lighting.pdf) **[snippet]**
- **Gooch 1998:** the cosine term (l·n) is used "to blend between two RGB colors, k_cool and k_warm". The undertones are "a linear blend between the blue/yellow and black/object-color tones". The equation itself is an image on the page, so the following is the commonly reproduced form, NOT a verbatim quote: `I = ((1+l·n)/2) k_cool + (1 − (1+l·n)/2) k_warm`, `k_cool = k_blue + α k_d`, `k_warm = k_yellow + β k_d`, `k_blue=(0,0,b)`, `k_yellow=(y,y,0)`. Example values: b=0.4, y=0.4, α=0.2, β=0.6 (Fig. 7) and b=0.55, y=0.3, α=0.25, β=0.5 (Fig. 8). "the exact values are not critical"; intermediate values are used so the shading does not interfere with black and white. — [Gooch 1998 node6](https://users.cs.northwestern.edu/~ago820/SIG98/paper/node6.html). The Qt3D gooch.frag clamps k_cool/k_warm to [0,1] — [Qt3D gooch.frag](https://code.qt.io/cgit/qt/qt3d.git/tree/src/extras/shaders/gl3/gooch.frag?h=6.1) **[snippet]**
- **UTS (Unity Toon Shader) step/feather model:** "Base Color Step: Sets the boundary between the Base Color and the Shade Colors." "Base Shading Feather: Feathers the boundary between the Base Color and the Shade Colors." "Shading Color Step: Sets the boundary between the 1st and 2nd Shade Colors. Set this to 0 if 2nd Shade Color is unnecessary." "Step Offset: Fine tunes light steps (boundaries) added in the ForwardAdd pass, such as real-time point lights." The page gives no blending formula, only three zones (base / 1st shade / 2nd shade). — [UTS 0.9 manual](https://docs.unity3d.com/Packages/com.unity.toonshader@0.9/manual/ShadingStepAndFeather.html)
- **GUILTY GEAR Xrd "×2 multiply" blend:** Xrd blends decal textures with "Dest×Source＋Source×Dest＝結果的に乗算したものの2倍" (dest×src + src×dest = twice the multiply). This gives "0.5未満で暗，0.5で透明，0.5超で明" (below 0.5 darkens, 0.5 is neutral, above 0.5 brightens). Xrd itself renders into an FP16 RGBA (64-bit) HDR buffer with bloom, and bloom applies above luminance 1.0. — [4Gamer Xrd part 2](https://www.4gamer.net/games/216/G021678/20140714079/)

### Inferences
- The owner's "blown out" result follows directly from stacking additive terms (volume add + point light add + rim) on art that is already near white. Valve never adds light to albedo. Every light lands inside the `[ ]` bracket that multiplies `k_d`, so black clothes stay black. Adding light on top of the albedo is what greys black areas and whitens skin.
- A 2×-multiply operator with mid-grey = neutral (Xrd decals; TF2 ramp ×2 with top ≈ 0.55–0.6) is a natural "relight relative to the art" form: `out = albedo × 2·T`, with T in [0, ~0.6]. The ceiling is then set by T_max (e.g. 0.6 → gain 1.2).
- Gooch and the TF2 ramp both say the same thing: shadows should shift hue toward cool and keep about 20–50 % of the albedo, not drop to black.

### Gaps
- Arc System Works GDC 2015 (Motomura) details of shadow colour (base × "SSS"/tint texture, vertex-colour threshold) were not fetched in this session, so there is no sourced formula. Genshin Impact ramp-texture talks and the lilToon parameter docs were also not reached (404 / not found).

---

## 2. Preventing over-brightness in LDR (tone curves, screen/soft-light, relight ratio, luminance-preserving)

### Takeaway
There are operators bounded by 1 (screen, soft light, Pegtop soft light, Reinhard) that cost a few ALU each. Soft-light-type operators automatically protect near-white and near-black art: `(D(Cb) − Cb)` and `Cb(1−Cb)` go to 0 at both ends. That is exactly the "highlight headroom" behaviour wanted. Use a luminance-based curve to avoid hue shifts. A per-channel curve desaturates highlights, and some argue this is desirable.

### Cited Findings
- **W3C formulas (verbatim):** Multiply `B(Cb, Cs) = Cb x Cs`. Screen `B(Cb, Cs) = Cb + Cs -(Cb x Cs)`. Overlay `B(Cb, Cs) = HardLight(Cs, Cb)`. Hard-light: `if(Cs <= 0.5) B = Multiply(Cb, 2 x Cs) else B = Screen(Cb, 2 x Cs -1)`. Soft-light: `if(Cs <= 0.5) B(Cb, Cs) = Cb - (1 - 2 x Cs) x Cb x (1 - Cb) else B(Cb, Cs) = Cb + (2 x Cs - 1) x (D(Cb) - Cb)`, with `if(Cb <= 0.25) D(Cb) = ((16 * Cb - 12) x Cb + 4) x Cb else D(Cb) = sqrt(Cb)`. Color-dodge: `min(1, Cb / (1 - Cs))`. Non-separable: `Lum(C) = 0.3 x Cred + 0.59 x Cgreen + 0.11 x Cblue`. ClipColor: `if(n < 0) C = L + (((C - L) × L) / (L - n))`, `if(x > 1) C = L + (((C - L) × (1 - L)) / (x - L))`. SetLum: `d = l - Lum(C)` … `return ClipColor(C)`. Luminosity `B = SetLum(Cb, Lum(Cs))`. — [W3C Compositing L1](https://www.w3.org/TR/compositing-1/)
- **Pegtop soft light (verbatim):** `f(a,b) = (1 - a) * ab + a * [1 - (1 - a) * (1 - b)]`. Equivalent split: `f(a,b) = a - (½ - b) * [½ - (½-a)²]` (b < ½), `a + (b - ½) * [½ - (½-a)²]` (else). The page contrasts this with Photoshop `2ab + a²(1 - 2b)` / `sqrt(a)(2b - 1) + 2a(1 - b)`, which has a "small discontinuance" at b = ½. Pegtop "does not share the disadvantage". — [Pegtop](http://www.pegtop.net/delphi/articles/blendmodes/softlight.htm)
- **Tone curves (verbatim GLSL):** Reinhard `v / (1.0f + v)`. Extended Reinhard: `numerator = v * (1.0f + (v / vec3(max_white * max_white))); return numerator / (1.0f + v);`. Luminance version: `l_new = l_old*(1+l_old/(max_white_l²))/(1+l_old)` applied via `change_luminance(c_in, l_out) = c_in * (l_out / l_in)` with `luminance = dot(v, vec3(0.2126, 0.7152, 0.0722))`. Hable/Uncharted 2: A=0.15, B=0.50, C=0.10, D=0.20, E=0.02, F=0.30, `((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F`, exposure_bias 2.0, W=11.2. Narkowicz ACES fit: `v*=0.6; clamp((v*(2.51*v+0.03))/(v*(2.43*v+0.59)+0.14),0,1)`. The page says Reinhard was designed for luminance only, and "Other tone mapping curves look better when applied to RGB components separately". It also quotes Naty Hoffman: the per-channel "hue and saturation shifts … are an important feature, not a bug." — [64.github.io tonemapping](https://64.github.io/tonemapping/)
- Franzke (MISE): additive light-map blending made reflected light "barely visible on bright pixels (e.g. Guybrush's shirt)". The alternative is sampling the light map in the character shader (light pre-pass). — [Franzke 2014](https://www.gamedeveloper.com/programming/dynamic-2d-character-lighting)
- Xrd (4Gamer): bloom acts above luminance 1.0, and "diffusion" starts at lower luminance. In fight mode the post effects are weak because heavy blur "遊びにくくなってしまう" (makes it harder to play). — [4Gamer Xrd part 2](https://www.4gamer.net/games/216/G021678/20140714079/)

### Inferences
- Candidate "relight" operator for painted art: compute a scalar or RGB light gain `G = ambient + Σ lights` that is ≈1 on average. Then apply `out = albedo × G` in the multiply-dominant range (G ≤ 1). Above 1, use soft light / screen-style headroom: `out = albedo + (G−1)·(D(albedo) − albedo)`, which is the W3C soft-light upper branch with Cs = G/2. This brightens mids and leaves near-white and near-black almost untouched, and it never exceeds 1. Cost: ~1 sqrt + a few MAD, or Pegtop with no sqrt at all.
- Extended Reinhard with white point W on the *gain* (not the colour) gives a hard ceiling `G_max` with a smooth knee. Applying it on luminance and rescaling RGB keeps hue.
- Screen with a light colour has a ceiling of 1 and never darkens. It still greys blacks (black + screen(L) = L), so use it only for the small specular/rim part, not for the main diffuse.

### Gaps
- No source fetched on the GLES3 cost of `GL_SRGB8_ALPHA8` sampling or of pow, or on linear vs gamma. I found no documented game use of "light gain × (1 − luma)". The "protect highlights" behaviour is shown here only via the soft-light math.

---

## 3. Multiple lights in one pass: budgets, windowing, folding into one virtual light

### Takeaway
Use 1–2 per-pixel lights plus a folded ambient term (Valve). Every light must window smoothly to zero at its radius so that choosing the N strongest per sprite does not pop (Karis/Frostbite). Extra lights can be collapsed on the CPU into one dominant direction plus ambient via an SH-L1 projection (Sloan's "optimal linear direction" and the 867/(316π) factor).

### Cited Findings
- **UE4 falloff (verbatim, eq. 9):** `falloff = saturate(1 − (distance/lightRadius)^4)^2 / (distance^2 + 1)`. "The 1 in the denominator is there to prevent the function exploding at distances close to the light source. It can be exposed as an artist-controllable parameter for cases where physical correctness isn't desired." This "is likely the largest bang for buck takeaway." — [Karis 2013](https://cdn2.unrealengine.com/Resources/files/2013SiggraphPresentationsNotes-26915738.pdf)
- **Frostbite:** a linear window `(I/d²)(1 − d²⁰/r²⁰)` "causes a hard cutoff which looks unnatural". Scale-bias with `threshold = 1/lightRadius²` "suffers from having a non-zero gradient at 0 which causes a visible discontinuity". Adopted: `E_window1 = (I/distance²) saturate(1 − x^n/lightRadius^n)²`, "n = 4 for Frostbite, as presented by Karis". Code (verbatim):
  ```hlsl
  float smoothDistanceAtt ( float squaredDistance , float invSqrAttRadius )
  {
      float factor = squaredDistance * invSqrAttRadius ;
      float smoothFactor = saturate (1.0f - factor * factor ) ;
      return smoothFactor * smoothFactor ;
  }
  ```
  and `attenuation = 1.0 / ( max ( sqrDist , 0.01*0.01) )`. — [Frostbite PBR v3.2 §4.x](https://seblagarde.files.wordpress.com/2015/07/course_notes_moving_frostbite_to_pbr_v32.pdf)
- **Valve:** 2 most significant local lights are evaluated per pixel. The rest are "added into the ambient cube on the fly". — [Valve 2006](https://steamcdn-a.akamaihd.net/apps/valve/2006/SIGGRAPH06_Course_ShadingInValvesSourceEngine.pdf)
- **Unity URP:** Additional Lights: "Choose between Per Vertex, Per Pixel, or Disabled." Per Object Limit: "This slider sets the limit for how many additional lights can affect each GameObject." "If you don't assign a sun source, the URP treats the brightest directional light in the scene as the main light." The page does not state numeric maxima. — [URP 14 asset docs](https://docs.unity3d.com/Packages/com.unity.render-pipelines.universal@14.0/manual/universalrp-asset.html)
- **Sloan, extracting one light from SH:** you minimise `E(c,a)` so that a directional light of intensity c plus ambient a best matches the SH lighting. The optimal values are `c = (867/(316π)) · dot₀(L̂_d, L̂_e)` and `a = (L̂_e[0] − c·(8√π/17))·√π/2`, as laid out in the PDF; check the layout against the original. "The dot product above ignores the DC term". "a good candidate direction is the 'optimal linear' direction, formed by normalizing the vector (−L̂_e[3], −L̂_e[1], L̂_e[2]), which are the linear coefficients of the lighting environment." "The optimal linear direction works well when a light probe is dominated by a single light source." Footnote: the normalization assumes ≤4th-order lighting, and for 5th/6th order the factor is 32π/31. "Directional lights are trivial to compute, you simply evaluate the SH basis functions in the given direction and scale appropriately." — [Sloan](https://www.ppsloan.org/publications/StupidSH36.pdf)
- **Franzke (Broken Age / MISE):** lights are radial basis functions. Ambient tint is "a distance-weighted average of nearby sources, with a global light as fallback". — [Franzke 2014](https://www.gamedeveloper.com/programming/dynamic-2d-character-lighting)

### Inferences
- CPU per-sprite fold (built from Sloan's L1 projection; the code is my derivation, not quoted). For each light i, with weight `w_i = I_i · smoothDistanceAtt(d_i²)` and unit direction `l_i` toward the light:
  - L1 vector `S += w_i · l_i`
  - DC `D += w_i`
  - Dominant direction `dir = normalize(S)`
  - Colour `col = Σ w_i c_i · max(0, dot(l_i, dir))`
  - Remainder `D − |S|` (light coming from scattered directions) goes into ambient.
  The ratio `|S|/D` (0..1) measures how directional the set is. Use it to blend between "one directional light" and "flat ambient" so the model never adds extra lights on top of each other.
- To avoid popping when the top-N set changes: windowed attenuation already sends lights to 0 at the radius. Also cross-fade the N-th light's weight by `(w_N − w_{N+1})/w_N` over a short time, or simply always fold everything into the virtual light.
- Ceiling: with one virtual light plus ambient normalised so that `ambient + col ≤ G_max`, the sum can no longer exceed the ceiling, whatever the number of scene lights. A CPU clamp of the per-sprite packet is cheaper than per-pixel tone mapping.

### Gaps
- No Mali/Adreno document with ALU counts per point light was readable (ARM page blocked/empty). I found no source for Unity's numeric per-object limits (commonly said to be 8 per object; not verified here).

---

## 4. Fallback without normal maps

### Takeaway
Franzke's production-proven methods: (a) an ambient gradient tint sampled by a pseudo-normal from the bounding-box centre (top of sprite = sky colour, feet = ground/AO colour), and (b) normals from a blurred alpha height field. Both need no extra assets.

### Cited Findings
- Ambient gradient: "tinting the sprite (or texture)" with a gradient lets the upper and lower body take different colours and "emulate ambient occlusion between the floor and the character". Simplest version: "vertices on the head sample from top of the gradient whereas the feet use the bottom of the gradient." In Broken Age it is used in every scene for every character. Normals are "the vectors from the translated center of the bounding box to each vertex", and the gradient is sampled by normal.y. Moving the centre offset changes how fast the gradient transitions. — [Franzke 2014](https://www.gamedeveloper.com/programming/dynamic-2d-character-lighting)
- Local light in MISE: the alpha mask is rendered "into an offscreen render-target", downsampled and blurred "using a Gaussian kernel". Then "compute the normals of the blob approximation as the gradient of the height field". The light model is "a very simple Labertian lighting model (aka 'n dot l')". Pseudo-code verbatim: `vec l = light_pos - fragment_world_pos / vec n = tex2D(normal_map, fragment_screen_pos) / scalar nDotL = saturate(dot(normalize(l), n)) / scalar dist = length(l) / vec result = light_color * nDotL / (dist * dist)`. The article gives no cost figures. — [Franzke 2014](https://www.gamedeveloper.com/programming/dynamic-2d-character-lighting)
- Hemisphere `mix(ground, sky, 0.5+0.5·n.y)` is the same idea as the gradient tint (see §1 sources).

### Inferences
- With no normal map, a per-vertex bounding-box pseudo-normal (Franzke) feeds the same ambient/hemisphere and virtual-light formulas at zero extra texture taps. Directional light becomes `0.5+0.5·dot(pseudoN.xy, L.xy)` across the box. This is the "gradient overlay in the light direction" asked about.
- An alpha-offset rim (sample alpha at uv + lightDir·k and light pixels where `alpha_here − alpha_offset > 0`) costs 1 extra tap. I found no source documenting it in this session.

### Gaps
- Genshin-style screen-space depth rim, the "inner rim from offset alpha" trick, and per-sprite light-probe tint: no sources read.

---

## 5. Shadows for sprites (blob, projected/sheared silhouette, multiple shadows)

### Takeaway
Only forum-level sources were found: shear the shadow sprite along the light-to-object direction, scale by height/distance, fade by light strength, and draw it on a separate layer. No numeric opacity/penumbra references with authority.

### Cited Findings
- A MonoGame dev computed the angle from light to blocker with atan2, rotated the shadow around the object, derived the skew from the sine of that angle, scaled the shadow by light distance and faded it by light strength. — [MonoGame community](https://community.monogame.net/t/solved-matrix-2d-skewing/11027) **[snippet]**
- Skew offsets each pixel row in proportion to its height, and "only looked right within ±45 degrees". — [yal.cc](https://yal.cc/?p=1024) **[snippet]**
- A shadow-only separate sprite/light layer avoids the shadow being lit or cast onto the character. — [Unity discussions, Octopath-style shadows](https://discussions.unity.com/t/how-do-games-like-octopath-traveler-handle-the-angle-of-shadows-and-is-this-even-possible-in-unity-urp/246459) **[snippet]**
- Godot "2D cast shadow" shader "works by stretching the x-coordinates of the vertices horizontally", with a `look_right` parameter. Code not readable. — [godotshaders](https://godotshaders.com/shader/2d-cast-shadow/) **[section]**
- TF2: the ambient cube's bounced light "is critical to truly grounding characters". — [TF2 2007](https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf)

### Inferences
- Shear matrix for a ground-projected silhouette, with the light at elevation θ and azimuth φ in screen x: `x' = x + (y − y_feet)·cot θ·sign`, `y' = y_feet + (y − y_feet)·k_flatten`. Length grows as the light gets lower. Opacity can be ∝ the light's share of the total gain from §3, so with a single dominant light there is one shadow.

### Gaps
- No authoritative source found for blob size/opacity vs height, penumbra widening, or stadium-style multi-shadow numbers.

---

## 6. Noisy normals, precision and GLES3 cost

### Takeaway
Little was sourced this session. TF2 confirms that minifying (mip-filtering) normal maps smooths diffuse lighting and that broad normals make rims obtrusive. The Valve ambient cube costs 11 pixel-shader instructions as a reference point.

### Cited Findings
- "minification of normal maps which tends to smooth out diffuse lighting" — [TF2 2007](https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf)
- Ambient cube: "6 vertex shader instructions or 11 pixel shader instructions" — [Valve 2006](https://steamcdn-a.akamaihd.net/apps/valve/2006/SIGGRAPH06_Course_ShadingInValvesSourceEngine.pdf)
- TF2 prefers a warp ramp over a hard toon threshold: "we preserve the variation in illumination for all normals while tightening the transition from light to dark around the terminator". — [TF2 2007](https://steamcdn-a.akamaihd.net/apps/valve/2007/NPAR07_IllustrativeRenderingInTeamFortress2.pdf)
- UTS exposes a feather (softening width) for each step boundary — [UTS 0.9](https://docs.unity3d.com/Packages/com.unity.toonshader@0.9/manual/ShadingStepAndFeather.html)

### Inferences
- With low-res estimated RG8 normals, use half-Lambert plus a wide smoothstep/feather (TF2-style ramp) rather than a hard step. Reconstruct `z = sqrt(max(0, 1 − x² − y²))` and scale xy by a "strength" < 1 before renormalising. Bilinear filtering of RG8 then acts like the smoothing TF2 describes.

### Gaps
- ARM Mali / Adreno guidance on mediump vs highp, the cost of normalize/pow/exp, branching and uniform arrays could not be read (ARM docs redirected to an empty page). Nothing sourced on `GL_SRGB8_ALPHA8` cost.
