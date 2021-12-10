package org.wordpress.android.ui.reader

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

const val FOLLOW_COMMENTS_UI_STATE_FLAGS_KEY = "follow-comments-ui-state-flags-key"

data class FollowConversationUiState(
    val flags: FollowConversationStatusFlags,
    val onFollowTapped: (() -> Unit)?,
    val onManageNotificationsTapped: () -> Unit
)

@Parcelize
data class FollowConversationStatusFlags(
    val type: FollowCommentsUiStateType,
    val isFollowing: Boolean,
    val isReceivingNotifications: Boolean,
    val isMenuEnabled: Boolean,
    val showMenuShimmer: Boolean,
    val isBellMenuVisible: Boolean,
    val isFollowMenuVisible: Boolean
) : Parcelable

enum class FollowCommentsUiStateType {
    DISABLED,
    LOADING,
    GONE,
    VISIBLE_WITH_STATE
}
