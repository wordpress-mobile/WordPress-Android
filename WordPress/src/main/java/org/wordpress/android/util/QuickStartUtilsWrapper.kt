package org.wordpress.android.util

import android.content.Context
import android.text.Spannable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class QuickStartUtilsWrapper
@Inject constructor(
    private val quickStartStore: QuickStartStore,
    private val resourceProvider: ResourceProvider
) {
    fun isQuickStartInProgress(siteId: Int): Boolean {
        return QuickStartUtils.isQuickStartInProgress(quickStartStore, siteId)
    }

    @JvmOverloads
    fun stylizeQuickStartPrompt(
        activityContext: Context,
        messageId: Int,
        iconId: Int = QuickStartUtils.ICON_NOT_SET
    ): Spannable {
        return QuickStartUtils.stylizeQuickStartPrompt(resourceProvider, activityContext, messageId, iconId)
    }

    fun isEveryQuickStartTaskDone(siteId: Int): Boolean {
        return QuickStartUtils.isEveryQuickStartTaskDone(quickStartStore, siteId)
    }

    fun getTaskCompletedTracker(task: QuickStartTask): Stat {
        return QuickStartUtils.getTaskCompletedTracker(task)
    }
}
