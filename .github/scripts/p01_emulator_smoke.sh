#!/usr/bin/env bash
# Invoked as a single file because android-emulator-runner runs script LINES
# independently with sh -c: shell functions / traps split across lines fail.
set -Eeuo pipefail
DIR="android/app/build/p01-test-mobile"
mkdir -p "$DIR"
capture() {
  local ec=$?
  set +e
  echo "SMOKE EXIT CODE: $ec"
  timeout 25s adb logcat -d -v threadtime > "$DIR/startup-logcat.txt" 2>&1
  timeout 20s adb shell dumpsys activity activities > "$DIR/activity-state.txt" 2>&1
  timeout 20s adb shell dumpsys window windows > "$DIR/window-state.txt" 2>&1
  timeout 20s adb shell dumpsys meminfo com.gamelutagpt.p01fix > "$DIR/memory.txt" 2>&1
  timeout 20s adb exec-out screencap -p > p01-motor-v2-screen.png
  timeout 25s adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1
  timeout 20s adb shell cat /sdcard/window.xml > p01-motor-v2-ui.xml 2>/dev/null
  grep -E -A 38 -B 4 'FATAL EXCEPTION|Process: com.gamelutagpt.p01fix|OutOfMemoryError|ANR in com.gamelutagpt.p01fix|am_crash' "$DIR/startup-logcat.txt" | tail -n 125 || true
  echo "=== Focused activity ==="
  grep -E 'mCurrentFocus|mFocusedApp|mResumedActivity|topResumedActivity|ResumedActivity' "$DIR/window-state.txt" "$DIR/activity-state.txt" | tail -n 12 || true
  return 0
}
trap capture EXIT

APK="android/app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$APK"
PKG="com.gamelutagpt.p01fix"
adb logcat -c
echo "Launching P01, waiting for onCreate and first game frames."
timeout 60s adb shell am start -W -n ${PKG}/com.gamelutagpt.MainActivity
sleep 12
pid="$(timeout 20s adb shell pidof "$PKG" | tr -d '\r' )"
if [[ ! "$pid" =~ ^[0-9]+ ]]; then
  echo "FAIL: game process disappeared after first launch"
  exit 81
fi
echo "Game process still running: $pid"
window="$(timeout 25s adb shell dumpsys window)"
if ! grep -Eq 'mCurrentFocus.*${PKG}|mFocusedApp.*${PKG}' <<< "$window"; then
  echo "FAIL: Android did not focus the game activity."
  echo "$window" | grep -E 'mCurrentFocus|mFocusedApp' | tail -n 10 || true
  exit 82
fi
logs="$(timeout 25s adb logcat -d -v brief)"
if grep -Eiq 'FATAL EXCEPTION|Process: com.gamelutagpt.p01fix|ANR in com.gamelutagpt.p01fix' <<< "$logs"; then
  echo "FAIL: game threw an exception or stopped responding."
  exit 83
fi
# Screenshot is captured in trap, so only sample after it has settled.
timeout 20s adb exec-out screencap -p > p01-motor-v2-screen.png
python3 - <<'PY'
from PIL import Image
import numpy as np
a=np.asarray(Image.open("p01-motor-v2-screen.png").convert("RGB"))
if a.shape[0] > 110 and a.shape[1] > 110:
    a=a[50:-50,50:-50,:]
fraction=float(np.mean(a.max(2)-a.min(2)>48))
print("Image non-gray pixel fraction",round(fraction,3))
assert fraction>.06, "Likely frozen system screen; game not visible"
PY
echo "SMOKE PASS: process alive, game focused, scene visible."
