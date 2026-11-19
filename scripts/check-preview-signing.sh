#!/usr/bin/env bash
# Fail closed: a valid signature alone does NOT prove an APK updates the installed app.
set -euo pipefail
BASELINE=preview-0.1.1-82e5e54
PACKAGE=id.veldafelix.spaces.preview2
DIRECTORY="${RUNNER_TEMP:?}/spaces-update-baseline"
mkdir -p "$DIRECTORY"
gh release download "$BASELINE" --repo "$GITHUB_REPOSITORY" --pattern spaces-mobile-preview.apk --dir "$DIRECTORY" --clobber
TOOLS="$(find "$ANDROID_HOME/build-tools" -mindepth 2 -maxdepth 2 -name apksigner | sort -V | tail -1 | xargs dirname)"
OLD="$DIRECTORY/spaces-mobile-preview.apk"
NEW="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
old_info=$("$TOOLS/apksigner" verify --print-certs "$OLD")
new_info=$("$TOOLS/apksigner" verify --print-certs "$NEW")
old_cert=$(printf '%s\n' "$old_info" | sed -n 's/^Signer #[0-9]* certificate SHA-256 digest: //p')
new_cert=$(printf '%s\n' "$new_info" | sed -n 's/^Signer #[0-9]* certificate SHA-256 digest: //p')
[[ "$old_cert" =~ ^[a-fA-F0-9]{64}$ ]] && [[ "$new_cert" =~ ^[a-fA-F0-9]{64}$ ]] || { echo '::error::Expected one SHA-256 signer per APK'; exit 1; }
echo "::notice title=Installed preview certificate::$old_cert"
echo "::notice title=Candidate certificate::$new_cert"
if [ "$old_cert" != "$new_cert" ]; then
  echo '::error title=Update blocked::Signing certificate differs from the installed 0.1.1 preview. Restore its original private key. Do not publish, change the package ID, or ask the user to uninstall/clear data.'
  exit 1
fi
old_badging=$("$TOOLS/aapt" dump badging "$OLD")
new_badging=$("$TOOLS/aapt" dump badging "$NEW")
for badging in "$old_badging" "$new_badging"; do
  actual=$(printf '%s\n' "$badging" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
  [ "$actual" = "$PACKAGE" ] || { echo '::error::Application ID changed; no new installation identity is allowed'; exit 1; }
done
old_code=$(printf '%s\n' "$old_badging" | sed -n "s/^package: .*versionCode='\([0-9]*\)'.*/\1/p")
new_code=$(printf '%s\n' "$new_badging" | sed -n "s/^package: .*versionCode='\([0-9]*\)'.*/\1/p")
[[ "$old_code" =~ ^[0-9]+$ ]] && [[ "$new_code" =~ ^[0-9]+$ ]] && ((new_code>old_code)) || { echo '::error::Update versionCode must increase'; exit 1; }
echo '::notice title=Preview update identity::Same preview2 package, same certificate, increasing versionCode. Emulator replacement/data checks must also pass before publication.'
