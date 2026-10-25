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
    private var workoutGeneration = 0
    private var startInFlight = false
    private var activated = false

    var overLimit: Bool { heartRate != nil && identity.limit > 0 && heartRate! >= identity.limit }

    func activate() {
        guard !activated else { return }
        activated = true
        guard WCSession.isSupported() else { status = "iPhone connection unavailable"; return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    func toggleWorkout() { isRunning ? stopWorkout() : startWorkout() }

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

    private func startWorkout() {
        guard !isBusy, !startInFlight else { return }
        isBusy = true
        startInFlight = true
        workoutGeneration += 1
        let generation = workoutGeneration
        guard HKHealthStore.isHealthDataAvailable() else { status = "Heart rate unavailable"; isBusy = false; startInFlight = false; return }
        let heartRateType = HKObjectType.quantityType(forIdentifier: .heartRate)!
        let workoutType = HKObjectType.workoutType()
        health.requestAuthorization(toShare: [workoutType], read: [heartRateType]) { [weak self] success, error in
            Task { @MainActor in
                guard let self else { return }
                guard self.workoutGeneration == generation else { return }
                guard success, error == nil else {
                    self.status = "Allow heart rate in Settings"; self.isBusy = false; self.startInFlight = false; return
                }
                // HealthKit can report a successful authorization request even when the user
                // declined sharing. A workout session requires explicit workout sharing access.
                guard self.health.authorizationStatus(for: workoutType) == .sharingAuthorized else {
                    self.status = "Allow workout access in Settings"
                    self.isBusy = false
                    self.startInFlight = false
                    return
                }
                do {
                    let configuration = HKWorkoutConfiguration()
                    configuration.activityType = .other
                    configuration.locationType = .unknown
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
                                return
                            }
                            self.startInFlight = false
                            guard success, error == nil else {
                                session.end()
                                builder.discardWorkout()
                                self.workoutSession = nil
                                self.workoutBuilder = nil
                                self.status = "Could not start heart rate"
                                self.isRunning = false
                                self.isBusy = false
                                return
                            }
                            self.isRunning = true
                            self.lastHeartRateAt = nil
                            self.status = "With iPhone"
                            self.isBusy = false
                        }
                    }
                } catch {
                    self.status = "Could not start heart rate"
                    self.isBusy = false
                    self.startInFlight = false
                }
            }
        }
    }

    private func stopWorkout() {
        workoutGeneration += 1
        let stopGeneration = workoutGeneration
        startInFlight = false
        isBusy = true
        workoutSession?.end()
        guard let builder = workoutBuilder else {
            isRunning = false
            isBusy = false
            heartRate = nil
            status = "Ready"
            workoutSession = nil
            lastHeartRateAt = nil
            sendLiveHeartRate(nil)
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

    nonisolated func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        Task { @MainActor [weak self] in
            guard let self else { return }
            phoneReachable = session.isReachable
            applyConfiguration(session.receivedApplicationContext)
        }
    }
    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        Task { @MainActor [weak self] in
            guard let self else { return }
            phoneReachable = session.isReachable
        }
    }
    nonisolated func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) {
        Task { @MainActor [weak self] in self?.applyConfiguration(context) }
    }
    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        if message["type"] as? String == "watch.group",
           let rows = message["members"] as? [[String: Any]] {
            Task { @MainActor [weak self] in self?.applyGroupSnapshot(rows) }
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
