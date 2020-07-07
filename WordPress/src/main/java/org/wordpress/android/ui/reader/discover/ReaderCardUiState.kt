package org.wordpress.android.ui.reader.discover

import android.text.Spanned
import android.view.View
import androidx.annotation.AttrRes
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
        val moreMenuVisibility: Boolean,
        val photoFrameVisibility: Boolean,
        val bookmarkAction: ActionUiState,
        val likeAction: ActionUiState,
        val reblogAction: ActionUiState,
        val commentsAction: ActionUiState,
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

        data class ActionUiState(
            val isEnabled: Boolean,
            val isSelected: Boolean = false,
            val contentDescription: UiString? = null,
            val count: Int = 0,
            val onClicked: ((Long, Long, Boolean) -> Unit)? = null
        )
    }
}
