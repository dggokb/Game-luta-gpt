#!/usr/bin/env bash
# Single-process emulator smoke test. Intentionally run with 'bash script.sh':
# android-emulator-runner launches each inline command in a separate /bin/sh.
set -Eeuo pipefail
PKG="com.gamelutagpt.p01fix"
ACTIVITY="com.gamelutagpt.MainActivity"
DIR="android/app/build/p01-test-mobile"
APK="android/app/build/outputs/apk/debug/app-debug.apk"
mkdir -p "$DIR"
capture() {
  local ec=$?
  set +e
  echo "SMOKE EXIT CODE: $ec"
  timeout 25s adb logcat -d -v threadtime > "$DIR/startup-logcat.txt" 2>&1
  timeout 20s adb shell dumpsys activity activities > "$DIR/activity-state.txt" 2>&1
  timeout 20s adb shell dumpsys window windows > "$DIR/window-state.txt" 2>&1
  timeout 20s adb shell dumpsys meminfo "$PKG" > "$DIR/memory.txt" 2>&1
  timeout 20s adb exec-out screencap -p > p01-motor-v2-screen.png
  timeout 25s adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1
  timeout 20s adb shell cat /sdcard/window.xml > p01-motor-v2-ui.xml 2>/dev/null
  grep -E -A 25 -B 4 'FATAL EXCEPTION|OutOfMemoryError|am_crash|ANR in com.gamelutagpt' "$DIR/startup-logcat.txt" | tail -n 120 || true
}
trap capture EXIT

test -s "$APK"
adb install -r "$APK"
adb logcat -c
echo "SMOKE start: $PKG/$ACTIVITY"
timeout 65s adb shell am start -W -n "$PKG/$ACTIVITY"
sleep 12

pid="$(timeout 20s adb shell pidof "$PKG" | tr -d '\r')"
if [[ ! "$pid" =~ ^[0-9]+ ]]; then
  echo "FAIL: application process exited during startup"
  exit 81
fi
echo "Game process still running: $pid"

activity="$(timeout 25s adb shell dumpsys activity activities)"
resumed="$(grep -E 'ResumedActivity=|ResumedActivity:|topResumedActivity=|mFocusedApp=' <<< "$activity" || true)"
if ! grep -Fq "$PKG" <<< "$resumed"; then
  echo "FAIL: game activity not resumed: $resumed"
  exit 82
fi
echo "Resumed game: $resumed"

# Android emulator's one-time "Viewing full screen / GOT IT" OS overlay.
adb shell input tap 1080 454 || true
sleep 4

logs="$(timeout 25s adb logcat -d -v brief)"
if grep -Eiq 'FATAL EXCEPTION|OutOfMemoryError.*com.gamelutagpt|ANR in com.gamelutagpt' <<< "$logs"; then
  echo "FAIL: Android reported an exception or ANR"
  exit 83
fi
timeout 20s adb exec-out screencap -p > p01-motor-v2-screen.png
python3 - <<'PY'
from PIL import Image
import numpy as np
a=np.asarray(Image.open("p01-motor-v2-screen.png").convert("RGB"))
assert a.shape[0]>100 and a.shape[1]>100,"Screen capture malformed"
a=a[50:-50,50:-50,:]
fraction=float(np.mean(a.max(2)-a.min(2)>48))
print("Game image colored fraction",round(fraction,3))
assert fraction>.06, "Game screen not visible or system is frozen"
PY
echo "SMOKE PASS: game started, remained resumed, and rendered a visible scene."
