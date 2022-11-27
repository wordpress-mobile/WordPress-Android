package org.wordpress.android.ui.deeplinks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import org.wordpress.android.util.AppLog.T.UTILS

@AndroidEntryPoint
class JetpackAppUninstallReceiver : BroadcastReceiver() {
    @Inject lateinit var openWebLinksWithJetpackHelper: DeepLinkOpenWebLinksWithJetpackHelper

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PACKAGE_FULLY_REMOVED -> {
                onJetpackUninstalled()
                AppLog.i(UTILS,"JetpackAppUninstallReceiver ACTION_PACKAGE_FULLY_REMOVED handled")
            }
        }
    }

    private fun onJetpackUninstalled() {
        // Toggle the appPref to off + re-enable components
        openWebLinksWithJetpackHelper.onJetpackUninstalled()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, JetpackAppUninstallReceiver::class.java)
    }
}
