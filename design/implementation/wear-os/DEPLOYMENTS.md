# Pixel Watch checkpoints

The user requested meaningful stable checkpoints on the Pixel Watch throughout implementation.

## 25 October 2026

1. Verified the physical Pixel Watch 3 reports `nosdcard,watch`, then installed/opened the T1 brand foundation APK over its existing wireless ADB connection. Standard install succeeded (37.49 MB). Physical display is 456 × 456. Preferences and backend behavior were preserved; no test fixture or fabricated measurement was installed as product content.
2. User authorized physical testing and screenshots with the watch on their wrist. Actual application capture showed the authenticated companion connection, with no workout measurement started at that point. This does not establish app sensor interoperability yet.
3. T2 native/reviewer gates passed. Deployment attempt stopped because the physical ADB transport disappeared before the characteristics guard. Discovery still advertised the prior watch endpoint, but connecting failed. No T2 installation took place; retry pending watch reconnection.
4. Watch reconnected. Reverified `nosdcard,watch`, installed T2 successfully and launched WearActivity. APK SHA256: `7eec947da2075815749c4456adf635aef4d432dcbb97528d2e99ec5c0e95b70a`. User notified to scroll/crown down to Start for real wrist measurement. A later capture showed system media controls, so it is not evidence of application heart-rate interoperability.
5. Reverified the physical watch and installed the reviewed T4 checkpoint, SHA256 `c5ae40e8b6cec737ea7edbf2a9bb1a3aab8a4f659e38d7390c6b35baebed6eb7`. Tested through actual application UI: Start became Stop and live wrist heart rate appeared with the configured limit. No runtime-permission override was used.
6. Entered actual dozing mode: the screen showed only the black/gray workout, connectivity and configured-limit summary. After 130.4 seconds, the workout service remained registered; wake restored a fresh wrist reading. Stop then returned the UI to Start and the workout service was no longer registered. Above-limit haptics and deliberate phone loss were not exercised.
7. Reverified `nosdcard,watch`, installed/opened the final reviewed APK, SHA256 `dc482344f8c723896769e8509c59320d952c862f3ab9f7ba0815ca1636d2fa15`. Inspected the real Ready screen: dash, configured limit and companion status were visible. Measurement was left stopped. Physical screenshots are local outside this repository.

Both watch and phone use applicationId `io.sellmair.pacemaker`. Every subsequent installation must repeat the watch-characteristics guard. Controlled native fixtures run on isolated Wear emulators; physical previews use real application state.
