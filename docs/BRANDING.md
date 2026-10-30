# REKEY branding

## Owner's decisions

- White logo/wordmark on a contrasting dark surface.
- Black icon on a light surface.
- Holiday for brand typography throughout the app.
- Poppins for other app text, in Thin (100), Regular (400), Medium (500) and Bold (700).
- Selected Holiday direction: **Letterhend Studio**, option 01 in the supplied comparison sheet.

## Holiday is pending, not silently substituted

Official source: https://www.letterhend.com/product/holiday-font/

License terms: https://www.letterhend.com/licenses/

At research time, the official product listed an App/Game license at USD 400. Its terms listed embedded fonts for one app and one game, with unlimited app/game views and downloads. The price is not a quote; verify current pricing and the actual agreement before purchase. A desktop license alone should not be assumed to cover app embedding.

The free downloads found during research were personal-use demos. They are not bundled here. No purchase has been made and no commercial license has been represented as obtained.

Before integration, the owner must supply the licensed font files and confirm app-embedding rights. Do not commit receipts, account details, license keys or other private purchase records to this public repository. Confirm whether the chosen license permits the intended repository distribution; if not, keep the font in an appropriately access-controlled asset source.

Integration point: `ui/brand/BrandTypography.kt`. Add legally usable font resources, update that object and `BrandContract.LICENSED_BRAND_FONT_BUNDLED`, then remove the pending-brand label only after checking real rendering. Check uppercase REKEY, punctuation, required language glyphs and any OpenType alternates. Android font rendering does not automatically reproduce every desktop OpenType feature.

The current key-shaped icon is **provisional**, not a final approved identity. The app's actual purpose may change the appropriate symbol.

## Poppins provenance

Official Google Fonts source, pinned to commit:

https://github.com/google/fonts/tree/8b0a1d0f5983c89bc2b93f1b5fb55f9e252744b5/ofl/poppins

Files are unmodified, downloaded from that commit:

| Resource | SHA-256 |
| --- | --- |
| `poppins_thin.ttf` | `6d8e5d9d29140cc93e321745fa1243c67889e6bc3639ec34db64f3da7a496352` |
| `poppins_regular.ttf` | `7e65201e9b79159e2300267cc885e16c8dcef2424cdfa09a29bfb0980a94a7ba` |
| `poppins_medium.ttf` | `90373e7d838d32468438fc3e152dca0bdb12edcab99ea639f158790b1ba1fd05` |
| `poppins_bold.ttf` | `983676516167748b74de6f4771fb384c664fd913acb8b471122ecacf5da5ea6c` |

The original OFL is included at `app/src/main/assets/licenses/poppins_ofl.txt` and travels in the APK. No runtime Google Fonts download or font-related network permission is needed.

## Contrast and accessibility

The white wordmark stays on black in both light and dark themes. The black icon stays on a white carrier. Do not place white directly on the light app background or black directly on the dark background. Body text uses the theme's foreground colors, not the fixed logo colors.

The wordmark is logo artwork with bounded dimensions and an accessible name. Other text uses scalable `sp` typography. Do not disable system font scaling globally to make a layout fit.
