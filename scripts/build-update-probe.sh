#!/usr/bin/env bash
# Compile a higher-version APK with the SAME restored key. Keep it out of deliverables.
set -euo pipefail
MAIN=app/build/outputs/apk/debug/app-debug.apk
SAVED=$(mktemp "${RUNNER_TEMP:?}/spaces-main.XXXXXX.apk")
cp "$MAIN" "$SAVED"
restore_main() {
  local status=$?
  trap - EXIT
  cp "$SAVED" "$MAIN"
  rm -f "$SAVED"
  exit "$status"
}
trap restore_main EXIT
python3 scripts/ci-build.py -PspacesUpdateProbe=true assembleDebug
mkdir -p app/build/outputs/update-probe
cp "$MAIN" app/build/outputs/update-probe/app-update-probe.apk
TOOLS="$(find "$ANDROID_HOME/build-tools" -mindepth 2 -maxdepth 2 -name aapt | sort -V | tail -1 | xargs dirname)"
old_code=$("$TOOLS/aapt" dump badging "$SAVED" | sed -n "s/^package: .*versionCode='\([0-9]*\)'.*/\1/p")
new_code=$("$TOOLS/aapt" dump badging "$MAIN" | sed -n "s/^package: .*versionCode='\([0-9]*\)'.*/\1/p")
[[ "$old_code" =~ ^[0-9]+$ ]] && [[ "$new_code" =~ ^[0-9]+$ ]] && ((new_code==old_code+1)) || { echo '::error::Update probe must increase versionCode by exactly one'; exit 1; }
