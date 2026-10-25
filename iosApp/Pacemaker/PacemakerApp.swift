//
//  PacemakerApp.swift
//  Pacemaker
//
//  Created by Sebastian Sellmair on 13.03.23.
//

import SwiftUI
import UIKit

final class PacemakerAppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--pacemaker-ui-testing") {
            return true
        }
        #endif
        WatchPhoneBridge.shared.activate()
        return true
    }
}

@main
struct PacemakerApp: App {
    @UIApplicationDelegateAdaptor(PacemakerAppDelegate.self) private var appDelegate
    var body: some Scene {
        WindowGroup {
            ContentView().edgesIgnoringSafeArea([.bottom, .top]) }
    }
}
