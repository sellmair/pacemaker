//
//  ContentView.swift
//  Pacemaker
//
//  Created by Sebastian Sellmair on 13.03.23.
//

import SwiftUI
import LibPacemaker

struct ComposeView: UIViewControllerRepresentable {
    let uiTesting: Bool

    func makeUIViewController(context: Context) -> UIViewController {
        #if DEBUG
        if uiTesting {
            let scenarioPrefix = "--pacemaker-ui-scenario="
            let scenario = ProcessInfo.processInfo.arguments
                .first(where: { $0.hasPrefix(scenarioPrefix) })
                .map { String($0.dropFirst(scenarioPrefix.count)) } ?? "reference"
            return IosPacemakerUiTestFixture().create(scenario: scenario)
        }
        #endif
        return IosPacemakerViewController().create()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var showWatchLaunchHelp = false

    private var uiTesting: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("--pacemaker-ui-testing")
        #else
        false
        #endif
    }

    var body: some View {
        ComposeView(uiTesting: uiTesting)
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .onAppear {
                    if !uiTesting {
                        WatchPhoneBridge.shared.activate()
                    }
                }
                .onReceive(NotificationCenter.default.publisher(for: .watchLaunchFailed)) { _ in showWatchLaunchHelp = true }
                .alert("watch_launch_failed_title", isPresented: $showWatchLaunchHelp) {
                    Button("OK", role: .cancel) { }
                } message: {
                    Text("watch_launch_failed_message")
                }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
