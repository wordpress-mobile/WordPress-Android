package org.wordpress.android.support.unified.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.unified.util.AttachmentActionsListener
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.mediapicker.MediaPickerActivity
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.reader.ReaderFileDownloadManager
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@AndroidEntryPoint
class UnifiedSupportActivity : AppCompatActivity() {
    @Inject lateinit var fileDownloadManager: ReaderFileDownloadManager
    @Inject lateinit var appLogWrapper: AppLogWrapper
    @Inject lateinit var activityNavigator: ActivityNavigator
    private val viewModel by viewModels<UnifiedSupportViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    @Suppress("TooGenericExceptionCaught")
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == RESULT_OK && result.data != null) {
                val uris = result.data?.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
                uris?.let { uriStrings ->
                    viewModel.addReplyAttachments(uriStrings.map { it.toUri() })
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
        // The ViewModel survives configuration changes and keeps its loaded state, so only
        // initialise (and load conversations) on the first creation, not on every recreation.
        if (savedInstanceState == null) {
            viewModel.init()
        }
    }

    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is ConversationsSupportViewModel.NavigationEvent.NavigateToConversationDetail -> {
                            navController.navigate(UnifiedScreen.Detail.name)
                        }
                        ConversationsSupportViewModel.NavigationEvent.NavigateBack -> {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }
    }

    private enum class UnifiedScreen {
        List,
        Detail
    }

    @Composable
    private fun NavigableContent() {
        navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val errorMessage by viewModel.errorMessage.collectAsState()

        val errorType = errorMessage
        if (errorType != null) {
            val message = when (errorType) {
                ConversationsSupportViewModel.ErrorType.GENERAL ->
                    getString(R.string.unified_support_generic_error)
                ConversationsSupportViewModel.ErrorType.FORBIDDEN ->
                    getString(R.string.he_support_forbidden_error)
                ConversationsSupportViewModel.ErrorType.OFFLINE ->
                    getString(R.string.no_network_title)
            }
            LaunchedEffect(errorType) {
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
                startDestination = UnifiedScreen.List.name,
            ) {
                composable(route = UnifiedScreen.List.name) {
                    val conversationsState by viewModel.conversationsState.collectAsState()
                    val conversations by viewModel.conversations.collectAsState()
                    UnifiedConversationsListScreen(
                        snackbarHostState = snackbarHostState,
                        conversations = conversations,
                        conversationsState = conversationsState,
                        onConversationClick = { conversation ->
                            viewModel.onConversationClick(conversation)
                        },
                        onBackClick = { finish() },
                        onCreateNewConversationClick = { viewModel.onCreateNewBotConversationClick() },
                        onRefresh = { viewModel.refreshConversations() },
                    )
                }

                composable(route = UnifiedScreen.Detail.name) {
                    val selectedConversation by viewModel.selectedConversation.collectAsState()
                    val isLoadingConversation by viewModel.isLoadingConversation.collectAsState()
                    val isSendingReply by viewModel.isSendingReply.collectAsState()
                    val videoDownloadState by viewModel.videoDownloadState.collectAsState()
                    val replyFormState by viewModel.replyFormState.collectAsState()
                    val userInfo by viewModel.userInfo.collectAsState()
                    selectedConversation?.let { conversation ->
                        UnifiedConversationDetailScreen(
                            snackbarHostState = snackbarHostState,
                            conversation = conversation,
                            isLoading = isLoadingConversation,
                            isSendingReply = isSendingReply,
                            userName = userInfo.userName,
                            onBackClick = {
                                viewModel.clearReplyForm()
                                viewModel.onBackClick()
                            },
                            onSendReply = { text, includeAppLogs ->
                                viewModel.sendReply(text, includeAppLogs)
                            },
                            onDownloadAttachment = { attachment ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = getString(
                                            R.string.he_support_downloading_attachment,
                                            attachment.filename
                                        ),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                fileDownloadManager.downloadFile(attachment.url, attachment.filename)
                            },
                            onLinkClick = { url ->
                                activityNavigator.openInCustomTab(this@UnifiedSupportActivity, url)
                            },
                            authorizationHeader = viewModel.getAuthorizationHeader(),
                            videoDownloadState = videoDownloadState,
                            onStartVideoDownload = { url -> viewModel.downloadVideoToTempFile(url) },
                            onResetVideoDownloadState = { viewModel.resetVideoDownloadState() },
                            replyFormState = replyFormState,
                            onReplyMessageChange = { viewModel.updateReplyMessage(it) },
                            onReplyIncludeAppLogsChange = { viewModel.updateReplyIncludeAppLogs(it) },
                            onReplyBottomSheetVisibilityChange = {
                                viewModel.updateReplyBottomSheetVisibility(it)
                            },
                            attachmentActionsListener = attachmentActionsListener,
                        )
                    }
                }
            }
        }
    }

    private val attachmentActionsListener = object : AttachmentActionsListener {
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
            val intent = MediaPickerActivity.buildIntent(
                this@UnifiedSupportActivity,
                mediaPickerSetup,
                null,
                null
            )
            photoPickerLauncher.launch(intent)
        }

        override fun onRemoveImage(uri: Uri) {
            viewModel.removeReplyAttachment(uri)
        }
    }

    companion object {
        const val AUTHORIZATION_TAG = "Authorization"

        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, UnifiedSupportActivity::class.java)
    }
}
