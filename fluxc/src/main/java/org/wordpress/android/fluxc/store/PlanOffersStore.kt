package org.wordpress.android.fluxc.store

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.PlanOfferAction
import org.wordpress.android.fluxc.action.PlanOfferAction.FETCH_PLAN_OFFERS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.plans.PlanOfferModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PlanOffersRestClient
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils
import org.wordpress.android.fluxc.store.PlanOffersStore.PlanOffersErrorType.GENERIC_ERROR
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class PlanOffersStore @Inject constructor(
    private val planOffersRestClient: PlanOffersRestClient,
    private val planOffersSqlUtils: PlanOffersSqlUtils,
    private val coroutineContext: CoroutineContext,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? PlanOfferAction ?: return
        when (actionType) {
            FETCH_PLAN_OFFERS -> {
                GlobalScope.launch(coroutineContext) { emitChange(fetchPlanOffers()) }
            }
        }
    }

    private suspend fun fetchPlanOffers(): OnPlanOffersFetched {
        val fetchedPlanOffersPayload = planOffersRestClient.fetchPlanOffers()

        return if (!fetchedPlanOffersPayload.isError) {
            planOffersSqlUtils.storePlanOffers(fetchedPlanOffersPayload.planOffers!!)
            val onPlanOffersFetched = OnPlanOffersFetched(fetchedPlanOffersPayload.planOffers)
            onPlanOffersFetched
        } else {
            val errorPayload = OnPlanOffersFetched(
                    getCachedPlanOffers(),
                    PlansFetchError(GENERIC_ERROR, fetchedPlanOffersPayload.error.message)
            )
            errorPayload
        }
    }

    fun getCachedPlanOffers(): List<PlanOfferModel> {
        return planOffersSqlUtils.getPlanOffers()
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, PlanOffersStore::class.java.simpleName + " onRegister")
    }

    class PlanOffersFetchedPayload(
        val planOffers: List<PlanOfferModel>? = null
    ) : Payload<BaseRequest.BaseNetworkError>()

    data class OnPlanOffersFetched(
        val planOffers: List<PlanOfferModel>? = null,
        val fetchError: PlansFetchError? = null
    ) : Store.OnChanged<PlansFetchError>() {
        init {
            // we allow setting error from constructor, so it will be a part of data class
            // and used when comparing this class, so we can test error/no error events
            this.error = fetchError
        }
    }

    data class PlansFetchError(
        val type: PlanOffersErrorType,
        val message: String = ""
    ) : OnChangedError

    enum class PlanOffersErrorType {
        GENERIC_ERROR
    }
}
