package org.wordpress.android.ui.posts.editor

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus

enum class SecondaryEditorAction(@StringRes val titleResource: Int?, val isVisible: Boolean) {
    SAVE_AS_DRAFT(R.string.menu_save_as_draft, isVisible = true),
    SAVE(R.string.save, isVisible = true),
    PUBLISH_NOW(R.string.menu_publish_now, isVisible = true),
    NONE(titleResource = null, isVisible = false);

    companion object {
        @JvmStatic
        fun getSecondaryAction(
            postStatus: PostStatus,
            userCanPublish: Boolean
        ): SecondaryEditorAction {
            return if (userCanPublish) {
                when (postStatus) {
                    PostStatus.DRAFT -> SAVE
                    PostStatus.PENDING, PostStatus.SCHEDULED -> PUBLISH_NOW
                    PostStatus.PRIVATE, PostStatus.PUBLISHED -> NONE
                    PostStatus.TRASHED, PostStatus.UNKNOWN -> SAVE_AS_DRAFT
                }
            } else {
                // User doesn't have publishing permissions
                when (postStatus) {
                    PostStatus.SCHEDULED,
                    PostStatus.DRAFT,
                    PostStatus.PENDING,
                    PostStatus.PRIVATE,
                    PostStatus.PUBLISHED,
                    PostStatus.UNKNOWN -> NONE
                    PostStatus.TRASHED -> SAVE_AS_DRAFT
                }
            }
        }
    }
}
