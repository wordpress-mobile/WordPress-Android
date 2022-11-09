package org.wordpress.android.ui.mysite.jetpackbadge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.push.GCMMessageHandler
import javax.inject.Inject

@AndroidEntryPoint
class JetpackAppInstallReceiver : BroadcastReceiver() {
    @Inject lateinit var gcmMessageHandler: GCMMessageHandler

    override fun onReceive(context: Context, intent: Intent) {
        disableNotifications(context)
    }

    private fun disableNotifications(context: Context) {
        // Turn toggle off on Notifications Settings screen
        val mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        mSharedPreferences
                .edit()
                .putBoolean(context.getString(R.string.wp_pref_notifications_main), false)
                .apply()

        gcmMessageHandler.removeAllNotifications(context)
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, JetpackAppInstallReceiver::class.java)
    }
}
