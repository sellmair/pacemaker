# Architecture map

Pacemaker shares user, group, and heart-rate source rules in `app-core`. Phone, Wear OS, and iOS entry points supply platform transport and sensor services.

## Phone

`ApplicationBackend.launchApplicationBackend` starts the shared state actors. Events and States from Evas travel in the backend coroutine context. `AndroidApplicationBackend` owns the foreground service, backend lifetime, binder, and incoming Intent decoding.

The Android watch path has three owners in `app-core/src/androidMain`:

- `WatchCompanionMessageActor` validates the selected node, user, run, and sample age before publishing a wrist reading.
- `WatchCompanionStatePublisher` maps current user, color, group, and selection state into watch snapshots.
- `WatchMeasurementStopDelivery` persists ordered Stop commands. Each command keeps its original node, message ID, generation, and run target. It leaves the queue only after an accepted ACK from that node for that message. Its retry job belongs to the backend scope.

`WatchCompanionDataLayer` discovers capabilities, sends to explicit nodes, and publishes discovery through Evas. The phone listener service forwards incoming Intents; `AndroidApplicationBackend` decodes and publishes inbound messages. The transport consumes ACK events. Selection policy stays with the phone actors. The pure snapshot mapping and companion protocol are in `app-core/src/commonMain`.

## Watches

On Wear OS, `WearCompanionRuntime` owns the process-local Evas Events and States. The system listener can apply durable configuration and group messages while the Activity is closed. `WearActivity` and `WearWorkoutService` share that store, with collectors and expiry timers bound to their own lifetimes. `WearWorkoutState` is the Evas state used by the UI. Health Services owns the local exercise and heart-rate callbacks; a measurement run ID protects it from delayed Stop messages.

On Apple Watch, `WatchSessionModel` uses a local HealthKit workout for live sensor samples and sends transient readings through WatchConnectivity. `WatchPhoneBridge` passes live readings, configuration, and group updates to the iOS Kotlin bridge. There is no phone/watch session-recording command flow.

## Stored data

`SqlUserService` and `User.sq` still store user profiles, sensor links, and heart-rate limits. Source selection and watch preferences use platform settings. The current code no longer writes workout sessions or a time series of heart rates; existing phone database files are left in place. The phone's watch Stop queue remains in `watch-companion` preferences so delivery survives process recreation.

## Tests and evidence

`HeartRateSourceTest` and `WatchCompanionSnapshotsTest` cover shared rules and pure snapshot mapping. `WatchCompanionActorsTest` exercises the phone message actor, selected-peer checks, and Stop delivery with controlled transport and store. `SqlUserServiceTest` covers live profile persistence. `WATCH_TESTING.md` separates automated checks from paired-device scenarios and records which observations used older APKs.
