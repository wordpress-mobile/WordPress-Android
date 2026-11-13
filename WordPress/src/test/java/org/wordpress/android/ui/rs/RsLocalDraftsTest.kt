package org.wordpress.android.ui.rs

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import javax.inject.Provider

@ExperimentalCoroutinesApi
class RsLocalDraftsTest : BaseUnitTest() {
    @Mock lateinit var context: Context
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var uploadUiStateUseCase: PostModelUploadUiStateUseCase
    @Mock lateinit var uploadServiceFacade: UploadServiceFacade

    private val trackers = mutableListOf<PostModelUploadStatusTracker>()
    private val trackerProvider = Provider { mock<PostModelUploadStatusTracker>().also { trackers += it } }

    private lateinit var site: SiteModel
    private lateinit var localDrafts: RsLocalDrafts

    @Before
    fun setUp() {
        site = SiteModel().apply { id = SITE_ID }
        localDrafts = RsLocalDrafts(
            appContext = context,
            postStore = postStore,
            uploadStatusTrackerProvider = trackerProvider,
            uploadUiStateUseCase = uploadUiStateUseCase,
            uploadServiceFacade = uploadServiceFacade,
        )
        whenever(uploadUiStateUseCase.createUploadUiState(any(), any(), any()))
            .thenReturn(PostUploadUiState.NothingToUpload)
    }

    // region load

    @Test
    fun `local drafts are listed most recently changed first`() = test {
        // The older draft has the higher id, so the change date has to win over the tie-breaker.
        val older = localDraft(id = 2, dateLocallyChanged = OLDER)
        val newer = localDraft(id = 1, dateLocallyChanged = NEWER)
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(older, newer))

        assertThat(load().map { it.localId }).containsExactly(1, 2)
    }

    @Test
    fun `drafts changed at the same moment are listed newest created first`() = test {
        // As after a batch of uploads fails together: each failure stamps the same change date.
        val drafts = listOf(1, 2, 3).map { localDraft(id = it, dateLocallyChanged = NEWER) }
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(drafts)

        assertThat(load().map { it.localId }).containsExactly(3, 2, 1)
    }

    @Test
    fun `a draft with nothing to upload is not marked as failed`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft()))

        assertThat(load().single().isUploadFailed).isFalse
    }

    @Test
    fun `a failed upload is marked as failed`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft()))
        whenever(uploadUiStateUseCase.createUploadUiState(any(), any(), any())).thenReturn(
            PostUploadUiState.UploadFailed(
                UploadError(PostError(PostErrorType.GENERIC_ERROR)),
                isEligibleForAutoUpload = false,
                retryWillPushChanges = true
            )
        )

        assertThat(load().single().isUploadFailed).isTrue
    }

    @Test
    fun `an upload in progress is not marked as failed`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft()))
        whenever(uploadUiStateUseCase.createUploadUiState(any(), any(), any()))
            .thenReturn(PostUploadUiState.UploadingPost(isDraft = true))

        assertThat(load().single().isUploadFailed).isFalse
    }

    @Test
    fun `the title is unescaped and its whitespace collapsed`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft(title = " Fish &amp;\n chips ")))

        assertThat(load().single().title).isEqualTo("Fish & chips")
    }

    @Test
    fun `a title keeps text that only looks like a tag`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(
            listOf(localDraft(title = "When x &lt; 5 and y &gt; 3, use <code>"))
        )

        assertThat(load().single().title).isEqualTo("When x < 5 and y > 3, use <code>")
    }

    @Test
    fun `a blank title is left blank for the UI to fill`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft(title = "  ")))

        assertThat(load().single().title).isEmpty()
    }

    @Test
    fun `the date is wrapped in the given format`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft()))

        assertThat(load(dateFormat = "Modified: %s").single().dateLabel).startsWith("Modified: ")
    }

    @Test
    fun `a draft without a change date has no date`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft(dateLocallyChanged = "")))

        assertThat(load(dateFormat = "Modified: %s").single().dateLabel).isEmpty()
    }

    @Test
    fun `every load uses its own upload status tracker`() = test {
        whenever(postStore.getLocalDraftPosts(site)).thenReturn(listOf(localDraft()))

        load()
        load()

        assertThat(trackers).hasSize(2)
        verify(uploadUiStateUseCase).createUploadUiState(any(), any(), same(trackers[0]))
        verify(uploadUiStateUseCase).createUploadUiState(any(), any(), same(trackers[1]))
    }

    // endregion

    // region actions

    @Test
    fun `retry uploads a draft as a first publish`() {
        val post = localDraft()
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(post)

        localDrafts.retry(site, LOCAL_ID)

        verify(uploadServiceFacade).uploadPost(any(), argThat<PostModel> { this === post }, eq(true), any())
    }

    @Test
    fun `retrying a private post is not tracked as a first publish`() {
        val post = localDraft().apply { setStatus("private") }
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(post)

        localDrafts.retry(site, LOCAL_ID)

        verify(uploadServiceFacade).uploadPost(any(), argThat<PostModel> { this === post }, eq(false), any())
    }

    @Test
    fun `retry leaves a post of another site alone`() {
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(localDraft(localSiteId = SITE_ID + 1))

        localDrafts.retry(site, LOCAL_ID)

        verify(uploadServiceFacade, never()).uploadPost(any(), any<PostModel>(), any(), any())
    }

    @Test
    fun `retry leaves a post the server has alone`() {
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(localDraft().apply { setIsLocalDraft(false) })

        localDrafts.retry(site, LOCAL_ID)

        verify(uploadServiceFacade, never()).uploadPost(any(), any<PostModel>(), any(), any())
    }

    @Test
    fun `open returns the draft`() {
        val post = localDraft()
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(post)

        assertThat(localDrafts.open(site, LOCAL_ID)).isSameAs(post)
    }

    @Test
    fun `opening a draft that is gone returns null`() {
        assertThat(localDrafts.open(site, LOCAL_ID)).isNull()
    }

    @Test
    fun `opening a draft that has since been uploaded returns null`() {
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(localDraft().apply { setIsLocalDraft(false) })

        assertThat(localDrafts.open(site, LOCAL_ID)).isNull()
    }

    @Test
    fun `a draft uploading or queued is reported as such`() {
        val post = localDraft()
        whenever(postStore.getPostByLocalPostId(LOCAL_ID)).thenReturn(post)
        whenever(uploadServiceFacade.isPostUploadingOrQueued(post)).thenReturn(true)

        assertThat(localDrafts.isUploadingOrQueued(site, LOCAL_ID)).isTrue
    }

    @Test
    fun `a draft that is gone is not uploading`() {
        assertThat(localDrafts.isUploadingOrQueued(site, LOCAL_ID)).isFalse
    }

    // endregion

    private suspend fun load(dateFormat: String? = null) =
        localDrafts.load(site, nowLabel = "Now", dateFormat = dateFormat)

    private fun localDraft(
        id: Int = LOCAL_ID,
        title: String = "Title",
        dateLocallyChanged: String = OLDER,
        localSiteId: Int = SITE_ID
    ) = PostModel().apply {
        setId(id)
        setTitle(title)
        setDateLocallyChanged(dateLocallyChanged)
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
