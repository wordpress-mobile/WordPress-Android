package org.wordpress.android.workers

import android.content.Context
import android.content.Intent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.ActivityLauncher
import javax.inject.Inject

class CreateSiteNotificationHandler @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore
) : LocalNotificationHandler {
    override fun shouldShowNotification(): Boolean {
        return accountStore.hasAccessToken() && !siteStore.hasSite()
    }

    override fun buildIntent(context: Context): Intent {
        return ActivityLauncher.createMainActivityAndSiteCreationActivityIntent(context)
    }
}
