package org.wordpress.android.support.he.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.support.he.model.AttachmentType
import org.wordpress.android.support.he.model.SupportAttachment
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import org.wordpress.android.ui.compose.utils.markdownToAnnotatedString
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AddMessageToSupportConversationParams
import uniffi.wp_api.CreateSupportTicketParams
import uniffi.wp_api.SupportConversationSummary
import uniffi.wp_api.SupportMessageAuthor
import uniffi.wp_api.WpErrorCode
import javax.inject.Inject
import javax.inject.Named
import kotlin.String

private const val APPLICATION_ID = "jetpack"

sealed class CreateConversationResult {
    data class Success(val conversation: SupportConversation) : CreateConversationResult()

    sealed class Error : CreateConversationResult() {
        data object Forbidden : Error()
        data object GeneralError : Error()
    }
}

class HESupportRepository @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
    private val wpComApiClientProvider: WpComApiClientProvider,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Access token for API authentication.
     * Marked as @Volatile to ensure visibility across threads since this repository is accessed
     * from multiple coroutine contexts (main thread initialization, IO dispatcher for API calls).
     */
    @Volatile
    private var accessToken: String? = null

    private val wpComApiClient: WpComApiClient by lazy {
        check(accessToken != null) { "Repository not initialized" }
        wpComApiClientProvider.getWpComApiClient(accessToken!!)
    }

    fun init(accessToken: String) {
        this.accessToken = accessToken
    }

    suspend fun loadConversations(): List<SupportConversation> = withContext(ioDispatcher) {
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

    suspend fun loadConversation(conversationId: Long): SupportConversation? = withContext(ioDispatcher) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.supportTickets().getSupportConversation(
                conversationId = conversationId.toULong()
            )
        }

        when (response) {
            is WpRequestResult.Success -> {
                val conversation = response.response.data
                conversation.toSupportConversation()
            }

            else -> {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading support conversation: $response")
                null
            }
        }
    }

    suspend fun createConversation(
        subject: String,
        message: String,
        tags: List<String>,
        attachments: List<String>
    ): CreateConversationResult = withContext(ioDispatcher) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.supportTickets().createSupportTicket(
                CreateSupportTicketParams(
                    subject = subject,
                    message = message,
                    tags = tags,
                    attachments = attachments,
                    application = APPLICATION_ID, // Only jetpack is supported
                )
            )
        }

        when {
            response is WpRequestResult.Success -> {
                val conversation = response.response.data
                CreateConversationResult.Success(conversation.toSupportConversation())
            }

            response is WpRequestResult.WpError && response.errorCode is WpErrorCode.Forbidden -> {
                appLogWrapper.e(
                    AppLog.T.SUPPORT,
                    "Error creating support conversation - Forbidden: $response"
                )
                CreateConversationResult.Error.Forbidden
            }

            else -> {
                appLogWrapper.e(
                    AppLog.T.SUPPORT,
                    "Error creating support conversation: $response"
                )
                CreateConversationResult.Error.GeneralError
            }
        }
    }

    suspend fun addMessageToConversation(
        conversationId: Long,
        message: String,
        attachments: List<String>
    ): CreateConversationResult = withContext(ioDispatcher) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.supportTickets().addMessageToSupportConversation(
                conversationId =  conversationId.toULong(),
                params = AddMessageToSupportConversationParams(
                    message = message,
                    attachments = attachments,
                )
            )
        }

        when {
            response is WpRequestResult.Success -> {
                val conversation = response.response.data
                CreateConversationResult.Success(conversation.toSupportConversation())
            }

            response is WpRequestResult.WpError && response.errorCode is WpErrorCode.Forbidden -> {
                appLogWrapper.e(
                    AppLog.T.SUPPORT,
                    "Error adding message to support conversation - Forbidden: $response"
                )
                CreateConversationResult.Error.Forbidden
            }

            else -> {
                appLogWrapper.e(
                    AppLog.T.SUPPORT,
                    "Error adding message to support conversation: $response"
                )
                CreateConversationResult.Error.GeneralError
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
                status = it.status,
                messages = emptyList()
            )
        }

    private fun uniffi.wp_api.SupportConversation.toSupportConversation(): SupportConversation =
        SupportConversation(
            id = id.toLong(),
            title = title,
            description = description,
            lastMessageSentAt = updatedAt,
            status = status,
            messages = messages.map { it.toSupportMessage() }
        )

    private fun uniffi.wp_api.SupportMessage.toSupportMessage(): SupportMessage =
        SupportMessage(
            id = id.toLong(),
            rawText = content,
            formattedText = markdownToAnnotatedString(content),
            createdAt = createdAt,
            authorName = when (author) {
                is SupportMessageAuthor.User -> (author as SupportMessageAuthor.User).v1.displayName
                is SupportMessageAuthor.SupportAgent -> (author as SupportMessageAuthor.SupportAgent).v1.name
            },
            authorIsUser = authorIsCurrentUser,
            attachments = attachments.map { it.toSupportAttachment() }
        )

    private fun uniffi.wp_api.SupportAttachment.toSupportAttachment(): SupportAttachment =
        SupportAttachment(
            id = id.toLong(),
            filename = filename,
            url = url,
            type = determineAttachmentType(contentType)
        )

    private fun determineAttachmentType(contentType: String): AttachmentType {
        return when {
            contentType.startsWith("image/") -> AttachmentType.Image
            contentType.startsWith("video/") -> AttachmentType.Video
            else -> AttachmentType.Other
        }
    }
}
