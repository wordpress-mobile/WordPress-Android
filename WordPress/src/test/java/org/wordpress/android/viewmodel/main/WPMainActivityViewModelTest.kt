package org.wordpress.android.viewmodel.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@RunWith(MockitoJUnitRunner::class)
class WPMainActivityViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: WPMainActivityViewModel

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Before
    fun setUp() {
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(false)
        viewModel = WPMainActivityViewModel(appPrefsWrapper)
        viewModel.start(isFabVisible = true, hasFullAccessToContent = true)
    }

    @Test
    fun `fab visible when asked`() {
        viewModel.onPageChanged(showFab = true, hasFullAccessToContent = true)
        assertThat(viewModel.fabUiState.value?.isFabVisible).isEqualTo(true)
    }

    @Test
    fun `fab hidden when asked`() {
        viewModel.onPageChanged(showFab = false, hasFullAccessToContent = true)
        assertThat(viewModel.fabUiState.value?.isFabVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip visible when asked`() {
        viewModel.onPageChanged(showFab = true, hasFullAccessToContent = true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(true)
    }

    @Test
    fun `fab tooltip hidden when asked`() {
        viewModel.onPageChanged(showFab = false, hasFullAccessToContent = true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip disabled when tapped`() {
        viewModel.onTooltipTapped(true)
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip disabled when user without full access to content uses the fab`() {
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(true)
        viewModel.onFabClicked(false)
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip disabled when bottom sheet opened`() {
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(true)
        viewModel.onFabClicked(true)
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip disabled when fab long pressed`() {
        viewModel.onFabLongPressed(true)
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `bottom sheet action is new post when new post is tapped`() {
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_POST } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_POST)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_POST)
    }

    @Test
    fun `bottom sheet action is new page when new page is tapped`() {
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_PAGE } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_PAGE)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_PAGE)
    }

    @Test
    fun `bottom sheet action is new story when new story is tapped`() {
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_STORY } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_STORY)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_STORY)
    }

    @Test
    fun `bottom sheet is visualized when user has full access to content and has all 3 options`() {
        viewModel.onFabClicked(hasFullAccessToContent = true)
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.mainActions.value?.size).isEqualTo(4) // 3 options plus NO_ACTION, first in list
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `bottom sheet is visualized when user has partial access to content and has only 2 options`() {
        viewModel.onFabClicked(hasFullAccessToContent = false)
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.mainActions.value?.size).isEqualTo(3) // 2 options plus NO_ACTION, first in list
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `when user taps to open the login page from the bottom sheet empty view cta the correct action is triggered`() {
        viewModel.onOpenLoginPage()

        assertThat(viewModel.startLoginFlow.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `onResume set expected content message when user has full access to content`() {
        viewModel.onResume(true)
        assertThat(viewModel.fabUiState.value!!.CreateContentMessageId).isEqualTo(R.string.create_post_page_fab_tooltip)
    }

    @Test
    fun `onResume set expected content message when user has not full access to content`() {
        viewModel.onResume(false)
        assertThat(viewModel.fabUiState.value!!.CreateContentMessageId)
                .isEqualTo(R.string.create_post_page_fab_tooltip_contributors)
    }
}
