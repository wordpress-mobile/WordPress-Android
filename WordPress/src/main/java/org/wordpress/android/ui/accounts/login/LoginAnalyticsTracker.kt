package org.wordpress.android.ui.accounts.login

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step
import org.wordpress.android.util.analytics.AnalyticsUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginAnalyticsTracker @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val unifiedLoginTracker: UnifiedLoginTracker
) {
    fun trackAnalyticsSignIn(isWpcomLogin: Boolean) {
        AnalyticsUtils.trackAnalyticsSignIn(accountStore, siteStore, isWpcomLogin)
    }

    fun trackLoginAccessed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_ACCESSED)
    }

    fun trackLoginMagicLinkOpened() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED)
    }

    fun trackLoginMagicLinkSucceeded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_SUCCEEDED)
    }

    fun trackUrlFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_URL_FORM_VIEWED)
        unifiedLoginTracker.track(Flow.LOGIN_SITE_ADDRESS, Step.START)
    }

    fun trackConnectedSiteInfoRequested(url: String?) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_REQUESTED, mapOf("url" to url))
    }

    fun trackConnectedSiteInfoSucceeded(properties: Map<String, *>) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_SUCCEEDED, properties)
    }

    fun trackFailure(message: String?) {
        unifiedLoginTracker.trackFailure(message)
    }

    fun trackSubmitClicked() {
        unifiedLoginTracker.trackClick(Click.SUBMIT)
    }

    fun trackShowHelpClick() {
        unifiedLoginTracker.trackClick(Click.SHOW_HELP)
        unifiedLoginTracker.track(step = Step.HELP)
    }

    fun siteAddressFormScreenResumed() {
        unifiedLoginTracker.setStep(Step.START)
    }
}
