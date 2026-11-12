package org.wordpress.android.ui.rs

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtilsWrapper
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.pages.CreatePageListItemLabelsUseCase
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.posts.CreatePostListItemLabelsUseCase

@ExperimentalCoroutinesApi
class RsLocalDraftsTest : BaseUnitTest() {
    @Mock lateinit var context: Context
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var pageStore: PageStore
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var uploadStatusTracker: PostModelUploadStatusTracker
    @Mock lateinit var uploadUiStateUseCase: PostModelUploadUiStateUseCase
    @Mock lateinit var pageLabels: CreatePageListItemLabelsUseCase
    @Mock lateinit var uploadServiceFacade: UploadServiceFacade
    @Mock lateinit var pendingDraftsNotifications: PendingDraftsNotificationsUtilsWrapper

    private lateinit var site: SiteModel
    private lateinit var localDrafts: RsLocalDrafts

    @Before
    fun setUp() {
        site = SiteModel().apply { id = SITE_ID }
        localDrafts = RsLocalDrafts(
            appContext = context,
            postStore = postStore,
            pageStore = pageStore,
            dispatcher = dispatcher,
            uploadStatusTracker = uploadStatusTracker,
            uploadUiStateUseCase = uploadUiStateUseCase,
            postLabels = CreatePostListItemLabelsUseCase(),
            pageLabels = pageLabels,
            uploadServiceFacade = uploadServiceFacade,
            pendingDraftsNotifications = pendingDraftsNotifications
        )
        whenever(uploadUiStateUseCase.createUploadUiState(any(), any(), any()))
            .thenReturn(PostUploadUiState.NothingToUpload)
    }

    // region load

    @Test
    fun `local drafts are listed most recently changed first`() = test {
        val older = localDraft(id = 1, title = "Older", dateLocallyChanged = OLDER)
        val newer = localDraft(id = 2, title = "Newer", dateLocallyChanged = NEWER)
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(older, newer))

        val drafts = load()

        assertThat(drafts.map { it.localId }).containsExactly(2, 1)
    }

    @Test
    fun `a draft with nothing to upload has no status label, since its badge already says it is local`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft()))

        val draft = load().single()

        assertThat(draft.statusLabels).isEmpty()
        assertThat(draft.isUploadFailed).isFalse
        assertThat(draft.canRetry).isFalse
    }

    @Test
    fun `a failed upload is labelled with its error and can be retried`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft()))
        whenever(uploadUiStateUseCase.createUploadUiState(any(), any(), any())).thenReturn(
            PostUploadUiState.UploadFailed(
                UploadError(PostError(PostErrorType.UNAUTHORIZED)),
                isEligibleForAutoUpload = false,
                retryWillPushChanges = true
            )
        )

        val draft = load().single()

        assertThat(draft.statusLabels).containsExactly(UiStringRes(R.string.error_refresh_unauthorized_posts))
        assertThat(draft.isUploadFailed).isTrue
        assertThat(draft.canRetry).isTrue
    }

    @Test
    fun `an upload in progress cannot be retried`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft()))
        whenever(uploadUiStateUseCase.createUploadUiState(any(), any(), any()))
            .thenReturn(PostUploadUiState.UploadingPost(isDraft = true))

        val draft = load().single()

        assertThat(draft.statusLabels).containsExactly(UiStringRes(R.string.post_uploading_draft))
        assertThat(draft.canRetry).isFalse
    }

    @Test
    fun `the title and excerpt are shown as plain text`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(
            listOf(localDraft(title = "Fish &amp; chips", content = "<!-- wp:paragraph --><p>Hello\n  world</p>"))
        )

        val draft = load().single()

        assertThat(draft.title).isEqualTo("Fish & chips")
        assertThat(draft.excerpt).isEqualTo("Hello world")
    }

    @Test
    fun `a query keeps the drafts whose title or content contains it, ignoring case`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(
            listOf(
                localDraft(id = 1, title = "Banana bread"),
                localDraft(id = 2, title = "Notes", content = "Grandma's BANANA recipe"),
                localDraft(id = 3, title = "Holiday")
            )
        )

        val drafts = load(query = "banana")

        assertThat(drafts.map { it.localId }).containsExactlyInAnyOrder(1, 2)
    }

    @Test
    fun `cached upload state is dropped before every load`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft(id = 5)))

        load()

        verify(uploadStatusTracker).invalidateUploadStatus(listOf(5))
    }

    @Test
    fun `pages are read from the page store and keep their qualifiers`() = test {
        val page = localDraft(isPage = true)
        pageStore.stub { on { getLocalDraftPages(site) } doReturn listOf(page) }
        whenever(pageLabels.createLabels(any(), any())).thenReturn(
            listOf(UiStringRes(R.string.page_local_draft), UiStringRes(R.string.page_status_pending_review)) to null
        )

        val draft = load(isPage = true).single()

        assertThat(draft.statusLabels).containsExactly(UiStringRes(R.string.page_status_pending_review))
        verify(postStore, never()).getLocalDraftPosts(any())
    }

    // endregion

    // region actions

    @Test
    fun `retry uploads the draft again`() {
        val post = localDraft()
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(post)

        localDrafts.retry(site, LOCAL_ID)

        verify(uploadServiceFacade).uploadPost(any(), argThat<PostModel> { this === post }, any(), any())
    }

    @Test
    fun `retry leaves a post of another site alone`() {
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(localDraft(localSiteId = SITE_ID + 1))

        localDrafts.retry(site, LOCAL_ID)

        verify(uploadServiceFacade, never()).uploadPost(any(), any<PostModel>(), any(), any())
    }

    @Test
    fun `delete removes the draft and everything that leads to it`() {
        val post = localDraft()
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(post)

        localDrafts.delete(site, LOCAL_ID)

        verify(dispatcher).dispatch(argThat { type == PostAction.REMOVE_POST && payload === post })
        verify(pendingDraftsNotifications).cancelPendingDraftAlarms(context, LOCAL_ID)
        verify(pendingDraftsNotifications).dismissPendingDraftNotification(context, LOCAL_ID)
        verify(uploadServiceFacade).cancelFinalNotification(post)
    }

    @Test
    fun `delete leaves a post the server has alone`() {
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(localDraft().apply { setIsLocalDraft(false) })

        localDrafts.delete(site, LOCAL_ID)

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `opening a draft cancels its queued upload`() {
        val post = localDraft()
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(post)
        whenever(uploadServiceFacade.isPostUploadingOrQueued(post)).thenReturn(true)

        val opened = localDrafts.open(site, LOCAL_ID)

        assertThat(opened).isSameAs(post)
        verify(uploadServiceFacade).cancelQueuedPostUpload(post)
    }

    @Test
    fun `opening a draft that is gone returns null`() {
        assertThat(localDrafts.open(site, LOCAL_ID)).isNull()
    }

    // endregion

    private suspend fun load(isPage: Boolean = false, query: String = "") =
        localDrafts.load(site, isPage, query, nowLabel = "Now", dateFormat = null)

    @Suppress("LongParameterList")
    private fun localDraft(
        id: Int = LOCAL_ID,
        title: String = "Title",
        content: String = "",
        dateLocallyChanged: String = OLDER,
        isPage: Boolean = false,
        localSiteId: Int = SITE_ID
    ) = PostModel().apply {
        setId(id)
        setTitle(title)
        setContent(content)
        setDateLocallyChanged(dateLocallyChanged)
        setIsPage(isPage)
        setIsLocalDraft(true)
        setLocalSiteId(localSiteId)
        setStatus("draft")
    }

    private companion object {
        const val SITE_ID = 3
        const val LOCAL_ID = 11

        // FluxC stores "+00:00" offsets, which Android's SimpleDateFormat parses but the JVM's doesn't,
        // so these use "+0000". Both are well over a week old, so the formatter shows an absolute date
        // rather than reaching for the stubbed-out android.text.format.DateUtils.
        const val OLDER = "2024-01-01T10:00:00+0000"
        const val NEWER = "2024-02-01T10:00:00+0000"
    }
}
