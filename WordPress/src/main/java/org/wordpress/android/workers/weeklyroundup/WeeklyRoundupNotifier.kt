package org.wordpress.android.workers.weeklyroundup

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationPushIds.WEEKLY_ROUNDUP_NOTIFICATION_ID
import org.wordpress.android.push.NotificationType.WEEKLY_ROUNDUP
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.stats.StatsTimeframe.WEEK
import org.wordpress.android.ui.stats.refresh.StatsActivity
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
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteUtils: SiteUtilsWrapper
) {
    fun shouldShowNotifications() = accountStore.hasAccessToken() && siteStore.hasSitesAccessedViaWPComRest()

    fun buildNotifications(): List<WeeklyRoundupNotification> {
        val site = selectedSiteRepository.getSelectedSite() ?: siteStore.sitesAccessedViaWPComRest[0]

        val notification = buildNotification(site)

        return listOf(notification)
    }

    fun onNotificationsShown(notifications: List<WeeklyRoundupNotification>) {
        notificationsTracker.trackShownNotification(WEEKLY_ROUNDUP)

        weeklyRoundupScheduler.schedule()
    }

    private fun buildNotification(site: SiteModel): WeeklyRoundupNotification {
        val context = contextProvider.getContext()
        val notificationId = WEEKLY_ROUNDUP_NOTIFICATION_ID + site.id
        return WeeklyRoundupNotification(
                id = notificationId,
                contentIntentBuilder = {
                    PendingIntent.getActivity(
                            context,
                            notificationId,
                            StatsActivity.buildIntent(context, site, WEEK),
                            FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
                    )
                },
                contentTitle = resourceProvider.getString(
                        R.string.weekly_roundup_notification_title,
                        siteUtils.getSiteNameOrHomeURL(site)
                ),
                contentText = resourceProvider.getString(
                        R.string.weekly_roundup_notification_text,
                        0,
                        0,
                        0
                ),
        )
    }
}
