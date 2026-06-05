package org.wordpress.android.support.unified.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.support.unified.model.UnifiedAttachment
import org.wordpress.android.support.unified.model.UnifiedConversation
import org.wordpress.android.support.unified.model.UnifiedMessage
import org.wordpress.android.ui.compose.utils.markdownToAnnotatedString
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.JsonValue
import uniffi.wp_api.ReplyToUnifiedConversationParams
import uniffi.wp_api.UnifiedConversationSummary
import javax.inject.Inject
import javax.inject.Named

class UnifiedSupportRepository @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
    private val wpComApiClientProvider: WpComApiClientProvider,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    @Volatile
    private var accessToken: String? = null

    private val wpComApiClient: WpComApiClient by lazy {
        check(accessToken != null) { "Repository not initialized" }
        wpComApiClientProvider.getWpComApiClient(accessToken!!)
    }

    fun init(accessToken: String) {
        this.accessToken = accessToken
    }

    suspend fun loadConversations(): List<UnifiedConversation> = withContext(ioDispatcher) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.unifiedConversations().getUnifiedConversationList()
        }
        when (response) {
            is WpRequestResult.Success -> response.response.data.toUnifiedConversations()
            else -> {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading unified conversations: $response")
                emptyList()
            }
        }
    }

    suspend fun loadConversation(conversationId: Long): UnifiedConversation? = withContext(ioDispatcher) {
        val response = wpComApiClient.request { requestBuilder ->
            requestBuilder.unifiedConversations().getUnifiedConversation(
                conversationId = conversationId.toULong()
            )
        }
        when (response) {
            is WpRequestResult.Success -> response.response.data.toUnifiedConversation()
            else -> {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading unified conversation $conversationId: $response")
                null
            }
        }
    }

    suspend fun replyToConversation(
        conversationId: Long,
        message: String,
        attachments: List<String> = emptyList(),
    ): UnifiedConversation? =
        withContext(ioDispatcher) {
            val response = wpComApiClient.request { requestBuilder ->
                requestBuilder.unifiedConversations().replyToUnifiedConversation(
                    conversationId = conversationId.toULong(),
                    params = ReplyToUnifiedConversationParams(
                        message = message,
                        attachments = attachments,
                    )
                )
            }
            when (response) {
                is WpRequestResult.Success -> response.response.data.toUnifiedConversation()
                else -> {
                    appLogWrapper.e(
                        AppLog.T.SUPPORT,
                        "Error replying to unified conversation $conversationId: $response"
                    )
                    null
                }
            }
        }

    private fun List<UnifiedConversationSummary>.toUnifiedConversations(): List<UnifiedConversation> =
        map { summary ->
            UnifiedConversation(
                id = summary.id.toLong(),
                title = summary.title,
                description = summary.description,
                status = summary.status,
                canAcceptReply = summary.canAcceptReply,
                createdAt = summary.createdAt,
                updatedAt = summary.updatedAt,
                messages = emptyList()
            )
        }

    private fun uniffi.wp_api.UnifiedConversation.toUnifiedConversation(): UnifiedConversation =
        UnifiedConversation(
            id = id.toLong(),
            title = title,
            description = description,
            status = status,
            canAcceptReply = canAcceptReply,
            createdAt = createdAt,
            updatedAt = updatedAt,
            messages = messages.map { it.toUnifiedMessage() }
        )

    private fun uniffi.wp_api.UnifiedMessage.toUnifiedMessage(): UnifiedMessage =
        UnifiedMessage(
            id = id.toLong(),
            rawText = message,
            formattedText = markdownToAnnotatedString(message),
            authorRole = authorRole,
            authorName = authorName,
            createdAt = createdAt,
            attachments = attachments.map { it.toUnifiedAttachment() }
        )

    private fun uniffi.wp_api.UnifiedAttachment.toUnifiedAttachment(): UnifiedAttachment =
        UnifiedAttachment(
            id = id.toLong(),
            filename = filename,
            contentType = contentType,
            size = size.toLong(),
            url = url,
            botCitationScore = metadata.botCitationScore()
        )

    private fun Map<String, JsonValue>.botCitationScore(): Float? =
        when (val score = this[METADATA_KEY_SCORE]) {
            is JsonValue.Float -> score.v1.toFloat()
            is JsonValue.Int -> score.v1.toFloat()
            else -> null
        }

    companion object {
        private const val METADATA_KEY_SCORE = "score"
    }
}
