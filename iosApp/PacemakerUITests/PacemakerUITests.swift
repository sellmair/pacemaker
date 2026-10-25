import XCTest

final class PacemakerUITests: XCTestCase {
    func testNativeScreenCaptures() {
        let language = testLanguage
        let app = launchFixture(language: language)
        XCTAssertTrue(app.staticTexts["PACEMAKER"].waitForExistence(timeout: 15))

        let liveMetric = app.staticTexts["142"]
        XCTAssertTrue(liveMetric.waitForExistence(timeout: 10))
        attachScreenshot(named: "native-\(language)-live", app: app)
        let mila = app.staticTexts["Mila"]
        reveal(mila, in: app)
        XCTAssertTrue(mila.isHittable)
        attachScreenshot(named: "native-\(language)-live-peers", app: app)

        button(label: localized("Devices"), in: app).tap()
        let devicesTitle = app.staticTexts[localized("YOUR SIGNAL.")]
        XCTAssertTrue(devicesTitle.waitForExistence(timeout: 10))
        let sensorCard = app.staticTexts["Polar H10"]
        reveal(sensorCard, in: app)
        XCTAssertTrue(sensorCard.isHittable)
        attachScreenshot(named: "native-\(language)-devices", app: app)

        button(label: localized("Profile"), in: app).tap()
        let profileTitle = app.staticTexts[localized("MAKE IT YOURS.")]
        XCTAssertTrue(profileTitle.waitForExistence(timeout: 10))
        let displayName = app.textViews[localized("Display name")]
        reveal(displayName, in: app)
        XCTAssertTrue(displayName.isHittable)
        attachScreenshot(named: "native-\(language)-profile", app: app)

        let limit = app.otherElements[localized("Heart-rate limit")]
        reveal(limit, in: app)
        XCTAssertTrue(limit.isHittable)
        attachScreenshot(named: "native-\(language)-profile-limit", app: app)

        let voiceAlerts = app.staticTexts[localized("Voice alerts")]
        reveal(voiceAlerts, in: app)
        XCTAssertTrue(voiceAlerts.isHittable)
        attachScreenshot(named: "native-\(language)-profile-lower", app: app)
    }

    func testNonzeroProfileScrollPositionRestoresAfterTabSwitch() throws {
        let app = launchFixture(language: "en")
        button(label: "Profile", in: app).tap()
        let profileTitle = app.staticTexts["MAKE IT YOURS."]
        XCTAssertTrue(profileTitle.waitForExistence(timeout: 10))
        let voiceAlerts = app.staticTexts["Voice alerts"]
        guard !voiceAlerts.isHittable else {
            throw XCTSkip("Profile content fits at this device size; run at accessibility XXXL to exercise scroll restoration.")
        }

        reveal(voiceAlerts, in: app)
        XCTAssertTrue(voiceAlerts.isHittable)
        let visibleFrame = voiceAlerts.frame
        attachScreenshot(named: "profile-nonzero-scroll-before-tab", app: app)

        button(label: "Live", in: app).tap()
        button(label: "Profile", in: app).tap()
        XCTAssertTrue(voiceAlerts.waitForExistence(timeout: 10))
        XCTAssertTrue(voiceAlerts.isHittable)
        XCTAssertLessThanOrEqual(abs(voiceAlerts.frame.minY - visibleFrame.minY), 5)
        attachScreenshot(named: "profile-nonzero-scroll-restored", app: app)
    }

    func testFixtureVoiceModesAndHeartRateSourceChoicesActivate() {
        let app = launchFixture(language: "en")
        button(label: "Profile", in: app).tap()
        let voiceAlerts = app.staticTexts["Voice alerts"]
        reveal(voiceAlerts, in: app)
        XCTAssertTrue(voiceAlerts.isHittable)
        for (option, expected) in [
            ("Off", "Off"),
            ("Warnings only", "Warnings only"),
            ("All updates", "All updates")
        ] {
            voiceAlerts.tap()
            let menuOption = app.staticTexts[option]
            XCTAssertTrue(menuOption.waitForExistence(timeout: 5))
            menuOption.tap()
            XCTAssertTrue(app.staticTexts[expected].waitForExistence(timeout: 5))
        }

        button(label: "Devices", in: app).tap()
        let sensorAction = app.buttons["Use this sensor"]
        reveal(sensorAction, in: app)
        XCTAssertTrue(sensorAction.isHittable)
        sensorAction.tap()
        button(label: "Live", in: app).tap()
        let sensorFooter = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "Polar H10")).firstMatch
        reveal(sensorFooter, in: app)
        XCTAssertTrue(sensorFooter.exists)

        button(label: "Devices", in: app).tap()
        let watchAction = app.buttons["Use wrist watch"]
        reveal(watchAction, in: app)
        XCTAssertTrue(watchAction.isHittable)
        watchAction.tap()
        button(label: "Live", in: app).tap()
        let watchFooter = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "Apple Watch")).firstMatch
        reveal(watchFooter, in: app)
        XCTAssertTrue(watchFooter.exists)

        button(label: "Devices", in: app).tap()
        let automaticSource = button(label: "Automatic", in: app)
        reveal(automaticSource, in: app)
        XCTAssertTrue(automaticSource.isHittable)
        automaticSource.tap()
        button(label: "Live", in: app).tap()
        let automaticFooter = app.staticTexts["Source: Automatic"]
        reveal(automaticFooter, in: app)
        XCTAssertTrue(automaticFooter.exists)
    }

    func testProfileLimitButtonsRespectFortyAndTwoHundredBPMBounds() {
        let low = launchFixture(scenario: "profile-min", language: "en")
        button(label: "Profile", in: low).tap()
        let lowerLimit = low.otherElements["Heart-rate limit"]
        reveal(lowerLimit, in: low)
        XCTAssertTrue(lowerLimit.waitForExistence(timeout: 10))
        let lowerDecrease = low.buttons["Decrease limit"]
        let lowerIncrease = low.buttons["Increase limit"]
        reveal(lowerDecrease, in: low)
        XCTAssertFalse(lowerDecrease.isEnabled)
        XCTAssertTrue(lowerIncrease.isEnabled)
        lowerIncrease.tap()
        let lowUpdated = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value CONTAINS %@", "41"),
            object: lowerLimit
        )
        XCTAssertEqual(XCTWaiter.wait(for: [lowUpdated], timeout: 5), .completed)
        low.terminate()

        let high = launchFixture(scenario: "profile-max", language: "en")
        button(label: "Profile", in: high).tap()
        let upperLimit = high.otherElements["Heart-rate limit"]
        reveal(upperLimit, in: high)
        XCTAssertTrue(upperLimit.waitForExistence(timeout: 10))
        let upperIncrease = high.buttons["Increase limit"]
        let upperDecrease = high.buttons["Decrease limit"]
        reveal(upperIncrease, in: high)
        XCTAssertFalse(upperIncrease.isEnabled)
        XCTAssertTrue(upperDecrease.isEnabled)
        upperDecrease.tap()
        let highUpdated = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value CONTAINS %@", "199"),
            object: upperLimit
        )
        XCTAssertEqual(XCTWaiter.wait(for: [highUpdated], timeout: 5), .completed)
    }

    func testDebugFixtureRendersKeyboardAndRestoresTabDraftAndScroll() {
        let app = launchFixture(language: "en")
        XCTAssertTrue(app.staticTexts["PACEMAKER"].waitForExistence(timeout: 15))

        let liveTab = button(label: localized("Live"), in: app)
        let devicesTab = button(label: localized("Devices"), in: app)
        let profileTab = button(label: localized("Profile"), in: app)
        XCTAssertTrue(liveTab.waitForExistence(timeout: 5))
        XCTAssertTrue(devicesTab.exists)
        XCTAssertTrue(profileTab.exists)
        XCTAssertTrue(app.staticTexts["Mila"].exists)
        XCTAssertTrue(app.staticTexts["Alex"].exists)
        XCTAssertFalse(app.staticTexts["Alex Rivera"].exists)
        attachScreenshot(named: "iPhone Live fixture", app: app)

        devicesTab.tap()
        XCTAssertTrue(app.staticTexts["YOUR SIGNAL."].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Polar H10"].exists)
        XCTAssertTrue(app.staticTexts["Apple Watch"].exists)
        attachScreenshot(named: "iPhone Devices fixture", app: app)

        profileTab.tap()
        let displayName = app.textViews[localized("Display name")]
        XCTAssertTrue(displayName.waitForExistence(timeout: 5))
        attachScreenshot(named: "iPhone Profile fixture", app: app)

        app.buttons[localized("Increase limit")].tap()
        let limit = app.otherElements[localized("Heart-rate limit")]
        XCTAssertTrue(limit.waitForExistence(timeout: 5))
        let limitUpdated = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value CONTAINS %@", "151"),
            object: limit
        )
        XCTAssertEqual(XCTWaiter.wait(for: [limitUpdated], timeout: 5), .completed)

        displayName.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        displayName.typeText(" X")
        let nameUpdated = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value CONTAINS %@", "X"),
            object: displayName
        )
        XCTAssertEqual(XCTWaiter.wait(for: [nameUpdated], timeout: 5), .completed)
        attachScreenshot(named: "iPhone Profile keyboard visible", app: app)

        devicesTab.tap()
        profileTab.tap()
        XCTAssertEqual(displayName.value as? String, "Alex Rivera X")
        _ = app.keyboards.firstMatch.waitForNonExistence(timeout: 5)

        let voiceAlerts = app.staticTexts[localized("Voice alerts")]
        for _ in 0..<5 where !voiceAlerts.exists {
            app.swipeUp()
        }
        XCTAssertTrue(voiceAlerts.waitForExistence(timeout: 5))
        attachScreenshot(named: "iPhone Profile scrolled", app: app)

        liveTab.tap()
        profileTab.tap()
        XCTAssertEqual(displayName.value as? String, "Alex Rivera X")
        XCTAssertTrue(voiceAlerts.waitForExistence(timeout: 5))
    }

    func testFixtureCoversLimitFreshnessPeerAndSourceScenarios() {
        let scenarios: [(String, String)] = [
            ("live-equal", "Within your limit"),
            ("live-above", "Above your limit"),
            ("live-gap-boundary", "Within your limit"),
            ("live-future", "Signal lost"),
            ("live-expired", "Signal lost"),
            ("live-disconnected", "Signal lost")
        ]

        for (scenario, expectedStatus) in scenarios {
            let app = launchFixture(scenario: scenario, language: "en")
            XCTAssertTrue(app.staticTexts[expectedStatus].waitForExistence(timeout: 10), scenario)
            if scenario == "live-equal" {
                XCTAssertTrue(app.staticTexts["150"].exists)
            } else if scenario == "live-above" {
                XCTAssertTrue(app.staticTexts["151"].exists)
                attachScreenshot(named: "scenario-live-above", app: app)
            } else if scenario == "live-gap-boundary" {
                XCTAssertTrue(app.staticTexts["142"].exists)
            } else if scenario == "live-future" || scenario == "live-disconnected" {
                attachScreenshot(named: "scenario-\(scenario)", app: app)
            }
            app.terminate()
        }

        let peerGap = launchFixture(scenario: "peer-gap", language: "en")
        XCTAssertTrue(peerGap.staticTexts["Mila"].waitForExistence(timeout: 10))
        XCTAssertTrue(peerGap.staticTexts["Connection lost"].exists)
        XCTAssertFalse(peerGap.staticTexts["Alex Rivera"].exists)
        peerGap.terminate()

        let permission = launchFixture(scenario: "devices-permission", language: "en")
        button(label: "Devices", in: permission).tap()
        XCTAssertTrue(permission.staticTexts["Bluetooth access needed"].waitForExistence(timeout: 10))
        XCTAssertTrue(permission.staticTexts["Bluetooth permission required"].exists)
        permission.terminate()

        let bluetoothOff = launchFixture(scenario: "devices-bluetooth-off", language: "en")
        button(label: "Devices", in: bluetoothOff).tap()
        XCTAssertTrue(bluetoothOff.staticTexts["Bluetooth is off"].waitForExistence(timeout: 10))
        bluetoothOff.terminate()

        let watchUnavailable = launchFixture(scenario: "devices-watch-unavailable", language: "en")
        button(label: "Devices", in: watchUnavailable).tap()
        XCTAssertTrue(watchUnavailable.staticTexts["Watch connection is unavailable."].waitForExistence(timeout: 10))
        watchUnavailable.terminate()
    }

    func testProductionNamePersistsAcrossRelaunchAndIsRestored() {
        let app = XCUIApplication()
        app.launchArguments = localeArguments(testLanguage)
        app.launch()
        XCTAssertTrue(app.staticTexts["PACEMAKER"].waitForExistence(timeout: 15))
        button(label: localized("Profile"), in: app).tap()

        let displayName = app.textViews[localized("Display name")]
        XCTAssertTrue(displayName.waitForExistence(timeout: 10))
        guard let originalName = displayName.value as? String, !originalName.isEmpty else {
            XCTFail("Production display name was not exposed through accessibility.")
            return
        }

        let suffix = "-QA"
        displayName.coordinate(withNormalizedOffset: CGVector(dx: 0.98, dy: 0.5)).tap()
        displayName.typeText(suffix)
        XCTAssertTrue((displayName.value as? String)?.hasSuffix(suffix) == true)

        app.terminate()
        app.launch()
        XCTAssertTrue(app.staticTexts["PACEMAKER"].waitForExistence(timeout: 15))
        button(label: localized("Profile"), in: app).tap()
        let relaunchedName = app.textViews[localized("Display name")]
        XCTAssertTrue(relaunchedName.waitForExistence(timeout: 10))
        XCTAssertEqual(relaunchedName.value as? String, originalName + suffix)

        relaunchedName.coordinate(withNormalizedOffset: CGVector(dx: 0.98, dy: 0.5)).tap()
        relaunchedName.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: suffix.count))
        let restoredInMemory = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value == %@", originalName),
            object: relaunchedName
        )
        XCTAssertEqual(XCTWaiter.wait(for: [restoredInMemory], timeout: 5), .completed)

        app.terminate()
        app.launch()
        XCTAssertTrue(app.staticTexts["PACEMAKER"].waitForExistence(timeout: 15))
        button(label: localized("Profile"), in: app).tap()
        XCTAssertEqual(app.textViews[localized("Display name")].value as? String, originalName)
    }

    private var testLanguage: String {
        ProcessInfo.processInfo.environment["PACEMAKER_UI_TEST_LANGUAGE"]
            ?? Locale.current.language.languageCode?.identifier
            ?? "en"
    }

    private func launchFixture(scenario: String = "reference", language: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        let selectedLanguage = language ?? testLanguage
        app.launchArguments = [
            "--pacemaker-ui-testing",
            "--pacemaker-ui-scenario=\(scenario)"
        ] + localeArguments(selectedLanguage)
        app.launch()
        return app
    }

    private func localeArguments(_ language: String) -> [String] {
        let locale = language == "de" ? "de_DE" : language == "it" ? "it_IT" : "en_US"
        return ["-AppleLanguages", "(\(language))", "-AppleLocale", locale]
    }

    private func attachScreenshot(named name: String, app: XCUIApplication) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        XCTContext.runActivity(named: name) { activity in
            activity.add(attachment)
        }
    }

    private func button(label: String, in app: XCUIApplication) -> XCUIElement {
        let matches = app.buttons.matching(NSPredicate(format: "label == %@", label))
        return matches.element(boundBy: max(0, matches.count - 1))
    }

    private func reveal(_ element: XCUIElement, in app: XCUIApplication, maxSwipes: Int = 8) {
        for _ in 0..<maxSwipes where !element.isHittable {
            app.swipeUp()
        }
    }

    private func localized(_ english: String) -> String {
        let language = testLanguage
        switch (english, language) {
        case ("Devices", "de"): return "Geräte"
        case ("Devices", "it"): return "Dispositivi"
        case ("Profile", "de"): return "Profil"
        case ("Profile", "it"): return "Profilo"
        case ("Display name", "de"): return "Anzeigename"
        case ("Display name", "it"): return "Nome visualizzato"
        case ("Increase limit", "de"): return "Limit erhöhen"
        case ("Increase limit", "it"): return "Aumenta il limite"
        case ("Heart-rate limit", "de"): return "Herzfrequenzlimit"
        case ("Heart-rate limit", "it"): return "Limite cardiaco"
        case ("Voice alerts", "de"): return "Sprachmeldungen"
        case ("Voice alerts", "it"): return "Avvisi vocali"
        case ("YOUR SIGNAL.", "de"): return "DEIN SIGNAL."
        case ("YOUR SIGNAL.", "it"): return "IL TUO SEGNALE."
        case ("MAKE IT YOURS.", "de"): return "MACH ES ZU DEINEM."
        case ("MAKE IT YOURS.", "it"): return "FALLO TUO."
        case ("Identity color", "de"): return "Identitätsfarbe"
        case ("Identity color", "it"): return "Colore identità"
        default: return english
        }
    }
}
