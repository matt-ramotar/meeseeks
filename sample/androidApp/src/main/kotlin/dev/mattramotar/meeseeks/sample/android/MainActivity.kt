package dev.mattramotar.meeseeks.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import dev.mattramotar.meeseeks.runtime.ScheduledTask
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.sample.demo.DemoScenario
import dev.mattramotar.meeseeks.sample.demo.DemoScheduleRequest
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SampleAndroidScreen()
            }
        }
    }
}

@Composable
private fun SampleAndroidScreen() {
    val facade = SampleAndroidApp.facade

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
    val telemetryEvents = facade.telemetryEvents

    LaunchedEffect(Unit) {
        while (true) {
            tasks = facade.listTasks()
            delay(1200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Meeseeks Android Sample", style = MaterialTheme.typography.headlineSmall)
        Text("Encryption toggle path is implemented in shared core config and fixed at app startup.")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoScenario.entries.forEach { candidate ->
                Button(onClick = { scenario = candidate }) {
                    Text(if (scenario == candidate) "* ${candidate.title}" else candidate.title)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleRow(label = "Periodic", checked = periodic) { periodic = it }
            ToggleRow(label = "Network", checked = requiresNetwork) { requiresNetwork = it }
            ToggleRow(label = "Charging", checked = requiresCharging) { requiresCharging = it }
            ToggleRow(label = "BatteryNotLow", checked = requiresBatteryNotLow) { requiresBatteryNotLow = it }
            ToggleRow(label = "HighPriority", checked = highPriority) { highPriority = it }
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
                }.getOrElse { throwable ->
                    "Error: ${throwable.message}"
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
            label = { Text("Task ID for cancel/reschedule") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                statusMessage = runCatching {
                    facade.cancel(TaskId(selectedTaskId))
                    "Cancelled $selectedTaskId"
                }.getOrElse { throwable ->
                    "Error: ${throwable.message}"
                }
            }) {
                Text("Cancel Task")
            }

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
                }.getOrElse { throwable ->
                    "Error: ${throwable.message}"
                }
            }) {
                Text("Reschedule Task")
            }
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
            items(telemetryEvents.value.takeLast(25)) { event ->
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
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
