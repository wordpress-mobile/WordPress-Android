package org.wordpress.android.support.aibot.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AddMessageToBotConversationParams
import uniffi.wp_api.BotConversationSummary
import uniffi.wp_api.CreateBotConversationParams
import uniffi.wp_api.GetBotConversationParams
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val BOT_ID = "jetpack-chat-mobile"
private const val READ_WRITE_TIMEOUT = 60L
private const val CONNECT_TIMEOUT = 30L

class AIBotSupportRepository @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
) {
    private var accessToken: String? = null
    private var userId: Long = 0

    private val wpComApiClient: WpComApiClient by lazy {
        check(accessToken != null || userId != 0L) { "Repository not initialized" }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()

        WpComApiClient(
            requestExecutor = WpRequestExecutor(httpClient = WpHttpClient.CustomOkHttpClient(okHttpClient)),
            authProvider = WpAuthenticationProvider.staticWithAuth(WpAuthentication.Bearer(token = accessToken!!)
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
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading conversations: $response")
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
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading conversation $chatId: $response")
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
                    userId = userId
                )
            )
        }

        when (response) {
            is WpRequestResult.Success -> {
                val conversation = response.response.data
                conversation.toBotConversation()
            }

            else -> {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error creating new conversation $response")
                null
            }
        }
    }

    suspend fun sendMessageToConversation(chatId: Long, message: String): BotConversation? =
        withContext(Dispatchers.IO) {
            val response = wpComApiClient.request { requestBuilder ->
                requestBuilder.supportBots().addMessageToBotConversation(
                    botId = BOT_ID,
                    chatId = chatId.toULong(),
                    params = AddMessageToBotConversationParams(
                        message = message,
                        context = mapOf()
                    )
                )
            }

            when (response) {
                is WpRequestResult.Success -> {
                    val conversation = response.response.data
                    conversation.toBotConversation()
                }

                else -> {
                    appLogWrapper.e(
                        AppLog.T.SUPPORT,
                        "Error sending message to conversation $chatId: $response"
                    )
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
            isWrittenByUser = role == "user"
        )
}
