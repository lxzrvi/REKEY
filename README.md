# REKEY

[![Android Release CI](https://github.com/lxzrvi/REKEY/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/lxzrvi/REKEY/actions/workflows/android-ci.yml?query=branch%3Amain)

Kotlin/Jetpack Compose Android app. The product brief is still pending; the current screen demonstrates branding, appearance controls and demo cards only.

## Build policy

- APK builds happen on **GitHub Actions only**.
- The workflow builds **only the release APK** and uploads only `app-release-unsigned.apk`.
- No unit-test, lint or emulator job is part of the release workflow.
- No debug APK, report, mapping, screenshot or other deliverable is uploaded.
- No production keystore is configured. The APK is unsigned and not Play Store-ready.

## Current app

- `REKEY` wordmark in Allura; all other text uses Poppins. Startup shows the wordmark only, with no loader.
- Circular white logo with black REKEY text; launcher icon uses a white background and black text.
- Edge-to-edge Compose UI with safe-area handling.
- Haze 2.0.1 pure live blur, always on at a fixed 15 dp: top/bottom edge blur and full-screen blur behind the appearance drawer. No tint, noise or background color.
- Draggable card stack with depth scale/fade, tap-to-front, animated add/remove and a progress counter.
- Appearance drawer with theme, seven premium accents plus a rainbow custom swatch (color wheel + hex), text-size buttons, animations and high contrast.
- Fixed 15 dp corner radius; no blur or radius sliders.
- Local persistence for appearance and demo-card state.

## Toolchain

- Gradle 9.7.1, AGP 9.4.1, Kotlin/Compose compiler 2.4.20.
- Compose BOM 2026.09.00, Material 3, Haze 2.0.1 + haze-blur 2.0.1.
- Compile/target SDK 37.0, minimum SDK 31, Java/Kotlin bytecode 17.
- Release packaging is intentionally unminified to keep the GitHub-only release path fast.

## GitHub Actions

`.github/workflows/android-ci.yml` runs on pushes to `main` and manual dispatch. It runs `:app:assembleRelease` and uploads the unsigned release APK directly (`archive: false`). All work is kept on `main`.

## Source structure

```text
app/src/main/java/io/github/lxzrvi/rekey/
  data/                   Local appearance/card preferences
  design/                 Design contracts
  ui/app/                 App composition
  ui/blur/                Haze 2 pure blur helpers
  ui/brand/               Allura wordmark typography
  ui/components/          Wordmark and splash
  ui/home/                Header, card stack, drawer, menu morph
  ui/theme/               Poppins, colors and Material theme
  MainActivity.kt         Edge-to-edge entry point
app/src/main/res/         Fonts, strings, themes and vector icons
.github/workflows/        GitHub-only release APK build
```

## Docs

- [Branding and font licensing](docs/BRANDING.md)
- [Build and CI details](docs/BUILD_AND_CI.md)
- [Architecture decisions](docs/ARCHITECTURE.md)
- [Change log](CHANGELOG.md)
- [Verification status](docs/STATUS.md)

The application ID `io.github.lxzrvi.rekey` is provisional. No application-code license has been chosen; Allura and Poppins include their SIL OFL licenses.
