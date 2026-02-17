package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.Serializable

@Serializable
public sealed class TaskStatus {
    @Serializable
    public data object Pending : TaskStatus()

    @Serializable
    public data object Running : TaskStatus()

    @Serializable
    public sealed class Finished : TaskStatus() {
        @Serializable
        public data object Cancelled : Finished()

        @Serializable
        public data object Completed : Finished()

        @Serializable
        public data object Failed : Finished()
    }
}
