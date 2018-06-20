package org.wordpress.android.ui.quickstart

import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

data class QuickStartDetailModel(
    val task: QuickStartTask,
    val isTaskCompleted: Boolean)
