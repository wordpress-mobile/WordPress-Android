package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.fluxc.store.AccountStore.SubscriptionError
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Success
import org.wordpress.android.util.NetworkUtilsWrapper

private const val ERROR = "Error"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderSiteNotificationsUseCaseTest : BaseUnitTest() {
    lateinit var useCase: ReaderSiteNotificationsUseCase
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var readerTracker: ReaderTracker
    @Mock lateinit var readerBlogTableWrapper: ReaderBlogTableWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    private val event = OnSubscriptionUpdated()

    private lateinit var dispatchCaptor: KArgumentCaptor<Action<AddOrDeleteSubscriptionPayload>>

    @Before
    fun setup() {
        useCase = ReaderSiteNotificationsUseCase(
                dispatcher,
                readerTracker,
                readerBlogTableWrapper,
                networkUtilsWrapper
        )

        doNothing().whenever(readerTracker).trackBlog(any(), any(), any())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(dispatcher.dispatch(argWhere<Action<Void>> {
            it.type == AccountAction.UPDATE_SUBSCRIPTION_NOTIFICATION_POST
        })).then {
            useCase.onSubscriptionUpdated(
                    event
            )
        }
        dispatchCaptor = argumentCaptor()
    }

    @Test
    fun `toggling notification when notification is disabled sets notification enabled to true`() = test {
        // Arrange
        val blogId = 1L
        whenever(readerBlogTableWrapper.isNotificationsEnabled(blogId)).thenReturn(false)

        // Act
        useCase.toggleNotification(blogId, 1L)

        // Assert
        verify(readerBlogTableWrapper).setNotificationsEnabledByBlogId(blogId, true)
    }

    @Test
    fun `toggling notification when notification is enabled sets notification enabled to false`() = test {
        // Arrange
        val blogId = 1L
        whenever(readerBlogTableWrapper.isNotificationsEnabled(blogId)).thenReturn(true)

        // Act
        useCase.toggleNotification(blogId, 1L)

        // Assert
        verify(readerBlogTableWrapper).setNotificationsEnabledByBlogId(blogId, false)
    }

    @Test
    fun `toggling notification when notification is enabled triggers delete subscription action`() = test {
        // Arrange
        val blogId = 1L
        whenever(readerBlogTableWrapper.isNotificationsEnabled(blogId)).thenReturn(true)

        // Act
        useCase.toggleNotification(blogId, 1L)

        // Assert
        verify(dispatcher, times(2)).dispatch(dispatchCaptor.capture())
        Assert.assertEquals(dispatchCaptor.firstValue.payload.action, SubscriptionAction.DELETE)
    }

    @Test
    fun `toggling notification when notification is disabled triggers new subscription action`() = test {
        // Arrange
        val blogId = 1L
        whenever(readerBlogTableWrapper.isNotificationsEnabled(blogId)).thenReturn(false)

        // Act
        useCase.toggleNotification(blogId, 1L)

        // Assert
        verify(dispatcher, times(2)).dispatch(dispatchCaptor.capture())
        Assert.assertEquals(dispatchCaptor.firstValue.payload.action, SubscriptionAction.NEW)
    }

    @Test
    fun `fetch subscriptions action invoked if notification is subscribed successfully`() = test {
        // Arrange

        // Act
        useCase.toggleNotification(1L, 1L)

        // Assert
        verify(dispatcher, times(2)).dispatch(dispatchCaptor.capture())
        Assert.assertEquals(dispatchCaptor.lastValue.type, AccountAction.FETCH_SUBSCRIPTIONS)
    }

    @Test
    fun `Success returned if notification is subscribed successfully`() = test {
        // Act
        val result = useCase.toggleNotification(1L, 1L)

        // Assert
        assertThat(result).isEqualTo(Success)
    }

    @Test
    fun `RequestFailed returned if notification subscription failed with error`() = test {
        // Arrange
        val failedEvent = OnSubscriptionUpdated()
        failedEvent.error = SubscriptionError(ERROR, ERROR)

        whenever(dispatcher.dispatch(argWhere<Action<Void>> {
            it.type == AccountAction.UPDATE_SUBSCRIPTION_NOTIFICATION_POST
        })).then {
            useCase.onSubscriptionUpdated(
                    failedEvent
            )
        }

        // Act
        val result = useCase.toggleNotification(1L, 1L)

        // Assert
        assertThat(result).isEqualTo(RequestFailed)
    }

    @Test
    fun `NoNetwork returned on toggling notification when no network is available`() = test {
        // Arrange
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // Act
        val result = useCase.toggleNotification(1L, 1L)

        // Assert
        assertThat(result).isEqualTo(NoNetwork)
    }
}
