package org.wordpress.android.workers.weeklyroundup

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class WeeklyRoundupNotifier @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
) {
    fun shouldShowNotifications() = accountStore.hasAccessToken() && siteStore.hasSitesAccessedViaWPComRest()

    fun buildNotifications(): List<WeeklyRoundupNotification> {
        return emptyList()
    }

    fun onNotificationsShown(notifications: List<WeeklyRoundupNotification>) {}
}
