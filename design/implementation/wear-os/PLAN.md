# Shared Rhythm for Wear OS — execution contract

Approved by the user on 25 October 2026. Baseline: commit 2da8e45. Status: implemented, verified and independently accepted. Results and native evidence are in RESULTS.md. Execution feedback: deploy meaningful stable checkpoints to the user's Pixel Watch when reachable, and explain what is ready to try.

## Team and ownership

- Implementor: GPT-6 Luna, high. Own Wear production Kotlin, resources/build configuration and focused host unit tests.
- Reviewer: GPT-6.1, high. Read-only independent code and native visual review.
- Parent: owns coordination, emulator/device safety, native instrumentation fixtures, Gradle execution and evidence.
- All supported code operations use Android Studio MCP with explicit projectPath /Users/Sebastian.Sellmair/StudioProjects/pacemaker. Never use IDEA tools.
- Run every Gradle command through the installed gradle-run skill wrapper. One execution owner (parent); no concurrent agent builds.
- Preserve unrelated changes while implementing. User later requested committing the completed watch delivery, followed by a separate appropriate commit for the remaining reviewed files. No push requested.

## Approved design and behavior

Closely follow design/previews/wearable.png and the brand/font masters in design. Charcoal opening canvas; small Your heart rate label; dominant Barlow Condensed number; separate bpm/limit line; concise status/group summary. Inter supporting text. No phone title bar, extra slogan, bottom tabs or decorative pulse.

Normal reading acid, exceeded limit coral, within-limit status mint. Missing/expired/future values use a dash with explanation. Preserve the watch's >= limit critical/haptic policy; equality says At your limit, greater says Above your limit.

Connect, Start and Stop keep their existing controller/service behavior. Starting disables repeated starts; Stop remains available during an active workout even if phone connectivity disappears. Put actions and group access BELOW the main reading, reachable by touch scrolling and crown. Keep the existing two-screen structure; group details support Close, system Back and swipe-to-dismiss, returning to the main screen without stopping a workout. Save navigation and scroll positions.

Group details use received names, bpm and individual limits, critical-first ordering and the existing twelve-member display cap. Label total/shown accurately when capped. Deduplicate identities. Derive display freshness from timestamps with HeartRateSource.MAX_AGE (15 seconds), BEFORE critical ordering. Retained membership is not a live reading. Main wrist measurement and phone connectivity remain separate. Do not change transport fields to invent participant colors.

Ambient uses black and muted monochrome summary of workout/connectivity and limit. Omit live bpm in ambient rather than showing a frozen value as current. No high-frequency ambient UI timer. Keep measurement services running and restore the interactive view on wake.

Configuration stays phone-managed. No standalone mode, on-watch settings editor, Tiles, complications or phone/iOS redesign. Preserve WearWorkoutService, companion authentication, permission sequencing, foreground-service ownership, session commands, storage and vibration behavior. No transport/persistence schema change.

## Ordered tasks

### T1 — foundation and native harness
Depends on: none.
Update Wear Material 3, foundation and tooling to verified stable 1.7.1 without changing Kotlin/AGP/SDK levels. Current compileSdk37/AGP9.3.1 satisfy compatibility. Remove unused direct mobile Material3 dependency. Add watch-local theme and bundled fonts. Minimal native test harness uses explicit testApplicationId io.sellmair.pacemaker.wear.test.
Provision a round Wear emulator; prove themed scaffold renders and fonts are real before dependent screens. Native sample state exists in tests only.

### T2 — main reading and controls
Depends on: T1 proof.
Split WearActivity platform wiring from plain WearHomeContent. Explicit immutable state/clock/ambient inputs and callbacks; no app graph required for rendering. Keep one AppScaffold, Wear ScreenScaffold and TransformingLazyColumn, preserving scaffold padding and crown scrolling. Lifecycle-aware state collection; display clock only interactive/visible. Implement reference hierarchy and truthful values. Reviewer compares actual native captures with reference before closing.

### T3 — group and navigation
Depends on: T2.
Plain WearGroupContent with explicit state/clock/callbacks. Flat readable rows, names/units/limits, freshness-before-critical ordering, unique IDs, honest capped totals. Saveable existing two-route state and per-route list positions. Native Close/Back/swipe return without stopping or duplicating service actions; no new navigation framework.

### T4 — ambient and identity
Depends on: T2.
Ambient manager at Activity boundary; black monochrome summary, no live bpm/actions/polling. Install supplied adaptive/monochrome/fallback resources in wearApp only; watch-local launch background and proper monochrome notification small icon. Keep phone resources untouched.

### T5 — final acceptance and obsolete presentation cleanup
Depends on: T3 and T4.
Usage-audit and remove superseded watch presentation. Build/test, capture native evidence, independently review accumulated change; fix and renew affected evidence until accepted. Preserve useful old behavior and backend utilities.

## Validation

Focused fixtures: below/equal/above limit; absent limit; missing/expired/future readings; fresh wrist reading with disconnected phone and usable Stop; unconfigured/not-selected/health unavailable/permissions denied; starting/start-stop failures; group ordering/staleness/dedup/empty/capped totals; callbacks, crown, Close/Back/swipe/restoration; ambient entry/wake without duplicate service actions or a high-frequency timer.

Native matrix: small/large round screens, default/large fonts, EN/DE/IT. Capture opening and scrolled controls, inspect circle clipping/units/labels/touch targets and installed adaptive/monochrome icons/startup. Controlled fixtures do NOT prove physical sensor interoperability.

Integrated Gradle tasks, via gradle-run wrapper from repository root:
:wearApp:assembleDebug
:wearApp:assembleDebugAndroidTest
:wearApp:testDebugUnitTest
:app-core:testAndroidHostTest
:androidApp:assembleDebug

Install app and test APK separately ONLY on a verified Wear device. Runner:
adb -s <verified-wear-serial> shell am instrument -w io.sellmair.pacemaker.wear.test/androidx.test.runner.AndroidJUnitRunner

Use focused reruns after repairs; no repeated broad builds without a new concern/change. Parent records bounded wrapper results, screenshots and review outcomes in this folder.

## Device safety and delivery

Phone and watch share applicationId io.sellmair.pacemaker. BEFORE EVERY install, verify ro.build.characteristics contains watch. NEVER install Wear app APK on Pixel 10 Pro or phone emulator. No physical watch was connected at planning; use round Wear emulator, install stable checkpoints on a physical watch when available, record actual transport/measurement/haptics checks as unverified without hardware.

The planning baseline :wearApp:assembleDebug passed through gradle-run (exit0); temporary wrapper-owned logs were removed on finish. Preserve the approved phone design and brand masters without adding a dependency on the phone UI module. Material pairing/permission/session/haptic-policy changes require revisiting this contract; routine layout and fixture repairs do not.
