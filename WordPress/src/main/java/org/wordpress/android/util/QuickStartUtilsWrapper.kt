package org.wordpress.android.util

import android.content.Context
import android.text.Spannable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_STARTED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartPayload
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartVariant.NEXT_STEPS
import org.wordpress.android.ui.prefs.AppPrefs
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
        val siteId = site.id.toLong()

        if (shouldCancelCompleteAction(siteId, site, task)) {
            return
        }

        if (context != null) {
            QuickStartUtils.cancelQuickStartReminder(context)
        }

        quickStartStore.setDoneTask(siteId, task, true)
        analyticsTrackerWrapper.track(QuickStartUtils.getTaskCompletedTracker(task), mySiteImprovementsFeatureConfig)

        if (QuickStartUtils.isEveryQuickStartTaskDone(quickStartStore, site.id)) {
            quickStartStore.setQuickStartCompleted(siteId, true)
            analyticsTrackerWrapper.track(Stat.QUICK_START_ALL_TASKS_COMPLETED, mySiteImprovementsFeatureConfig)
            val payload = CompleteQuickStartPayload(site, NEXT_STEPS.toString())
            dispatcher.dispatch(SiteActionBuilder.newCompleteQuickStartAction(payload))
        } else if (quickStartEvent?.task == task) {
            AppPrefs.setQuickStartNoticeRequired(true)
        } else {
            if (context != null && quickStartStore.hasDoneTask(siteId, CREATE_SITE)) {
                val nextTask =
                        QuickStartUtils.getNextUncompletedQuickStartTaskForReminderNotification(
                                quickStartStore,
                                siteId,
                                task.taskType
                        )
                if (nextTask != null) {
                    QuickStartUtils.startQuickStartReminderTimer(context, nextTask)
                }
            }
        }
    }

    private fun shouldCancelCompleteAction(
        siteId: Long,
        site: SiteModel,
        task: QuickStartTask
    ): Boolean {
        return quickStartStore.getQuickStartCompleted(siteId) ||
                QuickStartUtils.isEveryQuickStartTaskDone(quickStartStore, site.id) ||
                quickStartStore.hasDoneTask(siteId, task) ||
                !QuickStartUtils.isQuickStartAvailableForTheSite(site)
    }

    fun startQuickStart(siteId: Int) {
        quickStartStore.setDoneTask(siteId.toLong(), CREATE_SITE, true)
        analyticsTrackerWrapper.track(QUICK_START_STARTED, mySiteImprovementsFeatureConfig)
    }
}
