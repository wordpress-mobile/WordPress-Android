package org.wordpress.android.ui.posts.editor.media

import android.content.Context
import android.net.Uri
import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.posts.editor.EditorMedia
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase.AddMediaToEditorUiState.AddingMediaCompleted
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase.AddMediaToEditorUiState.AddingMultipleMedia
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase.AddMediaToEditorUiState.AddingSingleMedia
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

private typealias Path = String

/*
     * processes a list of media in the background (optimizing, resizing, etc.) and adds them to
     * the editor one at a time
     */
// TODO make sure we test both self-hosted and wpcom - also test photo orientation/rotation
class OptimizeAndAddMediaToEditorUseCase @Inject constructor(
    private val context: Context,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    private val _uiState: MutableLiveData<AddMediaToEditorUiState> = MutableLiveData()
    val uiState: LiveData<AddMediaToEditorUiState> = _uiState

    private lateinit var uriList: List<Uri>
    private lateinit var site: SiteModel
    private var isNew by Delegates.notNull<Boolean>() // TODO can't we derive that from the path?
    private lateinit var editorMediaListener: EditorMediaListener
    private lateinit var editorMedia: EditorMedia

    fun optimizeAndAddAsync(
        uriList: List<Uri>,
        site: SiteModel,
        isNew: Boolean, // TODO can't we derive that from the path?
        editorMediaListener: EditorMediaListener,
        editorMedia: EditorMedia
    ) {
        this.uriList = uriList
        this.site = site
        this.isNew = isNew
        this.editorMediaListener = editorMediaListener
        this.editorMedia = editorMedia

        launch {
            // Step 1 - update UI state - in progress
            _uiState.value = if (uriList.size > 1) {
                // adding multiple media items at once can take several seconds on slower devices, so we show a blocking
                // progress dialog in this situation - otherwise the user could accidentally back out of the process
                // before all items were added
                AddingMultipleMedia
            } else {
                AddingSingleMedia
            }

            // Step 2 - optimize and upload media
            val mediaFiles = optimizeAndUploadMediaAsync(uriList, this)
            // Step 3 - add the media into the post content = editor
            addMediaToEditor(editorMediaListener, mediaFiles)
            // Step 4 - save the post
            editorMediaListener.savePostAsyncFromEditorMedia()

            // Step 5 - update UI state - completed
            _uiState.value = AddingMediaCompleted
        }
    }

    private suspend fun optimizeAndUploadMediaAsync(uriList: List<Uri>, coroutineScope: CoroutineScope): List<Pair<Path, MediaFile>?> {
        // optimize and enqueue media in parallel
        return uriList.map { coroutineScope.async { processMedia(it) } }.map { it.await() }.toList()
    }

    private suspend fun addMediaToEditor(
        editorMediaListener: EditorMediaListener,
        results: List<Pair<Path, MediaFile>?>
    ) {
        withContext(bgDispatcher) {
            // TODO what if the upload service has already finished the upload, won't we add a local path to the image?
            editorMediaListener.appendMediaFiles(
                    results.filterNotNull().associateByTo(
                            ArrayMap(),
                            { it.first },
                            { it.second })
            )
            if (results.contains(null)) {
                // TODO  show toast
            }
        }
    }

    private suspend fun processMedia(mediaUri: Uri): Pair<Path, MediaFile>? {
        val path = MediaUtils.getRealPathFromURI(context, mediaUri) ?: return null

        val isVideo = MediaUtils.isVideo(mediaUri.toString())
        val optimizedMedia = WPMediaUtils.getOptimizedMedia(context, path, isVideo)
        var updatedMediaUri: Uri = mediaUri
        // TODO how come we don't need to rotate the image after it was optimized?
        if (optimizedMedia != null) {
            updatedMediaUri = optimizedMedia
        } else {
            // Fix for the rotation issue https://github.com/wordpress-mobile/WordPress-Android/issues/5737
            if (!site.isWPCom) {
                // If it's not wpcom we must rotate the picture locally
                val rotatedMedia = WPMediaUtils.fixOrientationIssue(context, path, isVideo)
                if (rotatedMedia != null) {
                    updatedMediaUri = rotatedMedia
                }
            }
        }

        trackAddMediaFromDeviceEvents(isNew, isVideo, updatedMediaUri)

        val mediaFile = withContext(mainDispatcher) {
            uploadMedia(mediaUri)
        } ?: return null
        return Pair(path, mediaFile)
    }

    private fun uploadMedia(uri: Uri): MediaFile? {
        val media = editorMedia.queueFileForUpload(uri)
        val mediaFile = FluxCUtils.mediaFileFromMediaModel(media)
        return if (media != null) {
            mediaFile
        } else {
            null
        }
    }

    /**
     * Analytics about media from device
     *
     * @param isNew Whether is a fresh media
     * @param isVideo Whether is a video or not
     * @param uri The URI of the media on the device, or null
     */
    private fun trackAddMediaFromDeviceEvents(isNew: Boolean, isVideo: Boolean, uri: Uri?) {
        if (uri == null) {
            AppLog.e(T.MEDIA, "Cannot track new media events if both path and mediaURI are null!!")
            return
        }

        val properties = AnalyticsUtils.getMediaProperties(context, isVideo, uri, null)
        val currentStat: Stat = if (isVideo) {
            if (isNew) {
                Stat.EDITOR_ADDED_VIDEO_NEW
            } else {
                Stat.EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY
            }
        } else {
            if (isNew) {
                Stat.EDITOR_ADDED_PHOTO_NEW
            } else {
                Stat.EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY
            }
        }

        AnalyticsUtils.trackWithSiteDetails(currentStat, site, properties)
    }

    fun cancel() {
        job.cancel()
    }

    sealed class AddMediaToEditorUiState(
        val editorOverlayVisibility: Boolean,
        val blockingProgressDialogVisibility: Boolean
    ) {
        object AddingMultipleMedia : AddMediaToEditorUiState(true, true)
        object AddingSingleMedia : AddMediaToEditorUiState(true, false)
        object AddingMediaCompleted : AddMediaToEditorUiState(false, false)
    }
}
