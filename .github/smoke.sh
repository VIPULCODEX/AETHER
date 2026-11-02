#!/usr/bin/env bash
# Boots the freshly built APK on an emulator and exercises it with random input, then fails on any crash.
# Catches what unit tests can't: startup crashes, dependency-injection mistakes, database errors, screens that
# throw when opened. Runs inside reactivecircus/android-emulator-runner.
set -u
APK="${1:?usage: smoke.sh path/to/app.apk}"
PKG=com.aether.android

adb install -r "$APK"
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true
adb logcat -c

# Cold start, then 800 pseudo-random touches/swipes across the app (fixed seed = reproducible).
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
sleep 5
adb shell monkey -p "$PKG" --throttle 120 --pct-syskeys 0 --pct-appswitch 0 -s 42 -v 800 > monkey.txt 2>&1
MONKEY_EXIT=$?

adb logcat -d > logcat.txt
grep -E "FATAL EXCEPTION|ANR in|Process: $PKG" logcat.txt > crashes.txt || true

echo "monkey exit=$MONKEY_EXIT"
if grep -q "CRASH" monkey.txt || [ -s crashes.txt ]; then
  echo "=== SMOKE TEST FAILED ==="
  grep -n "CRASH" monkey.txt | head -5
  grep -B2 -A25 "FATAL EXCEPTION" logcat.txt | head -80
  exit 1
fi
echo "=== SMOKE TEST PASSED: no crashes or ANRs in 800 random events ==="
