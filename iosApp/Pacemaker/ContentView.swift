//
//  ContentView.swift
//  Pacemaker
//
//  Created by Sebastian Sellmair on 13.03.23.
//

import SwiftUI
import LibPacemaker

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        IosPacemakerViewController().create()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var showWatchLaunchHelp = false
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .onAppear { WatchPhoneBridge.shared.activate() }
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
