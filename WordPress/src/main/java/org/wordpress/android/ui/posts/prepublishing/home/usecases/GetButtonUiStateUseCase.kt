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

        // todo: annmarie @ajesh - notice that the button states are based on the editorAction - should this only be used
        // post the first time the user opens the editor, or if it should be used every time the user opens the editor.
        // I think only the first time - and maybe then again - we also need to show loading if media is in progress. hmmm
        // Maybe something else goes after this ... ugh, but what if the user is offline? Or what if they are not offline
        // and media is uploading? Can the user tap the publish button when media is uploading? If we are only
        // getting the media state once something happens?????

        // So you can't press the publish button immediately when media is uploading; but you can press it while media is
        // actually uploading. At which time the bottom sheet doesn't know anything about the media upload state until the
        // next event comes in or it may never come in.

        // How can we check the media state? Is this even possible?? Who can we poll? The media store?

        // Are we moving media upload to sync??? I don't think so

        // None of these states should show if media is uploading. We need to show a loading state if media is uploading.

        // Okay, so what if we used MutableStateFlow- which, as a hot observable, will emit the last value to new subscribers???
        // This could work!

        // @ajesh on feb 20th

        // Yup this could work, I think we will need observe the media events and update the state of the
        // button accordingly everytime , this should be not only just consumed the first time when the user goes to
        // the editor
        return when (editorAction) {
            PrimaryEditorAction.PUBLISH_NOW -> PublishButtonUiState(onButtonClicked)
            PrimaryEditorAction.SCHEDULE -> ScheduleButtonUiState(onButtonClicked)
            PrimaryEditorAction.UPDATE, PrimaryEditorAction.CONTINUE -> UpdateButtonUiState(onButtonClicked)
            PrimaryEditorAction.SUBMIT_FOR_REVIEW -> SubmitButtonUiState(onButtonClicked)
            PrimaryEditorAction.SAVE -> SaveButtonUiState(onButtonClicked)
        }
    }
}
