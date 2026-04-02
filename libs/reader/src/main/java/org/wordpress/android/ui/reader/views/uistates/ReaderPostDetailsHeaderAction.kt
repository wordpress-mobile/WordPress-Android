package org.wordpress.android.ui.reader.views.uistates

sealed interface ReaderPostDetailsHeaderAction {
    data object BlogSectionClicked : ReaderPostDetailsHeaderAction
    data object FollowClicked : ReaderPostDetailsHeaderAction
    data class TagItemClicked(val tagSlug: String) : ReaderPostDetailsHeaderAction
    data class FeaturedImageClicked(
        val blogId: Long,
        val featuredImageUrl: String
    ) : ReaderPostDetailsHeaderAction
    data object ViewOriginalClicked : ReaderPostDetailsHeaderAction
    data object AuthorClicked : ReaderPostDetailsHeaderAction
}
