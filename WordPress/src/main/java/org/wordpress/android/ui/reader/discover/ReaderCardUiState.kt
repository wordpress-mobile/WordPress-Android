package org.wordpress.android.ui.reader.discover

import android.text.Spanned
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.reader.models.ReaderImageList
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.utils.UiDimen
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.image.ImageType

sealed class ReaderCardUiState {
    data class ReaderWelcomeBannerCardUiState(@StringRes val titleRes: Int) : ReaderCardUiState()

    data class ReaderPostUiState(
        val postId: Long,
        val blogId: Long,
        val blogSection: ReaderBlogSectionUiState,
        val title: UiString?,
        val excerpt: String?, // mTxtText
        val tagItems: List<TagUiState>,
        val photoTitle: String?,
        val featuredImageUrl: String?,
        val featuredImageCornerRadius: UiDimen,
        val fullVideoUrl: String?,
        val thumbnailStripSection: GalleryThumbnailStripData?,
        val discoverSection: DiscoverLayoutUiState?,
        val expandableTagsViewVisibility: Boolean,
        val videoOverlayVisibility: Boolean,
        val featuredImageVisibility: Boolean,
        val moreMenuVisibility: Boolean,
        val photoFrameVisibility: Boolean,
        val bookmarkAction: PrimaryAction,
        val likeAction: PrimaryAction,
        val reblogAction: PrimaryAction,
        val commentsAction: PrimaryAction,
        val moreMenuItems: List<SecondaryAction>? = null,
        val onItemClicked: (Long, Long) -> Unit,
        val onItemRendered: (ReaderCardUiState) -> Unit,
        val onMoreButtonClicked: (ReaderPostUiState) -> Unit,
        val onMoreDismissed: (ReaderPostUiState) -> Unit,
        val onVideoOverlayClicked: (Long, Long) -> Unit
    ) : ReaderCardUiState() {
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

    data class ReaderInterestsCardUiState(val interest: List<ReaderInterestUiState>) : ReaderCardUiState() {
        data class ReaderInterestUiState(
            val interest: String,
            val onClicked: ((String) -> Unit),
            val chipStyle: ChipStyle
        )

        sealed class ChipStyle(
            @ColorRes val chipStrokeColorResId: Int,
            @ColorRes val chipFontColorResId: Int,
            @ColorRes val chipFillColorResId: Int
        ) {
            object ChipStyleGreen : ChipStyle(
                    chipStrokeColorResId = R.color.green_5,
                    chipFontColorResId = R.color.green_50,
                    chipFillColorResId = R.color.green_0
            )
            object ChipStyleBlue : ChipStyle(
                    chipStrokeColorResId = R.color.blue_5,
                    chipFontColorResId = R.color.blue_50,
                    chipFillColorResId = R.color.blue_0
            )
            object ChipStyleYellow : ChipStyle(
                    chipStrokeColorResId = R.color.yellow_5,
                    chipFontColorResId = R.color.yellow_50,
                    chipFillColorResId = R.color.yellow_0
            )
            object ChipStyleOrange : ChipStyle(
                    chipStrokeColorResId = R.color.orange_5,
                    chipFontColorResId = R.color.orange_50,
                    chipFillColorResId = R.color.orange_0
            )
        }
    }

    enum class ReaderInterestChipStyleColor(val id: Int) {
        GREEN(0),
        BLUE(1),
        YELLOW(2),
        ORANGE(3)
    }

    data class ReaderRecommendedBlogsCardUiState(
        val blogs: List<ReaderRecommendedBlogUiState>
    ) : ReaderCardUiState() {
        data class ReaderRecommendedBlogUiState(
            val name: String,
            val url: String,
            val blogId: Long,
            val feedId: Long,
            val description: String?,
            val iconUrl: String?,
            val isFollowed: Boolean,
            val onItemClicked: (Long, Long) -> Unit,
            val onFollowClicked: (ReaderRecommendedBlogUiState) -> Unit
        ) {
            val followContentDescription: UiStringRes by lazy {
                when (isFollowed) {
                    true -> R.string.reader_btn_unfollow
                    false -> R.string.reader_btn_follow
                }.let(::UiStringRes)
            }
        }
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
    COMMENTS,
    REPORT_POST,
    TOGGLE_SEEN_STATUS
}
