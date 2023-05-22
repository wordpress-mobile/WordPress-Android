package org.wordpress.android.ui.domains

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.SiteCreationDomainPurchasingFeatureConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainsRegistrationTracker @Inject constructor(
    val tracker: AnalyticsTrackerWrapper,
    private val purchasingFeatureConfig: SiteCreationDomainPurchasingFeatureConfig,
) {
    private enum class PROPERTY(val key: String) {
        ORIGIN("origin"),
    }

    private enum class VALUE(val key: String) {
        ORIGIN_MENU("menu"),
        ORIGIN_SITE_CREATION("site_creation"),
    }

    fun trackDomainsPurchaseWebviewViewed(site: SiteModel?, isSiteCreation: Boolean) {
        if (purchasingFeatureConfig.isEnabledOrManuallyOverridden()) {
            val origin = if (isSiteCreation) VALUE.ORIGIN_SITE_CREATION.key else VALUE.ORIGIN_MENU.key
            tracker.track(
                AnalyticsTracker.Stat.DOMAINS_PURCHASE_WEBVIEW_VIEWED, site, mapOf(PROPERTY.ORIGIN.key to origin)
            )
        } else {
            tracker.track(AnalyticsTracker.Stat.DOMAINS_PURCHASE_WEBVIEW_VIEWED, site)
        }
    }

    fun trackDomainCreditSuggestionQueried() {
        tracker.track(AnalyticsTracker.Stat.DOMAIN_CREDIT_SUGGESTION_QUERIED)
    }

    fun trackDomainsSearchSelectDomainTapped(site: SiteModel?) {
        tracker.track(AnalyticsTracker.Stat.DOMAINS_SEARCH_SELECT_DOMAIN_TAPPED, site)
    }

    fun trackDomainsRegistrationFormSubmitted() {
        tracker.track(AnalyticsTracker.Stat.DOMAINS_REGISTRATION_FORM_SUBMITTED)
    }

    fun trackDomainsPurchaseDomainSuccess(isSiteCreation: Boolean) {
        val origin = if (isSiteCreation) VALUE.ORIGIN_SITE_CREATION.key else VALUE.ORIGIN_MENU.key
        tracker.track(AnalyticsTracker.Stat.DOMAINS_PURCHASE_DOMAIN_SUCCESS, mapOf(PROPERTY.ORIGIN.key to origin))
    }

    fun trackDomainCreditNameSelected() {
        tracker.track(AnalyticsTracker.Stat.DOMAIN_CREDIT_NAME_SELECTED)
    }

    fun trackDomainsRegistrationFormViewed() {
        tracker.track(AnalyticsTracker.Stat.DOMAINS_REGISTRATION_FORM_VIEWED)
    }
}
