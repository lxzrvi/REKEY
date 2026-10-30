# Build and CI

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

AGP 9 supplies Kotlin: do not also apply `org.jetbrains.kotlin.android`. The Compose Compiler plugin matches the bundled Kotlin version. The selected Compose UI and Core artifacts require compile SDK 37.0 and at least AGP 9.1, as declared in their AAR metadata.

The Gradle wrapper JAR was checked against Gradle's published SHA-256. The distribution URL also pins the published distribution checksum. CI enables wrapper validation. Action dependencies are pinned to commit SHAs with release-version comments.

## What the workflow proves

`build` job:

1. Compile and package an installable debug APK.
2. Run JVM design-contract tests.
3. Run Android lint with errors failing the job.
4. Compile an unsigned, minified release APK and run resource shrinking.
5. Upload the APK, reports and release mapping.

`emulator` job, after the build passes:

1. Boot an API 35 x86_64 Google APIs emulator.
2. Run four Compose UI tests: launch, font-weight presence, motion interaction and activity recreation.
3. Install and explicitly launch the APK, check its process and collect a screenshot.
4. Check the crash buffer and preserve logcat evidence.

The emulator disables system animations for deterministic UI testing. This tests the state changes and zero-duration path, not subjective animation smoothness. Real-device animation/performance review remains separate. API 35 is a smoke-test target, not a claim of compatibility with every supported Android version.

## Limits and safety

- The verified code baseline passed the complete remote workflow; exact run evidence and remaining limits are recorded in STATUS.md.
- A successful build is not a guarantee against every runtime bug.
- The release APK is unsigned and must not be described as store-ready.
- CI has read-only repository permissions and does not need signing secrets.
- No Build Scan is published; `setup-gradle` uses its basic GitHub cache provider.
- Debug APKs are test artifacts. Their signing key is not a production identity.
- Re-run and diagnose failing checks; do not disable lint/tests to obtain a green badge.

## Local prerequisites

Install a JDK compatible with Gradle 9.7.1 (JDK 21 recommended), Android SDK Platform 37.0 and Build Tools 36.0.0. Accept the Android SDK licenses through Android Studio or `sdkmanager`. Set `ANDROID_HOME` or create an uncommitted `local.properties` pointing to the SDK.

Use `./gradlew` on macOS/Linux and `gradlew.bat` on Windows. Do not use a globally installed Gradle in place of the wrapper.
