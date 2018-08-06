package org.wordpress.android.models.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus

enum class PageStatus {
    PUBLISHED,
    DRAFT,
    TRASHED,
    PENDING,
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
                PostStatus.PENDING -> PENDING
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
            PENDING -> PostStatus.PENDING
            SCHEDULED -> PostStatus.SCHEDULED
        }
    }
}
