package org.wordpress.android.ui.avatars

import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.AVATAR
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.TRAILING_LABEL
import org.wordpress.android.ui.utils.UiString

@DimenRes
const val AVATAR_LEFT_OFFSET_DIMEN = R.dimen.margin_small_medium
@DimenRes
const val AVATAR_SIZE_DIMEN = R.dimen.avatar_sz_small

sealed class TrainOfAvatarsItem(val type: TrainOfAvatarsViewType) {
    data class AvatarItem(val userAvatarUrl: String) : TrainOfAvatarsItem(AVATAR)
    data class TrailingLabelTextItem(val text: UiString, @AttrRes val labelColor: Int) : TrainOfAvatarsItem(
        TRAILING_LABEL
    )
}

enum class TrainOfAvatarsViewType {
    AVATAR,
    TRAILING_LABEL
}
