package org.wordpress.android.ui.suggestion

import android.view.View
import androidx.lifecycle.LiveData
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.suggestion.FinishAttempt.NotExactlyOneAvailable
import org.wordpress.android.ui.suggestion.FinishAttempt.OnlyOneAvailable
import org.wordpress.android.ui.suggestion.SuggestionType.Users
import org.wordpress.android.ui.suggestion.SuggestionType.XPosts
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class SuggestionViewModelTest {
    @Mock lateinit var mockSuggestionSourceProvider: SuggestionSourceProvider
    @Mock lateinit var mockResourceProvider: ResourceProvider
    @Mock lateinit var mockNetworkUtils: NetworkUtilsWrapper
    @Mock lateinit var mockSite: SiteModel
    @Mock lateinit var mockLiveData: LiveData<List<Suggestion>>
    @Mock lateinit var mockSuggestionSource: SuggestionSource

    @InjectMocks lateinit var viewModel: SuggestionViewModel

    private val xpostSuggestionTypeString = "xpost_suggestion_type_string"
    private val userSuggestionTypeString = "user_suggestion_type_string"

    @Before
    fun setUp() {
        setSuggestionsSupported(true)

        whenever(mockResourceProvider.getString(R.string.suggestion_xpost)).thenReturn(xpostSuggestionTypeString)
        whenever(mockResourceProvider.getString(R.string.suggestion_user)).thenReturn(userSuggestionTypeString)

        whenever(mockSuggestionSource.suggestions).thenReturn(mockLiveData)
    }

    @Test
    fun `init when suggestions not supported`() {
        setSuggestionsSupported(false)
        val anySuggestionType = XPosts
        assertFalse(viewModel.init(anySuggestionType, mockSite))
    }

    @Test
    fun `init with xpost suggestions`() {
        assertTrue(initViewModel(XPosts))
        verifyViewModelSuggestionType(XPosts)
    }

    @Test
    fun `init with user suggestions`() {
        assertTrue(initViewModel(Users))
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
    fun `getEmptyViewState visibility gone`() {
        initViewModel()
        stubEmptyViewStateText()

        val nonEmptyList = listOf(mock<Suggestion>())
        val actual = viewModel.getEmptyViewState(nonEmptyList)
        assertEquals(View.GONE, actual.visibility)
    }

    @Test
    fun `getEmptyViewState visibility visible`() {
        initViewModel()
        whenever(mockLiveData.value).thenReturn(emptyList())
        stubEmptyViewStateText()

        val actual = viewModel.getEmptyViewState(emptyList())
        assertEquals(View.VISIBLE, actual.visibility)
    }

    private fun stubEmptyViewStateText() {
        whenever(mockNetworkUtils.isNetworkAvailable()).thenReturn(true)
        whenever(mockResourceProvider.getString(R.string.loading)).thenReturn("")
    }

    @Test
    fun `getEmptyViewState text no matching suggestions if suggestions available`() {
        initViewModel()
        val nonEmptyList = listOf(mock<Suggestion>())
        whenever(mockLiveData.value).thenReturn(nonEmptyList)
        val expectedText = "expected_text"
        whenever(mockResourceProvider.getString(R.string.suggestion_no_matching, viewModel.suggestionTypeString))
                .thenReturn(expectedText)

        val actual = viewModel.getEmptyViewState(emptyList())
        assertEquals(expectedText, actual.string)
    }

    @Test
    fun `getEmptyViewState text loading if no suggestions available but network available`() {
        initViewModel()
        whenever(mockLiveData.value).thenReturn(emptyList())
        whenever(mockNetworkUtils.isNetworkAvailable()).thenReturn(true)
        val expectedText = "expected_text"
        whenever(mockResourceProvider.getString(R.string.loading))
                .thenReturn(expectedText)

        val actual = viewModel.getEmptyViewState(emptyList())
        assertEquals(expectedText, actual.string)
    }

    @Test
    fun `getEmptyViewState text no internet if no suggestions available and network unavailable`() {
        initViewModel()
        whenever(mockLiveData.value).thenReturn(emptyList())
        whenever(mockNetworkUtils.isNetworkAvailable()).thenReturn(false)
        val expectedText = "expected_text"
        whenever(mockResourceProvider.getString(R.string.suggestion_no_connection))
                .thenReturn(expectedText)

        val actual = viewModel.getEmptyViewState(emptyList())
        assertEquals(expectedText, actual.string)
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
        assertEquals(expected, actual)
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
        assertEquals(expected, actual)
    }

    @Test
    fun `onAttemptToFinish exactly 1 displayed suggestion`() {
        initViewModel()

        val mockSuggestion = Suggestion("", "expected_value", "")
        val listWithExactlyOne = listOf<Suggestion>(mockSuggestion)
        val actual = viewModel.onAttemptToFinish(listWithExactlyOne, "")

        val expected = OnlyOneAvailable(mockSuggestion.value)
        assertEquals(expected, actual)
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
        assertEquals(expectedPrefix, actualPrefix)
    }

    private fun verifySuggestionTypeString(type: SuggestionType) {
        val expectedTypeString = when (type) {
            XPosts -> xpostSuggestionTypeString
            Users -> userSuggestionTypeString
        }
        val actualTypeString = viewModel.suggestionTypeString
        assertEquals(expectedTypeString, actualTypeString)
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
