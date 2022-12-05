package org.wordpress.android.ui.reader.discover

import android.text.Spanned
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SPACER_NO_ACTION
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
        val source: String,
        val postId: Long,
        val blogId: Long,
        val feedId: Long,
        val isFollowed: Boolean,
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
        val moreMenuItems: List<ReaderPostCardAction>? = null,
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
                    chipStrokeColorResId = R.color.reader_topics_you_might_like_chip_green_stroke,
                    chipFontColorResId = R.color.reader_topics_you_might_like_chip_green_font,
                    chipFillColorResId = R.color.reader_topics_you_might_like_chip_green_fill
            )
            object ChipStylePurple : ChipStyle(
                    chipStrokeColorResId = R.color.reader_topics_you_might_like_chip_purple_stroke,
                    chipFontColorResId = R.color.reader_topics_you_might_like_chip_purple_font,
                    chipFillColorResId = R.color.reader_topics_you_might_like_chip_purple_fill
            )
            object ChipStyleYellow : ChipStyle(
                    chipStrokeColorResId = R.color.reader_topics_you_might_like_chip_yellow_stroke,
                    chipFontColorResId = R.color.reader_topics_you_might_like_chip_yellow_font,
                    chipFillColorResId = R.color.reader_topics_you_might_like_chip_yellow_fill
            )
            object ChipStyleOrange : ChipStyle(
                    chipStrokeColorResId = R.color.reader_topics_you_might_like_chip_orange_stroke,
                    chipFontColorResId = R.color.reader_topics_you_might_like_chip_orange_font,
                    chipFillColorResId = R.color.reader_topics_you_might_like_chip_orange_fill
            )
        }
    }

    enum class ReaderInterestChipStyleColor(val id: Int) {
        GREEN(0),
        PURPLE(1),
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
            val onItemClicked: (Long, Long, Boolean) -> Unit,
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
    SPACER_NO_ACTION
}
