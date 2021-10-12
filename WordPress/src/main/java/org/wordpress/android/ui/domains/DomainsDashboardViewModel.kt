package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.DomainsDashboardItem.Action
import org.wordpress.android.ui.domains.DomainsDashboardItem.Action.CHANGE_SITE_ADDRESS
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PrimaryDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.ClaimDomain
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.GetDomain
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.OpenManageDomains
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationHandler
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@Suppress("TooManyFunctions")
class DomainsDashboardViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val domainRegistrationHandler: DomainRegistrationHandler,
    private val htmlMessageUtils: HtmlMessageUtils,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
    lateinit var site: SiteModel
    lateinit var siteUrl: String
    private var hasDomainCredit: Boolean = false
    private var isStarted: Boolean = false

    private val _onNavigation = MutableLiveData<Event<DomainsDashboardNavigationAction>>()
    val onNavigation: LiveData<Event<DomainsDashboardNavigationAction>> = _onNavigation

    private val _uiModel = MutableLiveData<List<DomainsDashboardItem>>()
    val uiModel: LiveData<List<DomainsDashboardItem>> = _uiModel

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }
        this.site = site
        this.siteUrl = SiteUtils.getHomeURLOrHostName(site)
        _uiModel.value = getPrimaryDomainItems(siteUrl)
        checkDomainCredit()
        getSiteDomainsList()
        isStarted = true
    }

    override fun onCleared() {
        domainRegistrationHandler.clear()
        super.onCleared()
    }

    private fun getSiteDomainsList() {
        launch {
            val result = siteStore.fetchSiteDomains(site)
            when {
                result.isError -> {
                    AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching site domains")
                }
                else -> {
                    buildDashboardItems(result.domains)
                }
            }
        }
    }

    private fun checkDomainCredit() {
        val domainCreditAvailable =
                domainRegistrationHandler.buildSource(viewModelScope, site.id).distinctUntilChanged()
        hasDomainCredit = domainCreditAvailable.value?.isDomainCreditAvailable == true
    }

    private fun buildDashboardItems(domains: List<Domain>?) {
        val listItems = mutableListOf<DomainsDashboardItem>()

        listItems += getPrimaryDomainItems(siteUrl)

        val customDomains = domains?.filter { !it.wpcomDomain && !it.isWpcomStagingDomain }

        val hasCustomDomain = customDomains?.isNotEmpty() == true

//        AppLog.e(T.DOMAIN_REGISTRATION, domains?.toString())
//        AppLog.e(T.DOMAIN_REGISTRATION, customDomains?.toString())
//        AppLog.e(T.DOMAIN_REGISTRATION, hasCustomDomain.toString())
//        AppLog.e(T.DOMAIN_REGISTRATION, hasDomainCredit.toString())

        listItems += when {
            hasCustomDomain -> getManageDomainsItems(siteUrl, customDomains)
            hasDomainCredit -> getClaimDomainItems()
            else -> getPurchaseDomainItems(siteUrl)
        }

        _uiModel.value = listItems
    }

    private fun getPrimaryDomainItems(siteUrl: String) =
        listOf(PrimaryDomain(UiStringText(siteUrl), this::onChangeSiteClick))

    // for v1 release image/anim is de-scoped, set the image visibility to gone in layout for now.
    private fun getPurchaseDomainItems(siteUrl: String) =
            listOf(PurchaseDomain(
                    R.drawable.media_image_placeholder,
                    UiStringRes(string.domains_free_plan_get_your_domain_title),
                    UiStringText(htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                            string.domains_free_plan_get_your_domain_caption, siteUrl)),
                    ListItemInteraction.create(this::onGetDomainClick)
            ))


    private fun getClaimDomainItems() =
            listOf(PurchaseDomain(
                    R.drawable.media_image_placeholder,
                    UiStringRes(string.domains_paid_plan_claim_your_domain_title),
                    UiStringRes(string.domains_paid_plan_claim_your_domain_caption),
                    ListItemInteraction.create(this::onClaimDomainClick)
            ))

    // if site has a custom registered domain then show Site Domains, Add Domain and Manage Domains
    private fun getManageDomainsItems(siteUrl: String, domains: List<Domain>?): List<DomainsDashboardItem> {
        val listItems = mutableListOf<DomainsDashboardItem>()

        listItems += SiteDomainsHeader(UiStringRes(string.domains_site_domains))
//        listItems += DomainBlurb(UiStringText(htmlMessageUtils.getHtmlMessageFromStringFormatResId(
//                        string.domains_redirected_domains_blurb, siteUrl)))

       domains?.forEach {
            listItems += SiteDomains(
                    UiStringText(it.domain.toString()),
                    if (it.expirySoon) {
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                        string.domains_site_domain_expires_soon, it.expiry.toString()))
                    } else {
                        UiStringResWithParams(
                                string.domains_site_domain_expires, listOf(UiStringText(it.expiry.toString())))
                    }
            )
        }

        listItems += AddDomain(ListItemInteraction.create(this::onAddDomainClick))


//        NOTE: Manage domains option is de-scoped for v1 release
//        listItems += ManageDomains(ListItemInteraction.create(this::onManageDomainClick))

        return listItems
    }

    private fun onGetDomainClick() {
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)
        _onNavigation.value = Event(GetDomain(site))
    }

    private fun onClaimDomainClick() {
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)
        _onNavigation.value = Event(ClaimDomain(site))
    }

    private fun onAddDomainClick() {
        onClaimDomainClick()
    }

    private fun onManageDomainClick() {
        _onNavigation.postValue(Event(OpenManageDomains("${Constants.URL_MANAGE_DOMAINS}/${site.siteId}")))
    }

//  NOTE: Change site option is de-scoped for v1 release
    private fun onChangeSiteClick(action: Action): Boolean {
        when (action) {
            CHANGE_SITE_ADDRESS -> {} // TODO: next PR
        }
        return true
    }
}
