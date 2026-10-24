# Android phone implementation baseline

## Environment and starting state

- Android Studio MCP project path: /Users/Sebastian.Sellmair/StudioProjects/pacemaker.
- Shared UI is in app/src/commonMain; Android launches it from MainActivity.
- The Android Studio build_project baseline passed with no problems.
- Existing AndroidStartupTest passed on the Medium_Phone_API_35 emulator (1 test), including its foreground-service assertion. The physical Pixel 10 Pro was left unchanged after its baseline screenshot.
- MainActivity already calls enableEdgeToEdge. Its service binding and permission path are outside Batch 1.
- Inter and Barlow Condensed binaries and OFL license texts are in design/fonts/.
- Pre-existing user work was present: the implementation plan was staged and modified, skills-lock.json was modified, and .agents/skills/ contained untracked files. All are preserved.
- Physical-device baseline: design/implementation/android/baseline.png (captured by parent).
- Batch 1 Android build passed after the shared theme, Compose fonts, icons and controls were added. The font generator exposed accessors for Inter and Barlow; both font binaries are now Compose resources, and the two OFL notices are bundled as raw resources.

## Existing state and event contracts

- MeState provides current user, nullable heart-rate sample, stored limit, and sample epoch time.
- GroupState is a list of UserState values. The actor retains peer entries for one minute, while source samples are accepted as fresh for 15 seconds.
- SelectedHeartRateSourceState holds an optional HeartRateSource with kind, availability, optional sample/time, and companion identity. HeartRateSourceSelectionIntent persists the choice. A disconnected explicitly selected watch must not be replaced implicitly.
- HeartRateSensorConnectionState reports per-sensor connection intent and BLE connection state. HeartRateSensorConnectionIntent.Connect/Disconnect are separate from source-selection events.
- WatchCompanionDeviceState distinguishes paired and connected and carries candidate/device availability.
- UtteranceState is the three-value Silence, Warnings, All enum; its default is All.
- Participant color is represented by MeColorState and UserColors. It is identity data, not the brand action palette.

## Batch checklist

- [x] Establish Android baseline build and preserve pre-existing work.
- [x] Replace dynamic/light-only Material roles with fixed light/dark Shared Rhythm palettes.
- [x] Package licensed Inter and Barlow fonts as shared Compose resources.
- [x] Add theme tokens, identity/status separation, and reusable stateless components.
- [x] Provide Compose-native vector marks and required small UI icons.
- [x] Build the Android target; batch inspections are recorded in the implementation handoff.
