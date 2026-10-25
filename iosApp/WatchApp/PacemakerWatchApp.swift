import SwiftUI

@main
struct PacemakerWatchApp: App {
    @StateObject private var session = WatchSessionModel.shared
    var body: some Scene {
        WindowGroup { WatchHomeView().environmentObject(session) }
    }
}

struct WatchHomeView: View {
    @EnvironmentObject private var model: WatchSessionModel
    var body: some View {
        ScrollView {
            VStack(spacing: 7) {
                Text(model.identity.name).font(.caption).lineLimit(1)
                Text(model.heartRate.map(String.init) ?? "—")
                    .font(.system(size: 42, weight: .bold, design: .rounded))
                    .foregroundStyle(model.overLimit ? .red : .primary)
                Text(model.heartRate.map { "\($0) BPM" } ?? NSLocalizedString(model.status, comment: ""))
                    .font(.caption2).foregroundStyle(.secondary)
                if model.identity.limit > 0 { Text("Limit \(model.identity.limit)").font(.caption2) }
                Button(model.isRunning ? "Stop" : "Start") { model.toggleWorkout() }
                    .tint(model.isRunning ? .red : .green)
                    .disabled(model.isBusy || (!model.isRunning && !model.phoneReachable))
                if !model.phoneReachable {
                    Text("Open Pacemaker on your iPhone").font(.caption2).multilineTextAlignment(.center)
                }
                if !model.group.isEmpty {
                    Divider()
                    ForEach(model.group) { member in
                        HStack {
                            Text(member.name).lineLimit(1)
                            Spacer(minLength: 2)
                            Text(member.bpm.map { "\($0)" } ?? "—")
                                .foregroundStyle(member.isCritical ? .red : .primary)
                        }.font(.caption2)
                    }
                }
            }.frame(maxWidth: .infinity)
        }.task {
            model.activate()
            while !Task.isCancelled {
                model.refreshFreshness()
                try? await Task.sleep(for: .seconds(1))
            }
        }
    }
}
