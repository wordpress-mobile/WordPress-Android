package org.wordpress.android.ui.reader.views.uistates

import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.utils.UiString

data class ReaderPostDetailsHeaderUiState(
    val title: UiString?,
    val authorName: String?,
    val tagItems: List<TagUiState>,
    val blogSectionUiState: ReaderBlogSectionUiState,
    val followButtonUiState: FollowButtonUiState,
    val readingTime: UiString? = null,
    val excerpt: UiString? = null,
    val featuredImageUiState: ReaderFeaturedImageUiState? = null,
    val showViewOriginal: Boolean = false,
)

data class ReaderFeaturedImageUiState(
    val blogId: Long,
    val url: String,
)
