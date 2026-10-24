import Foundation
import Combine
import WatchConnectivity
import LibPacemaker
import HealthKit

@MainActor final class WatchPhoneBridge: NSObject, WCSessionDelegate {
    static let shared = WatchPhoneBridge()
    private var groupTimer: Timer?
    private var activePhoneRunId: String?
    private var phoneSessionRevision: Int64 = 0
    private var rejectedStartRunId: String?
    private var pendingCommand: [String: Any]?
    private var pendingCommandId: String?
    private let healthStore = HKHealthStore()
    private override init() { super.init() }

    func activate() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        phoneSessionRevision = max(phoneSessionRevision, Int64(UserDefaults.standard.integer(forKey: "pacemaker.phoneSessionRevision")))
        WCSession.default.activate()
        updateCompanionState(WCSession.default)
        groupTimer?.invalidate()
        groupTimer = Timer.scheduledTimer(withTimeInterval: 5, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.pushConfiguration()
                self?.sendGroupSnapshot()
                self?.synchronizeSession()
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

    private func synchronizeSession() {
        let session = WCSession.default
        guard let data = IosWatchBridge.shared.watchActiveSessionJson().data(using: .utf8),
              let state = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
        let runId = state["runId"] as? String
        if let pending = pendingCommand,
           pending["action"] as? String == "start",
           let pendingRun = pending["runId"] as? String,
           pendingRun != runId {
            // The unacknowledged Start may already be at the watch. Send a newer Stop
            // tombstone for that run before starting the newly authoritative run.
            _ = nextPhoneRevision(minimum: pending["revision"] as? Int64 ?? 0)
            let commandId = "ios-stop-\(pendingRun)-\(phoneSessionRevision)"
            pendingCommandId = commandId
            pendingCommand = ["action": "stop", "commandId": commandId,
                              "runId": pendingRun, "revision": phoneSessionRevision]
        }
        if pendingCommand == nil, let runId, runId != activePhoneRunId {
            if let current = activePhoneRunId {
                _ = nextPhoneRevision()
                let commandId = "ios-stop-\(current)-\(phoneSessionRevision)"
                pendingCommandId = commandId
                pendingCommand = ["action": "stop", "commandId": commandId,
                                  "runId": current, "revision": phoneSessionRevision]
            } else if runId != rejectedStartRunId {
                _ = nextPhoneRevision()
                let commandId = "ios-start-\(runId)-\(phoneSessionRevision)"
                pendingCommandId = commandId
                pendingCommand = ["action": "start", "commandId": commandId,
                                  "runId": runId, "revision": phoneSessionRevision]
                launchWatchWorkout()
            }
        } else if pendingCommand == nil, runId == nil, let current = activePhoneRunId {
            _ = nextPhoneRevision()
            let commandId = "ios-stop-\(current)-\(phoneSessionRevision)"
            pendingCommandId = commandId
            pendingCommand = ["action": "stop", "commandId": commandId,
                              "runId": current, "revision": phoneSessionRevision]
        }
        guard session.isReachable, let command = pendingCommand, let commandId = pendingCommandId else { return }
        session.sendMessage(command, replyHandler: { [weak self] reply in
            Task { @MainActor in
                guard let self, self.pendingCommandId == commandId,
                      reply["accepted"] as? Bool != nil else { return }
                let accepted = reply["accepted"] as? Bool == true
                if accepted, command["action"] as? String == "start" { self.activePhoneRunId = command["runId"] as? String }
                else if !accepted, command["action"] as? String == "start" { self.rejectedStartRunId = command["runId"] as? String }
                else if accepted, self.activePhoneRunId == (command["runId"] as? String) { self.activePhoneRunId = nil }
                self.pendingCommand = nil
                self.pendingCommandId = nil
                if runId == nil { self.rejectedStartRunId = nil }
                self.synchronizeSession()
            }
        }, errorHandler: { _ in })
    }

    private func nextPhoneRevision(minimum: Int64 = 0) -> Int64 {
        phoneSessionRevision = max(phoneSessionRevision, minimum) + 1
        UserDefaults.standard.set(phoneSessionRevision, forKey: "pacemaker.phoneSessionRevision")
        return phoneSessionRevision
    }

    private func launchWatchWorkout() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = .other
        configuration.locationType = .unknown
        healthStore.startWatchApp(with: configuration) { success, error in
            guard !success else { return }
            NotificationCenter.default.post(name: .watchLaunchFailed, object: error)
        }
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
            synchronizeSession()
            return
        }
        guard message["type"] as? String == "watch.session",
              let action = message["action"] as? String,
              let commandId = message["commandId"] as? String,
              let runId = message["runId"] as? String,
              let revision = message["revision"] as? Int64 else {
            replyHandler?(["accepted": false])
            return
        }
        IosWatchBridge.shared.receiveSessionCommand(commandId: commandId, runId: runId,
                                                   revision: revision, action: action) { [weak self] accepted in
            Task { @MainActor in
                replyHandler?(["commandId": commandId, "accepted": accepted])
                guard let self else { return }
                self.pushConfiguration()
                self.sendGroupSnapshot()
                self.synchronizeSession()
            }
        }
    }
}

extension Notification.Name {
    static let watchGroupSnapshot = Notification.Name("PacemakerWatchGroupSnapshot")
    static let watchLaunchFailed = Notification.Name("PacemakerWatchLaunchFailed")
}
