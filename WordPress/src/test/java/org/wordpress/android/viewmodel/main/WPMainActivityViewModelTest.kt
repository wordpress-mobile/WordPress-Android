package org.wordpress.android.viewmodel.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.CreateAction

@RunWith(MockitoJUnitRunner::class)
class WPMainActivityViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: WPMainActivityViewModel

    @Before
    fun setUp() {
        viewModel = WPMainActivityViewModel()
        viewModel.start(true)
    }

    @Test
    fun `fab visible when asked`() {
        viewModel.onPageChanged(true)
        assertThat(viewModel.showMainActionFab.value).isEqualTo(true)
    }

    @Test
    fun `fab hidden when asked`() {
        viewModel.onPageChanged(false)
        assertThat(viewModel.showMainActionFab.value).isEqualTo(false)
    }

    @Test
    fun `bottom sheet action is new post when new post is tapped`() {
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_POST } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction.invoke(CREATE_NEW_POST)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_POST)
    }

    @Test
    fun `bottom sheet action is new page when new page is tapped`() {
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_PAGE } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction.invoke(CREATE_NEW_PAGE)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_PAGE)
    }
}
