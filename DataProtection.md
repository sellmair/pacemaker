# Pacemaker – Datenschutzerklärung / Privacy Policy

Pacemaker keeps previously stored workout history on the phone. The current Wear OS companion sends live readings and measurement stop requests to that phone. This release does not create new Pacemaker workout-history entries. The watch does not keep Pacemaker workout history, and Pacemaker does not use a Pacemaker cloud account to synchronize history. Apple Watch support is deferred from the current Android release scope.

### Data shared with nearby Pacemaker users

During a group session, nearby users can receive the current heart rate, configured heart-rate limit, display name, color, and a random user ID. Phone-to-phone group communication uses Bluetooth Low Energy.

### Watch companion data

The paired phone remains responsible for nearby group sharing. Wear OS sends live readings and measurement control messages through Google's Wearable Data Layer. The operating system may route Data Layer messages through its encrypted internet relay when a direct nearby connection is unavailable. This is managed by Google Play services; Pacemaker does not choose or retain a separate cloud route.

Live watch readings are sent as transient messages. Pacemaker does not queue heart-rate samples for later delivery. The phone drops watch readings older than 15 seconds. Group snapshots shown on a watch expire after 60 seconds.

### Health and workout data

Wear OS reads heart rate during an active Health Services workout. The workout is required by the platform to receive continuous heart-rate data. Pacemaker ends its exercise when the user stops; it does not save a Pacemaker workout or synchronize watch history. Wear OS manages its own sensor and workout data under the user's system settings.

The watch companion does not collect location or GPS data.
