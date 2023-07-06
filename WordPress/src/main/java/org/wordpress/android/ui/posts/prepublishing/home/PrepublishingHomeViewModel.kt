package org.wordpress.android.ui.posts.prepublishing.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.posts.GetPostTagsUseCase
import org.wordpress.android.ui.posts.PostSettingsUtils
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType.CATEGORIES
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.SocialUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.StoryTitleUiState
import org.wordpress.android.ui.posts.prepublishing.home.usecases.GetButtonUiStateUseCase
import org.wordpress.android.ui.posts.trackPrepublishingNudges
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.usecase.UpdateStoryPostTitleUseCase
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.JetpackSocialFeatureConfig
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

private const val THROTTLE_DELAY = 500L

class PrepublishingHomeViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val postSettingsUtils: PostSettingsUtils,
    private val getButtonUiStateUseCase: GetButtonUiStateUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val storyRepositoryWrapper: StoryRepositoryWrapper,
    private val updateStoryPostTitleUseCase: UpdateStoryPostTitleUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val socialFeatureConfig: JetpackSocialFeatureConfig,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private var updateStoryTitleJob: Job? = null
    private lateinit var editPostRepository: EditPostRepository

    private val _uiState = MutableLiveData<List<PrepublishingHomeItemUiState>>()
    val uiState: LiveData<List<PrepublishingHomeItemUiState>> = _uiState

    private val _storyTitleUiState = MutableLiveData<StoryTitleUiState>()
    val storyTitleUiState: LiveData<StoryTitleUiState> = _storyTitleUiState

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

    private fun setupHomeUiState(
        editPostRepository: EditPostRepository,
        site: SiteModel,
        isStoryPost: Boolean
    ) {
        val prepublishingHomeUiStateList = mutableListOf<PrepublishingHomeItemUiState>().apply {
            if (isStoryPost) {
                _storyTitleUiState.postValue(StoryTitleUiState(
                    storyTitle = UiString.UiStringText(StringUtils.notNullStr(editPostRepository.title)),
                    storyThumbnailUrl = storyRepositoryWrapper.getCurrentStoryThumbnailUrl()
                ) { storyTitle ->
                    onStoryTitleChanged(storyTitle)
                })
            } else {
                add(
                    HeaderUiState(
                        UiString.UiStringText(site.name),
                        StringUtils.notNullStr(site.iconUrl)
                    )
                )
            }

            if (editPostRepository.status != PostStatus.PRIVATE) {
                showPublicPost(editPostRepository)
            } else {
                showPrivatePost(editPostRepository)
            }

            if (!editPostRepository.isPage) {
                showNotSetPost(editPostRepository, site)
            } else {
                UiString.UiStringRes(R.string.prepublishing_nudges_home_categories_not_set)
            }

            val categoriesString = getCategoriesUseCase.getPostCategoriesString(
                editPostRepository,
                site
            )

            add(HomeUiState(
                actionType = CATEGORIES,
                actionResult = if (categoriesString.isNotEmpty()) {
                    UiString.UiStringText(categoriesString)
                } else {
                    run { UiString.UiStringRes(R.string.prepublishing_nudges_home_categories_not_set) }
                },
                actionClickable = true,
                onActionClicked = ::onActionClicked
            ))

            setupSocialItem()

            add(getButtonUiStateUseCase.getUiState(editPostRepository, site) { publishPost ->
                launch(bgDispatcher) {
                    waitForStoryTitleJobAndSubmit(publishPost)
                }
            })
        }.toList()

        _uiState.postValue(prepublishingHomeUiStateList)
    }

    private fun MutableList<PrepublishingHomeItemUiState>.showNotSetPost(
        editPostRepository: EditPostRepository,
        site: SiteModel
    ) {
        add(HomeUiState(
            actionType = TAGS,
            actionResult = getPostTagsUseCase.getTags(editPostRepository)
                ?.let { UiString.UiStringText(it) }
                ?: run { UiString.UiStringRes(R.string.prepublishing_nudges_home_tags_not_set) },
            actionClickable = true,
            onActionClicked = ::onActionClicked
        )
        )

        val categoryString: String = getCategoriesUseCase.getPostCategoriesString(
            editPostRepository,
            site
        )
        if (categoryString.isNotEmpty()) {
            UiString.UiStringText(categoryString)
        }
    }

    private fun MutableList<PrepublishingHomeItemUiState>.showPrivatePost(
        editPostRepository: EditPostRepository
    ) {
        add(
            HomeUiState(
                actionType = PUBLISH,
                actionResult = editPostRepository.getEditablePost()
                    ?.let {
                        UiString.UiStringText(
                            postSettingsUtils.getPublishDateLabel(
                                it
                            )
                        )
                    },
                actionTypeColor = R.color.prepublishing_action_type_disabled_color,
                actionResultColor = R.color.prepublishing_action_result_disabled_color,
                actionClickable = false,
                onActionClicked = null
            )
        )
    }

    private fun MutableList<PrepublishingHomeItemUiState>.showPublicPost(
        editPostRepository: EditPostRepository
    ) {
        add(
            HomeUiState(
                actionType = PUBLISH,
                actionResult = editPostRepository.getEditablePost()
                    ?.let {
                        UiString.UiStringText(
                            postSettingsUtils.getPublishDateLabel(
                                it
                            )
                        )
                    },
                actionClickable = true,
                onActionClicked = ::onActionClicked
            )
        )
    }

    private fun MutableList<PrepublishingHomeItemUiState>.setupSocialItem() {
        if (socialFeatureConfig.isEnabled()) {
            // TODO in other PR: use actual data, for now just using fake data
            add(
                SocialUiState(
                    title = UiString.UiStringText("Sharing to 2 of 3 accounts"),
                    description = UiString.UiStringText("27/30 social shares remaining"),
                    isLowOnShares = false,
                    connectionIcons = listOf(
                        SocialUiState.ConnectionIcon(R.drawable.ic_social_facebook, isEnabled = false),
                        SocialUiState.ConnectionIcon(R.drawable.ic_social_tumblr)
                    ),
                    onItemClicked = { /* TODO in other PR: open social section in bottom sheet */ },
                )
            )
        }
    }

    private fun onStoryTitleChanged(storyTitle: String) {
        updateStoryTitleJob?.cancel()
        updateStoryTitleJob = launch(bgDispatcher) {
            // there's a delay here since every single character change event triggers onStoryTitleChanged
            // and without a delay we would have multiple save operations being triggered unnecessarily.
            delay(THROTTLE_DELAY)
            storyRepositoryWrapper.setCurrentStoryTitle(storyTitle)
            updateStoryPostTitleUseCase.updateStoryTitle(storyTitle, editPostRepository)
        }
    }

    private suspend fun waitForStoryTitleJobAndSubmit(publishPost: PublishPost) {
        updateStoryTitleJob?.join()
        analyticsTrackerWrapper.trackPrepublishingNudges(AnalyticsTracker.Stat.EDITOR_POST_PUBLISH_NOW_TAPPED)
        _onSubmitButtonClicked.postValue(Event(publishPost))
    }

    override fun onCleared() {
        super.onCleared()
        updateStoryTitleJob?.cancel()
    }

    private fun onActionClicked(actionType: ActionType) {
        _onActionClicked.postValue(Event(actionType))
    }
}
