# Architecture decisions

## ADR 001: Native Android, Kotlin and Compose

REKEY is a single Android application module using Jetpack Compose and Material 3. XML remains for manifests, resources, adaptive icons and splash themes.

## ADR 002: Wait for the product brief

The current screen demonstrates branding, appearance controls and demo cards. Product navigation, networking, authentication and domain features will be chosen when the owner supplies the actual app requirements. The provisional application ID is `io.github.lxzrvi.rekey`.

## ADR 003: Typography and brand colors

Allura is bundled only for the REKEY wordmark/icon. Poppins is bundled in four weights for UI text. Brand typography is separate from UI typography. OEM dynamic color is disabled to preserve the monochrome brand carrier; accent color is user-selectable, including a custom color wheel and hex input.

## ADR 004: Pure live blur with Haze 2

`ui/blur/LiveBlur.kt` wraps Haze 2.0.1 + haze-blur 2.0.1. Blur styles set `noiseFactor(0f)`, `backgroundColor(Color.Transparent)` and `colorEffects(emptyList())`. Top/bottom edges use progressive vertical gradients; the appearance drawer uses a full-screen blur that fades with the drawer. The header itself is not boxed or blurred.

## ADR 005: Local state only

`data/RekeyPreferences.kt` persists appearance, card count and the selected card position in SharedPreferences. `rememberSaveable` preserves transient UI state through recreation. No account, network permission or analytics is introduced.

## ADR 006: Release-only GitHub CI

The workflow runs only `:app:assembleRelease` on `main` and uploads only `app-release-unsigned.apk`. R8/resource shrinking are disabled for speed. Unit tests, lint and emulator checks are not part of this release path by owner request. The Gradle wrapper and GitHub Actions are checksum/SHA-pinned. CI has read-only permissions and no signing secrets.

## Deliberate defaults to confirm later

- Minimum Android 12 / API 31.
- Phone-first adaptive layout with safe-area handling.
- English UI copy; product languages are not yet specified.
- No analytics, ads, tracking, account system or network permission.
- No app-code license selected by the agent on the owner's behalf.

## ADR 007: Continuous card stack instead of a pager

`ui/home/CardStack.kt` replaces the pager. A single animated `pos` value drives translation, scale, fade and depth dim inside `graphicsLayer`, so dragging does not recompose the card tree. Cards can be tapped to the front, added from the trailing plus card, and removed individually. Startup shows only the REKEY wordmark; there is no loader.
