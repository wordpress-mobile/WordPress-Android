package org.wordpress.android.ui.suggestion

import android.view.View
import androidx.lifecycle.LiveData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.suggestion.FinishAttempt.NotExactlyOneAvailable
import org.wordpress.android.ui.suggestion.FinishAttempt.OnlyOneAvailable
import org.wordpress.android.ui.suggestion.SuggestionType.Users
import org.wordpress.android.ui.suggestion.SuggestionType.XPosts
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class SuggestionViewModelTest {
    @Mock lateinit var mockSuggestionSourceProvider: SuggestionSourceProvider
    @Mock lateinit var mockResourceProvider: ResourceProvider
    @Mock lateinit var mockNetworkUtils: NetworkUtilsWrapper
    @Mock lateinit var mockAnalyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var mockSite: SiteModel
    @Mock lateinit var mockLiveData: LiveData<SuggestionResult>
    @Mock lateinit var mockSuggestionSource: SuggestionSource

    @InjectMocks lateinit var viewModel: SuggestionViewModel

    private val xpostSuggestionTypeString = "xpost_suggestion_type_string"
    private val userSuggestionTypeString = "user_suggestion_type_string"

    @Before
    fun setUp() {
        setSuggestionsSupported(true)

        whenever(mockResourceProvider.getString(R.string.suggestion_xpost)).thenReturn(xpostSuggestionTypeString)
        whenever(mockResourceProvider.getString(R.string.suggestion_user)).thenReturn(userSuggestionTypeString)

        whenever(mockSuggestionSource.suggestionData).thenReturn(mockLiveData)
    }

    @Test
    fun `init when suggestions not supported`() {
        setSuggestionsSupported(false)
        val anySuggestionType = XPosts
        assertThat(viewModel.init(anySuggestionType, mockSite)).isFalse
    }

    @Test
    fun `init with xpost suggestions`() {
        assertThat(initViewModel(XPosts)).isTrue
        verifyViewModelSuggestionType(XPosts)
    }

    @Test
    fun `init with user suggestions`() {
        assertThat(initViewModel(Users)).isTrue
        verifyViewModelSuggestionType(Users)
    }

    @Test
    fun `onConnectionChanged not connected`() {
        initViewModel()
        viewModel.onConnectionChanged(ConnectionChangeEvent(false))
        verify(mockSuggestionSource, never()).refreshSuggestions()
    }

    @Test
    fun `onConnectionChanged connected`() {
        initViewModel()
        viewModel.onConnectionChanged(ConnectionChangeEvent(true))
        verify(mockSuggestionSource).refreshSuggestions()
    }

    @Test
    fun `getEmptyViewState visibility gone if displaying any suggestions`() {
        initViewModel()
        stubEmptyViewStateText()

        val nonEmptyList = listOf(mock<Suggestion>())
        val actual = viewModel.getEmptyViewState(nonEmptyList)
        assertThat(View.GONE).isEqualTo(actual.visibility)
    }

    @Test
    fun `getEmptyViewState visibility visible if not displaying any suggestions`() {
        initViewModel()
        stubEmptyViewStateText()

        val actual = viewModel.getEmptyViewState(emptyList())
        assertThat(View.VISIBLE).isEqualTo(actual.visibility)
    }

    private fun stubEmptyViewStateText() {
        whenever(mockNetworkUtils.isNetworkAvailable()).thenReturn(true)
        whenever(mockResourceProvider.getString(anyInt(), anyString())).thenReturn("")
    }

    @Test
    fun `getEmptyViewState text no matching suggestions if suggestions available`() {
        initViewModel()
        val nonEmptyList = listOf(mock<Suggestion>())
        whenever(mockLiveData.value).thenReturn(SuggestionResult(nonEmptyList, false))
        val expectedText = "expected_text"
        whenever(mockResourceProvider.getString(R.string.suggestion_no_matching, viewModel.suggestionTypeString))
                .thenReturn(expectedText)

        val actual = viewModel.getEmptyViewState(emptyList())
        assertThat(expectedText).isEqualTo(actual.string)
    }

    @Test
    fun `getEmptyViewState problem text if has network, suggestions list empty, and fetch error`() {
        initViewModel()
        whenever(mockNetworkUtils.isNetworkAvailable()).thenReturn(true)
        whenever(mockLiveData.value).thenReturn(SuggestionResult(emptyList(), true))
        val expectedText = "expected_text"
        whenever(mockResourceProvider.getString(R.string.suggestion_problem)).thenReturn(expectedText)

        val actual = viewModel.getEmptyViewState(emptyList())
        assertThat(expectedText).isEqualTo(actual.string)
    }

    @Test
    fun `getEmptyViewState text no suggestions of type if has network, suggestions list empty, and NO fetch error`() {
        initViewModel()
        whenever(mockNetworkUtils.isNetworkAvailable()).thenReturn(true)
        whenever(mockLiveData.value).thenReturn(SuggestionResult(emptyList(), false))
        val expectedText = "expected_text"
        whenever(mockResourceProvider.getString(R.string.suggestion_none, viewModel.suggestionTypeString))
                .thenReturn(expectedText)

        val actual = viewModel.getEmptyViewState(emptyList())
        assertThat(expectedText).isEqualTo(actual.string)
    }

    @Test
    fun `getEmptyViewState text loading if has network and suggestions have never been received`() {
        initViewModel()
        whenever(mockNetworkUtils.isNetworkAvailable()).thenReturn(true)
        whenever(mockSuggestionSource.isFetchInProgress()).thenReturn(true)
        val expectedText = "expected_text"
        whenever(mockResourceProvider.getString(R.string.loading))
                .thenReturn(expectedText)

        val actual = viewModel.getEmptyViewState(emptyList())
        assertThat(expectedText).isEqualTo(actual.string)
    }

    @Test
    fun `getEmptyViewState text no internet if no suggestions available and network unavailable`() {
        initViewModel()
        whenever(mockLiveData.value).thenReturn(SuggestionResult(emptyList(), false))
        whenever(mockNetworkUtils.isNetworkAvailable()).thenReturn(false)
        val expectedText = "expected_text"
        whenever(mockResourceProvider.getString(R.string.suggestion_no_connection))
                .thenReturn(expectedText)

        val actual = viewModel.getEmptyViewState(emptyList())
        assertThat(expectedText).isEqualTo(actual.string)
    }

    @Test
    fun `onAttemptToFinish no displayed suggestions`() {
        initViewModel()
        val userInput = "user_input"
        val expectedMesage = "expected_message"
        whenever(
                mockResourceProvider.getString(
                        R.string.suggestion_invalid,
                        userInput, viewModel.suggestionTypeString
                )
        )
                .thenReturn(expectedMesage)

        val actual = viewModel.onAttemptToFinish(emptyList(), userInput)

        val expected = NotExactlyOneAvailable(expectedMesage)
        assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `onAttemptToFinish no filter text from user`() {
        initViewModel(XPosts)
        val emptyUserInput = "+"
        val expectedMesage = "expected_message"
        whenever(mockResourceProvider.getString(R.string.suggestion_selection_needed))
                .thenReturn(expectedMesage)

        val listWithMoreThanOne = listOf<Suggestion>(mock(), mock())
        val actual = viewModel.onAttemptToFinish(listWithMoreThanOne, emptyUserInput)

        val expected = NotExactlyOneAvailable(expectedMesage)
        assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `onAttemptToFinish multiple displayed suggestions`() {
        initViewModel()
        val userInput = "user_input"
        val expectedMesage = "expected_message"
        whenever(
                mockResourceProvider.getString(
                        R.string.suggestion_invalid,
                        userInput, viewModel.suggestionTypeString
                )
        )
                .thenReturn(expectedMesage)

        val listWithMoreThanOne = listOf<Suggestion>(mock(), mock())
        val actual = viewModel.onAttemptToFinish(listWithMoreThanOne, userInput)

        val expected = NotExactlyOneAvailable(expectedMesage)
        assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `onAttemptToFinish exactly 1 displayed suggestion`() {
        initViewModel()

        val mockSuggestion = Suggestion("", "expected_value", "")
        val listWithExactlyOne = listOf(mockSuggestion)
        val actual = viewModel.onAttemptToFinish(listWithExactlyOne, "")

        val expected = OnlyOneAvailable(mockSuggestion.value)
        assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `trackExit xpost suggestion true`() {
        initViewModel(XPosts)

        val withSuggestion = true
        viewModel.trackExit(withSuggestion)

        val props = mapOf(
                "did_select_suggestion" to withSuggestion,
                "suggestion_type" to "xpost"
        )
        verify(mockAnalyticsTracker).track(AnalyticsTracker.Stat.SUGGESTION_SESSION_FINISHED, props)
    }

    @Test
    fun `trackExit xpost suggestion false`() {
        initViewModel(XPosts)

        val withSuggestion = false
        viewModel.trackExit(withSuggestion)

        val props = mapOf(
                "did_select_suggestion" to withSuggestion,
                "suggestion_type" to "xpost"
        )
        verify(mockAnalyticsTracker).track(AnalyticsTracker.Stat.SUGGESTION_SESSION_FINISHED, props)
    }

    @Test
    fun `trackExit user suggestion true`() {
        initViewModel(Users)

        val withSuggestion = true
        viewModel.trackExit(withSuggestion)

        val props = mapOf(
                "did_select_suggestion" to withSuggestion,
                "suggestion_type" to "user"
        )
        verify(mockAnalyticsTracker).track(AnalyticsTracker.Stat.SUGGESTION_SESSION_FINISHED, props)
    }

    @Test
    fun `trackExit user suggestion false`() {
        initViewModel(Users)

        val withSuggestion = false
        viewModel.trackExit(withSuggestion)

        val props = mapOf(
                "did_select_suggestion" to withSuggestion,
                "suggestion_type" to "user"
        )
        verify(mockAnalyticsTracker).track(AnalyticsTracker.Stat.SUGGESTION_SESSION_FINISHED, props)
    }

    private fun initViewModel(type: SuggestionType = XPosts): Boolean {
        whenever(mockSuggestionSourceProvider.get(type, mockSite)).thenReturn(mockSuggestionSource)
        return viewModel.init(type, mockSite)
    }

    private fun verifyViewModelSuggestionType(type: SuggestionType) {
        verifySuggestionPrefix(type)
        verifySuggestionTypeString(type)
    }

    private fun verifySuggestionPrefix(type: SuggestionType) {
        val expectedPrefix = when (type) {
            XPosts -> '+'
            Users -> '@'
        }
        val actualPrefix = viewModel.suggestionPrefix
        assertThat(expectedPrefix).isEqualTo(actualPrefix)
    }

    private fun verifySuggestionTypeString(type: SuggestionType) {
        val expectedTypeString = when (type) {
            XPosts -> xpostSuggestionTypeString
            Users -> userSuggestionTypeString
        }
        val actualTypeString = viewModel.suggestionTypeString
        assertThat(expectedTypeString).isEqualTo(actualTypeString)
    }

    private fun setSuggestionsSupported(areSupported: Boolean) {
        val origin = if (areSupported) {
            SiteModel.ORIGIN_WPCOM_REST
        } else {
            SiteModel.ORIGIN_UNKNOWN
        }
        whenever(mockSite.origin).thenReturn(origin)
    }
}
