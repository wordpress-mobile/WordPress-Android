package org.wordpress.android.ui.mysite

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wordpress.stories.compose.frame.FrameSaveNotifier
import com.wordpress.stories.compose.frame.StorySaveEvents
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveProcessStart
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryRepository
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STORY_SAVE_ERROR_SNACKBAR_MANAGE_TAPPED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenStories
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stories.StoriesMediaPickerResultHandler
import org.wordpress.android.ui.stories.StoriesTrackerHelper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class SiteStoriesHandler
@Inject constructor(
    private val eventBusWrapper: EventBusWrapper,
    private val resourceProvider: ResourceProvider,
    private val storiesTrackerHelper: StoriesTrackerHelper,
    private val contextProvider: ContextProvider,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val storiesMediaPickerResultHandler: StoriesMediaPickerResultHandler
) {
    private val _onSnackbar = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbar = _onSnackbar as LiveData<Event<SnackbarMessageHolder>>
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = merge(_onNavigation, storiesMediaPickerResultHandler.onNavigation)

    init {
        eventBusWrapper.register(this)
    }

    fun clear() {
        eventBusWrapper.unregister(this)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: StorySaveResult) {
        eventBusWrapper.removeStickyEvent(event)
        if (!event.isSuccess()) {
            // note: no tracking added here as we'll perform tracking in StoryMediaSaveUploadBridge
            val errorText = String.format(
                resourceProvider.getString(R.string.story_saving_snackbar_finished_with_error),
                StoryRepository.getStoryAtIndex(event.storyIndex).title
            )
            val snackbarMessage = FrameSaveNotifier.buildSnackbarErrorMessage(
                contextProvider.getContext(),
                StorySaveEvents.allErrorsInResult(event.frameSaveResult).size,
                errorText
            )

            _onSnackbar.postValue(
                Event(
                    SnackbarMessageHolder(
                        UiStringText(snackbarMessage),
                        UiStringRes(R.string.story_saving_failed_quick_action_manage),
                        buttonAction = {
                            val selectedSite = selectedSiteRepository.getSelectedSite()
                                ?: return@SnackbarMessageHolder
                            _onNavigation.postValue(Event(OpenStories(selectedSite, event)))
                            storiesTrackerHelper.trackStorySaveResultEvent(
                                event,
                                STORY_SAVE_ERROR_SNACKBAR_MANAGE_TAPPED
                            )
                        },
                        onDismissAction = { }
                    )
                )
            )
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStorySaveStart(event: StorySaveProcessStart) {
        eventBusWrapper.removeStickyEvent(event)
        val snackbarMessage = String.format(
            resourceProvider.getString(R.string.story_saving_snackbar_started),
            StoryRepository.getStoryAtIndex(event.storyIndex).title
        )
        _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(snackbarMessage))))
    }

    fun handleStoriesResult(siteModel: SiteModel, data: Intent, source: PagePostCreationSourcesDetail) {
        storiesMediaPickerResultHandler.handleMediaPickerResultForStories(data, siteModel, source)
    }
}
