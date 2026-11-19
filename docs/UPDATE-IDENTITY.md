# Signing and one-time reset — PRIVATE SECRET SETUP REQUIRED

## User decision and fixed identity

The user explicitly selected **start fresh without old app data**, with **phone only / no PC**, after being told that the old signing key could not be restored. This authorizes preparing a one-time reinstall route, not deleting anything remotely or losing an active session's recovery journal. No app has been uninstalled or data erased by this session.

Keep `id.veldafelix.spaces.preview2` for **both debug and release**. Do not create preview3 or another production package. 0.1.2/versionCode 3 is the exact authorized reset version; this does not authorize arbitrary future key changes. The original `.preview` installation remains separate and is not automatically merged.

## New key is actually backed up

New pinned public certificate SHA-256:

```text
f53383afe91aa4524dc2b58245bdf66dd4a0ce52ddbaffd96834de9ed3094287
```

A new RSA-3072 PKCS12 key was generated once, with random credentials. Its private JSON signing bundle was uploaded to the user's **private Google Drive**, not the public repository, a release, an Actions artifact or chat. Upload size/checksum matched the local file; Drive permissions were checked and contained **only the owner**. A real local restore verified that the bundle contains a private-key entry with the pinned certificate, and creates a mode-0600 key file. Private material lives only in the excluded `.local` workspace directory and private Drive backup; only public policy is committed in `config/preview-identity.json`.

This is a backup of the **signing key**, not a backup of the old app's collection/profiles/history. User data will start empty after the approved reinstall.

GitHub's Actions Secrets public-key endpoint returned HTTP 403 (`Resource not accessible by integration`). The agent cannot write the repository secret with the current connection. Reconnecting GitHub in Arena may resolve the missing permission; alternatively the owner can add the single secret from a phone browser. Never ask for GitHub passwords/tokens/2FA or paste the private bundle into chat.

## One phone-only setup step before builds can continue

1. Open the private Drive file **PRIVATE — spaces-mobile signing backup — keep safe.json**. Keep it private; do not publish/share it.
2. Open the repository's **Settings → Secrets and variables → Actions → New repository secret**.
3. Name: `SPACES_PREVIEW_SIGNING_BUNDLE`. Value: the **entire JSON file content**, not its Drive link. Save it privately on GitHub; do not send the value in chat.
4. Tell the agent setup is done. Run full Android CI, not just the read-only audit. If the bundle does not match the pinned public certificate, signing must stay blocked.

Do **not** uninstall the current phone apps yet. The new APK is not built/published until real signed installation and Android tests pass. See [phone reset checklist](PHONE-RESET.md).

## Build and publication guards

- No generated-key fallback and no dependence on a CI cache. A missing/malformed/wrong secret fails before signed build. Local builds without credentials produce unsigned output instead of a new valid identity.
- Verify the decoded PKCS12 private-key entry using keytool, compare its certificate with the public pin, restrict temporary permissions, and remove the restored key at job exit. No private key is uploaded as an artifact.
- Compare APK package, certificate and version against public policy. A different certificate is permitted **only** for versionCode 3 with the recorded reset approval, matching the pinned old baseline and pinned new key. The output must say **reset**, never claim an in-place update of 0.1.1.
- The CI-only installation test requires GitHub Actions and an actual emulator. Reset mode proves old-key `adb install -r` is rejected and the old data remains, then uninstalls **on the disposable emulator only**. It tests clean new installation, followed by a same-key higher-version 3→4 probe retaining private file/preferences and no extra package. The probe APK is never a publication deliverable.
- Both Android suites, source checks, pinned signing identity and real installation checks must pass before preparing a draft. No retry after a failed suite. Manual publication only.
- Debug/release share package and key. Manual production release also requires a successful full push-triggered Android run for the exact source and creates only a draft.
- After the first new-key APK is published, promote its release/code/certificate to `baseline` in the public policy and remove `one_time_reset`. Future versions must update in place using this same backed-up key; no automatic new reset exception.

## Why the old direct update was impossible (historical evidence)

The old script checked valid signatures independently, not certificate equality. That was inadequate. The read-only audit [44439015416](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44439015416) correctly blocked the previous ephemeral-key candidate from [44438095141](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44438095141):

| APK | Public signing certificate SHA-256 |
| --- | --- |
| Old-key 0.1.1 baseline | `f2bbc935f0290df36c3f1dbb44ae4579c2bb64b22e406e3b67b4159dcba6d7d2` |
| Rejected ephemeral 0.1.2 candidate | `4572df6410f8fd433b2afd9e8b98f0e031091b63db9f722b31f4f4391868f758` |

No matching original private key was restored; the cache listing was empty. Listing secrets was forbidden, which does not prove no separate backup exists. An APK's public certificate cannot recover its private key, and repeating an alias/password does not regenerate it. The newly backed-up key above is a **different key**, not a recovered old key; hence the explicitly approved one-time reset.
