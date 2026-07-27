package org.wordpress.android.ui.posts.prepublishing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.ADD_CATEGORY
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.CATEGORIES
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.HOME
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.PUBLISH
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.SOCIAL
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.TAGS
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType.Action
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType.PrepublishingScreenNavigation
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
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

        viewModel.start(mock(), null, hasPost = true)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when viewModel start is called with a currentScreen, navigateToScreen should be invoked with it`() {
        val expectedScreen = TAGS

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), expectedScreen, hasPost = true)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when viewModel start is called without a post, dismiss the sheet instead of navigating`() {
        var dismissEvent: Event<Unit>? = null
        var navigationEvent: Event<PrepublishingNavigationTarget>? = null
        viewModel.dismissBottomSheet.observeForever { dismissEvent = it }
        viewModel.navigationTarget.observeForever { navigationEvent = it }

        // TAGS rather than HOME because a restored sheet can land on any screen
        viewModel.start(mock(), TAGS, hasPost = false)

        assertThat(dismissEvent).isNotNull
        assertThat(navigationEvent).isNull()
    }

    @Test
    fun `when viewModel start is called with a post, don't dismiss the sheet`() {
        var dismissEvent: Event<Unit>? = null
        viewModel.dismissBottomSheet.observeForever { dismissEvent = it }

        viewModel.start(mock(), TAGS, hasPost = true)

        assertThat(dismissEvent).isNull()
    }

    @Test
    fun `when onBackClicked is pressed and currentScreen isn't HOME, navigate to HOME`() {
        val expectedScreen = HOME

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), TAGS, hasPost = true)
        viewModel.onBackClicked()

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onBackClicked is pressed and currentScreen is HOME, dismiss bottom sheet`() {
        var event: Event<Unit>? = null
        viewModel.dismissBottomSheet.observeForever {
            event = it
        }

        viewModel.start(mock(), HOME, hasPost = true)
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
    fun `when onActionClicked is called with Tags, navigate to TAGS screen`() {
        val expectedScreen = TAGS

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), mock(), hasPost = true)
        viewModel.onActionClicked(PrepublishingScreenNavigation.Tags)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onActionClicked is called with Publish, navigate to PUBLISH screen`() {
        val expectedScreen = PUBLISH

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), mock(), hasPost = true)
        viewModel.onActionClicked(PrepublishingScreenNavigation.Publish)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onActionClicked is called with Categories, navigate to CATEGORIES screen`() {
        val expectedScreen = CATEGORIES

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), mock(), hasPost = true)
        viewModel.onActionClicked(PrepublishingScreenNavigation.Categories)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onActionClicked is called with AddCategory, navigate to ADD_CATEGORY screen`() {
        val expectedScreen = ADD_CATEGORY

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), mock(), hasPost = true)
        viewModel.onActionClicked(PrepublishingScreenNavigation.AddCategory)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onActionClicked is called with Social, navigate to SOCIAL screen`() {
        val expectedScreen = SOCIAL

        var event: Event<PrepublishingNavigationTarget>? = null
        viewModel.navigationTarget.observeForever {
            event = it
        }

        viewModel.start(mock(), mock(), hasPost = true)
        viewModel.onActionClicked(PrepublishingScreenNavigation.Social)

        assertThat(event?.peekContent()?.targetScreen).isEqualTo(expectedScreen)
    }

    @Test
    fun `when onActionClicked is called with NavigateToSharingSettings, navigateToSharingSettings is emitted`() {
        val mockSite = mock<SiteModel>()
        var event: Event<SiteModel>? = null
        viewModel.navigateToSharingSettings.observeForever {
            event = it
        }

        viewModel.start(mockSite, mock(), hasPost = true)
        viewModel.onActionClicked(Action.NavigateToSharingSettings)

        assertThat(event?.peekContent()).isEqualTo(mockSite)
    }

    @Test
    fun `when onSubmitButtonClicked is triggered then bottom sheet should close and listener is triggered`() {
        viewModel.start(mock(), mock(), hasPost = true)

        viewModel.onSubmitButtonClicked(true)

        assertThat(viewModel.dismissBottomSheet.value?.peekContent()).isNotNull()
        assertThat(viewModel.triggerOnSubmitButtonClickedListener.value?.peekContent()).isNotNull()
    }
}
