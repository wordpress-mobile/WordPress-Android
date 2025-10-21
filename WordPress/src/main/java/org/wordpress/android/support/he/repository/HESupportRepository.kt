package org.wordpress.android.support.he.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AddMessageToBotConversationParams
import uniffi.wp_api.BotConversationSummary
import uniffi.wp_api.CreateBotConversationParams
import uniffi.wp_api.CreateSupportTicketParams
import uniffi.wp_api.GetBotConversationParams
import uniffi.wp_api.SupportConversationSummary
import uniffi.wp_api.SupportMessageAuthor
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import kotlin.String

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

    suspend fun createConversation(subject: String, message: String, ): SupportConversation? = withContext(ioDispatcher) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.supportTickets().createSupportTicket(
                CreateSupportTicketParams(
                    subject = subject,
                    message = message,
                    application = APPLICATION_ID, // Only jetpack is supported
                )
            )
        }

        when (response) {
            is WpRequestResult.Success -> {
                val conversations = response.response.data
                conversations.toSupportConversation()
            }

            else -> {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error crreating support conversations: $response")
                null
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

    private fun uniffi.wp_api.SupportConversation.toSupportConversation(): SupportConversation =
        SupportConversation(
            id = this.id.toLong(),
            title = this.title,
            description = this.description,
            lastMessageSentAt = this.updatedAt,
            messages = this.messages.map { it.toSupportMessage() }
        )

    private fun uniffi.wp_api.SupportMessage.toSupportMessage(): SupportMessage =
        SupportMessage(
            id = this.id.toLong(),
            text = this.content,
            createdAt = this.createdAt,
            authorName = when (this.author) {
                is SupportMessageAuthor.User -> (this.author as SupportMessageAuthor.User).v1.displayName
                is SupportMessageAuthor.SupportAgent -> (this.author as SupportMessageAuthor.SupportAgent).v1.name
            },
            authorIsUser = this.author is SupportMessageAuthor.User
        )
}
