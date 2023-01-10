package org.wordpress.android.ui.suggestion

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.XPostSiteModel
import org.wordpress.android.fluxc.store.XPostsResult
import org.wordpress.android.fluxc.store.XPostsStore
import org.wordpress.android.util.NoDelayCoroutineDispatcher

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class XPostsSuggestionSourceTest : BaseUnitTest() {
    @Mock
    lateinit var mockXPostsStore: XPostsStore

    @Mock
    lateinit var mockSite: SiteModel
    private lateinit var xPostsSource: XPostsSuggestionSource

    @Before
    fun setUp() {
        xPostsSource = XPostsSuggestionSource(mockXPostsStore, mockSite, NoDelayCoroutineDispatcher())
    }

    @Test
    fun `initialization sends non-empty db result and refreshes suggestions`() = test {
        val expectedDbXpostSite = XPostSiteModel(
            1,
            2,
            "db_xpost_site_title",
            "db_site_url",
            "db_xpost_subdomain",
            "db_xpost_blavatar"
        )
        val expectedDbResult = XPostsResult.dbResult(listOf(expectedDbXpostSite))
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(expectedDbResult)

        xPostsSource = spy(xPostsSource)
        doNothing().whenever(xPostsSource).refreshSuggestions()

        val actualResults = mutableListOf<SuggestionResult>()
        xPostsSource.suggestionData.observeForever { actualResults.add(it) }

        xPostsSource.initialize()

        val expectedResults = listOf(SuggestionResult(suggestionsFromResult(expectedDbResult), false))
        assertEquals(expectedResults, actualResults)
        verify(xPostsSource).refreshSuggestions()
    }

    @Test
    fun `initialization does not send empty db result and refreshes suggestions`() = test {
        val expectedDbResult = XPostsResult.dbResult(emptyList())
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(expectedDbResult)

        xPostsSource = spy(xPostsSource)
        doNothing().whenever(xPostsSource).refreshSuggestions()

        val actualResults = mutableListOf<SuggestionResult>()
        xPostsSource.suggestionData.observeForever { actualResults.add(it) }
        xPostsSource.initialize()

        assertEquals(emptyList<SuggestionResult>(), actualResults)
        verify(xPostsSource).refreshSuggestions()
    }

    @Test
    fun `refreshSuggestions sends api result`() = test {
        val expectedApiXpostSite = XPostSiteModel(
            11,
            12,
            "api_xpost_site_title",
            "api_site_url",
            "api_xpost_subdomain",
            "api_xpost_blavatar"
        )
        val expectedApiResult = XPostsResult.apiResult(listOf(expectedApiXpostSite))
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(expectedApiResult)

        val actualResults = mutableListOf<SuggestionResult>()
        xPostsSource.suggestionData.observeForever { actualResults.add(it) }
        xPostsSource.refreshSuggestions()

        val expectedApiSuggestionResult = SuggestionResult(suggestionsFromResult(expectedApiResult), false)
        val expected = listOf(expectedApiSuggestionResult)
        assertEquals(expected, actualResults)
    }

    @Test
    fun `refreshSuggestions does nothing if refresh already in progress`() = test {
        val actualResults = mutableListOf<SuggestionResult>()
        xPostsSource.suggestionData.observeForever { actualResults.add(it) }

        xPostsSource.fetchJob = mock<Job>().apply {
            whenever(this.isActive).thenReturn(true)
        }
        xPostsSource.refreshSuggestions()

        assertEquals(emptyList<SuggestionResult>(), actualResults)
    }

    @Test
    fun `sends previous result with error if api result is unknown`() = test {
        val actualResults = mutableListOf<SuggestionResult>()
        xPostsSource.suggestionData.observeForever { actualResults.add(it) }

        val earlierSuggestion = Suggestion("avatar_1", "value_1", "displayValue_1")
        val earlierSuggestionResult = SuggestionResult(listOf(earlierSuggestion), false)
        (xPostsSource.suggestionData as MutableLiveData<SuggestionResult>).setValue(earlierSuggestionResult)
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(XPostsResult.Unknown)

        xPostsSource.refreshSuggestions()

        assertEquals(2, actualResults.size)
        assertEquals(earlierSuggestionResult, actualResults[0])
        assertEquals(earlierSuggestionResult.copy(hadFetchError = true), actualResults[1])
    }

    @Test
    fun `isFetchInProgress true if job active`() {
        xPostsSource.fetchJob = mock<Job>().apply {
            whenever(this.isActive).thenReturn(true)
        }
        assertTrue(xPostsSource.isFetchInProgress())
    }

    @Test
    fun `isFetchInProgress false if job not active`() {
        xPostsSource.fetchJob = mock<Job>().apply {
            whenever(this.isActive).thenReturn(false)
        }
        assertFalse(xPostsSource.isFetchInProgress())
    }

    @Test
    fun `isFetchInProgress false if job null`() {
        xPostsSource.fetchJob = null
        assertFalse(xPostsSource.isFetchInProgress())
    }

    private fun suggestionsFromResult(result: XPostsResult.Result): List<Suggestion> =
        result.xPosts
            .map { Suggestion.fromXpost(it) }
            .sortedBy { it.value }
}
