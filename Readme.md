# Pacemaker (iOS & Android) [KMP]

Run together! A running companion monitoring the heart rate of a group of people doing sports together.

## Supports

- External Bluetooth (LE) heart rate monitors (tested with Polar H10)
- iPhone <-> iPhone connections (No internet necessary, BLE)
- Android <-> Android connections (No internet necessary, BLE)
- iPhone <-> Android connections (No internet necessary, BLE)

## Planned

WatchOS support (via UWB chip and Internet)

## Screenshots

<p float="left">
    <img src="/.img/screenshot-ios-1.png" alt="Screenshot iOS" width="250">
    <img src="/.img/screenshot-ios-2.png" alt="Screenshot iOS" width="250">
    <img src="/.img/screenshot-android-1.png" alt="Screenshot iOS" width="250">
</p>

## Install

- Google Play: https://play.google.com/store/apps/details?id=io.sellmair.pacemaker
- Apple App Store: https://apps.apple.com/us/app/pacemaker-heart-rate-monitor/id6446760560

# Technical Details

## Kotlin Multiplatform

This application is built as a Test/Dogfooding project for Kotlin/Multiplatform, Compose and JetBrains Fleet.

## Architecture

### No! ViewModels!: This project uses 'State Actors' instead.

The State Actor pattern used in 'Pacemaker' can be defined by two high level concepts:

#### Events

Every component in the application can emit any kind of event, including intents.
Example: Some UI button that emits an event to the application

```kotlin
@Composable
fun MyButton() {
    Button(
        onClick = Launching { LoginIntent.emit() }
    ) {
        // ...
    }
}
```

#### State Producers

States can be produced and observed. Lets look at the producing site first:<br>
Lets take the classic login example:

```kotlin
data class LoginState(val email: String, val password: String, val isLoggedIn: Boolean) : State {
    companion object Key : State.Key<LoginState> {
        val default get() = LoginState(email = "", password = "", isLoggedIn = false)
    }
}

fun CoroutineScope.launchLoginStateActor() = launchStateProducer(LoginState) {
    var state = LoginState.default

    collectEventsAsync<EmailChangedEvent> {
        state = state.copy(email = it.email)
        state.emit()
    }

    collectEventsAsync<PasswordChangedEvent> {
        state = state.copy(password = it.password)
        state.emit()
    }

    collectEventsAsync<LoginIntent> {
        val isLoggedIn = attemptLogin(state.email, state.password)
        state = state.copy(isLoggedIn = isLoggedIn)
        state.emit()
    }
}
```

Such states can then be used in the Application UI/Frontend easily

```kotlin
@Composable
fun MyLoginScreen() {
    val loginState by LoginState.collectAsState()
    MyLoginScreen(
        email = loginState.email,
        password = loginState.password
    )
}

@Composable
fun MyLoginScreen(
    email: String,
    password: String
) {
    Text(email)
    Text(password)
    Button(
        onClick = Launching { LoginIntent.emit() }
    ) {
        Text("Login")
    }
}
```

### Libraries used

- kotlinx.coroutines
- kotlinx.datetime
- SQLDelight
- Multiplatform Settings
- Okio


