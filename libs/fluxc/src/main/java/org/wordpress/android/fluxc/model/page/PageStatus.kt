package org.wordpress.android.fluxc.model.page

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus

enum class PageStatus {
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
                else -> throw IllegalArgumentException("Unexpected page status: ${status.name}")
            }
        }
    }

    fun toPostStatus(): PostStatus {
        return when (this) {
            PUBLISHED -> PostStatus.PUBLISHED
            DRAFT -> PostStatus.DRAFT
            TRASHED -> PostStatus.TRASHED
            SCHEDULED -> PostStatus.SCHEDULED
        }
    }
}
