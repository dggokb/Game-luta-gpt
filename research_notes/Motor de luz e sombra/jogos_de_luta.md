# Character lighting and shading in professional 2D / 2.5D fighting games (stage tie-in, over-brightness, ground shadows, per-stage vs per-character data)

Reading status legend used below:
- **[FULL]** = page fetched and read (via the fetch tool, which returns a model-made extract of the page, not the raw text; quotes marked as verbatim were requested verbatim from the page).
- **[SNIPPET]** = only a search-engine snippet/summary was seen; treat as a lead.
- **[BLOCKED]** = fetch refused from this network (HTTP 403 or tool block).

Blocked from this network during this session: streetfighter.com (SF6 official "ライト開発コラム" lighting columns, 403), unrealengine.com developer interviews (403), animenewsnetwork.com (403), web.archive.org (tool cannot fetch it). The archive.org "stream" text dumps worked and were the best route to the Guilty Gear Xrd primary material.

Key primary documents found (all on archive.org, collection "Five Guilty Gear Design + Graphics Documents", https://archive.org/details/GGXrd-design-docs):
- English translation of 4Gamer's Nishikawa Zenji article "The Secrets of GGXrd's Graphics" — https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt **[FULL]** (original Japanese: https://www.4gamer.net/games/216/G021678/20140703095/ , not fetched)
- English translation of CGWORLD #215 article on GGXrd -REVELATOR- — https://archive.org/stream/GGXrd-design-docs/CGWORLD%23215%20GGXrd%20-REVELATOR-_djvu.txt **[FULL]**
- 4Gamer report of the GDC 2015 Motomura talk (Japanese) — https://www.4gamer.net/games/216/G021678/20150317055/ **[FULL]**

---

## 1. Guilty Gear Xrd (SIGN / REVELATOR) and Strive: exact shading model, light per character, stage colour, ground shadow

### Takeaway
Xrd uses a forward-rendered, two-tone threshold toon shader where each character carries its own private light (hand-positioned per animation / per frame in cutscenes) and a single scene directional light exists only to cast the ground shadow; shade colour = Base texture × "SSS"/Tint texture, the lit side ignores the tint. Stage influence was zero in SIGN ("fixed for all stages") and was introduced in REVELATOR as a **per-stage, per-character hand-set colour** managed by a dedicated staff member, plus point lights that **replace** (not add to) the ambient term specifically to avoid over-bright, unreadable characters.

### Cited Findings

**Light sources (per-character vs global)**
- "only one global light source, a parallel light source equivalent to sunlight" — it casts the dynamic shadows of the characters onto the ground. — [4Gamer/Nishikawa, EN translation, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)
- Each character has its own light that follows it, described as "each character's own lighting staff"; its position and angle are adjusted per animation, and frame by frame in cutscenes. — [same, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)
- GDC 2015 report: 「それぞれのキャラクターが自分自身を照らす光源を持ち歩く」("each character carries around a light source that illuminates only itself"); stated reason includes keeping the look fair/consistent between left- and right-facing sides. — [4Gamer GDC 2015, FULL](https://www.4gamer.net/games/216/G021678/20150317055/)
- 「足元に置かれる影は，この平行光源をもとに生成している」("the shadow placed at the feet is generated from this directional light") — the directional light does not contribute to character shading, so the ground shadow differs between left- and right-facing characters. — [4Gamer GDC 2015, FULL](https://www.4gamer.net/games/216/G021678/20150317055/)
- 「セルフシャドウは使っていない。」("self-shadowing is not used") — reason: big hairstyles would throw odd shadows on neck and shoulders. — [4Gamer GDC 2015, FULL](https://www.4gamer.net/games/216/G021678/20150317055/)
- Static background shadows are baked into vertex colours and textures; forward rendering (not deferred). — [4Gamer/Nishikawa EN, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)

**Threshold shader**
- "The shading that results from the lighting is basically a very common two-toned toon shader mechanism." Example threshold: "a value above 128 is 'light', and a value below 127 is 'shade'." Near-threshold values flicker: "the light and shade may easily be reversed by a slight movement of the character or camera." — [4Gamer/Nishikawa EN, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)
- 「ライティング対象面が光源方向を向いているか，いないかの二値だけで判定する」("judged only as a binary: whether the surface faces the light direction or not"). Artists set a 「陰になりやすさ強度」("shade-likelihood strength") parameter by hand. — [4Gamer GDC 2015, FULL](https://www.4gamer.net/games/216/G021678/20150317055/)
- Vertex normals are hand-edited / transferred from simpler shapes (Softimage Gator) so shade shapes are broad and anime-like; UE3 mesh import was modified to keep edited normals. 「頂点単位の法線ベクトルを調整する制御」. — [4Gamer GDC 2015, FULL](https://www.4gamer.net/games/216/G021678/20150317055/); [4Gamer/Nishikawa EN, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)
- CEDEC 2016 cel-shading session (Xrd REVELATOR as case): 「とにかく陰影制御にこだわりました」("we were obsessive about controlling shading"); 「ほしい位置に陰影を出して画を引き締めるため、頂点カラーやテクスチャを使用している」("vertex colours and textures are used to put shade where we want it and tighten the image"). Example: normals in a triangular area around Sol's eye edited to get a crisp triangular shade. Preview in Softimage must equal in-game: 「同じ結果になることが保証されている必要があります」. — [CGWORLD CEDEC 2016, FULL](https://cgworld.jp/article/201609-cedec-cell.html)

**Shade colour ("SSS"/Tint texture)**
- "SSS textures do not simulate subsurface scattering." In shade: "the shader multiplies the value of the character model Base Texture by the value of the SSS Texture." In light: "the SSS texture value is ignored, so only the light source color is affected." — [4Gamer/Nishikawa EN, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)
- GDC: 「明部の配色テクスチャ（Base Tex）と暗部の配色テクスチャ（Tint Tex）を用意し」("prepare a lit-colour texture (Base Tex) and a dark-colour texture (Tint Tex)"); for skin, deeper shade shifts toward red. — [4Gamer GDC 2015, FULL](https://www.4gamer.net/games/216/G021678/20150317055/)
- Colour designers choose shade colours by "examining the ambient light color of the scene". — [4Gamer/Nishikawa EN, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)

**Vertex colour channels (Motomura)** — [4Gamer/Nishikawa EN, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)
- R: shade threshold offset (shade bias); "1 is the standard", 0 "means it will always be shaded".
- G: how much the outline expands with camera distance.
- B: outline Z-offset (hides lines in hair, under nose).
- A: outline thickness; "0.5 is the standard value, 1 is the maximum thickness, and 0 is no contour line."

**ILM ("lighting control") texture channels** — same source, FULL
- R: specular intensity (bigger for metal/slick materials).
- G: second shade bias; at maximum gives a baked shadow (e.g. under chin).
- B: "highlightability" (how strong/broad highlights are; lower = highlights fade).
- A: Motomura inner-line texture (black/white).

**Post-processing / effects light**
- HDR + bloom: "light bloom is applied to brightness values of 1.0 and above"; "diffusion is applied to lower brightness values"; FXAA; flames and flashes "do not actually illuminate the surrounding area" (in SIGN). — [4Gamer/Nishikawa EN, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)

**REVELATOR changes (stage tie-in, anti-blow-out rule)** — [CGWORLD #215 EN, FULL](https://archive.org/stream/GGXrd-design-docs/CGWORLD%23215%20GGXrd%20-REVELATOR-_djvu.txt), verbatim:
- Before: "the character shading was fixed for all stages, in line with traditional fighting games".
- Now: "each stage is designed with a separate color for each character" and "a full-time staff member is assigned to manage this very carefully."
- How it is authored in UE3: "The sphere in the foreground is the scene color." (a reference sphere placed in the scene; point lights are placed as actors).
- Point lights from attacks and stage elements: "These point lights are not added to the environmental lights, but rather, are replaced based on distance." Adding them "would make them too bright and reduce visibility." Paraphrase of the same page: the closer the point light, the more the ambient is taken over by it; point lights also drive contrast.
- Rim light added in REVELATOR: "Mask for the rim light, stored in the alpha channel of the Base Texture." (hair etc. masked out).
- Translator note: the SSS/colour-adjust behaviour described in the 4Gamer piece was introduced in REVELATOR, not SIGN. — [4Gamer/Nishikawa EN, FULL](https://archive.org/stream/GGXrd-design-docs/4Gamer%20-%20The%20Secrets%20of%20GGXrd%27s%20Graphics_djvu.txt)
- Story mode editor can set separate colours for lit and shaded areas per scene. — [CGWORLD #215 EN, FULL](https://archive.org/stream/GGXrd-design-docs/CGWORLD%23215%20GGXrd%20-REVELATOR-_djvu.txt)

**Strive (UE4)**
- Director Akira Katano: the most notable new tech is "the ambient background light and how it reflects on the characters." — [Inven Global interview, FULL](https://www.invenglobal.com/articles/14229/exclusive-a-glance-into-guilty-gear-strives-art-with-director-akira-katano)
- Team built a system that "allows us to forcibly control the shading and light of individual parts" even against physical correctness. — [Unreal Engine interview, SNIPPET; BLOCKED](https://www.unrealengine.com/en-US/developer-interviews/how-guilty-gear--strive--hits-an-ultra-combo-with-groundbreaking-visuals-and-gameplay)
- Producer Ken Miyauchi: stage-specific lighting such as sunlight through leaves on a forest stage; snow stages make character colours look more saturated; credits environment artist Genki Mamada for making characters stand out on the stages. — [ANN interview 2024, SNIPPET; BLOCKED](https://animenewsnetwork.com/interview/2024-09-30/guilty-gear-artistic-identity-and-upcoming-anime-with-ken-miyauchi-shoichi-kitazono-and-genki-mamada/.214138)
- Shacknews: "the stage background have actual effect on the lighting and shadows of the characters". — [Shacknews, SNIPPET](https://shacknews.com/article/128128/shacknews-best-art-style-of-2021-guilty-gear-strive?amphtml=1)

**Fan reconstructions (secondary)**
- Maya CGFX port: "check if the surface normal is facing the light. If it is, use the light color (main texture) otherwise use the shadow color (main texture * tint texture)". — [Mike Ton, SNIPPET](https://miketon.posthaven.com/maya-cgfx-shader-inspired-by-guilty-gear-xrd-gdc-presentation)

### Inferences
- For a sprite game, the Xrd recipe maps directly: the per-character light is just a 2D direction per stage (or per animation) fed to the normal-map shader; the shade colour should be a *multiplied tint* (palette/texture), not a darkened copy, and the lit side should stay ≈ base colour × light colour (no additive term).
- The REVELATOR "replace, don't add" rule is the most directly relevant fix for "blown out": any extra light (rim, effect, stage) should lerp the ambient/tint toward the light colour by a weight, keeping total ≤ the base colour, instead of summing on top.
- Arc's per-stage data = one hand-tuned colour per character per stage + placed point lights; per-character data = Base, Tint/SSS, ILM, vertex-colour bias, light direction keyed to animation.
- Bloom only above 1.0 in HDR implies the character's lit colour normally sits below 1.0; only effects exceed it.

### Gaps
- The CEDEC 2014 Arc talk page and the original GDC 2015 slide PDF were not retrieved; the GDC Vault video was not checked.
- No numeric value found for how strongly REVELATOR's stage colour multiplies the character (range of the per-stage colour), nor the rim light formula.
- Strive's exact rim/ambient implementation (Japanese CGWORLD/4Gamer Strive articles) not found in this session.

---

## 2. Dragon Ball FighterZ and Granblue Fantasy Versus

### Takeaway
No primary technical source on DBFZ/GBVS stage-to-character lighting was found; the only result says shading in DBFZ is fixed per character and not changed by environment.

### Cited Findings
- Secondary summary (video transcript of "The Animation of Guilty Gear Xrd / Dragon Ball FighterZ"): per-character shading was fixed, each character's shading hand-drawn in, with no ability to change it based on environment. — [Amara transcript, SNIPPET](https://amara.org/subtitles/bDmpkclvfB5I/en/1/download/The%20Animation%20of%20Guilty%20Gear%20Xrd%20%20Dragon%20Ball%20FighterZ.en.dfxp)

### Inferences
- DBFZ shipped on UE4 between REVELATOR and Strive; whether it reused REVELATOR's per-stage colour is unverified.

### Gaps
- No source found on DBFZ/GBVS per-stage colour correction or dramatic-finish lighting (search ran out of budget). Next: 4Gamer/CGWORLD "ドラゴンボール ファイターズ" CEDEC 2018 articles; GBVS "グラブル ヴァーサス CGWORLD".

---

## 3. King of Fighters XV / XIV / XIII

### Takeaway
SNK modified UE4's lighting code so fighters cast shadows on the floor but not on each other, combining realistic lighting/shadows with a non-realistic art style; no per-stage lighting data found.

### Cited Findings
- UE interview (SNK): the team wanted stylized shadows such as fighters' shadows showing on the floor but not on one another, achieved by modifying the engine's lighting code; programmer James Bulmer: UE4 allowed realistic lighting and shadows while keeping a non-realistic art style. — [Unreal Engine interview, SNIPPET; page BLOCKED (403)](https://www.unrealengine.com/en-US/developer-interviews/why-snk-shifted-to-unreal-engine-for-its-latest-fighting-games)
- SNK tested Unreal during KOF XIV development (led to Samurai Shodown 2019 on UE). — [UE interview, Korean version, SNIPPET](https://www.unrealengine.com/developer-interviews/why-snk-shifted-to-unreal-engine-for-its-latest-fighting-games?lang=ko)
- Famitsu KOF XV developer interview series exists (interview01/02/05/08), not read. — [Famitsu, SNIPPET](https://www.famitsu.com/matome/kof-xv/interview02.html)

### Inferences
- "Shadow on floor, not on each other" matches Xrd's "no self-shadow, ground shadow from a separate light" choice: in 2D/2.5D fighters the ground shadow is a separate pass, characters are not shadowed by each other.

### Gaps
- KOF XIII sprite workflow (3D → hand-pixelled, palettes) and whether stages tint palettes: not researched due to budget.

---

## 4. Street Fighter 6 (RE Engine) and earlier

### Takeaway
Capcom publishes an official Japanese "ライト開発コラム" (lighting development column) series for SF6 covering character lighting fundamentals (key/fill/rim), key visuals and Super Art lighting per character; it is blocked from this network, so only titles/snippets are known.

### Cited Findings
- Column "人物ライティングの基本" (basics of character lighting) covers three-point lighting: key, fill and rim/back light. — [SF6 column lighting02, SNIPPET; BLOCKED](https://www.streetfighter.com/6/ja-jp/column/detail/lighting02)
- Other columns: Mai key-visual lighting (brightness and sheen chosen for her) — [lighting07, SNIPPET](https://www.streetfighter.com/6/ja-jp/column/detail/lighting07); Vega (M. Bison) SA lighting — [lighting05](https://www.streetfighter.com/6/ja-jp/column/detail/lighting05); Sagat lighting direction — [lighting09](https://www.streetfighter.com/6/ja-jp/column/detail/lighting09); Alex SA3/CA lighting — [lighting11](https://www.streetfighter.com/6/ja-jp/column/detail/lighting11); Yasmin new-character lighting — [lighting13](https://www.streetfighter.com/6/ja-jp/column/detail/lighting13). All SNIPPET/BLOCKED.

### Inferences
- The existence of per-character SA lighting columns suggests SF6 also hand-authors character lighting for supers (cinematic), consistent with Arc's approach.

### Gaps
- No CAPCOM Open Conference RE:2023 or CEDEC SF6 lighting talk found by search. The SF6 columns (lighting01–13+) are the best primary source; retry from a network that can reach streetfighter.com (or Google cache / translated mirrors).
- No data on SF6 exposure/tone mapping or per-stage character light rigs.

---

## 5. Skullgirls (Z-Engine)

### Takeaway
Sprites are built from three layers (line, shading, colour), coloured via a palette shader, and lit in real time by dynamic rim lights and per-pixel lighting from stage light sources and hit sparks; normal-map-like data drives the rim light. No primary Lab Zero / Mike Z technical writeup was found.

### Cited Findings
- "Characters are assembled from three layers (line, shading and color) and colored via a custom shader. They are illuminated by dynamic rim lights and per-pixel lighting generated by light sources in the environment and hit sparks." — [Wikipedia, SNIPPET](https://en.wikipedia.org/wiki/Skullgirls)
- Poster "Ravidrath" (Lab Zero producer Peter Bartholow's known handle; identity not verified here): characters are "completely 2D, we just apply some effects that are usually reserved for 3D on them, like the normal-map like effect for the rim lighting, the compositing shader, and the per-pixel lighting." — [NeoGAF 2011 thread, SNIPPET](https://www.neogaf.com/threads/skullgirls-2d-fighting-game-announced-for-xbla-psn.423175/page-33)
- "each frame had 3 separate layers outline, color and shading (since their Z Engine allows for real time sprite shading and lighting)". — [SRK archive, SNIPPET](https://archive.supercombo.gg/t/2d-sprite-based-pixel-art-or-3d-for-fighting-games-art-talk/177072)
- TV Tropes claims a shading layer plus depth map per sprite. — [TV Tropes, SNIPPET, low reliability](https://tvtropes.org/pmwiki/pmwiki.php/VideoGame/Skullgirls)
- Official "A Rather Refined Palette" post (skullgirls.com/?p=291) now redirects to a generic page with no technical content. — [skullgirls.com, FULL, no content](https://skullgirls.com/?p=291)

### Inferences
- The closest analogue to this project: hand-painted shading lives in the sprite (shading layer), and real-time light is limited to rim + local point lights on top; i.e. the stage does not re-shade the whole sprite.

### Gaps
- Number of lights per stage, light colours per stage, floor shadow method: not found.

---

## 6. Sprite-era games (BlazBlue, P4A, UNI, Melty Blood, SF3, KOF XIII): stage tinting and ground shadows

### Takeaway
Not researched in this session beyond the general pattern above; no sources gathered.

### Gaps
- Whether BlazBlue/UNI/MBTL tint sprites per stage, and how their ground shadows are drawn (flattened silhouette vs blob), remains unanswered. Suggested next searches: "BlazBlue 影 スプライト 床", Mizuumi wiki stage pages, CPS3 palette fade documentation.

---

## 7. Readability and ground shadows (general)

### Takeaway
The documented rules converge: (1) character light is separate from stage light; (2) stage influence is a hand-tuned colour per character per stage, never free physical lighting; (3) extra lights replace part of the ambient instead of adding, to avoid over-bright characters; (4) ground shadow is a separate pass from a separate (scene) light, with no self-shadow and no character-on-character shadow.

### Cited Findings
- Xrd: ground shadow from a scene directional light that does not light the character; no self-shadow. — [4Gamer GDC 2015, FULL](https://www.4gamer.net/games/216/G021678/20150317055/)
- KOF XV: shadows on floor, not on each other. — [UE interview, SNIPPET](https://www.unrealengine.com/en-US/developer-interviews/why-snk-shifted-to-unreal-engine-for-its-latest-fighting-games)
- REVELATOR: adding lights "would make them too bright and reduce visibility" → replace by distance. — [CGWORLD #215 EN, FULL](https://archive.org/stream/GGXrd-design-docs/CGWORLD%23215%20GGXrd%20-REVELATOR-_djvu.txt)
- Xrd: background lighting deliberately limited to keep the 2D illusion. — [Giant Bomb, SNIPPET](https://www.giantbomb.com/games/3030-42679/)

### Inferences
- For this project's JSON: per-stage `tint` should be a per-character (or at least clamped) multiplier near white, rim should be masked and lerped (not added), and the reflection/shadow are independent of the fighter's own light.

### Gaps
- No numeric value-separation guideline (e.g. luminance difference character vs background) found from any studio.
