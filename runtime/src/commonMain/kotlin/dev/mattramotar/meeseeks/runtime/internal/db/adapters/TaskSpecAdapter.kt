package dev.mattramotar.meeseeks.runtime.internal.db.adapters

import dev.mattramotar.meeseeks.runtime.db.TaskSpec

internal val TaskSpecAdapter = TaskSpec.Adapter(
    stateAdapter = TaskStateAdapter,
    backoff_policyAdapter = BackoffPolicyAdapter
)