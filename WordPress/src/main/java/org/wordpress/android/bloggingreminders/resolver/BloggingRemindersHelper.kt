package org.wordpress.android.bloggingreminders.resolver

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker.ErrorType
import org.wordpress.android.bloggingreminders.JetpackBloggingRemindersSyncFlag
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntity.BloggingReminders
import org.wordpress.android.localcontentmigration.LocalContentEntityData.BloggingRemindersData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled.BloggingRemindersSyncDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted.BloggingRemindersSyncAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError.FailedToSaveBloggingRemindersWithException
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.orElse
import org.wordpress.android.localcontentmigration.thenWith
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersModelMapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.workers.reminder.ReminderScheduler
import javax.inject.Inject

class BloggingRemindersHelper @Inject constructor(
    private val jetpackBloggingRemindersSyncFlag: JetpackBloggingRemindersSyncFlag,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val bloggingRemindersSyncAnalyticsTracker: BloggingRemindersSyncAnalyticsTracker,
    private val siteStore: SiteStore,
    private val bloggingRemindersStore: BloggingRemindersStore,
    private val reminderScheduler: ReminderScheduler,
    private val bloggingRemindersModelMapper: BloggingRemindersModelMapper,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun migrateBloggingReminders() = if (!jetpackBloggingRemindersSyncFlag.isEnabled()) {
        Failure(BloggingRemindersSyncDisabled)
    } else if (!appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()) {
        Failure(BloggingRemindersSyncAlreadyAttempted)
    } else {
        bloggingRemindersSyncAnalyticsTracker.trackStart()
        appPrefsWrapper.saveIsFirstTryBloggingRemindersSyncJetpack(false)
        localMigrationContentResolver.getResultForEntityType<BloggingRemindersData>(BloggingReminders)
            .orElse {
                bloggingRemindersSyncAnalyticsTracker.trackFailed(ErrorType.QueryBloggingRemindersError)
                Failure(it)
            }
    }.thenWith(::setBloggingReminders)

    private fun setBloggingReminders(bloggingRemindersData: BloggingRemindersData) = runCatching {
        bloggingRemindersData.reminders.count { bloggingReminder ->
            siteStore.getSiteByLocalId(bloggingReminder.siteId)?.let { _ ->
                if (!isBloggingReminderAlreadySet(bloggingReminder.siteId)) {
                    updateBloggingReminders(bloggingReminder)
                    setLocalReminderNotification(bloggingReminder)
                    true
                } else {
                    false
                }
            } ?: false
        }.let { bloggingRemindersSyncAnalyticsTracker.trackSuccess(it) }
        Success(bloggingRemindersData)
    }.getOrElse { throwable ->
        bloggingRemindersSyncAnalyticsTracker.trackFailed(ErrorType.UpdateBloggingRemindersError)
        Failure(FailedToSaveBloggingRemindersWithException(throwable))
    }

    private fun isBloggingReminderAlreadySet(siteLocalId: Int) = runBlocking {
        bloggingRemindersStore.bloggingRemindersModel(siteLocalId).first().enabledDays.isNotEmpty()
    }

    private fun updateBloggingReminders(bloggingReminder: BloggingRemindersModel) = runBlocking {
        bloggingRemindersStore.updateBloggingReminders(bloggingReminder)
    }

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
