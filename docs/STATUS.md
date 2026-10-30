# Verification status

Updated: 2026-10-30 (Asia/Kolkata)

## Current policy

- All work is kept on `main`; other branches are removed.
- GitHub Actions builds only the unsigned release APK.
- The only uploaded artifact is `app-release-unsigned.apk`.
- No unit-test, lint or emulator job runs in the release workflow.
- No APK, report, mapping or screenshot is generated in the shared workspace.

## Latest verified release-only build

- Version: 0.5.0 (versionCode 5)
- Commit: `acf5a3c`
- Run: https://github.com/lxzrvi/REKEY/actions/runs/41063719899
- Result: success on the first attempt; single `Build release APK` job.
- Gradle build step: 2m 36s on this run (cold dependency cache after the palette/settings change). The previous warm-cache run built in 46s.
- Artifact: `app-release-unsigned.apk`, id `12980987822`, 23,860,850 bytes.
- SHA-256: `616cf1223bedc7fabf2020fe378010e15226a0dfb7d3e001f46d67346fdd392d`.

Check the live badge/Actions page for the current head result:

https://github.com/lxzrvi/REKEY/actions/workflows/android-ci.yml?query=branch%3Amain

## Historical notes

Earlier foundation increments used unit tests, lint and an API 35 emulator. Those checks are no longer part of the release workflow by owner request. Historical runs remain in GitHub Actions, but they do not describe the current CI contract.

## Limits

- The release APK is unsigned and not Play Store-ready.
- Physical-device verification has not been performed.
- Haze blur quality/performance on API 31/32 and lower-end devices still needs device review.
- The product brief is still pending; demo cards are not product features.
- A GitHub credential temporarily used for authorized pushes was pasted into chat and must be revoked by its owner.
