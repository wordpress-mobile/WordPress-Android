package org.wordpress.android.workers

import android.content.Context
import android.content.Intent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class CreateSitePushHandler @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore
) : LocalPushHandler {
    override fun shouldShowNotification(): Boolean {
        return accountStore.hasAccessToken() && !siteStore.hasSite()
    }

    override fun buildIntent(context: Context): Intent {
        return Intent() // TODO: Replace this with respective intent in ActivityLauncher
    }
}
