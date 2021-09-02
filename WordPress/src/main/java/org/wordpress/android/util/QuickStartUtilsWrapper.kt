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
import javax.inject.Inject

@Suppress("TooManyFunctions")
class QuickStartUtilsWrapper
@Inject constructor(
    private val quickStartStore: QuickStartStore,
    private val dispatcher: Dispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun isQuickStartInProgress(siteLocalId: Int): Boolean {
        return QuickStartUtils.isQuickStartInProgress(quickStartStore, siteLocalId)
    }

    fun isQuickStartAvailableForTheSite(siteModel: SiteModel): Boolean {
        return QuickStartUtils.isQuickStartAvailableForTheSite(siteModel)
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

    fun isEveryQuickStartTaskDone(siteLocalId: Int): Boolean {
        return QuickStartUtils.isEveryQuickStartTaskDone(quickStartStore, siteLocalId)
    }

    fun isEveryQuickStartTaskDoneForType(siteLocalId: Int, type: QuickStartTaskType): Boolean {
        return quickStartStore.getUncompletedTasksByType(siteLocalId.toLong(), type).isEmpty()
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
        val siteLocalId = site.id.toLong()

        if (shouldCancelCompleteAction(siteLocalId, site, task)) {
            return
        }

        if (context != null) {
            QuickStartUtils.cancelQuickStartReminder(context)
        }

        quickStartStore.setDoneTask(siteLocalId, task, true)

        if (isEveryQuickStartTaskDone(site.id)) {
            quickStartStore.setQuickStartCompleted(siteLocalId, true)
            val payload = CompleteQuickStartPayload(site, NEXT_STEPS.toString())
            dispatcher.dispatch(SiteActionBuilder.newCompleteQuickStartAction(payload))
        } else {
            if (quickStartEvent?.task == task) AppPrefs.setQuickStartNoticeRequired(true)

            if (context != null && quickStartStore.hasDoneTask(siteLocalId, CREATE_SITE)) {
                val nextTask =
                        QuickStartUtils.getNextUncompletedQuickStartTaskForReminderNotification(
                                quickStartStore,
                                siteLocalId,
                                task.taskType
                        )
                if (nextTask != null) {
                    QuickStartUtils.startQuickStartReminderTimer(context, nextTask)
                }
            }
        }
    }

    private fun shouldCancelCompleteAction(
        siteLocalId: Long,
        site: SiteModel,
        task: QuickStartTask
    ): Boolean {
        return quickStartStore.getQuickStartCompleted(siteLocalId) ||
                isEveryQuickStartTaskDone(site.id) ||
                quickStartStore.hasDoneTask(siteLocalId, task) ||
                !QuickStartUtils.isQuickStartAvailableForTheSite(site)
    }

    fun startQuickStart(siteLocalId: Int) {
        quickStartStore.setDoneTask(siteLocalId.toLong(), CREATE_SITE, true)
        analyticsTrackerWrapper.track(QUICK_START_STARTED)
    }

    fun getNextUncompletedQuickStartTask(siteLocalId: Long) =
            QuickStartUtils.getNextUncompletedQuickStartTask(quickStartStore, siteLocalId)
}
