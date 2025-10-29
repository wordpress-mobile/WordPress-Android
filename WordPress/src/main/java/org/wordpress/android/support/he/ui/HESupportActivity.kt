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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.reader.ReaderFileDownloadManager
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserType
import javax.inject.Inject

@AndroidEntryPoint
class HESupportActivity : AppCompatActivity() {
    @Inject lateinit var mediaPickerLauncher: MediaPickerLauncher
    @Inject lateinit var fileDownloadManager: ReaderFileDownloadManager
    private val viewModel by viewModels<HESupportViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    // State for selected images in Detail screen
    private var selectedDetailImageUris by mutableStateOf<List<Uri>>(emptyList())
    private var selectedDetailImagePaths by mutableStateOf<List<String>>(emptyList())

    // State for selected images in NewTicket screen
    private var selectedNewTicketImageUris by mutableStateOf<List<Uri>>(emptyList())
    private var selectedNewTicketImagePaths by mutableStateOf<List<String>>(emptyList())

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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                RequestCodes.PHOTO_PICKER -> {
                    // Handle media picker result based on current screen
                    val uris = data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
                    uris?.let { uriStrings ->
                        lifecycleScope.launch {
                            val newUris = uriStrings.map { it.toUri() }

                            // Determine which screen is active by checking current destination
                            val currentDestination = navController.currentDestination?.route

                            when (currentDestination) {
                                ConversationScreen.Detail.name -> {
                                    // Convert URIs to file paths
                                    val newPaths = newUris.mapNotNull { uri ->
                                        copyUriToTempFile(uri)?.absolutePath
                                    }
                                    // Update state immutably to trigger recomposition
                                    selectedDetailImageUris = selectedDetailImageUris + newUris
                                    selectedDetailImagePaths = selectedDetailImagePaths + newPaths
                                }
                                ConversationScreen.NewTicket.name -> {
                                    // Convert URIs to file paths
                                    val newPaths = newUris.mapNotNull { uri ->
                                        copyUriToTempFile(uri)?.absolutePath
                                    }
                                    // Update state immutably to trigger recomposition
                                    selectedNewTicketImageUris = selectedNewTicketImageUris + newUris
                                    selectedNewTicketImagePaths = selectedNewTicketImagePaths + newPaths
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun copyUriToTempFile(uri: Uri): java.io.File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "support_image_${System.currentTimeMillis()}.jpg"
            val tempFile = java.io.File(cacheDir, fileName)

            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            tempFile
        } catch (e: Exception) {
            null
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
                    val selectedConversation by viewModel.selectedConversation.collectAsState()
                    val isLoadingConversation by viewModel.isLoadingConversation.collectAsState()
                    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
                    val messageSendResult by viewModel.messageSendResult.collectAsState()

                    // Clear images after successful message send
                    LaunchedEffect(messageSendResult) {
                        if (messageSendResult is HESupportViewModel.MessageSendResult.Success) {
                            selectedDetailImageUris = emptyList()
                            selectedDetailImagePaths = emptyList()
                        }
                    }

                    selectedConversation?.let { conversation ->
                        HEConversationDetailScreen(
                            snackbarHostState = snackbarHostState,
                            conversation = conversation,
                            isLoading = isLoadingConversation,
                            isSendingMessage = isSendingMessage,
                            messageSendResult = messageSendResult,
                            onBackClick = { viewModel.onBackClick() },
                            onSendMessage = { message, includeAppLogs, _ ->
                                viewModel.onAddMessageToConversation(
                                    message = message,
                                    attachments = selectedDetailImagePaths
                                )
                            },
                            onClearMessageSendResult = { viewModel.clearMessageSendResult() },
                            onAddImageClick = {
                                if (selectedDetailImageUris.size < 4) {
                                    mediaPickerLauncher.showPhotoPickerForResult(
                                        activity = this@HESupportActivity,
                                        browserType = MediaBrowserType.FEEDBACK_FORM_MEDIA_PICKER,
                                        site = null,
                                        localPostId = null
                                    )
                                }
                            },
                            selectedImagePaths = selectedDetailImageUris.map { it.toString() },
                            onRemoveImage = { uriString ->
                                val index = selectedDetailImageUris.indexOfFirst { it.toString() == uriString }
                                if (index >= 0) {
                                    selectedDetailImageUris = selectedDetailImageUris.filterIndexed { i, _ -> i != index }
                                    selectedDetailImagePaths = selectedDetailImagePaths.filterIndexed { i, _ -> i != index }
                                }
                            },
                            onDownloadAttachment = { attachment ->
                                // Show loading snackbar
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = getString(R.string.he_support_downloading_attachment, attachment.filename),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                // Start download with proper filename
                                fileDownloadManager.downloadFile(attachment.url, attachment.filename)
                            }
                        )
                    }
                }

                composable(route = ConversationScreen.NewTicket.name) {
                    val userInfo by viewModel.userInfo.collectAsState()
                    val isSendingNewConversation by viewModel.isSendingMessage.collectAsState()

                    HENewTicketScreen(
                        snackbarHostState = snackbarHostState,
                        onBackClick = { viewModel.onBackClick() },
                        onSubmit = { category, subject, messageText, siteAddress, _ ->
                            viewModel.onSendNewConversation(
                                subject = subject,
                                message = messageText,
                                tags = listOf(category.key),
                                attachments = selectedNewTicketImagePaths
                            )
                        },
                        userName = userInfo.userName,
                        userEmail = userInfo.userEmail,
                        userAvatarUrl = userInfo.avatarUrl,
                        isSendingNewConversation = isSendingNewConversation,
                        onAddImageClick = {
                            if (selectedNewTicketImageUris.size < 4) {
                                mediaPickerLauncher.showPhotoPickerForResult(
                                    activity = this@HESupportActivity,
                                    browserType = MediaBrowserType.FEEDBACK_FORM_MEDIA_PICKER,
                                    site = null,
                                    localPostId = null
                                )
                            }
                        },
                        selectedImagePaths = selectedNewTicketImageUris.map { it.toString() },
                        onRemoveImage = { uriString ->
                            val index = selectedNewTicketImageUris.indexOfFirst { it.toString() == uriString }
                            if (index >= 0) {
                                selectedNewTicketImageUris = selectedNewTicketImageUris.filterIndexed { i, _ -> i != index }
                                selectedNewTicketImagePaths = selectedNewTicketImagePaths.filterIndexed { i, _ -> i != index }
                            }
                        },
                        )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, HESupportActivity::class.java)
    }
}
