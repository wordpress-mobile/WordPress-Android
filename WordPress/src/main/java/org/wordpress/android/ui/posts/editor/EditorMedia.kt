package org.wordpress.android.ui.posts.editor

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.ArrayMap
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.helpers.MediaFile
import java.util.ArrayList

interface EditorMediaListener {
    fun showOverlay(animate: Boolean)
    fun hideOverlay(animate: Boolean)
    fun appendMediaFiles(mediaMap: ArrayMap<String, MediaFile>)
    fun appendMediaFile(mediaFile: MediaFile, imageUrl: String)
}

class EditorMedia(
    private val activity: AppCompatActivity,
    private val site: SiteModel,
    private val editorMediaListener: EditorMediaListener,
    private val mediaStore: MediaStore
) {
    private var mAddMediaListThread: AddMediaListThread? = null
    private var mAllowMultipleSelection: Boolean = false

    enum class AddExistingdMediaSource {
        WP_MEDIA_LIBRARY,
        STOCK_PHOTO_LIBRARY
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

        val properties = AnalyticsUtils.getMediaProperties(activity, isVideo, uri, null)
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

    /**
     * Analytics about media already available in the blog's library.
     * @param source where the media is being added from
     * @param media media being added
     */
    private fun trackAddMediaEvent(source: AddExistingdMediaSource, media: MediaModel) {
        val stat = when (source) {
            AddExistingdMediaSource.WP_MEDIA_LIBRARY -> if (media.isVideo) {
                Stat.EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY
            } else {
                Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY
            }
            AddExistingdMediaSource.STOCK_PHOTO_LIBRARY -> Stat.EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY
        }
        AnalyticsUtils.trackWithSiteDetails(
                stat,
                site,
                null
        )
    }

    @Suppress("SameParameterValue")
    private fun addMedia(mediaUri: Uri?, isNew: Boolean): Boolean {
        mediaUri?.let {
            addMediaList(listOf(it), isNew)
            return true
        }
        return false
    }

    private fun addMediaList(uriList: List<Uri>, isNew: Boolean) {
        // fetch any shared media first - must be done on the main thread
        val fetchedUriList = fetchMediaList(uriList)
        mAddMediaListThread = AddMediaListThread(fetchedUriList, isNew, mAllowMultipleSelection)
        mAddMediaListThread!!.start()
    }

    private fun cancelAddMediaListThread() {
        if (mAddMediaListThread != null && !mAddMediaListThread!!.isInterrupted) {
            try {
                mAddMediaListThread!!.interrupt()
            } catch (e: SecurityException) {
                AppLog.e(T.MEDIA, e)
            }
        }
    }

    /*
     * called before we add media to make sure we have access to any media shared from another app (Google Photos, etc.)
     */
    private fun fetchMediaList(uriList: List<Uri>): List<Uri> {
        var didAnyFail = false
        val fetchedUriList = ArrayList<Uri>()
        for (mediaUri in uriList) {
            if (!MediaUtils.isInMediaStore(mediaUri)) {
                // Do not download the file in async task. See
                // https://github.com/wordpress-mobile/WordPress-Android/issues/5818
                var fetchedUri: Uri? = null
                try {
                    fetchedUri = MediaUtils.downloadExternalMedia(activity, mediaUri)
                } catch (e: IllegalStateException) {
                    // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
                    AppLog.e(T.UTILS, "Can't download the image at: $mediaUri", e)
                    CrashLoggingUtils
                            .logException(
                                    e,
                                    T.MEDIA,
                                    "Can't download the image at: " + mediaUri.toString()
                                            + " See issue #5823"
                            )
                    didAnyFail = true
                }

                if (fetchedUri != null) {
                    fetchedUriList.add(fetchedUri)
                } else {
                    didAnyFail = true
                }
            } else {
                fetchedUriList.add(mediaUri)
            }
        }

        if (didAnyFail) {
            ToastUtils.showToast(activity, R.string.error_downloading_image, Duration.SHORT)
        }

        return fetchedUriList
    }

    /*
     * processes a list of media in the background (optimizing, resizing, etc.) and adds them to
     * the editor one at a time
     */
    private inner class AddMediaListThread(
        private val uriList: List<Uri>,
        private val isNew: Boolean,
        private val allowMultipleSelection: Boolean = false
    ) : Thread() {
        @Suppress("DEPRECATION")
        private var mProgressDialog: ProgressDialog? = null
        private var mDidAnyFail: Boolean = false
        private var mFinishedUploads = 0
        private val mediaMap = ArrayMap<String, MediaFile>()

        init {
            editorMediaListener.showOverlay(false)
        }

        private fun showProgressDialog(show: Boolean) {
            activity.runOnUiThread {
                try {
                    if (show) {
                        mProgressDialog = ProgressDialog(activity)
                        mProgressDialog!!.setCancelable(false)
                        mProgressDialog!!.isIndeterminate = true
                        mProgressDialog!!.setMessage(activity.getString(R.string.add_media_progress))
                        mProgressDialog!!.show()
                    } else if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                        mProgressDialog!!.dismiss()
                    }
                } catch (e: IllegalArgumentException) {
                    AppLog.e(T.MEDIA, e)
                }
            }
        }

        override fun run() {
            // adding multiple media items at once can take several seconds on slower devices, so we show a blocking
            // progress dialog in this situation - otherwise the user could accidentally back out of the process
            // before all items were added
            val shouldShowProgress = uriList.size > 2
            if (shouldShowProgress) {
                showProgressDialog(true)
            }
            try {
                for (mediaUri in uriList) {
                    if (isInterrupted) {
                        return
                    }
                    if (!processMedia(mediaUri)) {
                        mDidAnyFail = true
                    }
                }
            } finally {
                if (shouldShowProgress) {
                    showProgressDialog(false)
                }
            }


            activity.runOnUiThread {
                if (!isInterrupted) {
                    savePostAsync(null)
                    editorMediaListener.hideOverlay()
                    if (mDidAnyFail) {
                        ToastUtils.showToast(activity, R.string.gallery_error, Duration.SHORT)
                    }
                }
            }
        }

        private fun processMedia(mediaUri: Uri): Boolean {
            val path = MediaUtils.getRealPathFromURI(activity, mediaUri) ?: return false

            val isVideo = MediaUtils.isVideo(mediaUri.toString())
            val optimizedMedia = WPMediaUtils.getOptimizedMedia(activity, path, isVideo)
            var updatedMediaUri: Uri = mediaUri
            if (optimizedMedia != null) {
                updatedMediaUri = optimizedMedia
            } else {
                // Fix for the rotation issue https://github.com/wordpress-mobile/WordPress-Android/issues/5737
                if (!site.isWPCom) {
                    // If it's not wpcom we must rotate the picture locally
                    val rotatedMedia = WPMediaUtils.fixOrientationIssue(activity, path, isVideo)
                    if (rotatedMedia != null) {
                        updatedMediaUri = rotatedMedia
                    }
                }
            }

            if (isInterrupted) {
                return false
            }

            trackAddMediaFromDeviceEvents(isNew, isVideo, updatedMediaUri)
            postProcessMedia(updatedMediaUri, path)

            return true
        }

        private fun postProcessMedia(mediaUri: Uri, path: String) {
            if (allowMultipleSelection) {
                getMediaFile(mediaUri)?.let {
                    mediaMap[path] = it
                }
                mFinishedUploads++
                if (uriList.size == mFinishedUploads) {
                    activity.runOnUiThread { editorMediaListener.appendMediaFiles(mediaMap) }
                }
            } else {
                activity.runOnUiThread { addMediaVisualEditor(mediaUri, path) }
            }
        }
    }

    private fun addMediaVisualEditor(uri: Uri, path: String) {
        val mediaFile = getMediaFile(uri)
        if (mediaFile != null) {
            editorMediaListener.appendMediaFile(mediaFile, path)
        }
    }

    private fun getMediaFile(uri: Uri): MediaFile? {
        val media = queueFileForUpload(uri, activity.contentResolver.getType(uri))
        val mediaFile = FluxCUtils.mediaFileFromMediaModel(media)
        return if (media != null) {
            mediaFile
        } else {
            null
        }
    }

    private fun addMediaItemGroupOrSingleItem(data: Intent) {
        val clipData = data.clipData
        if (clipData != null) {
            val uriList = ArrayList<Uri>()
            for (i in 0 until clipData.itemCount) {
                val item = clipData.getItemAt(i)
                uriList.add(item.uri)
            }
            addMediaList(uriList, false)
        } else {
            addMedia(data.data, false)
        }
    }

    private fun advertiseImageOptimisationAndAddMedia(data: Intent) {
        if (WPMediaUtils.shouldAdvertiseImageOptimization(activity)) {
            WPMediaUtils.advertiseImageOptimization(
                    activity
            ) { addMediaItemGroupOrSingleItem(data) }
        } else {
            addMediaItemGroupOrSingleItem(data)
        }
    }

    private fun addExistingMediaToEditor(source: AddExistingdMediaSource, mediaId: Long): Boolean {
        val media = mediaStore.getSiteMediaWithId(site, mediaId)
        if (media == null) {
            AppLog.w(T.MEDIA, "Cannot add null media to post")
            return false
        }

        trackAddMediaEvent(source, media)

        val mediaFile = FluxCUtils.mediaFileFromMediaModel(media)
        editorMediaListener.appendMediaFile(mediaFile, media.urlToUse)
        return true
    }

    private fun addExistingMediaToEditor(source: AddExistingdMediaSource, mediaIdList: List<Long>) {
        val mediaMap = ArrayMap<String, MediaFile>()
        for (mediaId in mediaIdList) {
            val media = mediaStore.getSiteMediaWithId(site, mediaId)
            if (media == null) {
                AppLog.w(T.MEDIA, "Cannot add null media to post")
            } else {
                trackAddMediaEvent(source, media)

                mediaMap[media.urlToUse] = FluxCUtils.mediaFileFromMediaModel(media)
            }
        }
        editorMediaListener.appendMediaFiles(mediaMap)
    }
}

private val MediaModel.urlToUse
    get() = if (url.isNullOrBlank()) filePath else url
