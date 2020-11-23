package org.wordpress.android.ui.reader.views.uistates

import androidx.annotation.AttrRes
import org.wordpress.android.ui.utils.UiString

data class ReaderBlogSectionUiState(
    val postId: Long,
    val blogId: Long,
    val dateLine: String,
    val blogName: UiString,
    val blogUrl: String?,
    val avatarOrBlavatarUrl: String?,
    val blogSectionClickData: ReaderBlogSectionClickData?
) {
    data class ReaderBlogSectionClickData(
        val onBlogSectionClicked: ((Long, Long) -> Unit)?,
        @AttrRes val background: Int
    )
    val dotSeparatorVisibility: Boolean = blogUrl != null
}
