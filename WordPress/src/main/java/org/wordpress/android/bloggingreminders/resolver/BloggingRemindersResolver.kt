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
import org.wordpress.android.bloggingreminders.provider.SiteIDBloggingReminderMap
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import javax.inject.Named

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
    @Named(APPLICATION_SCOPE) private val coroutineScope: CoroutineScope
) {
    fun trySyncBloggingReminders(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val isFeatureFlagEnabled = jetpackBloggingRemindersSyncFlag.isEnabled()
        if (!isFeatureFlagEnabled) {
            onFailure()
            return
        }
        val isFirstTry = appPrefsWrapper.getIsFirstTryBloggingRemindersSyncJetpack()
        if (!isFirstTry) {
            onFailure()
            return
        }
        bloggingRemindersSyncAnalyticsTracker.trackStart()
        appPrefsWrapper.saveIsFirstTryBloggingRemindersSyncJetpack(false)
        val bloggingRemindersResultCursor = getBloggingRemindersSyncResultCursor()
        if (bloggingRemindersResultCursor != null) {
            val siteModelBloggingReminderMap = queryResult.getValue<SiteIDBloggingReminderMap>(
                        bloggingRemindersResultCursor, object : TypeToken<SiteIDBloggingReminderMap?>() {}.type
            ) ?: emptyMap()
            if (siteModelBloggingReminderMap.isNotEmpty()) {
                val success = syncBloggingReminders(siteModelBloggingReminderMap)
                if (success) onSuccess() else onFailure()
            } else {
                bloggingRemindersSyncAnalyticsTracker.trackFailed(ErrorType.NoBloggingRemindersFoundError)
                onFailure()
            }
        } else {
            bloggingRemindersSyncAnalyticsTracker.trackFailed(ErrorType.QueryBloggingRemindersError)
            onFailure()
        }
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
    private fun syncBloggingReminders(siteIDBloggingReminderMap: SiteIDBloggingReminderMap): Boolean {
        try {
            coroutineScope.launch {
                for ((siteId, bloggingReminder) in siteIDBloggingReminderMap) {
                    if (siteId == null || bloggingReminder == null) {
                        continue
                    }
                    val siteLocalId = siteStore.getLocalIdForRemoteSiteId(siteId)
                    val isBloggingReminderAlreadySet = bloggingRemindersStore.bloggingRemindersModel(siteLocalId)
                            .first().enabledDays.isNotEmpty()
                    if (siteLocalId != 0 && !isBloggingReminderAlreadySet) {
                        bloggingRemindersStore.updateBloggingReminders(bloggingReminder.copy(siteId = siteLocalId))
                    }
                }
            }
            bloggingRemindersSyncAnalyticsTracker.trackSuccess()
            return true
        } catch (exception: Exception) {
            bloggingRemindersSyncAnalyticsTracker.trackFailed(ErrorType.UpdateBloggingRemindersError)
            return false
        }
    }
}
