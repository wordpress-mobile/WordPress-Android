package org.wordpress.android.ui.avatars

import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.AVATAR
import org.wordpress.android.ui.avatars.TrainOfAvatarsViewType.TRAILING_LABEL
import org.wordpress.android.ui.utils.UiString

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
