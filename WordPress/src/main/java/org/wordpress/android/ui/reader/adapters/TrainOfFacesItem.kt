package org.wordpress.android.ui.reader.adapters

import androidx.annotation.DimenRes
import org.wordpress.android.R
import org.wordpress.android.ui.reader.adapters.TrainOfFacesViewType.BLOGGERS_LIKING_TEXT
import org.wordpress.android.ui.reader.adapters.TrainOfFacesViewType.FACE
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams

@DimenRes const val FACE_ITEM_LEFT_OFFSET_DIMEN = R.dimen.margin_small_medium
@DimenRes const val FACE_ITEM_AVATAR_SIZE_DIMEN = R.dimen.avatar_sz_small

sealed class TrainOfFacesItem(val type: TrainOfFacesViewType) {
    data class FaceItem(val userId: Long, val userAvatarUrl: String) : TrainOfFacesItem(FACE)
    data class BloggersLikingTextItem(
        val textWithParams: UiStringResWithParams,
        val underlineDelimiterClosure: UiStringRes
    ) : TrainOfFacesItem(BLOGGERS_LIKING_TEXT)
}

enum class TrainOfFacesViewType {
    FACE,
    BLOGGERS_LIKING_TEXT
}
