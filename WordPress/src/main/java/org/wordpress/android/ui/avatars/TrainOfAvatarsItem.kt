package org.wordpress.android.ui.avatars

import androidx.annotation.DimenRes
import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.AVATAR
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.TRAILING_LABEL
import org.wordpress.android.ui.utils.UiString

@DimenRes
var AVATAR_LEFT_OFFSET_DIMEN = R.dimen.margin_small_medium

@DimenRes
var AVATAR_SIZE_DIMEN = R.dimen.avatar_sz_small

sealed class TrainOfAvatarsItem(val type: TrainOfAvatarsViewType) {
    data class AvatarItem(val userAvatarUrl: String) : TrainOfAvatarsItem(AVATAR)
    data class TrailingLabelTextItem(val text: UiString, val labelColor: Int) : TrainOfAvatarsItem(
        TRAILING_LABEL
    )
}

enum class TrainOfAvatarsViewType {
    AVATAR,
    TRAILING_LABEL
}
