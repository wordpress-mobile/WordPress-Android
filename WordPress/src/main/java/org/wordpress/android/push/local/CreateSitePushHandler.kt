package org.wordpress.android.push.local

import android.content.Context
import android.content.Intent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.local.LocalPushHandlerFactory.LocalPushHandler
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.main.WPMainActivity
import javax.inject.Inject

class CreateSitePushHandler
@Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore
) : LocalPushHandler {
    override fun shouldShowNotification(): Boolean {
        return accountStore.hasAccessToken() && !siteStore.hasSite()
    }

    override fun buildIntent(context: Context, id: Int): Intent {
        val intent = ActivityLauncher.buildSiteCreationOverMainActivityIntent(context)
        intent.putExtra(WPMainActivity.ARG_SOURCE_PUSH_ID, id)
        return intent
    }
}

