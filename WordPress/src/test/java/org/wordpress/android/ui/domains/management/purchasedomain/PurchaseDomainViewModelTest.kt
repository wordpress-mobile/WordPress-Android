package org.wordpress.android.ui.domains.management.purchasedomain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_EXISTING_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_SHOWN
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoBack
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToDomainPurchasing
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToExistingDomain
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class PurchaseDomainViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var viewModel: PurchaseDomainViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = PurchaseDomainViewModel(
            mainDispatcher = testDispatcher(),
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun `WHEN ViewModel initialized THEN track DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_SHOWN event`() {
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_SHOWN)
    }

    @Test
    fun `WHEN new domain selected THEN track DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED event`() {
        viewModel.onNewDomainSelected()
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED)
    }

    @Test
    fun `WHEN existing domain selected THEN track DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_EXISTING_DOMAIN_TAPPED event `() {
        viewModel.onExistingDomainSelected()
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_USE_DOMAIN_SCREEN_EXISTING_DOMAIN_TAPPED)
    }

    @Test
    fun `WHEN new domain selected THEN send GoToDomainPurchasing action event`() = testWithActionEvents { events ->
        viewModel.onNewDomainSelected()
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(GoToDomainPurchasing)
    }

    @Test
    fun `WHEN existing domain selected THEN send GoToExistingDomain action event`() = testWithActionEvents { events ->
        viewModel.onExistingDomainSelected()
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(GoToExistingDomain)
    }

    @Test
    fun `WHEN back button pressed THEN send GoBack action event`() = testWithActionEvents { events ->
        viewModel.onBackPressed()
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(GoBack)
    }

    private fun testWithActionEvents(block: suspend TestScope.(events: List<ActionEvent>) -> Unit) = test {
        val actionEvents = mutableListOf<ActionEvent>()
        val job = launch { viewModel.actionEvents.toList(actionEvents) }

        block(actionEvents)

        job.cancel()
    }
}
