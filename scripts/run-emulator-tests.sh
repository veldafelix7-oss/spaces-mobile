#!/usr/bin/env bash
# Two consecutive suites; never retry away an input failure.
set -euo pipefail
mkdir -p emulator-results
collect_results() {
  local status=$?
  trap - EXIT
  timeout 15s adb pull /sdcard/Android/data/id.veldafelix.spaces.preview2/files/screenshots emulator-results/ || true
  timeout 10s adb logcat -d -s AndroidRuntime > emulator-results/crashes.txt || true
  exit "$status"
}
trap collect_results EXIT
for pass in 1 2; do
  echo "::notice title=Emulator stage::Consecutive instrumentation suite ${pass}/2"
  result="emulator-results/instrumentation-${pass}.txt"
  timeout 240s adb shell am instrument -w -r id.veldafelix.spaces.preview2.test/androidx.test.runner.AndroidJUnitRunner | tee "$result"
  python3 scripts/check-instrumentation.py "$result"
done
