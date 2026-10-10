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

## Runtime and validation

The runtime P12 pack is still the rejected previous pack. The newly generated drafts have not been integrated. `p12_full_cycle.py` now requires traceable existing-video provenance before production preparation or verification, preventing the superseded generated-sheet path from preparing another pack.

`p12_pose_gate.py`, harmony and graphic audit thresholds remain unchanged. The old runtime still fails source binding and has its known 14 harmony defects and 16 critical graphic findings. These have NOT been marked resolved by the extraction work.

Pending: reviewed frame/impact selections; scale and support-foot correction; alpha/effect review; cached real super-resolution; Character Pack/Android resources and Java generation; mandatory audit passes; Android tests; emulator exercise and APK.

The character is NOT finalized. Extraction is not animation approval.
