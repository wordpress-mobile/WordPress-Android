package org.wordpress.android.ui.notifications

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import org.assertj.core.api.Assertions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString

@RunWith(MockitoJUnitRunner::class)
class NotificationsUseCaseTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private lateinit var notificationsUseCase: NotificationsUseCase

    private var holder: SnackbarMessageHolder? = null

    @Before
    fun setUp() {
        notificationsUseCase = NotificationsUseCase()
        notificationsUseCase.snackbarEvents.observeForever {
            it.applyIfNotHandled {
                holder = this
            }
        }
    }

    @Test
    fun `When NotificationUseCase starts, InitialState is the state of UiState`() = test() {
        // GIVEN
        // WHEN
        notificationsUseCase.uiStateFlow.test {
            // THEN
            assertEquals(awaitItem(), NotificationsUseCase.UiState.InitialState)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `When the user setAllNotesAsRead with no notifications, the snackbar is called`() = test() {
        // GIVEN
        // WHEN
        notificationsUseCase.setAllNotesAsRead(listOf())

        // THEN
        requireNotNull(holder).let {
            Assertions.assertThat(it.message)
                .isEqualTo(UiString.UiStringRes(R.string.mark_all_notifications_read_success))
        }

    }
}
