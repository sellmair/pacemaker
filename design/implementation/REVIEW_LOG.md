# Shared Rhythm implementation review

Reviewer: GPT-6.1, high effort. Implementor: GPT-6 Luna, high effort. Parent coordinates integration and Android evidence.

## Baseline

Android build passed. Existing startup instrumentation passed on the Android 15 emulator. Existing app-core host suite passed all 29 tests. See `android/BASELINE.md` and `android/core-regression-baseline.json`.

## Batch 1 — accepted

Independent review initially found six issues: focused input contrast, incomplete Material role mapping, light selected-device indicator, omitted icon circles, slider semantics, participant initials contrast. Implementor corrected all six; independent rereview accepted the code.

Five controlled native component tests passed on the emulator. Dark/light captures were pulled, visually inspected by the parent and independently inspected by the reviewer. The native component visual gate passed, including Barlow/Inter typography, semantic surfaces and selected-device treatment.

Captures: `android/components-dark-native.png`, `android/components-light-native.png`.

## Batch 2 — accepted with three-tab amendment

Independent review requested: full themed startup background, explicit IME handling, preserve permission-denied startup status when only some Bluetooth permissions are granted, and horizontal display-cutout insets. Corrections and follow-up fixes preserve independent backend binding and Bluetooth-enable prompting; cancelling the Bluetooth prompt does not block the UI.

The user changed navigation to three direct tabs: Live, Devices and Profile. The implementation and visual guide were updated accordingly. Independent review verified navigation/restoration/Back order and English/German/Italian tab resources. Eight native component/startup tests passed, including compact large-font retry access.

## Batch 3 — accepted

Independent review found and verified fixes for resurrecting a cleared own reading from GroupState, an old clock immediately after resume, an invented personal limit, light-mode radio contrast, and equality copy. The state-driven code gate passed.

Fourteen native Live tests pass, covering Automatic mode, explicit source unavailability, expired/future samples, retained peers, self deduplication, missing limits, equality, all three voice modes and compact font scaling. Three navigation tests pass for direct entry, Back and saved-tab restoration.

The initial native Live composition was more card-heavy than the approved guide. Parent requested the flat left-aligned metric, compact meter, limit editing outside the default Live view, and a compact speech selector retaining all three modes. Native screenshot fixtures were corrected to use the actual themed shell before final capture.

Independent review reopened all five final native captures and accepted visual fidelity: dominant 112 sp standard metric, flat meter and member rows, reference header without extra subtitle, regular TOGETHER label, compact legend reflow and accessible speech control. All material Live findings are closed.

Captures: `android/live-within-limit-light-native.png`, `android/live-above-limit-dark-native.png`, `android/live-no-local-signal-dark-native.png`, `android/live-compact-large-font-initial-dark-native.png`, `android/live-compact-large-font-dark-native.png`.

## Batch 4 — accepted

Independent review found a persisted BLE availability snapshot could hide fresh measurements after reconnect/relaunch. MainPage now reconciles the saved choice with the actual connection/Bluetooth states before rendering. Disconnecting is distinct from a missing connection actor and does not expose Connect. Blocked Bluetooth remains visible with retained discovery entries. Fractional stored limits are rounded consistently for display and checked against their original value for range feedback.

Twenty-one native checks passed after these fixes: Profile 8, Devices 9, BLE presentation 3, and a real-Activity preferences/IME integration test. The integration test exercises actual Evas routes, valid name and voice persistence across Activity recreation, keyboard dismissal before route Back, and return to Live. Later focused tests extend source identity and localized large-font coverage.

The reviewer verified final fidelity corrections for the segmented selector, slider geometry, compact bpm unit, discovery-section placement, full-width device action, input/control surface fills, Profile metric scale and voice dividers. Source reselect/platform availability, long localized device actions, natural bottom-tab height and full-height notice rules are corrected. Profile/Devices light/dark and German/Italian large-font captures are accepted.

## Batch 5 — accepted

The phone module owns the supplied adaptive/themed vectors, fallback PNGs, manifest icon/roundIcon and a phone-only charcoal launch theme. Native installed-resource adaptive and monochrome icon proofs passed and were independently inspected.

Automatic review initially rejected the broad obsolete-source deletion request because exact scope and preservation of behavior were insufficiently proven. The implementor and reviewer independently established a closed 20-file presentation cluster with zero external references, preserved a verified recovery archive, then made one coherent retry. The retry succeeded. The reviewer confirmed all exact paths are absent and retained state/intent/identity utilities remain. See `OBSOLETE_UI_CLEANUP.md` for the exact scope, original rejection, independent audit and resolution.

## Batch 6 — final accumulated review accepted

Full native suite: 58 passed. Post-cleanup app/instrumentation build: passed. Post-cleanup native Profile/Devices/navigation/launcher/original foreground-service startup suite: 30 passed. Actual landscape runs: 3 passed after cleanup, including font-scale 1.6 and lower-control scrolling. Core Android host regressions: 29 passed. Design export validation: 40 images passed.

The reviewer reopened the settled light Devices capture, all landscape captures and icon proofs, closed the final visual gate, and accepted the accumulated code and cleanup with no unresolved actionable findings. The final APK was installed/opened on the user's Pixel over wireless ADB.

Controlled UI fixtures do not prove physical sensor/watch interoperability. Dedicated iOS/Wear acceptance remains deferred. See `android/README.md` for the evidence limits and raw results.
