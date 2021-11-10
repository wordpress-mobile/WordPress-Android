package org.wordpress.android.ui.mysite.cards.domainregistration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

class DomainRegistrationSource
@Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appLogWrapper: AppLogWrapper,
    private val siteUtils: SiteUtilsWrapper
) : MySiteSource<DomainCreditAvailable> {
    private var continuation: CancellableContinuation<OnPlansFetched>? = null
    val refresh: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<DomainCreditAvailable> {
        val data = MediatorLiveData<DomainCreditAvailable>()
        data.refreshData(coroutineScope, siteLocalId, false)
        data.addSource(refresh) {
            if (refresh.value == true) {
                data.refreshData(coroutineScope, siteLocalId, true)
            }
        }
        return data
    }

    fun refresh() {
        refresh.postValue(true)
    }

    @Suppress("ReturnCount", "SwallowedException")
    private fun MediatorLiveData<DomainCreditAvailable>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        isRefresh: Boolean = false
    ) {
        continuation?.cancel()
        continuation = null
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite == null || selectedSite.id != siteLocalId) {
            postValues(false, isRefresh)
        } else {
            if (shouldFetchPlans(selectedSite)) {
                coroutineScope.launch(bgDispatcher) {
                    try {
                        val event = suspendCancellableCoroutine<OnPlansFetched> { cancellableContinuation ->
                            continuation = cancellableContinuation
                            fetchPlans(selectedSite)
                        }
                        continuation = null
                        if (event.isError) {
                            val message = "An error occurred while fetching plans : " + event.error.message
                            appLogWrapper.e(DOMAIN_REGISTRATION, message)
                            postValues(false, isRefresh)
                        } else if (siteLocalId == event.site.id) {
                            postValues(isDomainCreditAvailable(event.plans), isRefresh)
                        }
                    } catch (e: CancellationException) {
                        postValues(false, isRefresh)
                    }
                }
            } else {
                postValues(false, isRefresh)
            }
        }
    }

    private fun MediatorLiveData<DomainCreditAvailable>.postValues(
        isDomainCreditAvailable: Boolean,
        isRefresh: Boolean
    ) {
        if (isRefresh) refresh.postValue(false)
        this@postValues.postValue(DomainCreditAvailable(isDomainCreditAvailable))
    }

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    private fun shouldFetchPlans(site: SiteModel) = !siteUtils.onFreePlan(site)

    private fun fetchPlans(site: SiteModel) = dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) = continuation?.resume(event)
}
