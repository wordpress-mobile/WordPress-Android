package org.wordpress.android.ui.posts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostStatusFetched
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.UNKNOWN_POST
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftResult
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftUseCase
import org.wordpress.android.ui.uploads.OnAutoSavePostIfNotDraftCallback
import java.lang.reflect.Constructor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val FETCH_POST_STATUS_ERROR_MESSAGE = "FETCH_POST_STATUS_ERROR_MESSAGE"
private const val AUTO_SAVE_POST_ERROR_MESSAGE = "AUTO_SAVE_POST_ERROR_MESSAGE"
private const val DRAFT_STATUS = "draft"
private const val PUBLISH_STATUS = "publish"

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AutoSavePostIfNotDraftUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var postStore: PostStore

    @Test(expected = IllegalArgumentException::class)
    fun `local draft throws IllegalArgumentException`() {
        val useCase = AutoSavePostIfNotDraftUseCase(mock(), postStore, TEST_DISPATCHER)
        val post = PostModel()
        post.setIsLocalDraft(true)
        useCase.autoSavePostOrUpdateDraft(
                RemotePostPayload(post, SiteModel()),
                object : OnAutoSavePostIfNotDraftCallback {
                    override fun handleAutoSavePostIfNotDraftResult(result: AutoSavePostIfNotDraftResult) {
                    }
                })
    }

    @Test
    fun `error while fetching post status will result in FetchPostStatusFailed`() {
        val remotePostPayload = createRemotePostPayload()
        val onPostStatusFetched = createOnPostStatusFetchedEvent(
                post = remotePostPayload.post,
                status = PUBLISH_STATUS,
                error = PostError(UNKNOWN_POST, FETCH_POST_STATUS_ERROR_MESSAGE)
        )
        val useCase = createUseCase(onPostStatusFetched)
        useCase.autoSavePostOrUpdateDraftAndAssertResult(remotePostPayload) { result ->
            assertThat(result).isInstanceOf(AutoSavePostIfNotDraftResult.FetchPostStatusFailed::class.java)
        }
    }

    @Test
    fun `post is draft in remote`() {
        val remotePostPayload = createRemotePostPayload()
        val onPostStatusFetched = createOnPostStatusFetchedEvent(
                post = remotePostPayload.post,
                status = DRAFT_STATUS
        )
        val useCase = createUseCase(onPostStatusFetched)
        useCase.autoSavePostOrUpdateDraftAndAssertResult(remotePostPayload) { result ->
            assertThat(result).isInstanceOf(AutoSavePostIfNotDraftResult.PostIsDraftInRemote::class.java)
        }
    }

    @Test
    fun `error while auto-saving will result in PostAutoSaveFailed`() {
        val remotePostPayload = createRemotePostPayload()
        val onPostStatusFetched = createOnPostStatusFetchedEvent(
                post = remotePostPayload.post,
                status = PUBLISH_STATUS
        )
        val onPostChanged = createOnPostChangedEvent(
                remotePostPayload.post,
                error = PostError(UNKNOWN_POST, AUTO_SAVE_POST_ERROR_MESSAGE)
        )
        val useCase = createUseCase(onPostStatusFetched, onPostChanged)
        useCase.autoSavePostOrUpdateDraftAndAssertResult(remotePostPayload) { result ->
            assertThat(result).isInstanceOf(AutoSavePostIfNotDraftResult.PostAutoSaveFailed::class.java)
        }
    }

    @Test
    fun `post auto-saved`() {
        whenever(postStore.getPostByRemotePostId(any(), any())).thenReturn(PostModel())
        val remotePostPayload = createRemotePostPayload()
        val onPostStatusFetched = createOnPostStatusFetchedEvent(
                post = remotePostPayload.post,
                status = PUBLISH_STATUS
        )
        val onPostChanged = createOnPostChangedEvent(remotePostPayload.post)
        val useCase = createUseCase(onPostStatusFetched, onPostChanged)
        useCase.autoSavePostOrUpdateDraftAndAssertResult(remotePostPayload) { result ->
            assertThat(result).isInstanceOf(AutoSavePostIfNotDraftResult.PostAutoSaved::class.java)
        }
    }

    private fun createUseCase(
        onPostStatusFetched: OnPostStatusFetched,
        onPostChanged: OnPostChanged? = null
    ): AutoSavePostIfNotDraftUseCase {
        val dispatcher = mock<Dispatcher>()
        val useCase = AutoSavePostIfNotDraftUseCase(
                dispatcher = dispatcher,
                postStore = postStore,
                bgDispatcher = TEST_DISPATCHER
        )
        whenever(dispatcher.dispatch(argWhere<Action<Void>> { it.type == PostAction.FETCH_POST_STATUS })).then {
            useCase.onPostStatusFetched(onPostStatusFetched)
        }
        onPostChanged?.let { onPostChangedEvent ->
            whenever(dispatcher.dispatch(argWhere<Action<Void>> { it.type == PostAction.REMOTE_AUTO_SAVE_POST })).then {
                useCase.onPostChanged(onPostChangedEvent)
            }
        }
        return useCase
    }

    private fun createRemotePostPayload() = RemotePostPayload(PostModel(), SiteModel())

    /**
     * Since the `OnPostStatusFetched` is package-private we utilize reflection for the test. Without being able to
     * create `OnPostStatusFetched` event, we can't write most of the unit tests for `AutoSavePostIfNotDraftUseCase`.
     */
    private fun createOnPostStatusFetchedEvent(
        post: PostModel,
        status: String,
        error: PostError? = null
    ): OnPostStatusFetched {
        val constructor: Constructor<OnPostStatusFetched> = OnPostStatusFetched::class.java.getDeclaredConstructor(
                PostModel::class.java,
                String::class.java,
                PostError::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(post, status, error)
    }

    private fun createOnPostChangedEvent(post: PostModel, error: PostError? = null): OnPostChanged {
        val event = OnPostChanged(CauseOfOnPostChanged.RemoteAutoSavePost(post.id, post.remotePostId), 0)
        error?.let { event.error = it }
        return event
    }
}

private fun AutoSavePostIfNotDraftUseCase.autoSavePostOrUpdateDraftAndAssertResult(
    remotePostPayload: RemotePostPayload,
    assertionBlock: (AutoSavePostIfNotDraftResult) -> Unit
) {
    val countDownLatch = CountDownLatch(1)
    autoSavePostOrUpdateDraft(
            remotePostPayload,
            object : OnAutoSavePostIfNotDraftCallback {
                override fun handleAutoSavePostIfNotDraftResult(result: AutoSavePostIfNotDraftResult) {
                    assertionBlock(result)
                    countDownLatch.countDown()
                }
            })
    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
}
