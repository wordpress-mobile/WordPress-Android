package org.wordpress.android.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class DismissNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val notificationId: Int = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
        }
    }

    companion object {
        private const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"

        fun newIntent(context: Context, notificationId: Int): Intent =
            Intent(context, DismissNotificationReceiver::class.java).apply {
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
    }
}
