package org.wordpress.android.support.feature.aibot.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.support.feature.aibot.model.BotConversation
import org.wordpress.android.support.feature.aibot.model.BotMessage
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.BotConversationSummary
import uniffi.wp_api.CreateBotConversationParams
import uniffi.wp_api.GetBotConversationParams
import uniffi.wp_api.ChatId
import uniffi.wp_api.SupportBotsRequestCreateBotConversationResponse
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import java.util.Date
import javax.inject.Inject
import kotlin.String

private const val BOT_ID = "jetpack-chat-mobile"

class AIBotSupportRepository @Inject constructor() {
    private var accessToken: String? = null
    private var userId: Long = 0

    private val wpComApiClient: WpComApiClient by lazy {
        if (accessToken == null || userId == 0L) {
            throw IllegalStateException("Repository not initialized")
        }
        WpComApiClient(
            WpAuthenticationProvider.staticWithAuth(WpAuthentication.Bearer(token = accessToken!!)
            )
        )
    }

    fun init(accessToken: String, userId: Long) {
        this.accessToken = accessToken
        this.userId = userId
    }

    suspend fun loadConversations(): List<BotConversation> = withContext(Dispatchers.IO) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.supportBots().getBotConverationList(BOT_ID)
        }
        when (response) {
            is WpRequestResult.Success -> {
                val conversations = response.response.data
                conversations.toBotConversations()
            }

            else -> {
                emptyList()
            }
        }
    }

    suspend fun loadConversation(chatId: Long): BotConversation? = withContext(Dispatchers.IO) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.supportBots().getBotConversation(
                botId = BOT_ID,
                chatId = chatId.toULong(),
                params = GetBotConversationParams()
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                val conversation = response.response.data
                conversation.toBotConversation()
            }

            else -> {
                null
            }
        }
    }

    suspend fun createNewConversation(message: String): BotConversation? = withContext(Dispatchers.IO) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.supportBots().createBotConversation(
                botId = BOT_ID,
                CreateBotConversationParams(
                    message = message,
                    userId = 0
                )
            )
        }

        when (response) {
            is WpRequestResult.Success -> {
                val conversation = response.response.data
                conversation.toBotConversation()
            }

            else -> {
                null
            }
        }
    }

    private fun List<BotConversationSummary>.toBotConversations(): List<BotConversation> =
        map { it.toBotConversation() }


    private fun BotConversationSummary.toBotConversation(): BotConversation =
        BotConversation (
            id = chatId.toLong(),
            createdAt = createdAt,
            mostRecentMessageDate = lastMessage.createdAt,
            lastMessage = lastMessage.content,
            messages = listOf()
        )

    private fun uniffi.wp_api.BotConversation.toBotConversation(): BotConversation =
        BotConversation (
            id = chatId.toLong(),
            createdAt = createdAt,
            mostRecentMessageDate = messages.last().createdAt,
            lastMessage = messages.last().content,
            messages = messages.map { it.toBotMessage() }
        )

    private fun uniffi.wp_api.BotMessage.toBotMessage(): BotMessage =
        BotMessage(
            id = messageId.toLong(),
            text = content,
            date = createdAt,
            isWrittenByUser = role.isEmpty()
        )
}
