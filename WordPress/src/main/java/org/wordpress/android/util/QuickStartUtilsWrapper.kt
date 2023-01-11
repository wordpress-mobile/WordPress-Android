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
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartPayload
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartVariant.NEXT_STEPS
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType
import javax.inject.Inject

class QuickStartUtilsWrapper
@Inject constructor(
    private val quickStartStore: QuickStartStore,
    private val dispatcher: Dispatcher
) {
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
            iconId = iconId
        )
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
        context: Context?,
        quickStartType: QuickStartType
    ) {
        val siteLocalId = site.id.toLong()

        if (shouldCancelCompleteAction(siteLocalId, site, task, quickStartType)) {
            return
        }

        if (context != null) {
            QuickStartUtils.cancelQuickStartReminder(context)
        }

        quickStartStore.setDoneTask(siteLocalId, task, true)

        if (quickStartType.isEveryQuickStartTaskDone(quickStartStore, siteLocalId)) {
            quickStartStore.setQuickStartCompleted(siteLocalId, true)
            val payload = CompleteQuickStartPayload(site, NEXT_STEPS.toString())
            dispatcher.dispatch(SiteActionBuilder.newCompleteQuickStartAction(payload))
        } else {
            if (quickStartEvent?.task == task) AppPrefs.setQuickStartNoticeRequired(true)

            if (context != null && quickStartType.isQuickStartInProgress(quickStartStore, siteLocalId)) {
                val nextTask =
                    QuickStartUtils.getNextUncompletedQuickStartTaskForReminderNotification(
                        quickStartStore,
                        siteLocalId,
                        task.taskType,
                        quickStartType
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
        task: QuickStartTask,
        quickStartType: QuickStartType
    ): Boolean {
        return quickStartStore.getQuickStartCompleted(siteLocalId) ||
                quickStartType.isEveryQuickStartTaskDone(quickStartStore, siteLocalId) ||
                quickStartStore.hasDoneTask(siteLocalId, task) ||
                !QuickStartUtils.isQuickStartAvailableForTheSite(site)
    }

    fun startQuickStart(
        siteLocalId: Int,
        isSiteTitleTaskCompleted: Boolean,
        quickStartType: QuickStartType,
        quickStartTracker: QuickStartTracker
    ) {
        quickStartType.startQuickStart(quickStartStore, siteLocalId.toLong(), isSiteTitleTaskCompleted)
        quickStartTracker.track(QUICK_START_STARTED)
    }

    fun getNextUncompletedQuickStartTask(quickStartType: QuickStartType, siteLocalId: Long) =
        QuickStartUtils.getNextUncompletedQuickStartTask(quickStartStore, quickStartType, siteLocalId)
}
