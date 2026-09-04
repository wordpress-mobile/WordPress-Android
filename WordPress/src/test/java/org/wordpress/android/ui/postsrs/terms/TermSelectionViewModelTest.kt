package org.wordpress.android.ui.postsrs.terms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.TaxonomyType
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams

@ExperimentalCoroutinesApi
class TermSelectionViewModelTest : BaseUnitTest(
    StandardTestDispatcher()
) {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var restClient: PostRsRestClient
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private val site = SiteModel()
    private var activeViewModel: TermSelectionViewModel? = null

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(site)
        whenever(networkUtilsWrapper.isNetworkAvailable())
            .thenReturn(true)
        whenever(resourceProvider.getString(any()))
            .thenReturn("error")
    }

    @After
    fun tearDown() {
        activeViewModel?.viewModelScope?.cancel()
        activeViewModel = null
    }

    @Test
    fun `loads every page before showing terms`() = test {
        val secondPageParams = TermListParams(perPage = 100u)
        val firstTerm = term(id = 1L, name = "First")
        val secondTerm = term(id = 2L, name = "Second")
        whenever(
            restClient.fetchTermsPage(
                site,
                TermEndpointType.Categories,
                nextPageParams = null,
            )
        ).thenReturn(
            PostRsRestClient.TermsPageResult(
                listOf(firstTerm),
                secondPageParams,
            )
        )
        whenever(
            restClient.fetchTermsPage(
                site,
                TermEndpointType.Categories,
                nextPageParams = secondPageParams,
            )
        ).thenReturn(
            PostRsRestClient.TermsPageResult(
                listOf(secondTerm),
                null,
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.terms.map { it.id })
            .containsExactly(1L, 2L)
    }

    @Test
    fun `keeps fetched pages when a later page fails`() = test {
        val secondPageParams = TermListParams(perPage = 100u)
        val firstTerm = term(id = 1L, name = "First")
        whenever(
            restClient.fetchTermsPage(
                site,
                TermEndpointType.Categories,
                nextPageParams = null,
            )
        ).thenReturn(
            PostRsRestClient.TermsPageResult(
                listOf(firstTerm),
                secondPageParams,
            )
        )
        whenever(
            restClient.fetchTermsPage(
                site,
                TermEndpointType.Categories,
                nextPageParams = secondPageParams,
            )
        ).thenAnswer {
            throw PostRsRestClient.TermsFetchException("failure")
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isNull()
        assertThat(viewModel.uiState.value.terms.map { it.id })
            .containsExactly(1L)
        assertThat(viewModel.events.first())
            .isEqualTo(TermSelectionEvent.ShowSnackbar("error"))
    }

    @Test
    fun `shows an error when the first page fails`() = test {
        whenever(
            restClient.fetchTermsPage(
                site,
                TermEndpointType.Categories,
                nextPageParams = null,
            )
        ).thenAnswer {
            throw PostRsRestClient.TermsFetchException("failure")
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.error).isEqualTo("error")
        assertThat(viewModel.uiState.value.terms).isEmpty()
    }

    @Test
    fun `retry cancels an in-flight load`() = test {
        val staleTerm = term(id = 1L, name = "Stale")
        val freshTerm = term(id = 2L, name = "Fresh")
        var requestCount = 0
        whenever(
            restClient.fetchTermsPage(
                site,
                TermEndpointType.Categories,
                nextPageParams = null,
            )
        ).doSuspendableAnswer {
            requestCount++
            if (requestCount == 1) {
                delay(60_000)
                PostRsRestClient.TermsPageResult(
                    listOf(staleTerm),
                    null,
                )
            } else {
                PostRsRestClient.TermsPageResult(
                    listOf(freshTerm),
                    null,
                )
            }
        }

        val viewModel = createViewModel()
        runCurrent()

        viewModel.retry()
        advanceUntilIdle()

        assertThat(requestCount).isEqualTo(2)
        assertThat(viewModel.uiState.value.terms.map { it.id })
            .containsExactly(2L)
    }

    private fun createViewModel(): TermSelectionViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                TermSelectionViewModel.EXTRA_IS_CATEGORIES to true,
                TermSelectionViewModel.EXTRA_SELECTED_IDS to longArrayOf(),
            )
        )
        return TermSelectionViewModel(
            savedStateHandle = savedStateHandle,
            selectedSiteRepository = selectedSiteRepository,
            restClient = restClient,
            resourceProvider = resourceProvider,
            networkUtilsWrapper = networkUtilsWrapper,
            ioDispatcher = testDispatcher(),
        ).also { activeViewModel = it }
    }

    private fun term(
        id: Long,
        name: String,
    ) = AnyTermWithViewContext(
        id = id,
        count = 0L,
        description = "",
        link = "",
        name = name,
        slug = name,
        taxonomy = TaxonomyType.Category,
        parent = 0L,
    )
}
