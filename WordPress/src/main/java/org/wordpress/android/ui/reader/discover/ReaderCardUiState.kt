package org.wordpress.android.ui.reader.discover

import android.text.Spanned
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.models.ReaderImageList
import org.wordpress.android.ui.utils.UiDimen
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.image.ImageType

sealed class ReaderCardUiState {
    data class ReaderPostUiState(
        val postId: Long,
        val blogId: Long,
        val dateLine: String,
        val title: String?,
        val blogName: String?,
        val excerpt: String?, // mTxtText
        val blogUrl: String?,
        val photoTitle: String?,
        val featuredImageUrl: String?,
        val featuredImageCornerRadius: UiDimen,
        val fullVideoUrl: String?,
        val avatarOrBlavatarUrl: String?,
        val thumbnailStripSection: GalleryThumbnailStripData?,
        val discoverSection: DiscoverLayoutUiState?,
        val videoOverlayVisibility: Boolean,
        val featuredImageVisibility: Boolean,
        val moreMenuVisibility: Boolean,
        val photoFrameVisibility: Boolean,
        val bookmarkAction: PrimaryAction,
        val likeAction: PrimaryAction,
        val reblogAction: PrimaryAction,
        val commentsAction: PrimaryAction,
        val moreMenuItems: List<SecondaryAction>,
        val postHeaderClickData: PostHeaderClickData?,
        val onItemClicked: (Long, Long) -> Unit,
        val onItemRendered: (Long, Long) -> Unit,
        val onMoreButtonClicked: (Long, Long, View) -> Unit,
        val onVideoOverlayClicked: (Long, Long) -> Unit
    ) : ReaderCardUiState() {
        val dotSeparatorVisibility: Boolean = blogUrl != null

        data class PostHeaderClickData(
            val onPostHeaderViewClicked: ((Long, Long) -> Unit)?,
            @AttrRes val background: Int
        )

        data class GalleryThumbnailStripData(
            val images: ReaderImageList,
            val isPrivate: Boolean,
            val content: String // needs to be here as it's required by ReaderThumbnailStrip
        )

        data class DiscoverLayoutUiState(
            val discoverText: Spanned,
            val discoverAvatarUrl: String,
            val imageType: ImageType,
            val onDiscoverClicked: ((Long, Long) -> Unit)
        )
    }
}

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
}

enum class ReaderPostCardActionType {
    FOLLOW,
    SITE_NOTIFICATIONS,
    SHARE,
    VISIT_SITE,
    BLOCK_SITE,
    LIKE,
    BOOKMARK,
    REBLOG,
    COMMENTS
}
