package org.wordpress.android.ui.main

import android.content.Context
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.WordPress
import org.wordpress.android.models.recommend.RecommendApiCallsProvider
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendAppName
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Failure
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Success
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendTemplateData
import org.wordpress.android.test
import org.wordpress.android.ui.main.MeViewModel.RecommendAppUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class MeViewModelTest : BaseUnitTest() {
    @Mock lateinit var wordPress: WordPress
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var recommendApiCallsProvider: RecommendApiCallsProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var context: Context

    private lateinit var viewModel: MeViewModel

    private val recommendUiState: MutableList<RecommendAppUiState> = mutableListOf()

    @Before
    fun setUp() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(contextProvider.getContext()).thenReturn(context)

        viewModel = MeViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                selectedSiteRepository,
                recommendApiCallsProvider,
                networkUtilsWrapper,
                contextProvider
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
        whenever(recommendApiCallsProvider.getRecommendTemplate(anyString())).thenReturn(DEFAULT_SUCCESS_API_RESPONSE)

        viewModel.onRecommendTheApp()

        verify(recommendApiCallsProvider, times(1)).getRecommendTemplate(anyString())

        assertThat(recommendUiState).isEqualTo(listOf(
                RecommendAppUiState(showLoading = true),
                RecommendAppUiState(
                    message = DEFAULT_SUCCESS_API_RESPONSE.templateData.message,
                    link = DEFAULT_SUCCESS_API_RESPONSE.templateData.link)
                )
        )
    }

    @Test
    fun `recommend triggers an error when no network`() {
        val noNetError = "No Network Error"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(context.getString(R.string.no_network_message)).thenReturn(noNetError)

        viewModel.onRecommendTheApp()

        assertThat(recommendUiState).isEqualTo(listOf(
                RecommendAppUiState(showLoading = true),
                RecommendAppUiState(noNetError)
        ))
    }

    @Test
    fun `recommend triggers an error on api call error`() = test {
        whenever(recommendApiCallsProvider.getRecommendTemplate(anyString())).thenReturn(DEFAULT_FAILURE_API_RESPONSE)

        viewModel.onRecommendTheApp()

        assertThat(recommendUiState).isEqualTo(listOf(
                RecommendAppUiState(showLoading = true),
                RecommendAppUiState(DEFAULT_FAILURE_API_RESPONSE.error)
        ))
    }

    @Test
    fun `recommend use already fetched template when available`() = test {
        whenever(recommendApiCallsProvider.getRecommendTemplate(anyString())).thenReturn(DEFAULT_SUCCESS_API_RESPONSE)

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

        verify(recommendApiCallsProvider, times(1)).getRecommendTemplate(anyString())
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
