package org.wordpress.android.ui.media.library

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.MimeType

/** The media library's type filters, in tab order. [mimeType] is what a fetch for the filter asks the server for. */
enum class MediaFilter(
    @StringRes val labelResId: Int,
    @StringRes val emptyTitleResId: Int,
    val mimeType: MimeType.Type?
) {
    ALL(R.string.media_all, R.string.media_empty_list, null),
    IMAGES(R.string.media_images, R.string.media_empty_image_list, MimeType.Type.IMAGE),
    DOCUMENTS(R.string.media_documents, R.string.media_empty_documents_list, MimeType.Type.APPLICATION),
    VIDEOS(R.string.media_videos, R.string.media_empty_videos_list, MimeType.Type.VIDEO),
    AUDIO(R.string.media_audio, R.string.media_empty_audio_list, MimeType.Type.AUDIO);

    companion object {
        fun fromMimeType(mimeType: MimeType.Type?): MediaFilter = entries.firstOrNull { it.mimeType == mimeType } ?: ALL
    }
}
