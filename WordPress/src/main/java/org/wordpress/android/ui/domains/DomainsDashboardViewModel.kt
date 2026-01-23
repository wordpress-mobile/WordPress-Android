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
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.network.rest.wpcom.site.StatusType
import org.wordpress.android.fluxc.store.SiteStore
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
import org.wordpress.android.util.isDomainCreditAvailable
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class DomainsDashboardViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val fetchPlansUseCase: FetchPlansUseCase,
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

    override fun onCleared() {
        fetchPlansUseCase.clear()
        super.onCleared()
    }

    private fun refresh(site: SiteModel) = launch {
        _showProgressSpinner.postValue(true)

        val deferredPlansResult = async { fetchPlansUseCase.execute(site) }
        val deferredDomainsResult = async { siteStore.fetchSiteDomains(site) }
        val deferredAllDomainsResult = async { fetchAllDomainsUseCase.execute() }

        val plansResult = deferredPlansResult.await()
        val domainsResult = deferredDomainsResult.await()
        val allDomainsResult = deferredAllDomainsResult.await()

        if (plansResult.isError) {
            AppLog.e(DOMAIN_REGISTRATION, "An error occurred while fetching plans: ${plansResult.error.message}")
        }

        if (domainsResult.isError) {
            AppLog.e(DOMAIN_REGISTRATION, "An error occurred while fetching domains: ${domainsResult.error.message}")
        }

        val allDomains = if (allDomainsResult is AllDomains.Success) allDomainsResult.domains else emptyList()

        buildDashboardItems(site, plansResult.plans.orEmpty(), domainsResult.domains.orEmpty(), allDomains)
    }

    private fun buildDashboardItems(
        site: SiteModel,
        plans: List<PlanModel>,
        domains: List<Domain>,
        allDomains: List<AllDomainsDomain>
    ) {
        val listItems = mutableListOf<DomainsDashboardItem>()

        listItems += SiteDomainsHeader(UiStringRes(R.string.domains_free_domain))

        val freeDomain = domains.firstOrNull { it.wpcomDomain }
        val freeDomainUrl = freeDomain?.domain ?: getCleanUrl(site.unmappedUrl)
        val freeDomainIsPrimary = freeDomain?.primaryDomain ?: false

        listItems += SiteDomains(
            UiStringText(freeDomainUrl),
            freeDomainIsPrimary,
            UiStringRes(R.string.active),
            getStatusColor(StatusType.SUCCESS),
            UiStringRes(R.string.domains_site_domain_never_expires)
        )

        val customDomains = domains.filter { !it.wpcomDomain }
        val hasCustomDomains = customDomains.isNotEmpty()
        val hasDomainCredit = isDomainCreditAvailable(plans)
        val hasPaidPlan = !SiteUtils.onFreePlan(site)

        if (hasCustomDomains) {
            listItems += buildCustomDomainItems(site, customDomains, allDomains)
        }

        listItems += buildCtaItems(hasCustomDomains, hasDomainCredit, hasPaidPlan)

        _showProgressSpinner.postValue(false)
        _uiModel.postValue(listItems)
    }

    private fun getStatusColor(statusType: StatusType?) = when (statusType) {
        StatusType.SUCCESS -> R.color.jetpack_green_50
        StatusType.NEUTRAL -> R.color.gray_50
        StatusType.WARNING -> R.color.orange_50
        else -> R.color.red_50
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
        customDomains: List<Domain>,
        allDomains: List<AllDomainsDomain>
    ): List<DomainsDashboardItem> {
        val listItems = mutableListOf<DomainsDashboardItem>()
        listItems += SiteDomainsHeader(
            UiStringResWithParams(
                R.string.domains_site_other_domains,
                UiStringText(site.name)
            )
        )
        listItems += customDomains.map {
            val allDomainsDomain = allDomains.find { allDomainsItem -> it.domain == allDomainsItem.domain }

            SiteDomains(
                UiStringText(it.domain.orEmpty()),
                it.primaryDomain,
                allDomainsDomain?.domainStatus?.status?.let { status ->
                    UiStringText(status)
                } ?: UiStringRes(R.string.error),
                getStatusColor(allDomainsDomain?.domainStatus?.statusType),
                if (!it.hasRegistration) {
                    null
                } else if (it.expirySoon) {
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
                allDomainsDomain?.let { ListItemInteraction.create(allDomainsDomain, this::onDomainClick) }
            )
        }
        return listItems
    }

    private fun getCleanUrl(url: String?) = StringUtils.removeTrailingSlash(UrlUtils.removeScheme(url))

    private fun onDomainClick(allDomainsDomain: AllDomainsDomain) {
        _onNavigation.value = Event(
            OpenDomainManagement(
                allDomainsDomain.domain ?: return,
                allDomainsDomain.getDomainDetailsUrl() ?: return
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
