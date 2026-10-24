# Android acceptance matrix — complete

| Contract | Evidence | Outcome |
| --- | --- | --- |
| Android phone build | Final app + instrumentation APK build after cleanup | Passed |
| Existing foreground backend startup | Original AndroidStartupTest in full and post-cleanup suites | Passed |
| Source/group/storage contracts | app-core Android host suite, 29 tests; unchanged backend | Passed |
| Live freshness, cleared own value, peers and self deduplication | SharedRhythmLiveTest, 16 tests | Passed |
| Missing limits, equality and meter expansion | Live native fixtures | Passed |
| All three speech modes | Live/Profile callback tests and actual Evas recreation integration | Passed |
| 40–200 editing and no render-time stored-value mutation | Component/Profile tests, including fractional out-of-range values | Passed |
| Valid name, local blank draft, identity hue | Profile tests and actual Activity recreation through Evas | Passed |
| Source selection distinct from connect/disconnect | Devices tests; retained intent wiring reviewed | Passed |
| Stable explicit BLE/watch identity and unsupported platform | Device callback regression tests | Passed |
| Saved BLE selection reconciles runtime connection | Production resolver regression tests | Passed |
| Three direct tabs, restoration, system Back | Navigation tests | Passed |
| Keyboard dismisses before route Back | Real-Activity integration test | Passed |
| Both themes and reference fidelity | Actual native captures and independent review | Accepted |
| Compact/large font, German/Italian | Native font-scale 1.6 fixtures and reviewed captures | Passed |
| Actual landscape and lower controls reachable | 3 post-cleanup native runs/captures | Passed |
| Android adaptive/monochrome launcher | Installed-resource native test and reviewed icon pixels | Passed |
| Legacy presentation removed | Independent zero-external-reference audit, exact 20-file removal, rebuild | Passed |
| Accessibility semantics and no color-only state | Code/native semantics review | Passed within stated scope; no full TalkBack session |
| Physical sensor/watch interoperability | No controlled fixture proves hardware transport | Unverified |
| Dedicated iOS/Wear acceptance | Outside Android phone phase | Deferred |

Full native suite: 58 passed. Post-cleanup focused suite: 30 passed. Landscape: 3 passed after cleanup. Core: 29 passed. Design exports: 40 images validated.

Raw logs and screenshots are in this directory. Final cumulative review by GPT-6.1 high accepted the code, screenshots, cleanup and validation evidence without unresolved actionable findings.
