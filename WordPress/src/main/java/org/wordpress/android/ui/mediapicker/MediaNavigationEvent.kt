package org.wordpress.android.ui.mediapicker

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction
import org.wordpress.android.util.UriWrapper

sealed class MediaNavigationEvent {
    data class PreviewUrl(val url: String) : MediaNavigationEvent()
    data class PreviewMedia(val media: MediaModel) : MediaNavigationEvent()
    data class EditMedia(val uris: List<UriWrapper>) : MediaNavigationEvent()
    data class InsertMedia(val identifiers: List<Identifier>) : MediaNavigationEvent()
    data class IconClickEvent(val action: MediaPickerAction) : MediaNavigationEvent()
    object Exit : MediaNavigationEvent()
}
