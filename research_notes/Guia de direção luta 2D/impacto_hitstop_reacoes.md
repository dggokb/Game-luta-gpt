# Hit feel in 2D fighters: hitstop, hitstun/blockstun, shake, camera, hit reactions

Every item below is tagged **[DOCUMENTED]** (a source states it) or **[INFERRED]** (my reading or extrapolation from sourced data). All frame values are at 60 fps.

Method note: SuperCombo (wiki.supercombo.gg and its srk.shib.live mirror) blocked automated access during this session. SF6 numbers therefore come from the FAT (Frame Assistant Tool) open data file, which is an aggregation of SuperCombo/official data: https://raw.githubusercontent.com/D4RKONION/FAT/master/src/js/constants/framedata/SF6FrameData.json (fetched 2026-11-09; current-patch SF6 data, 30+ characters). DBFZ and GGST numbers come from Dustloop pages fetched the same day. FAT's SFV, USF4 and 3S files have **no hitstop column**, so SF2/SF3/SF4/SF5 hitstop could not be pulled from the same dataset.

## 1. Hitstop (hit freeze) per strength, per game; block vs hit; counter hit; KO; who freezes

### Takeaway
Modern Capcom/Arc System Works games use a short, strictly tiered hitstop. SF6 normals are 9/11/13 frames (L/M/H). DBFZ is 8/11/16 (L/M/H = attack levels 1/2/4). GGST is 11 to 15 by attack level. Counter hits either add a few frames (old GG, SF6 Punish Counter) or replace hitstop with a long "cinematic" freeze plus slowdown (GGST Mid/Large: 21F/31F). Both fighters freeze for the same time. A projectile freezes only itself and the victim, not the shooter.

### Cited Findings
**Summary table (normals, on hit)**

| Game (version) | Light | Medium | Heavy | Notes | Status |
|---|---|---|---|---|---|
| SF6 (FAT data, current patch Nov 2026), Ryu | 5LP/2LP/5LK/2LK = 9 | 5MP/2MP/5MK = 11 (2MK = 9) | 5HP/2HP/5HK/2HK = 13 | — | DOCUMENTED [FAT SF6 JSON](https://raw.githubusercontent.com/D4RKONION/FAT/master/src/js/constants/framedata/SF6FrameData.json) |
| SF6, whole cast (medians, n≈30 per button) | 9 (range 9–11) | 11 (range 9–12; 2MK median 9) | 13 (range 10–14) | Hitstop almost perfectly standardized by strength | DOCUMENTED data, aggregation mine, [FAT SF6 JSON](https://raw.githubusercontent.com/D4RKONION/FAT/master/src/js/constants/framedata/SF6FrameData.json) |
| SF6 Terry | — | 2MP = 11 | 2HP = 13 | Matches the cast pattern | DOCUMENTED (search snippet of [SuperCombo mirror Terry/Data](https://srk.shib.live/w/Street_Fighter_6/Terry/Data)) |
| SFV | 8 | 12 (Ken: 10) | 15 (Ken: 12) | Old (2016) community measurement; Ken was the documented outlier | DOCUMENTED-secondary [Shoryuken 2016 "Hitstop in SFV: Ken's not-so-little secret"](http://shoryuken.com/2016/06/07/hitstop-in-street-fighter-v-kens-not-so-little-secret/) (page returned 503 during this session; values from search snippet, unverified) |
| DBFZ (Dustloop, Lvl 0–4) | Lvl1 = 8 (5L, 2L, j.L) | Lvl2 = 11 (5M, 2M, j.M) | Lvl4 = 16 (5H, 2H); j.H Lvl3/4 = 14/16 | Lvl0 = 6. Same table: hitstun 14/16/18/20/22; ground blockstun 11/11/15/15/15 | DOCUMENTED [Dustloop DBFZ/Mechanics](https://www.dustloop.com/w/DBFZ/Mechanics); levels per move from [Dustloop DBFZ SS Goku Frame Data](https://www.dustloop.com/w/DBFZ/Goku_(Super_Saiyan)/Frame_Data) |
| GGST (Lv0–Lv4) | 11 / 12 | 13 | 14 / 15 | Standing hitstun 12/14/16/19/21, crouching 13/15/17/20/22, ground block 9/11/13/16/18 | DOCUMENTED [Dustloop GGST/Mechanics](https://www.dustloop.com/w/GGST/Mechanics) |
| GG Accent Core / XX (Lv1–Lv5) | 11 / 12 | 13 | 14 / 15 | Hitstop = blockstop. XX standing hitstun 10/12/14/17/19 | DOCUMENTED (search summary of [Dustloop GGACR Attack Attributes](https://www.dustloop.com/w/GGACR/Attack_Attributes)) |
| SF2, SF3, SF4, MvC3, KOF/Fatal Fury CotW | — | — | — | Not found | GAP |

**Block vs hit**
- Guilty Gear AC/XX: hitstop and blockstop are the same value per level (11–15). Instant Block shortens blockstun by 2–4F; Faultless Defense adds +2 — [Dustloop GGACR Attack Attributes (search summary)](https://www.dustloop.com/w/GGACR/Attack_Attributes) **[DOCUMENTED]**
- SFV: one source quotes the 8/12/15 values "on block", which suggests the same hitstop on hit and on block — [Shoryuken 2016](http://shoryuken.com/2016/06/07/hitstop-in-street-fighter-v-kens-not-so-little-secret/) **[DOCUMENTED-secondary]**
- DBFZ: "For independent objects, like projectiles and Z Assist attacks, the amount of hitstop is listed [as] X+Y where X is blockstun and Y is hitstop" — [Dustloop DBFZ/Mechanics](https://www.dustloop.com/w/DBFZ/Mechanics) **[DOCUMENTED]**. Example: Goku 5S blockstun is 5+15. Air blocking adds 2F of blockstun; landing while in blockstun adds 4F.
- Infil's glossary: "There's a similar mechanic for blocked moves called... blockstop" — [Infil Glossary: Hitstop](https://glossary.infil.net/?t=Hitstop) **[DOCUMENTED]**

**Counter hit / Punish Counter**
- GGST counter-hit types: Very Small keeps base hitstop with no slowdown. Small keeps base hitstop with 11F slowdown (+6 advantage). Mid has **21F hitstop** and 25F slowdown (+13). Large has **31F hitstop**, 35F slowdown (+18) and **camera zoom-in**. Mid/Large "create a unique, cinematic hitstop for easy confirms, overriding an attack's normal hitstop". "The larger the counter hit type, the more dramatic the visuals and audio, with louder audio cues, longer hitstop, longer slowdown; and in the case of Large Counter Hits, camera zoom-in." Slowdown "doubl[es] each frame they experience" — [Dustloop GGST Attack Attributes](https://www.dustloop.com/wiki/images/cache/f/fc/GGST%252FAttack_Attributes.html); [Dustloop GGST/Mechanics](https://www.dustloop.com/w/GGST/Mechanics) **[DOCUMENTED]**
- GGST normal → CH type: grounded heavy normals are Large; mid-strength normals and air normals are Mid; light normals are Small — [Dustloop GGST/Mechanics](https://www.dustloop.com/w/GGST/Mechanics) **[DOCUMENTED]** (The exact button names were icons and were stripped in extraction, so the mapping is partly **[INFERRED]**.)
- GG AC counter-hit extra hitstop by level (Lv1–5): +0, +2, +4, +8, +12. Untechable time doubles on CH — [Dustloop GGACR (search summary)](https://www.dustloop.com/w/GGACR/Attack_Attributes) **[DOCUMENTED]**
- GGXX: CH hitstop is halved when a move's normal hitstop is lower than usual, and absent when hitstop is 0 — [Dustloop Template:FrameDataGlossary-GGXX (search summary)](https://dustloop.com/w/Template:FrameDataGlossary-GGXX) **[DOCUMENTED]**
- SF6 Punish Counter: +20% damage, **+4 frames** of extra hitstun/advantage, and Drive gauge drain — [Infil Glossary: Punish Counter](https://glossary.infil.net/?t=Punish%20Counter) **[DOCUMENTED]**. Counter Hit is +2 — [Juicebox FGC on X](https://x.com/Juicebox_FGC/status/1578305895678689280) **[DOCUMENTED-community]**
- SF6 Manon patch note: "Increased hitstop on Punish Counter from 11 to 14 frames" — search snippet of SuperCombo SF6 data (srk.shib.live mirror, page blocked) **[DOCUMENTED-secondary, unverified]**. This shows SF6 can give PC its own longer hitstop on specific moves.
- DBFZ counter hit: the opponent flashes red, the HUD shows "Counter!", the announcer calls it, and the attacker gets +50% Ki. "There is no additional benefit to counter hits compared to normal hits" (no extra hitstop or hitstun) — [Dustloop DBFZ/Mechanics](https://www.dustloop.com/w/DBFZ/Mechanics) **[DOCUMENTED]**

**Special and system hitstop (for scale)**
- GGST: Gold Burst 16F, Blue Burst 15F. Roman Cancel shockwaves 0F hitstop (they use slowdown instead) — [Dustloop GGST/Mechanics](https://www.dustloop.com/w/GGST/Mechanics) **[DOCUMENTED]**
- GGST Floating Crumple: 16F hitstop + 11F (body) or 19F (head) airborne hitstun + grounded hitstun — [Dustloop GGST Attack Attributes](https://www.dustloop.com/wiki/images/cache/f/fc/GGST%252FAttack_Attributes.html) **[DOCUMENTED]**
- SF3 3rd Strike parry freeze: the attacker is frozen 16F + 4 (light) / 3 (medium) / 2 (heavy) / 0 (special/SA) — [SuperCombo SF3 System (search summary)](https://wiki.supercombo.gg/w/Street_Fighter_3:_3rd_Strike/System); [Fighting Game Guide SF3](https://www.fightinggameguide.com/sf3.html) **[DOCUMENTED-secondary]**

**Who freezes**
- GGST: "When an attack hits an object, both objects freeze in place briefly to give the attack the feeling of more impact." With a projectile, "Ky does not experience hitstop, only the projectile and the opponent do." Hitstop is used as a buffer window, so specials input during hitstop come out on the first frame after — [Dustloop GGST Attack Attributes](https://www.dustloop.com/wiki/images/cache/f/fc/GGST%252FAttack_Attributes.html) **[DOCUMENTED]**
- Sakurai (Famitsu col. 490): "both parties momentarily freeze, emphasizing the power of impact". Both freeze for the same duration, and more damage usually means longer hitstop — [Source Gaming translation](https://sourcegaming.info/2015/11/11/thoughts-on-hitstop-sakurais-famitsu-column-vol-490-1/) **[DOCUMENTED]**
- Infil: hitstop frames "exist outside the standard startup, active and recovery"; "when you feel like an attack is 'beefy', it's because of this" — [Infil Glossary](https://glossary.infil.net/?t=Hitstop) **[DOCUMENTED]**

**KO**
- Sakurai recommends extra hitstop on killing blows. This comes from a summary of his "Stop at the Important Parts!" episode — [note.com summary](https://note.com/matchima2/n/n435a2b0f151f?hl=en) **[DOCUMENTED-secondary]**
- I found no source giving SF6, DBFZ or GGST KO-freeze or KO-slowdown frame counts. **GAP**

### Inferences
- **[INFERRED]** Practical baseline for a 60 fps game with L/M/H, triangulated from SF6 (9/11/13) and DBFZ (8/11/16): L ≈ 8–9F, M ≈ 11F, H ≈ 13–16F. Use the same value on block. Give specials and supers 14–20F. Give Punish Counter or big counter hits +3 to +10F, or GGST-style 21–31F freezes plus slowdown for "highlight" hits. Reserve a KO freeze of roughly 30–60F plus slowdown for round-ending hits only. The KO number is design judgment, not sourced.
- **[INFERRED]** Hitstop steps of about 2 frames per strength (SF6 9→11→13, GGST 11→15) are enough for players to read L/M/H. The amount of victim reaction (sections 2 and 5) makes the larger perceived difference.
- **[INFERRED]** Because hitstop also works as an input buffer (Dustloop GGST), keep it the same for hit and block or document the difference. Cancel timing must stay consistent.

### Gaps
- SF2 (any version), SF3 3rd Strike, SF4/USF4 and SFV official per-strength hitstop: no primary source fetched. SuperCombo blocked access, and FAT has no hitstop column for those games. The SFV 8/12/15 figure comes from a single 2016 source and is unverified.
- MvC3/UMvC3 and KOF XV / Fatal Fury: City of the Wolves hitstop: not found.
- SF6 hitstop on block vs hit, and on KO: not found. The FAT field is a single "hitstop" value.
- Capcom developer statements on hitstop values: not found.

## 2. Hit shake (victim vibration), screen shake and camera zoom

### Takeaway
The best-documented rules for the victim's sprite shake come from Sakurai. Shake the victim (more than the attacker) horizontally on the ground and vertically in the air, start large and decay to zero across the hitstop, and never move the hurtbox. I found no hard numbers for SF6 or DBFZ screen shake. The one documented camera rule is GGST's: camera zoom-in only on Large Counter Hits. That supports using camera punches and zooms rarely, as an event marker rather than on every hit.

### Cited Findings
- Sakurai's 8 hitstop techniques (YouTube "Eight Hit Stop Techniques", Dec 2022): (1) shake the hit character more strongly; (2) keep the hurtbox in place; (3) shake horizontally on the ground and vertically in the air; (4) gradually reduce the shake; (5) control hitstop duration; (6) blend interpolated frames into the damage pose; (7) keep the attacker moving slightly during the freeze; (8) scale shake amount by camera distance — [Nintendo Wire recap](https://nintendowire.com/news/2022/12/12/this-week-in-sakurai-12-5-12-11-fine-tuning-hit-stop-and-cheating-the-system/); video: https://www.youtube.com/watch?v=tycbMSjDDLg **[DOCUMENTED]**
- Sakurai, Famitsu col. 490:
  - Air shake is vertical "because a vertical grounded vibration would clip the feet into the stage".
  - Shake "starts large and shrinks", computed by dividing the freeze frames.
  - After the first flinch frame, the pose blends into the hurt animation over 4 frames during hitstop.
  - Hurtboxes stay still, because moving them would cause "attacks that should have hit to miss and creating inconsistencies".
  - In Smash the attacker also vibrates slightly, which he says is not typical in 2D games.
  - 2D fighters like SF and GG use visible hitstop and sprite vibration that "simulates a sense of shock".
  - Smash Ryu "still has less hitstop in Smash than he did in Street Fighter II".
  - [Source Gaming translation](https://sourcegaming.info/2015/11/11/thoughts-on-hitstop-sakurais-famitsu-column-vol-490-1/) **[DOCUMENTED]**
- GGST: only Large Counter Hits get camera zoom-in. Mid and Large get unique cinematic hitstop and louder audio — [Dustloop GGST Attack Attributes](https://www.dustloop.com/wiki/images/cache/f/fc/GGST%252FAttack_Attributes.html) **[DOCUMENTED]**
- Infil describes hitstop as "like the game putting the characters on hold while someone shakes the camera a bit" — [Infil Glossary](https://glossary.infil.net/?t=Hitstop) **[DOCUMENTED, informal]**
- Infil, Screen Freeze: "Usually the screen gets darker, the camera angle might change, and the character does some special pose." This applies to supers and "certain other powerful techniques", not to normals — [Infil Glossary](https://glossary.infil.net/?t=Screen%20Freeze) **[DOCUMENTED]**
- Vlambeer, "The Art of Screenshake" (Jan Willem Nijman, Control Conference / INDIGO Classes, Amsterdam, Nov 2013): 30 small tweaks applied to a plain shooter. Two are reported in the write-up: screen shake "slightly in the opposite direction of the player's gunfire", and a 33% chance of enemies exploding on death — [Game Developer](https://gamedeveloper.com/design/vlambeer-co-founder-shares-advice-on-building-better-action-games); talk video https://youtu.be/AJdEqssNZ-U **[DOCUMENTED]**

### Inferences
- **[INFERRED]** Vlambeer's talk is about single-player shooters with many low-stakes hits per second. Screen shake on every shot works there. In a 1v1 fighter, constant screen shake hurts readability: spacing and frame-precise reactions depend on a stable view. So reserve screen shake for heavies, specials, wall/ground bounces, supers and KO, and use victim sprite shake for every hit. This matches GGST, which reserves zoom for Large CH, and Infil's description of dark screen and camera change for super freezes only.
- **[INFERRED]** Suggested sprite-shake starting values for a 2D sprite game, from Sakurai's rules plus common practice (no published fighter numbers):
  - Amplitude: about 2–3 px for L, 4–5 px for M, 6–8 px for H at 1080p game resolution. Alternate sign each frame (or every 2 frames) and decay linearly to 0 by the end of hitstop.
  - Direction: horizontal on the ground, vertical in the air.
  - Render offset only; hurtbox unchanged.
  - Attacker: little or no shake.
- **[INFERRED]** A camera "punch" or zoom on every hit plus dark flashes on every hit makes all hits look the same. A strength-tiered hierarchy (sprite shake < screen shake < zoom < dark screen) is what reads as professional.

### Gaps
- No pixel amplitudes, frequencies or screen-shake durations for SF6, DBFZ, SF3 or KOF were found. Frame-by-frame video analysis would be needed.
- No documentation was found of which DBFZ or SF6 moves trigger screen shake. Anecdotally these are Drive Impact wall splat, DBFZ Vanish/Smash! hits and supers, but that is unsourced.
- Sakurai's exact decay formula and frame counts are not in the text summaries. They are in the video itself.

## 3. Hitstun, blockstun and pushback for L/M/H (SF6, DBFZ, GGST)

### Takeaway
In SF6, hitstun grows from about 15 (L) to 22–24 (M) to 26–27 (H). Blockstun goes about 9–11, 16–18, 20–21. Pushback on hit roughly doubles per strength step, and pushback on block is about a third of on-hit pushback. DBFZ uses flat tables: hitstun 16/18/22 and blockstun 11/15/15 for L/M/H. GGST is 12–21 standing hitstun across Lv0–Lv4.

### Cited Findings
SF6 cast medians (FAT data; n≈30 characters per button; my aggregation) — [FAT SF6 JSON](https://raw.githubusercontent.com/D4RKONION/FAT/master/src/js/constants/framedata/SF6FrameData.json) **[DOCUMENTED data / aggregation mine]**:

| Button | Hitstop | Hitstun (median, range) | Blockstun (median, range) | Opp. pushback on hit (FAT "OppSoH") | On block ("OppSoB") |
|---|---|---|---|---|---|
| 5LP | 9 | 15 (13–18) | 9 (7–14) | 210 | 75 |
| 2LP | 9 | 16 (14–17) | 10 (8–12) | 210 | 75 |
| 5LK | 9 | 16 (12–18) | 11.5 (7–22) | 210 | 75 |
| 2LK | 9 | 15 (12–18) | 10 (8–13) | 210 | 75 |
| 5MP | 11 | 22 (19–26) | 18 (14–21) | 350 (350–490) | 125 |
| 2MP | 11 | 22 (19–25) | 16.5 (13–21) | 350 | 125 |
| 5MK | 11 | 24 (18–26) | 17 (13–20) | 490 (350–735) | 175 |
| 2MK | 9 | 23 (20–26) | 16.5 (15–20) | 420 | 150 |
| 5HP | 13 | 26 (24–28) | 21 (19–22) | 700 (560–920) | 250 |
| 2HP | 13 | 27 (24–36) | 20 (16–29) | 700 | 250 |
| 5HK | 13 | 27 (23–33) | 20.5 (17–26) | 700 | 250 |
| 2HK (sweep) | 13 | knockdown | 18 (14–25) | 700 | 250 |

- SF6 Ryu examples: 5LP is 4f startup, hitstun 14, blockstun 9, hitstop 9, +4 on hit, −1 on block. 5MP is hitstun 22, blockstun 14, hitstop 11, +7 / −1. 5HP is hitstun 27, blockstun 21, hitstop 13, +4 / −2. 5HK crumples on Punish Counter (+84). 2HK is a knockdown (+40), and a hard knockdown on PC (+47) — [FAT SF6 JSON](https://raw.githubusercontent.com/D4RKONION/FAT/master/src/js/constants/framedata/SF6FrameData.json) **[DOCUMENTED]**
- DBFZ attack-level table (Lvl0–4): hitstun on ground hit 14/16/18/20/22; untechable time on air hit 14/16/18/20/22; ground blockstun 11/11/15/15/15; hitstop 6/8/11/14/16 — [Dustloop DBFZ/Mechanics](https://www.dustloop.com/w/DBFZ/Mechanics) **[DOCUMENTED]**
- DBFZ SS Goku: 5L is Lvl1 (startup 6, −3 on block, blockstun 11, hitstun 16). 5M is Lvl2 (startup 9, −5, blockstun 15, hitstun 18). 5H is Lvl4 (startup 12~24, −8, Launch, air hit → wall splat). 2M is Lvl2, low, and launches. 2H is Lvl4, a launcher with head invulnerability — [Dustloop DBFZ Goku SS Frame Data](https://www.dustloop.com/w/DBFZ/Goku_(Super_Saiyan)/Frame_Data) **[DOCUMENTED]**
- GGST: Lv0–4 standing hitstun 12/14/16/19/21; crouching 13/15/17/20/22 (crouching is +1); ground blockstun 9/11/13/16/18 — [Dustloop GGST/Mechanics](https://www.dustloop.com/w/GGST/Mechanics) **[DOCUMENTED]**
- Infil, Pushback: pushback is "very important to fighting game balance". A move that is plus on block but pushes the attacker far away is less threatening — [Infil Glossary](https://glossary.infil.net/?t=Pushback) **[DOCUMENTED]**

### Inferences
- **[INFERRED]** The FAT fields "OppSoH"/"OppSoB" are read here as opponent pushback on hit/block in SF6 internal units. The field meaning is not documented in the file. Even so, the relative ratios are usable: L:M:H ≈ 1 : 1.7–2.3 : 3.3, and block pushback is about 36% of hit pushback.
- **[INFERRED]** Hitstun minus blockstun is about 5–6F at every strength in SF6, so the on-hit advantage bonus over block stays roughly constant. Bigger buttons buy more absolute stun, which gives room for longer and more visible hurt animations, not more relative advantage.
- **[INFERRED]** Hit-reaction animation length should cover hitstop plus hitstun: about 24F for L, 33F for M and 40F for H in SF6 terms. At 15–18 frames of animation for a light, a 3–4 frame recoil pose is too little. A hurt cycle (snap, hold, recover) is needed for every strength.

### Gaps
- SF6 pushback in pixels or screen fractions could not be verified (unit undocumented).
- I found no source for SF6 official frame-data site values or for MvC3/KOF hitstun tables.

## 4. Design rationale: Sakurai, Swink, Vlambeer — what transfers to fighters

### Takeaway
Sakurai's hitstop writings are the most directly applicable. They come from a fighting-game designer and cover freeze, shake direction, decay, hurtbox stability and pose blending. Vlambeer's screenshake advice is tuned for single-player shooters and should be applied sparingly in a 1v1 fighter. I did not fetch any Steve Swink "Game Feel" text this session.

### Cited Findings
- Sakurai, Famitsu col. 490:
  - Projectiles get less hitstop; electric attacks get an automatic increase.
  - A hitstop cap limits very powerful attacks (Hammer, Final Smashes).
  - Marth's sword tip gets more hitstop than the blade base.
  - In free-for-all, long freezes let a third player hit you, so Smash keeps hitstop short.
  - 3D fighters (Tekken, VF) use little hitstop, which "isn't very compatible with 3D fighters".
  - Dynasty Warriors uses little hitstop because of crowds.
  - The hurt animation exists because "polygonal damage freezes can look ambiguous".
  - [Source Gaming](https://sourcegaming.info/2015/11/11/thoughts-on-hitstop-sakurais-famitsu-column-vol-490-1/) **[DOCUMENTED]**
- Sakurai's YouTube "Eight Hit Stop Techniques" ("8つのヒットストップ仕様", Dec 9 2022): the 8 rules listed in section 2 — [Nintendo Wire](https://nintendowire.com/news/2022/12/12/this-week-in-sakurai-12-5-12-11-fine-tuning-hit-stop-and-cheating-the-system/) **[DOCUMENTED]**
- Sakurai's "Stop at the Important Parts!": a brief stop on hit makes attacks feel impactful, and the stop is recommended on decisive or killing blows — [note.com summary](https://note.com/matchima2/n/n435a2b0f151f?hl=en) **[DOCUMENTED-secondary]**
- Vlambeer screenshake: "30 minor tweaks", including directional shake opposite to fire — [Game Developer](https://gamedeveloper.com/design/vlambeer-co-founder-shares-advice-on-building-better-action-games) **[DOCUMENTED]**

### Inferences
- **[INFERRED]** Applies to fighters:
  - Freeze both fighters.
  - Scale freeze with damage or strength, and cap it.
  - Shake only the visual sprite (not the hurtbox), horizontal on the ground and vertical in the air, decaying.
  - Blend into the hurt pose during the freeze.
  - Give a big, decisive freeze to KO and counter hits.
  - Use directional kicks: shove the victim and the camera in the direction of the hit.
- **[INFERRED]** Does not apply, or apply rarely:
  - Constant screen shake.
  - Random "explode on death" style variance.
  - Long hitstop on every multi-hit or projectile hit. Sakurai reduces hitstop for projectiles, and multi-hit projectiles in GGST are paced by their own hitstop.
  - Camera moves that change framing during neutral.

### Gaps
- Steve Swink "Game Feel" (2008), the Vlambeer full 30-item list and Core-A Gaming hitstop videos: not retrieved this session.
- No Capcom/Arc/SNK developer interview on hitstop design was found.

## 5. Hit reactions: minimum hurt-animation set; strength vs reaction; audio sync

### Takeaway
Primary sources (frame data and glossaries) document the reaction states, but not animation frame counts. Every pro fighter has at least: standing hit (high/head and mid/body variants), crouching hit, air hit/juggle, knockdown and hard knockdown, launch, crumple or stagger, wall bounce/splat and ground bounce, plus the blocking poses (stand, crouch, air). Strength mainly changes recoil size, hitstun length and pushback. Heavy or counter hits switch to a different state (crumple, launch, knockdown).

### Cited Findings
- **Head vs body.** DBFZ strikes have a "head" or "body" attribute: standing/crouching attacks are body, jumping/air attacks are head. All 2H have head-attribute invulnerability, which is why they work as anti-airs — [Dustloop DBFZ/Mechanics](https://www.dustloop.com/w/DBFZ/Mechanics) **[DOCUMENTED]**. This is a gameplay attribute, not explicitly a reaction-animation rule.
- **Crumple variants (GGST).** Floating Crumple has two variants. Body is "a shallow float, with an animation of being pushed upwards": 16F hitstop, 11F airborne, 7F grounded. Head is "a launching float, with a massive recoiling animation": 16F hitstop, 19F airborne, 8F grounded — [Dustloop GGST Attack Attributes](https://www.dustloop.com/wiki/images/cache/f/fc/GGST%252FAttack_Attributes.html) **[DOCUMENTED]**
- **Other GGST hit effects.** Launch ("float" and "blowback"), Spinning Blowback, Guard Crush (a reeling animation), Stagger (escapable by mashing) and Vacuum (pulls the victim in) — [same source](https://www.dustloop.com/wiki/images/cache/f/fc/GGST%252FAttack_Attributes.html) **[DOCUMENTED]**
- **Glossary definitions (Infil).**
  - Crumple: "A state of super prolonged hitstun that sees the character reel over painfully, usually falling to the ground at the end" (SF4 Focus Attack, SF6 Drive Impact).
  - Wall Bounce: KOF calls it "wire" / "critical wire" / "counter wire".
  - Ground Bounce: usually once per combo.
  - Stagger: a prolonged reel.
  - Juggle.
  - [Infil Glossary](https://glossary.infil.net/) **[DOCUMENTED]**
- **SF6 strength-dependent states (Ryu).**
  - 2HP "forces stand" on hit (the victim switches to standing hurt).
  - 5HK puts airborne opponents into a juggle state and crumples on Punish Counter.
  - 2HK knocks down, and hard-knocks-down on PC.
  - [FAT SF6 JSON](https://raw.githubusercontent.com/D4RKONION/FAT/master/src/js/constants/framedata/SF6FrameData.json) **[DOCUMENTED]**
- **DBFZ.** Lights and mediums give plain hitstun (16/18F). Heavies (5H, 2H) launch, and 5H on an air hit gives a wall splat. 2M is a low launcher; j.H at Lvl3/4 sends the victim down into a slide or wall bounce. "Almost everyone has a wall bounce in 5H and 5LLLLLLL" — [Dustloop DBFZ/Mechanics](https://www.dustloop.com/w/DBFZ/Mechanics); [Goku SS Frame Data](https://www.dustloop.com/w/DBFZ/Goku_(Super_Saiyan)/Frame_Data) **[DOCUMENTED]**
- **Crouch hitstun.** GGST and GG XX give crouching victims +1F hitstun over standing — [Dustloop GGST/Mechanics](https://www.dustloop.com/w/GGST/Mechanics) **[DOCUMENTED]**. This implies a separate crouch-hit animation **[INFERRED]**.
- **Hit-reaction volume.** A full hit-reaction library for a combat-heavy (3D, non-fighter) game adds 20–40 animations before variations — [MoCap Online guide (search snippet)](https://mocaponline.com/blogs/mocap-news/combat-animation-game-dev-guide) **[DOCUMENTED-secondary, generic]**
- **Audio.** GGST ties louder audio cues to bigger counter-hit types — [Dustloop GGST](https://www.dustloop.com/wiki/images/cache/f/fc/GGST%252FAttack_Attributes.html) **[DOCUMENTED]**

### Inferences
- **[INFERRED]** Minimum professional hurt set (sprite fighter):
  1. Stand hit high / head-snap (from high punches and air attacks).
  2. Stand hit mid / body (doubled over; from kicks and low punches).
  3. Crouch hit.
  4. Air hit / juggle tumble (falling loop).
  5. Launch (rising).
  6. Knockdown: fall, hit floor, small bounce, lie down.
  7. Hard knockdown or sweep trip (feet swept).
  8. Wake-up / get-up (plus tech roll if the system allows).
  9. Crumple / stagger (for heavy Punish Counter or Drive-Impact-type hits).
  10. Wall splat / wall bounce.
  11. Ground bounce.
  12. Blockstun poses for stand, crouch and air, with a small and a big block-recoil variant.
  13. KO fall (often a longer, slower version of the knockdown).
  14. Optionally: dizzy/stun loop and a throw-victim pose set.
- **[INFERRED]** Strength scaling is the cheapest high-impact fix for "the hit character barely reacts". Use the same hurt pose, but:
  - Light: 1–2 snap frames, small lean, hold, then recover.
  - Medium: more lean or bend, longer hold.
  - Heavy: maximum recoil pose, a slide back driven by pushback, often a distinct "big" pose.
  - The pose should key on the first frame of hitstop (Sakurai: flinch frame, then a 4-frame blend during the freeze). The recovery should fill the hitstun window computed in section 3.
- **[INFERRED]** Audio and visual sync: play the impact SFX and spawn the hit spark on the first frame of hitstop, the same frame as the flinch pose. Both are frozen-time-safe, so audio should not be paused by hitstop. GGST's "louder audio cues" for bigger CH types support scaling SFX with strength.

### Gaps
- Exact hurt-animation frame counts and the full reaction list for SF2/SF3/SF6/KOF/DBFZ were not found in a primary source. Sprite-rip archives (Spriters Resource) and frame-by-frame analysis would be needed.
- No SF3 or Skullgirls animator statement on the importance of hit reactions was retrieved. The Skullgirls/Mizuumi wiki and Mike Zaimont or Alex Ahad interviews were not reached.
- No primary source was found stating that impact SFX must start on the first hitstop frame. That rule is industry practice (**[INFERRED]**).
