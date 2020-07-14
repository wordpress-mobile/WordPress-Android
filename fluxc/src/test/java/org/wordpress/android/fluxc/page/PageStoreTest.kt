package org.wordpress.android.fluxc.page

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.FetchPages
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.page.PageStatus.PENDING
import org.wordpress.android.fluxc.model.page.PageStatus.PRIVATE
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.model.page.PageStatus.SCHEDULED
import org.wordpress.android.fluxc.model.page.PageStatus.TRASHED
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.network.utils.CurrentDateUtils
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PageStore.OnPageChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType.TRASH
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.UNKNOWN_POST
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.test
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class PageStoreTest {
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var currentDateUtils: CurrentDateUtils
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
            initPage(7, 0, "page 7", "draft"),
            initPage(7, 0, "page 8", "unknown")
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
        store = PageStore(postStore, PostSqlUtils(), dispatcher, currentDateUtils, initCoroutineEngine())
    }

    @Test
    fun searchFindsAllResultsContainingText() {
        val result = runBlocking { store.search(site, query) }

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo(pageWithQuery.title)
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
    fun requestPagesFetchesFromServerAndReturnsEvent() = test {
        val expected = OnPostChanged(CauseOfOnPostChanged.FetchPages, 5, false)
        var event: OnPageChanged? = null
        val job = launch {
            event = store.requestPagesFromServer(site, true)
        }
        delay(10)
        store.onPostChanged(expected)
        delay(10)
        job.join()

        assertThat(event).isEqualTo(OnPageChanged.Success)
        verify(dispatcher).dispatch(any())
    }

    @Test
    fun requestPagesFetchesFromServerAndReturnsEventFromTwoRequests() = test {
        val expected = OnPostChanged(CauseOfOnPostChanged.FetchPages, 5, false)
        var firstEvent: OnPageChanged? = null
        var secondEvent: OnPageChanged? = null
        val firstJob = launch {
            firstEvent = store.requestPagesFromServer(site, true)
        }
        val secondJob = launch {
            secondEvent = store.requestPagesFromServer(site, true)
        }
        delay(10)
        store.onPostChanged(expected)
        delay(10)
        firstJob.join()
        secondJob.join()

        assertThat(firstEvent).isEqualTo(OnPageChanged.Success)
        assertThat(secondEvent).isEqualTo(OnPageChanged.Success)
        verify(dispatcher).dispatch(any())
    }

    @Test
    fun `request pages returns cached result when there is recent call`() = test {
        initNow(hour = 8, minute = 0)
        val firstJob = launch {
            store.requestPagesFromServer(site, forced = true)
        }
        delay(10)
        store.onPostChanged(OnPostChanged(FetchPages, 5, false))
        delay(10)
        firstJob.join()
        initNow(hour = 8, minute = 59)

        val secondEvent = store.requestPagesFromServer(site, forced = false)

        assertThat(secondEvent).isEqualTo(OnPageChanged.Success)
        verify(dispatcher).dispatch(any())
    }

    @Test
    fun `request pages fetches data when there is no recent call`() = test {
        val expected = OnPostChanged(CauseOfOnPostChanged.FetchPages, 5, false)
        var secondEvent: OnPageChanged? = null
        initNow(hour = 8, minute = 0)
        val firstJob = launch {
            store.requestPagesFromServer(site, forced = true)
        }
        delay(10)
        store.onPostChanged(expected)
        delay(10)
        firstJob.join()
        initNow(hour = 9, minute = 0)

        val secondJob = launch {
            secondEvent = store.requestPagesFromServer(site, forced = false)
        }
        delay(10)
        store.onPostChanged(expected)
        delay(10)
        secondJob.join()

        assertThat(secondEvent).isEqualTo(OnPageChanged.Success)
        verify(dispatcher, times(2)).dispatch(any())
    }

    private fun initNow(hour: Int, minute: Int) {
        val now = Calendar.getInstance(Locale.UK)
        now.set(2020, 1, 1, hour, minute)
        whenever(currentDateUtils.getCurrentCalendar()).thenReturn(now)
    }

    @Test
    fun requestPagesFetchesPaginatedFromServerAndReturnsSecondEvent() = test {
        val firstEvent = OnPostChanged(CauseOfOnPostChanged.FetchPages, 5, true)
        val lastEvent = OnPostChanged(CauseOfOnPostChanged.FetchPages, 5, false)
        var event: OnPageChanged? = null
        val job = launch {
            event = store.requestPagesFromServer(site, true)
        }
        delay(10)
        store.onPostChanged(firstEvent)
        delay(10)
        store.onPostChanged(lastEvent)
        delay(10)
        job.join()

        assertThat(event).isEqualTo(OnPageChanged.Success)
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())
        val firstPayload = actionCaptor.firstValue.payload as FetchPostsPayload
        assertThat(firstPayload.site).isEqualTo(site)
        assertThat(firstPayload.loadMore).isEqualTo(false)
        val lastPayload = actionCaptor.lastValue.payload as FetchPostsPayload
        assertThat(lastPayload.site).isEqualTo(site)
        assertThat(lastPayload.loadMore).isEqualTo(true)
    }

    @Test
    fun deletePageTest() = test {
        val post = pageHierarchy[0]
        whenever(postStore.getPostByLocalPostId(post.id)).thenReturn(post)
        val event = OnPostChanged(CauseOfOnPostChanged.DeletePost(post.id, post.remotePostId, TRASH), 0)
        val page = PageModel(post, site, post.id, post.title, PageStatus.fromPost(post), Date(), post.isLocallyChanged,
                post.remotePostId, null, post.featuredImageId)
        var result: OnPageChanged? = null
        launch {
            result = store.deletePageFromServer(page)
        }
        delay(10)
        store.onPostChanged(event)
        delay(10)

        assertThat(result).isEqualTo(OnPageChanged.Success)

        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        val payload = actionCaptor.firstValue.payload as RemotePostPayload
        assertThat(payload.site).isEqualTo(site)
        assertThat(payload.post).isEqualTo(post)
    }

    @Test
    fun deletePageWithErrorTest() = test {
        val post = pageHierarchy[0]
        whenever(postStore.getPostByLocalPostId(post.id)).thenReturn(null)
        val event = OnPostChanged(CauseOfOnPostChanged.DeletePost(post.id, post.remotePostId, TRASH), 0)
        event.error = PostError(UNKNOWN_POST)
        val page = PageModel(post, site, post.id, post.title, PageStatus.fromPost(post), Date(), post.isLocallyChanged,
            post.remotePostId, null, post.featuredImageId)
        var result: OnPageChanged? = null
        launch {
            result = store.deletePageFromServer(page)
        }
        delay(10)

        assertThat(result?.error?.type).isEqualTo(event.error.type)
    }

    @Test
    fun requestPagesAndVerifyAllPageTypesPresent() = test {
        val event = OnPostChanged(CauseOfOnPostChanged.FetchPages, 4, false)
        launch {
            store.requestPagesFromServer(site, true)
        }
        delay(10)
        store.onPostChanged(event)
        delay(10)

        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        val payload = actionCaptor.firstValue.payload as FetchPostsPayload
        assertThat(payload.site).isEqualTo(site)

        val pageTypes = payload.statusTypes
        assertThat(pageTypes.size).isEqualTo(6)
        assertThat(pageTypes.filter { it == PostStatus.PUBLISHED }.size).isEqualTo(1)
        assertThat(pageTypes.filter { it == PostStatus.DRAFT }.size).isEqualTo(1)
        assertThat(pageTypes.filter { it == PostStatus.TRASHED }.size).isEqualTo(1)
        assertThat(pageTypes.filter { it == PostStatus.SCHEDULED }.size).isEqualTo(1)

        whenever(postStore.getPagesForSite(site))
                .thenReturn(differentPageTypes.filter { payload.statusTypes.contains(PostStatus.fromPost(it)) })

        val pages = store.getPagesFromDb(site)

        assertThat(pages.size).isEqualTo(7)
        assertThat(pages.filter { it.status == PUBLISHED }.size).isEqualTo(1)
        assertThat(pages.filter { it.status == DRAFT }.size).isEqualTo(2)
        assertThat(pages.filter { it.status == TRASHED }.size).isEqualTo(1)
        assertThat(pages.filter { it.status == SCHEDULED }.size).isEqualTo(1)
        assertThat(pages.filter { it.status == PRIVATE }.size).isEqualTo(1)
        assertThat(pages.filter { it.status == PENDING }.size).isEqualTo(1)
    }

    @Test
    fun getTopLevelPageByLocalId() = test {
        doAnswer { invocation -> pageHierarchy.firstOrNull { it.id == invocation.arguments.first() } }
                .`when`(postStore).getPostByLocalPostId(any())

        val page = store.getPageByLocalId(1, site)

        assertThat(page).isNotNull()
        assertThat(page!!.pageId).isEqualTo(1)
        assertThat(page.remoteId).isEqualTo(1)
        assertThat(page.parent).isNull()
    }

    @Test
    fun getChildPageByRemoteId() = test {
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
    fun getPages() = test {
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
        page.setId(id)
        parentId?.let {
            page.setParentId(parentId)
        }
        title?.let {
            page.setTitle(it)
        }
        status?.let {
            page.setStatus(status)
        }
        page.setRemotePostId(remoteId)
        return page
    }
}
