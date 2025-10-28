package org.wordpress.android.support.he.ui

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import org.wordpress.android.support.he.repository.CreateConversationResult
import org.wordpress.android.support.he.repository.HESupportRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class HESupportViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var heSupportRepository: HESupportRepository

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var viewModel: HESupportViewModel

    private val testAccessToken = "test_access_token"
    private val testUserId = 12345L
    private val testUserName = "Test User"
    private val testUserEmail = "test@example.com"
    private val testAvatarUrl = "https://example.com/avatar.jpg"

    @Before
    fun setUp() {
        val accountModel = AccountModel().apply {
            displayName = testUserName
            userName = "testuser"
            email = testUserEmail
            avatarUrl = testAvatarUrl
            userId = testUserId
        }
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(testAccessToken)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel = HESupportViewModel(
            accountStore = accountStore,
            heSupportRepository = heSupportRepository,
            appLogWrapper = appLogWrapper,
            networkUtilsWrapper = networkUtilsWrapper,
        )
    }

    // region StateFlow initial values tests

    @Test
    fun `isSendingNewConversation is false initially`() {
        assertThat(viewModel.isSendingMessage.value).isFalse
    }

    // endregion

    // region initRepository() override tests

    @Test
    fun `initRepository calls repository init with correct access token`() = test {
        whenever(heSupportRepository.loadConversations()).thenReturn(emptyList())

        viewModel.init()
        advanceUntilIdle()

        verify(heSupportRepository).init(testAccessToken)
    }

    // endregion

    // region getConversations() override tests

    @Test
    fun `getConversations calls repository loadConversations`() = test {
        val conversations = listOf(createTestConversation(1), createTestConversation(2))
        whenever(heSupportRepository.loadConversations()).thenReturn(conversations)

        viewModel.init()
        advanceUntilIdle()

        verify(heSupportRepository).loadConversations()
        assertThat(viewModel.conversations.value).isEqualTo(conversations)
    }

    // endregion

    // region onSendNewConversation() tests

    @Test
    fun `onSendNewConversation creates new conversation successfully`() = test {
        val newConversation = createTestConversation(1)
        whenever(heSupportRepository.createConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = emptyList()
        )).thenReturn(CreateConversationResult.Success(newConversation))

        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = emptyList()
        )
        advanceUntilIdle()

        verify(heSupportRepository).createConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = emptyList()
        )
    }

    @Test
    fun `onSendNewConversation sets FORBIDDEN error on Unauthorized result`() = test {
        whenever(heSupportRepository.createConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = emptyList()
        )).thenReturn(CreateConversationResult.Error.Forbidden)

        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = emptyList()
        )
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.FORBIDDEN)
        verify(appLogWrapper).e(any(), eq("Unauthorized error creating HE conversation"))
    }

    @Test
    fun `onSendNewConversation sets GENERAL error on GeneralError result`() = test {
        whenever(heSupportRepository.createConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = emptyList()
        )).thenReturn(CreateConversationResult.Error.GeneralError)

        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = emptyList()
        )
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        verify(appLogWrapper).e(any(), eq("General error creating HE conversation"))
    }

    @Test
    fun `onSendNewConversation resets isSendingNewConversation even when error occurs`() = test {
        whenever(heSupportRepository.createConversation(
            any(), any(), any(), any()
        )).thenReturn(CreateConversationResult.Error.GeneralError)

        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = emptyList(),
            attachments = emptyList()
        )
        advanceUntilIdle()

        assertThat(viewModel.isSendingMessage.value).isFalse
    }

    // endregion

    // region getConversation() override tests

    @Test
    fun `getConversation calls repository loadConversation with correct id`() = test {
        val conversation = createTestConversation(5)
        whenever(heSupportRepository.loadConversation(5L)).thenReturn(conversation)

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        verify(heSupportRepository).loadConversation(5L)
    }

    // endregion

    // region onAddMessageToConversation() tests

    @Test
    fun `onAddMessageToConversation does nothing when no conversation is selected`() = test {
        viewModel.onAddMessageToConversation(
            message = "Test message",
            attachments = emptyList()
        )
        advanceUntilIdle()

        verify(appLogWrapper).e(any(), eq("Error answering a conversation: no conversation selected"))
        assertThat(viewModel.isSendingMessage.value).isFalse
    }

    @Test
    fun `onAddMessageToConversation calls repository with correct parameters`() = test {
        val existingConversation = createTestConversation(1)
        val updatedConversation = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "New message", true))
        )
        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(heSupportRepository.addMessageToConversation(
            conversationId = 1L,
            message = "Test message",
            attachments = listOf("attachment1")
        )).thenReturn(CreateConversationResult.Success(updatedConversation))

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.onAddMessageToConversation(
            message = "Test message",
            attachments = listOf("attachment1")
        )
        advanceUntilIdle()

        verify(heSupportRepository).addMessageToConversation(
            conversationId = 1L,
            message = "Test message",
            attachments = listOf("attachment1")
        )
    }

    @Test
    fun `onAddMessageToConversation updates selectedConversation on success`() = test {
        val existingConversation = createTestConversation(1)
        val updatedConversation = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "New message", true))
        )
        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(heSupportRepository.addMessageToConversation(
            conversationId = 1L,
            message = "Test message",
            attachments = emptyList()
        )).thenReturn(CreateConversationResult.Success(updatedConversation))

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.onAddMessageToConversation(
            message = "Test message",
            attachments = emptyList()
        )
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value).isEqualTo(updatedConversation)
    }

    @Test
    fun `onAddMessageToConversation sets FORBIDDEN error on Unauthorized result`() = test {
        val existingConversation = createTestConversation(1)
        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(heSupportRepository.addMessageToConversation(
            conversationId = 1L,
            message = "Test message",
            attachments = emptyList()
        )).thenReturn(CreateConversationResult.Error.Forbidden)

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.onAddMessageToConversation(
            message = "Test message",
            attachments = emptyList()
        )
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.FORBIDDEN)
        verify(appLogWrapper).e(any(), eq("Unauthorized error adding message to HE conversation"))
    }

    @Test
    fun `onAddMessageToConversation sets GENERAL error on GeneralError result`() = test {
        val existingConversation = createTestConversation(1)
        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(heSupportRepository.addMessageToConversation(
            conversationId = 1L,
            message = "Test message",
            attachments = emptyList()
        )).thenReturn(CreateConversationResult.Error.GeneralError)

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.onAddMessageToConversation(
            message = "Test message",
            attachments = emptyList()
        )
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        verify(appLogWrapper).e(any(), eq("General error adding message to HE conversation"))
    }

    @Test
    fun `onAddMessageToConversation resets isSendingNewConversation even when error occurs`() = test {
        val existingConversation = createTestConversation(1)
        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(heSupportRepository.addMessageToConversation(
            any(), any(), any()
        )).thenReturn(CreateConversationResult.Error.GeneralError)

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.onAddMessageToConversation(
            message = "Test message",
            attachments = emptyList()
        )
        advanceUntilIdle()

        assertThat(viewModel.isSendingMessage.value).isFalse
    }

    // endregion

    // Helper functions
    private fun createTestConversation(
        id: Long,
        title: String = "Test Conversation",
        description: String = "Test Description"
    ): SupportConversation {
        return SupportConversation(
            id = id,
            title = title,
            description = description,
            lastMessageSentAt = Date(),
            messages = emptyList()
        )
    }

    private fun createTestMessage(
        id: Long,
        text: String,
        authorIsUser: Boolean
    ): SupportMessage {
        return SupportMessage(
            id = id,
            rawText = text,
            formattedText = AnnotatedString(text),
            createdAt = Date(),
            authorName = if (authorIsUser) "User" else "Support",
            authorIsUser = authorIsUser
        )
    }
}
