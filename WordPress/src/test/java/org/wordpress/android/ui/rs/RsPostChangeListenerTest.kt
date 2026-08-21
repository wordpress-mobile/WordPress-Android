package org.wordpress.android.ui.rs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType

@ExperimentalCoroutinesApi
class RsPostChangeListenerTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var postStore: PostStore

    private lateinit var site: SiteModel
    private lateinit var listener: RsPostChangeListener

    @Before
    fun setUp() {
        site = SiteModel().apply { id = SITE_ID }
        listener = RsPostChangeListener(dispatcher, postStore)
    }

    @Test
    fun `start registers with the dispatcher and stop unregisters`() {
        listener.start(site, isPages = false)
        verify(dispatcher).register(listener)

        listener.stop()

        verify(dispatcher).unregister(listener)
    }

    @Test
    fun `stop without a start leaves the dispatcher alone`() {
        listener.stop()

        verify(dispatcher, never()).unregister(listener)
    }

    @Test
    fun `starting twice registers once`() {
        listener.start(site, isPages = false)
        listener.start(site, isPages = false)

        verify(dispatcher, times(1)).register(listener)
    }

    @Test
    fun `a change reported after stop is not emitted`() = assertEmits(0) {
        listener.stop()

        listener.onPostUploaded(OnPostUploaded(post(), false))
    }

    @Test
    fun `upload of a post of the observed site is reported`() = assertEmits(1) {
        listener.onPostUploaded(OnPostUploaded(post(), false))
    }

    @Test
    fun `upload of a page is ignored by a post listener`() = assertEmits(0) {
        listener.onPostUploaded(OnPostUploaded(post(isPage = true), false))
    }

    @Test
    fun `upload of a page is reported to a page listener`() = assertEmits(1, isPages = true) {
        listener.onPostUploaded(OnPostUploaded(post(isPage = true), false))
    }

    @Test
    fun `upload from another site is ignored`() = assertEmits(0) {
        listener.onPostUploaded(OnPostUploaded(post(localSiteId = OTHER_SITE_ID), false))
    }

    @Test
    fun `failed upload is ignored`() = assertEmits(0) {
        listener.onPostUploaded(OnPostUploaded(post(), false).apply { error = genericError() })
    }

    @Test
    fun `upload without a post is ignored`() = assertEmits(0) {
        listener.onPostUploaded(OnPostUploaded(null, false))
    }

    @Test
    fun `remote update of a post of the observed site is reported`() = assertEmits(1) {
        whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(post())

        listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
    }

    @Test
    fun `local update is ignored`() = assertEmits(0) {
        listener.onPostChanged(changed(updatePost(isLocalUpdate = true)))
    }

    @Test
    fun `remote update of a page is ignored by a post listener`() = assertEmits(0) {
        whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(post(isPage = true))

        listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
    }

    @Test
    fun `remote update from another site is ignored`() = assertEmits(0) {
        whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID))
            .thenReturn(post(localSiteId = OTHER_SITE_ID))

        listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
    }

    @Test
    fun `delete is reported even though the post is already gone from the store`() = assertEmits(1) {
        whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(null)

        listener.onPostChanged(
            changed(
                CauseOfOnPostChanged.DeletePost(
                    LOCAL_POST_ID,
                    REMOTE_POST_ID,
                    PostDeleteActionType.TRASH
                )
            )
        )
    }

    @Test
    fun `restore is reported`() = assertEmits(1) {
        whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(post())

        listener.onPostChanged(
            changed(CauseOfOnPostChanged.RestorePost(LOCAL_POST_ID, REMOTE_POST_ID))
        )
    }

    @Test
    fun `remote autosave is ignored`() = assertEmits(0) {
        listener.onPostChanged(
            changed(CauseOfOnPostChanged.RemoteAutoSavePost(LOCAL_POST_ID, REMOTE_POST_ID))
        )
    }

    @Test
    fun `a removed post is ignored`() = assertEmits(0) {
        listener.onPostChanged(
            changed(CauseOfOnPostChanged.RemovePost(LOCAL_POST_ID, REMOTE_POST_ID))
        )
    }

    @Test
    fun `a list fetch is ignored`() = assertEmits(0) {
        listener.onPostChanged(changed(CauseOfOnPostChanged.FetchPosts))
    }

    @Test
    fun `a failed change is ignored`() = assertEmits(0) {
        listener.onPostChanged(
            changed(updatePost(isLocalUpdate = false)).apply { error = genericError() }
        )
    }

    @Test
    fun `the two events of a single publish are reported once`() = assertEmits(1) {
        whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(post())

        listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
        listener.onPostUploaded(OnPostUploaded(post(), false))
    }

    @Test
    fun `upload of a post of the observed site reports the post it wrote`() {
        val uploads = collectUploads {
            listener.onPostUploaded(OnPostUploaded(uploadedPost(), false))
        }

        assertThat(uploads).containsExactly(RsUploadedPost(REMOTE_POST_ID, PostStatus.PUBLISHED))
    }

    @Test
    fun `a post published with a future date is reported as scheduled`() {
        val uploads = collectUploads {
            listener.onPostUploaded(
                OnPostUploaded(uploadedPost(status = "future"), false)
            )
        }

        assertThat(uploads).containsExactly(RsUploadedPost(REMOTE_POST_ID, PostStatus.SCHEDULED))
    }

    @Test
    fun `an upload that produced no remote id is not reported`() {
        val uploads = collectUploads {
            listener.onPostUploaded(OnPostUploaded(uploadedPost(remotePostId = 0L), false))
        }

        assertThat(uploads).isEmpty()
    }

    @Test
    fun `a failed upload is not reported`() {
        val uploads = collectUploads {
            listener.onPostUploaded(
                OnPostUploaded(uploadedPost(), false).apply { error = genericError() }
            )
        }

        assertThat(uploads).isEmpty()
    }

    @Test
    fun `an upload from another site is not reported`() {
        val uploads = collectUploads {
            listener.onPostUploaded(
                OnPostUploaded(uploadedPost(localSiteId = OTHER_SITE_ID), false)
            )
        }

        assertThat(uploads).isEmpty()
    }

    @Test
    fun `an uploaded page is not reported to a post listener`() {
        val uploads = collectUploads {
            listener.onPostUploaded(OnPostUploaded(uploadedPost(isPage = true), false))
        }

        assertThat(uploads).isEmpty()
    }

    @Test
    fun `a remote update is not reported as an upload`() {
        val uploads = collectUploads {
            whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(uploadedPost())

            listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
        }

        assertThat(uploads).isEmpty()
    }

    /**
     * Asserts that running [events] against a started listener makes `changes` fire [expected]
     * times.
     *
     * The collector has to be given a chance to subscribe before the events are posted - the flow
     * has no replay, so anything emitted beforehand is dropped - and time has to be advanced
     * afterwards to get past the debounce.
     */
    private fun assertEmits(expected: Int, isPages: Boolean = false, events: () -> Unit) {
        var emissions = 0
        test {
            listener.start(site, isPages)
            val job = launch { listener.changes.collect { emissions++ } }
            advanceUntilIdle()

            events()
            advanceUntilIdle()

            job.cancel()
        }
        assertThat(emissions).isEqualTo(expected)
    }

    /** Runs [events] against a started listener and returns everything `uploads` reported. */
    private fun collectUploads(isPages: Boolean = false, events: () -> Unit): List<RsUploadedPost> {
        val uploads = mutableListOf<RsUploadedPost>()
        test {
            listener.start(site, isPages)
            val job = launch { listener.uploads.collect { uploads.add(it) } }
            advanceUntilIdle()

            events()
            advanceUntilIdle()

            job.cancel()
        }
        return uploads
    }

    private fun uploadedPost(
        isPage: Boolean = false,
        localSiteId: Int = SITE_ID,
        remotePostId: Long = REMOTE_POST_ID,
        status: String = "publish"
    ) = post(isPage, localSiteId).apply {
        setRemotePostId(remotePostId)
        setStatus(status)
    }

    private fun post(isPage: Boolean = false, localSiteId: Int = SITE_ID) = PostModel().apply {
        setIsPage(isPage)
        setLocalSiteId(localSiteId)
    }

    private fun updatePost(isLocalUpdate: Boolean) =
        CauseOfOnPostChanged.UpdatePost(LOCAL_POST_ID, REMOTE_POST_ID, isLocalUpdate)

    private fun changed(cause: CauseOfOnPostChanged) = OnPostChanged(cause, 1)

    private fun genericError() = PostStore.PostError(PostStore.PostErrorType.GENERIC_ERROR)

    companion object {
        private const val SITE_ID = 1
        private const val OTHER_SITE_ID = 99
        private const val LOCAL_POST_ID = 42
        private const val REMOTE_POST_ID = 4242L
    }
}
