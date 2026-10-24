# Navigation update during execution

The user requested direct access to the three screen cases. The phone UI will use three bottom destinations:

- Live — current reading and group.
- Devices — Your signal; sensor/watch source selection.
- Profile — Make it yours; identity, limit and speech settings.

Remove the intermediate Setup destination and its Devices/Profile selector. Preserve the selected bottom tab across recreation. Back dismisses an open keyboard before leaving its screen; Back from Devices or Profile returns to Live. The shared implementation is used by Android without maintaining old navigation.

This execution decision supersedes the two-tab navigation in the original HTML concept.
