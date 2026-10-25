# Android watch testing

This is the handoff checklist for humans and agents. Current scope is an Android phone plus a Wear OS companion using the existing OS pairing and Wearable Data Layer. Apple Watch, standalone watch use, and session recording are outside this scope. Watch Start/Stop controls live heart-rate measurement; the phone selects the source and shares nearby readings. Existing database history must remain intact.

## How to use this file

1. Record the build and environment below.
2. Run the automated baseline, then the relevant manual scenarios.
3. Record PASS, FAIL, NOT RUN, or BLOCKED for each scenario, with evidence. A missing device, missing fault-injection tool, or lost ADB connection is BLOCKED, not PASS.
4. Repeat affected scenarios after code changes. Results from an older APK do not validate a newer APK.
5. Stop test measurements, restore settings, and record remaining limitations.

Agents must follow AGENTS.md: use Android Studio MCP for supported project/code/build/test operations, with explicit projectPath. Use ADB for device operations. Discover device serials and debugging ports each time; do not reuse stale addresses. Check the foreground app before tapping: a sleeping watch or a person using the phone can move the UI elsewhere.

## Automated strategy assessment

The layered approach is sensible: fast deterministic rules/actor tests, build and lint checks, a physical phone startup smoke test, and paired-device testing. It is not yet an automated Wear OS integration suite.

Current inventory:

| Layer | What it checks | What it does not establish |
| --- | --- | --- |
| app-core common tests | One-minute source freshness, own-source filtering, codecs, peer selection, measurement Stop run guard, and pure watch snapshot mapping | Android/Wear services, real sensor or transport |
| app-core Android host tests | Group state, SQL user behavior, phone watch-message validation, selection races, and persisted Stop delivery through a controlled store and transport | Wear listeners, Health Services, permission/foreground restrictions, rendered UI |
| AndroidStartupTest on a phone | MainActivity resumes and backend reaches foreground | Permission onboarding: it pre-grants permissions. Also not watch Start/Stop, live HR, UI removal, or recording prevention |
| Debug/profile builds and Wear lint | Compilation, packaging, static checks | Runtime permission grants, actual exercise end, radio delivery, round-screen usability |
| Shared JVM/iOS simulator runs | Portability of shared assertions | Independent additional scenarios or Apple Watch support |

The 2026-10-24 test counts describe an older source tree. Use fresh reports for the current case count, and do not count the same shared assertions on JVM, Android host, and iOS simulator as distinct scenarios.

Important gap: host tests exercise the production phone message actor and Stop delivery with controlled collaborators, while real Wear listeners, radio delivery, and Health Services remain paired-device checks. The run gate's helper test alone does not establish those platform outcomes.

### Automated baseline

Run these through Android Studio's terminal/build tools. The broad allTests task also runs configured shared-code targets such as the iOS simulator; that is not Apple Watch validation.

```sh
./gradlew :app-core:allTests :wearApp:lintProfile :wearApp:assembleDebug :androidApp:assembleDebug :wearApp:assembleProfile :androidApp:assembleProfile
```

Inspect the exit status and report contents, not just APK existence:

- app-core/build/test-results/ and app-core/build/reports/tests/
- wearApp/build/reports/lint-results-profile.html
- androidApp/build/outputs/apk/profile/androidApp-profile.apk
- wearApp/build/outputs/apk/profile/wearApp-profile.apk

Run AndroidStartupTest via the Studio test configuration on the selected phone. If the matching instrumentation APK is already installed, this ADB command runs it against the installed target app:

```sh
adb -s PHONE_SERIAL shell am instrument -w -r -e class io.sellmair.pacemaker.AndroidStartupTest io.sellmair.pacemaker.test/androidx.test.runner.AndroidJUnitRunner
```

A clean device needs the test APK built/installed first; the command above does not do that. Use profile builds for performance assessment. A debug build's frame timing is not release-like evidence.

The existing CI workflow invokes ./gradlew check assemble. Before treating CI as the full Android watch gate, inspect its task graph/reports to confirm the Wear tasks actually ran. Physical pairing/sensor scenarios are a separate gate.

### Highest-value follow-up automation

These are coverage gaps, not completed tests. Prefer testing actual production handlers/coordinators with injected transport, clock, persistence, and exercise operations over another parallel helper implementation.

1. Phone receiver: Stop(A), late HR(A), Start(B), late clear(A); clear before first HR; wrong peer/user; future/stale samples. Assert selected state, tombstones, notification/broadcast inputs, and current run are correct.
2. Persisted Stop queue: immutable node/run/message target, restart reload, ACK from wrong node, ACK after deselection, ACK enriching a previously unknown run, retries/backoff, and offline A not blocking selected B configuration.
3. Wear lifecycle: delayed Stop(A) while B is active must not call endExercise or stopSelf for B; failed end must retain the health foreground service and report failure; recovering an owned exercise after process recreation must really end it.
4. Permission and UI instrumentation: staged grants/denials on API 33-35 and 36+, source-selected persistence on reopen, no recording/history/Details/Forget even with legacy preferences, round/large-font layout, idle 60-second group expiry.
5. A paired-device smoke harness: real OS transport and Health Services on profile builds. This supplements, rather than replaces, deterministic fault injection.

Do not add elapsed-time sleeps to unit tests to simulate freshness. Use an injected clock and controlled coroutine scheduling. Do not count legacy recording tests toward current measurement coverage.

## Test environment and execution record

Copy this block for each run:

```text
Date/time/timezone:
Tester (human or agent):
Commit + dirty-tree description:
Phone APK SHA-256 / variant:
Watch APK SHA-256 / variant:
Phone model / API / app version:
Watch model / API / app version / screen size:
OS companion pairing / Play services state:
Locale / font scale:
Selected HR source / other sensors or participants:
Network (Bluetooth, Wi-Fi, internet) / power state:
Original screen timeout and any other changed settings:
Scenario ID:
Status: PASS | FAIL | NOT RUN | BLOCKED
Actions actually performed:
Observed result / elapsed times:
Evidence paths or links (screenshots, logs, service dump):
Failure/reproduction notes:
Cleanup and settings restored:
```

Use matching phone/watch package ID io.sellmair.pacemaker and matching signing certificates. The Wear launch component is io.sellmair.pacemaker/io.sellmair.pacemaker.wear.WearActivity. Preserve phone data when updating; do not uninstall/clear storage to make an upgrade test pass. Use a dedicated clean fixture for first-install tests.

For physical HR tests, the wearer must keep the watch on the wrist and respond to health permission prompts. Keep screenshots/logs with the test record; the temporary /private/tmp paths in the checkpoint below are local evidence, not durable repository attachments.

## Manual scenarios

For every scenario, use the execution record above. A successful UI message or a queued MessageClient send is insufficient evidence that Health Services ended.

### M01 — OS pairing and simple source selection

Prerequisites: phone and watch already paired by Wear OS; matching Pacemaker apps installed; no active measurement.

1. Open both apps, then phone Settings.
2. Find the watch among nearby heart-rate sources and select Connect.
3. Reopen the watch app without pressing Start.

Expected: the correct OS-paired watch appears; no custom Bluetooth pairing or phone chooser; the card has only Connect/Disconnect; the watch becomes ready and stays idle with no measurement foreground service. No recording/history controls, standalone mode, or Apple setup appears. Capture both screens and watch service state. Cleanup: leave a known source selection.

### M02 — Upgrade, old preferences, and no recording

Prerequisites: upgrade fixture with existing phone history and, if available, the former Sessions preference enabled. Capture a database/history baseline using an approved test fixture or debug export.

1. Update both apps in place, preserving data.
2. Visit all phone tabs/settings and reopen the watch.
3. Perform a short Start/Stop and restart the apps.

Expected: only current live/settings UI; no session/history tab, recording button, hidden recording toggle, Details, or Forget. Stored history is preserved and no new recording entries are created. Check database counts with an available fixture/export; if unavailable, mark the database assertion NOT RUN rather than inferring it from hidden UI. Cleanup: retain existing history.

### M03 — Health permission grant, denial, and revocation

Prerequisites: dedicated first-use fixture; record API level. Run API 33-35 and API 36+ separately.

1. Start from the watch and deny heart-rate access.
2. Retry and grant foreground access; handle background access as a separate stage where required.
3. Deny background access, then grant it and retry.
4. Stop, revoke health access in OS Settings, and try Start again.

Expected: actionable permission status, no crash/false running state/phone HR on denial; successful retry measures. API 33-35 uses BODY_SENSORS and separate background access; API 36+ uses health heart-rate/background grants. Also exercise notification denial independently. Do not use the startup test's pre-granted permissions as evidence. Cleanup: restore the wearer's original grants and stop measurement.

### M04 — Start and live heart rate

Prerequisites: selected, connected watch, idle, on wrist, required grants.

1. Scroll to the complete Start button and tap once.
2. Observe startup, first real sensor sample, and several subsequent updates.
3. Compare watch/phone readings and configured limit.

Expected: one active health workout, live HR on both devices, same user identity and limit; no session recording or duplicate own participant. Small sample-to-sample timing differences are acceptable; a fixed cached value alone is not proof of delivery. Record first-sample latency and evidence. Cleanup: M05.

### M05 — Watch Stop and late-reading protection

Prerequisites: M04 active.

1. Tap watch Stop.
2. Inspect the watch health service/exercise state and phone reading.
3. Wait beyond one minute.
4. Start a new measurement and stop it again.

Expected: actual Health Services end, health foreground service removed, own phone HR unavailable, unrelated group members retained, no old HR reappears. Start can be used again. Use a service/exercise observation plus UI; lost ADB means the service-end assertion is BLOCKED. Cleanup: idle.

### M06 — Phone Disconnect, including before first sample

Prerequisites: selected watch; run once while streaming and once immediately after Start before first HR.

1. Press Disconnect on the phone while keeping the watch app closed or screen off.
2. Inspect phone HR, watch measurement state, and exercise/foreground-service outcome.
3. Reconnect the source without pressing Start.
4. Reopen the watch before its first measurement after upgrade/fresh install.

Expected: phone clears own HR immediately; dedicated Stop reaches the original watch and ACK reflects actual end; retries finish even though the selected source has cleared. The stopped run cannot repopulate HR. Reconnect stays idle; persisted source selection lets the watch become ready without getting stuck unselected. Cleanup: idle, selected or disconnected as recorded.

### M07 — Offline Stop, retry, and restart

Prerequisites: M04 active; controllable transport outage. Turning off watch Wi-Fi alone may leave managed Bluetooth transport working.

1. Interrupt all phone/watch transport without deleting OS pairing.
2. Disconnect the source on the phone.
3. Restart the phone app/process, then restore transport.
4. Observe retry and actual watch exercise end.
5. Repeat with the Stop ACK lost after the watch has ended.

Expected: own phone HR clears; persisted exact node/run/message Stop survives restart; duplicate delivery is safe; ACK loss does not lose the stop or create a new measurement; no recording begins. Capture network state and evidence of actual outage. Cleanup: restore radios/pairing and idle state.

### M08 — Source isolation and source switching

Prerequisites: selected watch A plus a chest strap, and optionally a second authenticated watch/node B.

1. Switch watch A to the strap while A measures.
2. Confirm the strap supplies this phone's own HR and A stops.
3. Test selected A offline while another node B is reachable.
4. Where the fixture permits, select B while A's Stop is pending.

Expected: no automatic takeover from A to B, wrong-node readings/ACKs are ignored, B configuration is not blocked by offline A, and pending A Stop never targets or clears B. Another participant's strap remains visible. Mark second-watch assertions BLOCKED if the fixture lacks B. Cleanup: restore original source and stop test workouts.

### M09 — Reordered messages and measurement run isolation

Prerequisites: a controlled message replay/fault-injection fixture; none is currently supplied by the app UI. Do not forge production messages or claim a radio toggle reproduces a specific ordering.

Replay these exact sequences at the production receiver/service boundary:

1. Stop(A), then a fresh-timestamp HR(A).
2. Start(B), then clear/Stop(A).
3. Clear(A) before A's first HR, then HR(A).
4. Pending Stop(A) while B becomes selected; delayed ACK from A and a wrong-node ACK.
5. A null-run Stop whose successful ACK supplies the actual ended run ID.

Expected: stopped A stays rejected, B's state/exercise survives, only matching peer/message consumes the pending Stop, and the enriched ACK drains the queue. Assert actual prefs/current run/selected HR and service end calls. Until a fixture exists, record BLOCKED and prioritize the automated receiver/queue tests above. Cleanup: remove test messages and idle all fixtures.

### M10 — Screen off, UI closed, and short recovery

Prerequisites: M04 active; profile builds.

1. Turn watch screen off and leave its UI.
2. Verify it is actually dozing and phone HR continues across several samples.
3. Reopen menus and return to the app.
4. Briefly interrupt and restore transport.

Expected: measurement and live delivery continue without an open Activity; no unexpected new measurement on reconnect. Expired readings clear and new fresh data recovers, including the same numerical BPM. ADB disappearing when Wi-Fi sleeps does not prove the app transport disconnected. Cleanup: stop measurement and restore changed display settings.

### M11 — Local one-minute and remote 60-second freshness

Prerequisites: own watch sample and a distinct nearby participant.

1. Stop delivering own sensor samples without continually refreshing their timestamps.
2. Observe phone own reading, notification, and nearby broadcast after one minute.
3. Stop the other participant's updates and observe group expiry after 60 seconds.
4. Restore fresh samples, including a BPM identical to the old value.

Expected: own cached HR clears after the one-minute freshness window; remote group values expire after 60 seconds; no false timestamp refresh from cached color/limit updates; identical fresh BPM returns. Record original sample timestamps and observation times. Cleanup: restore transport and stop fixtures.

### M12 — Idle group screen and critical haptics

Prerequisites: watch connected, measurement stopped, separate participant with controllable readings/limit.

1. Open the watch group view and verify the participant name/BPM.
2. Send a fresh critical reading, then stop sending.
3. Leave the watch measurement idle and wait beyond 60 seconds.
4. Repeat while transport is unavailable.

Expected: fresh group data and rate-limited critical haptics; old group HR expires even without a running measurement service; stale data does not keep triggering alerts. Long names do not hide BPM. Cleanup: restore participant limit and stop test alerts.

### M13 — Round display, locales, fonts, and performance

Prerequisites: physical round watch; profile build. Record original font/locale settings.

1. Exercise main/group screens in English, German, and Italian.
2. Repeat with the largest supported font setting and a long user/participant name.
3. Scroll both directions and reach Start/Stop fully inside the circle.
4. Capture frame timing during repeated navigation and while live HR updates.

Expected: all actions reachable, scroll works, text/BPM readable, no mode picker/phone chooser; performance is usable on wrist. Record frame counts/jank/percentiles alongside subjective observation; a short warmed benchmark does not establish all-screen smoothness. Cleanup: restore locale/font settings.

### M14 — Process recovery and real exercise outcomes

Prerequisites: dedicated fixture and observable Health Services exercise state.

1. Interrupt/recreate the watch process during measurement without treating OS force-stop as ordinary process death.
2. Reopen, then request Disconnect from the phone.
3. Where supported by the test fixture, simulate start/end failure and another app owning an exercise.
4. Deliver stale Stop(A) while a new B measurement is active.

Expected: no orphan Pacemaker exercise, no takeover of another app's workout, truthful failure/ACK, retained health FGS if end failed, stale Stop does not destroy B's service. Mark failure injection BLOCKED without a suitable seam. Cleanup: verify all Pacemaker test exercises actually ended.

### M15 — Endurance and group regression

Prerequisites: wearer available, charged devices, profile builds, normal power settings; at least one nearby participant and a strap where possible.

1. Run at least 60 minutes with screens mostly off and normal phone use.
2. Observe live delivery, group exchange, recovery, haptics, battery/thermal behavior, and responsiveness.
3. End through watch Stop and repeat phone Disconnect in a separate short run.

Expected: no unexplained data gaps beyond documented transport recovery, stale readings clear, no stuck exercise/notification, no new recording. Record sampling intervals and dropouts rather than asserting uninterrupted delivery from two screenshots. Cleanup: stop all measurements; restore settings. This scenario has NOT RUN at the checkpoint below.

## Useful device evidence commands

Replace serial placeholders with current adb devices -l results. Prefer direct file output for large dumps and screenshots.

```sh
adb devices -l
adb mdns services
adb -s WATCH_SERIAL shell am start -n io.sellmair.pacemaker/io.sellmair.pacemaker.wear.WearActivity
adb -s WATCH_SERIAL shell dumpsys activity services io.sellmair.pacemaker
adb -s WATCH_SERIAL shell dumpsys power
adb -s WATCH_SERIAL shell dumpsys gfxinfo io.sellmair.pacemaker reset
adb -s WATCH_SERIAL shell dumpsys gfxinfo io.sellmair.pacemaker
adb -s WATCH_SERIAL shell settings get system screen_off_timeout
adb -s WATCH_SERIAL exec-out screencap -p > watch.png
```

Record/restore the actual original timeout, not a guessed default. On the checkpoint device it was 10000 ms, temporarily 120000, and restored/verified as 10000. A wake command cannot reach a watch whose Wi-Fi debugging is offline; reconnect Wi-Fi/debugging and rediscover its port. Wear OS app transport can still work while Wi-Fi ADB is absent.

## Checkpoint evidence — 2026-10-24, Europe/Berlin

Devices: Pixel 10 Pro and Pixel Watch 3, both API 37. This was a dirty working tree; use the artifact hashes to identify the final tested binaries.

- Final phone profile SHA-256: 1bcee575382dd77c04c093e41efefdeb9345202584cd419030a377a54711f322
- Final Wear profile SHA-256: 123ed5caa14aed1cddacfb5fe76d78043f894d72b4fb1f49ac92582db6b3dc0c
- Final automated baseline and Android Studio build passed; final source review reported no blocking Android findings.
- Final phone APK physical AndroidStartupTest: PASS, 1 test.
- Final APKs installed on both devices: PASS. Automatic companion connection and round-safe scroll to full Start: PASS.
- Final explicit Start: live 65 BPM simultaneously on watch and phone: PASS. Local evidence: /private/tmp/pacemaker-final-watch-live.png and /private/tmp/pacemaker-final-phone-live.png.
- Final phone UI/source card: only live/settings and Connect/Disconnect; old Sessions flag had previously been enabled: PASS. Final Disconnect cleared own HR while the unrelated participant remained: PASS; /private/tmp/pacemaker-final-disconnected.png.
- Final watch Stop/service-end regression: BLOCKED by Wi-Fi ADB dropping during the command/capture. Do not infer that the tap or exercise end succeeded.
- Preceding simplified APKs, before the final tombstone/ACK bookkeeping changes: watch Stop removed its service and cleared phone HR; confirmed-dozing live delivery; phone Disconnect ended the watch service; reconnect remained idle. PASS on that preceding snapshot only.
- Fresh permission denial/revocation matrix, controlled message-reordering/ACK-loss fixture, all locale/large-font combinations, and 60-minute endurance: NOT RUN in this final checkpoint.
- Original watch display timeout restored and read back as 10000 ms: PASS. Final phone source was disconnected. Final watch exercise end was not observable after ADB dropped; verify idle state at the next device session.

Append new run records rather than rewriting older evidence as if it belonged to a new APK.
