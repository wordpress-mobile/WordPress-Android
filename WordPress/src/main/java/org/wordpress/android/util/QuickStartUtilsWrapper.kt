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
        return QuickStartUtils.stylizeQuickStartPrompt(
                resourceProvider = resourceProvider,
                activityContext = activityContext,
                messageId = messageId,
                isThemedSnackbar = false,
                iconId = iconId
        )
    }

    @JvmOverloads
    fun stylizeThemedQuickStartPrompt(
        activityContext: Context,
        messageId: Int,
        iconId: Int = QuickStartUtils.ICON_NOT_SET
    ): Spannable {
        return QuickStartUtils.stylizeQuickStartPrompt(
                resourceProvider = resourceProvider,
                activityContext = activityContext,
                messageId = messageId,
                isThemedSnackbar = true,
                iconId = iconId
        )
    }

    fun isEveryQuickStartTaskDone(siteId: Int): Boolean {
        return QuickStartUtils.isEveryQuickStartTaskDone(quickStartStore, siteId)
    }

    fun getTaskCompletedTracker(task: QuickStartTask): Stat {
        return QuickStartUtils.getTaskCompletedTracker(task)
    }
}
