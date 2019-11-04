package org.wordpress.android.ui.posts.editor

import android.content.Context
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddExistingMediaSource
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsUtils
import javax.inject.Inject

@Reusable
class EditorTracker @Inject constructor(private val context: Context) {
    /**
     * Analytics about media from device
     *
     * @param isNew Whether is a fresh media
     * @param isVideo Whether is a video or not
     * @param uri The URI of the media on the device, or null
     */
    fun trackAddMediaFromDevice(
        site: SiteModel,
        isNew: Boolean,
        isVideo: Boolean,
        uri: Uri?
    ) {
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

    /**
     * Analytics about media already available in the blog's library.
     * @param source where the media is being added from
     * @param isVideo if media is a video
     */
    fun trackAddMediaEvent(site: SiteModel, source: AddExistingMediaSource, isVideo: Boolean) {
        val stat = when (source) {
            AddExistingMediaSource.WP_MEDIA_LIBRARY -> if (isVideo) {
                Stat.EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY
            } else {
                Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY
            }
            AddExistingMediaSource.STOCK_PHOTO_LIBRARY -> Stat.EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY
        }
        AnalyticsUtils.trackWithSiteDetails(stat, site, null)
    }
}
