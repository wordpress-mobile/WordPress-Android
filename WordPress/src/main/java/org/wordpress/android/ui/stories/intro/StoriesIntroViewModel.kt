package org.wordpress.android.ui.stories.intro

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class StoriesIntroViewModel @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _onDialogClosed = SingleLiveEvent<Unit>()
    val onDialogClosed: LiveData<Unit> = _onDialogClosed

    private val _onCreateButtonClicked = SingleLiveEvent<Unit>()
    val onCreateButtonClicked: LiveData<Unit> = _onCreateButtonClicked

    private val _onStoryOpenRequested = SingleLiveEvent<String>()
    val onStoryOpenRequested: LiveData<String> = _onStoryOpenRequested

    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true

        analyticsTrackerWrapper.track(Stat.STORY_INTRO_SHOWN)
    }

    fun onBackButtonPressed() {
        analyticsTrackerWrapper.track(Stat.STORY_INTRO_DISMISSED)
        _onDialogClosed.call()
    }

    fun onCreateStoryButtonPressed() {
        analyticsTrackerWrapper.track(Stat.STORY_INTRO_CREATE_STORY_BUTTON_TAPPED)

        appPrefsWrapper.shouldShowStoriesIntro = false

        _onCreateButtonClicked.call()
    }

    fun onStoryPreviewTapped1() {
        _onStoryOpenRequested.value = STORY_URL_1 + STORY_FULLSCREEN_URL_PARAMS
    }

    fun onStoryPreviewTapped2() {
        _onStoryOpenRequested.value = STORY_URL_2 + STORY_FULLSCREEN_URL_PARAMS
    }

    companion object {
        private const val STORY_URL_1 = "https://wpstories.wordpress.com/2020/12/02/story-demo-01/"
        private const val STORY_URL_2 = "https://wpstories.wordpress.com/2020/12/02/story-demo-02/"
        private const val STORY_FULLSCREEN_URL_PARAMS = "?wp-story-load-in-fullscreen=true&wp-story-play-on-load=true"
    }
}
