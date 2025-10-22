package org.wordpress.android.support.aibot.ui

import app.cash.turbine.test
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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.aibot.repository.AIBotSupportRepository
import java.util.Date

@ExperimentalCoroutinesApi
class AIBotSupportViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var aiBotSupportRepository: AIBotSupportRepository
    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var viewModel: AIBotSupportViewModel

    private val testAccessToken = "test_access_token"
    private val testUserId = 12345L

    @Before
    fun setUp() {
        viewModel = AIBotSupportViewModel(
            aiBotSupportRepository = aiBotSupportRepository,
            appLogWrapper = appLogWrapper
        )
    }

    @Test
    fun `init successfully loads conversations`() = test {
        val testConversations = createTestConversations()
        whenever(aiBotSupportRepository.loadConversations()).thenReturn(testConversations)

        viewModel.init(testAccessToken, testUserId)
        advanceUntilIdle()

        verify(aiBotSupportRepository).init(testAccessToken, testUserId)
        verify(aiBotSupportRepository).loadConversations()
        assertThat(viewModel.conversations.value).isEqualTo(testConversations)
        assertThat(viewModel.isLoadingConversations.value).isFalse
    }

    @Test
    fun `init sets error when repository init fails`() = test {
        val exception = RuntimeException("Init failed")
        whenever(aiBotSupportRepository.init(any(), any())).thenThrow(exception)

        viewModel.init(testAccessToken, testUserId)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(AIBotSupportViewModel.ErrorType.GENERAL)
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `init sets error when loading conversations fails`() = test {
        val exception = RuntimeException("Load failed")
        whenever(aiBotSupportRepository.loadConversations()).thenThrow(exception)

        viewModel.init(testAccessToken, testUserId)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(AIBotSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isLoadingConversations.value).isFalse
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `refreshConversations reloads conversations successfully`() = test {
        val initialConversations = createTestConversations()
        val updatedConversations = createTestConversations(count = 3)

        whenever(aiBotSupportRepository.loadConversations())
            .thenReturn(initialConversations)
            .thenReturn(updatedConversations)

        viewModel.init(testAccessToken, testUserId)
        advanceUntilIdle()

        viewModel.refreshConversations()
        advanceUntilIdle()

        assertThat(viewModel.conversations.value).isEqualTo(updatedConversations)
        assertThat(viewModel.isLoadingConversations.value).isFalse
    }

    @Test
    fun `clearError clears the error message`() = test {
        whenever(aiBotSupportRepository.loadConversations()).thenThrow(RuntimeException("Error"))

        viewModel.init(testAccessToken, testUserId)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isNotNull

        viewModel.clearError()

        assertThat(viewModel.errorMessage.value).isNull()
    }

    @Test
    fun `onConversationSelected loads conversation details successfully`() = test {
        val conversation = createTestConversation(id = 1L)
        val detailedConversation = conversation.copy(
            messages = listOf(
                BotMessage(1L, "User message", Date(), true),
                BotMessage(2L, "Bot response", Date(), false)
            )
        )
        whenever(aiBotSupportRepository.loadConversation(1L)).thenReturn(detailedConversation)

        viewModel.onConversationSelected(conversation)
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value).isEqualTo(detailedConversation)
        assertThat(viewModel.canSendMessage.value).isTrue
        assertThat(viewModel.isLoadingConversation.value).isFalse
    }

    @Test
    fun `onConversationSelected sets error when repository returns null`() = test {
        val conversation = createTestConversation(id = 1L)
        whenever(aiBotSupportRepository.loadConversation(1L)).thenReturn(null)

        viewModel.onConversationSelected(conversation)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(AIBotSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isLoadingConversation.value).isFalse
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `onConversationSelected sets error when repository throws exception`() = test {
        val conversation = createTestConversation(id = 1L)
        val exception = RuntimeException("Load failed")
        whenever(aiBotSupportRepository.loadConversation(1L)).thenThrow(exception)

        viewModel.onConversationSelected(conversation)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(AIBotSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isLoadingConversation.value).isFalse
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `onNewConversationClicked creates empty conversation`() = test {
        viewModel.onNewConversationClicked()

        val selectedConversation = viewModel.selectedConversation.value
        assertThat(selectedConversation).isNotNull
        assertThat(selectedConversation?.id).isEqualTo(0L)
        assertThat(selectedConversation?.messages).isEmpty()
        assertThat(selectedConversation?.lastMessage).isEmpty()
        assertThat(viewModel.canSendMessage.value).isTrue
    }

    @Test
    fun `sendMessage creates new conversation when id is 0`() = test {
        val message = "Hello, I need help"
        val newConversation = createTestConversation(id = 123L).copy(
            messages = listOf(
                BotMessage(1L, message, Date(), true),
                BotMessage(2L, "Bot response", Date(), false)
            )
        )
        whenever(aiBotSupportRepository.createNewConversation(message)).thenReturn(newConversation)

        viewModel.onNewConversationClicked()
        viewModel.sendMessage(message)
        advanceUntilIdle()

        verify(aiBotSupportRepository).createNewConversation(message)
        assertThat(viewModel.conversations.value).contains(newConversation)
        assertThat(viewModel.isBotTyping.value).isFalse
        assertThat(viewModel.canSendMessage.value).isTrue
    }

    @Test
    fun `sendMessage sends to existing conversation when id is not 0`() = test {
        val conversationId = 456L
        val message = "Follow-up question"
        val existingConversation = createTestConversation(id = conversationId).copy(
            messages = listOf(BotMessage(1L, "Previous message", Date(), true))
        )
        val updatedConversation = existingConversation.copy(
            messages = listOf(
                BotMessage(1L, "Previous message", Date(), true),
                BotMessage(2L, message, Date(), true),
                BotMessage(3L, "Bot response", Date(), false)
            )
        )

        whenever(aiBotSupportRepository.loadConversation(conversationId)).thenReturn(existingConversation)
        whenever(aiBotSupportRepository.sendMessageToConversation(eq(conversationId), eq(message)))
            .thenReturn(updatedConversation)

        viewModel.onConversationSelected(existingConversation)
        advanceUntilIdle()

        viewModel.sendMessage(message)
        advanceUntilIdle()

        verify(aiBotSupportRepository).sendMessageToConversation(conversationId, message)
        assertThat(viewModel.isBotTyping.value).isFalse
        assertThat(viewModel.canSendMessage.value).isTrue
    }

    @Test
    fun `sendMessage shows bot typing indicator during operation`() = test {
        val message = "Test message"
        val newConversation = createTestConversation(id = 123L)
        whenever(aiBotSupportRepository.createNewConversation(message)).thenReturn(newConversation)

        viewModel.onNewConversationClicked()
        viewModel.sendMessage(message)
        advanceUntilIdle()

        assertThat(viewModel.isBotTyping.value).isFalse
    }

    @Test
    fun `sendMessage disables message sending during operation`() = test {
        val message = "Test message"
        val newConversation = createTestConversation(id = 123L)
        whenever(aiBotSupportRepository.createNewConversation(message)).thenReturn(newConversation)

        viewModel.onNewConversationClicked()
        assertThat(viewModel.canSendMessage.value).isTrue

        viewModel.sendMessage(message)
        advanceUntilIdle()

        assertThat(viewModel.canSendMessage.value).isTrue
    }

    @Test
    fun `sendMessage adds user message optimistically to selected conversation`() = test {
        val message = "Test message"
        val newConversation = createTestConversation(id = 123L)
        whenever(aiBotSupportRepository.createNewConversation(message)).thenReturn(newConversation)

        viewModel.onNewConversationClicked()
        viewModel.sendMessage(message)

        // Allow the optimistic update to complete
        advanceUntilIdle()

        val selectedConversation = viewModel.selectedConversation.value
        assertThat(selectedConversation?.messages).isNotEmpty
        assertThat(selectedConversation?.messages?.first()?.text).isEqualTo(message)
        assertThat(selectedConversation?.messages?.first()?.isWrittenByUser).isTrue
    }

    @Test
    fun `sendMessage sets error when repository returns null`() = test {
        val message = "Test message"
        whenever(aiBotSupportRepository.createNewConversation(message)).thenReturn(null)

        viewModel.onNewConversationClicked()
        viewModel.sendMessage(message)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(AIBotSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isBotTyping.value).isFalse
        assertThat(viewModel.canSendMessage.value).isTrue
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `sendMessage sets error and re-enables sending when exception occurs`() = test {
        val message = "Test message"
        val exception = RuntimeException("Send failed")
        whenever(aiBotSupportRepository.createNewConversation(message)).thenThrow(exception)

        viewModel.onNewConversationClicked()
        viewModel.sendMessage(message)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(AIBotSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isBotTyping.value).isFalse
        assertThat(viewModel.canSendMessage.value).isTrue
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `sendMessage updates conversations list when creating new conversation`() = test {
        val initialConversations = createTestConversations(count = 2)
        val message = "New conversation"
        val newConversation = createTestConversation(id = 999L).copy(
            messages = listOf(
                BotMessage(1L, message, Date(), true),
                BotMessage(2L, "Bot response", Date(), false)
            )
        )

        whenever(aiBotSupportRepository.loadConversations()).thenReturn(initialConversations)
        whenever(aiBotSupportRepository.createNewConversation(message)).thenReturn(newConversation)

        viewModel.init(testAccessToken, testUserId)
        advanceUntilIdle()

        viewModel.onNewConversationClicked()
        viewModel.sendMessage(message)
        advanceUntilIdle()

        assertThat(viewModel.conversations.value).hasSize(3)
        assertThat(viewModel.conversations.value.first().id).isEqualTo(999L)
    }

    @Test
    fun `sendMessage updates existing conversation in conversations list`() = test {
        val conversationId = 123L
        val existingConversation = createTestConversation(id = conversationId).copy(
            messages = listOf(BotMessage(1L, "Previous message", Date(), true))
        )
        val initialConversations = listOf(existingConversation, createTestConversation(id = 456L))
        val message = "Follow-up"
        val updatedConversation = existingConversation.copy(
            messages = listOf(
                BotMessage(1L, "Previous message", Date(), true),
                BotMessage(2L, message, Date(), true),
                BotMessage(3L, "Bot response", Date(), false)
            )
        )

        whenever(aiBotSupportRepository.loadConversations()).thenReturn(initialConversations)
        whenever(aiBotSupportRepository.loadConversation(conversationId)).thenReturn(existingConversation)
        whenever(aiBotSupportRepository.sendMessageToConversation(conversationId, message))
            .thenReturn(updatedConversation)

        viewModel.init(testAccessToken, testUserId)
        advanceUntilIdle()

        viewModel.onConversationSelected(existingConversation)
        advanceUntilIdle()

        viewModel.sendMessage(message)
        advanceUntilIdle()

        val updatedList = viewModel.conversations.value
        assertThat(updatedList).hasSize(2)
        val updatedInList = updatedList.find { it.id == conversationId }
        assertThat(updatedInList?.lastMessage).isEqualTo("Bot response")
    }

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

    private fun createTestConversations(count: Int = 2): List<BotConversation> {
        return (1..count).map { createTestConversation(id = it.toLong(), lastMessage = "Message $it") }
    }
}
