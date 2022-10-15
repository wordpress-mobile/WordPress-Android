package org.wordpress.android.bloggingreminders.resolver

import android.database.Cursor
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker.ErrorType
import org.wordpress.android.bloggingreminders.JetpackBloggingRemindersSyncFlag
import org.wordpress.android.bloggingreminders.provider.BloggingRemindersProvider
import org.wordpress.android.bloggingreminders.provider.RemoteSiteId
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersModelMapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.extensions.filterNull
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.workers.reminder.ReminderScheduler
import javax.inject.Inject
import javax.inject.Named

typealias RemoteSiteIdBloggingRemindersMap = Map<RemoteSiteId, BloggingRemindersModel>

class BloggingRemindersResolver @Inject constructor(
    private val jetpackBloggingRemindersSyncFlag: JetpackBloggingRemindersSyncFlag,
    private val contextProvider: ContextProvider,
    private val wordPressPublicData: WordPressPublicData,
    private val queryResult: QueryResult,
    private val contentResolverWrapper: ContentResolverWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val bloggingRemindersSyncAnalyticsTracker: BloggingRemindersSyncAnalyticsTracker,
    private val siteStore: SiteStore,
    private val bloggingRemindersStore: BloggingRemindersStore,
    @Named(APPLICATION_SCOPE) private val coroutineScope: CoroutineScope,
    private val reminderScheduler: ReminderScheduler,
    private val bloggingRemindersModelMapper: BloggingRemindersModelMapper
) {
    fun trySyncBloggingReminders(onSuccess: () -> Unit, onFailure: () -> Unit) {
        if (!shouldTrySyncBloggingReminders()) {
            onFailure()
            return
        }
        val bloggingRemindersResultCursor = getBloggingRemindersSyncResultCursor()
        if (bloggingRemindersResultCursor != null) {
            val remindersMap = mapBloggingRemindersResultCursor(bloggingRemindersResultCursor).filterNull()
            if (remindersMap.isNotEmpty()) {
                val success = setBloggingReminders(remindersMap)
                if (success) onSuccess() else onFailure()
            } else {
                bloggingRemindersSyncAnalyticsTracker.trackSuccess(0)
                onSuccess()
            }
        } else {
            bloggingRemindersSyncAnalyticsTracker.trackFailed(ErrorType.QueryBloggingRemindersError)
            onFailure()
        }
    }

    private fun mapBloggingRemindersResultCursor(bloggingRemindersResultCursor: Cursor) =
            queryResult.getValue<Map<RemoteSiteId?, BloggingRemindersModel?>>(
                    bloggingRemindersResultCursor,
                    object : TypeToken<Map<RemoteSiteId?, BloggingRemindersModel?>>() {}.type
            ) ?: emptyMap()

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

    private fun getBloggingRemindersSyncResultCursor(): Cursor? {
        val wordpressBloggingRemindersSyncUriValue =
                "content://${wordPressPublicData.currentPackageId()}.${BloggingRemindersProvider::class.simpleName}"
        return contentResolverWrapper.queryUri(
                contextProvider.getContext().contentResolver,
                wordpressBloggingRemindersSyncUriValue
        )
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun setBloggingReminders(remindersMap: RemoteSiteIdBloggingRemindersMap): Boolean {
        try {
            coroutineScope.launch {
                var syncCount = 0
                for ((siteId, bloggingReminder) in remindersMap) {
                    val siteLocalId = siteStore.getLocalIdForRemoteSiteId(siteId)
                    if (siteLocalId != 0 && !isBloggingReminderAlreadySet(siteLocalId)) {
                        val bloggingReminderWithLocalId = bloggingReminder.copy(siteId = siteLocalId)
                        bloggingRemindersStore.updateBloggingReminders(bloggingReminderWithLocalId)
                        setLocalReminderNotification(bloggingReminderWithLocalId)
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
