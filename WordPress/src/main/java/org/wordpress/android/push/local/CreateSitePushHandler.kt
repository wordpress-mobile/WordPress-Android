package org.wordpress.android.push.local

import android.content.Context
import android.content.Intent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.local.LocalPushHandlerFactory.LocalPushHandler
import org.wordpress.android.ui.ActivityLauncher
import javax.inject.Inject

class CreateSitePushHandler
@Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore
) : LocalPushHandler {
    override fun shouldShowNotification(): Boolean {
        return accountStore.hasAccessToken() && !siteStore.hasSite()
    }

    override fun buildIntent(context: Context): Intent {
        return ActivityLauncher.buildSiteCreationOverMainActivityIntent(context)
    }
}

