# Verification status

Updated: 2026-10-30 (Asia/Kolkata)

## Current policy

- All work is kept on `main`; other branches are removed.
- GitHub Actions builds only the unsigned release APK.
- The only uploaded artifact is `app-release-unsigned.apk`.
- No unit-test, lint or emulator job runs in the release workflow.
- No APK, report, mapping or screenshot is generated in the shared workspace.

## Latest verified release-only baseline

- Commit: `51543a6b02594bf3d4bb88f319daccac89c9f3ae`
- Run: https://github.com/lxzrvi/REKEY/actions/runs/41009461508
- Result: success; Gradle reported `BUILD SUCCESSFUL` and uploaded `app-release-unsigned.apk`.

The 0.3.0 increment (Haze 2, release-only CI and UI polish) is verified by the latest `main` workflow. Check the live badge/Actions page for the current head result:

https://github.com/lxzrvi/REKEY/actions/workflows/android-ci.yml?query=branch%3Amain

## Historical notes

Earlier foundation increments used unit tests, lint and an API 35 emulator. Those checks are no longer part of the release workflow by owner request. Historical runs remain in GitHub Actions, but they do not describe the current CI contract.

## Limits

- The release APK is unsigned and not Play Store-ready.
- Physical-device verification has not been performed.
- Haze blur quality/performance on API 31/32 and lower-end devices still needs device review.
- The product brief is still pending; demo cards are not product features.
- A GitHub credential temporarily used for authorized pushes was pasted into chat and must be revoked by its owner.
