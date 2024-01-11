package org.wordpress.android.ui.mysite.cards.domainregistration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

class DomainRegistrationViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appLogWrapper: AppLogWrapper,
    private val siteUtils: SiteUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private lateinit var scope: CoroutineScope

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.DomainRegistrationCard>()
    val uiModel: LiveData<MySiteCardAndItem.Card.DomainRegistrationCard> = _uiModel

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val continuations = mutableMapOf<Int, CancellableContinuation<OnPlansFetched>?>()

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun getData(
        siteLocalId: Int,
        selectedSite: SiteModel?
    ) {
        _isRefreshing.postValue(true)
        if (selectedSite == null || selectedSite.id != siteLocalId || !shouldFetchPlans(selectedSite)) {
            postState(false)
        } else {
            fetchPlansAndRefreshData(siteLocalId, selectedSite)
        }
    }

    private fun fetchPlansAndRefreshData(
        siteLocalId: Int,
        selectedSite: SiteModel
    ) {
        if (continuations[siteLocalId] == null) {
            scope.launch(bgDispatcher) { fetchPlans(siteLocalId, selectedSite) }
        } else {
            appLogWrapper.d(DOMAIN_REGISTRATION, "A request is already running for $siteLocalId")
        }
    }

    @Suppress("SwallowedException")
    private suspend fun fetchPlans(siteLocalId: Int, selectedSite: SiteModel) {
        try {
            val event = suspendCancellableCoroutine<OnPlansFetched> { cancellableContinuation ->
                continuations[siteLocalId] = cancellableContinuation
                dispatchFetchPlans(selectedSite)
            }
            when {
                event.isError -> {
                    val message = "An error occurred while fetching plans :${event.error.message}"
                    appLogWrapper.e(DOMAIN_REGISTRATION, message)
                    postState(false)
                }

                siteLocalId == event.site.id -> {
                    postState((isDomainCreditAvailable(event.plans)))
                }

                else -> {
                    postState(false)
                }
            }
        } catch (e: CancellationException) {
            postState(false)
        }
    }

    private fun postState(isDomainCreditAvailable: Boolean) {
        _isRefreshing.postValue(false)
        if (isDomainCreditAvailable)
            _uiModel.postValue(
                MySiteCardAndItem.Card.DomainRegistrationCard(
                    ListItemInteraction.create(this::domainRegistrationClick)
                )
            )
    }

    private fun domainRegistrationClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        _onNavigation.value = Event(SiteNavigationAction.OpenDomainRegistration(selectedSite))
    }

    private fun shouldFetchPlans(site: SiteModel) = !siteUtils.onFreePlan(site)

    private fun dispatchFetchPlans(site: SiteModel) = dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) {
        continuations[event.site.id]?.resume(event)
        continuations[event.site.id] = null
    }
}
