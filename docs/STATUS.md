# Verification status

Updated: 2026-10-30 (Asia/Kolkata)

## Current policy

- All work is kept on `main`; other branches are removed.
- GitHub Actions builds only the unsigned release APK.
- The only uploaded artifact is `app-release-unsigned.apk`.
- No unit-test, lint or emulator job runs in the release workflow.
- No APK, report, mapping or screenshot is generated in the shared workspace.

## Latest verified release-only build

- Version: 0.4.0 (versionCode 4)
- Commit: `eda5cab`
- Run: https://github.com/lxzrvi/REKEY/actions/runs/41053368289
- Result: success; single `Build release APK` job.
- Gradle build step: 46 seconds. End-to-end GitHub job: 75 seconds including setup and upload.
- Artifact: `app-release-unsigned.apk`, id `12974919722`, 23,863,354 bytes.
- SHA-256: `9dbbfde521ebfb2b3170abedcd995e1c0fbcc376a30a0f9f8d69b7fa7543b824`.

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
