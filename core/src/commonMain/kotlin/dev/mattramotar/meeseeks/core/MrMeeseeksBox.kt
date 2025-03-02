package dev.mattramotar.meeseeks.core


/**
 * Central task manager.
 */
interface MrMeeseeksBox {


    /**
     * Summons a new [MrMeeseeks], scheduling the [task] for background execution.
     *
     * @param task The [Task] to schedule.
     * @param parameters A [TaskParameters] object containing information [MrMeeseeks] will need.
     * @return [MrMeeseeksId] identifying the summoned [MrMeeseeks].
     */
    fun summon(task: Task, parameters: TaskParameters): MrMeeseeksId

    /**
     * Sends a specific [MrMeeseeks] back to the box, canceling its scheduled or ongoing work.
     *
     * @param id A [MrMeeseeksId] identifying a specific [MrMeeseeks].
     */
    fun sendBackToBox(id: MrMeeseeksId)

    /**
     * Sends all currently active [MrMeeseeks] back to the box, removing them from scheduling.
     */
    fun sendAllBackToBox()

    /**
     * Triggers an immediate scheduling check.
     */
    fun triggerCheckForDueTasks()
}