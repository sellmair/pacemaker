# Pacemaker store kit

Prepared 25 October 2026 from the approved Shared Rhythm identity and implemented app screens. Open [index.html](index.html) for the visual gallery.

Ready-to-transfer archives: [playstore.zip](playstore.zip) and [appstore.zip](appstore.zip). Each contains that store's final copy, upload images, alt text, manifest and upload notes. Editable sources stay in this folder.

## Contents

| Package | Copy | Images |
| --- | --- | --- |
| [Google Play](playstore/) | English, German and Italian title, short/full description and update notes | Five English phone screenshots at 1080 × 1920; three localized 1024 × 500 feature graphics; 512 × 512 store icon; three unframed Wear OS captures per language at 454 × 454 |
| [App Store](appstore/) | English, German and Italian name, subtitle, promotional text, description, keywords and update notes | Five English iPhone screenshots at each of 1206 × 2622 and 1320 × 2868; default 1024 × 1024 icon |

The phone sets use native captures in English. German and Italian copy is ready separately; their feature graphics have localized marketing text around the same English Android UI. Wear OS screenshots show the corresponding app language. No tablet set is included.

Use the numbered screenshot order. Upload one five-image set per matching device slot, rather than combining the two iPhone size sets in a single slot. Feature graphics belong in the Google Play feature-graphic field. The App Store icon normally comes from the uploaded app's asset catalog; the supplied PNG is a matching handoff copy.

Individual field files contain only text for pasting. `listing.json` groups the same fields. The human-readable copy sheets are [Google Play](playstore/COPY.md) and [App Store](appstore/COPY.md). Fields named `whats_new` or `release_notes` describe an update; skip these for an initial release where appropriate.

## Creative direction and provenance

The kit uses the existing acid/charcoal/chalk palette, Barlow Condensed and Inter, and outlined Pacemaker lockups. The first image introduces training together; the next images explain personal limits, sources, status and voice preferences. Coral frames the Android above-limit example. Mint is used for the iPhone light/dark appearance image.

Every app screen is an actual native capture with controlled sample data from `design/implementation/`. App pixels, labels and measurements were not redrawn or generated. HTML/CSS provides the surrounding store typography and flat backgrounds. No personal physical-device screenshot is included. The approved concept previews were used for direction, not passed off as implemented app screenshots.

Scanned sources include `design/HANDOFF.md`, `src/brand.json`, brand masters, fonts, icon exports, preview boards and the Android/iOS/Wear implementation evidence. [manifest.json](manifest.json) records output dimensions, source paths, hashes, editable layouts and upload alt text. The copied native sources are under `source/assets/native/`. Font licenses are retained alongside the bundled fonts.

## Before submitting

The images and copy are an asset package, not a published store listing. Complete the publisher-specific fields in [submission-details.json](submission-details.json): actual support/privacy URLs, contact details, copyright owner and final release version. Set pricing and availability in the consoles. Data Safety, App Privacy, health declarations and age-rating answers need to match the submitted build and the publisher's practices; this kit does not invent those declarations.

Health & Fitness is the proposed category. Copy describes fitness comparisons with a user-chosen threshold, not medical monitoring or a safe-heart-rate assessment. Live readings require compatible hardware. Confirm real sensor/watch behavior against the final release build before publishing those claims.

The iOS package covers the iPhone design. The project also embeds an existing watchOS companion whose redesign was outside the completed iPhone work. If submitting that companion, supply genuine watchOS screenshots for the required Apple Watch slot and verify its operation. The Wear OS images are only for Google Play and must not be used as Apple Watch screenshots.

The publisher should confirm app-name availability in both consoles; no name-availability or trademark-clearance claim is made here.

## Specifications checked

Current official guidance was consulted on 25 October 2026:

- [Google Play listing fields](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en).
- [Google Play graphics and screenshots](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en): opaque RGB phone screenshots/feature graphics; short taglines; plain square Wear OS captures; separate 512 px icon.
- [Apple screenshot sizes](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/): the current page lists an iPhone Dynamic Island medium-display submission requirement. The package includes its 1206 × 2622 set as well as the large-display 1320 × 2868 set. Both are opaque PNGs.
- [Apple product-page copy](https://developer.apple.com/app-store/product-page/).
- [Apple version metadata](https://developer.apple.com/help/app-store-connect/reference/app-information/platform-version-information/): keyword validation uses UTF-8 bytes.

## Regenerate

From the repository root, with Node and the existing `design/node_modules` dependencies installed:

```sh
node store/source/generate.mjs
```

The renderer uses local Chrome through Playwright. Set `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` if Chrome is installed elsewhere. It uses no remote assets.

Edit `source/copy.json` for metadata and headline text, `source/poster.css` for presentation, and `source/generate.mjs` for image/source selection. It writes the metadata files, editable HTML layouts, final PNGs, gallery, manifest and validation report. The copy sheets and upload notes are editorial documents; keep them aligned when changing the metadata master.

[validation.json](validation.json) records text limits, font/image loading, layout bounds, screenshot dimensions and RGB encoding. The rendered gallery and representative full-size exports were visually inspected.
