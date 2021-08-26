package org.wordpress.android.ui.reader.adapters

import org.wordpress.android.ui.reader.adapters.TrainOfFacesViewType.BLOGGERS_LIKING_TEXT
import org.wordpress.android.ui.reader.adapters.TrainOfFacesViewType.FACE

sealed class TrainOfFacesItem(val type: TrainOfFacesViewType) {
    data class FaceItem(val userId: Long, val userAvatarUrl: String) : TrainOfFacesItem(FACE)
    data class BloggersLikingTextItem(
        val text: String,
        val closure: String
    ) : TrainOfFacesItem(BLOGGERS_LIKING_TEXT)
}

enum class TrainOfFacesViewType {
    FACE,
    BLOGGERS_LIKING_TEXT
}
