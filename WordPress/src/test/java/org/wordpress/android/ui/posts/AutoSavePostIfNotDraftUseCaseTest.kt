package org.wordpress.android.ui.posts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostStatusFetched
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.UNKNOWN_POST
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftResult
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftUseCase
import org.wordpress.android.ui.uploads.OnAutoSavePostIfNotDraftCallback
import java.lang.reflect.Constructor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.TEST_DISPATCHER
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val FETCH_POST_STATUS_ERROR_MESSAGE = "FETCH_POST_STATUS_ERROR_MESSAGE"
private const val DRAFT_STATUS = "DRAFT"

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AutoSavePostIfNotDraftUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var postStore: PostStore

    @Test
    fun `error while fetching post status will result in FetchPostStatusFailed`() {
        val useCase = createUseCase()
        val error = PostError(UNKNOWN_POST, FETCH_POST_STATUS_ERROR_MESSAGE)
        val remotePostPayload = createRemotePostPayload()
        whenever(dispatcher.dispatch(any())).then {
            useCase.onPostStatusFetched(
                    createOnPostStatusFetchedEvent(
                            post = remotePostPayload.post,
                            status = DRAFT_STATUS,
                            error = error
                    )
            )
        }
        useCase.autoSavePostOrUpdateDraftAndAssertResult(remotePostPayload) { result ->
            assertThat(result).isInstanceOf(AutoSavePostIfNotDraftResult.FetchPostStatusFailed::class.java)
        }
    }

    private fun createUseCase() = AutoSavePostIfNotDraftUseCase(
            dispatcher = dispatcher,
            postStore = postStore,
            bgDispatcher = TEST_DISPATCHER
    )

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
