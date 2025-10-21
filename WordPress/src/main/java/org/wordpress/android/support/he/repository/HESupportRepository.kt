package org.wordpress.android.support.he.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AddMessageToBotConversationParams
import uniffi.wp_api.BotConversationSummary
import uniffi.wp_api.CreateBotConversationParams
import uniffi.wp_api.CreateSupportTicketParams
import uniffi.wp_api.GetBotConversationParams
import uniffi.wp_api.SupportConversationSummary
import javax.inject.Inject
import javax.inject.Named

private const val APPLICATION_ID = "jetpack"

class HESupportRepository @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
    private val wpComApiClientProvider: WpComApiClientProvider,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    private var accessToken: String? = null

    private val wpComApiClient: WpComApiClient by lazy {
        check(accessToken != null) { "Repository not initialized" }
        wpComApiClientProvider.getWpComApiClient(accessToken!!)
    }

    fun init(accessToken: String) {
        this.accessToken = accessToken
    }

    suspend fun loadConversations(subject: String, message: String, ): List<SupportConversation> = withContext(ioDispatcher) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.supportTickets().getSupportConversationList()
        }

        when (response) {
            is WpRequestResult.Success -> {
                val conversations = response.response.data
                conversations.toSupportConversations()
            }

            else -> {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading support conversations: $response")
                emptyList()
            }
        }
    }

    private fun List<SupportConversationSummary>.toSupportConversations(): List<SupportConversation> =
        map {
            SupportConversation(
                id = it.id.toLong(),
                title = it.title,
                description = it.description,
                lastMessageSentAt = it.updatedAt,
                messages = emptyList()
            )
        }
}
