package org.wordpress.android.ui.reader.subscription

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SubscriptionModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.fluxc.store.AccountStore.SubscriptionError
import org.wordpress.android.ui.reader.subscription.ReaderBlogSubscriptionUseCase.UpdateResult
import org.wordpress.android.util.NetworkUtilsWrapper

private const val BLOG_ID = 123L
private const val ERROR_MESSAGE = "Error"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderBlogSubscriptionUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var useCase: ReaderBlogSubscriptionUseCase

    private val successEvent = OnSubscriptionUpdated()
    private val failureEvent = OnSubscriptionUpdated().apply {
        error = SubscriptionError(ERROR_MESSAGE, ERROR_MESSAGE)
    }

    @Before
    fun setup() {
        useCase = ReaderBlogSubscriptionUseCase(
            dispatcher,
            accountStore,
            networkUtilsWrapper,
            testDispatcher()
        )

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    // region getSubscriptionForBlog
    @Test
    fun `getSubscriptionForBlog returns subscription when found`() = test {
        val subscription = SubscriptionModel().apply { blogId = BLOG_ID.toString() }
        whenever(accountStore.subscriptions).thenReturn(listOf(subscription))

        val result = useCase.getSubscriptionForBlog(BLOG_ID)

        assertThat(result).isEqualTo(subscription)
    }

    @Test
    fun `getSubscriptionForBlog returns null when not found`() = test {
        whenever(accountStore.subscriptions).thenReturn(emptyList())

        val result = useCase.getSubscriptionForBlog(BLOG_ID)

        assertThat(result).isNull()
    }
    // endregion

    // region updateNotifyPosts
    @Test
    fun `updateNotifyPosts returns NoNetwork when offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.updateNotifyPosts(BLOG_ID, true)

        assertThat(result).isEqualTo(UpdateResult.NoNetwork)
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `updateNotifyPosts dispatches notification post action`() = test {
        setupDispatcherForNotifyPosts(successEvent)

        useCase.updateNotifyPosts(BLOG_ID, true)

        verify(dispatcher).dispatch(argWhere<Action<*>> {
            it.type == AccountAction.UPDATE_SUBSCRIPTION_NOTIFICATION_POST
        })
    }

    @Test
    fun `updateNotifyPosts returns Success on successful update`() = test {
        setupDispatcherForNotifyPosts(successEvent)

        val result = useCase.updateNotifyPosts(BLOG_ID, true)

        assertThat(result).isEqualTo(UpdateResult.Success)
    }

    @Test
    fun `updateNotifyPosts returns Failure on error`() = test {
        setupDispatcherForNotifyPosts(failureEvent)

        val result = useCase.updateNotifyPosts(BLOG_ID, true)

        assertThat(result).isEqualTo(UpdateResult.Failure)
    }

    @Test
    fun `updateNotifyPosts refreshes subscriptions on success`() = test {
        setupDispatcherForNotifyPosts(successEvent)

        useCase.updateNotifyPosts(BLOG_ID, true)

        verify(dispatcher).dispatch(argWhere<Action<*>> {
            it.type == AccountAction.FETCH_SUBSCRIPTIONS
        })
    }

    @Test
    fun `updateNotifyPosts does not refresh subscriptions on failure`() = test {
        setupDispatcherForNotifyPosts(failureEvent)

        useCase.updateNotifyPosts(BLOG_ID, true)

        verify(dispatcher, never()).dispatch(argWhere<Action<*>> {
            it.type == AccountAction.FETCH_SUBSCRIPTIONS
        })
    }
    // endregion

    // region updateEmailPosts
    @Test
    fun `updateEmailPosts returns NoNetwork when offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.updateEmailPosts(BLOG_ID, true)

        assertThat(result).isEqualTo(UpdateResult.NoNetwork)
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `updateEmailPosts dispatches email post action`() = test {
        setupDispatcherForEmailPosts(successEvent)

        useCase.updateEmailPosts(BLOG_ID, true)

        verify(dispatcher).dispatch(argWhere<Action<*>> {
            it.type == AccountAction.UPDATE_SUBSCRIPTION_EMAIL_POST
        })
    }

    @Test
    fun `updateEmailPosts returns Success on successful update`() = test {
        setupDispatcherForEmailPosts(successEvent)

        val result = useCase.updateEmailPosts(BLOG_ID, true)

        assertThat(result).isEqualTo(UpdateResult.Success)
    }

    @Test
    fun `updateEmailPosts returns Failure on error`() = test {
        setupDispatcherForEmailPosts(failureEvent)

        val result = useCase.updateEmailPosts(BLOG_ID, true)

        assertThat(result).isInstanceOf(UpdateResult.Failure::class.java)
    }
    // endregion

    // region updateEmailComments
    @Test
    fun `updateEmailComments returns NoNetwork when offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.updateEmailComments(BLOG_ID, true)

        assertThat(result).isEqualTo(UpdateResult.NoNetwork)
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `updateEmailComments dispatches email comment action`() = test {
        setupDispatcherForEmailComments(successEvent)

        useCase.updateEmailComments(BLOG_ID, true)

        verify(dispatcher).dispatch(argWhere<Action<*>> {
            it.type == AccountAction.UPDATE_SUBSCRIPTION_EMAIL_COMMENT
        })
    }

    @Test
    fun `updateEmailComments returns Success on successful update`() = test {
        setupDispatcherForEmailComments(successEvent)

        val result = useCase.updateEmailComments(BLOG_ID, true)

        assertThat(result).isEqualTo(UpdateResult.Success)
    }

    @Test
    fun `updateEmailComments returns Failure on error`() = test {
        setupDispatcherForEmailComments(failureEvent)

        val result = useCase.updateEmailComments(BLOG_ID, true)

        assertThat(result).isInstanceOf(UpdateResult.Failure::class.java)
    }
    // endregion

    // region cleanup
    @Test
    fun `cleanup unregisters from dispatcher`() {
        useCase.cleanup()

        verify(dispatcher).unregister(useCase)
    }
    // endregion

    private fun setupDispatcherForNotifyPosts(event: OnSubscriptionUpdated) {
        whenever(dispatcher.dispatch(argWhere<Action<*>> {
            it.type == AccountAction.UPDATE_SUBSCRIPTION_NOTIFICATION_POST
        })).then {
            useCase.onSubscriptionUpdated(event)
        }
    }

    private fun setupDispatcherForEmailPosts(event: OnSubscriptionUpdated) {
        whenever(dispatcher.dispatch(argWhere<Action<*>> {
            it.type == AccountAction.UPDATE_SUBSCRIPTION_EMAIL_POST
        })).then {
            useCase.onSubscriptionUpdated(event)
        }
    }

    private fun setupDispatcherForEmailComments(event: OnSubscriptionUpdated) {
        whenever(dispatcher.dispatch(argWhere<Action<*>> {
            it.type == AccountAction.UPDATE_SUBSCRIPTION_EMAIL_COMMENT
        })).then {
            useCase.onSubscriptionUpdated(event)
        }
    }
}
