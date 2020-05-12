package org.wordpress.android.ui.posts.prepublishing.home.usecases

import android.text.TextUtils
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.EditorAction
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.EditorAction.PUBLISH_NOW
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.EditorAction.SCHEDULE
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.EditorAction.UPDATE
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.PublishButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.ScheduleButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.UpdateButtonUiState
import org.wordpress.android.ui.posts.PublishPost
import javax.inject.Inject

class GetButtonUiStateUseCase @Inject constructor(private val postUtilsWrapper: PostUtilsWrapper) {
    fun getUiState(
        editPostRepository: EditPostRepository,
        editorAction: EditorAction,
        onButtonClicked: (PublishPost) -> Unit
    ): ButtonUiState {
        val status = editPostRepository.status
        return when {
            !TextUtils.isEmpty(editPostRepository.dateCreated) -> {
                when {
                    postUtilsWrapper.isPublishDateInTheFuture(editPostRepository.dateCreated) || status == PostStatus.SCHEDULED -> createButtonUiState(
                            SCHEDULE,
                            onButtonClicked
                    )
                    status == PostStatus.PUBLISHED || status == PostStatus.PRIVATE || editPostRepository.isLocalDraft ->
                        createButtonUiState(editorAction, onButtonClicked)
                    else -> createButtonUiState(editorAction, onButtonClicked)
                }
            }
            else -> createButtonUiState(editorAction, onButtonClicked)
        }
    }

    private fun createButtonUiState(editorAction: EditorAction, onButtonClicked: (PublishPost) -> Unit) =
            when (editorAction) {
                PUBLISH_NOW -> PublishButtonUiState(onButtonClicked)
                UPDATE -> UpdateButtonUiState(onButtonClicked)
                SCHEDULE -> ScheduleButtonUiState(onButtonClicked)
            }
}

