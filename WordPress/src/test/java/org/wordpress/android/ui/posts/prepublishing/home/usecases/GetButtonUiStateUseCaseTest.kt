package org.wordpress.android.ui.posts.prepublishing.home.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.PublishButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.SaveButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.ScheduleButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.SubmitButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState.UpdateButtonUiState
import org.wordpress.android.ui.posts.editor.EditorActionsProvider
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction.CONTINUE
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction.PUBLISH_NOW
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction.SAVE
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction.SCHEDULE
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction.SUBMIT_FOR_REVIEW
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction.UPDATE
import org.wordpress.android.ui.uploads.UploadUtilsWrapper

@ExperimentalCoroutinesApi
class GetButtonUiStateUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: GetButtonUiStateUseCase

    @Mock
    lateinit var editorActionsProvider: EditorActionsProvider

    @Mock
    lateinit var uploadUtilsWrapper: UploadUtilsWrapper

    @Mock
    lateinit var editPostRepository: EditPostRepository

    @Mock
    lateinit var site: SiteModel

    @Before
    fun setup() {
        useCase = GetButtonUiStateUseCase(editorActionsProvider, uploadUtilsWrapper)
        whenever(editPostRepository.status).thenReturn(DRAFT)
    }

    @Test
    fun `verify that PUBLISH_NOW EditorAction returns PublishButtonUiState`() {
        // arrange
        whenever(editorActionsProvider.getPrimaryAction(any(), any(), any())).thenReturn(PUBLISH_NOW)

        // act
        val uiState = useCase.getUiState(editPostRepository, mock()) {}

        // assert
        assertThat(uiState).isInstanceOf(PublishButtonUiState::class.java)
    }

    @Test
    fun `verify that SCHEDULE EditorAction returns ScheduleButtonUiState`() {
        // arrange
        whenever(editorActionsProvider.getPrimaryAction(any(), any(), any())).thenReturn(SCHEDULE)

        // act
        val uiState = useCase.getUiState(editPostRepository, mock()) {}

        // assert
        assertThat(uiState).isInstanceOf(ScheduleButtonUiState::class.java)
    }

    @Test
    fun `verify that UPDATE EditorAction returns UpdateButtonUiState`() {
        // arrange
        whenever(editorActionsProvider.getPrimaryAction(any(), any(), any())).thenReturn(UPDATE)

        // act
        val uiState = useCase.getUiState(editPostRepository, mock()) {}

        // assert
        assertThat(uiState).isInstanceOf(UpdateButtonUiState::class.java)
    }

    @Test
    fun `verify that CONTINUE EditorAction returns UpdateButtonUiState`() {
        // arrange
        whenever(editorActionsProvider.getPrimaryAction(any(), any(), any())).thenReturn(CONTINUE)

        // act
        val uiState = useCase.getUiState(editPostRepository, mock()) {}

        // assert
        assertThat(uiState).isInstanceOf(UpdateButtonUiState::class.java)
    }

    @Test
    fun `verify that SUBMIT_FOR_REVIEW EditorAction returns SubmitButtonUiState`() {
        // arrange
        whenever(editorActionsProvider.getPrimaryAction(any(), any(), any())).thenReturn(SUBMIT_FOR_REVIEW)

        // act
        val uiState = useCase.getUiState(editPostRepository, mock()) {}

        // assert
        assertThat(uiState).isInstanceOf(SubmitButtonUiState::class.java)
    }

    @Test
    fun `verify that SAVE EditorAction returns SaveButtonUiState`() {
        // arrange
        whenever(editorActionsProvider.getPrimaryAction(any(), any(), any())).thenReturn(SAVE)

        // act
        val uiState = useCase.getUiState(editPostRepository, mock()) {}

        // assert
        assertThat(uiState).isInstanceOf(SaveButtonUiState::class.java)
    }
}
