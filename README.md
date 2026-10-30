# REKEY

[![Android Release CI](https://github.com/lxzrvi/REKEY/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/lxzrvi/REKEY/actions/workflows/android-ci.yml?query=branch%3Amain)

An Android app built with Kotlin and Jetpack Compose.

**Current stage: foundation only.** The product brief has not been provided yet. The current screen demonstrates the design system and native motion; it does not implement assumed product features.

## Build policy

- **APK builds happen on GitHub Actions only.** The shared workspace contains source, not generated APKs or deliverable reports.
- **The only uploaded artifact is the release APK:** `app-release-unsigned.apk`.
- No debug APK, report archive, mapping file, screenshot or other deliverable is uploaded.
- Emulator checks use temporary test builds on the GitHub runner; results remain in job logs and are not uploaded as files.
- No production keystore is configured. The release APK is unsigned, not Play Store-ready, and cannot be installed as a normal signed release.

## Foundation

- Kotlin through Android Gradle Plugin 9.4.1's built-in Kotlin support.
- Jetpack Compose with the stable `2026.09.00` BOM and Material 3.
- Compile/target SDK 37.0; minimum SDK 26 (Android 8.0).
- Offline Poppins Thin, Regular, Medium and Bold, with the SIL Open Font License.
- Monochrome light/dark themes, adaptive black launcher icon and Android 12+ splash.
- White wordmark on a dark surface. **Holiday by Letterhend Studio is selected but not yet licensed/bundled; the visible wordmark is a marked Poppins fallback.**
- Native Compose animation with system animator-duration scaling.
- Release-variant JVM tests, lint and instrumented Compose interaction/recreation checks.

## GitHub Actions

`.github/workflows/android-ci.yml` runs on relevant branch pushes, pull requests to `main`, and manual dispatch.

The release job runs `:app:assembleRelease :app:testReleaseUnitTest :app:lintRelease`, then uploads only the unsigned release APK. The runtime job checks launching and UI behavior on an API 35 emulator without uploading test files.

The foundation previously passed its complete build and emulator workflow: [baseline run 40991187588](https://github.com/lxzrvi/REKEY/actions/runs/40991187588). The badge links to the latest `main` verification, including the release-only artifact policy. See [docs/STATUS.md](docs/STATUS.md) for historical evidence and limitations.

## Source structure

```text
app/src/main/java/io/github/lxzrvi/rekey/
  design/                 Platform-independent design decisions
  ui/brand/               Brand typography integration point
  ui/components/          Reusable brand components
  ui/theme/               Poppins, colors and Material theme
  ui/foundation/          Temporary foundation screen
  MainActivity.kt         Edge-to-edge entry point
app/src/main/res/         Fonts, strings, themes and vector icons
app/src/test/             JVM tests
app/src/androidTest/      Compose UI tests
.github/workflows/        GitHub-only release build
scripts/                  Ephemeral emulator checks
```

Use an Android Studio release supporting AGP 9.4.1 for source editing. Do not generate APK deliverables in the shared workspace; use the GitHub workflow.

## Docs

- [Branding and font licensing](docs/BRANDING.md)
- [Build and CI details](docs/BUILD_AND_CI.md)
- [Architecture decisions](docs/ARCHITECTURE.md)
- [Change log](CHANGELOG.md)
- [Verification status](docs/STATUS.md)

The application ID `io.github.lxzrvi.rekey` is provisional and should be confirmed before the first store release. No application-code license has been chosen without the owner's decision; Poppins has its own included OFL.
