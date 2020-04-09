package org.wordpress.android.ui.posts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingActionUiState

@RunWith(MockitoJUnitRunner::class)
class PrepublishingActionsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: PrepublishingActionsViewModel

    @Before
    fun setUp() {
        viewModel = PrepublishingActionsViewModel()
        viewModel.start()
    }

    @Test
    fun `verify that actions are propagated to prepublishingActionsUiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 3

        // act - viewModel is already started

        // assert
        assertThat(viewModel.prepublishingActionsUiState.value?.size).isEqualTo(expectedActionsAmount)
    }

    @Test
    fun `verify that publish action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = PUBLISH

        // act
        val publishAction = getActionUiState(expectedActionType)
        publishAction.onActionClicked.invoke(expectedActionType)

        // assert
        assertThat(viewModel.prepublishingActionType.value).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that visibility action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = VISIBILITY

        // act
        val visibilityAction = getActionUiState(expectedActionType)
        visibilityAction.onActionClicked.invoke(expectedActionType)

        // assert
        assertThat(viewModel.prepublishingActionType.value).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that tags action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = VISIBILITY

        // act
        val tagsAction = getActionUiState(expectedActionType)
        tagsAction.onActionClicked.invoke(expectedActionType)

        // assert
        assertThat(viewModel.prepublishingActionType.value).isEqualTo(expectedActionType)
    }

    private fun getActionUiState(actionType: ActionType): PrepublishingActionUiState {
        val actions = viewModel.prepublishingActionsUiState.value
                ?.filterIsInstance(PrepublishingActionUiState::class.java)
        return actions?.find { it.actionType == actionType }!!
    }
}
