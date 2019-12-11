package org.wordpress.android.viewmodel.domains

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.domains.DomainProductDetails
import org.wordpress.android.ui.domains.DomainRegistrationCompletedEvent
import org.wordpress.android.ui.domains.DomainRegistrationMainViewModel
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class DomainRegistrationMainViewModelTest : BaseUnitTest() {
    @Mock lateinit var tracker: AnalyticsTrackerWrapper

    private lateinit var viewModel: DomainRegistrationMainViewModel

    private val testDomainProductDetails = DomainProductDetails(76, "testdomain.blog")
    private val domainRegisteredEvent = DomainRegistrationCompletedEvent("testdomain.blog", "email@wordpress.org")

    @Before
    fun setUp() {
        viewModel = DomainRegistrationMainViewModel(tracker)
        viewModel.start()
    }

    @Test
    fun `domain suggestions are visible at start`() {
        var isDomainSuggestionVisible = false
        viewModel.domainSuggestionsVisible.observeForever { isVisible ->
            isDomainSuggestionVisible = isVisible
        }

        assertThat(isDomainSuggestionVisible).isTrue()
    }

    @Test
    fun `selecting domain from suggestions hides them`() {
        viewModel.selectDomain(testDomainProductDetails)

        var isDomainSuggestionVisible = true
        viewModel.domainSuggestionsVisible.observeForever { isVisible ->
            isDomainSuggestionVisible = isVisible
        }

        assertThat(isDomainSuggestionVisible).isFalse()
    }

    @Test
    fun `domain selection is propagated to selectedDomain`() {
        viewModel.selectDomain(testDomainProductDetails)

        var selectedDomainProductDetails: DomainProductDetails? = null
        viewModel.selectedDomain.observeForever { domainProductDetails ->
            selectedDomainProductDetails = domainProductDetails
        }

        assertThat(selectedDomainProductDetails).isEqualTo(testDomainProductDetails)
    }

    @Test
    fun `finishing domain registration clears selectedDomain`() {
        viewModel.completeDomainRegistration(domainRegisteredEvent)

        var selectedDomainProductDetails: DomainProductDetails? = testDomainProductDetails
        viewModel.selectedDomain.observeForever { domainProductDetails ->
            selectedDomainProductDetails = domainProductDetails
        }

        assertThat(selectedDomainProductDetails).isNull()
    }

    @Test
    fun `finishing domain registration triggers domainRegistrationCompleted`() {
        viewModel.completeDomainRegistration(domainRegisteredEvent)

        var domainRegisteredEvent: DomainRegistrationCompletedEvent? = null
        viewModel.domainRegistrationCompleted.observeForever { event ->
            domainRegisteredEvent = event
        }

        assertThat(domainRegisteredEvent).isEqualTo(domainRegisteredEvent)
    }
}
