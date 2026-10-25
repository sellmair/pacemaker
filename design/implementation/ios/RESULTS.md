# iPhone design delivery

25 October 2026. Implemented by GPT-6 Luna high; independently reviewed by GPT-6.1 Sol high. Approved phone design baseline: `1946989`. Delivery integrates the unrelated BLE reconnect fix already committed as `58b01c0`.

## Delivered

The existing shared Live, Devices and Profile screens now have native iPhone evidence and a repeatable XCUITest fixture. Debug fixtures use fixed time and immutable inputs without constructing production services; Release always opens production. Shared fixes make limit and hue sliders expose finite accessibility steps and keep the German waiting header readable at accessibility text sizes. Stored fractional values do not change until an action.

Supplied default, dark and tinted icon masters are installed in the iPhone asset catalog. Accent and launch backgrounds follow the approved identity. The old unassigned app icon was removed after usage inspection. Native status bars, safe areas, keyboard, menus, tab draft retention and actual nonzero scroll restoration were checked.

Native target support remains iPhone-only, portrait, with the existing deployment floor. No iPad layout work was added. An iPad compatibility-mode launch and screenshot were inspected successfully using a generic simulator build. Apple explains that removing iPad as a supported destination does not prevent compatibility downloads, and required hardware capabilities must be real application dependencies. [Apple compatibility guidance](https://developer.apple.com/help/app-review/before-submitting-for-review/test-compatible-devices).

## Native visual evidence

[Browse captures](CAPTURES.md), or use the [matrix index](matrix.json): 24 passing configurations, 144 original native PNGs. Each configuration has Live opening, Live peers, Devices, Profile opening, Profile limit and Profile lower captures.

| Size | Simulator | Logical size | Coverage |
| --- | --- | --- | --- |
| Small | iPhone SE 3, iOS 18.2 | 375 × 667 | EN default; EN/DE/IT accessibility XXXL; light/dark |
| Standard | iPhone 16 Pro, iOS 18.4 | 402 × 874 | EN default; EN/DE/IT accessibility XXXL; light/dark |
| Large | iPhone 16 Pro Max, iOS 18.4 | 440 × 956 | EN default; EN/DE/IT accessibility XXXL; light/dark |

Language is applied to the application process with AppleLanguages/AppleLocale; tests assert translated titles. Fonts are settled before capture. The reviewer inspected all configuration contact sheets and pressure-case originals against the approved previews. Fixture captures prove presentation and callbacks, not physical sensor transport.

Focused evidence: [iOS German waiting state](captures/focused/ios-small-german-xxxl-waiting.png), [Android regression](captures/focused/android-german-large-text-waiting.png), [dark startup filmstrip](captures/focused/startup-dark-filmstrip.png). The startup filmstrip predates the final waiting-header correction; it verifies unchanged launch surfaces only.

## Validation results

[Native results](native-results.json) retain bounded test/build events, without raw build logs or physical personal screenshots.

| Check | Result |
| --- | --- |
| Native behavior suite | 7 tests: 6 passed, 1 expected skip, 0 failures; 122.730 seconds |
| Dedicated small-screen nonzero scroll restoration | Passed after final test readiness repair; 11.606 seconds. The standard-screen skip occurred because its content fitted without scrolling. |
| Final threshold/freshness/peer/source scenarios | Passed after the shared header correction; 42.220 seconds |
| Visual matrix | 24 tests passed; 144 captures |
| Xcode Gradle helper branch tests | 3 passed; mocked wrapper/direct/failure paths |
| Android affected Live class | 20 tests, 0 failures, 0 ignored |
| Android Profile/component/preferences/navigation coverage | 19 passed in the initial 20-test run; repaired new slider test passed focused. No unresolved failure. |
| Signed Debug app and simulator test host | Build passed |
| Generic Release device app | Build passed; Debug fixture launch flag absent |
| Physical iPhone | Latest signed app installed and launched; existing data retained |

Native behavior tests cover keyboard/draft retention, actual source and voice-mode callbacks, limit boundaries, timestamp and peer cases, and production name persistence across relaunch with original value restored. Finite slider semantics are additionally exercised by Android SetProgress actions using the native accessibility bridge's increment calculation. This is not a claim that physical VoiceOver gestures were manually performed.

Managed gradle-run aggregate question: **“Do the delivery candidate iOS frameworks core Bluetooth tests and Android phone and Wear builds all pass together?”** Answer: **yes, exit 0**, 2.375 seconds, no failed tasks:

```text
:app:linkDebugFrameworkIosSimulatorArm64
:app:linkReleaseFrameworkIosArm64
:app-core:testAndroidHostTest
:bluetooth:jvmTest
:androidApp:assembleDebug
:androidApp:assembleDebugAndroidTest
:wearApp:assembleDebug
```

Xcode helper question: **“Does the Xcode Kotlin phase package the Apple framework with Xcode's build environment?”** Answer: **yes, exit 0** on the final generic compatibility build. Java native-access warnings remain informational. All agent Gradle calls used the managed wrapper. The workflow was finished after validation; finish removed only wrapper-owned logs.

## Launch setup

Use Android Studio configuration **iOS (iPhone)** or Xcode scheme **Pacemaker-iPhone** in this checkout. The actual Studio configuration built, installed and opened production on the physical phone. Xcode had previously been showing a different, older checkout under SideProjects; that checkout was left untouched. See [self-service launch instructions](../../../iosApp/README.md).

The Xcode phase owns framework preparation through the helper, including when KMP supplies its override flag. No personal device ID or temporary workflow identifier is stored in shared configuration.

## Remaining physical checks

Real BLE reconnection, Apple Watch measurement/interoperability, speech delivery, background session behavior and physical VoiceOver gestures were not certified by the simulator fixtures. Existing service/session ownership remains unchanged. The default installed icon and startup were inspected; all three appearance masters compiled from the supplied assets, but installed dark/tinted icon appearances were not independently captured.

## Checkout recovery

An external reset/checkout at 12:25 removed the test foundation. The user authorized restoration, which was followed by renewed source/build/native validation. A later external fast-forward at 12:46 brought in `58b01c0`; the reviewer inspected the BLE-only integration and renewed aggregate checks passed. No further checkout interference was observed before delivery.
