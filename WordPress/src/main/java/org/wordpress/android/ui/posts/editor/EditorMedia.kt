package org.wordpress.android.ui.posts.editor

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.util.ArrayMap
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
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

class EditorMedia(private val activity: AppCompatActivity, private val site: SiteModel) {
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
        for (i in uriList.indices) {
            val mediaUri = uriList[i] ?: continue
            if (!MediaUtils.isInMediaStore(mediaUri)) {
                // Do not download the file in async task. See
                // https://github.com/wordpress-mobile/WordPress-Android/issues/5818
                var fetchedUri: Uri? = null
                try {
                    fetchedUri = MediaUtils.downloadExternalMedia(activity, mediaUri)
                } catch (e: IllegalStateException) {
                    // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
                    AppLog.e(AppLog.T.UTILS, "Can't download the image at: $mediaUri", e)
                    CrashLoggingUtils
                            .logException(
                                    e,
                                    AppLog.T.MEDIA,
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
    private inner class AddMediaListThread : Thread {
        private val mUriList = ArrayList<Uri>()
        private val mIsNew: Boolean
        @Suppress("DEPRECATION")
        private var mProgressDialog: ProgressDialog? = null
        private var mDidAnyFail: Boolean = false
        private var mFinishedUploads = 0
        private var mAllowMultipleSelection = false
        private val mediaMap = ArrayMap<String, MediaFile>()

        internal constructor(uriList: List<Uri>, isNew: Boolean) {
            this.mUriList.addAll(uriList)
            this.mIsNew = isNew
            showOverlay(false)
        }

        internal constructor(uriList: List<Uri>, isNew: Boolean, allowMultipleSelection: Boolean) {
            this.mUriList.addAll(uriList)
            this.mIsNew = isNew
            this.mAllowMultipleSelection = allowMultipleSelection
            showOverlay(false)
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
            val shouldShowProgress = mUriList.size > 2
            if (shouldShowProgress) {
                showProgressDialog(true)
            }
            try {
                for (mediaUri in mUriList) {
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
                    hideOverlay()
                    if (mDidAnyFail) {
                        ToastUtils.showToast(activity, R.string.gallery_error, Duration.SHORT)
                    }
                }
            }
        }

        private fun processMedia(mediaUri: Uri?): Boolean {
            var mediaUri: Uri? = mediaUri ?: return false

            val path = MediaUtils.getRealPathFromURI(activity, mediaUri!!) ?: return false

            val isVideo = MediaUtils.isVideo(mediaUri.toString())
            val optimizedMedia = WPMediaUtils.getOptimizedMedia(activity, path, isVideo)
            if (optimizedMedia != null) {
                mediaUri = optimizedMedia
            } else {
                // Fix for the rotation issue https://github.com/wordpress-mobile/WordPress-Android/issues/5737
                if (!site.isWPCom) {
                    // If it's not wpcom we must rotate the picture locally
                    val rotatedMedia = WPMediaUtils.fixOrientationIssue(activity, path, isVideo)
                    if (rotatedMedia != null) {
                        mediaUri = rotatedMedia
                    }
                }
            }

            if (isInterrupted) {
                return false
            }

            trackAddMediaFromDeviceEvents(mIsNew, isVideo, mediaUri)
            postProcessMedia(mediaUri, path)

            return true
        }

        private fun postProcessMedia(mediaUri: Uri, path: String) {
            if (mAllowMultipleSelection) {
                val mediaFile = getMediaFile(mediaUri)
                if (mediaFile != null) {
                    mediaMap[path] = mediaFile
                }
                mFinishedUploads++
                if (mUriList.size == mFinishedUploads) {
                    activity.runOnUiThread { mEditorFragment.appendMediaFiles(mediaMap) }
                }
            } else {
                activity.runOnUiThread { addMediaVisualEditor(mediaUri, path) }
            }
        }
    }

    private fun addMediaVisualEditor(uri: Uri, path: String) {
        val mediaFile = getMediaFile(uri)
        if (mediaFile != null) {
            mEditorFragment.appendMediaFile(mediaFile, path, mImageLoader)
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
}
