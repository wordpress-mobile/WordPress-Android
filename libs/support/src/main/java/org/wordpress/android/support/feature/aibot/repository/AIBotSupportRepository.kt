package org.wordpress.android.support.feature.aibot.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.support.feature.aibot.model.BotConversation
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.BotConversationSummary
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject

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

    suspend fun createNewConversation() {
//        val response = wpComApiClient.request { requestBuilder ->
//            requestBuilder.supportBots(). .createBotConversation(
//                    botId = BOT_ID,
//            CreateBotConversationParams(
//                message = "",
//                userId = 0
//            )
//            )
//        }
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
}
