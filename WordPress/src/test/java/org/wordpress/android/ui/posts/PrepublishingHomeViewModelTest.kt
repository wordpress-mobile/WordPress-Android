package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.utils.UiString.UiStringText

@RunWith(MockitoJUnitRunner::class)
class PrepublishingHomeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingHomeViewModel
    @Mock lateinit var postSettingsUtils: PostSettingsUtils
    @Mock lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var getPostTagsUseCase: GetPostTagsUseCase

    @Before
    fun setUp() {
        viewModel = PrepublishingHomeViewModel(getPostTagsUseCase, postSettingsUtils)
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn("")
        whenever(editPostRepository.getPost()).thenReturn(PostModel())
    }

    @Test
    fun `verify that home actions are propagated to prepublishingHomeUiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 3

        // act
        viewModel.start(mock(), mock())

        // assert
        assertThat(viewModel.uiState.value?.size).isEqualTo(expectedActionsAmount)
    }

    @Test
    fun `verify that publish action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = PUBLISH

        // act
        viewModel.start(mock(), mock())
        val publishAction = getHomeUiState(expectedActionType)
        publishAction?.onActionClicked?.invoke(expectedActionType)

        // assert
        assertThat(requireNotNull(viewModel.onActionClicked.value).peekContent()).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that visibility action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = VISIBILITY

        // act
        viewModel.start(mock(), mock())
        val visibilityAction = getHomeUiState(expectedActionType)
        visibilityAction?.onActionClicked?.invoke(expectedActionType)

        // assert
        assertThat(requireNotNull(viewModel.onActionClicked.value).peekContent()).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that tags action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = VISIBILITY

        // act
        viewModel.start(mock(), mock())
        val tagsAction = getHomeUiState(expectedActionType)
        tagsAction?.onActionClicked?.invoke(expectedActionType)

        // assert
        assertThat(requireNotNull(viewModel.onActionClicked.value).peekContent()).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that publish action result is propagated from postSettingsUtils`() {
        // arrange
        val expectedLabel = "test data"
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn(expectedLabel)

        // act
        viewModel.start(editPostRepository, mock())
        val publishAction = getHomeUiState(PUBLISH)

        // assert
        assertThat((publishAction?.actionResult as? UiStringText)?.text).isEqualTo(expectedLabel)
    }

    @Test
    fun `verify that tags action result is propagated from getPostTagsUseCase`() {
        // arrange
        val expectedTags = "test, data"
        whenever(getPostTagsUseCase.getTags(editPostRepository)).thenReturn(expectedTags)

        // act
        viewModel.start(editPostRepository, mock())
        val tagsAction = getHomeUiState(TAGS)

        // assert
        assertThat((tagsAction?.actionResult as? UiStringText)?.text).isEqualTo(expectedTags)
    }

    private fun getHomeUiState(actionType: ActionType): HomeUiState? {
        val actions = viewModel.uiState.value
                ?.filterIsInstance(HomeUiState::class.java)
        return actions?.find { it.actionType == actionType }
    }
}
