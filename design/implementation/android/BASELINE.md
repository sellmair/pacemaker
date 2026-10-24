# Android baseline

- Date: 24 October 2026.
- Existing Android build: passed via Android Studio build_project before UI changes (implementor).
- Existing installed UI captured on Pixel 10 Pro: `baseline.png`.
- Repeatable test device: `Medium_Phone_API_35`, headless emulator, serial `emulator-5554`.
- Existing app and Android test APKs installed independently onto the emulator.
- Existing `AndroidStartupTest.launchesActivityAndKeepsBackendForegroundServiceRunning`: passed, 1 test, including foreground service assertion.
- Pre-existing workspace changes: staged/modified implementation plan, modified skills-lock.json, untracked agent skill files. Preserve these.

The baseline instrumentation run used the existing test APK. The final acceptance run must rebuild the test APK against the new UI.

## Capture and test notes

Use `android screen capture --device SERIAL -o PATH` and visually inspect every capture. Choose the explicit USB serial for the physical phone because it also appears over wireless.

Install the app and test APKs with separate `android install` calls. Passing both together is interpreted as split APKs and fails with duplicate base split errors.

The final focused UI tests should render stateless production content with controlled data and callbacks. Keep sample values out of the production data path. Prefer the emulator for preference/permission scenarios, preserving the physical phone's saved profile and sensor choice.
