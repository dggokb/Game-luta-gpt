# Iluminação de personagem vs. cenário em SF6/SFV, KOF XV, DBFZ/GBVS e Streets of Rage 4

Status legend per source: **[lido]** = page fetched and its text processed (through the fetch tool's extractor, so quotes are as the extractor returned them); **[snippet]** = only search-engine snippet/summary seen; **[bloqueado]** = fetch returned 403 / bot wall.

Network notes: streetfighter.com (official SF6 columns) = 403 on every fetch; a curl attempt was refused by the session's permission system as bot-check circumvention, so I stopped trying that route. unrealengine.com interview not fetched (earlier round: unreachable); its GameFAQs repost = 403. segaretro.org = Anubis bot wall. itec.daikin.co.jp page = empty body.

## (a) Rig de luz de personagem por estágio

### Takeaway
The only direct primary evidence of a per-stage character-light rig is KOF XV: SNK stores **character light intensity and angle per stage** (plus special-move light animation) in a UE4 Data Asset attached to each stage. For SF6, Capcom's official "ライト開発コラム" confirm a three-point (key/fill/rim) approach with high-saturation coloured rims, but every column I could see (via snippets) is about key visuals, Super Art/Critical Art cutscenes and win poses. None of them describes the in-match per-stage character rig.

### Cited Findings
- **KOF XV, UE Fest West 2022 session** 「Unreal Engineを馴染ませる 〜THE KING OF FIGHTERS XV における開発事例〜」, speaker 呂 朗標 (SNK), 2022-11-20, CGWORLD report published 2022-12-14 **[lido]**:
  - 「各ステージのパラメータ情報、キャラクターのライトの強さや角度、特別な演出中のライトの動き、演出中のカメラの動きなどはData Assetに書き込んでステージに持たせている。」 ("Each stage's parameters, **the character light's intensity and angle**, light motion during special presentations and camera motion during presentations are written into a Data Asset that the stage carries.") — [CGWORLD](https://cgworld.jp/article/202212-uefestw-kof.html)
  - Stage lights other than Movable are baked: 「MobilityがMovable以外のライトはライトビルドが必要になる。」 Light builds run automatically on Jenkins: 「製品版用の確認はJenkinsで定期的に自動ライトビルドしてコミットしている。」 — [CGWORLD](https://cgworld.jp/article/202212-uefestw-kof.html)
  - The article does not mention shadows, rim light, tonemapping or outlines (extractor's explicit check). — [CGWORLD](https://cgworld.jp/article/202212-uefestw-kof.html)
- **SF6 official lighting columns** (streetfighter.com, all **[bloqueado]**, content known only from **[snippet]**s):
  - Series list: lighting01 「ライティングという仕事part1」, 02 「人物ライティングの基本」, 03 「カメラ機能に『フォトライト』追加！」, 04 豪鬼キービジュアル, 05 ベガのSA演出, 06 リザルト演出, 07 舞のキービジュアル, 08 ライトボツ案集-エレナ編, 09 サガット演出, 10 「光の当たる向きを変えるとどう変わる？」, 11 アレックスSA3/CA, 12 イングリッドCA, 13 ヤスミン. — [lighting01](https://www.streetfighter.com/6/ja-jp/column/detail/lighting01), [lighting02](https://www.streetfighter.com/6/ja-jp/column/detail/lighting02), [lighting13](https://www.streetfighter.com/6/ja-jp/column/detail/lighting13)
  - Definitions (lighting02): 「キーライトは、キャラクターを照らすメインの光源の役割を持っています。」 ("The key light is the main source lighting the character.") and 「キーライト＝主役のライト、一番見せたい部分を照らす」 ("key light = the star's light; it lights the part you most want to show"). Fill lights soften dark areas and should be weaker than the key, because a bright fill makes the light direction hard to read. — [lighting02](https://www.streetfighter.com/6/ja-jp/column/detail/lighting02) [snippet]
  - **Numeric:** for the Ryu key visual, the column reportedly counts **2 key + 3 fill + 6 rim = 11 lights**. It says there is no fixed limit, but too many key lights make the main direction unreadable. — [lighting02](https://www.streetfighter.com/6/ja-jp/column/detail/lighting02) [snippet, English paraphrase by the search tool; not verified verbatim]
  - Lighting01 defines lighting as setting light on in-game models. It affects almost the whole screen (stages and cutscenes) and shows before/after shots of the Chun-Li and Dhalsim stages: 「ライティング前後で変化している部分がライティングアーティストが行っている仕事です。」 ("What changes between before and after lighting is the lighting artist's work.") — [lighting01](https://www.streetfighter.com/6/ja-jp/column/detail/lighting01) [snippet]
  - Performance constraint: lights in stages and cutscenes are placed with processing cost in mind, and shadow-casting lights are expensive, so their number (or the total number of lights) is cut. — [lighting02](https://www.streetfighter.com/6/ja-jp/column/detail/lighting02) [snippet, paraphrase]
- **SF6 Famitsu staff interview** (2022-06) **[lido]**: the only lighting content is praise of RE ENGINE's dark-area rendering on night Metro City: 「さすがRE ENGINEということで暗い場所の表現がズバ抜けています。」 ("as expected of RE ENGINE, the rendering of dark places is outstanding"). The engine was chosen for realistic muscle and sweat: 「『筋肉や汗をよりリアルでダイナミックに表現したい』というテーマがあり」. Nothing about the character rig. — [Famitsu](https://www.famitsu.com/news/202206/17264369.html)
- **SF6 CEDEC 2023/2024 sessions found are not about lighting.** CEDEC 2023 covered World Tour's 3D-to-2D switch. CEDEC 2024 covered cloth (RE ENGINE Chain + Havok Cloth), "演出風" wind and stage destruction. — [4Gamer CEDEC 2024](https://www.4gamer.net/games/635/G063504/20240830070/), [CGWORLD CEDEC 2024](https://cgworld.jp/article/202410-cedec03-sf6.html) [snippet]

### Inferences
- KOF XV's design implies a **dedicated character light, separate from the stage lights, re-tuned per stage** (only intensity and angle are named), with stage lighting baked. This matches the "character light per stage" pattern already known from GG Xrd.
- SF6's columns show Capcom thinks of character light as a three-point rig with many small coloured rims in cinematic shots. Whether in-match fighters get a similar dedicated rig cannot be confirmed from what was reachable.

### Gaps
- SF6 in-match rig (dedicated lights following the fighters? light channels? probes/IBL?) and exposure/tonemapping: not found. The official columns are 403 and no press re-publication of their full text turned up. No CAPCOM Open Conference or CEDEC lighting session on SF6 was found.
- SFV/SFIV per-stage character lighting files: only stage "nighttime" mods were found ([EventHubs](https://www.eventhubs.com/news/2016/apr/11/street-fighter-5-nighttime-mod-gives-its-stages-whole-new-ambiance/), [Kotaku](https://kotaku.com/street-fighter-v-mod-hits-the-light-switch-on-all-stage-1771165646), [snippet]). No documentation of a character-light asset turned up.

## (b) Como evitam personagem estourado ou perdido contra o fundo

### Takeaway
KOF XV keeps characters' colours faithful by **excluding characters from post-processing**, via a custom-depth-stencil engine mod that also feeds FXAA. SF6's columns use rim light explicitly so characters "don't melt into the background", but the context given is cutscenes. They also pull back the background with depth of field and fog and keep the hero's light level readable.

### Cited Findings
- KOF XV: 「3D描画ということはポストプロセスも影響を受ける。」 / 「アセット元の色合いを保持するため、ポストプロセスを適用しないようにすることだ。」 ("Being 3D, post-process affects it too… the approach is **not to apply post-process, to keep the asset's original colours**.") 「Custom Depth Stencilを流用してエンジン改造によってFXAAのシェーダに反映した。」 ("They repurposed Custom Depth Stencil and modified the engine so the FXAA shader respects it.") — [CGWORLD](https://cgworld.jp/article/202212-uefestw-kof.html) [lido]
- SF6 rim light purpose: 「主にキャラクターの存在感や立体感を強めたり、カットシーンなどの場合は背景にとけこまないようメリハリを付けたりする目的で使用します。」 ("Used mainly to strengthen the character's presence and volume, and in cutscenes to add contrast so they **don't melt into the background**.") Rims are more theatrical than key/fill and can look unnatural in realistic styles, but SF6's flashy style calls for strong coloured rims. — [lighting02](https://www.streetfighter.com/6/ja-jp/column/detail/lighting02) [snippet]
- SF6 rim colour: Yasmin's rims cover a small area but use highly saturated colours, basically light blue and/or orange. Multiple fill lights were used to avoid hard-edged shadows in a bright, saturated look. — [lighting13](https://www.streetfighter.com/6/ja-jp/column/detail/lighting13) [snippet, paraphrase]
- SF6 readability over richness: an Elena stage-style lighting idea was rejected because 「画面の情報量が多くなりすぎてしまい、エレナの動きが分かりづらくなってしまったのでボツにしました。」 ("the screen had too much information and Elena's movement became hard to read, so it was dropped"). — [lighting08](https://www.streetfighter.com/6/ja-jp/column/detail/lighting08) [snippet]
- SF6 background suppression: in M. Bison's SA, focus and fog reduce background information, and the whole frame is built with the effects section: 「ライト以外の全体の絵作りをエフェクトセクションと相談しながら調整しています。」 — [lighting05](https://www.streetfighter.com/6/ja-jp/column/detail/lighting05) [snippet]. In Ingrid's CA, a blue fog matched to the sky colour gives depth, and the light level is adjusted so the protagonist is not buried. — [lighting12](https://www.streetfighter.com/6/ja-jp/column/detail/lighting12) [snippet, paraphrase]
- SF6 light direction coherence: top-down light reads as natural. Inconsistent light angles feel wrong even with a background present. The main light direction can imply setting and time of day without any background. — [lighting10](https://www.streetfighter.com/6/ja-jp/column/detail/lighting10) [snippet, paraphrase]
- SF6 shadow tint: Akuma's key visual moved the backlight from blue to red: 「影色に青をより感じさせ、影中の情報量を増やし、今回の新ステージ、『炎魔洞門』の雰囲気も出していきました。」 ("made the shadow colour read more blue, increased the information in the shadows, and brought in the mood of the new stage, 炎魔洞門"). Backlighting was chosen to put the face in shadow: 「光を背負う、逆光にすることによって、顔に影を落として眼光鋭く、重たい感じに…」 — [lighting04](https://www.streetfighter.com/6/ja-jp/column/detail/lighting04) [snippet]
- SF6 win-pose: the base brightness is lowered so the fire can serve as the light source: 「炎は光源としての役割も持たせられるので、それを利用するために、ベースの明るさを他のリザルト演出よりも暗めにし」. Shadows were kept soft to preserve skin texture. — [lighting06](https://www.streetfighter.com/6/ja-jp/column/detail/lighting06) [snippet]
- Related RE ENGINE background, not SF6-specific: CGWORLD's RE7 article reportedly says indirect light uses a probe network rather than lightmaps, removing the lighting mismatch between characters and background. — [CGWORLD RE7](https://cgworld.jp/feature/201702-cgw222T2-bio.html) [snippet, summarised by the search tool; not verified]

### Inferences
- Two independent anti-"lost/over-bright" levers appear: (1) KOF puts characters on a separate path where post-process (bloom, grading) does not touch them, so their palette is exact; (2) SF6 uses saturated complementary rims (cyan/orange, blue against red) and dims the background (fog, DOF, lower base brightness) rather than brightening the character.

### Gaps
- No numeric rim widths, intensities or character-vs-background luminance offsets for any title.
- Whether SF6 excludes characters from tonemapping/grading like KOF: unknown.

## (c) Influência de cor do cenário no personagem

### Takeaway
KOF XV per-stage character-light angle and intensity is confirmed, but a per-stage character colour is not stated. SF6 columns show stage mood pushed into character shadow colour, though only in key visuals. For DBFZ/GBVS nothing primary was reachable. For SoR4, reviewers describe environment lights (neon, lamps) colouring characters, but no developer explains the method.

### Cited Findings
- KOF XV: per-stage character light **strength and angle** in the stage's Data Asset; colour is not mentioned in the report. — [CGWORLD](https://cgworld.jp/article/202212-uefestw-kof.html) [lido]
- SF6: Akuma's shadow colour pushed blue to evoke the new stage (see (b)). — [lighting04](https://www.streetfighter.com/6/ja-jp/column/detail/lighting04) [snippet]. Alex's SA3/CA: strong top-down "ring" light, blue chosen to oppose Zangief's red: 「ザンギエフの赤に対抗する形で青にしています。」 — [lighting11](https://www.streetfighter.com/6/ja-jp/column/detail/lighting11) [snippet]
- DBFZ: an Edge 2017 preview reports that a studio head's animators studied it frame by frame and concluded Arc either lit every frame by hand or "worked out a way to make it look like it has". This is an impression, not documentation. — [Edge via PressReader](https://www.pressreader.com/australia/edge/20170720/282630327693400) [snippet]
- GBVS Rising: CG Creator Week 2025 (Daikin) session by Arc System Works titled 「グランブルーファンタジー ヴァーサス -ライジング- キャラが映える影の作り方！法線編集で変わる見え方の工夫」 ("How to make shadows that make characters shine: normal editing"). — [Daikin event page](https://www.itec.daikin.co.jp/DC/event/202509-cgcweek/D1_arcsystemworks/) [snippet; the page fetched empty]
- SoR4: 「allow for characters to be lit by light sources in the environment」 and 「Even smoke plumes coming out of manhole covers are lit by neon signs and lamp posts in the background.」 — [DarkZero review](https://darkzero.co.uk/game-reviews/streets-of-rage-4-pc/) [lido]. Hands-on: neon lights cast glow on the surroundings and shadows fall on the hand-drawn sprites. — [Nintendo Life hands-on](https://www.nintendolife.com/news/2018/09/hands_on_streets_of_rage_4_is_a_grand_upper_revival) [snippet]. Another reviewer reportedly says real-time lighting was added after concerns that the art looked "too clean", to make everything mesh. — [Nintendo Wire](https://nintendowire.com/news/2020/04/30/review-streets-of-rage-4/) [snippet; review, not a developer]
- SoR4 engine: Lagarigue (lead programmer): "We had to make our custom engine to really have the frame level control and custom tools we needed" … "an engine and toolchain specifically built for side-scrolling Beat'em Ups." … "Also, we had to make a custom graphics integration pipeline, to match the workflow of our very talented artists". — [Nintendo Life 2018](https://www.nintendolife.com/news/2018/12/feature_behind_the_scenes_on_streets_of_rage_4_the_sequel_we_thought_wed_never_see) [lido]. The engine is a modified version of Guard Crush's Streets of Fury engine (uncited sentence in [Wikipedia](https://en.wikipedia.org/wiki/Streets_of_Rage_4) [lido]); PCGamingWiki lists "Guard Crush Engine" plus FNA ([PCGamingWiki](https://www.pcgamingwiki.com/wiki/Streets_of_Rage_4) [snippet]).
- Fiquet on art direction: neon-lit streets and bricks are the iconic elements; quote: "there's different graphic tricks but mainly we need to stay close to the neon-filled streets". — [Prima Games](https://primagames.com/gaming/streets-of-rage-4-interview-lizardcube-dotemu) [snippet], [Anime Herald](https://www.animeherald.com/interview/the-brawl-is-back-interview-with-streets-of-rage-4-director-ben-fiquet/) [snippet]

### Inferences
- The GBVS-R talk title suggests Arc's character readability lever is authored shading (normal editing controlling where the toon shadow falls) rather than stage-driven light. This fits the already-known Xrd model, but the talk content was not readable.

### Gaps
- **SoR4 key question unanswered:** no source says whether character lighting uses generated normal maps, silhouette-based rims, or tint zones. The PlayStation Blog pieces ([character redesign](https://blog.playstation.com/?p=223656) [snippet]; [three studios](https://blog.playstation.com/2020/04/30/streets-of-rage-4-how-three-studios-revived-a-legendary-series/) [lido]) and the Nintendo Life making-of ([2020](https://www.nintendolife.com/news/2020/12/feature_the_making_of_streets_of_rage_4_by_the_people_who_made_it_happen) [lido]) contain nothing on lighting. The Edge "Making of" ([Magzter](https://www.magzter.com/stories/Puzzle-Gaming/Edge/THE-MAKING-OF-STREETS-OF-RAGE-4)) is paywalled. No GDC, Game Camp France or ADDON talk was found. TMNT: Shredder's Revenge and Wonder Boy engine notes were not searched in depth (budget).
- DBFZ / GBVS per-stage colour correction and ground shadows: no CEDEC/GDC/CGWORLD article found. The CEDEC 2018 page that surfaced was an unrelated Capcom session ([CEDEC 2018](https://cedec.cesa.or.jp/2018/session/detail/s5abe04199bad5.html) [lido]).

## (d) Sombras no chão e reflexos

### Takeaway
KOF XV deliberately breaks physical shadowing: **characters cast shadows on the floor but not on each other**, achieved by modifying UE4's lighting code. SoR4 has real-time reflections of characters on shiny floors and puddles (reviewer-documented, implementation unknown).

### Cited Findings
- KOF XV: programmer James Bulmer reportedly says UE4 gave realistic lighting and shadows while keeping a stylised art style. A "non-realistic" requirement was that characters cast shadows on the ground but not on each other, made possible by modifying the engine's lighting code. — [Unreal Engine interview](https://www.unrealengine.com/en-US/developer-interviews/why-snk-shifted-to-unreal-engine-for-its-latest-fighting-games) [snippet only; original not fetched, GameFAQs repost [bloqueado](https://gamefaqs.gamespot.com/boards/253394-the-king-of-fighters-xv/79912533)]. Exact English wording not verified.
- KOF XV: the same interview reportedly credits real-time material editing for faster tuning of characters and backgrounds. — [Unreal Engine interview](https://www.unrealengine.com/en-US/developer-interviews/why-snk-shifted-to-unreal-engine-for-its-latest-fighting-games) [snippet]
- SoR4: modern effects add "realtime" reflections across shiny floors and puddles. — [DarkZero](https://darkzero.co.uk/game-reviews/streets-of-rage-4-pc/) [lido]. Another review: character sprites reflect in puddles and light catches their skin. — [LadiesGamers](https://ladiesgamers.com/streets-of-rage-4-review-on-pc/) [snippet]
- SF6: the columns note that shadow-casting lights are costly and get reduced in stages and cutscenes (see (a)). — [lighting02](https://www.streetfighter.com/6/ja-jp/column/detail/lighting02) [snippet]

### Inferences
- "Floor yes, each other no" means the ground shadow is a readability device (it anchors the fighter to the floor), while character-on-character shadows were judged to hurt the drawn look.

### Gaps
- SF6, DBFZ, GBVS ground-shadow method (projected blob, shadow map, dedicated light): not found.
- SoR4 ground shadow type and reflection technique (flipped sprite vs. screen-space): not found.
- No numeric shadow opacity or offset values found for any title.
