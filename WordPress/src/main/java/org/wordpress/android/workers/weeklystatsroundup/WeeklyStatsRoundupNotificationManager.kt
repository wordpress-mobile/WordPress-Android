package org.wordpress.android.workers.weeklystatsroundup

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.push.NotificationType.CREATE_SITE
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OVERVIEW_ITEMS_TO_LOAD
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.workers.reminder.ReminderConfig
import javax.inject.Inject

class WeeklyStatsRoundupNotificationManager @Inject constructor(
    private val context: Context,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val weeklyStatsRoundupScheduler: WeeklyStatsRoundupScheduler,
    private val notificationsTracker: SystemNotificationsTracker
) {
    suspend fun scheduleNotificationIfNeeded(
        siteId: Int,
        reminderConfig: ReminderConfig,
        statsGranularity: StatsGranularity,
        statsSiteProvider: StatsSiteProvider
    ) {
            if (shouldShowNotification(statsGranularity, statsSiteProvider)) {
                Log.d("Weekly Stats Roundup: %s", siteId.toString())
//                weeklyStatsRoundupScheduler.schedule(siteId, reminderConfig )
            }
    }

    suspend fun shouldShowNotification(statsGranularity: StatsGranularity, statsSiteProvider: StatsSiteProvider): Boolean {
        fetchStats(statsGranularity, statsSiteProvider)
        return accountStore.hasAccessToken() && !siteStore.hasSite()
    }

    fun buildIntent(context: Context): Intent {
        return ActivityLauncher.createMainActivityAndSiteCreationActivityIntent(context, CREATE_SITE)
    }

    fun onNotificationShown() {
        notificationsTracker.trackShownNotification(CREATE_SITE)
    }

    fun notify(id: Int, notification: WeeklyStatsRoundupNotification) {
        NotificationManagerCompat
                .from(context)
                .notify(id, notification.asNotificationCompatBuilder(context).build())
    }

    private suspend fun fetchStats(statsGranularity: StatsGranularity, statsSiteProvider: StatsSiteProvider) {
        val response = visitsAndViewsStore.fetchVisits(
                statsSiteProvider.siteModel,
                statsGranularity,
                LimitMode.Top(OVERVIEW_ITEMS_TO_LOAD),
                forced = true
        )
        val model = response.model
        val error = response.error

        if (model != null && model.dates.isNotEmpty()) {
            model.dates.map {
                Log.d("Views:    ", it.views.toString())
                Log.d("Likes:    ", it.likes.toString())
                Log.d("Comments: ", it.comments.toString())
            }
        }
    }
}
