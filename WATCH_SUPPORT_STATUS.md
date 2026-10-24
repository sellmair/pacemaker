# Watch companion support

## Current product behavior

Pacemaker supports Android Wear OS use as a companion to its paired phone. The watch can start or stop its own live measurement, display its heart rate and limit, show nearby group readings, and vibrate for critical readings. The phone manages the selected heart-rate source and nearby group sharing. The current release creates no new workout-history entries; previously stored phone history and database rows remain intact.

There is no watch-only or standalone mode and no mode picker.

## Android transport

Wear OS uses Google Play services Wearable Data Layer messages and capability discovery. The phone and watch applications share the package ID and signing identity required by Wear OS. The operating system may carry messages over a direct connection or its encrypted Google relay. Pacemaker does not send heart-rate samples through the persistent Data Layer API or store watch history.

The phone continues using Bluetooth LE for its existing chest straps and group sharing. The Wear application does not use custom Bluetooth pairing, MAC-address selection, GATT fragmentation, or an idle connected-device foreground service. The phone Settings source card selects a watch node and exposes one Connect or Disconnect action.

Phone disconnect uses an ordered, persisted measurement Stop queue with stable IDs, immutable node/run targets, and application-level acknowledgements. A message being queued by Google Play services is not treated as a completed measurement stop. Watch Start remains local to the watch and does not start phone session recording.

## Apple Watch status

Apple Watch changes are deferred from the current Android implementation scope. Existing iOS files remain in the repository and need separate implementation review and device validation before claiming supported behavior.

## Freshness and storage

Watch heart-rate values older than 15 seconds are cleared. Group readings shown on a watch expire after 60 seconds. Watch readings and group snapshots are transient; Pacemaker does not maintain a watch-side run history. No watch GPS data is collected.

## Validation record

- Latest automated validation passed: `:app-core:allTests` (JVM, Android host, and iOS simulator shared-code targets), `:wearApp:lintProfile`, debug and profile builds for both `:wearApp` and `:androidApp`, followed by Android Studio's project build. The lint task completed successfully; compiler output contains non-blocking warnings.
- The final profile APKs were installed on both Pixel devices. Explicit watch Start produced 65 BPM on both watch and phone; final phone startup instrumentation passed (1 test). Phone Disconnect cleared own HR while an unrelated participant remained. Final watch Stop/service-end observation was blocked when Wi-Fi ADB dropped during the command/capture.
- Session recording UI, hidden feature toggle, and Android watch session-recording commands were removed. Existing SQL history is preserved; no session actor starts new recording.
- On the preceding simplified Data Layer profile build, physical testing verified automatic companion discovery, selected-source display, live HR reaching the phone while the watch was dozing, watch Start/Stop, source Disconnect clearing HR and ending the workout foreground service, and reconnect remaining idle. These checks predate the last run-ID tombstone/ACK bookkeeping edits; the final-build evidence above is narrower.
- The phone UI was verified without session/history controls while the old hidden Sessions preference was enabled. The card exposes only Connect/Disconnect. Production recording was removed by source review; physical database row counts were not independently compared.
- The Wear screen is scrollable and keeps Start fully visible within the round display.
- Wi-Fi ADB to the watch has been intermittent. Its original screen timeout was restored and verified as 10000 ms. Earlier custom-BLE tests are historical and do not validate this transport.
- Apple Watch physical pairing and long-duration endurance remain unverified.
- See [WATCH_TESTING.md](WATCH_TESTING.md) for the automated coverage assessment, reusable manual scenarios, final APK hashes, precise evidence boundaries, and remaining device checks.
