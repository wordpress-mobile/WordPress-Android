package org.wordpress.android.ui.mysite.jetpackbadge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.prefs.notifications.usecase.UpdateNotificationSettingsUseCase
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class JetpackAppInstallReceiver : BroadcastReceiver() {
    @Inject
    lateinit var updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase

    @Inject
    lateinit var gcmMessageHandler: GCMMessageHandler

    @Inject
    @Named(APPLICATION_SCOPE)
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        disableNotifications(context)
    }

    private fun disableNotifications(context: Context) {
        // Turn toggle off on Notifications Settings screen
        applicationScope.launch { updateNotificationSettingsUseCase.updateNotificationSettings(false) }

        gcmMessageHandler.removeAllNotifications(context)
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, JetpackAppInstallReceiver::class.java)
    }
}
