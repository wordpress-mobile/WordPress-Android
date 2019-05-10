package org.wordpress.android.viewmodel.main

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchedPlansPayload
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.store.SiteStore.PlansError
import org.wordpress.android.ui.quickstart.QuickStartTaskState
import org.wordpress.android.viewmodel.quickstart.QuickStartViewModel
import javax.inject.Inject

@RunWith(MockitoJUnitRunner::class)
class MySiteViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: SiteStore
    private lateinit var viewModel: MySiteViewModel

    private val site = SiteModel()

    @Before
    fun setUp() {
        viewModel = MySiteViewModel(store, dispatcher)
    }

    @Test
    fun currentPlanIsSetWhenPlansAreLoaded() {
        val plans = listOf(
                PlanModel(1, "slug", "name", isCurrentPlan = false, hasDomainCredit =false),
                PlanModel(2, "slug", "name", isCurrentPlan = true, hasDomainCredit = true)
        )

        whenever(dispatcher.dispatch(any())).then {
            viewModel.onPlansFetched(OnPlansFetched(site, plans, null))
        }

        viewModel.currentPlan.observeForever { plan ->
            assertNotNull(plan)
            plan?.let {
                assert(it.isCurrentPlan)
            }
        }

        viewModel.loadPlans(site)
    }

    @Test
    fun currentPlanIsNullWhenNoCurrentPlans() {
        val plans = listOf(
                PlanModel(1, "slug", "name", isCurrentPlan = false, hasDomainCredit =false),
                PlanModel(2, "slug", "name", isCurrentPlan = false, hasDomainCredit = true)
        )

        whenever(dispatcher.dispatch(any())).then {
            viewModel.onPlansFetched(OnPlansFetched(site, plans, null))
        }

        viewModel.currentPlan.observeForever { plan ->
            assertNull(plan)
        }

        viewModel.loadPlans(site)
    }

    @Test
    fun currentPlanIsNullWhenNoPlans() {
        whenever(dispatcher.dispatch(any())).then {
            viewModel.onPlansFetched(OnPlansFetched(site, null, null))
        }

        viewModel.currentPlan.observeForever { plan ->
            assertNull(plan)
        }

        viewModel.loadPlans(site)
    }

    @Test
    fun currentPlanIsNullWhenError() {
        whenever(dispatcher.dispatch(any())).then {
            viewModel.onPlansFetched(OnPlansFetched(null, null, PlansError("Unknown Blog", "Unknown Blog")))
        }

        viewModel.currentPlan.observeForever { plan ->
            assertNull(plan)
        }

        viewModel.loadPlans(site)
    }
}
