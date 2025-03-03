package dev.mattramotar.meeseeks.runtime

import dev.mattramotar.meeseeks.runtime.impl.MeeseeksBoxSingleton


/**
 * Central task manager.
 */
interface MeeseeksBox {


    /**
     * Summons a new [MrMeeseeks], scheduling the [task] for background execution.
     *
     * @param task The [Task] to schedule.
     * @return [MrMeeseeksId] identifying the summoned [MrMeeseeks].
     */
    fun summon(task: Task): MrMeeseeksId

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

    companion object {
        val value: MeeseeksBox
            get() {
                return MeeseeksBoxSingleton.meeseeksBox
            }
    }
}