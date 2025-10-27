package org.wordpress.android.support.he.repository

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SupportConversationSummary
import uniffi.wp_api.SupportMessageAuthor
import uniffi.wp_api.WpErrorCode
import java.util.Date

@ExperimentalCoroutinesApi
class HESupportRepositoryTest : BaseUnitTest() {
    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var repository: HESupportRepository

    private val testAccessToken = "test_access_token_123"

    @Before
    fun setUp() {
        whenever(wpComApiClientProvider.getWpComApiClient(testAccessToken))
            .thenReturn(wpComApiClient)

        repository = HESupportRepository(
            appLogWrapper = appLogWrapper,
            wpComApiClientProvider = wpComApiClientProvider,
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `init sets access token`() {
        // When
        repository.init(testAccessToken)

        // Then - No exception thrown when using the repository
        // The test passes if no exception is thrown
    }

    @Test
    fun `repository requires initialization before use`() = runTest {
        // Given - repository not initialized

        // When/Then - Should throw when trying to use without init
        try {
            repository.loadConversations()
            error("Expected exception was not thrown")
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("Repository not initialized")
        }
    }

    @Test
    fun `loadConversations returns list when request succeeds`() = runTest {
        // Given
        repository.init(testAccessToken)

        val conversationSummary1 = createSupportConversationSummary(1L)
        val conversationSummary2 = createSupportConversationSummary(2L)
        val conversationList = listOf(conversationSummary1, conversationSummary2)

        // Create the actual response object using the concrete type
        val mockHeaderMap = mock<uniffi.wp_api.WpNetworkHeaderMap>()
        val responseObject = uniffi.wp_api.SupportTicketsRequestGetSupportConversationListResponse(
            data = conversationList,
            headerMap = mockHeaderMap
        )

        val successResponse = WpRequestResult.Success(responseObject)

        @Suppress("UNCHECKED_CAST")
        whenever(
            wpComApiClient.request<List<SupportConversationSummary>>(any())
        ).thenReturn(successResponse as WpRequestResult<List<SupportConversationSummary>>)

        // When
        val result = repository.loadConversations()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(conversationSummary1.toSupportConversation())
        assertThat(result[1]).isEqualTo(conversationSummary2.toSupportConversation())
    }

    @Test
    fun `loadConversations returns empty list when request fails`() = runTest {
        // Given
        repository.init(testAccessToken)

        val errorResponse: WpRequestResult<List<SupportConversationSummary>> =
            WpRequestResult.UnknownError(500.toUShort(), "Internal Server Error")

        whenever(
            wpComApiClient.request<List<SupportConversationSummary>>(any())
        ).thenReturn(errorResponse)

        // When
        val result = repository.loadConversations()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `loadConversation returns conversation when request succeeds`() = runTest {
        // Given
        repository.init(testAccessToken)
        val conversationId = 123L

        val supportConversation = createSupportConversation(conversationId)

        // Create the actual response object using the concrete type
        val mockHeaderMap = mock<uniffi.wp_api.WpNetworkHeaderMap>()
        val responseObject = uniffi.wp_api.SupportTicketsRequestGetSupportConversationResponse(
            data = supportConversation,
            headerMap = mockHeaderMap
        )

        val successResponse = WpRequestResult.Success(responseObject)

        @Suppress("UNCHECKED_CAST")
        whenever(
            wpComApiClient.request<uniffi.wp_api.SupportConversation>(any())
        ).thenReturn(successResponse as WpRequestResult<uniffi.wp_api.SupportConversation>)

        // When
        val result = repository.loadConversation(conversationId)

        // Then
        assertThat(result).isEqualTo(supportConversation.toSupportConversation())
    }

    @Test
    fun `loadConversation returns null when request fails`() = runTest {
        // Given
        repository.init(testAccessToken)
        val conversationId = 123L

        val errorResponse: WpRequestResult<uniffi.wp_api.SupportConversation> =
            WpRequestResult.UnknownError(404.toUShort(), "Not Found")

        whenever(
            wpComApiClient.request<uniffi.wp_api.SupportConversation>(any())
        ).thenReturn(errorResponse)

        // When
        val result = repository.loadConversation(conversationId)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `createConversation returns success when request succeeds`() = runTest {
        // Given
        repository.init(testAccessToken)
        val subject = "Test Subject"
        val message = "Test Message"
        val tags = listOf("tag1", "tag2")
        val attachments = listOf("attachment1.jpg")

        val supportConversation = createSupportConversation(1L)

        // Create the actual response object using the concrete type
        val mockHeaderMap = mock<uniffi.wp_api.WpNetworkHeaderMap>()
        val responseObject = uniffi.wp_api.SupportTicketsRequestCreateSupportTicketResponse(
            data = supportConversation,
            headerMap = mockHeaderMap
        )

        val successResponse = WpRequestResult.Success(responseObject)

        @Suppress("UNCHECKED_CAST")
        whenever(
            wpComApiClient.request<uniffi.wp_api.SupportConversation>(any())
        ).thenReturn(successResponse as WpRequestResult<uniffi.wp_api.SupportConversation>)

        // When
        val result = repository.createConversation(
            subject = subject,
            message = message,
            tags = tags,
            attachments = attachments
        )

        // Then
        assertThat(result).isInstanceOf(CreateConversationResult.Success::class.java)
        val successResult = result as CreateConversationResult.Success
        assertThat(successResult.conversation).isEqualTo(supportConversation.toSupportConversation())
    }

    @Test
    fun `createConversation returns Forbidden when request fails with WpErrorCode-Forbidden`() = runTest {
        // Given
        repository.init(testAccessToken)

        val errorResponse: WpRequestResult<uniffi.wp_api.SupportConversation> =
            WpRequestResult.WpError(
                errorCode = WpErrorCode.Forbidden(),
                errorMessage = "Forbidden",
                statusCode = 403.toUShort(),
                response = ""
            )

        whenever(
            wpComApiClient.request<uniffi.wp_api.SupportConversation>(any())
        ).thenReturn(errorResponse)

        // When
        val result = repository.createConversation(
            subject = "Test",
            message = "Test",
            tags = emptyList(),
            attachments = emptyList()
        )

        // Then
        assertThat(result).isInstanceOf(CreateConversationResult.Error.Forbidden::class.java)
    }

    @Test
    fun `createConversation returns GeneralError when request fails with non-auth error`() = runTest {
        // Given
        repository.init(testAccessToken)

        val errorResponse: WpRequestResult<uniffi.wp_api.SupportConversation> =
            WpRequestResult.UnknownError(500.toUShort(), "Internal Server Error")

        whenever(
            wpComApiClient.request<uniffi.wp_api.SupportConversation>(any())
        ).thenReturn(errorResponse)

        // When
        val result = repository.createConversation(
            subject = "Test",
            message = "Test",
            tags = emptyList(),
            attachments = emptyList()
        )

        // Then
        assertThat(result).isInstanceOf(CreateConversationResult.Error.GeneralError::class.java)
    }

    @Test
    fun `addMessageToConversation returns success when request succeeds`() = runTest {
        // Given
        repository.init(testAccessToken)
        val conversationId = 456L
        val message = "Test Reply Message"
        val attachments = listOf("reply-attachment.jpg")

        val supportConversation = createSupportConversation(conversationId)

        // Create the actual response object using the concrete type
        val mockHeaderMap = mock<uniffi.wp_api.WpNetworkHeaderMap>()
        val responseObject = uniffi.wp_api.SupportTicketsRequestAddMessageToSupportConversationResponse(
            data = supportConversation,
            headerMap = mockHeaderMap
        )

        val successResponse = WpRequestResult.Success(responseObject)

        @Suppress("UNCHECKED_CAST")
        whenever(
            wpComApiClient.request<uniffi.wp_api.SupportConversation>(any())
        ).thenReturn(successResponse as WpRequestResult<uniffi.wp_api.SupportConversation>)

        // When
        val result = repository.addMessageToConversation(
            conversationId = conversationId,
            message = message,
            attachments = attachments
        )

        // Then
        assertThat(result).isInstanceOf(CreateConversationResult.Success::class.java)
        val successResult = result as CreateConversationResult.Success
        assertThat(successResult.conversation).isEqualTo(supportConversation.toSupportConversation())
    }

    @Test
    fun `addMessageToConversation returns GeneralError when request fails with non-auth error`() = runTest {
        // Given
        repository.init(testAccessToken)

        val errorResponse: WpRequestResult<uniffi.wp_api.SupportConversation> =
            WpRequestResult.UnknownError(500.toUShort(), "Internal Server Error")

        whenever(
            wpComApiClient.request<uniffi.wp_api.SupportConversation>(any())
        ).thenReturn(errorResponse)

        // When
        val result = repository.addMessageToConversation(
            conversationId = 456L,
            message = "Test",
            attachments = emptyList()
        )

        // Then
        assertThat(result).isInstanceOf(CreateConversationResult.Error.GeneralError::class.java)
    }

    private fun createSupportConversationSummary(id: Long): SupportConversationSummary =
        SupportConversationSummary(
            id = id.toULong(),
            title = "Test Conversation $id",
            description = "Description $id",
            status = "open",
            createdAt = Date(System.currentTimeMillis()),
            updatedAt = Date(System.currentTimeMillis())
        )

    private fun createSupportConversation(id: Long): uniffi.wp_api.SupportConversation =
        uniffi.wp_api.SupportConversation(
            id = id.toULong(),
            title = "Test Conversation $id",
            description = "Description $id",
            status = "open",
            createdAt = Date(System.currentTimeMillis()),
            updatedAt = Date(System.currentTimeMillis()),
            messages = emptyList()
        )

    private fun SupportConversationSummary.toSupportConversation(): SupportConversation =
        SupportConversation(
            id = id.toLong(),
            title = title,
            description = description,
            lastMessageSentAt = updatedAt,
            messages = emptyList()
        )

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
            rawText = this.content,
            formattedText = AnnotatedString(this.content),
            createdAt = this.createdAt,
            authorName = when (this.author) {
                is SupportMessageAuthor.User -> (this.author as SupportMessageAuthor.User).v1.displayName
                is SupportMessageAuthor.SupportAgent -> (this.author as SupportMessageAuthor.SupportAgent).v1.name
            },
            authorIsUser = this.author is SupportMessageAuthor.User
        )
}
