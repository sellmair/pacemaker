# Android phone implementation — accepted

Completed 25 October 2026. GPT-6 Luna (high) implemented the shared Compose redesign; GPT-6.1 (high) independently reviewed the cumulative code and native screenshots against the approved previews.

The sole phone UI now has three direct tabs: Live, Devices and Profile. Common sources contain the fixed dark/light palette, bundled typography, mark, flat Live meter/member rows, source cards and profile controls. Android owns its adaptive/themed launcher and launch background. Twenty obsolete presentation/preview files were removed after independent usage analysis and a recovery archive; no old-design route or switch remains.

## Results

| Check | Result |
| --- | --- |
| Android app and instrumentation APK build | Passed after cleanup |
| Full native regression suite | 58 passed before removal of the independently proven unused cluster |
| Post-cleanup settings/navigation/launcher/original startup suite | 30 passed |
| Actual landscape, including font scale 1.6 and lower-control scrolling | 3 passed after cleanup |
| app-core Android host regressions | 29 passed; backend code unchanged |
| Design export validation | 40 images and native asset compilers passed |
| Independent cumulative code and visual review | Accepted, no open actionable findings |
| Pixel 10 Pro | Stable checkpoints installed throughout; final APK installed/opened over wireless ADB |

The native suites include fresh/expired/future/cleared measurements, self deduplication, retained peers, missing limits, meter-domain expansion, all three speech modes, direct tabs/restoration/Back, selected device identity, connection versus selection, unavailable watch platform, BLE reconnect presentation, fractional stored limits and blank-name validation. A real-Activity integration test confirms name, limit, hue and speech settings survive recreation through the actual Evas routes, and keyboard dismissal precedes route Back.

## Native previews

These are Android captures of production components with controlled sample state, except `baseline.png`, which records the original installed phone UI.

| View | Files |
| --- | --- |
| Live | `live-within-limit-light-native.png`, `live-above-limit-dark-native.png`, `live-no-local-signal-dark-native.png` |
| Devices | `devices-dark-native.png`, `devices-light-native.png`, `devices-selected-offline-watch-native.png`, `devices-bluetooth-off-native.png` |
| Profile | `profile-dark-native.png`, `profile-light-native.png` |
| Compact/large font | `live-compact-large-font-*-native.png`, `profile-compact-large-font-*-native.png` (see exact filenames in this folder) |
| German/Italian at font scale 1.6 | `profile-german-large-font-initial-native.png`, `profile-german-large-font-scrolled-native.png`, `devices-italian-large-font-initial-native.png`, `devices-italian-large-font-scrolled-native.png` |
| Landscape | `profile-dark-native-landscape.png`, `profile-compact-large-font-initial-native-landscape.png`, `profile-compact-large-font-scrolled-native-landscape.png`, `devices-dark-native-landscape.png` |
| Installed adaptive/themed icon layers | `launcher-adaptive-native.png`, `launcher-monochrome-native.png` |
| Components/startup | `components-dark-native.png`, `components-light-native.png`, `startup-permission-dark-native.png`, `startup-permission-light-native.png` |

The icon proofs draw the installed manifest's real adaptive and monochrome resources through Android's native mask. They do not claim a particular launcher's wallpaper/theme setting.

## Reproduction

All commands were run through Android Studio MCP terminal with explicit projectPath:

```sh
./gradlew :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest :app-core:testAndroidHostTest --console=plain
android install --device emulator-5554 --apks androidApp/build/outputs/apk/debug/androidApp-debug.apk --install-options=-g
android install --device emulator-5554 --apks androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk --install-options=-g
adb -s emulator-5554 shell am instrument -w -r io.sellmair.pacemaker.test/androidx.test.runner.AndroidJUnitRunner
```

The full suite targets the ten test classes listed in `final-native-tests.log`. Focused runs used the runner's `-e class` filter. Landscape runs set emulator rotation before launching the fixtures and passed `-e captureSuffix landscape`; original rotation settings were restored afterward. Test locale changes are process-local and restored after each test.

## Evidence limits

Controlled fixtures verify UI/state presentation and callbacks, not physical sensor/watch interoperability. Live hardware pairing, measurement transport and outdoor readability were not newly certified. Startup denied-state captures use controlled content; the original integration test exercises the granted-permission foreground service. Accessibility labels, selection, touch-target sizing and non-color status cues were inspected/tested; a full interactive TalkBack session was not performed.

Dedicated iOS/Wear acceptance and icon installation are deferred. Shared UI changes naturally affect other hosts. Physical Pixel preferences were preserved; controlled preference edits and permission grants ran only on the emulator.

See `ACCEPTANCE_MATRIX.md`, `DEPLOYMENTS.md`, `../REVIEW_LOG.md` and `../OBSOLETE_UI_CLEANUP.md` for decisions and audit history.
