# Finish Shared Rhythm on iPhone

Approved in chat 25 October 2026. Baseline master `19469896488a5899a4fa5f242bf332368eec197d`. Status: implementation and final independent review complete; commit identity reported in delivery.

## Contract and team

GPT-6 Luna high implements; GPT-6.1 Sol high independently reviews read-only. Parent owns sequencing,builds,simulators,evidence and physical checkpoints. Production edits serialized with implementor. Use Android Studio MCP with explicit repository projectPath for supported operations; never IDEA. All Gradle commands use installed gradle-run including Xcode integration.

Finish iPhone adoption using existing shared Compose UI: ApplicationWindow,PageRouter and plain Live/Devices/Profile content/state/callbacks. Strictly follow approved `design/previews/app-screens-dark.png`,`app-screens-light.png`,`live-above-limit.png`,`live-signal-lost.png`. Preserve brand header and three direct tabs. No invented sections,slogans,decorative pulse or old-design switch. Actual bundled Barlow Condensed and Inter,fixed palettes,system appearance.

Preserve immediate preferences,name validation,identity colors,40–200bpm editing range,three voice modes,source selection,timestamp freshness,individual limits/group totals,current phone threshold policy and baseline connection-gap behavior. No backend,storage,protocol,session or permission ownership changes. Preserve WatchPhoneBridge activation and launch-help alert. Fixture route is Debug-only,does not instantiate backend/storage/Bluetooth/watch services; Release always launches production.

iPhone-first: retain existing iPhone-only,portrait-only settings and deployment floor. No tablet layouts or watchOS redesign. Guard Android phone/Wear presentation. User authorizes periodic useful checkpoint installs on physical iPhone15Pro,preserving app data. No TestFlight/App Store publication.

## Tasks and gates

| ID | Depends on | Work and acceptance |
| --- | --- | --- |
| T1 | none | Shared deterministic phone scheme,minimal XCUITest target,managed Xcode Gradle step preserving env/signing/errors,isolated Debug fixture using plain composables/fixedclock/immutable inputs/callbacks. Real native app launch and settled-font fixture with actual accessible control action required before screen work. |
| T2 | T1 | iOS hosting,status/background,safe-area and keyboard/menus,tab/input/scroll restoration. Fix evidenced defects; reviewer compares native captures. |
| T3 | T2 | Complete all3screens and states,DynamicType through accessibilityXXXL,VoiceOver labels/values/selection/adjustable controls,ReduceMotion. Prefer shared fixes with Android regression protection. |
| T4 | T1 | Supplied default/dark/tinted icon catalog,AccentColor and native launch surfaces. Verify installed icons/startup;watch assets unchanged. |
| T5 | T3,T4 | Usage-checked cleanup,complete native matrix/integrated checks,cumulative independent review,focused repairs,evidence and commit. |

Native cases: tabs and retained state;keyboard/name editing/blank rejection;production preferences across relaunch;limit +/-/slider bounds,threevoice modes,color;Auto/sensor/watch unavailable/permissions/Bluetooth states;normal/equal/above/no-limit/missing/future/expired samples;stale/duplicate self group;background/resume freshness;watch launch-help. Fixtures prove presentation/callbacks separately from production wiring.

Native matrix: SE3,16Pro,16ProMax;light/dark;EN/DE/IT;default/accessibilityXXXL. All3screens both themes standard phone;compact/large pressure including keyboard Profile and scrolled controls;above-limit/signal-lost/unavailable sources/long translations. Inspect iPad compatibility mode without enabling tablet target.

## Validation

From repo root through gradle-run: `:app:linkDebugFrameworkIosSimulatorArm64`,`:app:linkReleaseFrameworkIosArm64`,`:app-core:testAndroidHostTest`,`:androidApp:assembleDebug`,`:wearApp:assembleDebug`. Affected shared UI changes require affected existing Android native tests/captures. Reuse passing evidence unless inputs change.

Native command base: `xcodebuild -project iosApp/Pacemaker.xcodeproj -scheme Pacemaker-iPhone -configuration Debug -destination 'platform=iOS Simulator,id=918CA124-EAE6-4336-AD26-42650E5CA826' -derivedDataPath /private/tmp/pacemaker-ios-design`. Run `build-for-testing`,then `test-without-building`;repeat focused matrix cases. Generic Release device build signing disabled. All actual commands/results retained in delivery record.

No toolchain/deployment-floor/backend redesign without revising contract. Real Bluetooth/Apple Watch/speech/physical VoiceOver require hardware;report any unverified cases. Completion requires cumulative reviewer acceptance,evidence,passing required checks and commit.

## Execution corrections

- T1–T5 are complete. See [results](RESULTS.md), [independent review](REVIEW.md) and [native capture index](matrix.json). The final candidate integrates external BLE fix `58b01c0` and passed renewed core, Bluetooth and platform checks.
- The user confirmed iPhone-only scope. The target was already family 1. No tablet layout or artificial hardware restriction was added; an iPad compatibility launch was inspected.

- Shared scheme named Pacemaker-iPhone because pre-existing user-only iosApp.Pacemaker scheme shadows a shared scheme with identical name;user scheme unchanged.
- Native name input is XCTest TextView and sliders labeled Other elements. Verify actual semantics and actions rather than assuming UIKit roles.
- External reset+checkout at12:25:05 removed T1 work while HEAD remained baseline. User explicitly authorized restoration and asked to report recurrence. Restored candidate receives focused renewed validation;passing artifact evidence alone does not certify recreated source.
