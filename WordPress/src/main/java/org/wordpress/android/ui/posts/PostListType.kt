package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE

enum class PostListType(val postStatuses: List<PostStatus>) {
    PUBLISHED(listOf(PostStatus.PUBLISHED, PRIVATE)),
    DRAFTS(listOf(PostStatus.DRAFT, PostStatus.PENDING)),
    SCHEDULED(listOf(PostStatus.SCHEDULED)),
    TRASHED(listOf(PostStatus.TRASHED)),
    SEARCH(listOf(
            PostStatus.DRAFT,
            PostStatus.PENDING,
            PostStatus.PUBLISHED,
            PRIVATE,
            PostStatus.SCHEDULED,
            PostStatus.TRASHED)
    );

    val titleResId: Int
        get() = when (this) {
            PUBLISHED -> R.string.post_list_published
            DRAFTS -> R.string.post_list_drafts
            SCHEDULED -> R.string.post_list_scheduled
            TRASHED -> R.string.post_list_trashed
            SEARCH -> 0 // we don't have title for search list
        }

    companion object {
        fun fromPostStatus(status: PostStatus): PostListType {
            return when (status) {
                PostStatus.PUBLISHED, PRIVATE -> PUBLISHED
                PostStatus.DRAFT, PostStatus.PENDING, PostStatus.UNKNOWN -> DRAFTS
                PostStatus.TRASHED -> TRASHED
                PostStatus.SCHEDULED -> SCHEDULED
            }
        }
    }
}
