package org.wordpress.android.ui.posts

import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE

enum class PostListType(val postStatuses: List<PostStatus>) {
    PUBLISHED(listOf(PostStatus.PUBLISHED, PostStatus.PRIVATE)),
    DRAFTS(listOf(PostStatus.DRAFT, PostStatus.PENDING)),
    SCHEDULED(listOf(PostStatus.SCHEDULED)),
    TRASHED(listOf(PostStatus.TRASHED));

    val titleResId: Int
        get() = when (this) {
            PUBLISHED -> string.post_list_published
            DRAFTS -> string.post_list_drafts
            SCHEDULED -> string.post_list_scheduled
            TRASHED -> string.post_list_trashed
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
