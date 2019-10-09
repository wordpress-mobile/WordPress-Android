package org.wordpress.android.ui.posts.editor

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus

enum class PrimaryEditorAction(@StringRes val titleResource: Int) {
    SUBMIT_FOR_REVIEW(R.string.submit_for_review),
    PUBLISH_NOW(R.string.button_publish),
    SCHEDULE(R.string.schedule_verb),
    UPDATE(R.string.update_verb),
    SAVE(R.string.save);

    companion object {
        @JvmStatic
        fun getPrimaryAction(postStatus: PostStatus, userCanPublish: Boolean): PrimaryEditorAction {
            return if (userCanPublish) {
                when (postStatus) {
                    PostStatus.SCHEDULED -> SCHEDULE
                    PostStatus.DRAFT -> PUBLISH_NOW
                    PostStatus.PENDING, PostStatus.TRASHED -> SAVE
                    PostStatus.PRIVATE, PostStatus.PUBLISHED, PostStatus.UNKNOWN -> UPDATE
                }
            } else {
                // User doesn't have publishing permissions
                when (postStatus) {
                    PostStatus.SCHEDULED,
                    PostStatus.DRAFT,
                    PostStatus.PENDING,
                    PostStatus.PRIVATE,
                    PostStatus.PUBLISHED,
                    PostStatus.UNKNOWN -> SUBMIT_FOR_REVIEW
                    PostStatus.TRASHED -> SAVE
                }
            }
        }
    }
}
