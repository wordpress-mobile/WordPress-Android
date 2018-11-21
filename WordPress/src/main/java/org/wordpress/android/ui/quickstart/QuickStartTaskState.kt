package org.wordpress.android.ui.quickstart

import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

// represents completion state of Quick Start tasks
data class QuickStartTaskState(
    val task: QuickStartTask,
    val isTaskCompleted: Boolean
)
