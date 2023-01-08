package org.wordpress.android.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class CommentFullScreenDialogViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CommentFullScreenDialogViewModel
    @Before
    fun setUp() {
        viewModel = CommentFullScreenDialogViewModel()
    }

    @Test
    fun `on init opens keyboard`() {
        var openKeyboardEvent: Event<Unit>? = null
        viewModel.onKeyboardOpened.observeForever { openKeyboardEvent = it }

        viewModel.init()

        assertThat(openKeyboardEvent).isNotNull()
        assertThat(openKeyboardEvent!!.getContentIfNotHandled()).isNotNull()
    }
}
