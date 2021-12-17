package org.wordpress.android.ui.notifications

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NotificationListViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var notificationUseCase: NotificationsUseCase

    private lateinit var notificationListViewModel: NotificationListViewModel

    private val snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()
    private var holder: SnackbarMessageHolder? = null

    @Before
    fun setUp() {
        whenever(notificationUseCase.snackbarEvents).thenReturn(snackbarEvents)
        notificationListViewModel = NotificationListViewModel(
            networkUtilsWrapper = networkUtilsWrapper,
            notificationUseCase = notificationUseCase,
            bgDispatcher = TEST_DISPATCHER,
            mainDispatcher = TEST_DISPATCHER
        )
        notificationListViewModel.snackbarEvents.observeForever {
            it.applyIfNotHandled {
                holder = this
            }
        }
        notificationListViewModel.start()
    }

    @Test
    fun `User clicks onClickReadAllNotifications and internet is not available`() = test() {
        // GIVEN
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // WHEN
        notificationListViewModel.onClickReadAllNotifications()

        // THEN
        requireNotNull(holder).let {
            Assertions.assertThat(it.message).isEqualTo(UiString.UiStringRes(R.string.no_network_message))
        }
    }

    @Test
    fun `User clicks onClickReadAllNotifications and requestNotifications is called`() = test() {
        // GIVEN
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(notificationUseCase.uiStateFlow).thenReturn(MutableStateFlow(NotificationsUseCase.UiState.InitialState))

        // WHEN
        notificationListViewModel.onClickReadAllNotifications()

        // THEN
        verify(notificationUseCase).requestNotifications()
    }
}
