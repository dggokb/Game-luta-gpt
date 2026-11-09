# VFX language and sound of hits and blocks in professional fighting games

Labels used on every item:
- **[DOCUMENTED]**: stated by a primary or reference source (developer column, developer interview, frame-data wiki), cited inline.
- **[DOCUMENTED-2ARY]**: stated by a secondary source (news summary, fan translation, TV Tropes, forum). Use with care.
- **[INFERRED]**: my conclusion from the documented items. Not stated by any source.
- **Measured from footage:** none. I did not measure any frame counts or sizes from video in this session. Every number below comes from a published table or interview, and is marked as such.

Research coverage note: the strongest primary sources found were (1) Capcom's official SF6 VFX dev column "Behind the Effects" (#001 Drive Impact, #002 Drive Rush, #003 Drive Parry), (2) Dustloop's Guilty Gear Strive mechanics page (hitstop, counter-hit tables), (3) A Sound Effect's Mortal Kombat 1 audio interview, and (4) a Riot 2XKO VFX artist interview. No primary source was found for DBFZ, MvC3, Fatal Fury CotW, or MSHvSF hit sparks specifically (see Gaps).

---

## 1. What a hit spark is for, and how pro games design it (purpose, size, duration, color, shape, placement)

### Takeaway
A hit spark is a **game-state signal first and decoration second**: it tells both players *that* contact happened, *what kind* of contact (hit vs block vs parry vs counter), and *how strong*. SF6's VFX team documents a strict grammar: on hit the effect **spills over onto the opponent**, on block it **stops short**, hits use **circle shapes**, parry uses an **X shape**, and the loudest effect in the game (Drive Impact) was designed first and **every other effect was derived from it**. Guilty Gear Strive documents that hitstop (the freeze that a spark lives inside) runs **11 to 15 frames** by strength, and that counter hits get "more dramatic visuals and audio" in tiers.

### Cited Findings

**Purpose of the hit effect**
- [DOCUMENTED-2ARY] Sakurai: "providing feedback when a character attacks or gets hit is important", especially when the camera is pulled back; hit marks ("ヒットマーク") serve that feedback role. Video in the "Effects" category of *Masahiro Sakurai on Creating Games*, July 2023. — [GoNintendo summary](https://www.gonintendo.com/contents/23556-masahiro-sakurai-discusses-hit-marks-in-his-latest-dev-video)
- [DOCUMENTED-2ARY] Sakurai: effects "lack physical presence and are regulated by time, so their density is extremely important"; the right level of detail, sparkle and size is what elevates them. — [GoNintendo, Mar 2024](https://www.gonintendo.com/contents/33516-masahiro-sakurai-discusses-the-intricacies-of-effects-creation)
- [DOCUMENTED-2ARY] Sakurai's hitstop video "Stop for Big Moments! [Design Specifics]": the game pauses for a split second when an attack lands; he lists escalating levels: as-is, stop, stop + vibration, stop + vibration + slow motion; he calls hitstop "extremely important for making the player feel the impact strongly" and says it should be considered in every genre. — [YouTube](https://www.youtube.com/watch?v=OdVkEOzdCPw); [summary on note.com](https://note.com/matchima2/n/n435a2b0f151f?hl=en)
- [DOCUMENTED-2ARY] Sakurai (Famitsu column vol. 490, fan translation): in Smash, grounded characters in hitstop vibrate side to side, airborne characters vibrate up and down. — [Source Gaming](https://sourcegaming.info/2015/11/11/thoughts-on-hitstop-sakurais-famitsu-column-vol-490-1/)

**Hit vs block vs parry: shape and spill rule (SF6, primary)**
- [DOCUMENTED] SF6 VFX team: "It was already a set design policy at this time to have effects **spill over to the opponent on hit and stop short on block**." — [SF6 column #003 Drive Parry](https://www.streetfighter.com/6/en-us/column/detail/effects003)
- [DOCUMENTED] SF6: the Drive Parry effect uses "the **x-shaped** light effect... to contrast with the **circle-shaped** effects you see when moves hit." — [SF6 column #003](https://www.streetfighter.com/6/en-us/column/detail/effects003)
- [DOCUMENTED] SF6: Perfect Parry was made bigger than normal parry, with ink exploding toward the opponent to show you "snatched advantage", but "we tried to make this stand out while **not standing out too much or making things difficult to parse**." Blue was explored for parry because "the idea of guarding seemed to fit the color." — [SF6 column #003](https://www.streetfighter.com/6/en-us/column/detail/effects003)
- [DOCUMENTED] SF6: the Perfect Parry effect was designed to "continue into your next attack" to reinforce that you move first (effect communicates frame advantage). — [SF6 column #003](https://www.streetfighter.com/6/en-us/column/detail/effects003)
- [DOCUMENTED-2ARY] TV Tropes "Hit Spark" trope page: blocked hits are usually a softer, ripple-like effect; critical/heavy hits look more chaotic. — [TV Tropes](https://TVTropes.org/pmwiki/pmwiki.php/Main/HitSpark)

**One loud anchor effect, everything else derived from it (SF6, primary)**
- [DOCUMENTED] SF6 Drive Impact: "Drive Impact represents the **loudest effect in the game**, and we used that to create the other effects in the game." — [SF6 column #001 Drive Impact](https://www.streetfighter.com/6/en-us/column/detail/effects02)
- [DOCUMENTED] SF6 Drive Rush was designed after Drive Impact: "We had the base of Drive Impact to work off of... our job was to find an effect that screamed 'drive rush' while matching with what we had." — [SF6 column #002 Drive Rush](https://www.streetfighter.com/6/en-us/column/detail/effects002)
- [DOCUMENTED] SF6 concept keywords for the whole system: "Brush", "Paint", "Comic", "Spray", "Graffiti", "Music Video", "Sports". A "music video noise" motif was scrapped because "to most people it would look like a bug." A painting motif mixing SF4/SF5 concepts was rejected on cost. 3D graffiti became the base. — [SF6 column #001](https://www.streetfighter.com/6/en-us/column/detail/effects02)
- [DOCUMENTED] SF6: the effect style was deliberately made *different* from the character style: "Since the characters in SF6 lean towards the realism end of the spectrum, if we also made the effect realistic then they'd overlap with each other and there would be a bit of **information overload**... We feel that having a **gap between the effect and character** is more interesting." — [SF6 column #001](https://www.streetfighter.com/6/en-us/column/detail/effects02)
- [DOCUMENTED] SF6: Drive Impact effects "fill the screen **from behind the character performing the attack**, so it's easy to see which side is on the attack." Color per character is chosen "based on the character's personalities, culture, and feeling." — [SF6 column #001](https://www.streetfighter.com/6/en-us/column/detail/effects02)
- [DOCUMENTED] SF6: the director wanted the system effect recognisable "from a quick glance" and character-specific even though the mechanic is shared. — [SF6 column #001](https://www.streetfighter.com/6/en-us/column/detail/effects02)
- [DOCUMENTED] SF6 first VFX column (Luke's Sand Blast): concept was a bullet of compressed air with sparks from air friction; "we had to balance it for playability, so we did things such as adding **afterglow to the bullet and sparks for better readability**." — [SF6 column "Behind the Effects" opened](https://www.streetfighter.com/6/en-us/column/detail/effects01)
- [DOCUMENTED] Capcom's RE ENGINE talk (CAPCOM Open Conference RE:2023, Aoyama & Yoneyama): SF6's Hadoken is built from separate effect assets for charge, launch, travel, and impact. — [Docswell slides](https://www.docswell.com/s/CAPCOM_RandD/Z8GQ31-RE2023)

**Size relative to hitbox (Riot 2XKO, primary interview)**
- [DOCUMENTED] Tyier Godette (VFX Artist, Riot, 2XKO): "One of the biggest things was the hitbox." "We didn't want to extend the effect too far outward" or "make the move itself feel bigger than it actually was", to avoid "making the hitbox feel misleading." The effect had to "work consistently no matter where the move happened on screen." — [VFX Apprentice interview](https://www.vfxapprentice.com/blog/2xko-vfx-artist-interview-tyier-godette)

**Duration: hitstop numbers by strength (Guilty Gear Strive, reference wiki of official frame data)**
- [DOCUMENTED] GGST hitstop by Attack Level: Lv0 = 11f, Lv1 = 12f, Lv2 = 13f, Lv3 = 14f, Lv4 = 15f. Ground blockstun: 9 / 11 / 13 / 16 / 18f. Standing hitstun: 12 / 14 / 16 / 19 / 21f. — [Dustloop GGST Mechanics](https://www.dustloop.com/w/GGST/Mechanics)
- [DOCUMENTED] GGST counter hits come in tiers (Very Small, Small, Mid, Large): "The larger the counter hit type, the **more dramatic the visuals and audio, with louder audio cues, longer hitstop, longer slowdown**; and in the case of Large Counter Hits, **camera zoom-in**." A "COUNTER!" text is shown. — [Dustloop GGST Mechanics](https://www.dustloop.com/w/GGST/Mechanics)
- [DOCUMENTED] GGST counter-hit values: Small = base hitstop + 11f slowdown; Mid = 21f hitstop + 25f slowdown; Large = 31f hitstop + 35f slowdown. Gold Burst 16f hitstop, Blue Burst 15f. — [Dustloop GGST Mechanics](https://www.dustloop.com/w/GGST/Mechanics)
- [DOCUMENTED-2ARY] Realtime VFX forum feedback thread on fighting-game character VFX: the artist made the hit/fire shockwave "a lot faster so that it's **gone before the player being hit can move again**." — [realtimevfx.com thread 9498](https://realtimevfx.com/t/fighting-game-character-vfx-showcase-feedback/9498) (seen via search snippet; page not fetched in full)
- [DOCUMENTED-2ARY] Realtime VFX forum (speculative, from a non-fighting-game artist): fighting games likely force "punchy timing" and "more emphasis on the fall-off of the effect, rather than the build-up." No replies from practitioners. — [realtimevfx.com thread 6599](https://realtimevfx.com/t/vfx-in-fighting-games/6599)

**DBFZ / ArcSys**
- [DOCUMENTED-2ARY] ArcSys emulates anime "limited animation" (fluid look without extra frames); Daniel Floyd's breakdown, based on Junya Motomura's GDC talk, covers "the power of visual effects on attacks" for hand-drawn feel in 3D, but the article gives no specifics. — [Kotaku](https://kotaku.com/breaking-down-the-animation-of-guilty-gear-and-dragonba-1836849279); GDC talk: [YouTube](https://www.youtube.com/watch?v=yhGjCzxJV3E)

### Inferences
- [INFERRED] A coherent hit-effect system needs one **grammar table** where each axis maps to exactly one visual variable, as SF6 does: *outcome* (hit/block/parry) = **shape + spill** (circle that spills onto the defender vs. shape that stops short at the guard), *strength* = **size + hitstop length**, *counter* = **extra layer + text + longer freeze**, *character/system identity* = **color**. Mixing variables (e.g., strength changes color and shape at random) is the likely cause of the "random" look the owner reports.
- [INFERRED] Effect duration should be **slaved to hitstop**: the bright core lives during hitstop (GGST: ~11–15f at 60fps ≈ 0.18–0.25 s) and the tail fades by the time the defender can act (Realtime VFX advice). A green-screen clip that plays 0.5–1 s regardless of hitstop will read as "lasting longer than the hit."
- [INFERRED] Strength scaling in GGST is small per step (+1f hitstop per level). Visual size steps between weak/medium/strong should likewise be **clear but proportionate** (e.g., about 1.0x / 1.3x / 1.6x of a base size), not three unrelated clips.
- [INFERRED] Following Godette, the spark should be **anchored at the contact point** (hitbox/hurtbox overlap) and should not extend much past the real hitbox; a big spark on a short jab misleads players about range.
- [INFERRED] Following SF6 #001, pick **one "loudest" effect** for the game (here likely the Super/Ultra or SHINKAI impact), design it first, and derive weak/medium/strong/counter/block as reduced versions of the same brush, palette and shape family. That is the missing "concept."
- [INFERRED] Counter hit in a 2D game can borrow GGST's tier structure cheaply: same spark + a contrasting ring/flash layer + "COUNTER" text + a few extra frames of hitstop. Large counters (rare) can add a slight camera punch-in.

### Gaps
- No primary source found on exact DBFZ, MvC3, MSHvSF, Fatal Fury CotW, SF2/SF3/SF4 hit spark sizes, frame counts, or color code. The commonly cited color conventions (e.g., SF4/SF5/SF6 hit sparks warm/orange-yellow, block sparks blue/white; SF6 "PUNISH COUNTER" text) come from general knowledge and were **not verified** this session. They should be checked by frame-stepping footage.
- No source gave hit spark size as a ratio of character height. This needs measuring from footage (e.g., training-mode screenshots).
- SF6 hitstop values per strength were not retrieved (SuperCombo/FAT wikis not fetched). Only GGST numbers are documented here.
- Arc System Works has no public VFX-specific talk found for DBFZ hit effects; the Motomura GDC talks focus on animation/rendering.
- Sakurai's "hit marks" and "Flash, Blast, and Smoke" videos were only available as secondary summaries; their exact rules on placement, size and duration were not obtained.

---

## 2. What to avoid (covering characters, too many effects, mixed styles, effects outlasting the hit, ambient effects)

### Takeaway
Pro sources consistently cut effects that **hurt reading of space and state**: SF6 removed speed lines from Drive Rush because they made distance hard to judge, rejected a VHS-noise style because it looked like a bug, and kept the effect style deliberately different from the characters to avoid information overload. Riot keeps effects inside the hitbox. Sakurai stresses density control.

### Cited Findings
- [DOCUMENTED] SF6 Drive Rush: "The **speed lines** from the other designs were **removed** due to worries that they made the character seem to go too fast, in addition to worries that it **may make judging the distance between the two players difficult**." — [SF6 column #002](https://www.streetfighter.com/6/en-us/column/detail/effects002)
- [DOCUMENTED] SF6: realistic effects on realistic characters would "overlap with each other" causing "information overload, weakening the intended effect." — [SF6 column #001](https://www.streetfighter.com/6/en-us/column/detail/effects02)
- [DOCUMENTED] SF6: a style motif was scrapped because "to most people it would look like a bug." — [SF6 column #001](https://www.streetfighter.com/6/en-us/column/detail/effects02)
- [DOCUMENTED] SF6 Perfect Parry: aim to "stand out while not standing out too much or making things difficult to parse." — [SF6 column #003](https://www.streetfighter.com/6/en-us/column/detail/effects003)
- [DOCUMENTED] 2XKO: do not let the effect exceed the hitbox or make the move look bigger than it is. — [VFX Apprentice](https://www.vfxapprentice.com/blog/2xko-vfx-artist-interview-tyier-godette)
- [DOCUMENTED-2ARY] Sakurai: effect density is "extremely important" because effects are regulated by time. — [GoNintendo](https://www.gonintendo.com/contents/33516-masahiro-sakurai-discusses-the-intricacies-of-effects-creation)
- [DOCUMENTED-2ARY] Riot's 2017 League of Legends VFX guide: VFX must prioritize gameplay clarity through accurate hitbox representation, value/saturation ranges, shape language and timing stages (not fighting-game specific). — [Deck.gallery copy of the guide](https://www.deck.gallery/league-of-legends-2017/)
- [DOCUMENTED-2ARY] Player feedback (EA UFC forum) asks for an option to disable hit sparks because bright flashy sparks are distracting. — [EA forums](https://forums.ea.com/discussions/ufc-6-feedback-en/give-the-option-to-disable-hitspark/13534138)
- [DOCUMENTED-2ARY] A review of 2XKO credits readability to "clear art direction and visual design" and says hits are reinforced by "crunchy sound effects." — [GamingBolt review](https://gamingbolt.com/2xko-review-lets-go)

### Inferences
- [INFERRED] For green-screen video effects specifically, "mixed styles" is the main risk: clips from different sources differ in line weight, palette, frame rate, motion blur and lighting. Normalize them: one palette (2–3 hues + white core), one edge treatment, one playback rate, additive blending for flashes.
- [INFERRED] Effects should sit **behind or at the contact point**, never centered over a face/torso for long. SF6 places its biggest effect *behind the attacker*.
- [INFERRED] A cap on simultaneous effects (e.g., one hit spark per contact, reuse/replace the previous spark of the same attacker instead of stacking) prevents clutter in long combos.
- [INFERRED] No ambient effects on every step; reserve movement effects for state changes (see section 3).

### Gaps
- Sakurai's specific video on "not hiding the characters" was not located; no direct quote obtained.
- No Skullgirls (Lab Zero) VFX postmortem was found in this session.

---

## 3. Movement effects (dust, landing, dash)

### Takeaway
Very little primary documentation exists. The documented signal is negative: SF6 removed speed lines from a dash-like mechanic for readability of distance. Pro practice must be measured from footage.

### Cited Findings
- [DOCUMENTED] SF6 removed speed lines from Drive Rush (a dash) because they distorted speed perception and distance judgment; instead "we gave the attacks an extra effect to make them seem powered-up." — [SF6 column #002](https://www.streetfighter.com/6/en-us/column/detail/effects002)
- [DOCUMENTED] SF6 Drive Rush colors were changed from a proposed blue (shared by all Drive effects) to "vitamin-y" yellow and green, meaning a system state gets its own consistent color. — [SF6 column #002](https://www.streetfighter.com/6/en-us/column/detail/effects002)

### Inferences
- [INFERRED] Use dust only on **events with gameplay meaning**: dash start, landing from a jump, knockdown/ground bounce, wall/ground slide on heavy hit. Keep it small (low on the screen, at the feet, under ~1/4 of character height), low-contrast and short (a few frames). Do not use it on walking or idle.
- [INFERRED] A kick trail (smear) should match the limb's real arc and the active frames only, ending when the move's active frames end, so it does not imply extra range.

### Gaps
- No source found giving size/duration rules for dust/landing effects in SF6, DBFZ or MvC. Needs measuring from footage.

---

## 4. Sound: minimum professional set, sync with hitstop, layering, and libraries

### Takeaway
Pro fighting games use a **shared core library of impacts and blocks** for normals, custom sounds for specials, impact size that scales with damage, mix ducking so hits cut through, and a large set of voice "efforts" per character. SF6 even uses different hit sounds by attack height for accessibility. The sound must be cut to the frame (Sakurai flagged a sound 2 frames too long, see Gaps for sourcing).

### Cited Findings
- [DOCUMENTED] Mortal Kombat 1 (NetherRealm), Dan Forden: a **general core library of impacts and blocks** covers simple punches and kicks; weapon/magic attacks get custom launches, impacts and blocks "since we want to represent the unique character of the attack." — [A Sound Effect](https://www.asoundeffect.com/mortal-kombat-1-game-audio/)
- [DOCUMENTED] MK1, Matt Swanson: hits are split by category: "body impacts have more crunch, face hits have more squish, and blade hits sound sharp and painful"; "combined with our **ducking system**, we ensure that each hit punches through the mix." Each character has a sonic palette of "impact layers, whooshes, and sustained drones." — [A Sound Effect](https://www.asoundeffect.com/mortal-kombat-1-game-audio/)
- [DOCUMENTED] MK1, Rich Carle: "the **size and shape of the impacts correspond** well to the **amount of damage** a character takes"; impacts are matched to each character's visual reaction. More side-chain compression so gameplay-critical sounds "pop through better when the fighting gets intense." — [A Sound Effect](https://www.asoundeffect.com/mortal-kombat-1-game-audio/)
- [DOCUMENTED] MK1 voices (George Hufnagl, Angela Geiss): each actor records **50+ effort categories**, giving **300–400 files per character**, about half used in core gameplay; reaction grunts/efforts matter as much as the hits. — [A Sound Effect](https://www.asoundeffect.com/mortal-kombat-1-game-audio/)
- [DOCUMENTED] GGST: counter-hit tiers have "louder audio cues" as the tier increases. — [Dustloop](https://www.dustloop.com/w/GGST/Mechanics)
- [DOCUMENTED] SF6 sound accessibility settings include separate sounds for **high / mid / low attack hits**, a cross-up hit sound, a distance-to-opponent sound, and Super Art availability sound (developed with ePARA for blind players). — [Automaton (JP)](https://automaton-media.com/articles/newsjp/20221011-222336/); [Automaton (EN)](https://automaton-media.com/en/news/20230502-18743/)
- [DOCUMENTED-2ARY] SF6 sound effects were produced with Forcewick Sound Design (per soundtrack credits); no hit-design interview found. — [Capcom Sound discography](https://www.capcom.co.jp/sound/en/post_discography/m00007089/)
- [DOCUMENTED-2ARY] SF6 June 2024 update added sound samples from 6 classic Street Fighter games (shows Capcom treats hit/voice sounds as series identity). — [Time Extension](https://www.timeextension.com/news/2024/06/street-fighter-6-update-adds-iconic-sounds-from-6-classic-street-fighter-games)
- [DOCUMENTED-2ARY] Sakurai: game sound effects are usually exaggerated; realistic ones can convey more information; either works if the balance suits the game. — [GoNintendo](https://www.gonintendo.com/contents/18181-masahiro-sakurai-discusses-game-audio-fiction-non-fiction)

**Free/affordable libraries**
- [DOCUMENTED] Kenney "Impact Sounds": 130 files, license **CC0** (public domain, commercial use, no attribution). — [kenney.nl](https://kenney.nl/assets/impact-sounds)
- [DOCUMENTED-2ARY] Sonniss #GameAudioGDC bundles: free, royalty-free, commercial use, historically no attribution and unlimited projects; recent terms reported to forbid AI training. Official license text was not fetched; verify at sonniss.com. — [GameFromScratch 2024](https://gamefromscratch.com/sonniss-27-5gb-sound-effect-giveaway-at-gdc-2024/); [gamedev.tv 2018 post](https://community.gamedev.tv/t/30gb-of-high-quality-royalty-free-sound-effects-the-sonniss-gameaudiogdc-bundle-2018/63406); [Bedroom Producers Blog 2026](https://bedroomproducersblog.com/2026/03/16/sonniss-gdc-2026-bundle/)
- [DOCUMENTED-2ARY] Shapeforms "Hit and Punch" library on itch.io (paid/affordable; check license on page). — [itch.io](https://shapeforms.itch.io/hit-and-punch)

### Inferences
- [INFERRED] **Minimum sound set** for a "professional" feel, derived from MK1's core-library model plus GGST/SF6 conventions:
  1. Whoosh/swing: 3 sizes (L/M/H), played on startup, so whiffs are audible.
  2. Hit impact: 3 sizes (L/M/H), each built from layers (low "thump" body + mid "slap/crunch" + short high transient), with 2–3 random variations each to avoid repetition.
  3. Block: 1–2 sounds, clearly different in timbre from hits (duller, more "guard/clack"), plus a stronger variant for heavy blocks.
  4. Counter hit: the normal hit + an extra sting layer (louder, as GGST documents).
  5. Special/Super impacts: a few custom ones per character (MK1 model).
  6. Voices: per character at least attack efforts (L/M/H), hurt (light/heavy), knockdown, special/super call, win/lose; MK1 uses 50+ categories, a small game can start with ~10–15.
  7. Announcer: round start ("Round 1 / Fight"), "K.O.", "Perfect"/"Time".
  8. UI: menu move/confirm/cancel, and a land/jump/dash foley set.
- [INFERRED] **Sync with hitstop**: trigger the impact sound on the first frame of contact (start of hitstop), not after; let it ring through the freeze. The visual spark core and the sound transient should start on the same frame. Bigger hitstop = bigger sound tail.
- [INFERRED] **Mix rule**: hit/block sounds and voices on top; music ducked briefly on heavy hits (MK1 side-chain approach).

### Gaps
- No SF6, DBFZ, or ArcSys sound team interview on hit-sound design was found (EN or JP searches).
- No GDC audio talk on fighting games was retrieved in this session.
- Sakurai's Melee roundtable remark that a sound "didn't fit" the motion because it was **2 frames too long** appeared only in a search-result summary; the exact source URL could not be confirmed, so it should be treated as unverified.
- Sonniss current license text not read directly; Freesound (mixed CC0/CC-BY/CC-BY-NC licenses per file) and Zapsplat were not checked.
