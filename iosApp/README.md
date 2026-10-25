# Run the current iPhone app

The iPhone app hosts the shared Compose screens from this checkout. Use the same checkout in Android Studio and Xcode: different checkouts have the same application bundle ID and can overwrite each other's installed app.

## Android Studio / KMP

1. Open the repository root in Android Studio.
2. Select the shared **iOS (iPhone)** run configuration.
3. Choose your connected iPhone or an iPhone simulator, then Run.

The configuration is committed under `.run/`. It uses the current project's Xcode integration and the shared **iOS (iPhone)** scheme. The phone target remains portrait-only and iPhone-only. Device destinations stay local to your IDE; no personal device ID is committed.

## Xcode

From the repository root:

```sh
open iosApp/Pacemaker.xcodeproj
```

Select **iOS (iPhone)**, choose your iPhone, and Run. **Pacemaker-iPhone** is also a shared phone scheme and includes the native UI test target for automated validation. Debug launches production normally; the controlled fixture route requires an explicit Debug-only UI-test argument.

Use the project's existing automatic signing team. The phone target embeds its existing watch companion, so Xcode may also need its signing profile. Bluetooth and HealthKit permissions remain managed by the existing app.

## Avoid stale builds

The Kotlin build phase always invokes `scripts/xcode_gradle.py`. It deliberately does not skip work when the KMP IDE supplies `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED`: that flag alone does not prove a fresh framework exists. Gradle still performs its normal up-to-date checks.

When the local managed Gradle skill is installed, the helper uses its compact-output workflow and retains failures for diagnosis. Ordinary Xcode builds also work without that local skill installation. Agent-owned builds supply a workflow explicitly and fail closed if the required wrapper is missing; a managed build failure never falls back to an unmanaged command.

If you see the old design, first inspect the Xcode window's full project path. During this implementation, Xcode had been open on a separate older `SideProjects/pacemaker` checkout while current work lived in `StudioProjects/pacemaker`. Open the project inside the checkout containing your latest work. Deleting app data is unnecessary and would discard preferences.

Native acceptance results and controlled captures are recorded in `../design/implementation/ios/`.
