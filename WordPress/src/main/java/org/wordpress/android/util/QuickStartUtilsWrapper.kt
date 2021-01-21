package org.wordpress.android.util

import android.text.Spannable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class QuickStartUtilsWrapper
@Inject constructor(
    private val quickStartStore: QuickStartStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val resourceProvider: ResourceProvider
) {
    fun isQuickStartInProgress(): Boolean {
        return selectedSiteRepository.getSelectedSite()
                ?.let { QuickStartUtils.isQuickStartInProgress(quickStartStore, it.id) } ?: false
    }

    @JvmOverloads
    fun stylizeQuickStartPrompt(
        messageId: Int,
        iconId: Int = QuickStartUtils.ICON_NOT_SET
    ): Spannable {
        return QuickStartUtils.stylizeQuickStartPrompt(resourceProvider, messageId, iconId)
    }

    fun isEveryQuickStartTaskDone(siteId: Int): Boolean {
        return QuickStartUtils.isEveryQuickStartTaskDone(quickStartStore, siteId)
    }

    fun getTaskCompletedTracker(task: QuickStartTask): Stat {
        return QuickStartUtils.getTaskCompletedTracker(task)
    }
}
