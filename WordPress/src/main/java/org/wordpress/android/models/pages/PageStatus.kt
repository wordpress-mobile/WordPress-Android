package org.wordpress.android.models.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus

enum class PageStatus {
    UNKNOWN,
    PUBLISHED,
    DRAFT,
    TRASHED,
    SCHEDULED;

    companion object {
        fun fromPost(post: PostModel): PageStatus {
            return fromPostStatus(PostStatus.fromPost(post))
        }

        fun fromPostStatus(status: PostStatus): PageStatus {
            return when (status) {
                PostStatus.PUBLISHED -> PUBLISHED
                PostStatus.DRAFT -> DRAFT
                PostStatus.TRASHED -> TRASHED
                PostStatus.SCHEDULED -> SCHEDULED
                else -> UNKNOWN
            }
        }
    }
}
