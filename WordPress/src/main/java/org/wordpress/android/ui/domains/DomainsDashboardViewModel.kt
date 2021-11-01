package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_ADD_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_GET_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.store.SiteStore
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
import org.wordpress.android.ui.domains.usecases.FetchPlansUseCase
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@Suppress("TooManyFunctions")
class DomainsDashboardViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val fetchPlansUseCase: FetchPlansUseCase,
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
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_VIEWED, site)
        refresh(site)
        isStarted = true
    }

    override fun onCleared() {
        fetchPlansUseCase.clear()
        super.onCleared()
    }

    private fun refresh(site: SiteModel) = launch {
        // TODO: Probably needs a loading spinner here

        val deferredPlansResult = async { fetchPlansUseCase.execute(site) }
        val deferredDomainsResult = async { siteStore.fetchSiteDomains(site) }

        val plansResult = deferredPlansResult.await()
        val domainsResult = deferredDomainsResult.await()

        if (plansResult.isError) {
            AppLog.e(DOMAIN_REGISTRATION, "An error occurred while fetching plans: ${plansResult.error.message}")
        }

        if (domainsResult.isError) {
            AppLog.e(DOMAIN_REGISTRATION, "An error occurred while fetching domains: ${domainsResult.error.message}")
        }

        buildDashboardItems(site, plansResult.plans.orEmpty(), domainsResult.domains.orEmpty())
    }

    private fun buildDashboardItems(site: SiteModel, plans: List<PlanModel>, domains: List<Domain>) {
        val listItems = mutableListOf<DomainsDashboardItem>()

        val freeDomain = domains.firstOrNull { it.wpcomDomain || it.isWpcomStagingDomain }
        val customDomains = domains.filter { !it.wpcomDomain && !it.isWpcomStagingDomain }

        freeDomain?.let {
            listItems += getFreeDomainItems(it.domain.toString(), it.primaryDomain)
        }

        customDomains.let {
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
            listOf(
                    PurchaseDomain(
                            R.drawable.media_image_placeholder,
                            UiStringRes(R.string.domains_free_plan_get_your_domain_title),
                            UiStringText(
                                    htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                            R.string.domains_free_plan_get_your_domain_caption,
                                            siteUrl
                                    )
                            ),
                            ListItemInteraction.create(this::onGetDomainClick)
                    )
            )

    private fun getClaimDomainItems() =
            listOf(
                    PurchaseDomain(
                            R.drawable.media_image_placeholder,
                            UiStringRes(R.string.domains_paid_plan_claim_your_domain_title),
                            UiStringRes(R.string.domains_paid_plan_claim_your_domain_caption),
                            ListItemInteraction.create(this::onClaimDomainClick)
                    )
            )

    // if site has a custom registered domain then show Site Domains, Add Domain and Manage Domains
    private fun getManageDomainsItems(siteUrl: String, domains: List<Domain>): List<DomainsDashboardItem> {
        val listItems = mutableListOf<DomainsDashboardItem>()

        if (domains.isNotEmpty()) listItems += SiteDomainsHeader(UiStringRes(R.string.domains_site_domains))

        domains.forEach {
            listItems += SiteDomains(
                    UiStringText(it.domain.toString()),
                    if (it.expirySoon) {
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                        R.string.domains_site_domain_expires_soon,
                                        it.expiry.toString()
                                )
                        )
                    } else {
                        UiStringResWithParams(
                                R.string.domains_site_domain_expires,
                                listOf(UiStringText(it.expiry.toString()))
                        )
                    },
                    it.primaryDomain
            )
        }

        if (domains.isNotEmpty()) {
            listItems += AddDomain(ListItemInteraction.create(hasDomainCredit, this::onAddDomainClick))
            listItems += DomainBlurb(
                    UiStringResWithParams(
                            R.string.domains_redirected_domains_blurb,
                            listOf(UiStringText(siteUrl))
                    )
            )
        }

//        NOTE: Manage domains option is de-scoped for v1 release
//        listItems += ManageDomains(ListItemInteraction.create(this::onManageDomainClick))

        return listItems
    }

    private fun getCleanUrl(url: String) = StringUtils.removeTrailingSlash(UrlUtils.removeScheme(url))

    private fun onGetDomainClick() {
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_GET_DOMAIN_TAPPED, site)
        _onNavigation.value = Event(GetDomain(site))
    }

    private fun onClaimDomainClick() {
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)
        _onNavigation.value = Event(ClaimDomain(site))
    }

    private fun onAddDomainClick(hasDomainCredit: Boolean) {
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_ADD_DOMAIN_TAPPED, site)
        if (hasDomainCredit) onClaimDomainClick() else onGetDomainClick()
    }

    private fun onManageDomainClick() {
        _onNavigation.postValue(Event(OpenManageDomains("${Constants.URL_MANAGE_DOMAINS}/${site.siteId}")))
    }

    //  NOTE: Change site option is de-scoped for v1 release
    private fun onChangeSiteClick(action: Action): Boolean {
        when (action) {
            CHANGE_SITE_ADDRESS -> {
                TODO("Not yet implemented")
            }
        }
        return true
    }

    fun onSuccessfulDomainRegistration() {
        refresh(site)
    }
}
