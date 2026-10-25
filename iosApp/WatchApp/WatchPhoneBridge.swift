import Foundation
import Combine
import WatchConnectivity
import LibPacemaker

@MainActor final class WatchPhoneBridge: NSObject, WCSessionDelegate {
    static let shared = WatchPhoneBridge()
    private var groupTimer: Timer?
    private override init() { super.init() }

    func activate() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
        updateCompanionState(WCSession.default)
        groupTimer?.invalidate()
        groupTimer = Timer.scheduledTimer(withTimeInterval: 5, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.pushConfiguration()
                self?.sendGroupSnapshot()
            }
        }
    }

    private func pushConfiguration() {
        guard let data = IosWatchBridge.shared.watchConfigurationJson().data(using: .utf8),
              let context = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
        var configuration = context
        configuration.removeValue(forKey: "group")
        try? WCSession.default.updateApplicationContext(configuration)
    }

    private func sendGroupSnapshot() {
        let session = WCSession.default
        guard session.isReachable,
              let data = IosWatchBridge.shared.watchGroupSnapshotJson().data(using: .utf8),
              let context = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let members = context["group"] as? [[String: Any]] else { return }
        session.sendMessage(["type": "watch.group", "members": members], replyHandler: nil) { _ in }
    }

    nonisolated func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        Task { @MainActor [weak self] in
            self?.pushConfiguration()
            self?.updateCompanionState(session)
        }
    }
    nonisolated func sessionDidBecomeInactive(_ session: WCSession) { }
    nonisolated func sessionDidDeactivate(_ session: WCSession) { WCSession.default.activate() }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        Task { @MainActor [weak self] in
            self?.updateCompanionState(session)
            self?.handle(message, replyHandler: nil)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {
        Task { @MainActor [weak self] in
            self?.updateCompanionState(session)
            self?.handle(message, replyHandler: replyHandler)
        }
    }

    private func updateCompanionState(_ session: WCSession) {
        IosWatchBridge.shared.setWatchCompanionState(paired: session.isPaired && session.isWatchAppInstalled,
                                                     connected: session.isReachable)
    }

    private func handle(_ message: [String: Any], replyHandler: (([String: Any]) -> Void)?) {
        if message["type"] as? String == "watch.group",
           let members = message["members"] as? [[String: Any]] {
            NotificationCenter.default.post(name: .watchGroupSnapshot, object: members)
            return
        }
        if message["type"] as? String == "watch.hr",
           let measuredAt = message["measuredAt"] as? Double {
            if let bpm = message["bpm"] as? Int {
                IosWatchBridge.shared.receiveHeartRateBpm(bpm: Int32(bpm), measuredAtEpochMillis: Int64(measuredAt))
            } else {
                IosWatchBridge.shared.clearWatchHeartRate()
            }
            sendGroupSnapshot()
            return
        }
        replyHandler?(["accepted": false])
    }
}

extension Notification.Name {
    static let watchGroupSnapshot = Notification.Name("PacemakerWatchGroupSnapshot")
}
