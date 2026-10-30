# Build and CI

## Owner's build policy

- APK builds run on **GitHub Actions only**. The Arena workspace holds source files, not APK deliverables.
- The **only uploaded artifact is the release APK**. Do not upload debug APKs, test reports, mappings, screenshots or separate report files.
- Runtime checks may compile temporary debug/test APKs on the GitHub runner. These are never uploaded and disappear with the runner.
- No production keystore is configured. The downloadable file is explicitly `app-release-unsigned.apk`; it is not a signed or Play Store-ready release.

## Pinned toolchain

| Component | Version |
| --- | --- |
| Gradle | 9.7.1 |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.2.10, supplied by AGP |
| Compose Compiler plugin | 2.2.10, matching Kotlin |
| Compose BOM | 2026.09.00 |
| Compose UI / animation / foundation | 1.12.1 through the BOM |
| Material 3 | 1.4.0 through the BOM |
| Activity Compose | 1.13.0 |
| AndroidX Core | 1.19.1 |
| Compile/target SDK | 37.0 |
| Minimum SDK | 26 |
| Build Tools | 36.0.0 |
| CI JDK | Temurin 21 |
| Java/Kotlin bytecode compatibility | 17 |

AGP supplies Kotlin; do not also apply `org.jetbrains.kotlin.android`. The Compose Compiler plugin matches the bundled Kotlin version. Google's SDK package is `platforms;android-37.0`, with `compileSdkMinor = 0`.

The Gradle wrapper and distribution have published checksums. CI validates the wrapper. GitHub Actions are pinned to commit SHAs.

## Release job

The workflow runs `:app:assembleRelease :app:testReleaseUnitTest :app:lintRelease`.

- Release compilation, R8 minification and resource shrinking must succeed.
- Release-variant JVM tests must pass.
- Release lint runs with errors failing the job.
- Only `app/build/outputs/apk/release/app-release-unsigned.apk` is uploaded, directly as an APK rather than an additional ZIP archive.
- Artifact retention is 14 days.

## Runtime job

After the release job passes, an API 35 x86_64 emulator runs four Compose tests and an explicit install/launch/process/crash-buffer check. A temporary debug APK is needed because the release APK is unsigned and cannot be installed for this check. It is not a deliverable.

Results are visible in the GitHub job logs. No screenshots, log files, report artifacts or test APKs are uploaded. System animations are disabled for deterministic tests; this is not a physical-device performance guarantee.

## Source editing and safety

Open the source in an Android Studio release supporting AGP 9.4.1. Follow the GitHub-only build policy above; do not generate local APK deliverables in the shared workspace.

CI has read-only repository permissions and needs no signing secrets. Production signing must be configured securely later; never commit a keystore or password. Do not disable tests or lint to obtain a green badge.
