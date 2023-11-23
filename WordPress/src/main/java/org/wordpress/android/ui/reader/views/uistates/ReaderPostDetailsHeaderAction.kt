package org.wordpress.android.ui.reader.views.uistates

sealed interface ReaderPostDetailsHeaderAction {
    data object BlogSectionClicked : ReaderPostDetailsHeaderAction
    data object FollowClicked : ReaderPostDetailsHeaderAction
    data class TagItemClicked(val tagSlug: String) : ReaderPostDetailsHeaderAction
    data object LikesClicked : ReaderPostDetailsHeaderAction
    data object CommentsClicked : ReaderPostDetailsHeaderAction
}
