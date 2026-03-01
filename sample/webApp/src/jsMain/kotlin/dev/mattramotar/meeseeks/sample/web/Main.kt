package dev.mattramotar.meeseeks.sample.web

import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.sample.demo.DemoWebAppContext
import dev.mattramotar.meeseeks.sample.demo.DemoSampleConfig
import dev.mattramotar.meeseeks.sample.demo.DemoScenario
import dev.mattramotar.meeseeks.sample.demo.DemoScheduleRequest
import dev.mattramotar.meeseeks.sample.demo.DemoTaskFacadeFactory
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.HTMLSelectElement

private val facade = DemoTaskFacadeFactory.create(
    appContext = DemoWebAppContext,
    config = DemoSampleConfig(encryptionEnabled = false),
)

fun main() {
    val root = document.getElementById("app") as? HTMLDivElement ?: return

    val controls = div("row")
    val scenarioSelect = document.createElement("select") as HTMLSelectElement
    DemoScenario.entries.forEach { scenario ->
        val option = document.createElement("option") as HTMLOptionElement
        option.value = scenario.name
        option.textContent = scenario.title
        scenarioSelect.appendChild(option)
    }

    val periodic = checkbox("Periodic")
    val network = checkbox("Network")
    val charging = checkbox("Charging")
    val batteryNotLow = checkbox("BatteryNotLow")
    val highPriority = checkbox("HighPriority")

    controls.appendChild(label("Scenario"))
    controls.appendChild(scenarioSelect)
    controls.appendChild(periodic)
    controls.appendChild(network)
    controls.appendChild(charging)
    controls.appendChild(batteryNotLow)
    controls.appendChild(highPriority)

    val actions = div("row")
    val scheduleBtn = button("Schedule")
    val cancelAllBtn = button("Cancel All")
    val reschedulePendingBtn = button("Reschedule Pending")
    val exportTelemetryBtn = button("Export Telemetry")
    val clearTelemetryBtn = button("Clear Telemetry")
    val taskIdInput = document.createElement("input") as HTMLInputElement
    taskIdInput.type = "text"
    taskIdInput.placeholder = "Task ID for cancel/reschedule"
    val cancelBtn = button("Cancel Task")
    val rescheduleBtn = button("Reschedule Task")

    actions.appendChild(scheduleBtn)
    actions.appendChild(cancelAllBtn)
    actions.appendChild(reschedulePendingBtn)
    actions.appendChild(exportTelemetryBtn)
    actions.appendChild(clearTelemetryBtn)
    actions.appendChild(taskIdInput)
    actions.appendChild(cancelBtn)
    actions.appendChild(rescheduleBtn)

    val statusLine = document.createElement("div") as HTMLDivElement
    statusLine.textContent = "Ready"

    val tasksTitle = document.createElement("h3") as HTMLElement
    tasksTitle.textContent = "Tasks"
    val tasksPre = document.createElement("pre") as HTMLPreElement

    val telemetryTitle = document.createElement("h3") as HTMLElement
    telemetryTitle.textContent = "Telemetry (Recent)"
    val telemetryPre = document.createElement("pre") as HTMLPreElement

    val telemetryStatsTitle = document.createElement("h3") as HTMLElement
    telemetryStatsTitle.textContent = "Telemetry Statistics JSON"
    val telemetryStatsPre = document.createElement("pre") as HTMLPreElement

    val telemetryEventsTitle = document.createElement("h3") as HTMLElement
    telemetryEventsTitle.textContent = "Telemetry Events JSON"
    val telemetryEventsPre = document.createElement("pre") as HTMLPreElement

    root.appendChild(controls)
    root.appendChild(actions)
    root.appendChild(statusLine)
    root.appendChild(tasksTitle)
    root.appendChild(tasksPre)
    root.appendChild(telemetryTitle)
    root.appendChild(telemetryPre)
    root.appendChild(telemetryStatsTitle)
    root.appendChild(telemetryStatsPre)
    root.appendChild(telemetryEventsTitle)
    root.appendChild(telemetryEventsPre)

    fun requestFromControls(): DemoScheduleRequest {
        return DemoScheduleRequest(
            scenario = DemoScenario.fromName(scenarioSelect.value),
            periodic = periodic.checked,
            requiresNetwork = network.checked,
            requiresCharging = charging.checked,
            requiresBatteryNotLow = batteryNotLow.checked,
            highPriority = highPriority.checked,
        )
    }

    fun refresh() {
        tasksPre.textContent = facade.listTasks()
            .joinToString(separator = "\n") { task ->
                "${task.id.value} | ${task.status} | ${task.task.payload::class.simpleName}"
            }

        telemetryPre.textContent = facade.telemetryEvents.value.takeLast(25).joinToString("\n")
    }

    scheduleBtn.onclick = {
        statusLine.textContent = runCatching {
            val id = facade.schedule(requestFromControls())
            refresh()
            "Scheduled ${id.value}"
        }.getOrElse { error ->
            "Error: ${error.message}"
        }
        null
    }

    cancelAllBtn.onclick = {
        facade.cancelAll()
        refresh()
        statusLine.textContent = "Cancelled all tasks"
        null
    }

    reschedulePendingBtn.onclick = {
        facade.reschedulePendingTasks()
        refresh()
        statusLine.textContent = "Rescheduled pending tasks"
        null
    }

    cancelBtn.onclick = {
        statusLine.textContent = runCatching {
            facade.cancel(TaskId(taskIdInput.value))
            refresh()
            "Cancelled ${taskIdInput.value}"
        }.getOrElse { error ->
            "Error: ${error.message}"
        }
        null
    }

    rescheduleBtn.onclick = {
        statusLine.textContent = runCatching {
            facade.reschedule(TaskId(taskIdInput.value), requestFromControls())
            refresh()
            "Rescheduled ${taskIdInput.value}"
        }.getOrElse { error ->
            "Error: ${error.message}"
        }
        null
    }

    exportTelemetryBtn.onclick = {
        val snapshot = facade.telemetrySnapshot()
        telemetryStatsPre.textContent = snapshot.statisticsJson
        telemetryEventsPre.textContent = snapshot.eventsJson
        statusLine.textContent = "Exported telemetry snapshots"
        null
    }

    clearTelemetryBtn.onclick = {
        facade.clearTelemetry()
        telemetryStatsPre.textContent = ""
        telemetryEventsPre.textContent = ""
        refresh()
        statusLine.textContent = "Cleared telemetry"
        null
    }

    window.setInterval({ refresh() }, 1200)
    refresh()
}

private fun div(className: String): HTMLDivElement {
    val node = document.createElement("div") as HTMLDivElement
    node.className = className
    return node
}

private fun button(text: String): HTMLButtonElement {
    val node = document.createElement("button") as HTMLButtonElement
    node.textContent = text
    return node
}

private fun label(text: String): HTMLDivElement {
    val node = document.createElement("div") as HTMLDivElement
    node.textContent = text
    return node
}

private fun checkbox(label: String): HTMLElement {
    val wrapper = document.createElement("div") as HTMLDivElement
    val input = document.createElement("input") as HTMLInputElement
    input.type = "checkbox"
    input.id = "ck-${label.lowercase()}"
    val text = document.createElement("label") as HTMLElement
    text.textContent = label
    wrapper.appendChild(input)
    wrapper.appendChild(text)
    return wrapper
}

private val HTMLElement.checked: Boolean
    get() {
        val input = this.querySelector("input") as? HTMLInputElement
        return input?.checked == true
    }
