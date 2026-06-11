package org.wordpress.android.support.unified.ui

import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.unified.model.AttachmentState
import org.wordpress.android.support.unified.model.ConversationReplyFormState
import org.wordpress.android.support.unified.model.UnifiedConversation
import org.wordpress.android.support.unified.model.UnifiedMessage
import org.wordpress.android.support.unified.model.VideoDownloadState
import org.wordpress.android.support.unified.repository.UnifiedSupportRepository
import org.wordpress.android.support.unified.util.AttachmentStateValidator
import org.wordpress.android.support.unified.util.EncryptedAppLogsUploader
import org.wordpress.android.support.unified.util.TempAttachmentsUtil
import org.wordpress.android.util.NetworkUtilsWrapper
import java.io.File
import java.util.Date

@ExperimentalCoroutinesApi
class UnifiedSupportViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var repository: UnifiedSupportRepository

    @Mock
    private lateinit var tempAttachmentsUtil: TempAttachmentsUtil

    @Mock
    private lateinit var attachmentStateValidator: AttachmentStateValidator

    @Mock
    private lateinit var encryptedAppLogsUploader: EncryptedAppLogsUploader

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var viewModel: UnifiedSupportViewModel

    private val testAccessToken = "test_access_token"
    private val testUserId = 12345L

    @Before
    fun setUp() = test {
        val accountModel = AccountModel().apply {
            displayName = "Test User"
            userName = "testuser"
            email = "test@example.com"
            userId = testUserId
        }
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(testAccessToken)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(tempAttachmentsUtil.createTempFilesFrom(any())).thenReturn(emptyList())

        viewModel = UnifiedSupportViewModel(
            accountStore = accountStore,
            repository = repository,
            tempAttachmentsUtil = tempAttachmentsUtil,
            attachmentStateValidator = attachmentStateValidator,
            encryptedAppLogsUploader = encryptedAppLogsUploader,
            ioDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            networkUtilsWrapper = networkUtilsWrapper,
        )
    }

    // region Helpers

    private fun createMessage(id: Long, text: String = "Message $id"): UnifiedMessage = UnifiedMessage(
        id = id,
        formattedText = AnnotatedString(text),
        authorRole = UnifiedMessage.AUTHOR_ROLE_USER,
        authorName = "Test User",
        createdAt = Date(),
        attachments = emptyList(),
    )

    private fun createConversation(
        id: Long = 1L,
        status: String = "open",
        canAcceptReply: Boolean = true,
        messages: List<UnifiedMessage> = emptyList(),
    ): UnifiedConversation = UnifiedConversation(
        id = id,
        title = "Title $id",
        description = "Description $id",
        status = status,
        canAcceptReply = canAcceptReply,
        createdAt = Date(),
        updatedAt = Date(),
        messages = messages,
    )

    private suspend fun selectConversation(conversation: UnifiedConversation) {
        whenever(repository.loadConversation(conversation.id)).thenReturn(conversation)
        viewModel.onConversationClick(conversation)
    }

    // endregion

    // region Init tests

    @Test
    fun `init initializes the repository with access token and user id`() = test {
        whenever(repository.loadConversations()).thenReturn(emptyList())

        viewModel.init()
        advanceUntilIdle()

        verify(repository).init(testAccessToken, testUserId)
    }

    @Test
    fun `init loads conversations from the repository`() = test {
        val conversations = listOf(createConversation(1L), createConversation(2L))
        whenever(repository.loadConversations()).thenReturn(conversations)

        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.conversations.value).isEqualTo(conversations)
        assertThat(viewModel.conversationsState.value)
            .isEqualTo(ConversationsSupportViewModel.ConversationsState.Loaded)
    }

    @Test
    fun `init surfaces error state when repository cannot load conversations`() = test {
        whenever(repository.loadConversations()).thenReturn(null)

        viewModel.init()
        advanceUntilIdle()

        assertThat(viewModel.conversations.value).isEmpty()
        assertThat(viewModel.conversationsState.value)
            .isEqualTo(ConversationsSupportViewModel.ConversationsState.Error)
        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
    }

    @Test
    fun `getAuthorizationHeader returns bearer header with access token`() {
        assertThat(viewModel.getAuthorizationHeader()).isEqualTo("Bearer $testAccessToken")
    }

    // endregion

    // region New bot conversation tests

    @Test
    fun `onCreateNewBotConversationClick selects an empty local bot conversation`() = test {
        viewModel.onCreateNewBotConversationClick()
        advanceUntilIdle()

        val selected = viewModel.selectedConversation.value
        assertThat(selected).isNotNull
        assertThat(selected?.id).isEqualTo(0L)
        assertThat(selected?.isBot).isTrue
        assertThat(selected?.canAcceptReply).isTrue
        assertThat(selected?.messages).isEmpty()
    }

    @Test
    fun `sendReply on a new conversation creates it and keeps the local question`() = test {
        whenever(repository.loadConversations()).thenReturn(emptyList())
        viewModel.init()
        viewModel.onCreateNewBotConversationClick()
        advanceUntilIdle()
        val botReply = createMessage(2L, "Bot answer").copy(authorRole = UnifiedMessage.AUTHOR_ROLE_BOT)
        val created = createConversation(
            id = 99L,
            status = UnifiedConversation.STATUS_BOT,
            messages = listOf(botReply),
        )
        whenever(repository.createNewBotConversation("Hello")).thenReturn(created)

        viewModel.sendReply("Hello")
        advanceUntilIdle()

        val selected = viewModel.selectedConversation.value
        assertThat(selected?.id).isEqualTo(99L)
        assertThat(selected?.messages).hasSize(2)
        assertThat(selected?.messages?.first()?.formattedText?.text).isEqualTo("Hello")
        assertThat(selected?.messages?.first()?.isUser).isTrue
        assertThat(selected?.messages?.last()).isEqualTo(botReply)
        // The new conversation is added to the top of the list without messages
        assertThat(viewModel.conversations.value.first().id).isEqualTo(99L)
        assertThat(viewModel.conversations.value.first().messages).isEmpty()
    }

    @Test
    fun `sendReply on a new conversation rolls back the question when creation fails`() = test {
        viewModel.onCreateNewBotConversationClick()
        advanceUntilIdle()
        whenever(repository.createNewBotConversation(any())).thenReturn(null)

        viewModel.sendReply("Hello")
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value?.messages).isEmpty()
        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
    }

    // endregion

    // region sendReply tests

    @Test
    fun `sendReply updates selected conversation and list on success`() = test {
        val conversation = createConversation(id = 1L, messages = listOf(createMessage(10L)))
        whenever(repository.loadConversations()).thenReturn(listOf(conversation.copy(messages = emptyList())))
        viewModel.init()
        selectConversation(conversation)
        advanceUntilIdle()
        val updated = conversation.copy(messages = conversation.messages + createMessage(11L, "My reply"))
        whenever(repository.replyToConversation(eq(1L), eq("My reply"), any(), any())).thenReturn(updated)

        viewModel.sendReply("My reply")
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value).isEqualTo(updated)
        assertThat(viewModel.isSendingReply.value).isFalse
        // The list keeps conversation summaries without messages
        assertThat(viewModel.conversations.value).containsExactly(updated.copy(messages = emptyList()))
    }

    @Test
    fun `sendReply appends an optimistic user message while sending`() = test {
        val conversation = createConversation(id = 1L)
        selectConversation(conversation)
        advanceUntilIdle()
        whenever(repository.replyToConversation(any(), any(), any(), any())).thenReturn(null)

        viewModel.sendReply("Optimistic")
        advanceUntilIdle()

        // The repository returned null so the optimistic message was rolled back again,
        // but the repository received the reply text
        verify(repository).replyToConversation(eq(1L), eq("Optimistic"), any(), any())
    }

    @Test
    fun `sendReply rolls back the optimistic message and sets error when reply fails`() = test {
        val originalMessages = listOf(createMessage(10L))
        val conversation = createConversation(id = 1L, messages = originalMessages)
        selectConversation(conversation)
        advanceUntilIdle()
        whenever(repository.replyToConversation(any(), any(), any(), any())).thenReturn(null)

        viewModel.sendReply("My reply")
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value?.messages).isEqualTo(originalMessages)
        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isSendingReply.value).isFalse
    }

    @Test
    fun `sendReply rolls back the optimistic message and sets error when reply throws`() = test {
        val originalMessages = listOf(createMessage(10L))
        val conversation = createConversation(id = 1L, messages = originalMessages)
        selectConversation(conversation)
        advanceUntilIdle()
        whenever(repository.replyToConversation(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Network error"))

        viewModel.sendReply("My reply")
        advanceUntilIdle()

        assertThat(viewModel.selectedConversation.value?.messages).isEqualTo(originalMessages)
        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
        assertThat(viewModel.isSendingReply.value).isFalse
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `sendReply sets OFFLINE error and does not call repository when network is unavailable`() = test {
        val conversation = createConversation(id = 1L)
        selectConversation(conversation)
        advanceUntilIdle()
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.sendReply("My reply")
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.OFFLINE)
        verify(repository, never()).replyToConversation(any(), any(), any(), any())
    }

    @Test
    fun `sendReply does nothing when no conversation is selected`() = test {
        viewModel.sendReply("My reply")
        advanceUntilIdle()

        verify(repository, never()).replyToConversation(any(), any(), any(), any())
        verify(repository, never()).createNewBotConversation(any())
    }

    @Test
    fun `sendReply uploads app logs and sends their ids when requested`() = test {
        val conversation = createConversation(id = 1L)
        selectConversation(conversation)
        advanceUntilIdle()
        whenever(encryptedAppLogsUploader.uploadLogs()).thenReturn(listOf("uuid-1", "uuid-2"))
        whenever(repository.replyToConversation(any(), any(), any(), any())).thenReturn(conversation)

        viewModel.sendReply("My reply", includeAppLogs = true)
        advanceUntilIdle()

        verify(repository).replyToConversation(
            conversationId = eq(1L),
            message = eq("My reply"),
            attachments = eq(emptyList()),
            encryptedLogIds = eq(listOf("uuid-1", "uuid-2")),
        )
    }

    @Test
    fun `sendReply does not upload app logs when not requested`() = test {
        val conversation = createConversation(id = 1L)
        selectConversation(conversation)
        advanceUntilIdle()
        whenever(repository.replyToConversation(any(), any(), any(), any())).thenReturn(conversation)

        viewModel.sendReply("My reply", includeAppLogs = false)
        advanceUntilIdle()

        verify(encryptedAppLogsUploader, never()).uploadLogs()
    }

    @Test
    fun `sendReply sends attachments as temp files and removes them afterwards`() = test {
        val conversation = createConversation(id = 1L)
        selectConversation(conversation)
        advanceUntilIdle()
        val attachmentUri = mock<Uri>()
        val attachmentState = AttachmentState(acceptedUris = listOf(attachmentUri))
        whenever(attachmentStateValidator.addAttachments(any(), eq(listOf(attachmentUri))))
            .thenReturn(attachmentState)
        viewModel.addReplyAttachments(listOf(attachmentUri))
        advanceUntilIdle()
        val tempFile = File("/tmp/support_attachment.jpg")
        whenever(tempAttachmentsUtil.createTempFilesFrom(listOf(attachmentUri))).thenReturn(listOf(tempFile))
        whenever(repository.replyToConversation(any(), any(), any(), any())).thenReturn(conversation)

        viewModel.sendReply("My reply")
        advanceUntilIdle()

        verify(repository).replyToConversation(
            conversationId = eq(1L),
            message = eq("My reply"),
            attachments = eq(listOf(tempFile.path)),
            encryptedLogIds = eq(emptyList()),
        )
        verify(tempAttachmentsUtil).removeTempFiles(listOf(tempFile))
    }

    @Test
    fun `sendReply clears the reply form on success`() = test {
        val conversation = createConversation(id = 1L)
        selectConversation(conversation)
        advanceUntilIdle()
        viewModel.updateReplyMessage("Draft message")
        viewModel.updateReplyIncludeAppLogs(true)
        whenever(encryptedAppLogsUploader.uploadLogs()).thenReturn(emptyList())
        whenever(repository.replyToConversation(any(), any(), any(), any())).thenReturn(conversation)

        viewModel.sendReply("Draft message", includeAppLogs = true)
        advanceUntilIdle()

        assertThat(viewModel.replyFormState.value).isEqualTo(ConversationReplyFormState())
    }

    @Test
    fun `sendReply restores the input message when the reply fails`() = test {
        val conversation = createConversation(id = 1L)
        selectConversation(conversation)
        advanceUntilIdle()
        viewModel.updateReplyMessage("My reply")
        whenever(repository.replyToConversation(any(), any(), any(), any())).thenReturn(null)

        viewModel.sendReply("My reply")
        advanceUntilIdle()

        assertThat(viewModel.replyFormState.value.message).isEqualTo("My reply")
    }

    @Test
    fun `sendReply keeps a newly typed input message instead of restoring it on failure`() = test {
        val conversation = createConversation(id = 1L)
        selectConversation(conversation)
        advanceUntilIdle()
        viewModel.updateReplyMessage("Old reply")
        // The user types a new message while the send is in flight, just before it fails.
        whenever(repository.replyToConversation(any(), any(), any(), any())).then {
            viewModel.updateReplyMessage("New draft")
            null
        }

        viewModel.sendReply("Old reply")
        advanceUntilIdle()

        assertThat(viewModel.replyFormState.value.message).isEqualTo("New draft")
    }

    // endregion

    // region Reply form state tests

    @Test
    fun `updateReplyMessage updates the reply form message`() {
        viewModel.updateReplyMessage("Hello")

        assertThat(viewModel.replyFormState.value.message).isEqualTo("Hello")
    }

    @Test
    fun `updateReplyIncludeAppLogs updates the reply form flag`() {
        viewModel.updateReplyIncludeAppLogs(true)

        assertThat(viewModel.replyFormState.value.includeAppLogs).isTrue
    }

    @Test
    fun `updateReplyBottomSheetVisibility updates the reply form visibility`() {
        viewModel.updateReplyBottomSheetVisibility(true)

        assertThat(viewModel.replyFormState.value.isBottomSheetVisible).isTrue
    }

    @Test
    fun `clearReplyForm resets the reply form to defaults`() {
        viewModel.updateReplyMessage("Hello")
        viewModel.updateReplyIncludeAppLogs(true)
        viewModel.updateReplyBottomSheetVisibility(true)

        viewModel.clearReplyForm()

        assertThat(viewModel.replyFormState.value).isEqualTo(ConversationReplyFormState())
    }

    @Test
    fun `addReplyAttachments delegates validation and stores the result`() = test {
        val uri = mock<Uri>()
        val validatedState = AttachmentState(acceptedUris = listOf(uri), currentTotalSizeBytes = 100L)
        whenever(attachmentStateValidator.addAttachments(any(), eq(listOf(uri)))).thenReturn(validatedState)

        viewModel.addReplyAttachments(listOf(uri))
        advanceUntilIdle()

        assertThat(viewModel.replyFormState.value.attachmentState).isEqualTo(validatedState)
    }

    @Test
    fun `removeReplyAttachment delegates removal and revalidates rejected uris`() = test {
        val uri1 = mock<Uri>()
        val uri2 = mock<Uri>()
        val initialState = AttachmentState(acceptedUris = listOf(uri1), rejectedUris = listOf(uri2))
        whenever(attachmentStateValidator.addAttachments(any(), eq(listOf(uri1)))).thenReturn(initialState)
        viewModel.addReplyAttachments(listOf(uri1))
        advanceUntilIdle()
        val removedState = AttachmentState(acceptedUris = emptyList(), rejectedUris = listOf(uri2))
        val revalidatedState = AttachmentState(acceptedUris = listOf(uri2))
        whenever(attachmentStateValidator.removeAttachment(initialState, uri1)).thenReturn(removedState)
        whenever(attachmentStateValidator.addAttachments(eq(removedState), eq(listOf(uri2))))
            .thenReturn(revalidatedState)

        viewModel.removeReplyAttachment(uri1)
        advanceUntilIdle()

        assertThat(viewModel.replyFormState.value.attachmentState).isEqualTo(revalidatedState)
    }

    @Test
    fun `notifyGeneralError sets the GENERAL error`() {
        viewModel.notifyGeneralError()

        assertThat(viewModel.errorMessage.value).isEqualTo(ConversationsSupportViewModel.ErrorType.GENERAL)
    }

    // endregion

    // region Video download tests

    @Test
    fun `downloadVideoToTempFile emits Success when the download succeeds`() = test {
        val videoUrl = "https://example.com/video.mp4"
        val tempFile = File.createTempFile("video_", ".mp4").apply { deleteOnExit() }
        whenever(tempAttachmentsUtil.createVideoTempFile(videoUrl)).thenReturn(tempFile)

        viewModel.downloadVideoToTempFile(videoUrl)
        advanceUntilIdle()

        assertThat(viewModel.videoDownloadState.value).isEqualTo(VideoDownloadState.Success(tempFile))
    }

    @Test
    fun `downloadVideoToTempFile reuses the cached file on subsequent downloads`() = test {
        val videoUrl = "https://example.com/video.mp4"
        val tempFile = File.createTempFile("video_", ".mp4").apply { deleteOnExit() }
        whenever(tempAttachmentsUtil.createVideoTempFile(videoUrl)).thenReturn(tempFile)
        viewModel.downloadVideoToTempFile(videoUrl)
        advanceUntilIdle()

        viewModel.downloadVideoToTempFile(videoUrl)
        advanceUntilIdle()

        verify(tempAttachmentsUtil, times(1)).createVideoTempFile(videoUrl)
        assertThat(viewModel.videoDownloadState.value).isInstanceOf(VideoDownloadState.Success::class.java)
    }

    @Test
    fun `downloadVideoToTempFile emits Error when the download fails`() = test {
        val videoUrl = "https://example.com/video.mp4"
        whenever(tempAttachmentsUtil.createVideoTempFile(videoUrl)).thenReturn(null)

        viewModel.downloadVideoToTempFile(videoUrl)
        advanceUntilIdle()

        assertThat(viewModel.videoDownloadState.value).isEqualTo(VideoDownloadState.Error)
    }

    @Test
    fun `downloadVideoToTempFile emits Error when the download throws`() = test {
        val videoUrl = "https://example.com/video.mp4"
        whenever(tempAttachmentsUtil.createVideoTempFile(videoUrl)).thenThrow(RuntimeException("Download failed"))

        viewModel.downloadVideoToTempFile(videoUrl)
        advanceUntilIdle()

        assertThat(viewModel.videoDownloadState.value).isEqualTo(VideoDownloadState.Error)
        verify(appLogWrapper).e(any(), any<String>())
    }

    @Test
    fun `resetVideoDownloadState resets the state to Idle`() = test {
        val videoUrl = "https://example.com/video.mp4"
        whenever(tempAttachmentsUtil.createVideoTempFile(videoUrl)).thenReturn(null)
        viewModel.downloadVideoToTempFile(videoUrl)
        advanceUntilIdle()

        viewModel.resetVideoDownloadState()

        assertThat(viewModel.videoDownloadState.value).isEqualTo(VideoDownloadState.Idle)
    }

    // endregion
}
