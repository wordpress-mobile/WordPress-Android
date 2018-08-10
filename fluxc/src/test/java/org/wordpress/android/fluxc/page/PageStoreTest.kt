package org.wordpress.android.fluxc.page

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction.DELETE_POST
import org.wordpress.android.fluxc.action.PostAction.FETCH_PAGES
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.model.page.PageStatus.SCHEDULED
import org.wordpress.android.fluxc.model.page.PageStatus.TRASHED
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload

@RunWith(MockitoJUnitRunner::class)
class PageStoreTest {
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var site: SiteModel
    private lateinit var actionCaptor: KArgumentCaptor<Action<Any>>

    private val query = "que"
    private val pageWithoutQuery = initPage(1, 10, "page 1")
    private val pageWithQuery = initPage(2, title = "page2 start $query end ")
    private val pageWithoutTitle = initPage(3, 10)
    private val differentPageTypes = listOf(
            initPage(1, 0, "page 1", "publish"),
            initPage(2, 0, "page 2", "draft"),
            initPage(3, 0, "page 3", "future"),
            initPage(4, 0, "page 4", "trash"),
            initPage(5, 0, "page 5", "private"),
            initPage(6, 0, "page 6", "pending"),
            initPage(7, 0, "page 7", "draft")
    )

    private val pageHierarchy = listOf(
            initPage(1, 0, "page 1", "publish", 1),
            initPage(11, 1, "page 2", "publish", 2),
            initPage(111, 2, "page 3", "publish", 3),
            initPage(12, 1, "page 4", "publish", 4),
            initPage(2, 0, "page 5", "publish", 5),
            initPage(4, 0, "page 6", "publish", 6),
            initPage(3, 0, "page 7", "publish", 7)
    )

    private lateinit var store: PageStore

    @Before
    fun setUp() {
        actionCaptor = argumentCaptor()
        val pages = listOf(pageWithoutQuery, pageWithQuery, pageWithoutTitle)
        whenever(postStore.getPagesForSite(site)).thenReturn(pages)
        store = PageStore(postStore, dispatcher)
    }

    @Test
    fun searchFindsAllResultsContainingText() {
        val result = runBlocking { store.search(site, query) }

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo(pageWithQuery.title)
    }

    @Test
    fun searchOrdersResultsByStatus() {
        val trashStatus = "trash"
        val draftStatus = "draft"
        val publishStatus = "publish"
        val futureStatus = "future"
        val title = "title"
        val trashedSite1 = initPage(1, title = title, status = trashStatus)
        val draftSite1 = initPage(2, title = title, status = draftStatus)
        val publishedSite1 = initPage(3, title = title, status = publishStatus)
        val scheduledSite1 = initPage(4, title = title, status = futureStatus)
        val scheduledSite2 = initPage(5, title = title, status = futureStatus)
        val publishedSite2 = initPage(6, title = title, status = publishStatus)
        val draftSite2 = initPage(7, title = title, status = draftStatus)
        val trashedSite2 = initPage(8, title = title, status = trashStatus)
        val pages = listOf(
                trashedSite1,
                draftSite1,
                publishedSite1,
                scheduledSite1,
                scheduledSite2,
                publishedSite2,
                draftSite2,
                trashedSite2
        )
        whenever(postStore.getPagesForSite(site)).thenReturn(pages)

        val result = runBlocking { store.groupedSearch(site, title) }

        assertThat(result.keys).contains(PUBLISHED, DRAFT, SCHEDULED, TRASHED)
        assertPage(result, 0, PageStatus.PUBLISHED)
        assertPage(result, 1, PageStatus.PUBLISHED)
        assertPage(result, 0, PageStatus.DRAFT)
        assertPage(result, 1, PageStatus.DRAFT)
        assertPage(result, 0, PageStatus.SCHEDULED)
        assertPage(result, 1, PageStatus.SCHEDULED)
        assertPage(result, 0, PageStatus.TRASHED)
        assertPage(result, 1, PageStatus.TRASHED)
    }

    private fun assertPage(map: Map<PageStatus, List<PageModel>>, position: Int, status: PageStatus) {
        val page = map[status]?.get(position)
        assertThat(page).isNotNull()
        assertThat(page!!.status).isEqualTo(status)
    }

    @Test
    fun emptySearchResultWhenNothingContainsQuery() {
        val result = runBlocking { store.search(site, "foo") }

        assertThat(result).isEmpty()
    }

    @Test
    fun requestPagesFetchesFromServerAndReturnsEvent() = runBlocking {
        val expected = OnPostChanged(5, false)
        expected.causeOfChange = FETCH_PAGES
        var event: OnPostChanged? = null
        val job = launch {
            event = store.requestPagesFromServer(site)
        }
        delay(10)
        store.onPostChanged(expected)
        delay(10)
        job.join()

        assertThat(expected).isEqualTo(event)
        verify(dispatcher).dispatch(any())
    }

    @Test
    fun requestPagesFetchesPaginatedFromServerAndReturnsSecondEvent() = runBlocking<Unit> {
        val firstEvent = OnPostChanged(5, true)
        val lastEvent = OnPostChanged(5, false)
        firstEvent.causeOfChange = FETCH_PAGES
        lastEvent.causeOfChange = FETCH_PAGES
        var event: OnPostChanged? = null
        val job = launch {
            event = store.requestPagesFromServer(site)
        }
        delay(10)
        store.onPostChanged(firstEvent)
        delay(10)
        store.onPostChanged(lastEvent)
        delay(10)
        job.join()

        assertThat(lastEvent).isEqualTo(event)
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())
        val firstPayload = actionCaptor.firstValue.payload as FetchPostsPayload
        assertThat(firstPayload.site).isEqualTo(site)
        assertThat(firstPayload.loadMore).isEqualTo(false)
        val lastPayload = actionCaptor.lastValue.payload as FetchPostsPayload
        assertThat(lastPayload.site).isEqualTo(site)
        assertThat(lastPayload.loadMore).isEqualTo(true)
    }

    @Test
    fun deletePageTest() = runBlocking<Unit> {
        val post = pageHierarchy[0]
        whenever(postStore.getPostByLocalPostId(post.id)).thenReturn(post)
        val event = OnPostChanged(0)
        event.causeOfChange = DELETE_POST
        val page = PageModel(post, site, null)
        var result: OnPostChanged? = null
        launch {
            result = store.deletePage(page)
        }
        delay(10)
        store.onPostChanged(event)
        delay(10)

        assertThat(result).isEqualTo(event)

        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        val payload = actionCaptor.firstValue.payload as RemotePostPayload
        assertThat(payload.site).isEqualTo(site)
        assertThat(payload.post).isEqualTo(post)
    }

    @Test
    fun requestPagesAndVerifyAllPageTypesPresent() = runBlocking<Unit> {
        val event = OnPostChanged(4, false)
        event.causeOfChange = FETCH_PAGES
        launch {
            store.requestPagesFromServer(site)
        }
        delay(10)
        store.onPostChanged(event)
        delay(10)

        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        val payload = actionCaptor.firstValue.payload as FetchPostsPayload
        assertThat(payload.site).isEqualTo(site)

        val pageTypes = payload.statusTypes
        assertThat(pageTypes.size).isEqualTo(4)
        assertThat(pageTypes.filter { it == PostStatus.PUBLISHED }.size).isEqualTo(1)
        assertThat(pageTypes.filter { it == PostStatus.DRAFT }.size).isEqualTo(1)
        assertThat(pageTypes.filter { it == PostStatus.TRASHED }.size).isEqualTo(1)
        assertThat(pageTypes.filter { it == PostStatus.SCHEDULED }.size).isEqualTo(1)

        whenever(postStore.getPagesForSite(site))
                .thenReturn(differentPageTypes.filter { payload.statusTypes.contains(PostStatus.fromPost(it)) })

        val pages = store.getPagesFromDb(site)

        assertThat(pages.size).isEqualTo(5)
        assertThat(pages.filter { it.status == PUBLISHED }.size).isEqualTo(1)
        assertThat(pages.filter { it.status == DRAFT }.size).isEqualTo(2)
        assertThat(pages.filter { it.status == TRASHED }.size).isEqualTo(1)
        assertThat(pages.filter { it.status == SCHEDULED }.size).isEqualTo(1)
    }

    @Test
    fun getTopLevelPageByLocalId() = runBlocking {
        doAnswer { invocation -> pageHierarchy.firstOrNull { it.id == invocation.arguments.first() } }
                .`when`(postStore).getPostByLocalPostId(any())

        val page = store.getPageByLocalId(1, site)

        assertThat(page).isNotNull()
        assertThat(page!!.pageId).isEqualTo(1)
        assertThat(page.remoteId).isEqualTo(1)
        assertThat(page.parent).isNull()
    }

    @Test
    fun getChildPageByRemoteId() = runBlocking {
        doAnswer { invocation -> pageHierarchy.firstOrNull { it.remotePostId == invocation.arguments.first() } }
                .`when`(postStore).getPostByRemotePostId(any(), any())

        val page = store.getPageByRemoteId(3, site)

        assertThat(page).isNotNull()
        assertThat(page!!.pageId).isEqualTo(111)
        assertThat(page.remoteId).isEqualTo(3)

        assertThat(page.parent).isNotNull()
        assertThat(page.parent!!.remoteId).isEqualTo(2)

        assertThat(page.parent!!.parent).isNotNull()
        assertThat(page.parent!!.parent!!.remoteId).isEqualTo(1)
        assertThat(page.parent!!.parent!!.parent).isNull()
    }

    @Test
    fun getPages() = runBlocking<Unit> {
        whenever(postStore.getPagesForSite(site)).thenReturn(pageHierarchy)

        val pages = store.getPagesFromDb(site)

        assertThat(pages.size).isEqualTo(7)
        assertThat(pages).doesNotContainNull()

        assertThat(pages.filter { it.pageId < 10 }.all { it.parent == null }).isTrue()
        assertThat(pages.filter { it.pageId > 10 }.all { it.parent != null }).isTrue()
    }

    private fun initPage(
        id: Int,
        parentId: Long? = null,
        title: String? = null,
        status: String? = "draft",
        remoteId: Long = id.toLong()
    ): PostModel {
        val page = PostModel()
        page.id = id
        parentId?.let {
            page.parentId = parentId
        }
        title?.let {
            page.title = it
        }
        status?.let {
            page.status = status
        }
        page.remotePostId = remoteId
        return page
    }
}
