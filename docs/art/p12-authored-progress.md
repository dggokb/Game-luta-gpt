# P12 authored motion — in progress, NOT APPROVED

Branch: `test/p09-three-engines-20261009`. No merge to main branches.

## Source checkpoint

33 of 42 states have independently drawn source sequences. Every new sequence is preserved in `art/keys/p12/sheets/`; the new `.source.json` files contain the exact generation prompt, grid, peak and Git blob identity. Twelve sheets recovered from the interrupted session have no surviving exact prompt; their recovery is explicitly disclosed by the extractor.

The extractor isolates actual connected figures and preserves their color/alpha. It rejects cropped or merged drawings. An initial GROUNDED sheet failed because it had seven connected figures instead of eight; it was replaced with a two-column, four-row sheet. Previews now use one scale per state, instead of hiding scale variation by independently resizing thumbnails.

## Engine work

The rejected Idle deformation generator has been removed from `p12_full_cycle.py`. Its entry points now use `p12_authored_cycle.py`, which reads independently drawn start, peak, recovery and intermediate poses. It has no Idle deformation fallback. Full refinement requires all 42 clean sources and explicit scale/root/movement review registration. That registration is still missing; the new production renderer has NOT prepared or integrated the pack.

The combat contract is preserved verbatim in `p12-motion-contract.json`. The renderer keeps frame counts, durations, impact indices, locomotion stride, moves, specials and cancel windows. Native pixels are reproducibly derived from their consumed sources. AnimeVideo-v3 uses a persistent content-addressed cache keyed by source pixels, size, model and alpha pipeline version; alpha is preserved separately.

The existing harmony and visual audit errors remain blocking. `p12_pose_gate.py` is unchanged. The runtime smoke workflow no longer fetches hardcoded old run 38011770619: it selects the artifact and checkout SHA from the corresponding successful P12 build. Valid build runs are no longer cancelled by later pushes.

## Verification so far

- 8 focused extractor/renderer/cache tests pass.
- The first renderer test exposed a color rounding error at unit scale; it was corrected by avoiding unnecessary premultiplied conversion when no resize is requested.
- Official AnimeVideo-v3 weights downloaded and SHA-256 checked against `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`.
- Source geometry checks show actual standing-to-squat, squat-to-standing and landing absorption changes. These limited measurements do not approve attacks or walking.
- Runtime source gate still rejects 0/40 consumed action bindings, correctly: the runtime still contains the old rejected pack.
- The full existing Python suite was started and remains running at this checkpoint; no success is claimed.

## Unresolved

Nine source states remain to be drawn: COMBAT, ULTRA_BEAM, THROW_GRAB, THROW_TOSS, INTRO, JUMP_HEAVY_DOWN, VICTORY, DEFEAT and TAUNT. DEFENSE_CROUCH needs a lower posture. All states still need anatomical scale/root registration, support-foot/gait evidence, final continuity review, refinement, Character Pack import and mandatory audit passes. No new APK or emulator approval exists yet.

The character is NOT finalized. Source coverage is not animation approval.
