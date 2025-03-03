package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable

@Serializable
sealed class TaskStatus {
    @Serializable
    data object Pending : TaskStatus()

    @Serializable
    data object Running : TaskStatus()

    @Serializable
    sealed class Finished : TaskStatus() {
        @Serializable
        data object Cancelled : Finished()

        @Serializable
        data object Completed : Finished()
    }
}

