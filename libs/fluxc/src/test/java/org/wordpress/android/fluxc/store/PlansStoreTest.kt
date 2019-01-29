package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PlansAction
import org.wordpress.android.fluxc.generated.PlansActionBuilder
import org.wordpress.android.fluxc.model.plans.PlanModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PLAN_MODELS
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient
import org.wordpress.android.fluxc.persistence.PlansSqlUtils
import org.wordpress.android.fluxc.store.PlansStore.PlansErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.PlansStore.PlansFetchError
import org.wordpress.android.fluxc.store.PlansStore.PlansFetchedPayload
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class PlansStoreTest {
    @Mock private lateinit var plansRestClient: PlansRestClient
    @Mock private lateinit var plansSqlUtils: PlansSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    private lateinit var plansStore: PlansStore

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        plansStore = PlansStore(plansRestClient, plansSqlUtils, Unconfined, dispatcher)
    }

    @Test
    fun fetchPlans() = test {
        initRestClient(PLAN_MODELS)

        val action = PlansActionBuilder.generateNoPayloadAction(PlansAction.FETCH_PLANS)

        plansStore.onAction(action)

        verify(plansRestClient).fetchPlans()
        verify(plansSqlUtils).storePlans(PLAN_MODELS)

        val expectedEvent = PlansStore.OnPlansFetched(PLAN_MODELS)
        verify(dispatcher).emitChange(eq(expectedEvent))
    }

    @Test
    fun fetchCachedPlansAfterError() = test {
        initRestClient(PLAN_MODELS)
        val action = PlansActionBuilder.generateNoPayloadAction(PlansAction.FETCH_PLANS)
        plansStore.onAction(action)

        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR))
        error.message = "NETWORK_ERROR"
        // tell rest client to return error and no plans
        initRestClient(error = error)

        val expectedEventWithoutError = PlansStore.OnPlansFetched(PLAN_MODELS)
        verify(dispatcher, times(1)).emitChange(eq(expectedEventWithoutError))

        plansStore.onAction(action)

        verify(plansRestClient, times(2)).fetchPlans()
        verify(plansSqlUtils, times(1)).storePlans(PLAN_MODELS) // plans should not be stored on error

        val expectedEventWithError = PlansStore.OnPlansFetched(
                PLAN_MODELS,
                PlansFetchError(GENERIC_ERROR, "NETWORK_ERROR")
        )
        verify(dispatcher, times(1)).emitChange(eq(expectedEventWithError))
    }

    private suspend fun initRestClient(
        planModels: List<PlanModel>? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val payload = PlansFetchedPayload(planModels)

        if (error != null) {
            payload.error = error
        }

        whenever(plansRestClient.fetchPlans()).thenReturn(payload)
        whenever(plansSqlUtils.getPlans()).thenReturn(PLAN_MODELS)
    }
}
