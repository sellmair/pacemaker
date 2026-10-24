# Pixel preview deployments

The user requested ongoing stable checkpoints on their Pixel 10 Pro during implementation. The phone is reachable through its existing wireless ADB pairing after USB removal.

## 24–25 October 2026

1. Installed and opened the shared-theme/three-tab/initial Live checkpoint over wireless ADB. Full install succeeded (66.16 MB).
2. Installed and opened the refined preview-matching Live checkpoint over wireless ADB. Delta install succeeded (2.51 MB).
3. Installed and opened the final Live typography/accessibility checkpoint over wireless ADB. Full install succeeded (66.55 MB); Android brought the existing task to the front.
4. Installed and opened the Devices/Profile checkpoint with the BLE reconnect correction after 21 native settings checks passed. Wireless delta install succeeded (3.93 MB).
5. Installed and opened the final visual/launcher checkpoint after the full 58-test native suite passed. Wireless delta install succeeded (3.37 MB).
6. Installed and opened the final APK after obsolete UI cleanup, a fresh build, 30 post-cleanup native checks and 3 landscape checks. Wireless delta install succeeded (9.88 MB); Android brought the existing task to the front.

Deployment command: `android run --device <paired-wireless-serial> --apks androidApp/build/outputs/apk/debug/androidApp-debug.apk --activity io.sellmair.pacemaker.MainActivity` through Android Studio terminal.

Implementation is complete. Emulator fixtures remain the source of controlled measurement and preference-test evidence; Pixel preferences were preserved.
