package org.wordpress.android.bloggingreminders.resolver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker.ErrorType
import org.wordpress.android.bloggingreminders.JetpackBloggingRemindersSyncFlag
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntity.BloggingReminders
import org.wordpress.android.localcontentmigration.LocalContentEntityData.BloggingRemindersData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersModelMapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.workers.reminder.ReminderScheduler
import javax.inject.Inject
import javax.inject.Named

class BloggingRemindersResolver @Inject constructor(
    private val jetpackBloggingRemindersSyncFlag: JetpackBloggingRemindersSyncFlag,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val bloggingRemindersSyncAnalyticsTracker: BloggingRemindersSyncAnalyticsTracker,
    private val siteStore: SiteStore,
    private val bloggingRemindersStore: BloggingRemindersStore,
    @Named(APPLICATION_SCOPE) private val coroutineScope: CoroutineScope,
    private val reminderScheduler: ReminderScheduler,
    private val bloggingRemindersModelMapper: BloggingRemindersModelMapper,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun trySyncBloggingReminders(onSuccess: () -> Unit, onFailure: () -> Unit) {
        if (!shouldTrySyncBloggingReminders()) {
            onFailure()
            return
        }
        val (reminders) = localMigrationContentResolver.getDataForEntityType<BloggingRemindersData>(BloggingReminders)
        if (reminders.isNotEmpty()) {
            val success = setBloggingReminders(reminders)
            if (success) onSuccess() else onFailure()
        } else {
            bloggingRemindersSyncAnalyticsTracker.trackSuccess(0)
            onSuccess()
        }
    }

    @Suppress("ReturnCount")
    private fun shouldTrySyncBloggingReminders(): Boolean {
        val isFeatureFlagEnabled = jetpackBloggingRemindersSyncFlag.isEnabled()
        if (!isFeatureFlagEnabled) {
            return false
        }
        val isFirstTry = appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()
        if (!isFirstTry) {
            return false
        }
        bloggingRemindersSyncAnalyticsTracker.trackStart()
        appPrefsWrapper.saveIsFirstTryBloggingRemindersSyncJetpack(false)
        return true
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun setBloggingReminders(reminders: List<BloggingRemindersModel>): Boolean {
        try {
            coroutineScope.launch {
                var syncCount = 0
                for (bloggingReminder in reminders) {
                    val site = siteStore.getSiteByLocalId(bloggingReminder.siteId)
                    if (site != null && !isBloggingReminderAlreadySet(bloggingReminder.siteId)) {
                        bloggingRemindersStore.updateBloggingReminders(bloggingReminder)
                        setLocalReminderNotification(bloggingReminder)
                        syncCount = syncCount.inc()
                    }
                }
                bloggingRemindersSyncAnalyticsTracker.trackSuccess(syncCount)
            }
            return true
        } catch (exception: Exception) {
            bloggingRemindersSyncAnalyticsTracker.trackFailed(ErrorType.UpdateBloggingRemindersError)
            return false
        }
    }

    private suspend fun isBloggingReminderAlreadySet(siteLocalId: Int) =
            bloggingRemindersStore.bloggingRemindersModel(siteLocalId).first().enabledDays.isNotEmpty()

    private fun setLocalReminderNotification(bloggingRemindersModel: BloggingRemindersModel) {
        val bloggingRemindersUiModel = bloggingRemindersModelMapper.toUiModel(bloggingRemindersModel)
        reminderScheduler.schedule(
                bloggingRemindersUiModel.siteId,
                bloggingRemindersUiModel.hour,
                bloggingRemindersUiModel.minute,
                bloggingRemindersUiModel.toReminderConfig()
        )
    }
}
