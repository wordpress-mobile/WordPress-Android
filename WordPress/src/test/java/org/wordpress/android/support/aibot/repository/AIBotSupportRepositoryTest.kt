package org.wordpress.android.support.aibot.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.BotConversation as ApiBotConversation
import uniffi.wp_api.BotConversationSummary
import uniffi.wp_api.BotMessage as ApiBotMessage
import uniffi.wp_api.BotMessageSummary
import uniffi.wp_api.MessageContext
import uniffi.wp_api.SupportBotsRequestAddMessageToBotConversationResponse
import uniffi.wp_api.SupportBotsRequestCreateBotConversationResponse
import uniffi.wp_api.SupportBotsRequestGetBotConversationResponse
import uniffi.wp_api.SupportBotsRequestGetBotConversationListResponse
import uniffi.wp_api.UserMessageContext
import uniffi.wp_api.UserPaidSupportEligibility
import uniffi.wp_api.WpNetworkHeaderMap
import java.util.Date

@ExperimentalCoroutinesApi
class AIBotSupportRepositoryTest : BaseUnitTest() {
    @Mock
    private lateinit var appLogWrapper: AppLogWrapper
    @Mock
    private lateinit var wpComApiClientProvider: WpComApiClientProvider
    @Mock
    private lateinit var wpComApiClient: WpComApiClient

    private lateinit var repository: AIBotSupportRepository

    private val testAccessToken = "test_access_token"
    private val testUserId = 12345L
    private val testChatId = 1L
    private val testMessage = "Test message"

    @Before
    fun setUp() = test {
        whenever(wpComApiClientProvider.getWpComApiClient(testAccessToken)).thenReturn(wpComApiClient)

        repository = AIBotSupportRepository(
            appLogWrapper = appLogWrapper,
            wpComApiClientProvider = wpComApiClientProvider,
            testDispatcher()
        )
    }

    @Test
    fun `loadConversations returns list of conversations on success`() = test {
        // Create a mock response object with the data property
        val testConversations = listOf(
            createTestBotConversationSummary(chatId = 1L, message = "First conversation"),
            createTestBotConversationSummary(chatId = 2L, message = "Second conversation")
        )

        // Create the actual response type
        val response = SupportBotsRequestGetBotConversationListResponse(
            data = testConversations,
            headerMap = mock<WpNetworkHeaderMap>()
        )

        val successResponse = WpRequestResult.Success(response = response)

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<SupportBotsRequestGetBotConversationListResponse>(any()))
            .thenReturn(successResponse)

        val result = repository.loadConversations()

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].lastMessage).isEqualTo("First conversation")
        assertThat(result[1].id).isEqualTo(2L)
        assertThat(result[1].lastMessage).isEqualTo("Second conversation")
    }

    @Test
    fun `loadConversation returns conversation on success`() = test {
        val testChatId = 123L

        val userMessage = createUserMessage(1L, "User message")
        val botMessage = createBotMessage(2L, "Bot response")

        val testMessages = listOf(userMessage, botMessage)

        val apiConversation = createApiBotConversation(
            chatId = testChatId,
            messages = testMessages
        )

        val response = SupportBotsRequestGetBotConversationResponse(
            data = apiConversation,
            headerMap = mock<WpNetworkHeaderMap>()
        )

        val successResponse = WpRequestResult.Success(response = response)

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<SupportBotsRequestGetBotConversationResponse>(any()))
            .thenReturn(successResponse)

        val result = repository.loadConversation(testChatId)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(testChatId)
        assertThat(result?.messages).hasSize(2)
        assertThat(result?.messages?.get(0)?.isWrittenByUser).isTrue
        assertThat(result?.messages?.get(0)?.rawText).isEqualTo("User message")
        assertThat(result?.messages?.get(1)?.isWrittenByUser).isFalse
        assertThat(result?.messages?.get(1)?.rawText).isEqualTo("Bot response")
        assertThat(result?.lastMessage).isEqualTo("Bot response")
    }

    @Test
    fun `loadConversations returns empty list on error`() = test {
        val errorResponse: WpRequestResult<Any> = WpRequestResult.UnknownError(
            statusCode = 500u.toUShort(),
            response = ""
        )

        repository.init(testAccessToken, testUserId)

        // Mock the suspend function call
        whenever(wpComApiClient.request<Any>(any())).thenReturn(errorResponse)

        val result = repository.loadConversations()

        assertThat(result).isEmpty()
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `loadConversation returns null on error`() = test {
        val errorResponse: WpRequestResult<Any> = WpRequestResult.UnknownError(
            statusCode = 404u.toUShort(),
            response = ""
        )

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<Any>(any())).thenReturn(errorResponse)

        val result = repository.loadConversation(testChatId)

        assertThat(result).isNull()
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `createNewConversation returns conversation on success`() = test {
        val newChatId = 456L
        val testMessage = "New conversation message"

        val userMessage = createUserMessage(messageId = 1L, content = testMessage)
        val botMessage = createBotMessage(messageId = 2L, content = "Bot welcome response")

        val testMessages = listOf(userMessage, botMessage)

        val apiConversation = createApiBotConversation(
            chatId = newChatId,
            messages = testMessages
        )

        val response = SupportBotsRequestCreateBotConversationResponse(
            data = apiConversation,
            headerMap = mock<WpNetworkHeaderMap>()
        )

        val successResponse = WpRequestResult.Success(response = response)

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<SupportBotsRequestCreateBotConversationResponse>(any()))
            .thenReturn(successResponse)

        val result = repository.createNewConversation(testMessage)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(newChatId)
        assertThat(result?.messages).hasSize(2)
        assertThat(result?.messages?.get(0)?.rawText).isEqualTo(testMessage)
        assertThat(result?.messages?.get(0)?.isWrittenByUser).isTrue
        assertThat(result?.messages?.get(1)?.rawText).isEqualTo("Bot welcome response")
        assertThat(result?.messages?.get(1)?.isWrittenByUser).isFalse
    }

    @Test
    fun `createNewConversation returns null on error`() = test {
        val errorResponse: WpRequestResult<Any> = WpRequestResult.UnknownError(
            statusCode = 500u.toUShort(),
            response = ""
        )

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<Any>(any())).thenReturn(errorResponse)

        val result = repository.createNewConversation(testMessage)

        assertThat(result).isNull()
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `sendMessageToConversation returns updated conversation on success`() = test {
        val existingChatId = 789L
        val newMessage = "Follow-up message"

        val previousUserMessage = createUserMessage(messageId = 1L, content = "Previous user message")
        val previousBotMessage = createBotMessage(messageId = 2L, content = "Previous bot response")
        val newUserMessage = createUserMessage(messageId = 3L, content = newMessage)
        val newBotMessage = createBotMessage(messageId = 4L, content = "Bot follow-up response")

        val testMessages = listOf(previousUserMessage, previousBotMessage, newUserMessage, newBotMessage)

        val apiConversation = createApiBotConversation(
            chatId = existingChatId,
            messages = testMessages
        )

        val response = SupportBotsRequestAddMessageToBotConversationResponse(
            data = apiConversation,
            headerMap = mock<WpNetworkHeaderMap>()
        )

        val successResponse = WpRequestResult.Success(response = response)

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<SupportBotsRequestAddMessageToBotConversationResponse>(any()))
            .thenReturn(successResponse)

        val result = repository.sendMessageToConversation(existingChatId, newMessage)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(existingChatId)
        assertThat(result?.messages).hasSize(4)
        assertThat(result?.messages?.get(2)?.rawText).isEqualTo(newMessage)
        assertThat(result?.messages?.get(2)?.isWrittenByUser).isTrue
        assertThat(result?.messages?.get(3)?.rawText).isEqualTo("Bot follow-up response")
        assertThat(result?.messages?.get(3)?.isWrittenByUser).isFalse
        assertThat(result?.lastMessage).isEqualTo("Bot follow-up response")
    }

    @Test
    fun `sendMessageToConversation returns null on error`() = test {
        val errorResponse: WpRequestResult<Any> = WpRequestResult.UnknownError(
            statusCode = 500u.toUShort(),
            response = ""
        )

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<Any>(any())).thenReturn(errorResponse)

        val result = repository.sendMessageToConversation(testChatId, testMessage)

        assertThat(result).isNull()
        verify(appLogWrapper).e(any(), any<String>())
    }

    private fun createTestBotConversationSummary(chatId: Long, message: String): BotConversationSummary {
        return BotConversationSummary(
            chatId = chatId.toULong(),
            createdAt = Date(),
            summaryMessage = BotMessageSummary(
                content = message,
                createdAt = Date(),
                role = "user"
            )
        )
    }

    private fun createUserMessage(messageId: Long, content: String): ApiBotMessage = ApiBotMessage(
        messageId = messageId.toULong(),
        content = content,
        role = "user",
        createdAt = Date(),
        context = MessageContext.User(
            UserMessageContext(
                selectedSiteId = null,
                wpcomUserId = 1L,
                wpcomUserName = "UserName",
                userPaidSupportEligibility = UserPaidSupportEligibility(
                    isUserEligible = true,
                    wapuuAssistantEnabled = true
                ),
                plan = null,
                products = listOf(),
                planInterface = false,
            )
        )
    )

    private fun createBotMessage(messageId: Long, content: String): ApiBotMessage = ApiBotMessage(
        messageId = messageId.toULong(),
        content = content,
        role = "bot",
        createdAt = Date(),
        context = MessageContext.User(
            UserMessageContext(
                selectedSiteId = null,
                wpcomUserId = 1L,
                wpcomUserName = "UserName",
                userPaidSupportEligibility = UserPaidSupportEligibility(
                    isUserEligible = true,
                    wapuuAssistantEnabled = true
                ),
                plan = null,
                products = listOf(),
                planInterface = false,
            )
        )
    )

    private fun createApiBotConversation(
        chatId: Long,
        messages: List<ApiBotMessage>
    ): ApiBotConversation = ApiBotConversation(
        chatId = chatId.toULong(),
        createdAt = Date(),
        messages = messages,
        wpcomUserId = testUserId,
        externalId = "",
        externalIdProvider = "",
        sessionId = "",
        botSlug = "test-bot",
        botVersion = "",
        zendeskTicketId = ""
    )
}
