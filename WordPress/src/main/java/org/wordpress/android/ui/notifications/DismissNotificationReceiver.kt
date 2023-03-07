package org.wordpress.android.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import javax.inject.Inject

class DismissNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    override fun onReceive(context: Context?, intent: Intent?) {
        (context?.applicationContext as WordPress).component().inject(this)
        if (intent != null) {
            dismissNotification(intent, context)
            trackAnalyticsEvent(intent)
        }
    }

    private fun trackAnalyticsEvent(intent: Intent) {
        val stat = intent.getSerializableExtraCompat<Stat>(EXTRA_STAT_TO_TRACK)
        if (stat != null) {
            analyticsTrackerWrapper.track(stat)
        }
    }

    private fun dismissNotification(intent: Intent, context: Context) {
        val notificationId: Int = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    companion object {
        private const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        private const val EXTRA_STAT_TO_TRACK = "EXTRA_STAT_TO_TRACK"

        fun newIntent(context: Context, notificationId: Int, stat: Stat? = null): Intent =
            Intent(context, DismissNotificationReceiver::class.java).apply {
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_STAT_TO_TRACK, stat)
            }
    }
}
