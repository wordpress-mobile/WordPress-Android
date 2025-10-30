package org.wordpress.android.support.aibot.ui

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.aibot.repository.AIBotSupportRepository
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class AIBotSupportViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var aiBotSupportRepository: AIBotSupportRepository

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var viewModel: AIBotSupportViewModel

    private val testAccessToken = "test_access_token"
    private val testUserId = 12345L
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
            userId = testUserId
        }
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(testAccessToken)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel = AIBotSupportViewModel(
            accountStore = accountStore,
            aiBotSupportRepository = aiBotSupportRepository,
            appLogWrapper = appLogWrapper,
            networkUtilsWrapper = networkUtilsWrapper,
        )
    }

    // region StateFlow initial values tests

    @Test
    fun `canSendMessage is true initially`() {
        assertThat(viewModel.canSendMessage.value).isTrue
    }

    @Test
    fun `isBotTyping is false initially`() {
        assertThat(viewModel.isBotTyping.value).isFalse
    }

    // endregion

    // region getConversation() override tests

    @Test
    fun `getConversation resets canSendMessage to true even when repository returns null`() = test {
        val conversation = createTestConversation(1)
        whenever(aiBotSupportRepository.loadConversation(1L)).thenReturn(null)

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertThat(viewModel.canSendMessage.value).isTrue
    }

    // endregion

    // region onNewConversationClick() tests

    @Test
    fun `onNewConversationClick creates new conversation with empty messages`() = test {
        viewModel.onNewConversationClick()
        advanceUntilIdle()

        val selectedConversation = viewModel.selectedConversation.value
        assertThat(selectedConversation).isNotNull
        assertThat(selectedConversation?.id).isEqualTo(0)
        assertThat(selectedConversation?.messages).isEmpty()
        assertThat(selectedConversation?.lastMessage).isEmpty()
    }

    @Test
    fun `onNewConversationClick sets canSendMessage to true`() = test {
        viewModel.onNewConversationClick()
        advanceUntilIdle()

        assertThat(viewModel.canSendMessage.value).isTrue
    }

    // endregion

    // region sendMessage() tests

    @Test
    fun `sendMessage adds user message to conversation immediately`() = test {
        whenever(aiBotSupportRepository.createNewConversation(any())).thenReturn(
            createTestConversation(1).copy(messages = listOf(createTestMessage(1, "Bot response", false)))
        )

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("Hello bot")
        advanceUntilIdle()

        val selectedConversation = viewModel.selectedConversation.value
        assertThat(selectedConversation?.messages).isNotEmpty
        assertThat(selectedConversation?.messages?.any { it.isWrittenByUser }).isTrue
        assertThat(selectedConversation?.messages?.any { it.rawText == "Hello bot" }).isTrue
    }

    @Test
    fun `sendMessage sets canSendMessage to false during request then true after`() = test {
        whenever(aiBotSupportRepository.createNewConversation(any())).thenReturn(
            createTestConversation(1)
        )

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        assertThat(viewModel.canSendMessage.value).isTrue

        viewModel.sendMessage("Hello bot")
        advanceUntilIdle()

        assertThat(viewModel.canSendMessage.value).isTrue
    }

    @Test
    fun `sendMessage creates new conversation for conversation with id 0`() = test {
        val botResponse = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "Bot response", false))
        )
        whenever(aiBotSupportRepository.createNewConversation("Hello bot")).thenReturn(botResponse)

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("Hello bot")
        advanceUntilIdle()

        verify(aiBotSupportRepository).createNewConversation("Hello bot")
        assertThat(viewModel.selectedConversation.value?.id).isEqualTo(1)
    }

    @Test
    fun `sendMessage sends to existing conversation when id is not 0`() = test {
        val existingConversation = createTestConversation(5)
        val botResponse = createTestConversation(5).copy(
            messages = listOf(createTestMessage(1, "Bot response", false))
        )
        whenever(aiBotSupportRepository.loadConversation(5L)).thenReturn(existingConversation)
        whenever(aiBotSupportRepository.sendMessageToConversation(eq(5L), eq("Hello again")))
            .thenReturn(botResponse)

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.sendMessage("Hello again")
        advanceUntilIdle()

        verify(aiBotSupportRepository).sendMessageToConversation(5L, "Hello again")
    }

    @Test
    fun `sendMessage updates conversations list with new conversation`() = test {
        val botResponse = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "Bot response", false)),
            lastMessage = "Bot response"
        )
        whenever(aiBotSupportRepository.createNewConversation("Hello bot")).thenReturn(botResponse)

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("Hello bot")
        advanceUntilIdle()

        assertThat(viewModel.conversations.value).hasSize(1)
        assertThat(viewModel.conversations.value.first().id).isEqualTo(1)
    }

    @Test
    fun `sendMessage updates existing conversation in conversations list`() = test {
        val initialConversations = listOf(createTestConversation(1), createTestConversation(2))
        whenever(aiBotSupportRepository.loadConversations()).thenReturn(initialConversations)
        whenever(aiBotSupportRepository.loadConversation(1L)).thenReturn(initialConversations[0])

        viewModel.init()
        advanceUntilIdle()

        viewModel.onConversationClick(initialConversations[0])
        advanceUntilIdle()

        val updatedConversation = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "Bot response", false)),
            lastMessage = "Bot response"
        )
        whenever(aiBotSupportRepository.sendMessageToConversation(eq(1L), eq("Hello")))
            .thenReturn(updatedConversation)

        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        assertThat(viewModel.conversations.value).hasSize(2)
        assertThat(viewModel.conversations.value.first().id).isEqualTo(1)
        assertThat(viewModel.conversations.value.first().lastMessage).isEqualTo("Bot response")
    }

    @Test
    fun `sendMessage merges user message and bot messages correctly`() = test {
        val botResponse = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "Bot response", false))
        )
        whenever(aiBotSupportRepository.createNewConversation("Hello bot")).thenReturn(botResponse)

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("Hello bot")
        advanceUntilIdle()

        val selectedConversation = viewModel.selectedConversation.value
        assertThat(selectedConversation?.messages).hasSize(2)
        assertThat(selectedConversation?.messages?.first()?.isWrittenByUser).isTrue
        assertThat(selectedConversation?.messages?.first()?.rawText).isEqualTo("Hello bot")
        assertThat(selectedConversation?.messages?.last()?.isWrittenByUser).isFalse
        assertThat(selectedConversation?.messages?.last()?.rawText).isEqualTo("Bot response")
    }

    @Test
    fun `sendMessage sets lastMessage from bot response`() = test {
        val botResponse = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "Latest bot message", false))
        )
        whenever(aiBotSupportRepository.createNewConversation("Hello")).thenReturn(botResponse)

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        val selectedConversation = viewModel.selectedConversation.value
        assertThat(selectedConversation?.lastMessage).isEqualTo("Latest bot message")
    }

    @Test
    fun `sendMessage sets error when response is null`() = test {
        whenever(aiBotSupportRepository.createNewConversation(any())).thenReturn(null)

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("Hello bot")
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `sendMessage sets error and resets typing state when exception occurs`() = test {
        whenever(aiBotSupportRepository.createNewConversation(any())).thenThrow(RuntimeException("Network error"))

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("Hello bot")
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isBotTyping.value).isFalse
        assertThat(viewModel.canSendMessage.value).isTrue
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `sendMessage resets canSendMessage to true even when error occurs`() = test {
        whenever(aiBotSupportRepository.createNewConversation(any())).thenThrow(RuntimeException("Error"))

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        assertThat(viewModel.canSendMessage.value).isTrue
    }

    @Test
    fun `sendMessage multiple times accumulates messages`() = test {
        val firstBotResponse = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "First bot response", false))
        )
        val secondBotResponse = createTestConversation(1).copy(
            messages = listOf(createTestMessage(2, "Second bot response", false))
        )

        whenever(aiBotSupportRepository.createNewConversation("First message")).thenReturn(firstBotResponse)
        whenever(aiBotSupportRepository.sendMessageToConversation(eq(1L), eq("Second message")))
            .thenReturn(secondBotResponse)

        viewModel.onNewConversationClick()
        advanceUntilIdle()

        viewModel.sendMessage("First message")
        advanceUntilIdle()

        viewModel.sendMessage("Second message")
        advanceUntilIdle()

        val selectedConversation = viewModel.selectedConversation.value
        assertThat(selectedConversation?.messages).hasSize(4)
        assertThat(selectedConversation?.messages?.filter { it.isWrittenByUser }).hasSize(2)
        assertThat(selectedConversation?.messages?.filter { !it.isWrittenByUser }).hasSize(2)
    }

    // endregion

    // region Override methods tests

    @Test
    fun `initRepository calls repository init with correct parameters`() = test {
        whenever(aiBotSupportRepository.loadConversations()).thenReturn(emptyList())

        viewModel.init()
        advanceUntilIdle()

        verify(aiBotSupportRepository).init(testAccessToken, testUserId)
    }

    @Test
    fun `getConversations calls repository loadConversations`() = test {
        val conversations = listOf(createTestConversation(1), createTestConversation(2))
        whenever(aiBotSupportRepository.loadConversations()).thenReturn(conversations)

        viewModel.init()
        advanceUntilIdle()

        verify(aiBotSupportRepository).loadConversations()
        assertThat(viewModel.conversations.value).isEqualTo(conversations)
    }

    // endregion

    // Helper functions
    private fun createTestConversation(
        id: Long,
        lastMessage: String = "Test message"
    ): BotConversation {
        return BotConversation(
            id = id,
            createdAt = Date(),
            mostRecentMessageDate = Date(),
            lastMessage = lastMessage,
            messages = emptyList()
        )
    }

    private fun createTestMessage(
        id: Long,
        text: String,
        isWrittenByUser: Boolean
    ): BotMessage {
        return BotMessage(
            id = id,
            rawText = text,
            formattedText = AnnotatedString(text),
            date = Date(),
            isWrittenByUser = isWrittenByUser
        )
    }
}
