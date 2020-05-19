package org.wordpress.android.ui.posts.prepublishing.home.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.PublishButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.ScheduleButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.UpdateButtonUiState
import org.wordpress.android.ui.posts.PublishPost
import org.wordpress.android.ui.posts.editor.EditorActionsProvider
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction.SAVE
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction.SUBMIT_FOR_REVIEW
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import java.lang.IllegalStateException
import javax.inject.Inject

class GetButtonUiStateUseCase @Inject constructor(
    private val editorActionsProvider: EditorActionsProvider,
    private val uploadUtilsWrapper: UploadUtilsWrapper
) {
    fun getUiState(
        editPostRepository: EditPostRepository,
        site: SiteModel,
        onButtonClicked: (PublishPost) -> Unit
    ): ButtonUiState {
        val editorAction = editorActionsProvider.getPrimaryAction(
                editPostRepository.status,
                uploadUtilsWrapper.userCanPublish(site)
        )

        return when (editorAction) {
            PrimaryEditorAction.PUBLISH_NOW -> PublishButtonUiState(onButtonClicked)
            PrimaryEditorAction.SCHEDULE -> ScheduleButtonUiState(onButtonClicked)
            PrimaryEditorAction.UPDATE -> UpdateButtonUiState(onButtonClicked)
            SUBMIT_FOR_REVIEW, SAVE -> {
                throw IllegalStateException("The $editorAction action shouldn't be available in this bottom sheet.")
            }
        }
    }
}
