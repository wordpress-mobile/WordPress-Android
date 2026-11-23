package org.wordpress.android.ui.media

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.accounts.login.ApplicationPasswordReauthNotifier
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.media.library.AddMediaOption
import org.wordpress.android.ui.media.library.MediaLibraryEvent
import org.wordpress.android.ui.media.library.MediaLibraryScreen
import org.wordpress.android.ui.media.library.MediaLibraryViewModel
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import javax.inject.Inject

/**
 * The site's media library. The grid itself lives in [MediaLibraryViewModel] and [MediaLibraryScreen]; this
 * Activity handles what needs an Activity: permissions, the camera and pickers, the media details screen, the
 * upload snackbars and the incoming share and notification intents.
 */
@AndroidEntryPoint
class MediaBrowserActivity : BaseAppCompatActivity(), WPMediaUtils.LaunchCameraCallback,
    ApplicationPasswordReauthNotifier.Listener {
    @Inject lateinit var mediaPickerLauncher: MediaPickerLauncher
    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper
    @Inject lateinit var systemNotificationsTracker: SystemNotificationsTracker
    @Inject lateinit var activityNavigator: ActivityNavigator
    @Inject lateinit var reauthNotifier: ApplicationPasswordReauthNotifier
    @Inject lateinit var connectionStatus: LiveData<ConnectionStatus>

    private val viewModel: MediaLibraryViewModel by viewModels()

    // the option to run once the permissions it needs are granted
    private var pendingAddMediaOption: AddMediaOption? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE) == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            finish()
            return
        }

        // the view id anchors the upload snackbars and lets the screenshot tests wait for the library
        val composeView = ComposeView(this).apply {
            id = R.id.media_browser_container
            setContent {
                val state by viewModel.uiState.collectAsState()
                AppThemeM3 {
                    MediaLibraryScreen(
                        state = state,
                        actions = viewModel,
                        onNavigateBack = { onBackPressedDispatcher.onBackPressed() },
                        onAddMediaOptionClick = ::onAddMediaOptionClick
                    )
                }
            }
        }
        setContentView(composeView)

        observeEvents()
        connectionStatus.observe(this) { status ->
            // continue any pending deletes once the connection comes back
            if (status == ConnectionStatus.AVAILABLE) {
                viewModel.onConnectionAvailable()
            }
        }

        if (savedInstanceState == null) {
            handleSharedMedia()
            if (intent.getBooleanExtra(ARG_LAUNCH_PHOTO_PICKER, false)) {
                onAddMediaOptionClick(AddMediaOption.CHOOSE_FILE)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getSerializableExtraCompat<NotificationType>(ARG_NOTIFICATION_TYPE)?.let {
            systemNotificationsTracker.trackTappedNotification(it)
        }
    }

    override fun onStart() {
        super.onStart()
        reauthNotifier.addListener(this)
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumePendingDeletes()
        ActivityId.trackLastActivity(ActivityId.MEDIA)
    }

    override fun onStop() {
        reauthNotifier.removeListener(this)
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MediaLibraryEvent.OpenMediaSettings ->
                            MediaSettingsActivity.showForResult(this@MediaBrowserActivity, viewModel.site,
                                event.media, event.mediaIds)
                        is MediaLibraryEvent.ShowToast ->
                            ToastUtils.showToast(this@MediaBrowserActivity, event.message, ToastUtils.Duration.LONG)
                    }
                }
            }
        }
    }

    /** Uploads media shared into the app; the action is cleared so a recreation doesn't upload it again */
    private fun handleSharedMedia() {
        val uris = when (intent.action) {
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_SEND ->
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            else -> null
        }
        if (!uris.isNullOrEmpty()) {
            viewModel.onSharedMedia(uris)
        }
        intent.action = null
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.FILE_LIBRARY -> if (resultCode == RESULT_OK) {
                data?.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)?.let { uris ->
                    viewModel.onFilesPicked(uris.map { Uri.parse(it) })
                }
            }
            RequestCodes.TAKE_PHOTO -> if (resultCode == RESULT_OK) {
                viewModel.onPhotoCaptured()
            }
            RequestCodes.TAKE_VIDEO -> if (resultCode == RESULT_OK) {
                data?.data?.let { viewModel.onVideoCaptured(it) }
            }
            RequestCodes.MEDIA_SETTINGS -> if (resultCode == MediaSettingsActivity.RESULT_MEDIA_DELETED) {
                viewModel.onMediaChangedElsewhere()
            }
            RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT -> if (resultCode == RESULT_OK) {
                viewModel.onMediaChangedElsewhere()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val allGranted = WPPermissionUtils.setPermissionListAsked(this, requestCode, permissions, grantResults, true)
        val option = pendingAddMediaOption
        if (allGranted && requestCode == WPPermissionUtils.MEDIA_BROWSER_PERMISSION_REQUEST_CODE && option != null) {
            onAddMediaOptionClick(option)
        }
    }

    private fun onAddMediaOptionClick(option: AddMediaOption) {
        pendingAddMediaOption = option
        val permissions = when (option) {
            AddMediaOption.CAPTURE_PHOTO, AddMediaOption.CAPTURE_VIDEO ->
                PermissionUtils.getCameraAndStoragePermissions()
            AddMediaOption.CHOOSE_FILE -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .takeIf { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q }
            AddMediaOption.STOCK_MEDIA -> null
        }
        if (permissions != null && !PermissionUtils.checkAndRequestPermissions(
                this, WPPermissionUtils.MEDIA_BROWSER_PERMISSION_REQUEST_CODE, permissions
            )
        ) {
            return
        }

        val site = viewModel.site
        when (option) {
            AddMediaOption.CAPTURE_PHOTO -> WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID, this)
            AddMediaOption.CAPTURE_VIDEO -> WPMediaUtils.launchVideoCamera(this)
            AddMediaOption.CHOOSE_FILE -> mediaPickerLauncher.showFilePicker(this, true, site)
            AddMediaOption.STOCK_MEDIA -> mediaPickerLauncher.showStockMediaPickerForResult(
                this, site, RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT, true
            )
        }
    }

    override fun onMediaCapturePathReady(mediaCapturePath: String?) {
        viewModel.mediaCapturePath = mediaCapturePath
    }

    override fun onCameraError(errorMessage: String?) {
        ToastUtils.showToast(this, errorMessage, ToastUtils.Duration.LONG)
    }

    override fun onReauthRequired(siteUrl: String) {
        activityNavigator.navigateToApplicationPasswordReauthentication(this, siteUrl)
    }

    @Suppress("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UploadService.UploadErrorEvent) {
        // retrying won't help when the site is out of space, so leave the retry action off
        val isQuotaError = event.errorMessage?.contains(getString(R.string.error_media_quota_exceeded)) == true
        onUploadEvent(event, event.mediaModelList, isError = true, event.errorMessage, offerRetry = !isQuotaError)
    }

    @Suppress("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UploadService.UploadMediaSuccessEvent) {
        onUploadEvent(event, event.mediaModelList, isError = false, event.successMessage)
    }

    private fun onUploadEvent(
        event: Any,
        mediaList: List<MediaModel>?,
        isError: Boolean,
        message: String?,
        offerRetry: Boolean = true
    ) {
        EventBus.getDefault().removeStickyEvent(event)
        if (mediaList.isNullOrEmpty()) return
        uploadUtilsWrapper.onMediaUploadedSnackbarHandler(
            this,
            findViewById(R.id.media_browser_container),
            isError,
            if (offerRetry) mediaList else null,
            viewModel.site,
            message
        )
        viewModel.onUploadFinished()
    }

    companion object {
        const val ARG_LAUNCH_PHOTO_PICKER = "launch_photo_picker"

        /** Result key for picked remote media ids; still used by the media picker and the editors */
        const val RESULT_IDS = "result_ids"
    }
}
