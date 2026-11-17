package org.wordpress.android.ui.reader.discover

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SPACER_NO_ACTION
import org.wordpress.android.ui.reader.models.ReaderImageList

import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.image.ImageType

sealed class ReaderCardUiState {
    // only ReaderImageList keeps the compiler from inferring this as stable, and it isn't changed once the card
    // is built - so equal cards skip recomposition after a reload instead of recomposing because they're new
    @Immutable
    data class ReaderPostUiState(
        val source: String,
        val postId: Long,
        val blogId: Long,
        val feedId: Long,
        val isFollowed: Boolean,
        val blogSection: CompactBlogSectionData,
        val interactionSection: InteractionSectionData,
        val title: UiString?,
        val excerpt: String?,
        val featuredImageUrl: String?,
        val thumbnailStripSection: GalleryThumbnailStripData?,
        val featuredImageVisibility: Boolean,
        val moreMenuVisibility: Boolean,
        val likeAction: PrimaryAction,
        val reblogAction: PrimaryAction,
        val commentsAction: PrimaryAction,
        val moreMenuItems: List<ReaderPostCardAction>? = null,
        val onItemClicked: (Long, Long) -> Unit,
        val onItemRendered: (ReaderPostUiState) -> Unit,
        val onMoreButtonClicked: (ReaderPostUiState) -> Unit,
        val onMoreDismissed: (ReaderPostUiState) -> Unit,
    ) : ReaderCardUiState() {
        data class CompactBlogSectionData(
            val postId: Long,
            val blogId: Long,
            val dateLine: String,
            val blogName: UiString,
            val avatarOrBlavatarUrl: String?,
            val authorAvatarUrl: String?,
            val isAuthorAvatarVisible: Boolean,
            val blavatarType: ImageType,
            val onClicked: ((Long, Long) -> Unit)?,
        )

        data class InteractionSectionData(
            val likeCount: Int,
            val commentCount: Int,
        )

        data class GalleryThumbnailStripData(
            val images: ReaderImageList,
            val isPrivate: Boolean,
            val content: String // the photo viewer needs it to build the gallery
        )
    }
}

data class ReaderPostActions(
    val likeAction: PrimaryAction,
    val reblogAction: PrimaryAction,
    val commentsAction: PrimaryAction,
    val bookmarkAction: PrimaryAction
)

sealed class ReaderPostCardAction {
    abstract val type: ReaderPostCardActionType
    open val onClicked: ((Long, Long, ReaderPostCardActionType) -> Unit)? = null
    open val isSelected: Boolean = false

    data class PrimaryAction(
        val isEnabled: Boolean,
        val contentDescription: UiString? = null,
        val count: Int = 0,
        override val isSelected: Boolean = false,
        override val type: ReaderPostCardActionType,
        override val onClicked: ((Long, Long, ReaderPostCardActionType) -> Unit)? = null
    ) : ReaderPostCardAction()

    data class SecondaryAction(
        val label: UiString,
        @AttrRes val labelColor: Int,
        @DrawableRes val iconRes: Int,
        @AttrRes val iconColor: Int = labelColor,
        override val isSelected: Boolean = false,
        override val type: ReaderPostCardActionType,
        override val onClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ) : ReaderPostCardAction()

    data class SpacerNoAction(
        override val isSelected: Boolean = false,
        override val type: ReaderPostCardActionType = SPACER_NO_ACTION,
        override val onClicked: ((Long, Long, ReaderPostCardActionType) -> Unit)? = null
    ) : ReaderPostCardAction()
}

enum class ReaderPostCardActionType {
    FOLLOW,
    SITE_NOTIFICATIONS,
    SHARE,
    VISIT_SITE,
    BLOCK_SITE,
    BLOCK_USER,
    LIKE,
    BOOKMARK,
    REBLOG,
    COMMENTS,
    REPORT_POST,
    REPORT_USER,
    TOGGLE_SEEN_STATUS,
    SPACER_NO_ACTION,
    READING_PREFERENCES,
}
