package org.wordpress.android.support.he.ui

import android.app.Application
import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.he.model.MessageSendResult
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import org.wordpress.android.support.he.repository.CreateConversationResult
import org.wordpress.android.support.he.repository.HESupportRepository
import org.wordpress.android.support.he.util.TempAttachmentsUtil
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
class HESupportViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var heSupportRepository: HESupportRepository

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    private lateinit var tempAttachmentsUtil: TempAttachmentsUtil

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var contentResolver: ContentResolver

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
        runBlocking {
            whenever(tempAttachmentsUtil.createTempFilesFrom(any())).thenReturn(emptyList())
            whenever(tempAttachmentsUtil.removeTempFiles(any())).thenReturn(Unit)
        }

        // Mock ContentResolver to return file sizes
        whenever(application.contentResolver).thenReturn(contentResolver)
        val assetFileDescriptor = mock<AssetFileDescriptor>()
        whenever(assetFileDescriptor.length).thenReturn(1024L * 1024L) // 1MB by default
        whenever(contentResolver.openAssetFileDescriptor(any(), any())).thenReturn(assetFileDescriptor)

        viewModel = HESupportViewModel(
            heSupportRepository = heSupportRepository,
            ioDispatcher = UnconfinedTestDispatcher(),
            tempAttachmentsUtil = tempAttachmentsUtil,
            application = application,
            accountStore = accountStore,
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
            tags = emptyList()
        )
        advanceUntilIdle()

        assertThat(viewModel.isSendingMessage.value).isFalse
    }

    @Test
    fun `onSendNewConversation sets OFFLINE error when network is not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1")
        )
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.OFFLINE)
        assertThat(viewModel.isSendingMessage.value).isFalse
    }

    @Test
    fun `onSendNewConversation does not call repository when network is not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1")
        )
        advanceUntilIdle()

        verify(heSupportRepository, never()).createConversation(any(), any(), any(), any())
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
            message = "Test message"
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
            attachments = emptyList()
        )).thenReturn(CreateConversationResult.Success(updatedConversation))

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        verify(heSupportRepository).addMessageToConversation(
            conversationId = 1L,
            message = "Test message",
            attachments = emptyList()
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
            message = "Test message"
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
            message = "Test message"
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
            message = "Test message"
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
            message = "Test message"
        )
        advanceUntilIdle()

        assertThat(viewModel.isSendingMessage.value).isFalse
    }

    @Test
    fun `onAddMessageToConversation sets OFFLINE error when network is not available`() = test {
        val existingConversation = createTestConversation(1)
        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)

        // Network available when loading conversation
        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        // Network unavailable when sending message
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.OFFLINE)
        assertThat(viewModel.isSendingMessage.value).isFalse
    }

    @Test
    fun `onAddMessageToConversation sets Failure result when network is not available`() = test {
        val existingConversation = createTestConversation(1)
        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)

        // Network available when loading conversation
        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        // Network unavailable when sending message
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        assertThat(viewModel.messageSendResult.value).isEqualTo(MessageSendResult.Failure)
    }

    @Test
    fun `onAddMessageToConversation does not call repository when network is not available`() = test {
        val existingConversation = createTestConversation(1)
        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)

        // Network available when loading conversation
        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        // Network unavailable when sending message
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        verify(heSupportRepository, never()).addMessageToConversation(any(), any(), any())
    }

    // endregion

    // region Attachment management tests

    @Test
    fun `addAttachments adds URIs to attachment state`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()

        viewModel.addAttachments(listOf(uri1, uri2))
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1, uri2)
        assertThat(viewModel.attachmentState.value.rejectedUris).isEmpty()
    }

    @Test
    fun `addAttachments appends to existing attachments`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val uri3 = mock<Uri>()

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()
        viewModel.addAttachments(listOf(uri2, uri3))
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1, uri2, uri3)
    }

    @Test
    fun `removeAttachment removes specific URI from attachments list`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val uri3 = mock<Uri>()

        viewModel.addAttachments(listOf(uri1, uri2, uri3))
        advanceUntilIdle()
        viewModel.removeAttachment(uri2)
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1, uri3)
    }

    @Test
    fun `removeAttachment does nothing when URI not in list`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val uri3 = mock<Uri>()

        viewModel.addAttachments(listOf(uri1, uri2))
        advanceUntilIdle()
        viewModel.removeAttachment(uri3)
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1, uri2)
    }

    @Test
    fun `clearAttachments removes all attachments`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()

        viewModel.addAttachments(listOf(uri1, uri2))
        advanceUntilIdle()
        viewModel.clearAttachments()

        assertThat(viewModel.attachmentState.value.acceptedUris).isEmpty()
        assertThat(viewModel.attachmentState.value.rejectedUris).isEmpty()
    }

    @Test
    fun `attachments list is empty initially`() {
        assertThat(viewModel.attachmentState.value.acceptedUris).isEmpty()
    }

    @Test
    fun `addAttachments rejects file larger than 20MB`() = test {
        val uri1 = mock<Uri>()
        val assetFileDescriptor = mock<AssetFileDescriptor>()
        whenever(assetFileDescriptor.length).thenReturn(21L * 1024L * 1024L) // 21MB
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(assetFileDescriptor)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).isEmpty()
        assertThat(viewModel.attachmentState.value.rejectedUris).containsExactly(uri1)
        assertThat(viewModel.attachmentState.value.rejectedTotalSizeBytes).isEqualTo(21L * 1024L * 1024L)
    }

    @Test
    fun `addAttachments accepts file smaller than 20MB`() = test {
        val uri1 = mock<Uri>()
        val assetFileDescriptor = mock<AssetFileDescriptor>()
        whenever(assetFileDescriptor.length).thenReturn(10L * 1024L * 1024L) // 10MB
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(assetFileDescriptor)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1)
        assertThat(viewModel.attachmentState.value.rejectedUris).isEmpty()
        assertThat(viewModel.attachmentState.value.currentTotalSizeBytes).isEqualTo(10L * 1024L * 1024L)
    }

    @Test
    fun `addAttachments rejects files when total size exceeds 20MB`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val uri3 = mock<Uri>()
        val descriptor1 = mock<AssetFileDescriptor>()
        val descriptor2 = mock<AssetFileDescriptor>()
        val descriptor3 = mock<AssetFileDescriptor>()

        // Start with 12MB, then try to add 10MB (exceeds limit) and 3MB (fits)
        whenever(descriptor1.length).thenReturn(12L * 1024L * 1024L)
        whenever(descriptor2.length).thenReturn(10L * 1024L * 1024L)
        whenever(descriptor3.length).thenReturn(3L * 1024L * 1024L)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(descriptor1)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri2), any())).thenReturn(descriptor2)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri3), any())).thenReturn(descriptor3)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()
        viewModel.addAttachments(listOf(uri2, uri3))
        advanceUntilIdle()

        // uri1 (12MB) accepted, uri2 (10MB) rejected (12+10=22 exceeds 20MB), uri3 (3MB) accepted (12+3=15MB)
        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1, uri3)
        assertThat(viewModel.attachmentState.value.rejectedUris).containsExactly(uri2)
        assertThat(viewModel.attachmentState.value.currentTotalSizeBytes).isEqualTo(15L * 1024L * 1024L)
        assertThat(viewModel.attachmentState.value.rejectedTotalSizeBytes).isEqualTo(10L * 1024L * 1024L)
    }

    @Test
    fun `addAttachments accepts multiple files within total size limit`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val descriptor1 = mock<AssetFileDescriptor>()
        val descriptor2 = mock<AssetFileDescriptor>()

        // 12MB + 7MB = 19MB (within limit)
        whenever(descriptor1.length).thenReturn(12L * 1024L * 1024L)
        whenever(descriptor2.length).thenReturn(7L * 1024L * 1024L)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(descriptor1)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri2), any())).thenReturn(descriptor2)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()
        viewModel.addAttachments(listOf(uri2))
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1, uri2)
        assertThat(viewModel.attachmentState.value.rejectedUris).isEmpty()
        assertThat(viewModel.attachmentState.value.currentTotalSizeBytes).isEqualTo(19L * 1024L * 1024L)
    }

    @Test
    fun `addAttachments accepts file exactly at 20MB limit`() = test {
        val uri1 = mock<Uri>()
        val assetFileDescriptor = mock<AssetFileDescriptor>()
        whenever(assetFileDescriptor.length).thenReturn(20L * 1024L * 1024L) // Exactly 20MB
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(assetFileDescriptor)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1)
        assertThat(viewModel.attachmentState.value.rejectedUris).isEmpty()
        assertThat(viewModel.attachmentState.value.currentTotalSizeBytes).isEqualTo(20L * 1024L * 1024L)
    }

    @Test
    fun `addAttachments accepts files when total is exactly 20MB`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val descriptor1 = mock<AssetFileDescriptor>()
        val descriptor2 = mock<AssetFileDescriptor>()

        // 10MB + 10MB = exactly 20MB (at limit)
        whenever(descriptor1.length).thenReturn(10L * 1024L * 1024L)
        whenever(descriptor2.length).thenReturn(10L * 1024L * 1024L)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(descriptor1)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri2), any())).thenReturn(descriptor2)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()
        viewModel.addAttachments(listOf(uri2))
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1, uri2)
        assertThat(viewModel.attachmentState.value.rejectedUris).isEmpty()
        assertThat(viewModel.attachmentState.value.currentTotalSizeBytes).isEqualTo(20L * 1024L * 1024L)
    }

    @Test
    fun `addAttachments accepts file when size cannot be determined`() = test {
        val uri1 = mock<Uri>()
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(null)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()

        // File should be accepted since we can't determine size (fail open approach)
        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1)
        assertThat(viewModel.attachmentState.value.rejectedUris).isEmpty()
        assertThat(viewModel.attachmentState.value.currentTotalSizeBytes).isEqualTo(0L)
    }

    @Test
    fun `addAttachments rejects files that exceed total size even when individual files are valid`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val uri3 = mock<Uri>()
        val uri4 = mock<Uri>()
        val descriptor1 = mock<AssetFileDescriptor>()
        val descriptor2 = mock<AssetFileDescriptor>()
        val descriptor3 = mock<AssetFileDescriptor>()
        val descriptor4 = mock<AssetFileDescriptor>()

        // uri1: 5MB (accepted), uri2: 12MB (accepted), uri3: 8MB (rejected - would exceed total)
        whenever(descriptor1.length).thenReturn(5L * 1024L * 1024L)
        whenever(descriptor2.length).thenReturn(12L * 1024L * 1024L)
        whenever(descriptor3.length).thenReturn(8L * 1024L * 1024L)
        whenever(descriptor4.length).thenReturn(2L * 1024L * 1024L)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(descriptor1)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri2), any())).thenReturn(descriptor2)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri3), any())).thenReturn(descriptor3)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri4), any())).thenReturn(descriptor4)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()
        viewModel.addAttachments(listOf(uri2, uri3, uri4))
        advanceUntilIdle()

        // uri1 accepted (5MB), uri2 accepted (5+12=17 < 20), uri3 rejected (17+8=25 > 20), uri4 accepted (17+2=19 < 20)
        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1, uri2, uri4)
        assertThat(viewModel.attachmentState.value.rejectedUris).containsExactly(uri3)
        assertThat(viewModel.attachmentState.value.currentTotalSizeBytes).isEqualTo(19L * 1024L * 1024L)
        assertThat(viewModel.attachmentState.value.rejectedTotalSizeBytes).isEqualTo(8L * 1024L * 1024L)
    }

    // endregion

    // region Attachment integration tests

    @Test
    fun `onSendNewConversation sends attachments to repository`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val tempFile1 = java.io.File("/tmp/file1.jpg")
        val tempFile2 = java.io.File("/tmp/file2.jpg")
        val newConversation = createTestConversation(1)

        whenever(tempAttachmentsUtil.createTempFilesFrom(listOf(uri1, uri2)))
            .thenReturn(listOf(tempFile1, tempFile2))
        whenever(heSupportRepository.createConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = listOf(tempFile1.path, tempFile2.path)
        )).thenReturn(CreateConversationResult.Success(newConversation))

        viewModel.addAttachments(listOf(uri1, uri2))
        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
        )
        advanceUntilIdle()

        verify(tempAttachmentsUtil).createTempFilesFrom(listOf(uri1, uri2))
        verify(heSupportRepository).createConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
            attachments = listOf(tempFile1.path, tempFile2.path)
        )
        verify(tempAttachmentsUtil).removeTempFiles(listOf(tempFile1, tempFile2))
    }

    @Test
    fun `onSendNewConversation clears attachments after success`() = test {
        val uri1 = mock<Uri>()
        val newConversation = createTestConversation(1)

        whenever(heSupportRepository.createConversation(
            any(), any(), any(), any()
        )).thenReturn(CreateConversationResult.Success(newConversation))

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()
        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1)

        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
        )
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).isEmpty()
    }

    @Test
    fun `onSendNewConversation does not clear attachments on error`() = test {
        val uri1 = mock<Uri>()

        whenever(heSupportRepository.createConversation(
            any(), any(), any(), any()
        )).thenReturn(CreateConversationResult.Error.GeneralError)

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()

        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
        )
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1)
    }

    @Test
    fun `onSendNewConversation cleans up temp files even on error`() = test {
        val uri1 = mock<Uri>()
        val tempFile1 = java.io.File("/tmp/file1.jpg")

        whenever(tempAttachmentsUtil.createTempFilesFrom(listOf(uri1)))
            .thenReturn(listOf(tempFile1))
        whenever(heSupportRepository.createConversation(
            any(), any(), any(), any()
        )).thenReturn(CreateConversationResult.Error.GeneralError)

        viewModel.addAttachments(listOf(uri1))
        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
        )
        advanceUntilIdle()

        verify(tempAttachmentsUtil).removeTempFiles(listOf(tempFile1))
    }

    @Test
    fun `onAddMessageToConversation sends attachments to repository`() = test {
        val uri1 = mock<Uri>()
        val tempFile1 = java.io.File("/tmp/file1.jpg")
        val existingConversation = createTestConversation(1)
        val updatedConversation = createTestConversation(1).copy(
            messages = listOf(createTestMessage(1, "New message", true))
        )

        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(tempAttachmentsUtil.createTempFilesFrom(listOf(uri1)))
            .thenReturn(listOf(tempFile1))
        whenever(heSupportRepository.addMessageToConversation(
            conversationId = 1L,
            message = "Test message",
            attachments = listOf(tempFile1.path)
        )).thenReturn(CreateConversationResult.Success(updatedConversation))

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.addAttachments(listOf(uri1))
        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        verify(tempAttachmentsUtil).createTempFilesFrom(listOf(uri1))
        verify(heSupportRepository).addMessageToConversation(
            conversationId = 1L,
            message = "Test message",
            attachments = listOf(tempFile1.path)
        )
        verify(tempAttachmentsUtil).removeTempFiles(listOf(tempFile1))
    }

    @Test
    fun `onAddMessageToConversation clears attachments after success`() = test {
        val uri1 = mock<Uri>()
        val existingConversation = createTestConversation(1)
        val updatedConversation = createTestConversation(1)

        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(heSupportRepository.addMessageToConversation(
            any(), any(), any()
        )).thenReturn(CreateConversationResult.Success(updatedConversation))

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()
        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1)

        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).isEmpty()
    }

    @Test
    fun `onAddMessageToConversation does not clear attachments on error`() = test {
        val uri1 = mock<Uri>()
        val existingConversation = createTestConversation(1)

        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(heSupportRepository.addMessageToConversation(
            any(), any(), any()
        )).thenReturn(CreateConversationResult.Error.GeneralError)

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.addAttachments(listOf(uri1))
        advanceUntilIdle()

        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        assertThat(viewModel.attachmentState.value.acceptedUris).containsExactly(uri1)
    }

    @Test
    fun `onAddMessageToConversation cleans up temp files even on error`() = test {
        val uri1 = mock<Uri>()
        val tempFile1 = java.io.File("/tmp/file1.jpg")
        val existingConversation = createTestConversation(1)

        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(tempAttachmentsUtil.createTempFilesFrom(listOf(uri1)))
            .thenReturn(listOf(tempFile1))
        whenever(heSupportRepository.addMessageToConversation(
            any(), any(), any()
        )).thenReturn(CreateConversationResult.Error.GeneralError)

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.addAttachments(listOf(uri1))
        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        verify(tempAttachmentsUtil).removeTempFiles(listOf(tempFile1))
    }

    @Test
    fun `onSendNewConversation handles exception during temp file creation`() = test {
        val uri1 = mock<Uri>()

        whenever(tempAttachmentsUtil.createTempFilesFrom(listOf(uri1)))
            .thenThrow(RuntimeException("File creation failed"))

        viewModel.addAttachments(listOf(uri1))
        viewModel.onSendNewConversation(
            subject = "Test Subject",
            message = "Test Message",
            tags = listOf("tag1"),
        )
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        verify(appLogWrapper).e(any(), any())
    }

    @Test
    fun `onAddMessageToConversation handles exception during temp file creation`() = test {
        val uri1 = mock<Uri>()
        val existingConversation = createTestConversation(1)

        whenever(heSupportRepository.loadConversation(1L)).thenReturn(existingConversation)
        whenever(tempAttachmentsUtil.createTempFilesFrom(listOf(uri1)))
            .thenThrow(RuntimeException("File creation failed"))

        viewModel.onConversationClick(existingConversation)
        advanceUntilIdle()

        viewModel.addAttachments(listOf(uri1))
        viewModel.onAddMessageToConversation(
            message = "Test message"
        )
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        verify(appLogWrapper).e(any(), any())
    }

    // endregion

    // Helper functions
    private fun createTestConversation(
        id: Long,
        title: String = "Test Conversation",
        description: String = "Test Description",
        status: String = "open"
    ): SupportConversation {
        return SupportConversation(
            id = id,
            title = title,
            description = description,
            lastMessageSentAt = Date(),
            status = status,
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
            authorIsUser = authorIsUser,
            attachments = emptyList()
        )
    }
}
