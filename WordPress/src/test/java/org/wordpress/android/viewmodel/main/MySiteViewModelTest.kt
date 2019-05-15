package org.wordpress.android.viewmodel.main

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.store.SiteStore.PlansError

@RunWith(MockitoJUnitRunner::class)
class MySiteViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: SiteStore
    private lateinit var viewModel: MySiteViewModel

    @Mock private lateinit var siteObserver: Observer<SiteModel>

    private val site = SiteModel().apply { id = 1 }

    @Before
    fun setUp() {
        viewModel = MySiteViewModel(store, dispatcher)
    }

    @Test
    fun isDomainRegistrationVisibleIsTrueWhenCurrentPlanHasDomainCreditAvailable() {
        val plans = listOf(
                PlanModel(
                        1,
                        "slug",
                        "name",
                        isCurrentPlan = false,
                        hasDomainCredit = false
                ),
                PlanModel(
                        2,
                        "slug",
                        "name",
                        isCurrentPlan = true,
                        hasDomainCredit = true
                )
        )

        whenever(dispatcher.dispatch(any())).then {
            viewModel.onPlansFetched(OnPlansFetched(site, plans, null))
        }

        viewModel.isDomainRegistrationVisible.observeForever { isDomainRegistrationVisible ->
            assertNotNull(isDomainRegistrationVisible)
            isDomainRegistrationVisible?.let {
                assert(it)
            }
        }

        viewModel.loadPlans(site)
    }

    @Test
    fun isDomainRegistrationVisibleIsFalseWhenThereIsNoCurrentPlan() {
        val plans = listOf(
                PlanModel(
                        1,
                        "slug",
                        "name",
                        isCurrentPlan = false,
                        hasDomainCredit = false
                ),
                PlanModel(
                        2,
                        "slug",
                        "name",
                        isCurrentPlan = false,
                        hasDomainCredit = true
                )
        )

        whenever(dispatcher.dispatch(any())).then {
            viewModel.onPlansFetched(OnPlansFetched(site, plans, null))
        }

        viewModel.isDomainRegistrationVisible.observeForever { isDomainRegistrationVisible ->
            assertNotNull(isDomainRegistrationVisible)
            isDomainRegistrationVisible?.let { assertFalse(it) }
        }

        viewModel.loadPlans(site)
    }

    @Test
    fun isDomainRegistrationVisibleIsFalseWhenThereAreNoPlans() {
        whenever(dispatcher.dispatch(any())).then {
            viewModel.onPlansFetched(OnPlansFetched(site, null, null))
        }

        viewModel.isDomainRegistrationVisible.observeForever { isDomainRegistrationVisible ->
            assertNotNull(isDomainRegistrationVisible)
            isDomainRegistrationVisible?.let { assertFalse(it) }
        }

        viewModel.loadPlans(site)
    }

    @Test
    fun isDomainRegistrationVisibleIsFalseWhenThereIsAnErrorLoadingPlans() {
        whenever(dispatcher.dispatch(any())).then {
            viewModel.onPlansFetched(
                    OnPlansFetched(
                            null,
                            null,
                            PlansError("Unknown Blog", "Unknown Blog")
                    )
            )
        }

        viewModel.isDomainRegistrationVisible.observeForever { isDomainRegistrationVisible ->
            assertNotNull(isDomainRegistrationVisible)
            isDomainRegistrationVisible?.let { assertFalse(it) }
        }

        viewModel.loadPlans(site)
    }

    @Test
    fun setSiteWillFireObserversIfNewSiteIsDifferentThanOldSite() {
        viewModel.setSite(site)
        viewModel.site.observeForever(siteObserver)

        val site = SiteModel().apply { id = 2 }
        viewModel.setSite(site)

        verify(siteObserver, times(1)).onChanged(site)
        assertEquals(2, viewModel.site.value?.id)
    }

    @Test
    fun setSiteWillNotFireObserversIfNewSiteHasSameIdAsOldSite() {
        viewModel.setSite(site)
        viewModel.site.observeForever(siteObserver)

        val site = SiteModel().apply { id = 1 }
        viewModel.setSite(site)

        verify(siteObserver, never()).onChanged(site)
    }

    @Test
    fun loadPlansDoesNotDispatchEventIfSiteIsNull() {
        viewModel.loadPlans(null)

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun loadPlansDispatchesEventIfSiteIsNotNull() {
        viewModel.loadPlans(site)

        val argument = ArgumentCaptor.forClass(Action::class.java)

        verify(dispatcher, times(1)).dispatch(argument.capture())
        assertEquals(site.id, (argument.value.payload as SiteModel).id)
    }

    @Test
    fun clearPlansRemovesAllPlans() {
        val plans = listOf(
                PlanModel(
                        1,
                        "slug",
                        "name",
                        isCurrentPlan = false,
                        hasDomainCredit = false
                ),
                PlanModel(
                        2,
                        "slug",
                        "name",
                        isCurrentPlan = false,
                        hasDomainCredit = true
                )
        )

        viewModel.onPlansFetched(OnPlansFetched(site, plans, null))
        viewModel.clearPlans()

        assertNull(viewModel.plans.value)
    }
}
