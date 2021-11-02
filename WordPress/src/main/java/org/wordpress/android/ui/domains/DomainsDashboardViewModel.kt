package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_ADD_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_GET_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.DomainsDashboardItem.Action
import org.wordpress.android.ui.domains.DomainsDashboardItem.Action.CHANGE_SITE_ADDRESS
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.DomainBlurb
import org.wordpress.android.ui.domains.DomainsDashboardItem.FreeDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.ClaimDomain
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.GetDomain
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.OpenManageDomains
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@Suppress("TooManyFunctions")
class DomainsDashboardViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val htmlMessageUtils: HtmlMessageUtils,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
    lateinit var site: SiteModel
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
        dispatcher.register(this)
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_VIEWED, site)
        refresh()
        isStarted = true
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    private fun refresh() {
        checkDomainCredit()
        getSiteDomainsList()
    }

    private fun getSiteDomainsList() {
        // TODO: Probably needs a loading spinner here instead
        _uiModel.value = getFreeDomainItems(getHomeUrlOrHostName(site.unmappedUrl), false)

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
        // TODO: Refactor this
        if (shouldFetchPlans(site)) fetchPlans(site)
    }

    private fun shouldFetchPlans(site: SiteModel) = !SiteUtils.onFreePlan(site) && !SiteUtils.hasCustomDomain(site)

    private fun fetchPlans(site: SiteModel) = dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) {
        if (event.isError) {
            val message = "An error occurred while fetching plans : " + event.error.message
            AppLog.e(DOMAIN_REGISTRATION, message)
        } else if (site.id == event.site.id) {
            hasDomainCredit = isDomainCreditAvailable(event.plans)
        }
    }

    private fun buildDashboardItems(domains: List<Domain>?) {
        val listItems = mutableListOf<DomainsDashboardItem>()

        val freeDomain = domains?.firstOrNull { it.wpcomDomain || it.isWpcomStagingDomain }
        val customDomains = domains?.filter { !it.wpcomDomain && !it.isWpcomStagingDomain }

        freeDomain?.let {
            listItems += getFreeDomainItems(it.domain.toString(), it.primaryDomain)
        }

        customDomains?.let {
            listItems += getManageDomainsItems(freeDomain?.domain.toString(), customDomains)
        }

        listItems += if (hasDomainCredit) {
            getClaimDomainItems()
        } else {
            getPurchaseDomainItems(freeDomain?.domain.toString())
        }

        _uiModel.value = listItems
    }

    private fun getFreeDomainItems(siteUrl: String, isPrimary: Boolean) =
        listOf(FreeDomain(UiStringText(siteUrl), isPrimary, this::onChangeSiteClick))

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
    private fun getManageDomainsItems(siteUrl: String, domains: List<Domain>): List<DomainsDashboardItem> {
        val listItems = mutableListOf<DomainsDashboardItem>()

        if (domains.isNotEmpty()) listItems += SiteDomainsHeader(UiStringRes(string.domains_site_domains))

        domains.forEach {
            listItems += SiteDomains(
                    UiStringText(it.domain.toString()),
                    if (it.expirySoon) {
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                        string.domains_site_domain_expires_soon, it.expiry.toString()
                                )
                        )
                    } else {
                        UiStringResWithParams(
                                string.domains_site_domain_expires,
                                listOf(UiStringText(it.expiry.toString()))
                        )
                    },
                    it.primaryDomain
            )
        }

        if (domains.isNotEmpty()) {
            listItems += AddDomain(ListItemInteraction.create(this::onAddDomainClick))
            listItems += DomainBlurb(UiStringResWithParams(
                    string.domains_redirected_domains_blurb, listOf(UiStringText(siteUrl))))
        }

//        NOTE: Manage domains option is de-scoped for v1 release
//        listItems += ManageDomains(ListItemInteraction.create(this::onManageDomainClick))

        return listItems
    }

    private fun getHomeUrlOrHostName(unmappedUrl: String): String {
        var homeURL = UrlUtils.removeScheme(unmappedUrl)
        homeURL = StringUtils.removeTrailingSlash(homeURL)
        return homeURL
    }

    private fun onGetDomainClick() {
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_GET_DOMAIN_TAPPED, site)
        _onNavigation.value = Event(GetDomain(site))
    }

    private fun onClaimDomainClick() {
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)
        _onNavigation.value = Event(ClaimDomain(site))
    }

    private fun onAddDomainClick() {
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_ADD_DOMAIN_TAPPED, site)
        if (hasDomainCredit) onClaimDomainClick() else onGetDomainClick()
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

    fun onSuccessfulDomainRegistration() {
        refresh()
    }
}
