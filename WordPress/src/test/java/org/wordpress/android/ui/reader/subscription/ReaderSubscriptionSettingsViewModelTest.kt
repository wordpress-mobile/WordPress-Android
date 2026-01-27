package org.wordpress.android.ui.reader.subscription

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SubscriptionModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.subscription.ReaderBlogSubscriptionUseCase.UpdateResult
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event

private const val BLOG_ID = 123L
private const val BLOG_NAME = "Test Blog"
private const val BLOG_URL = "https://test.wordpress.com"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderSubscriptionSettingsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var subscriptionUseCase: ReaderBlogSubscriptionUseCase

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModel: ReaderSubscriptionSettingsViewModel

    private var snackbarEvents: MutableList<Event<SnackbarMessageHolder>> = mutableListOf()

    @Before
    fun setup() {
        viewModel = ReaderSubscriptionSettingsViewModel(
            subscriptionUseCase,
            analyticsTrackerWrapper,
            testDispatcher()
        )

        viewModel.snackbarEvents.observeForever { snackbarEvents.add(it) }
    }

    // region start
    @Test
    fun `start initializes ui state with blog info`() = test {
        whenever(subscriptionUseCase.getSubscriptionForBlog(BLOG_ID)).thenReturn(null)

        viewModel.start(BLOG_ID, BLOG_NAME, BLOG_URL)

        val state = viewModel.uiState.first { it != null }
        assertThat(state?.blogId).isEqualTo(BLOG_ID)
        assertThat(state?.blogName).isEqualTo(BLOG_NAME)
        assertThat(state?.blogUrl).isEqualTo(BLOG_URL)
    }

    @Test
    fun `start initializes ui state with subscription settings when subscription exists`() = test {
        val subscription = SubscriptionModel().apply {
            blogId = BLOG_ID.toString()
            shouldNotifyPosts = true
            shouldEmailPosts = true
            shouldEmailComments = false
        }
        whenever(subscriptionUseCase.getSubscriptionForBlog(BLOG_ID)).thenReturn(subscription)

        viewModel.start(BLOG_ID, BLOG_NAME, BLOG_URL)

        val state = viewModel.uiState.first { it != null }
        assertThat(state?.notifyPostsEnabled).isTrue()
        assertThat(state?.emailPostsEnabled).isTrue()
        assertThat(state?.emailCommentsEnabled).isFalse()
    }

    @Test
    fun `start initializes ui state with default values when subscription not found`() = test {
        whenever(subscriptionUseCase.getSubscriptionForBlog(BLOG_ID)).thenReturn(null)

        viewModel.start(BLOG_ID, BLOG_NAME, BLOG_URL)

        val state = viewModel.uiState.first { it != null }
        assertThat(state?.notifyPostsEnabled).isFalse()
        assertThat(state?.emailPostsEnabled).isFalse()
        assertThat(state?.emailCommentsEnabled).isFalse()
    }
    // endregion

    // region onNotifyPostsToggled
    @Test
    fun `onNotifyPostsToggled sets loading state`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateNotifyPosts(BLOG_ID, true)).thenReturn(UpdateResult.Success)

        viewModel.onNotifyPostsToggled(true)

        // After completion, loading should be false
        val state = viewModel.uiState.value
        assertThat(state?.isLoading).isFalse()
    }

    @Test
    fun `onNotifyPostsToggled updates state on success`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateNotifyPosts(BLOG_ID, true)).thenReturn(UpdateResult.Success)

        viewModel.onNotifyPostsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.notifyPostsEnabled).isTrue()
        assertThat(state?.isLoading).isFalse()
    }

    @Test
    fun `onNotifyPostsToggled reverts state on failure`() = test {
        initializeViewModel(notifyPostsEnabled = false)
        whenever(subscriptionUseCase.updateNotifyPosts(BLOG_ID, true))
            .thenReturn(UpdateResult.Failure)

        viewModel.onNotifyPostsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.notifyPostsEnabled).isFalse()
    }

    @Test
    fun `onNotifyPostsToggled shows error snackbar on failure`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateNotifyPosts(BLOG_ID, true))
            .thenReturn(UpdateResult.Failure)

        viewModel.onNotifyPostsToggled(true)

        assertThat(snackbarEvents).hasSize(1)
        val message = snackbarEvents.first().peekContent().message
        assertThat(message).isEqualTo(UiStringRes(R.string.reader_subscription_settings_update_error))
    }

    @Test
    fun `onNotifyPostsToggled reverts state on no network`() = test {
        initializeViewModel(notifyPostsEnabled = false)
        whenever(subscriptionUseCase.updateNotifyPosts(BLOG_ID, true)).thenReturn(UpdateResult.NoNetwork)

        viewModel.onNotifyPostsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.notifyPostsEnabled).isFalse()
    }

    @Test
    fun `onNotifyPostsToggled shows no network snackbar on no network`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateNotifyPosts(BLOG_ID, true)).thenReturn(UpdateResult.NoNetwork)

        viewModel.onNotifyPostsToggled(true)

        assertThat(snackbarEvents).hasSize(1)
        val message = snackbarEvents.first().peekContent().message
        assertThat(message).isEqualTo(UiStringRes(R.string.no_network_message))
    }

    @Test
    fun `onNotifyPostsToggled is ignored while loading`() = test {
        initializeViewModelWithLoadingState()

        viewModel.onNotifyPostsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.notifyPostsEnabled).isFalse()
    }
    // endregion

    // region onEmailPostsToggled
    @Test
    fun `onEmailPostsToggled updates state on success`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateEmailPosts(BLOG_ID, true)).thenReturn(UpdateResult.Success)

        viewModel.onEmailPostsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.emailPostsEnabled).isTrue()
        assertThat(state?.isLoading).isFalse()
    }

    @Test
    fun `onEmailPostsToggled reverts state on failure`() = test {
        initializeViewModel(emailPostsEnabled = false)
        whenever(subscriptionUseCase.updateEmailPosts(BLOG_ID, true))
            .thenReturn(UpdateResult.Failure)

        viewModel.onEmailPostsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.emailPostsEnabled).isFalse()
    }

    @Test
    fun `onEmailPostsToggled shows error snackbar on failure`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateEmailPosts(BLOG_ID, true))
            .thenReturn(UpdateResult.Failure)

        viewModel.onEmailPostsToggled(true)

        assertThat(snackbarEvents).hasSize(1)
        val message = snackbarEvents.first().peekContent().message
        assertThat(message).isEqualTo(UiStringRes(R.string.reader_subscription_settings_update_error))
    }

    @Test
    fun `onEmailPostsToggled reverts state on no network`() = test {
        initializeViewModel(emailPostsEnabled = false)
        whenever(subscriptionUseCase.updateEmailPosts(BLOG_ID, true)).thenReturn(UpdateResult.NoNetwork)

        viewModel.onEmailPostsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.emailPostsEnabled).isFalse()
    }

    @Test
    fun `onEmailPostsToggled shows no network snackbar on no network`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateEmailPosts(BLOG_ID, true)).thenReturn(UpdateResult.NoNetwork)

        viewModel.onEmailPostsToggled(true)

        assertThat(snackbarEvents).hasSize(1)
        val message = snackbarEvents.first().peekContent().message
        assertThat(message).isEqualTo(UiStringRes(R.string.no_network_message))
    }

    @Test
    fun `onEmailPostsToggled is ignored while loading`() = test {
        initializeViewModelWithLoadingState()

        viewModel.onEmailPostsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.emailPostsEnabled).isFalse()
    }
    // endregion

    // region onEmailCommentsToggled
    @Test
    fun `onEmailCommentsToggled updates state on success`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateEmailComments(BLOG_ID, true)).thenReturn(UpdateResult.Success)

        viewModel.onEmailCommentsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.emailCommentsEnabled).isTrue()
        assertThat(state?.isLoading).isFalse()
    }

    @Test
    fun `onEmailCommentsToggled reverts state on failure`() = test {
        initializeViewModel(emailCommentsEnabled = false)
        whenever(subscriptionUseCase.updateEmailComments(BLOG_ID, true))
            .thenReturn(UpdateResult.Failure)

        viewModel.onEmailCommentsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.emailCommentsEnabled).isFalse()
    }

    @Test
    fun `onEmailCommentsToggled shows error snackbar on failure`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateEmailComments(BLOG_ID, true))
            .thenReturn(UpdateResult.Failure)

        viewModel.onEmailCommentsToggled(true)

        assertThat(snackbarEvents).hasSize(1)
        val message = snackbarEvents.first().peekContent().message
        assertThat(message).isEqualTo(UiStringRes(R.string.reader_subscription_settings_update_error))
    }

    @Test
    fun `onEmailCommentsToggled reverts state on no network`() = test {
        initializeViewModel(emailCommentsEnabled = false)
        whenever(subscriptionUseCase.updateEmailComments(BLOG_ID, true)).thenReturn(UpdateResult.NoNetwork)

        viewModel.onEmailCommentsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.emailCommentsEnabled).isFalse()
    }

    @Test
    fun `onEmailCommentsToggled shows no network snackbar on no network`() = test {
        initializeViewModel()
        whenever(subscriptionUseCase.updateEmailComments(BLOG_ID, true)).thenReturn(UpdateResult.NoNetwork)

        viewModel.onEmailCommentsToggled(true)

        assertThat(snackbarEvents).hasSize(1)
        val message = snackbarEvents.first().peekContent().message
        assertThat(message).isEqualTo(UiStringRes(R.string.no_network_message))
    }

    @Test
    fun `onEmailCommentsToggled is ignored while loading`() = test {
        initializeViewModelWithLoadingState()

        viewModel.onEmailCommentsToggled(true)

        val state = viewModel.uiState.value
        assertThat(state?.emailCommentsEnabled).isFalse()
    }
    // endregion

    // region cleanup
    @Test
    fun `onCleared calls useCase cleanup`() = test {
        // Access the onCleared method via reflection since it's protected
        val method = viewModel.javaClass.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)

        verify(subscriptionUseCase).cleanup()
    }
    // endregion

    private suspend fun initializeViewModel(
        notifyPostsEnabled: Boolean = false,
        emailPostsEnabled: Boolean = false,
        emailCommentsEnabled: Boolean = false
    ) {
        val subscription = SubscriptionModel().apply {
            blogId = BLOG_ID.toString()
            shouldNotifyPosts = notifyPostsEnabled
            shouldEmailPosts = emailPostsEnabled
            shouldEmailComments = emailCommentsEnabled
        }
        whenever(subscriptionUseCase.getSubscriptionForBlog(BLOG_ID)).thenReturn(subscription)
        viewModel.start(BLOG_ID, BLOG_NAME, BLOG_URL)
        // Wait for start to complete
        viewModel.uiState.first { it != null }
    }

    private suspend fun initializeViewModelWithLoadingState() {
        initializeViewModel()
        // Manually set the loading state to simulate an in-progress request
        val currentState = viewModel.uiState.value!!
        val field = viewModel.javaClass.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val mutableStateFlow = field.get(viewModel) as MutableStateFlow<ReaderSubscriptionSettingsUiState?>
        mutableStateFlow.value = currentState.copy(isLoading = true)
    }
}
