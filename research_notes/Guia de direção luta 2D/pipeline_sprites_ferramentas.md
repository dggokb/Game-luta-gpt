# Sprite production pipelines, style unification and rendering tools for a 2D fighting game (custom Java/OpenGL ES 3 engine)

How to read these notes: everything under "Cited Findings" is DOCUMENTED, with a URL. Everything under "Inferences" is INFERRED, meaning my reasoning applied to this project's pipeline (AI anime video on green screen, then Python scripts to sprite sheets; AI-painted stages; green-screen VFX videos; DBFZ as the reference). Where a source is secondhand (forum, review, vendor), the bullet says so. Research date: 2026-11-09.

---

## 1. Production pipelines: how studios and indies turn rotoscope, 3D or video into sprites that look hand-made

### Takeaway
No professional 2D fighter ships raw 3D renders or raw video frames. Every documented pipeline (KOF XII/XIII, Skullgirls, Dead Cells, Guilty Gear Xrd/DBFZ) has the same three steps: (a) use the source (3D, keys) only for pose and timing; (b) cut down to a small set of strong key poses, often with deliberately uneven ("limited") timing; (c) run a cleanup pass that redraws lines and replaces source lighting with simple, consistent cel shading under one set of rules. That cleanup pass is what this project is missing.

### Cited Findings

**SNK, The King of Fighters XII/XIII (3D model as the underdrawing, then hand-drawn dot art on top)**
- DOCUMENTED: A 4Gamer article (Japanese) says each character has a dedicated 3D model that is posed and lit, then output as 2D frames. Those frames are the underdrawing (下絵). Artists then hand-draw illustration-style expressions and shading over them as dot art: "2Dドット絵の前段階として，まず3Dモデルを用いたポージングやライティングなどを行って描画を行い，それを2Dフレームとして出力。…さらにイラストテイストの表情や陰影をドット絵として描くポストプロセスを施す" — [4Gamer, 2010](https://www.4gamer.net/games/111/G011100/20100820077/index_2.html)
- DOCUMENTED: Lighting on the 3D model is only diffuse toon shading. Specular highlights are placed by hand ("3Dモデルへのライティングは拡散反射系のトゥーンシェーディングのみ…鏡面反射のハイライトは手付け"). Punch and kick poses start from IK and are then deliberately roughened for a more intense feel. Some models have exaggerated proportions, such as oversized fists, because they are built for the final 2D look. — [4Gamer](https://www.4gamer.net/games/111/G011100/20100820077/index_2.html)
- DOCUMENTED: The finished sprites get no real-time 3D lighting at runtime. Brightness changes and flashes are done by program-driven color manipulation. — [4Gamer](https://www.4gamer.net/games/111/G011100/20100820077/index_2.html)
- DOCUMENTED: Producer Kei Yamamoto describes the same order: 3D model, pose, light, output as 2D frames, then hand work. — [PlayStation Blog, KOF XIII interview, 2011](https://blog.eu.playstation.com/2011/11/01/the-king-of-fighters-xiii-interview-fighting-its-way-back-to-the-throne/). KOF XII's HD sprites were marketed as "drawn 100% by hand." — [Shacknews](https://shacknews.com/article/55973/king-of-fighters-12-bringing)
- Conflict note: fan claims that the XII/XIII sprites were "3D models retraced as pixels" are unsourced. The 4Gamer interview shows the 3D was only a base and the final sprites differ from it in proportions and details. — [4Gamer](https://www.4gamer.net/games/111/G011100/20100820077/index_2.html)

**Lab Zero, Skullgirls (hand-drawn HD, keyframes then outsourced in-betweens and cleanup)**
- DOCUMENTED: Pipeline order: the designer picks the attacks, then keyframes are animated and put into the game to tune timing and feel. "Based on that we determine the number of frames for each animation," and then contractors do the in-betweens and cleanup. — Lab Zero blog "Diamonds in the Rough: Animation Clean-Up" [skullgirls.com/?p=114](https://skullgirls.com/?p=114) (quoted via search-engine extract; the page itself now redirects to a generic Skullgirls site and could not be re-read).
- DOCUMENTED: The cleanup artist "takes rough, sketchy frames … and solidifies the lines". Because "our animators don't animate the shading," cleanup artists also have to interpret and render the shading, on top of keeping the character on model. — same Lab Zero post, [skullgirls.com/?p=114](https://skullgirls.com/?p=114) (same caveat).
- DOCUMENTED: Guinness record for most animation frames per character: 11,515 frames across the first 8 characters, about 1,439 per fighter on average. — [Guinness World Records](https://www.guinnessworldrecords.com/world-records/105505-most-frames-of-animation-per-character-in-a-videogame)
- DOCUMENTED: Lead animator Mariel Cartwright's GDC 2014 Animation Bootcamp talk covers fluid animation within frame restrictions, such as "only six frames to deliver a punch," built on strong key frames, anticipation and timing. — [GDC Vault](https://gdcvault.com/play/1020575/Animation-Bootcamp-Fluid-and-Powerful); [Siliconera summary, 2016](https://siliconera.com/?p=538907); live demo talk [Game Developer](https://gamedeveloper.com/art/video-i-skullgirls-i-animator-mariel-cartwright-presents-a-live-animation-demo)
- Unverified: a forum estimate of about 4 finished frames per artist per day comes from a poster who says they don't know how Skullgirls was made. Do not use it as data. — [Dakka forum](https://www.dakkadakka.com/dakkaforum/posts/list/510710.page)

**Motion Twin, Dead Cells (3D animated, rendered as 2D frames; a good analog for a "source then cleanup" pipeline)**
- DOCUMENTED: The artist draws a pixel-art model sheet first, then builds and skins the 3D model. Frames are rendered small with no antialiasing (the in-game character is about 50 px tall), with a basic toon shader plus a normal map exported per frame. — [Thomas Vasseur, Game Developer, 25 Jan 2018](https://www.gamedeveloper.com/production/art-design-deep-dive-using-a-3d-pipeline-for-2d-animation-in-i-dead-cells-i-)
- DOCUMENTED: Animation is pose-to-pose keyframes. Interpolation frames are added "before or after the key frames. Never in-between." VFX carry the sense of impact. Timing is fixed by moving keys, not redrawing. Known weakness: "we still haven't found any solution for flickering pixels." — [same article](https://www.gamedeveloper.com/production/art-design-deep-dive-using-a-3d-pipeline-for-2d-animation-in-i-dead-cells-i-)

**Arc System Works, Guilty Gear Xrd / DBFZ / Granblue Fantasy Versus (3D made to read as 2D)**
- DOCUMENTED: GDC 2015, "GuiltyGearXrd's Art Style: The X Factor Between 2D and 3D" (Junya C. Motomura), covers how the art style was chosen and built. — [GDC Vault](https://gdcvault.com/play/1022031/GuiltyGearXrd-s-Art-Style-The); ASW mirror with handout [arcsystemworks.com](https://www.arcsystemworks.com/guilty-gear-xrd-art-talk-at-game-developers-conference-2015/); slides PDF [ggxrd.com](https://www.ggxrd.com/Motomura_Junya_GuiltyGearXrd.pdf) (returned 503 when fetched).
- DOCUMENTED (secondhand summary of the talk): ASW used limited animation, meaning fewer, held drawings. Full animation was tested and rejected because it made the characters read as 3D. — [noggi3d talk summary](https://noggi3d.notion.site/guilty-gear-style-summary); [Gamestar on DBFZ using the same approach](https://www.gamestar.de/artikel/dragon-ball-fighterz,3325366.html)
- DOCUMENTED (secondhand): vertex normals were hand-edited on parts such as the heads to get clean light/shadow shapes. Vertex-color channels control the shadow threshold and line thickness. One texture packs shadow map, specular and interior lines (alpha = inner lines). — [Polycount thread](https://polycount.com/discussion/comment/2567560); [Unreal forum shader playground](https://forums.unrealengine.com/t/guilty-gear-xrd-shader-playground/74763)
- DOCUMENTED (primary, ASW, 28 Nov 2025): "Guilty Gear Toon Line Control Techniques." Every ASW 3D fighter since Xrd uses the inverse-hull outline. Vertex-color alpha scales line width: 0.5 is standard, 1.0 doubles it, 0 removes it. Thin lines go on hair and small details, thick lines on big volumes. Width is corrected by camera distance × tan(FOV/2) so line weight stays constant on screen. — [ASW Academy / docswell](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)
- DOCUMENTED: DBFZ VFX are "traditional keyframed 2d style animation, but as multiple individual 3d meshes." This is a single expert forum post (bgolus, 2017), not ASW. — [realtimevfx.com](https://realtimevfx.com/t/dragonball-fighter-z/2522)
- DOCUMENTED: GBVS uses the same 2.5D cel-shaded approach as Xrd/DBFZ, on Unreal Engine. Cygames chose ASW because the GG/BlazBlue look fit Granblue. — [Shacknews review](https://www.shacknews.com/article/116659/granblue-fantasy-versus-review-opening-the-skies-with-beauty-in-simplicity); [Unreal Engine developer interview](https://www.unrealengine.com/ja/developer-interviews/inventive-combat-in-granblue-fantasy-versus-combines-accessibility-with-new-layers-of-depth)

**Them's Fightin' Herds, Pocket Bravery**
- DOCUMENTED: TFH was built from the earlier fan project's beta assets after the Hasbro C&D. A Russian source says Lab Zero gave Mane6 its (Skullgirls) engine. — [Wikipedia](https://en.wikipedia.org/wiki/Them%27s_Fightin%27_Herds); [DTF.ru](https://dtf.ru/p/31097) (single source on the engine)
- DOCUMENTED: Pocket Bravery (Statera Studio, Rio de Janeiro, founded 2020) is a pixel-art fighter whose special-move animation reviewers praise. No process interview was found. — [Wikipedia](https://en.wikipedia.org/wiki/Pocket_Bravery); [Siliconera review](https://www.siliconera.com/?p=983520)

**Indie 3D-to-2D tools**
- DOCUMENTED: C-Dogs SDL renders sprite sheets from Blender by script, with ImageMagick touch-up. — [cxong write-up (via search)](https://cxong.github.io/2017/03/3d-rendered-pixel-sprites); the Kidbash pipeline goes Blender → Aseprite → Unity — [daily.dev summary](https://daily.dev/posts/how-kidbash-mixes-retro-style-graphics-with-a-modern-art-pipeline-jhtmcx4g6); the True Pixel Art Generator Blender add-on renders high resolution and then resolves down to the target pixel size — [Superhive](https://superhivemarket.com/products/true-pixel-art-generator-v2); a 2025 paper describes a deterministic hybrid 3D-to-2D pipeline using Blender compositing nodes — [Sanggitarupa journal](https://jurnal.isi-ska.ac.id/index.php/sanggitarupa/article/view/8428)

### Inferences (INFERRED)
- The client's frames come from AI video at 24–30 fps of smooth, interpolated motion with photographic-style lighting that changes from clip to clip. That is the opposite of what KOF, Skullgirls and ASW ship: few frames, held keys, and lighting rules applied after the fact. The likely main cause of the amateur look is the missing cleanup step, not the generator.
- A cleanup recipe for the Python pipeline, mapped to the documented practices:
  1. **Key-pose selection, not decimation.** Pick startup / active / recovery key poses per move (Cartwright's "six frames for a punch"; Dead Cells' "never in-between"). Hold impact frames, and drop the AI's smooth in-betweens. A per-move JSON listing frame indices plus hold durations, chosen by a human, is closer to pro practice than an automatic every-Nth-frame rule.
  2. **Lighting reset.** Throw away the video's shading. Posterize luminance into 2–3 cel bands (base, shadow, optional highlight), with the shadow side always on the same screen side (for example, light from front-top-left). This mirrors KOF's "diffuse toon only, hand specular" rule. A normal/shape guide could come from a depth/normal estimator run on the frame. That is speculative and not documented for fighting games.
  3. **Fixed per-character palette.** Quantize every frame of a character to one master palette (16–48 colors per region) extracted from a hand-approved reference frame, with no dithering. That kills color drift between clips and also enables palette swaps (see section 3).
  4. **Line pass.** Redraw or regenerate the outline at a uniform screen width (ASW: thick on outer silhouette and big volumes, thin on interior detail), in one line color per character (a dark tone of the local color, not pure black). Video edge detection alone will flicker. Smoothing over time or hand-fixing key frames is needed.
  5. **Green spill and matte.** Despill and remove the fringe before quantizing, otherwise green ends up in the palette.
  6. **Human pass on keys only.** As at Lab Zero, contract or do cleanup by hand on the 3–6 key frames of each move, which is where off-model AI errors show most.

### Gaps
- No primary Lab Zero numbers for per-frame cleanup time, sprite resolution, or color counts (the blog page no longer renders).
- No primary source on the Them's Fightin' Herds animation tools (Toon Boom, Photoshop, etc.) or on Pocket Bravery's sprite process.
- The full Motomura GDC 2015 slide PDF could not be fetched (HTTP 503). Details on limited-animation timing (which frames are held, for how long) come from secondhand summaries.
- I found no published postmortem of a commercial fighting game built from AI-generated video sprites.

---

## 2. Style unification across characters, stages and VFX

### Takeaway
The documented rules are few but consistent. Fighters own the highest saturation and value contrast. Stages are pushed back in value and saturation and avoid distracting motion. VFX use the same rendering language as the characters: ASW's VFX are keyframed 2D-style animation on cel-shaded meshes, with the same line logic. I found no official DBFZ or SF6 stage art-direction document, so the DBFZ-specific rules below are inferred from observation and general readability sources.

### Cited Findings
- DOCUMENTED (general game-art guidance): most readability failures are value failures. A detailed background in the same mid-tones as the actors swallows them, so background art should be pushed back in value and saturation before detail is added. — [bugnet.io, "Readable game art"](https://bugnet.io/blog/readable-game-art-why-clarity-beats-detail)
- DOCUMENTED: Swords & Soldiers II desaturated clutter (corpses) so units read in front of it, and adjusted background brightness near the gameplay layer. — [Making gameplay stand out against rich backgrounds (dev blog, 2015)](https://tanzaniangamiabooty.blogspot.com/2015/05/making-gameplay-stand-out-against-rich.html); see also [Game Developer, "How to reduce visual confusion in your game"](https://www.gamedeveloper.com/design/how-to-reduce-visual-confusion-in-your-game)
- DOCUMENTED (indie fighting-game devlog, ~2023): each character should have its own palette for readability. Stages should be "interesting yet not overpowering," and stage secondary action "should NOT be distracting." — [Moldy Skull Games, Zeitgeist devlog 3](https://moldy-skull-games.itch.io/zeitgeist/devlog/577786/3-fighting-game-readability-and-you)
- DOCUMENTED: SF6 art director Kaname Fujioka designed Luke around "colors that are easy to understand with an emphasis on blue and orange" (a strong two-color read per character). — [Game Informer, Nov 2022](https://www.gameinformer.com/2022/11/23/street-fighter-6-art-director-breaks-down-each-revealed-character-design). SF6 stages were partly built by the outsourcer Streamline Studios (environments, props, set dressing, lighting). — [Streamline case study](https://streamline-studios.com/case-study-posts/street-fighter-vi)
- DOCUMENTED: DBFZ developers aimed to keep the original work in mind and convey the series' sense of scale in stages. Attacks visibly damage the stage backdrop. — [Famitsu interview via ShonenGameZ](https://www.shonengamez.com/?p=12822); [GamingBolt preview](https://gamingbolt.com/?p=301016)
- DOCUMENTED: KOF XIII flashes and brightness changes on sprites are done by program-side color manipulation, not real-time lighting. This is one global mechanism applied to all sprites, which keeps them consistent. — [4Gamer](https://www.4gamer.net/games/111/G011100/20100820077/index_2.html)
- DOCUMENTED: DBFZ VFX use keyframed 2D-style animation on 3D meshes, the same limited-animation logic as the characters (expert forum comment). — [realtimevfx.com](https://realtimevfx.com/t/dragonball-fighter-z/2522)
- DOCUMENTED: ASW controls line width in screen space (distance × tan(FOV/2)) so line weight stays constant. This is the mechanism behind its uniform line look. — [ASW Toon Line Control, 2025](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)

### Inferences (INFERRED, from observing DBFZ/SF6 and the general sources above)
- **One "style bible" spec shared by the three asset types.** Write numbers, not adjectives: light direction (one screen side), number of cel bands (2 or 3), line width in px at the reference resolution (for example, outer 3 px and inner 1–2 px at 1080p character height), line color rule, and the allowed value range per layer, for example: characters at value 15–95%, saturation up to 100%; stage midground at 30–80% value and ≤60% saturation; far background at 45–85% value and ≤40% saturation. These exact numbers are proposals, not sourced.
- **DBFZ-like stage treatment** (observation, not documented): painted backgrounds with little line work and soft edges, atmospheric perspective (lighter and bluer with distance), a floor plane with low-frequency texture, and saturated highlights reserved for fighters and VFX. A "squint/grayscale test" works well here: render a screenshot in grayscale, and fighters must separate from the stage by value alone (supported by the bugnet value-first argument).
- **Unifying AI-painted stages**: run all stage paintings through the same post-process. Repaint or reduce texture detail on the play-area band, apply a stage LUT, add a gentle blur on far layers (depth of field), and slightly lower contrast. Give characters a shared rim/ambient tint from the stage (the project already has a stage-light tint; keep it subtle so the character palette stays recognizable).
- **Unifying green-screen VFX**: video VFX carry photographic gradients that clash with cel-shaded characters. Posterize them to the same 2–3 band scheme, add the same outline rule (or a uniform dark core plus light rim), limit each effect to the character's or element's palette ramp, and re-time them to held frames (limited animation, like the characters). Additive glows should be a separate layer so the core shape stays readable.
- **Shared palette discipline**: build a master palette (for example, 64–128 colors) and let each character use a sub-ramp. Stages use the same hues at lower saturation and value. VFX use the hottest values. This is the 2D analog of SF6's "easy to understand" two-color characters.

### Gaps
- No primary DBFZ or SF6 interview or talk found that explains how stage art is kept subordinate (desaturation, depth of field, contrast numbers). Claims about DBFZ stages above are observation and inference only.
- I found no sourced value or saturation numbers used by any commercial fighter.
- No primary ASW source on DBFZ VFX production (only an expert forum comment).

---

## 3. Rendering techniques usable in a custom OpenGL ES 3 2D engine

### Takeaway
Everything needed is cheap fragment-shader work that runs on ES 3.0: palette lookup from indexed sprites (the classic fighting-game approach: Ikemen/MUGEN PalFX, KOF-style program color manipulation), screen-space outline from alpha, rim light and 2D lighting from generated normal maps (Laigter), per-stage LUT grading, and separable blur for stage depth of field. Primary documentation exists for the palette technique, PalFX parameters, and normal-map generation. The others are standard techniques I describe from general knowledge, marked as such.

### Cited Findings
- DOCUMENTED, palette swap via indexed grayscale: store each sprite as 8-bit grayscale where the gray level is the palette index, quantize offline with Pillow (`convert("P", colors=N, dither=NONE)`), then look up the color in the shader. GLSL: `int index = int(texture(Texture, TexCoords).r * 255.0); Color = Palette[index];` (the palette can be a uniform array or a 256×N texture, one row per costume). — [pvigier, "Palette swapping with shaders," 2019](https://pvigier.github.io/2019/10/06/palette-swapping-with-shaders.html)
- DOCUMENTED: the same red-channel-index idea is in the spriterecolour tool: `out.rgb = GetPaletteColour(in.r, in.g)`. — [sinbad/spriterecolour](https://awesome.ecosyste.ms/projects/github.com%2Fsinbad%2Fspriterecolour)
- DOCUMENTED: Ikemen GO PalFX (MUGEN-compatible) parameters: duration, add, mul, sine-wave modulation (sinadd), color (saturation level) and invert. Used for hit flashes and power-up tints "without need to duplicate sprites." — [Ikemen GO wiki, Lifebar features](https://github-wiki-see.page/m/ikemen-engine/Ikemen-GO/wiki/Lifebar-features)
- DOCUMENTED: a Skullgirls Mobile palette editor rebuilds palettes from the sprite sheet's colors, and noisy (anti-aliased) edges add extra color rows. This shows why edge cleanup before indexing matters. — [Krazete/sgmpalette](https://github.com/Krazete/sgmpalette)
- DOCUMENTED, 2D lighting: Laigter (GPL-3.0, free, Win/macOS/Linux) generates normal, specular, parallax and occlusion maps from 2D sprites, with a real-time lighting preview. — [Laigter docs](https://laigter.readthedocs.io/en/latest/Introduction/intro.html); [itch.io](https://azagaya.itch.io/laigter); [GitHub (via ecosyste.ms)](https://awesome.ecosyste.ms/projects/github.com%2Fazagaya%2Flaigter)
- DOCUMENTED: Dead Cells exports a normal map per frame and lights sprites with a toon shader. — [Game Developer, 2018](https://www.gamedeveloper.com/production/art-design-deep-dive-using-a-3d-pipeline-for-2d-animation-in-i-dead-cells-i-)
- DOCUMENTED: ASW line-width formula (distance × tan(FOV/2)) and per-vertex width control. Post-process outlines keep a constant thickness regardless of camera distance. — [ASW Toon Line Control](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)
- DOCUMENTED: KOF XIII sprites get runtime color manipulation, not runtime lighting. — [4Gamer](https://www.4gamer.net/games/111/G011100/20100820077/index_2.html)

### Inferences (INFERRED, standard technique; not from a fighting-game source unless noted)
- **Palette swap for this engine**: the project already has a hue-band swap shader for mirror matches. Moving to true indexed sprites (R8 texture plus a 256×K RGBA8 palette texture, `GL_NEAREST`) gives exact, artist-controlled alternate colors, and also lets PalFX-style hit flashes (mul/add/sin) be one uniform set. Requirement: frames must be quantized to the fixed palette offline (section 1, step 3), and texture filtering on the index texture must be NEAREST. To scale smoothly, render the palette result to an FBO first, or accept nearest sampling.
- **Outline shader (when useful)**: a sprite-space outline (sample alpha at N offsets, output the line color where the center is transparent and a neighbor is opaque) is useful for (a) unifying line weight across characters from different AI clips, (b) readability against busy stages, and (c) SHINKAI or selection glows. It is not a replacement for interior lines. Run it at a fixed screen-pixel width (ASW's constant-width principle) by passing texel size × scale as a uniform.
- **Rim light**: in 2D without normals, derive it from the alpha gradient (the difference between alpha at offset −lightDir and the center), multiplied by a stage-defined rim color. With Laigter normals, use `max(dot(N, L), 0)` banded into 2 steps to stay cel-like. Keep the rim on the side opposite the key light to "seat" characters in the stage, which DBFZ does visibly in screenshots (observation).
- **Stage depth of field**: blur far parallax layers offline (cheapest on mobile) or apply a separable 9-tap Gaussian once per layer per resolution change, not per frame. Use real-time blur only for dynamic effects (supers, camera zoom).
- **Per-stage LUT grading**: a 16³ or 32³ LUT stored as a 2D strip (for example 256×16) sampled in the final pass works on ES 3.0 (3D textures are also core in ES 3.0). Apply a strong grade to stage layers and a weak, shared grade (for example 10–20% blend) to characters and VFX, so all three share a color temperature without wrecking the character palettes.
- **Order of passes** (proposed): stage layers (pre-blurred, graded) → shadows/floor contact → characters (palette lookup → cel light/rim → outline) → VFX (same palette ramps, additive glow last) → weak global grade → HUD.

### Gaps
- I found no open-source GLSL from a shipped commercial fighter (Skullgirls/Z-Engine palette shader code is not public). Lab Zero's palette implementation is undocumented in public sources I could find.
- Ikemen GO's internal shader source was not examined in this session. The MIT-licensed repo is the best place to read a production-grade PalFX implementation: [ikemen-engine/Ikemen-GO](https://github.com/ikemen-engine/Ikemen-GO).

---

## 4. AI-assisted pipelines (2025–2026): what helps, what fails

### Takeaway
As of 2025–2026, AI tools are documented as reliable for matting (BiRefNet), upscaling and interpolation, and for image-to-video base motion (Wan 2.2 i2v). Frame-to-frame character consistency and exact pose adherence are still the reported failure points, especially for fighting-game poses, smears and non-human proportions. The evidence is mostly anecdotal (single developers, vendor blogs). No vendor accuracy claim should be trusted.

### Cited Findings
- DOCUMENTED (first-hand, May 2025): a ComfyUI test making fighting-game sprites with Flux.1-dev + ControlNet OpenPose. Design stayed consistent within one generation but not between generations. "The AI was rather poor at actually replicating the character's poses precisely," limbs shifted between frames, OpenPose failed on smear frames, obscuring clothing and non-human proportions, and each generation took about 10 minutes. — [shark-works blog, Pt. 1](https://shark-works.blogspot.com/2025/05/attempts-at-sprite-generation-in.html)
- DOCUMENTED (opinion, Apr 2025): fighting-game development bottlenecks at animation. Off-model frames are easier to pass off as intentional, and with fewer frames, manual fixes become practical. — [shark-works, consistent character assessment](https://shark-works.blogspot.com/2025/04/a-cursory-assessment-of-consistent.html)
- DOCUMENTED (anecdotal Reddit report, May 2026): a local pipeline went from reference image to Wan 2.2 i2v 14B to BiRefNet background removal (clean alpha) to stitched sheets. Wan 2.2 Animate "didn't stay true to the original base animation." For aligned customization layers the author switched to Wan 2.1 VACE inpainting + SAM3 segmentation. — [Reddit snapshot](https://reddit.sentinel-team.org/posts/1sve5id/snapshots/2026-05-02T01%3A14%3A58.433553Z)
- DOCUMENTED: Wan 2.2 Animate (14B) has two modes, character animation and character replacement, and ships as official ComfyUI templates. — [ComfyUI docs](https://docs.comfy.org/tutorials/video/wan/wan2-2-animate.md); [comfy.org workflow](https://comfy.org/workflows/video_wan2_2_14B_animate-c2c4b45b7df5/)
- DOCUMENTED (vendor guidance, treat with caution): make a reference/model sheet before animating, and never ship raw generated sheets without cleanup and animation tests. — [SpriteCook blog](https://www.spritecook.ai/blog/animating-sprite-sheets-tips-for-consistency); [Makko on consistency being "the hard part"](https://dev.to/makko_ai/ai-game-art-generator-characters-backgrounds-animations-and-why-consistency-is-the-hard-part-4apm)
- DOCUMENTED: itch.io users call AI-generated sprite sheets "glitch-riddled" and malformed. — [itch.io comments](https://mmltech.itch.io/warrior-poses-sheet/comments)
- DOCUMENTED, removing interpolated or duplicate frames: anime is often drawn at about 12 fps with irregular duplicate patterns, so fixed 1-in-N decimation fails. `ffmpeg -vf mpdecimate,setpts=N/FRAME_RATE/TB` drops near-identical frames (defaults hi=768, lo=320, frac=0.33). It outputs variable frame rate and can desync audio. — [VideoHelp forum](https://forum.videohelp.com/threads/414492-How-to-decimate-irregular-duplicate-frames-with-ffmpeg); [FFmpeg-user list](https://ffmpeg.org/pipermail/ffmpeg-user/2021-June/053149.html). MultiPassDedup handles anime with mixed animation cadences. — [MultiPassDedup](https://awesome.ecosyste.ms/projects/github.com%2Froutinelife1%2Fmultipassdedup)
- DOCUMENTED: RIFE (ECCV 2022) does real-time frame interpolation, and Practical-RIFE / rife-ncnn-vulkan are the maintained runtimes. — [hzwer/ECCV2022-RIFE](https://www.github.com/hzwer/ECCV2022-RIFE); [VapourSynth-RIFE-ncnn-Vulkan](https://github.com/styler00dollar/VapourSynth-RIFE-ncnn-Vulkan)

### Inferences (INFERRED)
- For this project, interpolation tools (RIFE, GMFSS) are mostly the wrong direction. AI video already looks too smooth and "3D," which is exactly what ASW rejected with limited animation. The useful direction is the reverse: dedupe, then hand-select keys and hold them.
- AI video models do not reliably sharpen motion. Their temporal smoothing produces mushy contact frames. Generate with prompts or reference motion that hit clear key poses, then select the frame closest to each key, rather than trusting every frame.
- Use a fixed reference stack per character (model sheet + approved palette + a LoRA or reference image) and generate all moves from it. Then run the deterministic cleanup from section 1 (palette lock, cel bands, uniform outline). That deterministic step is what makes outputs from different generations match. The AI cannot guarantee it.
- BiRefNet or SAM-class matting beats chroma keying for hair and motion blur. The client's green screens still need despill. Proposed order: matte → despill → quantize.
- Upscaling (for example Real-ESRGAN anime models; see section 5) should happen before quantization and outline, never after, or the upscaler reintroduces gradients and soft edges.

### Gaps
- No published commercial fighting game (2024–2026) documents a shipped AI-video sprite pipeline with measurable results.
- Real-ESRGAN anime-video model specifics were not verified in this session (search returned only RIFE material).
- No reliable comparison of 2026 video models (Wan 2.x, Kling, Veo, etc.) for character consistency in fighting-game moves was found. Vendor numbers are marketing.

---

## 5. Tools: frame data and hitbox editors, atlas tools, VFX resources, open-source engines

### Takeaway
The most useful open references for professional conventions are Ikemen GO (MIT, MUGEN-compatible: sprite/animation/hitbox data formats, PalFX, state controllers) and its ecosystem. For assets: Aseprite for frame cleanup, Laigter for normals, and a texture packer for atlases. In this session I verified the engine and Laigter. The other tool URLs come from prior knowledge and were not re-fetched, as marked.

### Cited Findings
- DOCUMENTED: Ikemen GO is an open-source (MIT code, some assets CC-BY 3.0) fighting engine in Go. It is a complete rewrite of Ikemen, aims for full compatibility with Mugen 1.0/1.1 characters, stages, storyboards, lifebars and screenpacks, and has builds for Windows, macOS and Linux. — [GitHub ikemen-engine/Ikemen-GO](https://github.com/ikemen-engine/Ikemen-GO); [build wiki](https://github.com/ikemen-engine/Ikemen-GO/wiki/Building,-Installing-and-Distributing)
- DOCUMENTED: Ikemen PalFX parameters (add/mul/sinadd/color/invert/time) apply to sprites to "adjust palette without need to duplicate sprites." — [Ikemen GO wiki](https://github-wiki-see.page/m/ikemen-engine/Ikemen-GO/wiki/Lifebar-features)
- DOCUMENTED: Laigter, a free, open-source normal-map generator for 2D sprites. — [docs](https://laigter.readthedocs.io/)
- DOCUMENTED: Aseprite was used in documented indie 3D-to-2D and pixel pipelines (Kidbash; Kaz Deliverance). — [daily.dev on Kidbash](https://daily.dev/posts/how-kidbash-mixes-retro-style-graphics-with-a-modern-art-pipeline-jhtmcx4g6); [Kaz Deliverance devlog](https://celestialscholar.itch.io/motorcycle-and-misc-city-stuff/devlog/310754/making-of-kaz-deliverance-art-edition)
- DOCUMENTED: ComfyUI official Wan 2.2 Animate workflow; BiRefNet used for sprite alpha (see section 4). — [ComfyUI docs](https://docs.comfy.org/tutorials/video/wan/wan2-2-animate.md)
- Tool URLs from prior knowledge, NOT re-verified this session (flag as such):
  - Fighter Factory (MUGEN/Ikemen character editor with sprite, palette, animation and Clsn1/Clsn2 hitbox editing): https://mugenguild.com (Fighter Factory Studio thread)
  - Aseprite (frame-by-frame sprite editor, CLI export of sheets and JSON): https://www.aseprite.org/
  - TexturePacker (commercial atlas packer, JSON/plain-text formats): https://www.codeandweb.com/texturepacker ; free-tex-packer (open source): https://github.com/odrick/free-tex-packer
  - rembg (background removal CLI/library, can use BiRefNet models): https://github.com/danielgatis/rembg ; BiRefNet: https://github.com/ZhengPeng7/BiRefNet
  - Real-ESRGAN (includes anime-specific models): https://github.com/xinntao/Real-ESRGAN
  - realtimevfx.com community (VFX breakdowns, stylized/anime FX threads): https://realtimevfx.com/

### Inferences (INFERRED)
- **Hitbox/frame-data convention to copy**: the MUGEN/Ikemen AIR format (per-frame duration, offset, Clsn1 attack boxes, Clsn2 hurt boxes, loop start) is the de-facto open convention. Mirroring it in the Java engine's JSON lets the team use Fighter Factory as a visual hitbox editor, or at least study the conventions. Use frame counts in 60 fps ticks and per-frame hold durations. That is the same data that supports limited-animation holds.
- **Atlas**: use one atlas per character per palette-index set (R8 index texture) and keep VFX in a separate atlas with its own ramps. Check that the packer outputs trimmed rects plus original offsets so the foot axis (a recurring problem in the project's task history) is stored as data, not baked into padding.
- **Recommended minimal toolchain for the client (proposal)**: ComfyUI (generation) → BiRefNet/rembg (matte) → Python dedupe + human key-pose picker → despill → Real-ESRGAN (optional) → Python palette lock and cel posterize → Aseprite (hand fix of key frames, line pass) → Laigter (optional normals) → packer → engine JSON in Ikemen-like AIR format.

### Gaps
- I verified no dedicated standalone frame-data/hitbox editor outside the MUGEN ecosystem (for example, generic tools) in this session.
- No VFX pack was verified that matches an anime cel style for commercial use. realtimevfx.com is a community, not an asset source.
- Ikemen GO renderer specifics (shader code for PalFX, outline, blending) were not inspected.
