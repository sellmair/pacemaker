# Watch companion support

## Current product behavior

Pacemaker supports Android Wear OS use as a companion to its paired phone. The watch can start or stop its own live measurement, display its heart rate and limit, show nearby group readings, and vibrate for critical readings. The phone manages the selected heart-rate source and nearby group sharing. The current source creates no new workout-history entries; previously stored phone history and database rows remain intact.

There is no watch-only or standalone mode and no mode picker.

## Android transport

Wear OS uses Google Play services Wearable Data Layer messages and capability discovery. The phone and watch applications share the package ID and signing identity required by Wear OS. The operating system may carry messages over a direct connection or its encrypted Google relay. Pacemaker does not send heart-rate samples through the persistent Data Layer API or store watch history.

The phone continues using Bluetooth LE for its existing chest straps and group sharing. The Wear application does not use custom Bluetooth pairing, MAC-address selection, GATT fragmentation, or an idle connected-device foreground service. The phone Settings source card selects a watch node and exposes one Connect or Disconnect action.

Phone disconnect uses an ordered, persisted measurement Stop queue with stable IDs, immutable node/run targets, and application-level acknowledgements. A message being queued by Google Play services is not treated as a completed measurement stop. Watch Start remains local to the watch and does not start phone session recording.

## Apple Watch status

The iOS and watchOS code exchanges live configuration, group snapshots, and heart-rate readings through WatchConnectivity and HealthKit. Paired-device behavior still needs its own validation; the Android device results below do not establish Apple Watch support.

## Freshness and storage

The phone treats its selected heart-rate source as stale after one minute. Apple Watch clears its local HealthKit reading after 15 seconds without a fresh sample. Group readings shown on a watch expire after 60 seconds. Watch readings and group snapshots are transient; Pacemaker does not maintain a watch-side run history. No watch GPS data is collected.

## Validation record

- The automated and physical results below predate the current actor and recording cleanup. They describe the APKs used at that checkpoint and must be repeated for a final device claim.
- The final profile APKs were installed on both Pixel devices. Explicit watch Start produced 65 BPM on both watch and phone; final phone startup instrumentation passed (1 test). Phone Disconnect cleared own HR while an unrelated participant remained. Final watch Stop/service-end observation was blocked when Wi-Fi ADB dropped during the command/capture.
- Session recording code and schema generation have since been removed. The existing phone database file is not deleted; profile data still uses SQLDelight.
- On the preceding simplified Data Layer profile build, physical testing verified automatic companion discovery, selected-source display, live HR reaching the phone while the watch was dozing, watch Start/Stop, source Disconnect clearing HR and ending the workout foreground service, and reconnect remaining idle. These checks predate the last run-ID tombstone/ACK bookkeeping edits; the final-build evidence above is narrower.
- The phone UI was verified without session/history controls while the old hidden Sessions preference was enabled. The card exposes only Connect/Disconnect. Production recording was removed by source review; physical database row counts were not independently compared.
- The Wear screen is scrollable and keeps Start fully visible within the round display.
- Wi-Fi ADB to the watch has been intermittent. Its original screen timeout was restored and verified as 10000 ms. Earlier custom-BLE tests are historical and do not validate this transport.
- Apple Watch physical pairing and long-duration endurance remain unverified.
- See [WATCH_TESTING.md](WATCH_TESTING.md) for the automated coverage assessment, reusable manual scenarios, final APK hashes, precise evidence boundaries, and remaining device checks.
