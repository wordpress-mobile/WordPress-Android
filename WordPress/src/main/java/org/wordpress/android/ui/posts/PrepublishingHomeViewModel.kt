package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.StoryTitleUiState
import org.wordpress.android.ui.posts.prepublishing.home.usecases.GetButtonUiStateUseCase
import org.wordpress.android.ui.posts.prepublishing.visibility.usecases.GetPostVisibilityUseCase
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.usecase.UpdateStoryPostTitleUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

private const val THROTTLE_DELAY = 500L

class PrepublishingHomeViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val getPostVisibilityUseCase: GetPostVisibilityUseCase,
    private val postSettingsUtils: PostSettingsUtils,
    private val getButtonUiStateUseCase: GetButtonUiStateUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val storyRepositoryWrapper: StoryRepositoryWrapper,
    private val updateStoryPostTitleUseCase: UpdateStoryPostTitleUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private var updateStoryTitleJob: Job? = null
    private lateinit var editPostRepository: EditPostRepository

    private val _uiState = MutableLiveData<List<PrepublishingHomeItemUiState>>()
    val uiState: LiveData<List<PrepublishingHomeItemUiState>> = _uiState

    private val _onActionClicked = MutableLiveData<Event<ActionType>>()
    val onActionClicked: LiveData<Event<ActionType>> = _onActionClicked

    private val _onSubmitButtonClicked = MutableLiveData<Event<PublishPost>>()
    val onSubmitButtonClicked: LiveData<Event<PublishPost>> = _onSubmitButtonClicked

    fun start(editPostRepository: EditPostRepository, site: SiteModel, isStoryPost: Boolean) {
        this.editPostRepository = editPostRepository
        if (isStarted) return
        isStarted = true

        setupHomeUiState(editPostRepository, site, isStoryPost)
    }

    private fun setupHomeUiState(editPostRepository: EditPostRepository, site: SiteModel, isStoryPost: Boolean) {
        val prepublishingHomeUiStateList = mutableListOf<PrepublishingHomeItemUiState>().apply {
            if (isStoryPost) {
                add(
                        StoryTitleUiState(
                                storyTitle = UiStringText(StringUtils.notNullStr(editPostRepository.title)),
                                storyThumbnailUrl = storyRepositoryWrapper.getCurrentStoryThumbnailUrl()
                        ) { storyTitle ->
                            onStoryTitleChanged(storyTitle)
                        })
            } else {
                add(HeaderUiState(UiStringText(site.name), StringUtils.notNullStr(site.iconUrl)))
            }

            add(
                    HomeUiState(
                            actionType = VISIBILITY,
                            actionResult = getPostVisibilityUseCase.getVisibility(editPostRepository).textRes,
                            actionClickable = true,
                            onActionClicked = ::onActionClicked
                    )
            )
            if (editPostRepository.status != PostStatus.PRIVATE) {
                add(
                        HomeUiState(
                                actionType = PUBLISH,
                                actionResult = editPostRepository.getEditablePost()
                                        ?.let { UiStringText(postSettingsUtils.getPublishDateLabel(it)) },
                                actionClickable = true,
                                onActionClicked = ::onActionClicked
                        )
                )
            } else {
                add(
                        HomeUiState(
                                actionType = PUBLISH,
                                actionResult = editPostRepository.getEditablePost()
                                        ?.let { UiStringText(postSettingsUtils.getPublishDateLabel(it)) },
                                actionTypeColor = R.color.prepublishing_action_type_disabled_color,
                                actionResultColor = R.color.prepublishing_action_result_disabled_color,
                                actionClickable = false,
                                onActionClicked = null
                        )
                )
            }

            if (!editPostRepository.isPage) {
                add(HomeUiState(
                        actionType = TAGS,
                        actionResult = getPostTagsUseCase.getTags(editPostRepository)?.let { UiStringText(it) }
                                ?: run { UiStringRes(R.string.prepublishing_nudges_home_tags_not_set) },
                        actionClickable = true,
                        onActionClicked = ::onActionClicked
                )
                )
            }

            add(getButtonUiStateUseCase.getUiState(editPostRepository, site) { publishPost ->
                analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_PUBLISH_NOW_TAPPED)
                _onSubmitButtonClicked.postValue(Event(publishPost))
            })
        }.toList()

        _uiState.postValue(prepublishingHomeUiStateList)
    }

    private fun onStoryTitleChanged(storyTitle: String) {
        updateStoryTitleJob?.cancel()
        updateStoryTitleJob = launch(bgDispatcher) {
            delay(THROTTLE_DELAY)
            storyRepositoryWrapper.setCurrentStoryTitle(storyTitle)
            updateStoryPostTitleUseCase.updateStoryTitle(storyTitle, editPostRepository)
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateStoryTitleJob?.cancel()
    }

    private fun onActionClicked(actionType: ActionType) {
        _onActionClicked.postValue(Event(actionType))
    }
}
