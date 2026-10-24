# Pacemaker / Shared Rhythm

Version 1.0 · 24 October 2026

A flat identity for training together and keeping a shared rhythm. The approved direction is two synchronized angular pulse lines in acid yellow on charcoal. This package translates that concept into exact vector geometry and a reusable app design system.

## Start here

Open `index.html` in a browser. It works offline with bundled fonts and no remote dependencies. Its controls preview dark/light appearance, live/above-limit/lost-signal states, source selection, voice alerts and a configurable example limit.

Quick previews:

- `previews/identity-overview.png` — brand overview.
- `previews/app-screens-dark.png` and `app-screens-light.png` — live group, devices, profile/limit.
- `previews/live-above-limit.png` and `live-signal-lost.png` — edge states.
- `previews/components-dark.png` and `components-light.png` — controls, status messages and iconography.
- `previews/wearable.png` — Wear OS visual direction.
- `previews/icon-proof.svg` and `icon-proof.png` — mask and small-size proofs.

All sample measurements, people, device states and limits are illustrative. The screenshots are design concepts, not captures of implemented product changes.

## Package map

| Folder | Contents |
| --- | --- |
| `brand/` | Padded app-icon master, transparent marks, outlined wordmarks/lockups, pattern, PNG masters |
| `icons/android/` | Adaptive/themed XML, fallback launcher PNGs, Play Store icon, separate layers, manifest snippet |
| `icons/ios/` | Xcode app-icon set, standalone smaller PNGs, editable layer sources |
| `icons/web/` | SVGs, PNGs, ICO, Apple touch icon, PWA manifest and integration snippet |
| `icons/ui/` | 20 original interface SVGs, sprite and inline definitions |
| `tokens/` | JSON/CSS theme tokens, Android color resources and Material 3 role mapping |
| `fonts/` | Barlow Condensed SemiBold, Inter variable, licenses and source URLs |
| `src/brand.json` | Authoritative palette, geometry, type, spacing, motion and theme data |
| `references/` | Approved AI-generated concept and the original generation prompt |
| `tools/` | Reproducible export, preview, validation and packaging scripts |
| `validation/` | Machine-readable image, color, browser and native compiler results |

This is a design handoff. Existing production icon files and app UI are not modified. The proposed UI follows the repository's current Live and Settings scope. Timeline/history, maps, GPS, accounts, and new Apple Watch functionality are outside this design.

## Icon and logo rules

The master uses a 1024 × 1024 square. Two filled paths define the symbol, with the second path translated 170 units vertically. Flat foreground and background fills are deliberate: do not reintroduce texture, gloss, bevels, gradients or shadows.

The bare mark is 664 × 404 units. Preserve that ratio and keep at least one horizontal stroke thickness (54 units) clear around it. Do not rotate, stretch, disconnect, add heart shapes or animate the mark as though it were measured ECG data. Its two lines are a brand metaphor.

Use `brand/app-icon.svg` for a padded square, and `brand/mark-*.svg` for a tightly cropped transparent symbol. The `currentColor` version is intended for inline SVG; CSS on an HTML `img` does not recolor its internals. Use fixed-color SVGs for image elements.

Bare mark minimum: 24 CSS px wide. At 16 px, use the provided favicon and inspect it in context. Wordmarks and lockups are outlined paths and do not require installed fonts. Keep the letters unmodified. The Barlow-derived glyph outlines remain subject to the included font license.

## Android adoption

Copy the contents of `icons/android/res/` into the Android application's resource directory. Add the two attributes from `AndroidManifest.snippet.xml` to its existing application declaration:

```xml
android:icon="@mipmap/pacemaker_launcher"
android:roundIcon="@mipmap/pacemaker_launcher_round"
```

The names are prefixed to avoid accidentally replacing the current shared-module resources. For this repository, the application module is `androidApp`; explicitly setting its manifest icon references avoids ambiguity with existing `ic_launcher` resources in `app` and `app-core`.

The package contains 108 dp vector layers. The symbol is 50.4 dp wide and stays within the central 66 dp safe circle. Version 26 resources define background/foreground; version 33 adds monochrome for themed launchers. The OS chooses the mask and theme color. [Android adaptive icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

Fallback PNGs cover mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi at 48, 72, 96, 144 and 192 px, with standard and circular variants. The PNG layers under `layers/` are authoring exports; the supplied resource definitions use vectors.

Upload `play-store-512.png` separately to the Play listing. It is a full-square 512 px sRGB RGBA PNG with all pixels opaque, under 1 MiB. It contains no rounded outer mask or shadow. [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications).

## iOS and iPadOS adoption

Copy `icons/ios/AppIcon.appiconset` into the app's `Assets.xcassets`, replacing its existing AppIcon set when adopting the identity. Keep the target's app-icon name as AppIcon.

The catalog includes three 1024 px assets:
- Default: opaque sRGB RGB, no alpha.
- Dark: acid mark on genuine transparency.
- Tinted: white mark on black, grayscale values encoded as sRGB RGB.

Xcode generates the smaller variants from the single-size catalog. The standalone legacy PNGs are provided for manual size slots, not as an additional competing catalog. Platform masking is left to iOS. [Apple asset-catalog guidance](https://developer.apple.com/documentation/xcode/configuring-your-app-icon).

The SVGs in `composer-layers/` are clean background/foreground sources for optional Icon Composer authoring. A native `.icon` project and platform clear/glass treatments are not included. The verified delivery here is the asset-catalog route. [Apple app-icon guidance](https://developer.apple.com/design/human-interface-guidelines/app-icons).

## Web adoption

Copy `icons/web/` to a public asset directory and adapt `head-snippet.html` to its URL. The example assumes `/assets/pacemaker/`. Set `start_url` in the PWA manifest to the site's actual entry route.

Use SVG for the logo and favicons where supported. The ICO contains 16, 32 and 48 px entries. PNGs include a 180 px Apple touch icon and 192/512 px PWA icons. Dedicated maskable versions add extra clear space. Manifest icon URLs are relative to the manifest.

The web manifest and icon links supply identity metadata; they do not implement offline caching or a service worker.

## Color system

| Role | Dark | Light |
| --- | --- | --- |
| Canvas | #171A19 | #F3F5ED |
| Surface | #222624 | #FFFFFF |
| Raised surface | #2C312E | #E6EADF |
| Primary text | #F3F5ED | #171A19 |
| Secondary text | #A9B3AA | #546055 |
| Control boundary | #748178 | #7A8578 |
| Primary fill | #DBFF00 | #DBFF00 |
| Text on primary | #171A19 | #171A19 |
| Accent text | #DBFF00 | #4D6000 |

Use charcoal/chalk/neutral surfaces for most of the app and acid for a restrained number of primary actions and brand elements. White text on acid is not part of this system. Acid on light backgrounds is a fill, not body text.

Status colors are separate tokens. Mint denotes connection/within-limit states, coral denotes above-limit/error states, amber denotes attention, and ice denotes information. Each has a matching surface and a light-theme ink value. Text and a recognizable icon must carry the same meaning as color.

Participant colors remain stable identifiers, accompanied by names/initials. Do not repurpose a participant's color as an alert severity. A user-selected color that fails contrast should remain a decorative swatch with a readable neutral name label.

## Typography and layout

Barlow Condensed SemiBold 600 is the display and metric voice. Inter is the reading and control voice. Both are bundled under SIL OFL with their licenses. [Barlow source](https://github.com/google/fonts/tree/main/ofl/barlowcondensed), [Inter source](https://github.com/google/fonts/tree/main/ofl/inter).

| Use | Family | Size / line height | Weight |
| --- | --- | --- | --- |
| Brand headline | Barlow Condensed | 76–156 / 0.92× | 600 |
| Live metric | Barlow Condensed | 88–112 / 0.95× | 600 |
| Screen title | Barlow Condensed | 36–42 / 1× | 600 |
| Group reading | Barlow Condensed | 32 / 1.1× | 600 |
| Body/input | Inter | 16 / 24 | 400–500 |
| Button/member name | Inter | 14 / 20 | 600 |
| Supporting label | Inter | 12 / 18 | 400–600 |

Use tabular figures for updating values. Keep the numeral width stable. Do not animate through intermediate numbers. Mark bpm separately from the number, and always pair a displayed limit with its meaning.

Use a 4 px spacing grid with 8/12/16/24/32 px common steps. Phone gutters are 24 px; allow 18–20 px in compact widths. Controls use 8 px corners, cards 12 px, sheets 20 px. The brand symbol keeps its sharp geometry; touch controls need not imitate its corners.

Use minimum 48 px touch targets; adapt to native platform units. Support Dynamic Type/font scaling and longer localized strings with reflow or scrolling. The guide is checked from 320 px wide, but native font-scale and device testing remains part of implementation.

## Components and interaction

- Primary action: acid fill, charcoal label, semibold text, 48 px minimum height.
- Secondary action: transparent or neutral fill, readable border, normal text color.
- Destructive action: explicit label plus error tokens; never acid.
- State messages: icon + title + explanation; use a side rule and a flat semantic surface.
- Group row: initials, name, current bpm, and individual limit/delta. Keep numeric columns aligned.
- Device row: device name, availability, active-source distinction, and an explicit action.
- Inputs: visible labels, text/error feedback, focus indicator and no placeholder-only labeling.
- Limit control: large value with unit, decrement/increment buttons and a slider. Example range is for preview only; preserve product requirements when integrating.
- Motion: 120/180/240 ms for direct transitions; reduced motion removes nonessential transitions. No flashing glow, decorative heartbeat or continuous ambient animation.

Every control and numeric element needs an accessible native equivalent. Announce meaningful limit/connectivity changes, not each sensor sample. The supplied contrast checks cover intended token pairs; they are not a claim that a future native implementation automatically meets all accessibility requirements.

## State and content rules

Show “Within your limit” rather than “Safe.” The interface compares a measurement with a user-chosen threshold; it does not assess medical safety.

An unavailable value is an em dash with a signal-status explanation, never zero or a stale number marked live. Keep connection states distinct: searching, available, connecting, connected, disconnecting, lost, and paired/offline.

Above-limit content uses a coral label, explicit value/limit comparison and a short “Ease the pace” suggestion. Sound/haptics must honor the user's settings. Avoid making urgent state information depend on sound alone.

Use brief, calm, direct copy. “Your signal.” and “Keep pace. Together.” are brand moments; controls say “Use this sensor,” “Voice alerts,” and “Save preferences.” Avoid guilt, competition claims and medical promises.

## Implementation order

1. Adopt app-icon resources and verify masks on real Android/iOS home screens.
2. Apply semantic colors and the bundled typography through the app theme.
3. Bring the live overview and its signal/limit states into the new hierarchy.
4. Update device/source selection, profile and voice controls.
5. Apply the same tokens to Wear OS, keeping its interface smaller than the phone.
6. Verify font scaling, localization, dark/light themes, outdoor readability, and assistive technology.

The proposed layout is intentionally a design proposal. It reorganizes the current main/settings interactions without implementing them in the app.

## Reproduction and validation

From this directory:

```sh
pnpm install --frozen-lockfile --ignore-scripts
pnpm generate
pnpm validate
pnpm preview
python3 tools/package.py
```

Node 20+ and pnpm are sufficient for the icon generator. The installed dependency lock is included. Font binaries are local and are not fetched during generation.

Preview rendering uses Playwright, preferring locally installed Chrome on macOS. Else set `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` to a compatible Chromium binary or run `pnpm exec playwright install chromium`.

Validation checks 40 PNGs, catalog/manifest references, alpha/color modes, file size, Android safe geometry, intended contrast pairs and ICO entries. Where SDKs exist, it compiles/links Android resources with aapt2 and compiles the iOS catalog with actool. A missing SDK is reported as skipped, not passed. Browser checks cover local/offline loading, responsive layouts and representative control/state changes.

Results are in `validation/report.json`, `native-compilers.json`, and `browser-checks.json`. Android resources passed aapt2 compile/link with build tools 36.1.0 and Android 36; iOS assets passed actool. The IDE build tool could not provide useful diagnostics for standalone design resources, so native resource compilers supplied the platform validation. No production app build or on-device visual validation is claimed.

## Provenance

The imagegen concept was approved in this chat and is preserved in `references/` with its exact prompt. Production assets in this package are newly authored SVG geometry rendered deterministically, not resized copies of that raster. The typefaces retain their original licenses and source references. The guide uses original component artwork and no downloaded photography.
