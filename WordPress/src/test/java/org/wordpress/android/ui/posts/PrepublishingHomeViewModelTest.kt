package org.wordpress.android.ui.posts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.PrepublishingHomeUiState

@RunWith(MockitoJUnitRunner::class)
class PrepublishingHomeViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: PrepublishingHomeViewModel

    @Before
    fun setUp() {
        viewModel = PrepublishingHomeViewModel(mock())
        viewModel.start(mock())
    }

    @Test
    fun `verify that home actions are propagated to prepublishingHomeUiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 3

        // act - viewModel is already started

        // assert
        assertThat(viewModel.uiState.value?.size).isEqualTo(expectedActionsAmount)
    }

    @Test
    fun `verify that publish action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = PUBLISH

        // act
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
        val tagsAction = getHomeUiState(expectedActionType)
        tagsAction?.onActionClicked?.invoke(expectedActionType)

        // assert
        assertThat(requireNotNull(viewModel.onActionClicked.value).peekContent()).isEqualTo(expectedActionType)
    }

    private fun getHomeUiState(actionType: ActionType): PrepublishingHomeUiState? {
        val actions = viewModel.uiState.value
                ?.filterIsInstance(PrepublishingHomeUiState::class.java)
        return actions?.find { it.actionType == actionType }
    }
}
