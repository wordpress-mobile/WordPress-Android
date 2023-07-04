package org.wordpress.android.ui.posts.prepublishing.home.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.editor.EditorActionsProvider
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.PublishButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.SaveButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.ScheduleButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.SubmitButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.UpdateButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PublishPost
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
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
            PrimaryEditorAction.UPDATE, PrimaryEditorAction.CONTINUE -> UpdateButtonUiState(onButtonClicked)
            PrimaryEditorAction.SUBMIT_FOR_REVIEW -> SubmitButtonUiState(onButtonClicked)
            PrimaryEditorAction.SAVE -> SaveButtonUiState(onButtonClicked)
        }
    }
}
