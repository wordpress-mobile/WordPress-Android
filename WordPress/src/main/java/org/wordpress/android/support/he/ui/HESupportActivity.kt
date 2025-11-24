package org.wordpress.android.support.he.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.he.util.AttachmentActionsListener
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.reader.ReaderFileDownloadManager
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@AndroidEntryPoint
class HESupportActivity : AppCompatActivity() {
    @Inject lateinit var fileDownloadManager: ReaderFileDownloadManager
    @Inject lateinit var appLogWrapper: AppLogWrapper
    @Inject lateinit var accountStore: AccountStore
    private val viewModel by viewModels<HESupportViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    @Suppress("TooGenericExceptionCaught")
    private val photoPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == RESULT_OK && result.data != null) {
                val uris = result.data?.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
                uris?.let { uriStrings ->
                    val newUris = uriStrings.map { it.toUri() }
                    viewModel.addAttachments(newUris)
                }
            }
        } catch (e: Exception) {
            viewModel.notifyGeneralError()
            appLogWrapper.e(
                AppLog.T.SUPPORT, "Error getting attachments to add: ${e.stackTraceToString()}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        composeView = ComposeView(this)
        setContentView(
            composeView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    NavigableContent()
                }
            }
        )
        observeNavigationEvents()
        viewModel.init()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup cached video files
        viewModel.cleanupVideoCache()
    }


    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is ConversationsSupportViewModel.NavigationEvent.NavigateToConversationDetail -> {
                            navController.navigate(ConversationScreen.Detail.name)
                        }
                        ConversationsSupportViewModel.NavigationEvent.NavigateToNewConversation -> {
                            navController.navigate(ConversationScreen.NewTicket.name)
                        }
                        ConversationsSupportViewModel.NavigationEvent.NavigateBack -> {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }
    }

    private enum class ConversationScreen {
        List,
        Detail,
        NewTicket
    }

    @Composable
    private fun NavigableContent() {
        navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val errorMessage by viewModel.errorMessage.collectAsState()

        // Show snackbar when error occurs
        errorMessage?.let { errorType ->
            val message = when (errorType) {
                ConversationsSupportViewModel.ErrorType.GENERAL -> getString(R.string.he_support_generic_error)
                ConversationsSupportViewModel.ErrorType.FORBIDDEN -> getString(R.string.he_support_forbidden_error)
                ConversationsSupportViewModel.ErrorType.OFFLINE -> getString(R.string.no_network_title)
            }
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearError()
            }
        }

        AppThemeM3 {
            NavHost(
                navController = navController,
                startDestination = ConversationScreen.List.name,
            ) {
                composable(route = ConversationScreen.List.name) {
                    val conversationsState by viewModel.conversationsState.collectAsState()
                    val conversations by viewModel.conversations.collectAsState()
                    HEConversationsListScreen(
                        snackbarHostState = snackbarHostState,
                        conversations = conversations,
                        conversationsState = conversationsState,
                        onConversationClick = { conversation ->
                            viewModel.onConversationClick(conversation)
                        },
                        onBackClick = { finish() },
                        onCreateNewConversationClick = {
                            viewModel.onCreateNewConversationClick()
                        },
                        onRefresh = {
                            viewModel.refreshConversations()
                        }
                    )
                }

                composable(route = ConversationScreen.Detail.name) {
                    // Clear attachments when leaving conversation screen
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        onDispose {
                            viewModel.clearAttachments()
                        }
                    }

                    val selectedConversation by viewModel.selectedConversation.collectAsState()
                    val isLoadingConversation by viewModel.isLoadingConversation.collectAsState()
                    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
                    val messageSendResult by viewModel.messageSendResult.collectAsState()
                    val attachmentState by viewModel.attachmentState.collectAsState()
                    val videoDownloadState by viewModel.videoDownloadState.collectAsState()

                    selectedConversation?.let { conversation ->
                        HEConversationDetailScreen(
                            snackbarHostState = snackbarHostState,
                            conversation = conversation,
                            isLoading = isLoadingConversation,
                            isSendingMessage = isSendingMessage,
                            messageSendResult = messageSendResult,
                            onBackClick = { viewModel.onBackClick() },
                            onSendMessage = { message, includeAppLogs ->
                                viewModel.onAddMessageToConversation(
                                    message = message,
                                )
                            },
                            onClearMessageSendResult = { viewModel.clearMessageSendResult() },
                            attachmentState = attachmentState,
                            attachmentActionsListener = createAttachmentActionListener(),
                            onDownloadAttachment = { attachment ->
                                // Show loading snackbar
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = getString(
                                            R.string.he_support_downloading_attachment,
                                            attachment.filename
                                        ),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                // Start download with proper filename
                                fileDownloadManager.downloadFile(attachment.url, attachment.filename)
                            },
                            onGetAuthorizationHeaderArgument = { viewModel.getAuthorizationHeader() },
                            videoDownloadState = videoDownloadState,
                            onStartVideoDownload = { url ->
                                viewModel.downloadVideoToTempFile(url)
                            },
                            onResetVideoDownloadState = { viewModel.resetVideoDownloadState() }
                        )
                    }
                }

                composable(route = ConversationScreen.NewTicket.name) {
                    val userInfo by viewModel.userInfo.collectAsState()
                    val isSendingNewConversation by viewModel.isSendingMessage.collectAsState()
                    val attachmentState by viewModel.attachmentState.collectAsState()

                    // Clear attachments when leaving the new ticket screen
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        onDispose {
                            viewModel.clearAttachments()
                        }
                    }

                    HENewTicketScreen(
                        snackbarHostState = snackbarHostState,
                        onBackClick = { viewModel.onBackClick() },
                        onSubmit = { category, subject, messageText, siteAddress ->
                            viewModel.onSendNewConversation(
                                subject = subject,
                                message = messageText,
                                tags = listOf(category.key),
                            )
                        },
                        userInfo = userInfo,
                        isSendingNewConversation = isSendingNewConversation,
                        attachmentState = attachmentState,
                        attachmentActionsListener = createAttachmentActionListener()
                    )
                }
            }
        }
    }

    private fun createAttachmentActionListener(): AttachmentActionsListener {
        return object : AttachmentActionsListener {
            override fun onAddImageClick() {
                val mediaPickerSetup = MediaPickerSetup(
                    primaryDataSource = MediaPickerSetup.DataSource.DEVICE,
                    availableDataSources = setOf(),
                    canMultiselect = true,
                    requiresPhotosVideosPermissions = true,
                    requiresMusicAudioPermissions = false,
                    allowedTypes = setOf(MediaType.IMAGE, MediaType.VIDEO),
                    cameraSetup = MediaPickerSetup.CameraSetup.HIDDEN,
                    systemPickerEnabled = true,
                    editingEnabled = true,
                    queueResults = false,
                    defaultSearchView = false,
                    title = R.string.he_support_select_attachments
                )
                val intent = org.wordpress.android.ui.mediapicker.MediaPickerActivity.buildIntent(
                    this@HESupportActivity,
                    mediaPickerSetup,
                    null,
                    null
                )
                photoPickerLauncher.launch(intent)
            }

            override fun onRemoveImage(uri: Uri) {
                viewModel.removeAttachment(uri)
            }
        }
    }


    companion object {
        const val AUTHORIZATION_TAG = "Authorization"

        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, HESupportActivity::class.java)
    }
}
