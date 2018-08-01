package org.wordpress.android.networking

import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore

@RunWith(MockitoJUnitRunner::class)
class PageStoreTest {
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var site: SiteModel
    private val pageWithoutQuery = PostModel()
    private val pageWithQuery = PostModel()
    private val pageWithoutTitle = PostModel()
    private lateinit var store: PageStore

    private val query = "que"

    @Before
    fun setUp() {
        pageWithoutQuery.title = "page 1"
        pageWithQuery.title = "page2 start $query end "
        val pages = listOf(pageWithoutQuery, pageWithQuery, pageWithoutTitle)
        whenever(postStore.getPagesForSite(site)).thenReturn(pages)
        store = PageStore(postStore, dispatcher)
    }

    @Test
    fun searchFindsAllResultsContainingText() {
        val result = runBlocking {
            store.search(site, query)
        }

        assertEquals(result.size, 1)
        assertEquals(result[0].title, pageWithQuery.title)
    }


    @Test
    fun emptySearchResultWhenNothingContainsQuery() {
        val result = runBlocking {
            store.search(site, "foo")
        }

        assertEquals(result.size, 0)
    }
}
