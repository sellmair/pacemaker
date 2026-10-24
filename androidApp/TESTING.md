# Android app testing guide for agents

## End-to-end startup test

`AndroidStartupTest` launches the real `MainActivity`, then verifies that
`AndroidApplicationBackend` reaches foreground state. It grants the Bluetooth
runtime permissions required by the connected-device foreground service before
launching the app. The test does not require a paired heart-rate sensor.

Use an Android emulator or device with API level 31 or newer, then run from the
repository root:

```sh
./gradlew :androidApp:connectedDebugAndroidTest --console=plain --no-configuration-cache
```

## Run the built test APK directly

If Gradle builds the APKs but cannot start its connected test runner because a
host-side test-platform dependency cannot be downloaded, build the APKs and run
the Android instrumentation runner directly. The connected test task may already
have produced the APKs; otherwise build them with:

```sh
./gradlew :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest --console=plain --no-configuration-cache
```

With the device attached and `adb` available on `PATH`, install both APKs and
start the test runner:

```sh
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb install -r androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk
adb shell am instrument -w -r io.sellmair.pacemaker.test/androidx.test.runner.AndroidJUnitRunner
```

The run passes when the output ends with `OK (1 test)`. The instrumentation test
grants Bluetooth scan, connect, and advertise permissions plus notification
permission on Android 13 and newer before launching the activity.
