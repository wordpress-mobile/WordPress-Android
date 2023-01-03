package org.wordpress.android.ui.stories

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stories.intro.StoriesIntroViewModel
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class StoriesIntroViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: StoriesIntroViewModel
    @Mock lateinit var onDialogClosedObserver: Observer<Unit>
    @Mock lateinit var onCreateButtonClickedObserver: Observer<Unit>
    @Mock lateinit var onStoryOpenRequestedObserver: Observer<String>
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Before
    fun setUp() = test {
        viewModel = StoriesIntroViewModel(
                analyticsTrackerWrapper,
                appPrefsWrapper,
                NoDelayCoroutineDispatcher()
        )
        viewModel.onDialogClosed.observeForever(onDialogClosedObserver)
        viewModel.onCreateButtonClicked.observeForever(onCreateButtonClickedObserver)
        viewModel.onStoryOpenRequested.observeForever(onStoryOpenRequestedObserver)
    }

    @Test
    fun `pressing back button closes the dialog`() {
        viewModel.onBackButtonPressed()
        verify(onDialogClosedObserver).onChanged(anyOrNull())
    }

    @Test
    fun `pressing create button triggers appropriate event`() {
        viewModel.onCreateStoryButtonPressed()
        verify(onCreateButtonClickedObserver).onChanged(anyOrNull())
    }

    @Test
    fun `tapping preview images triggers request for opening story in browser`() {
        viewModel.onStoryPreviewTapped1()
        verify(onStoryOpenRequestedObserver).onChanged(any())

        reset(onStoryOpenRequestedObserver)

        viewModel.onStoryPreviewTapped2()
        verify(onStoryOpenRequestedObserver).onChanged(any())
    }

    @Test
    fun `opening is tracked when view model starts`() {
        viewModel.start()
        verify(analyticsTrackerWrapper).track(eq(Stat.STORY_INTRO_SHOWN))
    }

    @Test
    fun `closing is tracked when view is dismissed`() {
        viewModel.onBackButtonPressed()
        verify(analyticsTrackerWrapper).track(eq(Stat.STORY_INTRO_DISMISSED))
    }

    @Test
    fun `pref is updated when user taps create story button`() {
        viewModel.onCreateStoryButtonPressed()
        verify(appPrefsWrapper).shouldShowStoriesIntro = false

        verify(analyticsTrackerWrapper).track(eq(Stat.STORY_INTRO_CREATE_STORY_BUTTON_TAPPED))
    }
}
