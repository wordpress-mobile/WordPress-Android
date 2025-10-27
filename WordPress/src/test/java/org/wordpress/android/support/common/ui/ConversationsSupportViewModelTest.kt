package org.wordpress.android.support.common.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.model.Conversation

@ExperimentalCoroutinesApi
class ConversationsSupportViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var viewModel: TestConversationsSupportViewModel

    private val testAccessToken = "test_access_token"
    private val testUserName = "Test User"
    private val testUserEmail = "test@example.com"
    private val testAvatarUrl = "https://example.com/avatar.jpg"

    @Before
    fun setUp() {
        val accountModel = AccountModel().apply {
            displayName = testUserName
            userName = "testuser"
            email = testUserEmail
            avatarUrl = testAvatarUrl
        }
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(testAccessToken)

        viewModel = TestConversationsSupportViewModel(
            accountStore = accountStore,
            appLogWrapper = appLogWrapper
        )
    }

    // Init Tests

    @Test
    fun `init successfully initializes repository and loads conversations`() = test {
        val testConversations = createTestConversations()
        viewModel.setConversationsToReturn(testConversations)

        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.initRepositoryCalled).isTrue
        assertThat(viewModel.conversations.value).isEqualTo(testConversations)
        assertThat(viewModel.isLoadingConversations.value).isFalse
        assertThat(viewModel.errorMessage.value).isNull()
    }

    @Test
    fun `init loads user info correctly`() = test {
        viewModel.init()
        advanceUntilIdle()

        val userInfo = viewModel.userInfo.value
        assertThat(userInfo.userName).isEqualTo(testUserName)
        assertThat(userInfo.userEmail).isEqualTo(testUserEmail)
        assertThat(userInfo.avatarUrl).isEqualTo(testAvatarUrl)
    }

    @Test
    fun `init uses userName when displayName is empty`() = test {
        val accountModel = AccountModel().apply {
            displayName = ""
            userName = "fallbackuser"
            email = testUserEmail
        }
        whenever(accountStore.account).thenReturn(accountModel)

        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.userInfo.value.userName).isEqualTo("fallbackuser")
    }

    @Test
    fun `init sets avatarUrl to null when empty`() = test {
        val accountModel = AccountModel().apply {
            displayName = testUserName
            userName = "testuser"
            email = testUserEmail
            avatarUrl = ""
        }
        whenever(accountStore.account).thenReturn(accountModel)

        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.userInfo.value.avatarUrl).isNull()
    }

    @Test
    fun `init sets FORBIDDEN error when access token is null`() = test {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.FORBIDDEN)
        assertThat(viewModel.initRepositoryCalled).isFalse
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `init sets GENERAL error when initialization throws exception`() = test {
        viewModel.setShouldThrowOnInit(true)

        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `init sets GENERAL error when loading conversations fails`() = test {
        viewModel.setShouldThrowOnGetConversations(true)

        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isLoadingConversations.value).isFalse
        verify(appLogWrapper).e(any(), any<String>())
    }

    // Refresh Conversations Tests

    @Test
    fun `refreshConversations reloads conversations successfully`() = test {
        val initialConversations = createTestConversations(count = 2)
        val updatedConversations = createTestConversations(count = 3)

        viewModel.setConversationsToReturn(initialConversations)
        viewModel.init()
        advanceUntilIdle()

        viewModel.setConversationsToReturn(updatedConversations)
        viewModel.refreshConversations()
        advanceUntilIdle()

        assertThat(viewModel.conversations.value).isEqualTo(updatedConversations)
        assertThat(viewModel.isLoadingConversations.value).isFalse
    }

    @Test
    fun `refreshConversations handles error gracefully`() = test {
        viewModel.init()
        advanceUntilIdle()

        viewModel.setShouldThrowOnGetConversations(true)
        viewModel.refreshConversations()
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isLoadingConversations.value).isFalse
    }

    // Clear Error Tests

    @Test
    fun `clearError clears the error message`() = test {
        viewModel.setShouldThrowOnGetConversations(true)
        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isNotNull

        viewModel.clearError()

        assertThat(viewModel.errorMessage.value).isNull()
    }

    // Navigation Tests

    @Test
    fun `onConversationClick emits NavigateToConversationDetail event`() = test {
        val conversation = createTestConversation(1)
        viewModel.setConversationToReturn(conversation)

        var emittedEvent: ConversationsSupportViewModel.NavigationEvent? = null
        val job = launch {
            viewModel.navigationEvents.collect { event ->
                emittedEvent = event
            }
        }

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertThat(emittedEvent).isEqualTo(ConversationsSupportViewModel.NavigationEvent.NavigateToConversationDetail)
        job.cancel()
    }

    @Test
    fun `onConversationClick sets selected conversation`() = test {
        val conversation = createTestConversation(1)
        viewModel.setConversationToReturn(conversation)

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value).isEqualTo(conversation)
    }

    @Test
    fun `onConversationClick sets loading state to false after loading`() = test {
        val conversation = createTestConversation(1)
        viewModel.setConversationToReturn(conversation)

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertThat(viewModel.isLoadingConversation.value).isFalse
    }

    @Test
    fun `onConversationClick refreshes conversation with updated data`() = test {
        val initialConversation = createTestConversation(1)
        val updatedConversation = createTestConversation(1)
        viewModel.setConversationToReturn(updatedConversation)

        viewModel.onConversationClick(initialConversation)
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value).isEqualTo(updatedConversation)
    }

    @Test
    fun `onConversationClick sets error when getConversation returns null`() = test {
        val conversation = createTestConversation(1)
        viewModel.setConversationToReturn(null)

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `onConversationClick sets error when getConversation throws exception`() = test {
        val conversation = createTestConversation(1)
        viewModel.setShouldThrowOnGetConversation(true)

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isLoadingConversation.value).isFalse
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `onBackFromDetailClick clears selected conversation`() = test {
        val conversation = createTestConversation(1)
        viewModel.setConversationToReturn(conversation)
        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value).isNotNull

        viewModel.onBackClick()
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value).isNull()
    }

    @Test
    fun `onBackFromDetailClick emits NavigateBack event`() = test {
        var emittedEvent: ConversationsSupportViewModel.NavigationEvent? = null
        val job = launch {
            viewModel.navigationEvents.collect { event ->
                emittedEvent = event
            }
        }

        viewModel.onBackClick()
        advanceUntilIdle()

        assertThat(emittedEvent).isEqualTo(ConversationsSupportViewModel.NavigationEvent.NavigateBack)
        job.cancel()
    }

    @Test
    fun `onCreateNewConversationClick emits NavigateToNewConversation event`() = test {
        var emittedEvent: ConversationsSupportViewModel.NavigationEvent? = null
        val job = launch {
            viewModel.navigationEvents.collect { event ->
                emittedEvent = event
            }
        }

        viewModel.onCreateNewConversationClick()
        advanceUntilIdle()

        assertThat(emittedEvent).isEqualTo(ConversationsSupportViewModel.NavigationEvent.NavigateToNewConversation)
        job.cancel()
    }

    @Test
    fun `setNewConversation sets selected conversation and emits navigation event`() = test {
        val conversation = createTestConversation(1)
        var emittedEvent: ConversationsSupportViewModel.NavigationEvent? = null
        val job = launch {
            viewModel.navigationEvents.collect { event ->
                emittedEvent = event
            }
        }

        viewModel.setNewConversation(conversation)
        advanceUntilIdle()

        assertThat(emittedEvent).isEqualTo(ConversationsSupportViewModel.NavigationEvent.NavigateToConversationDetail)
        assertThat(viewModel.selectedConversation.value).isEqualTo(conversation)
        job.cancel()
    }

    // Helper Methods

    private fun createTestConversations(count: Int = 2): List<TestConversation> {
        return (1..count).map { createTestConversation(it.toLong()) }
    }

    private fun createTestConversation(id: Long): TestConversation {
        return TestConversation(id)
    }
    // Test Implementation Classes

    private data class TestConversation(val id: Long) : Conversation {
        override fun getConversationId(): Long = id
    }

    private class TestConversationsSupportViewModel(
        accountStore: AccountStore,
        appLogWrapper: AppLogWrapper
    ) : ConversationsSupportViewModel<TestConversation>(accountStore, appLogWrapper) {
        var initRepositoryCalled = false
        private var shouldThrowOnInit = false
        private var shouldThrowOnGetConversations = false
        private var shouldThrowOnGetConversation = false
        private var conversationsToReturn: List<TestConversation> = emptyList()
        private var conversationToReturn: TestConversation? = null

        fun setShouldThrowOnInit(shouldThrow: Boolean) {
            shouldThrowOnInit = shouldThrow
        }

        fun setShouldThrowOnGetConversations(shouldThrow: Boolean) {
            shouldThrowOnGetConversations = shouldThrow
        }

        fun setShouldThrowOnGetConversation(shouldThrow: Boolean) {
            shouldThrowOnGetConversation = shouldThrow
        }

        fun setConversationsToReturn(conversations: List<TestConversation>) {
            conversationsToReturn = conversations
        }

        fun setConversationToReturn(conversation: TestConversation?) {
            conversationToReturn = conversation
        }

        @Suppress("TooGenericExceptionThrown")
        override fun initRepository(accessToken: String) {
            if (shouldThrowOnInit) {
                throw RuntimeException("Init failed")
            }
            initRepositoryCalled = true
        }

        @Suppress("TooGenericExceptionThrown")
        override suspend fun getConversations(): List<TestConversation> {
            if (shouldThrowOnGetConversations) {
                throw RuntimeException("Get conversations failed")
            }
            return conversationsToReturn
        }

        @Suppress("TooGenericExceptionThrown")
        override suspend fun getConversation(conversationId: Long): TestConversation? {
            if (shouldThrowOnGetConversation) {
                throw RuntimeException("Get conversation failed")
            }
            return conversationToReturn
        }
    }
}
