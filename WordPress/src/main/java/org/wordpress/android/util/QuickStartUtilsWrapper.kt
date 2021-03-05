package org.wordpress.android.util

import android.content.Context
import android.text.Spannable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_STARTED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig
import javax.inject.Inject

class QuickStartUtilsWrapper
@Inject constructor(
    private val quickStartStore: QuickStartStore,
    private val dispatcher: Dispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val mySiteImprovementsFeatureConfig: MySiteImprovementsFeatureConfig
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
                activityContext = activityContext,
                messageId = messageId,
                isThemedSnackbar = true,
                iconId = iconId
        )
    }

    fun isEveryQuickStartTaskDone(siteId: Int): Boolean {
        return QuickStartUtils.isEveryQuickStartTaskDone(quickStartStore, siteId)
    }

    fun isEveryQuickStartTaskDoneForType(siteId: Int, type: QuickStartTaskType): Boolean {
        return quickStartStore.getUncompletedTasksByType(siteId.toLong(), type).isEmpty()
    }

    fun getTaskCompletedTracker(task: QuickStartTask): Stat {
        return QuickStartUtils.getTaskCompletedTracker(task)
    }

    @JvmOverloads
    fun completeTaskAndRemindNextOne(
        task: QuickStartTask,
        site: SiteModel,
        quickStartEvent: QuickStartEvent? = null,
        context: Context?
    ) {
        QuickStartUtils.completeTaskAndRemindNextOne(
                quickStartStore = quickStartStore,
                task = task,
                dispatcher = dispatcher,
                site = site,
                quickStartEvent = quickStartEvent,
                context = context,
                track = {
                    analyticsTrackerWrapper.track(it, mySiteImprovementsFeatureConfig)
                })
    }

    fun startQuickStart(siteId: Int) {
        quickStartStore.setDoneTask(siteId.toLong(), CREATE_SITE, true)
        analyticsTrackerWrapper.track(QUICK_START_STARTED, mySiteImprovementsFeatureConfig)
    }
}
