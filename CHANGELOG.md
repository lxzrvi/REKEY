# Changelog

## 0.4.0 — Text-only startup, in-place menu morph, redesigned card stack

- Startup now shows only the REKEY wordmark; the loading spinner is removed.
- Main-page logo is smaller (42 dp) and the wordmark is smaller (26 sp).
- The three-dot control no longer travels: it morphs into an X in the same place, with the Appearance label directly to its left.
- Theme options are full-width segmented buttons with gaps; text size uses S/M/L/XL buttons instead of a slider.
- The sidebar has rounded top-left and bottom-left corners, with the border following only the left edge.
- Replaced the pager carousel with a continuous draggable card stack (`ui/home/CardStack.kt`): depth scale/fade, tap-to-front, add card, per-card remove, and a counter with a progress bar. Card count and position persist locally.

## 0.3.0 — Release-only CI, Haze 2 and UI polish

- Simplified GitHub Actions to a single release-APK job on `main`; removed unit-test, lint and emulator jobs.
- Removed test sources, test dependencies and the ephemeral emulator script.
- Disabled R8/resource shrinking for a faster unsigned release build.
- Upgraded to Haze 2.0.1 + haze-blur 2.0.1 and Kotlin/Compose compiler 2.4.20.
- Replaced `GlassBlur.kt` with `ui/blur/LiveBlur.kt`: pure blur, top/bottom progressive edge styles and full-screen drawer blur.
- Removed the boxed/blurred header; the circular logo and wordmark now sit directly on the background.
- Made carousel cards wider with smaller text and an infinite circular 3D rotate/scale feel.
- Moved the drawer toggle inside the drawer; the drawer is flush to the top/right/bottom screen edges with a left-only border.
- Replaced Material sliders with thin thumb-less sliders; long press resets to the default.
- Added a custom accent color wheel and hex input.
- Changed the launcher icon to a white background with smaller black REKEY text.

## 0.2.0 — Branding and glass home

- Bundled Allura for the REKEY wordmark only; Poppins remains the body font.
- Added the glass home screen, appearance drawer, ambient background, splash and demo-card carousel.
- Added local persistence for appearance and demo-card state.

## 0.1.0 — Foundation

- Kotlin/Compose Android scaffold, edge-to-edge entry, themes, typography, launcher/splash resources and GitHub-only release output.
