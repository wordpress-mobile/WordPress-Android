package org.wordpress.android.ui.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState
import org.wordpress.android.ui.utils.UiString.UiStringText

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderFollowCommentsHandlerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var readerCommentsFollowUseCase: ReaderCommentsFollowUseCase

    private lateinit var followCommentsHandler: ReaderFollowCommentsHandler
    private val blogId = 100L
    private val postId = 1000L
    private var uiState: FollowCommentsState? = null
    private var holder: SnackbarMessageHolder? = null

    @Before
    fun setup() {
        followCommentsHandler = ReaderFollowCommentsHandler(readerCommentsFollowUseCase, TEST_DISPATCHER)
    }

    @Test
    fun `handleFollowCommentsStatusRequest collects expected state`() = test {
        val userMessage = UiStringText("handleFollowCommentsStatusRequest")
        val state = FollowCommentsState.FollowStateChanged(
                blogId = blogId,
                postId = postId,
                isFollowing = true,
                isReceivingNotifications = false,
                isInit = false,
                userMessage = userMessage
        )

        whenever(readerCommentsFollowUseCase.getMySubscriptionToPost(blogId, postId, false)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        followCommentsHandler.handleFollowCommentsStatusRequest(blogId, postId, false)

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(userMessage)
        }
    }

    @Test
    fun `handleFollowCommentsClicked collects expected state`() = test {
        val userMessage = UiStringText("handleFollowCommentsClicked")
        val state = FollowCommentsState.FollowStateChanged(
                blogId = blogId,
                postId = postId,
                isFollowing = true,
                isReceivingNotifications = false,
                isInit = false,
                userMessage = userMessage
        )

        whenever(readerCommentsFollowUseCase.setMySubscriptionToPost(blogId, postId, true)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        followCommentsHandler.handleFollowCommentsClicked(blogId, postId, true, null)

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(userMessage)
        }
    }

    private fun setupObservers() {
        uiState = null

        followCommentsHandler.followStatusUpdate.observeForever {
            uiState = it
        }

        holder = null
        followCommentsHandler.snackbarEvents.observeForever { event ->
            event.applyIfNotHandled {
                holder = this
            }
        }
    }
}
