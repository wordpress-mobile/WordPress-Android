package org.wordpress.android.ui.posts.prepublishing.visibility

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.DRAFT
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PENDING_REVIEW
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLISH
import org.wordpress.android.ui.posts.prepublishing.visibility.usecases.GetPostVisibilityUseCase
import org.wordpress.android.ui.posts.prepublishing.visibility.usecases.UpdatePostPasswordUseCase
import org.wordpress.android.ui.posts.prepublishing.visibility.usecases.UpdateVisibilityUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event

class PrepublishingVisibilityViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingVisibilityViewModel

    @Mock lateinit var getPostVisibilityUseCase: GetPostVisibilityUseCase
    @Mock lateinit var updatePostPasswordUseCase: UpdatePostPasswordUseCase
    @Mock lateinit var updatePostStatusUseCase: UpdateVisibilityUseCase
    private lateinit var editPostRepository: EditPostRepository

    @InternalCoroutinesApi
    @Before
    fun setup() {
        viewModel = PrepublishingVisibilityViewModel(
                getPostVisibilityUseCase,
                updatePostPasswordUseCase,
                updatePostStatusUseCase,
                mock()
        )
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
    }

    @Test
    fun `when viewModel is started toolbarUiState is called with the visibility title`() {
        // arrange
        val expectedStringResource = R.string.prepublishing_nudges_toolbar_title_visibility

        // act
        viewModel.start(editPostRepository)

        // assert
        assertThat((viewModel.toolbarUiState.value as UiStringRes).stringRes)
                .isEqualTo(expectedStringResource)
    }

    @Test
    fun `if current VISIBILITY is PUBLISH then it is checked in the uiState`() {
        // arrange
        val expectedVisibility = PUBLISH
        whenever(getPostVisibilityUseCase.getVisibility(any())).thenReturn(expectedVisibility)

        // act
        viewModel.start(editPostRepository)

        // assert
        assertThat(getCheckedVisibilityUiState()?.visibility).isEqualTo(expectedVisibility)
    }

    @Test
    fun `if current VISIBILITY is DRAFT then it is checked in the uiState`() {
        // arrange
        val expectedVisibility = DRAFT
        whenever(getPostVisibilityUseCase.getVisibility(any())).thenReturn(expectedVisibility)

        // act
        viewModel.start(editPostRepository)

        // assert
        assertThat(getCheckedVisibilityUiState()?.visibility).isEqualTo(expectedVisibility)
    }

    @Test
    fun `if current VISIBILITY is PENDING_REVIEW then it is checked in the uiState`() {
        // arrange
        val expectedVisibility = PENDING_REVIEW
        whenever(getPostVisibilityUseCase.getVisibility(any())).thenReturn(expectedVisibility)

        // act
        viewModel.start(editPostRepository)

        // assert
        assertThat(getCheckedVisibilityUiState()?.visibility).isEqualTo(expectedVisibility)
    }

    @Test
    fun `if current VISIBILITY is PRIVATE then it is checked in the uiState`() {
        // arrange
        val expectedVisibility = PRIVATE
        whenever(getPostVisibilityUseCase.getVisibility(any())).thenReturn(expectedVisibility)

        // act
        viewModel.start(editPostRepository)

        // assert
        assertThat(getCheckedVisibilityUiState()?.visibility).isEqualTo(expectedVisibility)
    }

    @Test
    fun `if current VISIBILITY is PASSWORD_PROTECTED then it is checked in the uiState`() {
        // arrange
        val expectedVisibility = PASSWORD_PROTECTED
        whenever(getPostVisibilityUseCase.getVisibility(any())).thenReturn(expectedVisibility)

        // act
        viewModel.start(editPostRepository)

        // assert
        assertThat(getCheckedVisibilityUiState()?.visibility).isEqualTo(expectedVisibility)
    }

    @Test
    fun `verify that onPostPasswordChanged causes updatePostPasswordUseCase to be called`() {
        // arrange
        val password = "password"

        // act
        viewModel.start(editPostRepository)
        viewModel.onPostPasswordChanged(password)

        // assert
        verify(updatePostPasswordUseCase).updatePassword(eq(password), any(), any())
    }

    @Test
    fun `verify that tapping PASSWORD_PROTECTED will showPasswordDialog`() {
        // arrange
        var event: Event<Unit>? = null
        viewModel.showPasswordDialog.observeForever {
            event = it
        }

        // act
        viewModel.start(editPostRepository)
        invokeVisibilityOnItemTapped(PASSWORD_PROTECTED)

        // assert
        assertThat(event).isNotNull
    }

    @Test
    fun `If PRIVATE VISIBILITY is tapped & password is not empty updatePasswordUseCase clears it`() {
        // arrange
        val expectedEmptyPassword = ""

        val post = PostModel()
        val password = "password"
        post.setPassword(password)
        editPostRepository.set { post }

        // act
        viewModel.start(editPostRepository)
        invokeVisibilityOnItemTapped(PRIVATE)

        // assert
        verify(updatePostPasswordUseCase).updatePassword(eq(expectedEmptyPassword), any(), any())
    }

    @Test
    fun `If PRIVATE VISIBILITY is tapped & password is empty updatePostStatusUseCase is called`() {
        // arrange
        val emptyPassword = ""
        val post = PostModel().apply { setPassword(emptyPassword) }
        editPostRepository.set { post }

        // act
        viewModel.start(editPostRepository)
        invokeVisibilityOnItemTapped(PRIVATE)

        // assert
        verify(updatePostStatusUseCase).updatePostVisibility(any(), any(), any())
    }

    @Test
    fun `when onBackClicked is triggered navigateToHomeScreen is called`() {
        // arrange
        var event: Event<Unit>? = null
        viewModel.navigateToHomeScreen.observeForever {
            event = it
        }

        // act
        viewModel.onBackButtonClicked()

        // assert
        assertThat(event).isNotNull
    }

    @Test
    fun `when onCloseClicked is triggered dismissBottomSheet is called`() {
        // arrange
        var event: Event<Unit>? = null
        viewModel.dismissBottomSheet.observeForever {
            event = it
        }

        // act
        viewModel.onCloseButtonClicked()

        // assert
        assertThat(event).isNotNull
    }

    private fun getCheckedVisibilityUiState() =
            viewModel.uiState.value?.find { visibilityUiState -> visibilityUiState.checked }

    private fun invokeVisibilityOnItemTapped(visibility: Visibility) {
        viewModel.uiState.value?.find { visibilityUiState -> visibilityUiState.visibility == visibility }
                ?.onItemTapped?.invoke(visibility)
    }
}
