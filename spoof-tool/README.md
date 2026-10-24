# Spoof Tool

The spoof tool contains a Compose Desktop control app and the Kotlin/Native macOS BLE advertiser it controls.

On an Apple Silicon Mac, start the desktop app with hot reload enabled:

```shell
./gradlew :spoof-tool:hotRunJvm --auto
```

The Gradle task builds the native advertiser before launching the desktop app. Use the controls to set the test identity, heart rate, and heart-rate limit, then start or stop advertising. The BLE advertiser only runs while the desktop app is running and advertising is enabled.
