package org.wordpress.android.ui.mysite.jetpackbadge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.util.BuildConfigWrapper
import javax.inject.Inject

class JetpackAppInstallReceiver : BroadcastReceiver() {
    @Inject lateinit var mBuildConfigWrapper: BuildConfigWrapper
    @Inject lateinit var gcmMessageHandler: GCMMessageHandler

    override fun onReceive(context: Context?, intent: Intent?) {
        (context?.applicationContext as WordPress).component().inject(this)

        // TODO: Remove this logging before merge
        StringBuilder().apply {
            append("Action: ${intent?.action}\n")
            append("URI: ${intent?.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Log.d(TAG, log)
                Toast.makeText(context, log, Toast.LENGTH_LONG).show()
            }
        }

        if (intent != null && !mBuildConfigWrapper.isJetpackApp) {
            disableNotifications(context)
        }
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
        private const val TAG = "JetpackAppInstallReceiver"

        fun newIntent(context: Context): Intent = Intent(context, JetpackAppInstallReceiver::class.java)
    }
}
