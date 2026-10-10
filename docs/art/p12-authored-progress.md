# P12 — existing Drive video extraction, NOT APPROVED

Branch: `test/p09-three-engines-20261009`. No merge to main branches.

The user corrected the source requirement: use the existing videos in `animations/p12` on Google Drive. Newly generated sprite sheets are superseded drafts and are NOT the movement source for this integration. No more poses are to be generated for this task.

## Real source work

The exact Drive folder is recorded in `tools/sprites/videos/p12-drive-inventory.json`. It contains 33 MP4 videos and two initial pose PNGs. The originals were downloaded, byte lengths checked, and each complete video strictly decoded with FFmpeg. Two initial partial downloads were detected and replaced with complete originals.

`p12_video_sources.py` extracts every sequential frame with its original timestamp, source bounding box, original-video SHA-256 and extracted-frame SHA-256. It uses the existing chroma-key engine, with no pose synthesis, limb warping or resize. All 33 videos have 97 frames: 3,201 real source frames were extracted without decode errors. Cache reuse requires the same video, extractor version, keyer and all frame hashes.

The existing measurement engine calibrated the original `inicio_centro` and `inicio_agachado` against `agachado.mp4`. Standing visual height remains 208. Measured source scales are 0.5375 and 0.5366; the crouch reference matches video frame 52 with score 0.97 and zoom 1.343. This calibration is a measurement, not final animation approval.

## Segments and defects still under review

The input filenames do not map one-to-one to 42 states. Jump, crouch, grab and knockdown/getup videos contain multiple phases. The extraction tool records candidate bindings to all 42 states, explicitly pending interval review. No combat durations, impact indices, commands, hitboxes or cancel windows have changed.

No dedicated standing-hit or downward-air-heavy file was found in the P12 folder. Candidate footage exists in the initial recoil of `derruba_levanta.mp4` and the downward slam of `jH.mp4`; these candidates are not automatically approved as replacement clips. The former includes an attached beam/impact effect. The latter has visible zoom and scale changes around frames 65–70, plus a ground ring. These need correction and review using the actual source frames.

Additional review is required for body scale, effects attached to the silhouette, foot support, segment boundaries, motion recognition and state transitions. A different hash is not a semantic pass.

A first native LIGHT_JAB trial was rejected by the existing size-measurement engine: its best match against the known initial poses scored only 0.40. No arbitrary scale was applied and no runtime atlas was published. Matching/calibration against the actual video Idle and source-head measurements remains necessary.

All 8 existing focused extractor/renderer/cache regression tests pass locally after installing the required CPU Torch dependency. They are engineering tests, not a visual approval of the new video-derived P12.

## Runtime and validation

The runtime P12 pack is still the rejected previous pack. The newly generated drafts have not been integrated. `p12_full_cycle.py` now requires traceable existing-video provenance before production preparation or verification, preventing the superseded generated-sheet path from preparing another pack.

`p12_pose_gate.py`, harmony and graphic audit thresholds remain unchanged. The old runtime still fails source binding and has its known 14 harmony defects and 16 critical graphic findings. These have NOT been marked resolved by the extraction work.

Candidate frame/impact selections now cover all 42 states in `tools/sprites/videos/p12-segments-draft.json`. Each selected source hash matches the original extraction. Counts and impact indices match the unchanged combat contract. Walk previews use original source PTS because runtime walking is distance-driven, not duration-driven. `docs/art/p12-video-state-review/` contains 42 animated source previews, individual three-phase sheets, six overview pages, an offline HTML gallery and exact provenance. All are explicitly DRAFT_NOT_APPROVED, not normalized final animations.

Source review identified distinct high kicks in `jL.mp4`, a real low leg sweep in `2M.mp4` and a forward claw strike in `L.mp4`. It also records real unresolved mismatches: `M.mp4` shows a body charge rather than an extended kick; `2H.mp4` rises into a roar/recoil without a clear striking limb; `agarrao.mp4` embeds a dark opponent and shows bite/release rather than a distinct toss. Existing engine state names and combat data have not been changed to hide these differences.

Production `p12_full_cycle.py` now calls `p12_video_cycle.py`, not the superseded authored-sheet renderer. Its source reader hashes the actual consumed video extraction pixels against the recorded originals and only permits subtractive alpha masks, measured uniform whole-pose camera scaling and root translation. The mandatory start/peak files are actually read into the rendered atlas. It rejects missing/unreviewed registration before runtime or SR writes. No registration was fabricated, and no state was marked reviewed. Cached SR implementation is reused without rewriting the model or alpha validation.

The additional 10 video provenance tests exercise changed pixels disguised as a video frame, stale masks, alpha-only removal, repeated neutral frames, fabricated timestamps, shifted impact indices and distance-driven walk timing. All 18 focused P12 tests pass (8 previous + 10 new). A preparation trial remains correctly blocked because video registration is absent; production atlases and APK have not been replaced.

Pending: visual confirmation/correction of the 42 candidate selections; scale and support-foot correction; alpha/effect review; cached real super-resolution; Character Pack/Android resources and Java generation; mandatory audit passes; Android tests; emulator exercise and APK.

The character is NOT finalized. Extraction is not animation approval.
