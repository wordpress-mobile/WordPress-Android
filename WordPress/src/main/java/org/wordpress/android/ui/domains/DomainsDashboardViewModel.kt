package org.wordpress.android.ui.domains

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.ui.domains.DomainsListItem.AddDomain
import org.wordpress.android.ui.domains.DomainsListItem.DomainBlurb
import org.wordpress.android.ui.domains.DomainsListItem.ManageDomains
import org.wordpress.android.ui.domains.DomainsListItem.PrimaryDomain
import org.wordpress.android.ui.domains.DomainsListItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsListItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsListItem.SiteDomainsHeader
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

@Suppress("TooManyFunctions")
class DomainsDashboardViewModel @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val htmlMessageUtils: HtmlMessageUtils
) : ViewModel() {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _uiModel = MutableLiveData<List<DomainsListItem>>()
    val uiModel = _uiModel

    val siteUrl: String = SiteUtils.getHomeURLOrHostName(selectedSiteRepository.selectedSiteChange.value)

    private var isStarted: Boolean = false
    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        _uiModel.value = buildSiteDomainsList()
    }

    private fun buildSiteDomainsList(): List<DomainsListItem> {
        val freePlan = selectedSiteRepository.getSelectedSite()?.let { SiteUtils.onFreePlan(it) }
        val customDomain = selectedSiteRepository.getSelectedSite()?.let { SiteUtils.hasCustomDomain(it) }
        return when {
            customDomain == true -> manageDomainsItems()
            freePlan == true -> getDomainItems()
            else -> claimDomainItems()
        }
    }

    private fun getDomainItems(): List<DomainsListItem> {
        val listItems = mutableListOf<DomainsListItem>()

        listItems += PrimaryDomain(UiStringText(siteUrl), ListItemInteraction.create(this::onMoreMenuClick))

        listItems +=
            PurchaseDomain(
                    R.drawable.media_image_placeholder,
                    UiStringRes(string.domains_free_plan_get_your_domain_title),
                    UiStringText(htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                            string.domains_free_plan_get_your_domain_caption, siteUrl)),
                    ListItemInteraction.create(this::onGetDomainClick)
            )

        return listItems
    }

    private fun claimDomainItems(): List<DomainsListItem> {
        val listItems = mutableListOf<DomainsListItem>()

        listItems += PrimaryDomain(UiStringText(siteUrl), ListItemInteraction.create(this::onMoreMenuClick))

        listItems +=
            PurchaseDomain(
                    R.drawable.media_image_placeholder,
                    UiStringRes(string.domains_paid_plan_claim_your_domain_title),
                    UiStringRes(string.domains_paid_plan_claim_your_domain_caption),
                    ListItemInteraction.create(this::onClaimDomainClick)
            )

        return listItems
    }

    // if site has a registered domain then show Site Domains, Add Domain and Manage Domains
    private fun manageDomainsItems(): List<DomainsListItem> {
        val listItems = mutableListOf<DomainsListItem>()

        listItems += PrimaryDomain(UiStringText(siteUrl), ListItemInteraction.create(this::onMoreMenuClick))

        listItems += SiteDomainsHeader(UiStringRes(string.domains_site_domains))

        // TODO: Loop through and add all domains, replace hard coded date.  Not sure where to find this info yet!
        listItems += SiteDomains(
                UiStringText(siteUrl),
                UiStringResWithParams(string.domains_site_domain_expires, listOf(UiStringText("03/09/2024"))))

        listItems += AddDomain(ListItemInteraction.create(this::onAddDomainClick))

        listItems += ManageDomains(ListItemInteraction.create(this::onManageDomainClick))

        // if site has redirected domain then show this blurb
        listItems += DomainBlurb(
                UiStringText(htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                        string.domains_redirected_domains_blurb, siteUrl)))

        return listItems
    }

    private fun onGetDomainClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        _onNavigation.value = Event(OpenDomainRegistration(selectedSite))
    }

    private fun onClaimDomainClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        _onNavigation.value = Event(OpenDomainRegistration(selectedSite))
    }

    private fun onAddDomainClick() {
        onClaimDomainClick()
    }

    private fun onManageDomainClick() {
        // TODO: next PR
    }

    private fun onMoreMenuClick() {
        // TODO: next PR
    }
}
