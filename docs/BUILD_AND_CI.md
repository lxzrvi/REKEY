# Build and CI

## Owner's build policy

- APK builds run on **GitHub Actions only**. The workspace holds source files, not APK deliverables.
- The **only uploaded artifact is the release APK**: `app-release-unsigned.apk`.
- The release workflow does not run unit tests, lint or emulator checks.
- No debug APK, report, mapping, screenshot or other deliverable is uploaded.
- No production keystore is configured. The APK is unsigned and not Play Store-ready.

## Pinned toolchain

| Component | Version |
| --- | --- |
| Gradle | 9.7.1 |
| Android Gradle Plugin | 9.4.1 |
| Kotlin / Compose Compiler | 2.4.20 |
| Compose BOM | 2026.09.00 |
| Material 3 | 1.4.0 through the BOM |
| Haze / haze-blur | 2.0.1 |
| Activity Compose | 1.13.0 |
| AndroidX Core | 1.19.1 |
| Compile/target SDK | 37.0 |
| Minimum SDK | 31 |
| Build Tools | 36.0.0 |
| CI JDK | Temurin 21 |
| Java/Kotlin bytecode | 17 |

AGP 9 supplies built-in Kotlin; the root build overrides the Kotlin Gradle plugin to 2.4.20 without applying `org.jetbrains.kotlin.android`. The Compose Compiler plugin matches Kotlin. Google's SDK package is `platforms;android-37.0`, with `compileSdkMinor = 0`.

The Gradle wrapper and distribution have published checksums. CI validates the wrapper. GitHub Actions are pinned to commit SHAs.

## Release job

The single job runs:

```bash
./gradlew --no-daemon --stacktrace --max-workers=4 :app:assembleRelease
```

- Release compilation and packaging must succeed.
- R8 minification and resource shrinking are disabled to keep the release path fast.
- Only `app/build/outputs/apk/release/app-release-unsigned.apk` is uploaded, directly as an APK (`archive: false`).
- Artifact retention is 14 days.
- Workflow triggers: push to `main` and manual dispatch.

## Source editing and safety

Open the source in an Android Studio release supporting AGP 9.4.1. Do not generate local APK deliverables in the shared workspace.

CI has read-only repository permissions and needs no signing secrets. Production signing must be configured securely later; never commit a keystore or password.
