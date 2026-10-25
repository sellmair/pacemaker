# App Store upload package

See COPY.md for the complete English, German and Italian copy. Plain field files are under metadata/<locale>.

1. Enter name, subtitle, promotional_text, description and keywords into the matching fields.
2. Upload screenshots/en-US/iphone-1206x2622/01–05 in order to the matching Dynamic Island medium-display slot.
3. Upload screenshots/en-US/iphone-1320x2868/01–05 to the matching large-display slot.
4. The app icon normally comes from the signed build's AppIcon catalog. graphics/app-icon-1024.png is the matching default master for reference.
5. Use whats_new.txt for the Shared Rhythm update only. First releases do not use that field.
6. Supply the real support/privacy URLs, review contact and copyright owner in App Store Connect. Complete App Privacy and other declarations from the release build.

The screenshots are native iPhone UI with controlled example data; the marketing surround is flat HTML/CSS artwork. The Debug test fixture is not a production review/demo mode. No iPad set is included because the native target is iPhone-only.

This package covers iPhone. If shipping the existing embedded watchOS companion, add genuine current Apple Watch screenshots to its slot. The Android Wear OS images are not Apple Watch evidence.

Screenshot requirements checked 25 October 2026:
https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/
