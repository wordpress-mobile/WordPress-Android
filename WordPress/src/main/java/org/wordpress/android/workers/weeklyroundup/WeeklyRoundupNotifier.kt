package org.wordpress.android.workers.weeklyroundup

import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationPushIds.WEEKLY_ROUNDUP_NOTIFICATION_ID
import org.wordpress.android.push.NotificationType.WEEKLY_ROUNDUP
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.StatsTimeframe.WEEK
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WeeklyRoundupNotifier @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val contextProvider: ContextProvider,
    private val resourceProvider: ResourceProvider,
    private val weeklyRoundupScheduler: WeeklyRoundupScheduler,
    private val notificationsTracker: SystemNotificationsTracker,
    private val siteUtils: SiteUtilsWrapper,
    private val weeklyRoundupRepository: WeeklyRoundupRepository,
    private val appPrefs: AppPrefsWrapper
) {
    fun shouldShowNotifications() = accountStore.hasAccessToken() && siteStore.hasSitesAccessedViaWPComRest()

    suspend fun buildNotifications(): List<WeeklyRoundupNotification> = coroutineScope {
        siteStore.sitesAccessedViaWPComRest
                .map { async { weeklyRoundupRepository.fetchWeeklyRoundupData(it) } }
                .awaitAll()
                .asSequence()
                .filterNotNull()
                .filter { appPrefs.shouldShowWeeklyRoundupNotification(it.site.siteId) }
                .sortedByDescending { it.score }
                .take(TOP_FIVE_SITES)
                .filter { it.views >= MIN_SITE_VIEWS }
                .map { buildNotification(it) }
                .toList()
                .reversed()
    }

    fun onNotificationsShown(notifications: List<WeeklyRoundupNotification>) {
        repeat(notifications.size) {
            notificationsTracker.trackShownNotification(WEEKLY_ROUNDUP)
        }

        weeklyRoundupScheduler.schedule()
    }

    private fun buildNotification(data: WeeklyRoundupData): WeeklyRoundupNotification {
        val context = contextProvider.getContext()
        val site = data.site
        val notificationId = WEEKLY_ROUNDUP_NOTIFICATION_ID + site.id
        return WeeklyRoundupNotification(
                id = notificationId,
                contentIntentBuilder = {
                    ActivityLauncher.buildStatsPendingIntentOverMainActivityInNewStack(
                            context,
                            site,
                            WEEK,
                            data.period,
                            WEEKLY_ROUNDUP,
                            notificationId,
                            FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
                    )
                },
                contentTitle = resourceProvider.getString(
                        R.string.weekly_roundup_notification_title,
                        siteUtils.getSiteNameOrHomeURL(site)
                ),
                contentText = resourceProvider.getString(
                        R.string.weekly_roundup_notification_text,
                        data.views,
                        data.likes,
                        data.comments
                )
        )
    }

    companion object {
        private const val TOP_FIVE_SITES = 5
        private const val MIN_SITE_VIEWS = 5
    }
}
