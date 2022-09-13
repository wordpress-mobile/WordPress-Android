package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_ADD_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_GET_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_DASHBOARD_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
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
import org.wordpress.android.ui.domains.usecases.FetchPlansUseCase
import org.wordpress.android.ui.plans.isDomainCreditAvailable
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

        val freeDomain = domains.firstOrNull { it.wpcomDomain }
        val freeDomainUrl = freeDomain?.domain ?: getCleanUrl(site.unmappedUrl)
        val freeDomainIsPrimary = freeDomain?.primaryDomain ?: false

        listItems += FreeDomain(UiStringText(freeDomainUrl), freeDomainIsPrimary, this::onChangeSiteClick)

        val customDomains = domains.filter { !it.wpcomDomain }
        val hasCustomDomains = customDomains.isNotEmpty()
        val hasDomainCredit = isDomainCreditAvailable(plans)
        val hasPaidPlan = !SiteUtils.onFreePlan(site)

        listItems += buildCustomDomainItems(customDomains, hasCustomDomains)

        listItems += buildCtaItems(freeDomainUrl, hasCustomDomains, hasDomainCredit, hasPaidPlan)

//        NOTE: Manage domains option is de-scoped for v1 release
//        if (hasCustomDomains) {
//            listItems += ManageDomains(ListItemInteraction.create(this::onManageDomainClick))
//        }

        _showProgressSpinner.postValue(false)
        _uiModel.postValue(listItems)
    }

    private fun buildCtaItems(
        freeDomainUrl: String,
        hasCustomDomains: Boolean,
        hasDomainCredit: Boolean,
        hasPaidPlan: Boolean
    ): List<DomainsDashboardItem> {
        val listItems = mutableListOf<DomainsDashboardItem>()
        if (hasDomainCredit) {
            listItems += PurchaseDomain(
                    null,
                    UiStringRes(R.string.domains_paid_plan_claim_your_domain_title),
                    UiStringRes(R.string.domains_paid_plan_claim_your_domain_caption),
                    ListItemInteraction.create(this::onClaimDomainClick)
            )
        } else if (hasCustomDomains) {
            listItems += AddDomain(ListItemInteraction.create(hasDomainCredit, this::onAddDomainClick))
            if (!hasPaidPlan) {
                listItems += DomainBlurb(
                        UiStringResWithParams(
                                R.string.domains_redirected_domains_blurb,
                                listOf(UiStringText(freeDomainUrl))
                        )
                )
            }
        } else {
            listItems += if (hasPaidPlan) {
                PurchaseDomain(
                        R.drawable.img_illustration_domains_card_header,
                        UiStringRes(R.string.domains_paid_plan_add_your_domain_title),
                        UiStringRes(R.string.domains_paid_plan_add_your_domain_caption),
                        ListItemInteraction.create(this::onGetDomainClick)
                )
            } else {
                PurchaseDomain(
                        R.drawable.img_illustration_domains_card_header,
                        UiStringRes(R.string.domains_free_plan_get_your_domain_title),
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                        R.string.domains_free_plan_get_your_domain_caption,
                                        freeDomainUrl
                                )
                        ),
                        ListItemInteraction.create(this::onGetDomainClick)
                )
            }
        }
        return listItems
    }

    private fun buildCustomDomainItems(
        customDomains: List<Domain>,
        hasCustomDomains: Boolean
    ): List<DomainsDashboardItem> {
        val listItems = mutableListOf<DomainsDashboardItem>()
        if (hasCustomDomains) listItems += SiteDomainsHeader(UiStringRes(R.string.domains_site_domains))
        listItems += customDomains.map {
            SiteDomains(
                    UiStringText(it.domain.orEmpty()),
                    if (it.expirySoon) {
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
                    it.primaryDomain
            )
        }
        return listItems
    }

    private fun getCleanUrl(url: String?) = StringUtils.removeTrailingSlash(UrlUtils.removeScheme(url))

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
