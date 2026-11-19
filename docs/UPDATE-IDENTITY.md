# Installed-app update identity — BLOCKED

## Selected identity

Continue the existing 0.1.1 installation: `id.veldafelix.spaces.preview2`.
Do not make `preview3`, change application ID to escape a certificate mismatch, or tell the user to uninstall/clear data to conceal a failed update. The earlier `.preview` app remains separate; these installed apps cannot be automatically merged by renaming an APK.

Baseline: public release `preview-0.1.1-82e5e54`, versionCode 2.
Current source: 0.1.2, versionCode 3, **same debug application ID**, not distributed.

## Evidence

The older script verified each APK's signature independently and the new package name. That was insufficient: two valid signatures can belong to different keys. The old side-by-side marker test also did not prove same-package update compatibility. No previous claim of permanent preview signing should be inferred from those checks.

A real read-only audit [44439015416](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44439015416), using the APK already compiled in [44438095141](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44438095141), compared public signing certificate SHA-256 fingerprints:

| APK | Certificate SHA-256 (not the APK file checksum) |
| --- | --- |
| Installed-version baseline 0.1.1 | `f2bbc935f0290df36c3f1dbb44ae4579c2bb64b22e406e3b67b4159dcba6d7d2` |
| Compiled 0.1.2 candidate | `4572df6410f8fd433b2afd9e8b98f0e031091b63db9f722b31f4f4391868f758` |

The audit correctly failed with **Update blocked**. No new APK/release was published. The actual replacement/data-retention emulator job was **skipped**, not passed. The Actions cache API returned zero caches at inspection. The secrets listing endpoint returned HTTP 403; this does not prove that no private backup exists, but no matching key was restored by the tested build.

## Guards now in place

- Compare the candidate certificate to the installed-version baseline, not merely signature validity.
- Require the same `preview2` package and a strictly increasing versionCode.
- Accept standard, SDK-scoped and V2 apksigner output; reject missing/multiple distinct certificate digests.
- Do not automatically cache a newly generated, unverified debug key. Caches are not a permanent backup anyway.
- Only after identity succeeds, run a disposable-emulator `adb install -r` test with private file/preferences markers and an unchanged package list. No uninstall, downgrade, certificate bypass or data clearing in that test.
- Require build, both Android suites, identity and actual replacement/data checks before preparing even a draft.
- Apply the same certificate/package guard to the manually signed release workflow; it cannot silently publish a third package under the production name.
- Optional fast audit: `gh workflow run android.yml --ref arena/7c652660-spaces-mobile -f audit_build_run_id=<existing-build-run-id>`. This reads an existing compiled artifact; all build/publication jobs are skipped. An incompatible APK must produce a failed audit.

## Required to unblock a direct update

1. Recover the **original private key corresponding to the baseline certificate** from a trusted backup. The APK contains a public certificate, not a recoverable copy of the private key. Generating another debug key or using the same alias/password will not reproduce it.
2. Store that original key securely as the preview signing secret and keep a separate private backup. Do not commit it, upload it as an artifact, or request passwords/tokens in chat.
3. Rebuild and require the real identity plus `adb install -r` data checks to pass, then repeat the Android suites before manual publication.

If the original key is genuinely lost, an in-place update of the installed 0.1.1 is impossible under normal Android signature checks. A user-approved export/reinstall/import plan would be a separate, destructive-risk decision, requiring tested backups first. No such migration, uninstall or data deletion is implemented or authorized by this change. Do not declare this issue finished or deliver the incompatible candidate.
