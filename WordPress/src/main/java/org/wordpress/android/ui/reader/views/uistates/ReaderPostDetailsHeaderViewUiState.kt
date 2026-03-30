package org.wordpress.android.ui.reader.views.uistates

import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.utils.UiString

data class ReaderPostDetailsHeaderUiState(
    val title: UiString?,
    val authorName: String?,
    val tagItems: List<TagUiState>,
    val tagItemsVisibility: Boolean,
    val blogSectionUiState: ReaderBlogSectionUiState,
    val followButtonUiState: FollowButtonUiState,
    val dateLine: String,
    val readingTime: UiString? = null,
    val blogDescription: UiString? = null,
    val featuredImageUiState: ReaderFeaturedImageUiState? = null,
    val onFeaturedImageClicked: ((Long, String) -> Unit)? = null,
    val interactionSectionUiState: InteractionSectionUiState,
)

data class ReaderFeaturedImageUiState(
    val blogId: Long,
    val url: String,
)
