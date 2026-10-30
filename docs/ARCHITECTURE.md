# Foundation architecture decisions

## ADR 001: Native Android, Kotlin and Compose

The owner requested Kotlin. This is currently a single Android application module using Jetpack Compose and Material 3. No cross-platform runtime, WebView shell or XML screen layout is introduced. XML remains appropriate for Android manifests, resources, adaptive icons and splash themes.

## ADR 002: Wait for the product brief

The repository originally contained only a README. The first screen is explicitly a development foundation. Navigation, networking, persistence, authentication, account models and domain features will be chosen when the actual app requirements are known. Adding them now would create speculative code and unnecessary permissions.

The provisional application ID is `io.github.lxzrvi.rekey`. Confirm it before publishing; changing it after release creates a different app identity.

## ADR 003: Centralized typography and fixed brand colors

Poppins is bundled in four real weights. The Material type scale is defined centrally. Brand typography is separate from UI typography, so the licensed Letterhend Holiday file can replace the current marked fallback without changing every screen.

The brand is deliberately monochrome. OEM dynamic color is disabled to preserve it. Light and dark themes are both supported; logo and icon carrier colors remain fixed to preserve contrast.

## ADR 004: Native animation first

The foundation uses Compose `AnimatedVisibility`, fade and vertical size transitions with bounded durations. Compose honors Android's Animator duration scale. There is no infinite animation, animation service or additional animation SDK.

Evaluate Lottie only for a real designer-authored vector animation; consider other tools only if the feature genuinely needs them. Avoid adding a dependency just because an animation might exist later.

## ADR 005: Small, testable foundation

- `design/DesignContract.kt` holds platform-independent design decisions.
- `ui/theme` owns colors, typography and shapes.
- `ui/brand` owns the brand-font integration point.
- `ui/components` owns reusable brand presentation.
- `ui/foundation` is temporary and can be replaced when product screens arrive.
- `rememberSaveable` preserves the demo's interaction state through recreation.

JVM tests check basic design contracts. Emulator tests exercise actual Compose rendering, interaction and activity recreation. Neither replaces feature-level tests when real business logic is added.

## ADR 006: Secure, reproducible CI

The Gradle distribution and wrapper are checksum-pinned. Dependencies use a version catalog and a stable Compose BOM. GitHub Actions are SHA-pinned. CI has no write permissions or signing secrets. Release shrinking is checked without pretending that an unsigned APK is production-signed.

## Deliberate defaults to confirm later

- Minimum Android 8.0 / API 26.
- Phone-first adaptive layout, scrollable and capped at 640 dp content width.
- English foundation copy; product languages are not yet specified.
- No analytics, ads, tracking, account system or network permission.
- No app-code license selected by the agent on the owner's behalf.
