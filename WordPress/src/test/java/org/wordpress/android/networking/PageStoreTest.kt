package org.wordpress.android.networking

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction.FETCH_PAGES
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged

@RunWith(MockitoJUnitRunner::class)
class PageStoreTest {
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var site: SiteModel

    private val query = "que"
    private val pageWithoutQuery = initPage(1, 10, "page 1")
    private val pageWithQuery = initPage(2, title = "page2 start $query end ")
    private val pageWithoutTitle = initPage(3, 10)

    private fun initPage(id: Int, parentId: Long? = null, title: String? = null): PostModel {
        val page = PostModel()
        page.id = id
        parentId?.let {
            page.parentId = parentId
        }
        title?.let {
            page.title = it
        }
        page.status = "DRAFT"
        return page
    }
    private lateinit var store: PageStore

    @Before
    fun setUp() {
        val pages = listOf(pageWithoutQuery, pageWithQuery, pageWithoutTitle)
        whenever(postStore.getPagesForSite(site)).thenReturn(pages)
        store = PageStore(postStore, dispatcher)
    }

    @Test
    fun searchFindsAllResultsContainingText() {
        val result = runBlocking { store.search(site, query) }

        assertEquals(result.size, 1)
        assertEquals(result[0].title, pageWithQuery.title)
    }


    @Test
    fun emptySearchResultWhenNothingContainsQuery() {
        val result = runBlocking { store.search(site, "foo") }

        assertEquals(result.size, 0)
    }

    @Test
    fun loadsPagesFromDb() {
        val pages = runBlocking { store.loadPagesFromDb(site) }

        assertEquals(pages.size, 3)
        assertEquals(pages[0].pageId, pageWithoutQuery.id)
        assertEquals(pages[0].parentId, pageWithoutQuery.parentId)
        assertEquals(pages[1].pageId, pageWithQuery.id)
        assertEquals(pages[1].parentId, pageWithQuery.parentId)
        assertEquals(pages[2].pageId, pageWithoutTitle.id)
        assertEquals(pages[2].parentId, pageWithoutTitle.parentId)
    }

    @Test
    fun requestPagesFetchesFromServerAndReturnsEvent() {
        val expected = OnPostChanged(5, true)
        expected.causeOfChange = FETCH_PAGES
        val async = async { store.requestPagesFromServer(site, true) }

        val result = runBlocking {
            store.onPostChanged(expected)
            async.await()
        }

        assertEquals(expected, result)
        verify(dispatcher).dispatch(eq(PostActionBuilder.newFetchPagesAction(FetchPostsPayload(site, true))))
    }
}
