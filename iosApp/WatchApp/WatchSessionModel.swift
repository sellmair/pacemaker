import Foundation
import Combine
import HealthKit
import WatchConnectivity
import WatchKit

struct WatchIdentity {
    var id = ""
    var name = "Pacemaker"
    var limit = 0
}

struct WatchGroupMember: Identifiable {
    let id: String
    let name: String
    let bpm: Int?
    let limit: Int?
    let measuredAt: Date?
    var isCritical: Bool { bpm != nil && limit != nil && bpm! >= limit! }
}

@MainActor final class WatchSessionModel: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WatchSessionModel()
    @Published private(set) var identity = WatchIdentity()
    @Published private(set) var heartRate: Int?
    @Published private(set) var isRunning = false
    @Published private(set) var isBusy = false
    @Published private(set) var phoneReachable = false
    @Published private(set) var group: [WatchGroupMember] = []
    @Published private(set) var status = "Ready"

    private let health = HKHealthStore()
    private var workoutSession: HKWorkoutSession?
    private var workoutBuilder: HKLiveWorkoutBuilder?
    private var lastHeartRateAt: Date?
    private var lastGroupAlert: [String: Date] = [:]
    private var runId = UUID().uuidString
    private var sessionRevision: Int64 = 0
    private var commandResults: [String: Bool] = [:]
    private var commandRevisions: [String: Int64] = [:]
    private var acceptedPhoneRunId: String?
    private var pendingSessionCommand: [String: Any]?
    private var pendingSessionCommands: [[String: Any]] = []
    private var sendingSessionCommand = false
    private var attemptedSessionCommandIds = Set<String>()
    private var workoutGeneration = 0
    private var startInFlight = false
    private var activated = false
    private var phoneWorkoutConfiguration: HKWorkoutConfiguration?

    var overLimit: Bool { heartRate != nil && identity.limit > 0 && heartRate! >= identity.limit }

    func activate() {
        guard !activated else { return }
        activated = true
        guard WCSession.isSupported() else { status = "iPhone connection unavailable"; return }
        let session = WCSession.default
        pendingSessionCommands = UserDefaults.standard.array(forKey: "pacemaker.pendingSessionCommands") as? [[String: Any]] ?? []
        if pendingSessionCommands.isEmpty,
           let legacy = UserDefaults.standard.dictionary(forKey: "pacemaker.pendingSessionCommand") {
            pendingSessionCommands = [legacy]
            persistSessionCommands()
        }
        pendingSessionCommand = pendingSessionCommands.first
        attemptedSessionCommandIds = Set(pendingSessionCommands.compactMap { $0["commandId"] as? String })
        if let pending = pendingSessionCommand {
            runId = pending["runId"] as? String ?? runId
            sessionRevision = pending["revision"] as? Int64 ?? sessionRevision
        }
        session.delegate = self
        session.activate()
    }

    func handlePhoneWorkoutLaunch(_ configuration: HKWorkoutConfiguration) {
        phoneWorkoutConfiguration = configuration
        activate()
    }

    func toggleWorkout() { isRunning ? stopWorkout() : startWorkout() }

    private func rollbackPhoneStart(_ requestedRunId: String?) {
        guard let requestedRunId, acceptedPhoneRunId == requestedRunId else { return }
        acceptedPhoneRunId = nil
    }

    func refreshFreshness() {
        if isRunning {
            if lastHeartRateAt == nil || Date().timeIntervalSince(lastHeartRateAt!) > 15 || Date().timeIntervalSince(lastHeartRateAt!) < 0 {
                heartRate = nil
                status = "Heart rate unavailable"
                sendLiveHeartRate(nil)
            }
        }
        group = group.map { member in
            guard let measuredAt = member.measuredAt, Date().timeIntervalSince(measuredAt) >= 0,
                  Date().timeIntervalSince(measuredAt) <= 60 else {
                return WatchGroupMember(id: member.id, name: member.name, bpm: nil, limit: member.limit, measuredAt: member.measuredAt)
            }
            return member
        }
    }

    private func startWorkout(phoneRunId: String? = nil, phoneRevision: Int64? = nil, completion: ((Bool) -> Void)? = nil) {
        guard !isBusy, !startInFlight else { completion?(false); return }
        isBusy = true
        startInFlight = true
        workoutGeneration += 1
        let generation = workoutGeneration
        guard HKHealthStore.isHealthDataAvailable() else { status = "Heart rate unavailable"; isBusy = false; startInFlight = false; completion?(false); return }
        if let phoneRunId { runId = phoneRunId; acceptedPhoneRunId = phoneRunId }
        let heartRateType = HKObjectType.quantityType(forIdentifier: .heartRate)!
        let workoutType = HKObjectType.workoutType()
        health.requestAuthorization(toShare: [workoutType], read: [heartRateType]) { [weak self] success, error in
            Task { @MainActor in
                guard let self else { return }
                guard self.workoutGeneration == generation else { completion?(false); return }
                guard success, error == nil else {
                    self.rollbackPhoneStart(phoneRunId)
                    self.status = "Allow heart rate in Settings"; self.isBusy = false; self.startInFlight = false; completion?(false); return
                }
                // HealthKit can report a successful authorization request even when the user
                // declined sharing. A workout session requires explicit workout sharing access.
                guard self.health.authorizationStatus(for: workoutType) == .sharingAuthorized else {
                    self.rollbackPhoneStart(phoneRunId)
                    self.status = "Allow workout access in Settings"
                    self.isBusy = false
                    self.startInFlight = false
                    completion?(false)
                    return
                }
                do {
                    let configuration = self.phoneWorkoutConfiguration ?? HKWorkoutConfiguration()
                    if self.phoneWorkoutConfiguration == nil {
                        configuration.activityType = .other
                        configuration.locationType = .unknown
                    }
                    let session = try HKWorkoutSession(healthStore: self.health, configuration: configuration)
                    let builder = session.associatedWorkoutBuilder()
                    builder.dataSource = HKLiveWorkoutDataSource(healthStore: self.health, workoutConfiguration: configuration)
                    session.delegate = self
                    builder.delegate = self
                    self.workoutSession = session
                    self.workoutBuilder = builder
                    session.startActivity(with: Date())
                    builder.beginCollection(withStart: Date()) { [weak self] success, error in
                        Task { @MainActor [weak self] in
                            guard let self else { return }
                            guard self.workoutGeneration == generation else {
                                self.startInFlight = false
                                session.end()
                                builder.discardWorkout()
                                completion?(false)
                                return
                            }
                            self.startInFlight = false
                            guard success, error == nil else {
                                session.end()
                                builder.discardWorkout()
                                self.workoutSession = nil
                                self.workoutBuilder = nil
                                self.rollbackPhoneStart(phoneRunId)
                                self.status = "Could not start heart rate"
                                self.isRunning = false
                                self.isBusy = false
                                completion?(false)
                                return
                            }
                            self.isRunning = true
                            self.lastHeartRateAt = nil
                            self.runId = phoneRunId ?? UUID().uuidString
                            self.sessionRevision = phoneRevision ?? (self.sessionRevision + 1)
                            self.acceptedPhoneRunId = self.runId
                            if phoneRunId == nil { self.sendSessionState("start") }
                            self.status = "With iPhone"
                            self.isBusy = false
                            completion?(true)
                        }
                    }
                } catch {
                    self.status = "Could not start heart rate"
                    self.isBusy = false
                    self.startInFlight = false
                    self.rollbackPhoneStart(phoneRunId)
                    completion?(false)
                }
            }
        }
    }

    private func stopWorkout(phoneRevision: Int64? = nil, sendToPhone: Bool = true, completion: ((Bool) -> Void)? = nil) {
        workoutGeneration += 1
        let stopGeneration = workoutGeneration
        startInFlight = false
        isBusy = true
        workoutSession?.end()
        sessionRevision = phoneRevision ?? (sessionRevision + 1)
        if sendToPhone { sendSessionState("stop") }
        guard let builder = workoutBuilder else {
            isRunning = false
            isBusy = false
            heartRate = nil
            status = "Ready"
            workoutSession = nil
            acceptedPhoneRunId = nil
            lastHeartRateAt = nil
            sendLiveHeartRate(nil)
            completion?(true)
            return
        }
        let builderToDiscard = builder
        builder.endCollection(withEnd: Date()) { [weak self] success, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                // Deliberately discard the builder: Pacemaker does not save workouts to Apple Health.
                builderToDiscard.discardWorkout()
                guard self.workoutGeneration == stopGeneration, self.workoutBuilder === builderToDiscard else { return }
                self.heartRate = nil
                self.isRunning = false
                self.isBusy = false
                self.status = success && error == nil ? "Ready" : "Could not stop heart rate"
                self.workoutBuilder = nil
                self.workoutSession = nil
                self.lastHeartRateAt = nil
                self.acceptedPhoneRunId = nil
                completion?(success && error == nil)
            }
        }
        sendLiveHeartRate(nil)
    }

    private func sendLiveHeartRate(_ bpm: Int?, measuredAt: Date = Date()) {
        let session = WCSession.default
        guard session.isReachable else { return } // no queued/stale live readings
        var payload: [String: Any] = ["type": "watch.hr", "id": UUID().uuidString,
                                      "userId": identity.id, "measuredAt": measuredAt.timeIntervalSince1970 * 1000]
        payload["bpm"] = bpm
        session.sendMessage(payload, replyHandler: nil) { _ in }
    }

    private func sendSessionState(_ action: String) {
        let commandId = "watch-\(action)-\(runId)-\(sessionRevision)"
        if pendingSessionCommands.contains(where: { $0["commandId"] as? String == commandId }) { return }
        if action == "stop", !sendingSessionCommand,
           let index = pendingSessionCommands.firstIndex(where: {
               $0["action"] as? String == "start" && $0["runId"] as? String == runId &&
                   !attemptedSessionCommandIds.contains($0["commandId"] as? String ?? "")
           }) {
            pendingSessionCommands.remove(at: index)
            persistSessionCommands()
            return
        }
        pendingSessionCommands.append(["type": "watch.session", "action": action, "commandId": commandId,
                                       "runId": runId, "revision": sessionRevision])
        persistSessionCommands()
        retryPendingSessionCommand()
    }

    private func retryPendingSessionCommand() {
        guard !sendingSessionCommand, WCSession.default.isReachable,
              let command = pendingSessionCommands.first,
              let commandId = command["commandId"] as? String else { return }
        pendingSessionCommand = command
        sendingSessionCommand = true
        attemptedSessionCommandIds.insert(commandId)
        WCSession.default.sendMessage(command, replyHandler: { [weak self] response in
            Task { @MainActor in
                guard let self, self.pendingSessionCommand?["commandId"] as? String == commandId,
                      response["commandId"] as? String == commandId else { return }
                self.sendingSessionCommand = false
                if response["accepted"] as? Bool == true || response["accepted"] as? Bool == false {
                    if self.pendingSessionCommands.first?["commandId"] as? String == commandId {
                        self.pendingSessionCommands.removeFirst()
                    }
                    self.attemptedSessionCommandIds.remove(commandId)
                    self.pendingSessionCommand = self.pendingSessionCommands.first
                    self.persistSessionCommands()
                    self.retryPendingSessionCommand()
                }
            }
        }, errorHandler: { [weak self] _ in
            Task { @MainActor in self?.sendingSessionCommand = false }
        })
    }

    private func persistSessionCommands() {
        UserDefaults.standard.set(pendingSessionCommands, forKey: "pacemaker.pendingSessionCommands")
        if let first = pendingSessionCommands.first {
            UserDefaults.standard.set(first, forKey: "pacemaker.pendingSessionCommand")
        } else {
            UserDefaults.standard.removeObject(forKey: "pacemaker.pendingSessionCommand")
        }
    }

    nonisolated func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        Task { @MainActor [weak self] in
            guard let self else { return }
            phoneReachable = session.isReachable
            applyConfiguration(session.receivedApplicationContext)
            if session.isReachable { retryPendingSessionCommand() }
        }
    }
    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        Task { @MainActor [weak self] in
            guard let self else { return }
            phoneReachable = session.isReachable
            if session.isReachable { retryPendingSessionCommand() }
        }
    }
    nonisolated func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) {
        Task { @MainActor [weak self] in self?.applyConfiguration(context) }
    }
    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {
        Task { @MainActor [weak self] in
            guard let self else { replyHandler(["accepted": false]); return }
            self.handleControl(message, replyHandler: replyHandler)
        }
    }
    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        if message["type"] as? String == "watch.session" {
            Task { @MainActor [weak self] in self?.handleControl(message, replyHandler: nil) }
            return
        }
        if message["type"] as? String == "watch.group",
           let rows = message["members"] as? [[String: Any]] {
            Task { @MainActor [weak self] in self?.applyGroupSnapshot(rows) }
        }
    }

    private func handleControl(_ message: [String: Any], replyHandler: (([String: Any]) -> Void)?) {
        guard let action = message["action"] as? String,
              let commandId = message["commandId"] as? String,
              let incomingRunId = message["runId"] as? String,
              let revision = message["revision"] as? Int64,
              action == "start" || action == "stop" else {
            replyHandler?(["accepted": false]); return
        }
        if let result = commandResults[commandId] {
            replyHandler?(["commandId": commandId, "accepted": result]); return
        }
        let priorRevision = commandRevisions[incomingRunId] ?? -1
        guard revision > priorRevision,
              action == "start" ? (acceptedPhoneRunId == nil || acceptedPhoneRunId == incomingRunId) :
                (acceptedPhoneRunId == nil || acceptedPhoneRunId == incomingRunId) else {
            commandResults[commandId] = false
            replyHandler?(["commandId": commandId, "accepted": false]); return
        }
        commandRevisions[incomingRunId] = revision // tombstone even if STOP arrives before START
        if action == "start" {
            guard !isRunning else {
                let accepted = runId == incomingRunId
                commandResults[commandId] = accepted
                replyHandler?(["commandId": commandId, "accepted": accepted]); return
            }
            startWorkout(phoneRunId: incomingRunId, phoneRevision: revision) { [weak self] accepted in
                guard let self else { replyHandler?(["commandId": commandId, "accepted": false]); return }
                self.commandResults[commandId] = accepted
                replyHandler?(["commandId": commandId, "accepted": accepted])
            }
        } else {
            if isBusy && startInFlight && acceptedPhoneRunId == incomingRunId {
                stopWorkout(phoneRevision: revision, sendToPhone: false) { [weak self] accepted in
                    guard let self else { replyHandler?(["commandId": commandId, "accepted": false]); return }
                    self.commandResults[commandId] = accepted
                    replyHandler?(["commandId": commandId, "accepted": accepted])
                }
                return
            }
            guard isRunning, runId == incomingRunId else {
                let accepted = !isRunning && (acceptedPhoneRunId == nil || acceptedPhoneRunId == incomingRunId)
                commandResults[commandId] = accepted
                replyHandler?(["commandId": commandId, "accepted": accepted]); return
            }
            stopWorkout(phoneRevision: revision, sendToPhone: false) { [weak self] accepted in
                guard let self else { replyHandler?(["commandId": commandId, "accepted": false]); return }
                self.commandResults[commandId] = accepted
                replyHandler?(["commandId": commandId, "accepted": accepted])
            }
        }
    }

    private func applyConfiguration(_ context: [String: Any]) {
        if let id = context["userId"] as? String { identity.id = id }
        if let name = context["name"] as? String { identity.name = name }
        if let limit = context["limit"] as? Int { identity.limit = limit }
        if let rows = context["group"] as? [[String: Any]] { applyGroupSnapshot(rows) }
    }

    private func applyGroupSnapshot(_ rows: [[String: Any]]) {
            group = rows.compactMap { row in
                guard let id = row["userId"] as? String, id != identity.id,
                      let name = row["name"] as? String else { return nil }
                let measured = (row["measuredAt"] as? Double).map { Date(timeIntervalSince1970: $0 / 1000) }
                let age = measured.map { Date().timeIntervalSince($0) }
                let bpm = age != nil && age! >= 0 && age! <= 60 ? row["bpm"] as? Int : nil
                return WatchGroupMember(id: id, name: name, bpm: bpm, limit: row["limit"] as? Int, measuredAt: measured)
            }.sorted { lhs, rhs in
                if lhs.isCritical != rhs.isCritical { return lhs.isCritical }
                return (lhs.bpm ?? 0) > (rhs.bpm ?? 0)
            }
            for member in group where member.isCritical {
                let last = lastGroupAlert[member.id] ?? .distantPast
                if Date().timeIntervalSince(last) > 30 { WKInterfaceDevice.current().play(.notification); lastGroupAlert[member.id] = Date() }
            }
    }
}

extension WatchSessionModel: HKWorkoutSessionDelegate, HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didChangeTo toState: HKWorkoutSessionState,
                                    from fromState: HKWorkoutSessionState, date: Date) { }
    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {
        Task { @MainActor [weak self] in
            guard let self else { return }
            guard self.workoutSession === workoutSession else { return }
            self.heartRate = nil
            self.isRunning = false
            self.isBusy = false
            self.status = "Could not start heart rate"
            self.sendLiveHeartRate(nil)
        }
    }
    nonisolated func workoutBuilder(_ workoutBuilder: HKLiveWorkoutBuilder, didCollectDataOf types: Set<HKSampleType>) {
        guard let type = HKObjectType.quantityType(forIdentifier: .heartRate), types.contains(type),
              let statistics = workoutBuilder.statistics(for: type),
              let sample = statistics.mostRecentQuantity() else { return }
        let bpm = Int(sample.doubleValue(for: HKUnit.count().unitDivided(by: .minute())).rounded())
        let measuredAt = statistics.mostRecentQuantityDateInterval()?.end ?? Date()
        let sampleAge = Date().timeIntervalSince(measuredAt)
        guard sampleAge >= 0 && sampleAge <= 15 else { return }
        let builderId = ObjectIdentifier(workoutBuilder)
        Task { @MainActor in
            guard isRunning, !isBusy, self.workoutBuilder.map(ObjectIdentifier.init) == builderId else { return }
            heartRate = bpm
            lastHeartRateAt = measuredAt
            status = "With iPhone"
            sendLiveHeartRate(bpm, measuredAt: measuredAt)
            if overLimit { WKInterfaceDevice.current().play(.notification) }
        }
    }
    nonisolated func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) { }
}
