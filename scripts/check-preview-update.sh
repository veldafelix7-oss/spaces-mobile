#!/usr/bin/env bash
# Run ONLY on the disposable CI emulator, before any instrumentation clears app prefs.
set -euo pipefail
PACKAGE=id.veldafelix.spaces.preview2
mkdir -p emulator-results
adb install baseline/spaces-mobile-preview.apk
adb shell am start -W -n "$PACKAGE/id.veldafelix.spaces.MainActivity"
adb shell am force-stop "$PACKAGE"
adb shell run-as "$PACKAGE" mkdir -p files shared_prefs
adb shell "run-as $PACKAGE sh -c 'echo retained-file > files/update-check.txt'"
printf '%s\n' '<?xml version="1.0" encoding="utf-8"?><map><string name="check">retained-pref</string></map>' | adb shell run-as "$PACKAGE" tee shared_prefs/update-check.xml > /dev/null
adb shell pm list packages | tr -d '\r' | grep '^package:id\.veldafelix\.spaces' | sort > emulator-results/packages-before.txt
# No uninstall, -d downgrade, -t certificate bypass or data clearing is allowed.
adb install -r apks/debug/app-debug.apk
adb shell run-as "$PACKAGE" cat files/update-check.txt | tr -d '\r' | tee emulator-results/update-file.txt
grep -qx retained-file emulator-results/update-file.txt
adb shell run-as "$PACKAGE" cat shared_prefs/update-check.xml | tr -d '\r' | tee emulator-results/update-pref.txt
grep -q retained-pref emulator-results/update-pref.txt
adb shell pm list packages | tr -d '\r' | grep '^package:id\.veldafelix\.spaces' | sort > emulator-results/packages-after.txt
cmp emulator-results/packages-before.txt emulator-results/packages-after.txt
adb shell am start -W -n "$PACKAGE/id.veldafelix.spaces.MainActivity"
echo '::notice title=Preview replacement::adb install -r accepted; private file/preferences retained; package list unchanged.'
