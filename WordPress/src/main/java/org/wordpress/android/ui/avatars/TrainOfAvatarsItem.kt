package org.wordpress.android.ui.avatars

import androidx.annotation.DimenRes
import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.AVATAR
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.TRAILING_LABEL
import org.wordpress.android.ui.utils.UiString.UiStringText

@DimenRes const val AVATAR_LEFT_OFFSET_DIMEN = R.dimen.margin_small_medium
@DimenRes const val AVATAR_SIZE_DIMEN = R.dimen.avatar_sz_small

sealed class TrainOfAvatarsItem(val type: TrainOfAvatarsViewType) {
    data class AvatarItem(val userId: Long, val userAvatarUrl: String) : TrainOfAvatarsItem(AVATAR)
    data class TrailingLabelTextItem(val text: UiStringText) : TrainOfAvatarsItem(TRAILING_LABEL)
}

enum class TrainOfAvatarsViewType {
    AVATAR,
    TRAILING_LABEL
}
