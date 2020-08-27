package org.wordpress.android.ui.posts.editor

import androidx.annotation.StringRes
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

@Reusable
class EditorActionsProvider @Inject constructor() {
    fun getPrimaryAction(postStatus: PostStatus, userCanPublish: Boolean): PrimaryEditorAction {
        return if (userCanPublish) {
            when (postStatus) {
                PostStatus.SCHEDULED -> PrimaryEditorAction.SCHEDULE
                PostStatus.DRAFT -> PrimaryEditorAction.PUBLISH_NOW
                PostStatus.PENDING, PostStatus.TRASHED -> PrimaryEditorAction.SAVE
                PostStatus.PRIVATE, PostStatus.PUBLISHED, PostStatus.UNKNOWN -> PrimaryEditorAction.UPDATE
            }
        } else {
            // User doesn't have publishing permissions
            when (postStatus) {
                PostStatus.DRAFT,
                PostStatus.PENDING,
                PostStatus.UNKNOWN -> PrimaryEditorAction.SUBMIT_FOR_REVIEW
                PostStatus.TRASHED -> {
                    AppLog.e(T.EDITOR, "User shouldn't be able to open a trashed post in an editor " +
                            "without publishing rights.")
                    PrimaryEditorAction.SAVE
                }
                PostStatus.PUBLISHED,
                PostStatus.SCHEDULED,
                PostStatus.PRIVATE -> {
                    AppLog.e(T.EDITOR, "User shouldn't be able to open a public ($postStatus) post in an editor " +
                            "without publishing rights.")
                    PrimaryEditorAction.SUBMIT_FOR_REVIEW
                }
            }
        }
    }

    fun getSecondaryAction(
        postStatus: PostStatus,
        userCanPublish: Boolean
    ): SecondaryEditorAction {
        return if (userCanPublish) {
            when (postStatus) {
                PostStatus.DRAFT -> SecondaryEditorAction.SAVE
                PostStatus.PENDING, PostStatus.SCHEDULED -> SecondaryEditorAction.PUBLISH_NOW
                PostStatus.PRIVATE, PostStatus.PUBLISHED -> SecondaryEditorAction.NONE
                PostStatus.TRASHED, PostStatus.UNKNOWN -> SecondaryEditorAction.SAVE_AS_DRAFT
            }
        } else {
            // User doesn't have publishing permissions
            when (postStatus) {
                PostStatus.DRAFT,
                PostStatus.PENDING,
                PostStatus.UNKNOWN -> SecondaryEditorAction.NONE
                PostStatus.TRASHED -> {
                    AppLog.e(T.EDITOR, "User shouldn't be able to open a trashed post in an editor " +
                            "without publishing rights.")
                    SecondaryEditorAction.SAVE_AS_DRAFT
                }
                PostStatus.PUBLISHED,
                PostStatus.SCHEDULED,
                PostStatus.PRIVATE -> {
                    AppLog.e(T.EDITOR, "User shouldn't be able to open a public ($postStatus) post in an editor " +
                            "without publishing rights.")
                    SecondaryEditorAction.NONE
                }
            }
        }
    }
}

enum class PrimaryEditorAction(@StringRes val titleResource: Int) {
    SUBMIT_FOR_REVIEW(R.string.submit_for_review),
    PUBLISH_NOW(R.string.button_publish),
    SCHEDULE(R.string.schedule_verb),
    UPDATE(R.string.update_verb),
    SAVE(R.string.save);
}

enum class SecondaryEditorAction(@StringRes val titleResource: Int?, val isVisible: Boolean) {
    SAVE_AS_DRAFT(R.string.menu_save_as_draft, isVisible = true),
    SAVE(R.string.save, isVisible = true),
    PUBLISH_NOW(R.string.menu_publish_now, isVisible = true),
    NONE(titleResource = null, isVisible = false);
}
