# Obsolete UI cleanup proposal and usage audit

This is the Batch 5 cleanup usage audit. The initial Studio deletion request was rejected before changing files. After an independent read-only usage audit and a verified recovery archive, one coherent Studio patch removed the exact 20 listed files; no piecemeal deletion was used.

## Proposed deletion set

1. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/mainPage/GroupHeartRateScale.kt`
2. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/mainPage/MyStatusHeader.kt`
3. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/mainPage/UtteranceControlButton.kt`
4. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/settingsPage/HeartRateSensorCard.kt`
5. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/settingsPage/SettingsPageContent.kt`
6. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/settingsPage/SettingsPageHeader.kt`
7. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/ChangeableMemberHeartRateLimit.kt`
8. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/ColorHueSlider.kt`
9. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/GradientBackdrop.kt`
10. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/Headline.kt`
11. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/HeartRateScale.kt`
12. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/HeartRateSensorIcon.kt`
13. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/MemberHeartRateIndicator.kt`
14. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/MemberHeartRateLimit.kt`
15. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/OnHeartRateScalePosition.kt`
16. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/OnHeartRateScaleSpring.kt`
17. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/widget/UserHead.kt`
18. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/icons/HeartRateSensorIcons.kt`
19. `app/src/commonMain/kotlin/io/sellmair/pacemaker/ui/icons/WatchIcons.kt`
20. `app/src/androidMain/kotlin/Previews.kt`

The submitted patch contained 20 paths (the initial automatic-review message said 19). This document retains the exact submitted path list and records that count discrepancy.

## Studio usage evidence

- `SettingsPageContent` has no incoming project call sites. Its own `SettingsPageDevicesList` references `HeartRateSensorCard`, and the latter also appears in `HeartRateSensorPreview` in `app/src/androidMain/kotlin/Previews.kt`.
- `SettingsPageHeader` has no incoming project call sites. It alone references `ColorHueSlider` and the old circular `UserHead` overload. Current `ProfilePageContent` owns name and hue updates through its state/callback API.
- `MyStatusHeader` has no incoming project call sites. It references `UtteranceControlButton` and old sensor icons; the new Live and Profile content directly exposes speech state and its three choices.
- The call hierarchy for `MemberHeartRateIndicator` leads only to the unused `GroupHeartRateOverview` implementation in `GroupHeartRateScale.kt`. The old scale helpers `OnHeartRateScalePosition`, `heartRateOfY`, `yOfHeartRate`, `ScaleSide`, and `onHeartRateScaleSpring` have usages only among these obsolete presentation files.
- `GradientBackdrop` is called only by the obsolete `HeartRateSensorCard`; `Headline` and `TextStyle.Headline` are called only by their own implementation and the unused settings header.
- Search results for `WatchIcons` and `HeartRateSensorIconState` are limited to the old settings UI, old icon wrappers/vector definitions, and the Android previews. New screens use `PacemakerIcons`.
- The specific directory `app/androidMainPreviews` does not exist. Studio's Android preview source is `app/src/androidMain/kotlin/Previews.kt`; its two previews exercise only the legacy sensor card and old sensor-state icon widget.
- The proposal does not include `app-core`, models, Evas intents/state, identity color conversion, or the current `MeState` name-abbreviation helper. The new settings screens continue to use those behavior/data contracts.

## Automatic review response

> This action was rejected by automatic review.
> Reason: Deleting 19 shared UI, icon, and preview source files is a broad destructive change; although replacing the old Android design is authorized, the exact deletion scope and preservation of reusable behavior are not sufficiently authorized or proven safe.
> Do not bypass this rejection through a workaround or indirect execution. Continue with a safer alternative, or carry out checks to prove that the action is authorized or low risk before trying again.

## Independent reviewer safety audit — 25 October 2026

GPT-6.1 high independently read all 20 files and enumerated 29 declared entry/helper/type names. A project-wide Studio regex search across Kotlin, Java, Swift, XML and Gradle Kotlin files, excluding only the exact 20 proposed paths, returned zero references with `more=false`. No current UI, Android/iOS host, Wear module, test or build script calls this cluster.

Incoming call hierarchy confirmed that SettingsPageContent and SettingsPageHeader have no callers; ColorHueSlider is called only by the removed header, GradientBackdrop only by the removed sensor card, MemberHeartRateIndicator only by the removed group overview, and scale/spring helpers only by removed scale widgets. Old sensor icon callers are confined to the old card/header/devices list/previews. Ambiguous overloaded call targets were not counted as hierarchy proof; the exhaustive external-reference search covers them.

The reviewer verified retained intent wiring in SettingsPage for source selection, explicit connect/disconnect, name, limit, hue and all three speech modes, plus MainPage speech selection. UserColors, color conversion, name abbreviation, Launching, Evas/state actors, storage and transport files are outside the proposed deletion set. Previews.kt imports and previews only the removed widgets.

The exact 20 paths form one coherent obsolete presentation cluster. The requested recovery archive was created at `/private/tmp/pacemaker-obsolete-ui-before-cleanup.tar.gz`; Studio terminal `tar -tzf` verified exactly these 20 entries. After the independent audit and archive check, the complete deletion patch succeeded. Android rebuild and affected tests are pending with the parent.
