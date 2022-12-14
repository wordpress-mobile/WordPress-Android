package org.wordpress.android.ui.main

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.WordPress
import org.wordpress.android.models.recommend.RecommendApiCallsProvider
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendAppName
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Failure
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Success
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendTemplateData
import org.wordpress.android.ui.main.MeViewModel.RecommendAppUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.analytics.AnalyticsUtils.RecommendAppSource
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class MeViewModelTest : BaseUnitTest() {
    @Mock lateinit var wordPress: WordPress
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var recommendApiCallsProvider: RecommendApiCallsProvider
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var viewModel: MeViewModel

    private val recommendUiState: MutableList<RecommendAppUiState> = mutableListOf()

    @Before
    fun setUp() {
        viewModel = MeViewModel(
                testDispatcher(),
                testDispatcher(),
                selectedSiteRepository,
                recommendApiCallsProvider,
                analyticsUtilsWrapper
        )

        setupObservers()
    }

    @Test
    fun `shows dialog and signs user out`() {
        val events = mutableListOf<Event<Boolean>>()
        viewModel.showDisconnectDialog.observeForever { events.add(it) }

        viewModel.signOutWordPress(wordPress)

        verify(wordPress).wordPressComSignOut()
        assertThat(events[0].getContentIfNotHandled()).isTrue()
        assertThat(events[1].getContentIfNotHandled()).isFalse()
    }

    @Test
    fun `opens disconnect dialog`() {
        val events = mutableListOf<Event<Boolean>>()
        viewModel.showDisconnectDialog.observeForever { events.add(it) }

        viewModel.openDisconnectDialog()

        assertThat(events[0].getContentIfNotHandled()).isTrue()
    }

    @Test
    fun `recommend template is recovered on first call`() = test {
        whenever(recommendApiCallsProvider.getRecommendTemplate(
                anyString(),
                eq(RecommendAppSource.ME)
        )).thenReturn(DEFAULT_SUCCESS_API_RESPONSE)

        viewModel.onRecommendTheApp()

        verify(recommendApiCallsProvider, times(1)).getRecommendTemplate(anyString(), eq(RecommendAppSource.ME))

        assertThat(recommendUiState).isEqualTo(listOf(
                RecommendAppUiState(showLoading = true),
                RecommendAppUiState(
                    message = DEFAULT_SUCCESS_API_RESPONSE.templateData.message,
                    link = DEFAULT_SUCCESS_API_RESPONSE.templateData.link)
                )
        )
    }

    @Test
    fun `recommend triggers an error when no network`() = test {
        val noNetError = "No Network Error"
        whenever(recommendApiCallsProvider.getRecommendTemplate(
                anyString(),
                eq(RecommendAppSource.ME)
        )).thenReturn(Failure(noNetError))

        viewModel.onRecommendTheApp()

        assertThat(recommendUiState).isEqualTo(listOf(
                RecommendAppUiState(showLoading = true),
                RecommendAppUiState(noNetError)
        ))
    }

    @Test
    fun `recommend triggers an error on api call error`() = test {
        whenever(recommendApiCallsProvider.getRecommendTemplate(
                anyString(),
                eq(RecommendAppSource.ME)
        )).thenReturn(DEFAULT_FAILURE_API_RESPONSE)

        viewModel.onRecommendTheApp()

        assertThat(recommendUiState).isEqualTo(listOf(
                RecommendAppUiState(showLoading = true),
                RecommendAppUiState(DEFAULT_FAILURE_API_RESPONSE.error)
        ))
    }

    @Test
    fun `recommend use already fetched template when available`() = test {
        whenever(recommendApiCallsProvider.getRecommendTemplate(
                anyString(),
                eq(RecommendAppSource.ME)
        )).thenReturn(DEFAULT_SUCCESS_API_RESPONSE)

        // first call successfully gets the template
        viewModel.onRecommendTheApp()
        // second call should use the already available template
        viewModel.onRecommendTheApp()

        assertThat(recommendUiState).isEqualTo(listOf(
                RecommendAppUiState(showLoading = true),
                RecommendAppUiState(
                        message = DEFAULT_SUCCESS_API_RESPONSE.templateData.message,
                        link = DEFAULT_SUCCESS_API_RESPONSE.templateData.link
                ),
                RecommendAppUiState(
                        message = DEFAULT_SUCCESS_API_RESPONSE.templateData.message,
                        link = DEFAULT_SUCCESS_API_RESPONSE.templateData.link
                )
        ))

        verify(recommendApiCallsProvider, times(1)).getRecommendTemplate(anyString(), eq(RecommendAppSource.ME))
    }

    @Test
    fun `recommend the app tracking is triggered when no error`() = test {
        whenever(recommendApiCallsProvider.getRecommendTemplate(
                anyString(),
                eq(RecommendAppSource.ME)
        )).thenReturn(DEFAULT_SUCCESS_API_RESPONSE)

        viewModel.onRecommendTheApp()

        verify(analyticsUtilsWrapper, times(1)).trackRecommendAppEngaged(eq(RecommendAppSource.ME))
    }

    @Test
    fun `recommend the app tracking is not triggered on error`() = test {
        whenever(recommendApiCallsProvider.getRecommendTemplate(
                anyString(),
                eq(RecommendAppSource.ME)
        )).thenReturn(DEFAULT_FAILURE_API_RESPONSE)

        viewModel.onRecommendTheApp()

        verify(analyticsUtilsWrapper, times(0)).trackRecommendAppEngaged(eq(RecommendAppSource.ME))
    }

    private fun setupObservers() {
        recommendUiState.clear()

        viewModel.recommendUiState.observeForever {
            it.applyIfNotHandled {
                recommendUiState.add(this)
            }
        }
    }

    companion object {
        private val DEFAULT_SUCCESS_API_RESPONSE = Success(
                templateData = RecommendTemplateData(
                        name = RecommendAppName.WordPress.appName,
                        message = "sharing message",
                        link = "https://sharinglink.org"
                )
        )
        private val DEFAULT_FAILURE_API_RESPONSE = Failure("API call Error")
    }
}
