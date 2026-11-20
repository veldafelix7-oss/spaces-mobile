#!/usr/bin/env bash
# ONLY on the disposable CI emulator. Reset mode is exact-version approved, never a bypass.
set -euo pipefail
PACKAGE=id.veldafelix.spaces.preview2
MODE="${INSTALL_MODE:?identity job must supply update or reset}"
[[ "$MODE" = update || "$MODE" = reset ]] || { echo 'Invalid installation mode'; exit 1; }
[[ "${CI:-}" = true && "${GITHUB_ACTIONS:-}" = true ]] || { echo 'This installation test is CI-emulator-only'; exit 1; }
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1 ]] || { echo 'Refusing installation/reset outside an emulator'; exit 1; }
mkdir -p emulator-results
adb install baseline/spaces-mobile-preview.apk
adb shell am start -W -n "$PACKAGE/id.veldafelix.spaces.MainActivity"
adb shell am force-stop "$PACKAGE"
adb install apks/androidTest/debug/app-debug-androidTest.apk
seed() {
  adb shell run-as "$PACKAGE" mkdir -p files shared_prefs
  printf '%s\n' retained-file | adb shell run-as "$PACKAGE" tee files/update-check.txt > /dev/null
  printf '%s\n' '<?xml version="1.0" encoding="utf-8"?><map><string name="check">retained-pref</string></map>' | adb shell run-as "$PACKAGE" tee shared_prefs/update-check.xml > /dev/null
  # Real 0.1.2 schema, seeded while the published baseline is installed/stopped.
  printf '%s\n' '<?xml version="1.0" encoding="utf-8"?><map><string name="history">[{"id":"upgrade-fixture","game":"com.tencent.igfit","start":1000,"reason":"manual","samples":[{"at":1000,"temp":35.5}]}]</string><boolean name="active" value="true"/><string name="active_game">com.tencent.igfit</string><long name="active_start" value="1000"/><string name="draft_samples">[{"at":2000,"temp":35.5}]</string><string name="journal">[]</string><string name="com.tencent.igfit.mode">eco</string></map>' | adb shell run-as "$PACKAGE" tee shared_prefs/spaces.xml > /dev/null
}
verify_data() {
  adb shell run-as "$PACKAGE" cat files/update-check.txt | tr -d '\r' | tee emulator-results/update-file.txt
  grep -qx retained-file emulator-results/update-file.txt
  adb shell run-as "$PACKAGE" cat shared_prefs/update-check.xml | tr -d '\r' | tee emulator-results/update-pref.txt
  grep -q retained-pref emulator-results/update-pref.txt
}
packages() { adb shell pm list packages | tr -d '\r' | grep '^package:id\.veldafelix\.spaces' | sort; }
seed
packages > emulator-results/packages-before.txt
if [ "$MODE" = reset ]; then
  # Prove Android refuses the old-key update and leaves its data intact.
  if adb install -r apks/debug/app-debug.apk > emulator-results/old-key-replacement.txt 2>&1; then
    echo '::error::Expected old-key update rejection; reset policy does not match APKs'; exit 1
  fi
  grep -q INSTALL_FAILED_UPDATE_INCOMPATIBLE emulator-results/old-key-replacement.txt
  verify_data
  # Explicitly approved destructive step, confined to this disposable emulator.
  # Nothing in this script runs on the user phone.
  adb uninstall "$PACKAGE"
  adb install apks/debug/app-debug.apk
  adb shell am start -W -n "$PACKAGE/id.veldafelix.spaces.MainActivity"
  adb shell am force-stop "$PACKAGE"
  if adb shell run-as "$PACKAGE" cat files/update-check.txt > /dev/null 2>&1; then
    echo '::error::Fresh reset unexpectedly retained old private data'; exit 1
  fi
  seed
else
  adb install -r apks/debug/app-debug.apk
  verify_data
fi
verify_app_storage() {
  local phase="$1"
  timeout 60s adb shell am instrument -w -r -e class id.veldafelix.spaces.UpgradeContinuityTest -e spaces_upgrade true "$PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" | tee "emulator-results/storage-$phase.txt"
  grep -q 'OK (1 test)' "emulator-results/storage-$phase.txt"
  if grep -q 'FAILURES!!!' "emulator-results/storage-$phase.txt"; then exit 1; fi
}
verify_app_storage candidate
# Prove the new private key supports a genuine higher-version update (candidate -> candidate + 1).
# The probe is CI-only and must never be published as the user APK.
adb shell am force-stop "$PACKAGE"
adb install -r probe/app-update-probe.apk
verify_data
verify_app_storage probe
packages > emulator-results/packages-after.txt
cmp emulator-results/packages-before.txt emulator-results/packages-after.txt
adb shell am start -W -n "$PACKAGE/id.veldafelix.spaces.MainActivity"
echo "::notice title=Installation verified::Mode=$MODE; same package, pinned key, higher-version replacement retained private file/preferences plus legacy history, active draft and profile across both replacements; no extra package."
