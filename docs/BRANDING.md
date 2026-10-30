# REKEY branding

## Owner's decisions

- App name is exactly `REKEY`.
- Allura is used only for the REKEY name/wordmark/icon text.
- Poppins is used for all other app text.
- The in-app logo is circular: white carrier with black REKEY text.
- The launcher icon uses a white background with smaller black REKEY text.
- The splash remains white REKEY on black.

## Allura provenance

Official Google Fonts source, pinned to commit:

https://github.com/google/fonts/tree/8b0a1d0f5983c89bc2b93f1b5fb55f9e252744b5/ofl/allura

| Resource | SHA-256 |
| --- | --- |
| `allura_regular.ttf` | `9c142b2e515832c0dfc4ff8b8ea18f40314943bf937b72e2b23c4661bac14cc6` |

Allura is by Robert E. Leuschke / TypeSETit, licensed under SIL OFL 1.1. The license is included at `app/src/main/assets/licenses/allura_ofl.txt`.

The launcher/brand/splash vectors are outline derivatives of the bundled Allura glyphs, generated so the wordmark renders consistently without relying on system font substitution.

## Poppins provenance

Official Google Fonts source, pinned to commit:

https://github.com/google/fonts/tree/8b0a1d0f5983c89bc2b93f1b5fb55f9e252744b5/ofl/poppins

Files are unmodified: `poppins_thin.ttf`, `poppins_regular.ttf`, `poppins_medium.ttf`, `poppins_bold.ttf`. The original OFL is included at `app/src/main/assets/licenses/poppins_ofl.txt` and travels in the APK. No runtime Google Fonts download or font-related network permission is needed.

## Contrast and accessibility

The circular logo keeps a white carrier with black text in both themes. The launcher icon keeps a white background with black text. Body text uses theme foreground colors, not fixed logo colors. The wordmark is logo artwork with an accessible name; other text uses scalable `sp` typography.
