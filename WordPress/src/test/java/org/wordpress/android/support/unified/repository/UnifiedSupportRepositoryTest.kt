package org.wordpress.android.support.unified.repository

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
import org.wordpress.android.support.unified.model.UnifiedConversation
import org.wordpress.android.support.unified.model.UnifiedMessage
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.BotConversation as ApiBotConversation
import uniffi.wp_api.BotMessage as ApiBotMessage
import uniffi.wp_api.MessageContext
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SupportBotsRequestCreateBotConversationResponse
import uniffi.wp_api.UserMessageContext
import uniffi.wp_api.UserPaidSupportEligibility
import uniffi.wp_api.WpNetworkHeaderMap
import java.util.Date

@ExperimentalCoroutinesApi
class UnifiedSupportRepositoryTest : BaseUnitTest() {
    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    private lateinit var wpComApiClient: WpComApiClient

    private lateinit var repository: UnifiedSupportRepository

    private val testAccessToken = "test_access_token"
    private val testUserId = 12345L

    @Before
    fun setUp() = test {
        whenever(wpComApiClientProvider.getWpComApiClient(testAccessToken)).thenReturn(wpComApiClient)

        repository = UnifiedSupportRepository(
            appLogWrapper = appLogWrapper,
            wpComApiClientProvider = wpComApiClientProvider,
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `createNewBotConversation returns bot conversation on success`() = test {
        val newChatId = 456L
        val testMessage = "New conversation message"

        val apiConversation = createApiBotConversation(
            chatId = newChatId,
            messages = listOf(
                createApiBotMessage(messageId = 1L, content = testMessage, role = "user"),
                createApiBotMessage(messageId = 2L, content = "Bot welcome response", role = "bot")
            )
        )

        val response = SupportBotsRequestCreateBotConversationResponse(
            data = apiConversation,
            headerMap = mock<WpNetworkHeaderMap>()
        )

        val successResponse = WpRequestResult.Success(response = response)

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<SupportBotsRequestCreateBotConversationResponse>(any()))
            .thenReturn(successResponse)

        val result = repository.createNewBotConversation(testMessage)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(newChatId)
        assertThat(result?.status).isEqualTo(UnifiedConversation.STATUS_BOT)
        assertThat(result?.isBot).isTrue
        assertThat(result?.canAcceptReply).isTrue
        assertThat(result?.description).isEqualTo("Bot welcome response")
        assertThat(result?.messages).hasSize(2)
        assertThat(result?.messages?.get(0)?.formattedText?.text).isEqualTo(testMessage)
        assertThat(result?.messages?.get(0)?.authorRole).isEqualTo(UnifiedMessage.AUTHOR_ROLE_USER)
        assertThat(result?.messages?.get(0)?.isUser).isTrue
        assertThat(result?.messages?.get(1)?.formattedText?.text).isEqualTo("Bot welcome response")
        assertThat(result?.messages?.get(1)?.authorRole).isEqualTo(UnifiedMessage.AUTHOR_ROLE_BOT)
        assertThat(result?.messages?.get(1)?.isUser).isFalse
    }

    @Test
    fun `createNewBotConversation returns null on error`() = test {
        val errorResponse: WpRequestResult<Any> = WpRequestResult.UnknownError(
            statusCode = 500u,
            response = "",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        repository.init(testAccessToken, testUserId)
        whenever(wpComApiClient.request<Any>(any())).thenReturn(errorResponse)

        val result = repository.createNewBotConversation("Test message")

        assertThat(result).isNull()
        verify(appLogWrapper).e(any(), any<String>())
    }

    private fun createApiBotMessage(messageId: Long, content: String, role: String): ApiBotMessage = ApiBotMessage(
        messageId = messageId.toULong(),
        content = content,
        role = role,
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
