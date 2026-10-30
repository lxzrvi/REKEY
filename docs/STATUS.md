# Verification status

Updated: 2026-10-30 (Asia/Kolkata)

## Scope

The Android foundation is implemented and remotely verified. This is not a completed product: the owner has not supplied the app concept or feature requirements yet.

## Verified code baseline

- Setup branch: `setup/android-foundation`
- Verified code commit: `71bd8be674a45576295679d7b55fb05552605e31`
- Complete workflow: https://github.com/lxzrvi/REKEY/actions/runs/40991187588
- Workflow conclusion: **success**, first attempt
- Completed: 2026-10-30 02:30:57 UTC / 08:00:57 IST
- Build job: https://github.com/lxzrvi/REKEY/actions/runs/40991187588/job/123853721603
- Emulator job: https://github.com/lxzrvi/REKEY/actions/runs/40991187588/job/123854973578

The verified code was promoted to `main` only after both jobs passed. Documentation-only commits may advance `main`; check the live main workflow for the latest repository-head result:

https://github.com/lxzrvi/REKEY/actions/workflows/android-ci.yml?query=branch%3Amain

## Actual results

| Check | Result |
| --- | --- |
| Gradle wrapper validation | Passed |
| Debug Kotlin compilation and APK packaging | Passed |
| JVM design-contract tests | 4 tests, 0 failures, 0 errors, 0 skipped |
| Android lint | 0 errors; 3 non-blocking version-update notices |
| Unsigned release compilation, R8 and resource shrinking | Passed |
| API 35 x86_64 emulator connected Compose tests | 4 tests, 0 failures, 0 errors, 0 skipped |
| Explicit APK install/launch and process check | Passed; observed app PID 4168 |
| Emulator crash buffer | Empty at capture |
| Emulator screenshot | Captured and visually inspected: white wordmark, black icon and Poppins rendering visible |
| Physical-device review | Not performed |
| Licensed Holiday integration | Pending licensed files and app-embedding permission |

The three remaining lint notices report newer Gradle, AGP and Kotlin versions. They are not build errors. The project retains its tested, mutually compatible pinned versions rather than upgrading solely to silence update notices.

## CI failures found and fixed

1. **Run 40990444884**, commit `045e8911cdfb893ad6c1282d2905643a1ab17c83`: SDK setup failed because Google publishes `platforms;android-37.0`, not `platforms;android-37`.
   - Fixed the CI package identifier and declared `compileSdkMinor = 0`.
2. **Run 40990673545**, commit `62fa3baf70d48eb5d970f0368f6736c917042ee7`: compilation and the four JVM tests passed, but lint rejected the API-27-only `windowLightNavigationBar` attribute in the API-26 base theme.
   - Removed that base-theme attribute; the version-aware edge-to-edge controller handles system-bar appearance.
   - Added explicit disabled cloud-backup/device-transfer rules.
   - Consolidated adaptive/monochrome icon resources for the minimum SDK.
   - Removed deprecated AGP 9 lint-report toggles.
3. **Run 40991187588**, commit `71bd8be674a45576295679d7b55fb05552605e31`: complete build and emulator workflow passed.
4. **Run 41008990061**, commit `255bd2efff869308e4272444524d487b4b3dab3a`: the first release-only run stopped during task selection because AGP 9 does not create `testReleaseUnitTest` by default. No artifact was uploaded.
   - Explicitly set `android.onlyEnableUnitTestForTheTestedBuildType=false` so release JVM tests remain enabled alongside debug emulator checks. The fix requires a fresh successful workflow before being considered verified.

No tests were disabled, no lint baseline was introduced, and no failing check was marked successful manually.

## Current artifact policy

The owner clarified that APK builds must run only on GitHub and only a release APK may be delivered. The workflow now runs release-variant unit tests and lint, and uploads exactly one file: `app-release-unsigned.apk`.

No debug APK, report archive, mapping, screenshot or other deliverable is uploaded. Emulator test builds and outputs are temporary on the GitHub runner; job results remain in Actions logs. Previously generated local deliverables and debug/report artifacts are removed under this correction. Historical test results above remain a record of what actually ran, not a promise that old artifacts remain available.

The latest main verification is available from the repository Actions page and README badge. The earlier final main baseline also passed: https://github.com/lxzrvi/REKEY/actions/runs/40992545441 (commit `4e814a573f6f612e3f673f527fdd2febf76d5ddc`).

The release APK is unsigned until secure production signing is configured; it is not a Play Store-ready or normally installable signed release.

## Limits and next gates

- Emulator verification used API 35 with system animations disabled for deterministic tests. It does not establish performance on every supported device or subjective animation smoothness.
- The minimum supported version remains API 26; broader device/API coverage should expand with actual product features.
- Holiday by Letterhend Studio is selected, but the current wordmark is explicitly marked Poppins fallback. No font purchase or commercial license is claimed.
- Add product features only after the owner's brief, then extend feature tests, device coverage and production signing as appropriate.
- No GitHub credential is stored in the repository or CI configuration. A credential temporarily used for the authorized push must be revoked by its owner because it was pasted into chat.
