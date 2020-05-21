package org.wordpress.android.ui.posts.prepublishing

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingNavigationTarget
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import org.wordpress.android.ui.posts.PrepublishingScreen.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingScreen.TAGS
import org.wordpress.android.ui.posts.PrepublishingScreen.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingViewModel
import org.wordpress.android.viewmodel.Event

class PrepublishingViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingViewModel

    @Before
    fun setup() {
        viewModel = PrepublishingViewModel(mock())
    }

    @Test
    fun `when viewModel start is called with a null currentScreen, navigateToScreen should be invoked with HOME`() {
        val expectedScreen = HOME

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), null)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when viewModel start is called with a currentScreen, navigateToScreen should be invoked with it`() {
        val expectedScreen = TAGS

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), expectedScreen)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onBackClicked is pressed and currentScreen isn't HOME, navigate to HOME`() {
        val expectedScreen = HOME

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), TAGS)
        viewModel.onBackClicked()

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onBackClicked is pressed and currentScreen is HOME, dismiss bottom sheet`() {
        var event: Event<Unit>? = null
        viewModel.dismissBottomSheet.observeForever {
            event = it
        }

        viewModel.start(mock(), HOME)
        viewModel.onBackClicked()

        assertThat(event).isNotNull
    }

    @Test
    fun `when onCloseClicked is pressed, dismiss bottom sheet`() {
        var event: Event<Unit>? = null
        viewModel.dismissBottomSheet.observeForever {
            event = it
        }

        viewModel.onCloseClicked()

        assertThat(event).isNotNull
    }

    @Test
    fun `when onActionClicked is called with TAGS, navigate to TAGS screen`() {
        val expectedScreen = TAGS

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), mock())
        viewModel.onActionClicked(ActionType.TAGS)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onActionClicked is called with PUBLISH, navigate to PUBLISH screen`() {
        val expectedScreen = PUBLISH

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), mock())
        viewModel.onActionClicked(ActionType.PUBLISH)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onActionClicked is called with VISIBILITY, navigate to VISIBILITY screen`() {
        val expectedScreen = VISIBILITY

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), mock())
        viewModel.onActionClicked(ActionType.VISIBILITY)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onSubmitButtonClicked is triggered then bottom sheet should close and listener is triggered`() {
        viewModel.start(mock(), mock())

        viewModel.onSubmitButtonClicked(mock(), true)

        assertThat(viewModel.dismissBottomSheet.value?.peekContent()).isNotNull()
        assertThat(viewModel.triggerOnSubmitButtonClickedListener.value?.peekContent()).isNotNull()
    }
}
