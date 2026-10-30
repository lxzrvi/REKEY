# REKEY

An Android app built with Kotlin and Jetpack Compose.

**Current stage: foundation only.** The product brief has not been provided yet. The current screen demonstrates the design system and native motion; it does not pretend to implement the app's future features.

## Foundation

- Kotlin through Android Gradle Plugin 9.4.1's built-in Kotlin support.
- Jetpack Compose with the stable `2026.09.00` BOM and Material 3.
- Compile/target SDK 37; minimum SDK 26 (Android 8.0).
- Offline Poppins Thin, Regular, Medium and Bold, with the SIL Open Font License.
- Monochrome light/dark themes, adaptive black launcher icon and Android 12+ splash.
- White wordmark on a dark surface. **Holiday by Letterhend Studio is selected but not yet licensed/bundled; the visible wordmark is a marked Poppins fallback.**
- Native Compose animation, with system animator-duration scaling.
- JVM contract tests and instrumented Compose interaction/recreation tests.
- GitHub Actions: debug APK, unit tests, lint, unsigned release build and API 35 emulator smoke tests.

## Open and build

Use an Android Studio release that supports AGP 9.4.1, with JDK 21 recommended for the command line. Install Android SDK Platform 37 and Build Tools 36.0.0. `local.properties` is machine-specific and intentionally not committed.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease
```

Windows: use `gradlew.bat` with the same tasks.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

The release build verifies shrinking but is **unsigned**, not a Play Store release. No signing key or credential belongs in Git.

With an emulator/device connected:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## GitHub Actions

The workflow is in `.github/workflows/android-ci.yml`. It runs on relevant branch pushes, pull requests to `main`, and manual dispatch. It uploads the debug APK, build reports, R8 mapping and emulator evidence as workflow artifacts. An APK artifact is not proof that tests passed; check the complete workflow result.

**Remote verification status:** pending the first push. No GitHub build or emulator success is claimed yet. See [docs/STATUS.md](docs/STATUS.md) for the current evidence and outstanding work.

## Structure

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
.github/workflows/        Android CI
scripts/                  Emulator smoke check
```

## Docs

- [Branding and font licensing](docs/BRANDING.md)
- [Build and CI details](docs/BUILD_AND_CI.md)
- [Architecture decisions](docs/ARCHITECTURE.md)
- [Change log](CHANGELOG.md)
- [Verification status](docs/STATUS.md)

The application ID `io.github.lxzrvi.rekey` is provisional and should be confirmed before the first store release. No application-code license has been chosen without the owner's decision; Poppins has its own included OFL.
