# Android phone implementation plan — Shared Rhythm

Status: complete and independently accepted on 25 October 2026. All six implementation batches are finished. The final Android APK is installed on the Pixel 10 Pro. Native evidence: full suite 58 passed, post-cleanup focused suite 30 passed, landscape 3 passed, core regressions 29 passed, and 40 design images validated. See `implementation/android/README.md` for captures, commands and validation limits.
Scope confirmed in chat: replace the old design in the shared Compose sources and focus implementation, testing and visual acceptance on the Android phone app. Improvements that reach iOS through shared code are welcome. Do not maintain a legacy-design switch or parallel old UI.

## Execution team

| Role | Model | Reasoning effort | Responsibility |
| --- | --- | --- | --- |
| Parent/orchestrator | Current chat | Current setting | Own scope, task sequencing, integration, evidence and completion |
| Implementor subagent | GPT-6 Luna (`gpt-6-luna`) | high | Implement bounded batches, run relevant checks, fix review findings |
| Reviewer subagent | GPT-6.1 (`gpt-6.1-sol`) | high | Independently inspect the diff, behavior and screenshots; report actionable findings |

Create these two subagents when execution begins. Spawn them with fresh context and explicit model/effort settings so the requested models are used rather than inherited. Provide this plan, the design references, repository instructions and the current task contract to both.

The implementor owns code edits. The reviewer remains read-only and reviews each completed batch against both its acceptance criteria and the accumulated change. The parent sends findings back to the implementor, reruns affected checks after fixes, and requests re-review. Continue this loop autonomously; these are agent review gates, not requests for repeated human confirmation.

Do not silently replace an unavailable requested model. Report the actual availability problem. Use Android Studio MCP with an explicit projectPath for every supported code operation, and never use the IDEA server.

## Source of truth

Visual references:
- `design/index.html` — editable visual guide and screen concepts.
- `design/src/brand.json` — authoritative colors, mark geometry, typography, spacing and motion.
- `design/previews/app-screens-dark.png` and `app-screens-light.png`.
- `design/previews/live-above-limit.png` and `live-signal-lost.png`.
- `design/HANDOFF.md` — asset installation and design rules.
- `design/icons/android/` — already generated Android icon resources.

The HTML preview is a visual reference. Its fake data, simplified voice toggle, example slider range and fake Save button are not new product requirements.

## Current code and decisions

The phone UI lives in `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/`. Android MainActivity calls the shared ApplicationWindow, which applies PacemakerTheme and PageRouter. The same shared UI is hosted by iOS.

1. Replace PacemakerTheme and the current main/settings UI in commonMain. Do not introduce an Android-only duplicate UI or a legacy mode.
2. Keep Android-specific system bars, entry/lifecycle behavior, launcher resources and permission presentation in Android source sets.
3. Continue using the Evas states and intents already in the project. Do not introduce a parallel application state engine or new backend ownership.
4. Follow system light/dark appearance using the fixed Shared Rhythm palettes. Dynamic wallpaper colors must not replace the approved acid/charcoal brand.
5. Use three bottom navigation destinations: Live, Devices (Your signal), and Profile (Make it yours). This follows the user's execution feedback. Remove the intermediate Setup level and its internal tabs. Back from Devices/Profile returns to Live after dismissing an open keyboard; preserve the selected tab across recreation.
6. Replace the vertical group scale with the proposed large current reading, horizontal limit meter and named group rows. Preserve the ability to edit the user's limit from the new controls.
7. Preserve the current 40–200 bpm UI limit range, rather than copying the guide's 80–200 example. Preserve existing stored values, user colors and persistence behavior.
8. Preserve all three UtteranceState values: Silence, Warnings and All. Use explicit labels such as Off, Warnings only and All updates; do not reduce them to a boolean.
9. Preserve immediate preference updates through existing intents. Name, identity color, limit and speech controls must not imply an unimplemented draft/save transaction. Omit the mock Save preferences action; show unobtrusive saved/validation feedback where useful.
10. Expose Automatic, sensor selection and watch-source selection through existing source-selection behavior. An explicitly selected disconnected watch must not silently switch to a strap. The watch appears as a phone input source; watch app redesign is outside this phase.

Main files:
- `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/PacemakerTheme.kt`
- `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/ApplicationWindow.kt`
- `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/PageRouter.kt`
- `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/mainPage/`
- `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/settingsPage/`
- `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/`
- `app/src/commonMain/composeResources/`
- `androidApp/src/main/kotlin/io/sellmair/pacemaker/MainActivity.kt`
- `androidApp/src/main/AndroidManifest.xml`
- `androidApp/src/main/res/`

## Batch 0 — Establish the baseline and contracts

Implementor:
- Re-read AGENTS.md, applicable skills and current Git status; preserve unrelated work.
- Establish the Android build and existing startup-test baseline through Studio.
- Record current state/event contracts for MeState, GroupState, SelectedHeartRateSourceState, HeartRateSensorConnectionState, WatchCompanionDeviceState and UtteranceState.
- Capture the current Android phone screens and record existing build/test failures separately.
- Identify reusable behavior helpers; identify presentation widgets that will become obsolete.

Reviewer gate:
- Confirm the plan follows the final shared-code/Android-focus instruction.
- Confirm the initial file scope, baseline failures and behavior invariants are clear.

Deliverable: baseline notes and an agreed implementation checklist under `design/implementation/`.

## Batch 1 — Shared theme, typography and components

Implementor:
- Replace the light-only, user-hue-driven Material color scheme with the two Shared Rhythm schemes.
- Keep participant identity colors separate from brand/action colors.
- Package the existing licensed Barlow Condensed and Inter fonts in Compose resources and verify actual Android rendering of the required weights and tabular numerals.
- Add reusable theme tokens for status colors, spacing, shapes, typography and short motion.
- Convert the supplied mark and required UI SVGs into Compose-compatible vector resources or ImageVectors. Avoid runtime SVG parsing on Android.
- Build the small component family needed by these screens: primary/secondary buttons, status notices, member rows, device cards, labeled inputs and limit controls.
- Keep components stateless where possible, with explicit state and callbacks; keep state collection at screen boundaries.
- Use the existing dependency versions unless a concrete compatibility problem requires a narrowly scoped change.

Reviewer gate:
- Compare light/dark previews with the kit.
- Check acid text is not used on light surfaces, text on acid is charcoal, and status/participant colors remain distinct.
- Check font/resource portability and avoid unnecessary architecture or dependency changes.

Deliverable: reusable shared design system compiling for the Android target.

## Batch 2 — Application shell and navigation

Implementor:
- Rework ApplicationWindow/PageRouter to apply the new theme, brand header and labeled Live/Devices/Profile bottom navigation.
- Make Devices and Profile direct top-level destinations; remove the intermediate Setup section selector.
- Preserve the selected route and appropriate screen/input state across rotation; handle Back and IME dismissal predictably.
- Correct Scaffold padding and status/navigation/keyboard insets instead of layering manual spacers.
- Update Android system-bar icon appearance when the app theme changes.
- Render a branded loading/permission/connection state before the backend is ready rather than a blank content area.
- Keep foreground-service binding, permission requests and existing service lifecycle behavior intact.

Reviewer gate:
- Test navigation, Back, rotation, keyboard visibility and both system-bar appearances.
- Check the app is usable at compact phone widths and in landscape.
- Confirm no old-design route or feature flag remains.

Deliverable: the new shell is the sole phone UI entry.

## Batch 3 — Live group screen and measurement states

Implementor:
- Build the primary bpm reading, explicit status label, limit marker/meter, group rows and speech-mode control from real state.
- Render the current user once, even when GroupState contains the same user or temporarily omits them.
- Keep member identity stable by user ID; display names/initials, bpm, and each member's limit or a truthful missing-limit state.
- Keep peer measurements visible when the local source is unavailable.
- Derive within/above-limit status only when both a valid reading and applicable limit exist.
- Show an em dash for absent, future-dated or expired readings, with a reason/status where known. Never synthesize a number or label an old reading Live.
- Use existing source freshness contracts and timestamps. Because GroupState can retain a peer longer than a measurement is fresh, make fresh/retained-unavailable presentation explicit without changing transport or retention policy.
- Use a lifecycle-aware, bounded clock update only where freshness presentation needs it; avoid a separate sensor polling loop.
- Make the meter cover the actual reading and limit; handle out-of-range values without drawing an incorrect position.
- Label counts accurately, including whether the user is counted.

Reviewer gate:
- Inspect deterministic fixtures for no source, waiting for first sample, within limit, above limit, expired sample, empty group, missing peer limit and multiple members.
- Verify no duplicate self row, stale-live value, invented availability or dropped voice mode.
- Compare Android screenshots with the dark/light reference screens.

Deliverable: a real-data live screen with explicit edge states.

## Batch 4 — Devices, profile and settings

Implementor:
- Replace the existing settings layout with the new device cards and profile/limit controls.
- Wire Automatic and specific-device selection to HeartRateSourceSelectionIntent; preserve companion device identity and explicit source persistence.
- Distinguish searching, available, connecting, connected, disconnecting, permission required, unsupported and paired/offline states using the backend data that actually exists.
- Keep connection/disconnection actions honest; selected source and active streaming source are not always the same thing.
- Wire name, identity color and limit changes to existing update intents and speech modes to UtteranceState.
- Validate name input without losing persisted data, and avoid resetting user edits on every unrelated state update.
- Retain the color-selection capability in the new style; the user's color must not recolor every primary control.
- Localize new visible text and accessibility labels in the existing English, German and Italian resource sets.
- Provide empty/error states and target sizes appropriate for touch and TalkBack.

Reviewer gate:
- Exercise source changes, unavailable selected watch, reconnect, profile edits, all voice modes and limit boundaries.
- Verify persistence/relaunch behavior and long localized labels.
- Confirm no backend, watch transport or storage behavior was accidentally changed.

Deliverable: complete Android phone settings in the new design.

## Batch 5 — Android identity assets and cleanup

Implementor:
- Install the prepared adaptive/background/foreground/monochrome and fallback icons in the Android application resource directory.
- Update only the phone application's manifest icon and roundIcon references.
- Align Android startup/launch background with the identity using the platform's existing launch configuration.
- Keep common in-app marks and launcher mark geometry consistent.
- Remove superseded presentation code, unused gradients, old visual constants and resources after Studio usage analysis proves they are no longer needed.
- Retain behavior utilities and identity-color functions that still have genuine callers; remove old presentation, not working functionality.

Reviewer gate:
- Inspect the merged phone manifest and launcher resources, regular/themed launcher appearance and startup transition.
- Verify cleanup removed the superseded UI without touching unrelated modules.

Deliverable: a complete phone identity, with no maintained old-design implementation.

## Batch 6 — Android acceptance and final review

Implementor:
- Run Studio inspections and the Android phone build; run the existing app-core regression tests relevant to source selection, group state and persistence.
- Extend focused UI tests using deterministic state/callback fixtures. Add only the minimal Compose test dependencies/harness needed by those tests.
- Keep the existing AndroidStartupTest passing, including the foreground-service assertion.
- Exercise source/voice/profile/limit interactions and meaningful restoration/error paths.
- Capture native Android screenshots for the agreed visual matrix and store them under `design/implementation/android/`.
- Re-run the design asset validation if exported assets or tokens changed.
- Record build/test commands, results, remaining limitations and screenshot references.

Visual and interaction matrix:
- Light and dark system appearance.
- Compact phone, standard phone and landscape.
- Default and large system font; longer German/Italian copy.
- Gesture navigation, system Back, keyboard/IME, correct status/navigation insets.
- Within/above limit, missing/expired reading, empty and multi-member groups.
- Source unavailable/reconnecting and permissions denied.
- TalkBack labels, minimum 48 dp touch targets, visible selection and no color-only status.
- Regular and themed launcher icons.

Reviewer final gate:
- Review the full accumulated diff, not only the last patch.
- Compare actual Android captures with the approved guide and explain intentional differences: three speech modes, the retained limit range, truthful source states and immediate preference persistence.
- Verify every acceptance criterion with evidence; distinguish an untested device scenario from a passing test.
- Send actionable findings back to the implementor and re-review the fixes.
- Finish only after required checks pass and review findings are resolved, or clearly report a concrete external blocker.

## Completion criteria

- The Android phone launches directly into the new Shared Rhythm UI.
- Shared Compose screens/theme replace the old design; no compatibility mode preserves it.
- Both color schemes, local fonts, mark, navigation, real-data Live screen and settings are implemented.
- Source selection, three voice modes, name/color/limit changes and backend startup still work.
- Missing/stale values and individual limits are represented truthfully.
- Android phone icons and launch appearance match the kit.
- Relevant tests/builds and the independent reviewer agree the change is ready.
- The final handoff links native screenshots and records any Android validation limitations.

iOS receives the common-source changes naturally. Dedicated iOS polish, icon installation and acceptance, Wear OS UI work, web implementation and plans for those platforms are deferred to later requests. This phase requires no separate iOS design or legacy-preservation work.
