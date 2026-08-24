package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_ADD_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_GET_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_GET_PLAN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchasePlan
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.ClaimDomain
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.GetDomain
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.GetPlan
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.OpenDomainManagement
import org.wordpress.android.ui.domains.management.getDomainDetailsUrl
import org.wordpress.android.ui.domains.usecases.AllDomains
import org.wordpress.android.ui.domains.usecases.FetchAllDomainsUseCase
import org.wordpress.android.ui.domains.usecases.FetchPlansUseCase
import org.wordpress.android.ui.domains.usecases.FetchSiteDomainsUseCase
import org.wordpress.android.ui.domains.usecases.SiteDomainsResult
import org.wordpress.android.ui.domains.usecases.hasDomainCredit
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import uniffi.wp_api.AllDomainItem
import uniffi.wp_api.DomainListItemStatusType
import uniffi.wp_api.SiteDomain
import javax.inject.Inject
import javax.inject.Named

class DomainsDashboardViewModel @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val fetchPlansUseCase: FetchPlansUseCase,
    private val fetchSiteDomainsUseCase: FetchSiteDomainsUseCase,
    private val fetchAllDomainsUseCase: FetchAllDomainsUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    lateinit var site: SiteModel
    private var isStarted: Boolean = false

    private val _showProgressSpinner = MutableLiveData<Boolean>()
    val progressBar: LiveData<Boolean> = _showProgressSpinner

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

    private fun refresh(site: SiteModel) = launch {
        _showProgressSpinner.postValue(true)

        val deferredPlansResult = async { fetchPlansUseCase.execute(site) }
        val deferredDomainsResult = async { fetchSiteDomainsUseCase.execute(site) }
        val deferredAllDomainsResult = async { fetchAllDomainsUseCase.execute() }

        val plansResult = deferredPlansResult.await()
        val domainsResult = deferredDomainsResult.await()
        val allDomainsResult = deferredAllDomainsResult.await()

        val domains = if (domainsResult is SiteDomainsResult.Success) domainsResult.domains else emptyList()
        val allDomains = if (allDomainsResult is AllDomains.Success) allDomainsResult.domains else emptyList()

        buildDashboardItems(site, plansResult.hasDomainCredit(), domains, allDomains)
    }

    private fun buildDashboardItems(
        site: SiteModel,
        hasDomainCredit: Boolean,
        domains: List<SiteDomain>,
        allDomains: List<AllDomainItem>
    ) {
        val listItems = mutableListOf<DomainsDashboardItem>()

        listItems += SiteDomainsHeader(UiStringRes(R.string.domains_free_domain))

        val freeDomain = domains.firstOrNull { it.wpcomDomain == true }
        val freeDomainUrl = freeDomain?.domain ?: getCleanUrl(site.unmappedUrl)
        val freeDomainIsPrimary = freeDomain?.primaryDomain ?: false

        listItems += SiteDomains(
            UiStringText(freeDomainUrl),
            freeDomainIsPrimary,
            UiStringRes(R.string.active),
            getStatusColor(DomainListItemStatusType.Success),
            UiStringRes(R.string.domains_site_domain_never_expires)
        )

        val customDomains = domains.filter { it.wpcomDomain != true }
        val hasCustomDomains = customDomains.isNotEmpty()
        val hasPaidPlan = !SiteUtils.onFreePlan(site)

        if (hasCustomDomains) {
            listItems += buildCustomDomainItems(site, customDomains, allDomains)
        }

        listItems += buildCtaItems(hasCustomDomains, hasDomainCredit, hasPaidPlan)

        _showProgressSpinner.postValue(false)
        _uiModel.postValue(listItems)
    }

    private fun getStatusColor(
        statusType: DomainListItemStatusType?
    ) = when (statusType) {
        is DomainListItemStatusType.Success,
        is DomainListItemStatusType.Premium -> R.color.jetpack_green_50
        is DomainListItemStatusType.Neutral -> R.color.gray_50
        is DomainListItemStatusType.Warning -> R.color.orange_50
        is DomainListItemStatusType.Alert,
        is DomainListItemStatusType.Error,
        is DomainListItemStatusType.Other,
        null -> R.color.red_50
    }

    private fun buildCtaItems(
        hasCustomDomains: Boolean,
        hasDomainCredit: Boolean,
        hasPaidPlan: Boolean
    ): List<DomainsDashboardItem> {
        val listItems = mutableListOf<DomainsDashboardItem>()
        if (hasDomainCredit) {
            listItems += PurchaseDomain(
                R.drawable.browser_address_bar,
                UiStringRes(R.string.domains_paid_plan_claim_your_domain_title),
                UiStringRes(R.string.domains_paid_plan_claim_your_domain_caption),
                ListItemInteraction.create(this::onClaimDomainClick)
            )
        } else if (hasCustomDomains) {
            listItems += AddDomain(ListItemInteraction.create(hasDomainCredit, this::onAddDomainClick))
        } else {
            listItems += if (hasPaidPlan) {
                PurchaseDomain(
                    R.drawable.browser_address_bar,
                    UiStringRes(R.string.domains_paid_plan_add_your_domain_title),
                    UiStringRes(R.string.domains_paid_plan_add_your_domain_caption),
                    ListItemInteraction.create(this::onGetDomainClick)
                )
            } else {
                PurchasePlan(
                    R.drawable.browser_address_bar,
                    UiStringRes(R.string.domains_free_plan_get_your_domain_title),
                    UiStringRes(R.string.domains_upgrade_to_plan_caption),
                    ListItemInteraction.create(this::onGetPlanClick),
                    ListItemInteraction.create(this::onGetDomainClick)
                )
            }
        }
        return listItems
    }

    private fun buildCustomDomainItems(
        site: SiteModel,
        customDomains: List<SiteDomain>,
        allDomains: List<AllDomainItem>
    ): List<DomainsDashboardItem> {
        val listItems = mutableListOf<DomainsDashboardItem>()
        listItems += SiteDomainsHeader(
            UiStringResWithParams(
                R.string.domains_site_other_domains,
                UiStringText(site.name)
            )
        )
        listItems += customDomains.map {
            val allDomainItem = allDomains.find { item ->
                it.domain == item.domain
            }

            SiteDomains(
                UiStringText(it.domain),
                it.primaryDomain == true,
                allDomainItem?.domainStatus?.label?.let { label ->
                    UiStringText(label)
                } ?: UiStringRes(R.string.error),
                getStatusColor(allDomainItem?.domainStatus?.statusType),
                if (it.hasRegistration != true) {
                    null
                } else if (it.expirySoon == true) {
                    UiStringText(
                        htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                            R.string.domains_site_domain_expires_soon,
                            it.expiry.orEmpty()
                        )
                    )
                } else {
                    UiStringResWithParams(
                        R.string.domains_site_domain_expires,
                        listOf(UiStringText(it.expiry.orEmpty()))
                    )
                },
                allDomainItem?.let {
                    ListItemInteraction.create(allDomainItem, this::onDomainClick)
                }
            )
        }
        return listItems
    }

    private fun getCleanUrl(url: String?) =
        StringUtils.removeTrailingSlash(UrlUtils.removeScheme(url))

    private fun onDomainClick(allDomainItem: AllDomainItem) {
        _onNavigation.value = Event(
            OpenDomainManagement(
                allDomainItem.domain,
                allDomainItem.getDomainDetailsUrl() ?: return
            )
        )
    }

    private fun onGetDomainClick() {
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_GET_DOMAIN_TAPPED, site)
        _onNavigation.value = Event(GetDomain(site))
    }

    private fun onGetPlanClick() {
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_GET_PLAN_TAPPED, site)
        _onNavigation.value = Event(GetPlan(site))
    }

    private fun onClaimDomainClick() {
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)
        _onNavigation.value = Event(ClaimDomain(site))
    }

    private fun onAddDomainClick(hasDomainCredit: Boolean) {
        analyticsTrackerWrapper.track(DOMAINS_DASHBOARD_ADD_DOMAIN_TAPPED, site)
        if (hasDomainCredit) onClaimDomainClick() else onGetDomainClick()
    }

    fun onSuccessfulDomainRegistration() {
        refresh(site)
    }
}
