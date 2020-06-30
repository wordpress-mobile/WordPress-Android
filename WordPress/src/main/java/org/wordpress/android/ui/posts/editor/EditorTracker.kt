package org.wordpress.android.ui.posts.editor

import android.content.Context
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddExistingMediaSource
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import javax.inject.Inject

@Reusable
class EditorTracker @Inject constructor(
    private val context: Context,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
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

    @JvmOverloads
    fun trackEditorEvent(event: TrackableEvent, editorName: String, properties: MutableMap<String, String> = mutableMapOf()) {
        val currentStat = when (event) {
            TrackableEvent.BOLD_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_BOLD
            TrackableEvent.BLOCKQUOTE_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_BLOCKQUOTE
            TrackableEvent.ELLIPSIS_COLLAPSE_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_ELLIPSIS_COLLAPSE
            TrackableEvent.ELLIPSIS_EXPAND_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_ELLIPSIS_EXPAND
            TrackableEvent.HEADING_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HEADING
            TrackableEvent.HEADING_1_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HEADING_1
            TrackableEvent.HEADING_2_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HEADING_2
            TrackableEvent.HEADING_3_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HEADING_3
            TrackableEvent.HEADING_4_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HEADING_4
            TrackableEvent.HEADING_5_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HEADING_5
            TrackableEvent.HEADING_6_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HEADING_6
            TrackableEvent.HORIZONTAL_RULE_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HORIZONTAL_RULE
            TrackableEvent.FORMAT_ALIGN_LEFT_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_ALIGN_LEFT
            TrackableEvent.FORMAT_ALIGN_CENTER_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_ALIGN_CENTER
            TrackableEvent.FORMAT_ALIGN_RIGHT_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_ALIGN_RIGHT
            TrackableEvent.HTML_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_HTML
            TrackableEvent.IMAGE_EDITED -> Stat.EDITOR_EDITED_IMAGE
            TrackableEvent.ITALIC_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_ITALIC
            TrackableEvent.LINK_ADDED_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_LINK_ADDED
            TrackableEvent.LIST_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_LIST
            TrackableEvent.LIST_ORDERED_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_LIST_ORDERED
            TrackableEvent.LIST_UNORDERED_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_LIST_UNORDERED
            TrackableEvent.MEDIA_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_IMAGE
            TrackableEvent.NEXT_PAGE_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_NEXT_PAGE
            TrackableEvent.PARAGRAPH_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_PARAGRAPH
            TrackableEvent.PREFORMAT_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_PREFORMAT
            TrackableEvent.READ_MORE_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_READ_MORE
            TrackableEvent.STRIKETHROUGH_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_STRIKETHROUGH
            TrackableEvent.UNDERLINE_BUTTON_TAPPED -> Stat.EDITOR_TAPPED_UNDERLINE
            TrackableEvent.REDO_TAPPED -> Stat.EDITOR_TAPPED_REDO
            TrackableEvent.UNDO_TAPPED -> Stat.EDITOR_TAPPED_UNDO
            TrackableEvent.EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_CLOSED -> Stat.EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_CLOSED
            TrackableEvent.EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_SHOWN -> Stat.EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_SHOWN
        }

        if (properties.isEmpty()) {
            analyticsTrackerWrapper.track(currentStat, mapOf("editor" to editorName))
        } else {
            properties.put("editor", editorName)
            analyticsTrackerWrapper.track(currentStat, properties)
        }
    }
}
