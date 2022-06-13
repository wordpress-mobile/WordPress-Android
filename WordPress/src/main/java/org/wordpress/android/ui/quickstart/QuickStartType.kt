package org.wordpress.android.ui.quickstart

import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType

sealed class QuickStartType(
    val label: String,
    val trackingLabel: String,
    val tasks: List<QuickStartTask>
) {
    object NewSiteQuickStartType : QuickStartType(
            NEW_SITE,
            NEW_SITE_TRACKING_LABEL,
            QuickStartNewSiteTask.values().toList()
    )
    object ExistingSiteQuickStartType : QuickStartType(
            EXISTING_SITE,
            EXISTING_SITE_TRACKING_LABEL,
            QuickStartExistingSiteTask.values().toList()
    )

    val taskTypes = tasks.map { it.taskType }.distinct()

    fun getTaskFromString(string: String?): QuickStartTask {
        val quickStartTask: QuickStartTask = when (this) {
            is NewSiteQuickStartType -> QuickStartNewSiteTask.fromString(string)
            is ExistingSiteQuickStartType -> QuickStartExistingSiteTask.fromString(string)
        }
        return quickStartTask
    }

    fun startQuickStart(
        quickStartStore: QuickStartStore,
        siteLocalId: Long,
        isSiteTitleTaskCompleted: Boolean
    ) {
        quickStartStore.setQuickStartCompleted(siteLocalId, false)
        when (this) {
            is NewSiteQuickStartType -> {
                quickStartStore.setDoneTask(siteLocalId, QuickStartNewSiteTask.CREATE_SITE, true)
                if (isSiteTitleTaskCompleted) {
                    quickStartStore.setDoneTask(siteLocalId, QuickStartNewSiteTask.UPDATE_SITE_TITLE, true)
                }
            }
            is ExistingSiteQuickStartType -> Unit // Do Nothing
        }
    }

    fun isQuickStartInProgress(quickStartStore: QuickStartStore, siteLocalId: Long): Boolean {
        val isQuickStartCompleted = quickStartStore.getQuickStartCompleted(siteLocalId)
        return when (this) {
            is NewSiteQuickStartType -> !isQuickStartCompleted &&
                    quickStartStore.hasDoneTask(siteLocalId, QuickStartNewSiteTask.CREATE_SITE)
            is ExistingSiteQuickStartType -> quickStartStore.isQuickStartStatusSet(siteLocalId) &&
                    !isQuickStartCompleted
        }
    }

    fun isEveryQuickStartTaskDone(quickStartStore: QuickStartStore, siteLocalId: Long): Boolean {
        return quickStartStore.getDoneCount(siteLocalId) >= tasks
                .filter { it.taskType != QuickStartTaskType.UNKNOWN }.size
    }

    companion object {
        private const val NEW_SITE = "new-site"
        private const val EXISTING_SITE = "existing-site"

        private const val NEW_SITE_TRACKING_LABEL = "new_site"
        private const val EXISTING_SITE_TRACKING_LABEL = "existing_site"

        fun fromLabel(label: String): QuickStartType {
            return when (label) {
                NewSiteQuickStartType.label -> NewSiteQuickStartType
                ExistingSiteQuickStartType.label -> ExistingSiteQuickStartType
                else -> NewSiteQuickStartType
            }
        }
    }
}
