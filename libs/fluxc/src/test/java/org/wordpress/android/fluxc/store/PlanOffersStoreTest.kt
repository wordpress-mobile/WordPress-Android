package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PlanOffersAction
import org.wordpress.android.fluxc.generated.PlanOffersActionBuilder
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PLAN_OFFER_MODELS
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PlanOffersRestClient
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils
import org.wordpress.android.fluxc.store.PlanOffersStore.PlanOffersErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.PlanOffersStore.PlanOffersFetchedPayload
import org.wordpress.android.fluxc.store.PlanOffersStore.PlansFetchError
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class PlanOffersStoreTest {
    @Mock private lateinit var planOffersRestClient: PlanOffersRestClient
    @Mock private lateinit var planOffersSqlUtils: PlanOffersSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    private lateinit var planOffersStore: PlanOffersStore

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        planOffersStore = PlanOffersStore(planOffersRestClient, planOffersSqlUtils, initCoroutineEngine(), dispatcher)
    }

    @Test
    fun fetchPlanOffers() = test {
        initRestClient(PLAN_OFFER_MODELS)

        val action = PlanOffersActionBuilder.generateNoPayloadAction(PlanOffersAction.FETCH_PLAN_OFFERS)

        planOffersStore.onAction(action)

        verify(planOffersRestClient).fetchPlanOffers()
        verify(planOffersSqlUtils).storePlanOffers(PLAN_OFFER_MODELS)

        val expectedEvent = PlanOffersStore.OnPlanOffersFetched(PLAN_OFFER_MODELS)
        verify(dispatcher).emitChange(eq(expectedEvent))
    }

    @Test
    fun fetchCachedPlanOffersAfterError() = test {
        initRestClient(PLAN_OFFER_MODELS)

        val action = PlanOffersActionBuilder.generateNoPayloadAction(PlanOffersAction.FETCH_PLAN_OFFERS)

        planOffersStore.onAction(action)

        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR))
        error.message = "NETWORK_ERROR"

        // tell REST client to return error and no plan offers
        initRestClient(error = error)

        val expectedEventWithoutError = PlanOffersStore.OnPlanOffersFetched(PLAN_OFFER_MODELS)
        verify(dispatcher, times(1)).emitChange(eq(expectedEventWithoutError)) // sanity check

        planOffersStore.onAction(action)

        verify(planOffersRestClient, times(2)).fetchPlanOffers()
        // plan offers should not be stored on error
        verify(planOffersSqlUtils, times(1)).storePlanOffers(PLAN_OFFER_MODELS)

        val expectedEventWithError = PlanOffersStore.OnPlanOffersFetched(
                PLAN_OFFER_MODELS,
                PlansFetchError(GENERIC_ERROR, "NETWORK_ERROR")
        )
        verify(dispatcher, times(1)).emitChange(eq(expectedEventWithError))
    }

    private suspend fun initRestClient(
        planOffers: List<PlanOffersModel>? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val payload = PlanOffersFetchedPayload(planOffers)

        if (error != null) {
            payload.error = error
        }

        whenever(planOffersRestClient.fetchPlanOffers()).thenReturn(payload)
        whenever(planOffersSqlUtils.getPlanOffers()).thenReturn(PLAN_OFFER_MODELS)
    }
}
