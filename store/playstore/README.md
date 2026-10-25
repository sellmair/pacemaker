# Google Play upload package

See COPY.md for the complete English, German and Italian copy. Plain field files are under metadata/<locale>.

1. Upload graphics/app-icon-512.png as the listing icon.
2. Upload graphics/<locale>/feature-graphic-1024x500.png as the feature graphic.
3. Add screenshots/en-US/phone-1080x1920/01–05 in numeric order to the phone slot.
4. Add screenshots/<locale>/wear-os/01–03 to the Wear OS slot when distributing the watch app.
5. Use ALT_TEXT.tsv for image descriptions. Complete the console's publisher/contact/privacy and app-content fields.

Phone screens are English; German/Italian feature graphics translate the headline around an English UI sample. Wear captures use each locale's actual app strings. The watch files are square, unframed and opaque. All samples are controlled native captures.

Use release_notes.txt only when describing this design update. Do not submit the code fixture as a demo mode.

Verified asset guidance: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en
