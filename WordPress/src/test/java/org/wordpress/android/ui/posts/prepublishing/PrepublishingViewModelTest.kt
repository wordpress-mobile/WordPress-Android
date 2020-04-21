package org.wordpress.android.ui.posts.prepublishing

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.posts.PrepublishingNavigationTarget
import org.wordpress.android.ui.posts.PrepublishingScreen
import org.wordpress.android.ui.posts.PrepublishingViewModel
import org.wordpress.android.viewmodel.Event

class PrepublishingViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingViewModel

    @Before
    fun setup() {
        viewModel = PrepublishingViewModel()
    }

    @Test
    fun `when viewModel start is called with a null currentScreen navigateToScreen should be invoked with HOME`() {
        val expectedScreen = PrepublishingScreen.HOME

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), null)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when viewModel start is called with a currentScreen navigateToScreen should be invoked with it`(){
        val expectedScreen = PrepublishingScreen.TAGS

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), expectedScreen)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }
}
