package org.wordpress.android.ui.rs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
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
    fun `upload of a post of the observed site is reported`() {
        val emissions = countEmissions {
            listener.onPostUploaded(OnPostUploaded(post(), false))
        }

        assertThat(emissions).isEqualTo(1)
    }

    @Test
    fun `upload of a page is ignored by a post listener`() {
        val emissions = countEmissions {
            listener.onPostUploaded(OnPostUploaded(post(isPage = true), false))
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `upload of a page is reported to a page listener`() {
        val emissions = countEmissions(isPages = true) {
            listener.onPostUploaded(OnPostUploaded(post(isPage = true), false))
        }

        assertThat(emissions).isEqualTo(1)
    }

    @Test
    fun `upload from another site is ignored`() {
        val emissions = countEmissions {
            listener.onPostUploaded(OnPostUploaded(post(localSiteId = OTHER_SITE_ID), false))
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `failed upload is ignored`() {
        val emissions = countEmissions {
            listener.onPostUploaded(OnPostUploaded(post(), false).apply { error = genericError() })
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `upload without a post is ignored`() {
        val emissions = countEmissions {
            listener.onPostUploaded(OnPostUploaded(null, false))
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `remote update of a post of the observed site is reported`() {
        val emissions = countEmissions {
            whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(post())

            listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
        }

        assertThat(emissions).isEqualTo(1)
    }

    @Test
    fun `local update is ignored`() {
        val emissions = countEmissions {
            listener.onPostChanged(changed(updatePost(isLocalUpdate = true)))
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `remote update of a page is ignored by a post listener`() {
        val emissions = countEmissions {
            whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(post(isPage = true))

            listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `remote update from another site is ignored`() {
        val emissions = countEmissions {
            whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID))
                .thenReturn(post(localSiteId = OTHER_SITE_ID))

            listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `delete is reported even though the post is already gone from the store`() {
        val emissions = countEmissions {
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

        assertThat(emissions).isEqualTo(1)
    }

    @Test
    fun `restore is reported`() {
        val emissions = countEmissions {
            whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(post())

            listener.onPostChanged(
                changed(CauseOfOnPostChanged.RestorePost(LOCAL_POST_ID, REMOTE_POST_ID))
            )
        }

        assertThat(emissions).isEqualTo(1)
    }

    @Test
    fun `remote autosave is ignored`() {
        val emissions = countEmissions {
            listener.onPostChanged(
                changed(CauseOfOnPostChanged.RemoteAutoSavePost(LOCAL_POST_ID, REMOTE_POST_ID))
            )
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `a list fetch is ignored`() {
        val emissions = countEmissions {
            listener.onPostChanged(changed(CauseOfOnPostChanged.FetchPosts))
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `a failed change is ignored`() {
        val emissions = countEmissions {
            listener.onPostChanged(
                changed(updatePost(isLocalUpdate = false)).apply { error = genericError() }
            )
        }

        assertThat(emissions).isZero()
    }

    @Test
    fun `the two events of a single publish are reported once`() {
        val emissions = countEmissions {
            whenever(postStore.getPostByLocalPostId(LOCAL_POST_ID)).thenReturn(post())

            listener.onPostChanged(changed(updatePost(isLocalUpdate = false)))
            listener.onPostUploaded(OnPostUploaded(post(), false))
        }

        assertThat(emissions).isEqualTo(1)
    }

    /**
     * Runs [events] against a started listener and returns how many times `changes` fired.
     *
     * The collector has to be given a chance to subscribe before the events are posted - the flow
     * has no replay, so anything emitted beforehand is dropped - and time has to be advanced
     * afterwards to get past the debounce.
     */
    private fun countEmissions(isPages: Boolean = false, events: () -> Unit): Int {
        var emissions = 0
        test {
            listener.start(site, isPages)
            val job = launch { listener.changes.collect { emissions++ } }
            advanceUntilIdle()

            events()
            advanceUntilIdle()

            job.cancel()
        }
        return emissions
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
