package dev.mattramotar.meeseeks.sample.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.mattramotar.meeseeks.runtime.ScheduledTask
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.sample.demo.DemoDesktopAppContext
import dev.mattramotar.meeseeks.sample.demo.DemoSampleConfig
import dev.mattramotar.meeseeks.sample.demo.DemoScenario
import dev.mattramotar.meeseeks.sample.demo.DemoScheduleRequest
import dev.mattramotar.meeseeks.sample.demo.DemoTaskFacadeFactory
import kotlinx.coroutines.delay

private val facade = DemoTaskFacadeFactory.create(
    appContext = DemoDesktopAppContext,
    config = DemoSampleConfig(encryptionEnabled = false),
)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Meeseeks Desktop Sample") {
        MaterialTheme {
            DesktopScreen()
        }
    }
}

@Composable
private fun DesktopScreen() {
    var scenario by remember { mutableStateOf(DemoScenario.SUCCESS) }
    var periodic by remember { mutableStateOf(false) }
    var requiresNetwork by remember { mutableStateOf(false) }
    var requiresCharging by remember { mutableStateOf(false) }
    var requiresBatteryNotLow by remember { mutableStateOf(false) }
    var highPriority by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Ready") }
    var telemetryStatisticsJson by remember { mutableStateOf("") }
    var telemetryEventsJson by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf<List<ScheduledTask>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            tasks = facade.listTasks()
            delay(1200)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Meeseeks Desktop Sample", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoScenario.entries.forEach { candidate ->
                Button(onClick = { scenario = candidate }) {
                    Text(if (scenario == candidate) "* ${candidate.title}" else candidate.title)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleControl("Periodic", periodic) { periodic = it }
            ToggleControl("Network", requiresNetwork) { requiresNetwork = it }
            ToggleControl("Charging", requiresCharging) { requiresCharging = it }
            ToggleControl("BatteryNotLow", requiresBatteryNotLow) { requiresBatteryNotLow = it }
            ToggleControl("HighPriority", highPriority) { highPriority = it }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                statusMessage = runCatching {
                    val taskId = facade.schedule(
                        DemoScheduleRequest(
                            scenario = scenario,
                            periodic = periodic,
                            requiresNetwork = requiresNetwork,
                            requiresCharging = requiresCharging,
                            requiresBatteryNotLow = requiresBatteryNotLow,
                            highPriority = highPriority,
                        )
                    )
                    "Scheduled ${taskId.value}"
                }.getOrElse { error ->
                    "Error: ${error.message}"
                }
            }) {
                Text("Schedule")
            }

            Button(onClick = {
                facade.cancelAll()
                statusMessage = "Cancelled all tasks"
            }) {
                Text("Cancel All")
            }

            Button(onClick = {
                facade.reschedulePendingTasks()
                statusMessage = "Rescheduled pending tasks"
            }) {
                Text("Reschedule Pending")
            }

            Button(onClick = {
                val snapshot = facade.telemetrySnapshot()
                telemetryStatisticsJson = snapshot.statisticsJson
                telemetryEventsJson = snapshot.eventsJson
                statusMessage = "Exported telemetry snapshots"
            }) {
                Text("Export Telemetry")
            }

            Button(onClick = {
                facade.clearTelemetry()
                telemetryStatisticsJson = ""
                telemetryEventsJson = ""
                statusMessage = "Cleared telemetry"
            }) {
                Text("Clear Telemetry")
            }
        }

        OutlinedTextField(
            value = selectedTaskId,
            onValueChange = { selectedTaskId = it },
            label = { Text("Task ID") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                statusMessage = runCatching {
                    facade.cancel(TaskId(selectedTaskId))
                    "Cancelled $selectedTaskId"
                }.getOrElse { error ->
                    "Error: ${error.message}"
                }
            }) { Text("Cancel") }

            Button(onClick = {
                statusMessage = runCatching {
                    facade.reschedule(
                        TaskId(selectedTaskId),
                        DemoScheduleRequest(
                            scenario = scenario,
                            periodic = periodic,
                            requiresNetwork = requiresNetwork,
                            requiresCharging = requiresCharging,
                            requiresBatteryNotLow = requiresBatteryNotLow,
                            highPriority = highPriority,
                        )
                    )
                    "Rescheduled $selectedTaskId"
                }.getOrElse { error ->
                    "Error: ${error.message}"
                }
            }) { Text("Reschedule") }
        }

        Text(statusMessage)

        Text("Tasks", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.height(160.dp)) {
            items(tasks) { task ->
                Text("${task.id.value} | ${task.status} | ${task.task.payload::class.simpleName}")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Telemetry", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(facade.telemetryEvents.value.takeLast(20)) { event ->
                Text(event)
            }
        }

        Text("Telemetry Statistics JSON", style = MaterialTheme.typography.titleSmall)
        Text(telemetryStatisticsJson)

        Text("Telemetry Events JSON", style = MaterialTheme.typography.titleSmall)
        Text(telemetryEventsJson)
    }
}

@Composable
private fun ToggleControl(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
