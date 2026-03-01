import SwiftUI
import sample

struct ContentView: View {
    @StateObject private var vm = SampleIosViewModel()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Meeseeks iOS Sample")
                .font(.title2)

            Picker("Scenario", selection: $vm.selectedScenario) {
                ForEach(vm.scenarios, id: \.self) { scenario in
                    Text(scenario).tag(scenario)
                }
            }

            HStack {
                Toggle("Periodic", isOn: $vm.periodic)
                Toggle("Network", isOn: $vm.requiresNetwork)
                Toggle("Charging", isOn: $vm.requiresCharging)
                Toggle("Battery Not Low", isOn: $vm.requiresBatteryNotLow)
                Toggle("High Priority", isOn: $vm.highPriority)
            }

            HStack {
                Button("Schedule") { vm.schedule() }
                Button("Reschedule Task") { vm.rescheduleTask() }
                Button("Cancel All") { vm.cancelAll() }
                Button("Reschedule Pending") { vm.reschedulePending() }
            }

            TextField("Task ID", text: $vm.taskId)
                .textFieldStyle(.roundedBorder)

            HStack {
                Button("Cancel") { vm.cancelTask() }
                Button("Refresh") { vm.refresh() }
                Button("Export Telemetry") { vm.exportTelemetry() }
                Button("Clear Telemetry") { vm.clearTelemetry() }
            }

            Text(vm.status)
                .font(.callout)

            Text("Tasks")
                .font(.headline)
            List(vm.taskLines, id: \.self) { line in
                Text(line).font(.caption)
            }

            Text("Telemetry")
                .font(.headline)
            List(vm.telemetryLines, id: \.self) { line in
                Text(line).font(.caption2)
            }

            Text("Telemetry Statistics JSON")
                .font(.headline)
            TextEditor(text: $vm.telemetryStatisticsJson)
                .font(.caption2)
                .frame(height: 120)
                .border(Color.gray.opacity(0.3), width: 1)

            Text("Telemetry Events JSON")
                .font(.headline)
            TextEditor(text: $vm.telemetryEventsJson)
                .font(.caption2)
                .frame(height: 120)
                .border(Color.gray.opacity(0.3), width: 1)
        }
        .padding()
        .onAppear { vm.refresh() }
    }
}

final class SampleIosViewModel: ObservableObject {
    private let bridge = DemoIosBridge(encryptionEnabled: false)

    @Published var scenarios: [String] = []
    @Published var selectedScenario: String = "SUCCESS"
    @Published var periodic: Bool = false
    @Published var requiresNetwork: Bool = false
    @Published var requiresCharging: Bool = false
    @Published var requiresBatteryNotLow: Bool = false
    @Published var highPriority: Bool = false
    @Published var taskId: String = ""
    @Published var taskLines: [String] = []
    @Published var telemetryLines: [String] = []
    @Published var telemetryStatisticsJson: String = ""
    @Published var telemetryEventsJson: String = ""
    @Published var status: String = "Ready"

    init() {
        scenarios = bridge.availableScenarios()
        selectedScenario = scenarios.first ?? "SUCCESS"
    }

    func schedule() {
        let id = bridge.schedule(
            scenarioName: selectedScenario,
            periodic: periodic,
            requiresNetwork: requiresNetwork,
            requiresCharging: requiresCharging,
            requiresBatteryNotLow: requiresBatteryNotLow,
            highPriority: highPriority
        )
        status = "Scheduled \(id)"
        refresh()
    }

    func rescheduleTask() {
        let id = bridge.reschedule(
            taskId: taskId,
            scenarioName: selectedScenario,
            periodic: periodic,
            requiresNetwork: requiresNetwork,
            requiresCharging: requiresCharging,
            requiresBatteryNotLow: requiresBatteryNotLow,
            highPriority: highPriority
        )
        status = "Rescheduled \(id)"
        refresh()
    }

    func cancelTask() {
        bridge.cancel(taskId: taskId)
        status = "Cancelled \(taskId)"
        refresh()
    }

    func cancelAll() {
        bridge.cancelAll()
        status = "Cancelled all"
        refresh()
    }

    func reschedulePending() {
        bridge.reschedulePendingTasks()
        status = "Rescheduled pending"
        refresh()
    }

    func refresh() {
        taskLines = bridge.listTaskLines()
        telemetryLines = bridge.telemetryLines(limit: 30)
    }

    func exportTelemetry() {
        telemetryStatisticsJson = bridge.telemetryStatisticsJson()
        telemetryEventsJson = bridge.telemetryEventsJson()
        status = "Exported telemetry snapshots"
    }

    func clearTelemetry() {
        bridge.clearTelemetry()
        telemetryStatisticsJson = ""
        telemetryEventsJson = ""
        status = "Cleared telemetry"
        refresh()
    }
}
