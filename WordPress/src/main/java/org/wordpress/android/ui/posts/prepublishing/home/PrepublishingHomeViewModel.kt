package org.wordpress.android.ui.posts.prepublishing.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.posts.GetPostTagsUseCase
import org.wordpress.android.ui.posts.PostSettingsUtils
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType.PrepublishingScreenNavigation
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.SocialUiState
import org.wordpress.android.ui.posts.prepublishing.home.usecases.GetButtonUiStateUseCase
import org.wordpress.android.ui.posts.trackPrepublishingNudges
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PrepublishingHomeViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val postSettingsUtils: PostSettingsUtils,
    private val getButtonUiStateUseCase: GetButtonUiStateUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private var updateStoryTitleJob: Job? = null
    private lateinit var editPostRepository: EditPostRepository

    private val _onActionClicked = MutableLiveData<Event<ActionType>>()
    val onActionClicked: LiveData<Event<ActionType>> = _onActionClicked

    private val _onSubmitButtonClicked = MutableLiveData<Event<PublishPost>>()
    val onSubmitButtonClicked: LiveData<Event<PublishPost>> = _onSubmitButtonClicked

    private val _uiState = MutableLiveData<List<PrepublishingHomeItemUiState>>()
    private var _socialUiState: MutableLiveData<SocialUiState> = MutableLiveData(SocialUiState.Hidden)

    val uiState: LiveData<List<PrepublishingHomeItemUiState>> = merge(
        _uiState,
        _socialUiState
    ) { list, socialUiState ->
        list?.map { item ->
            if (item is SocialUiState) {
                socialUiState ?: SocialUiState.Hidden
            } else {
                item
            }
        }
    }

    fun start(editPostRepository: EditPostRepository, site: SiteModel) {
        this.editPostRepository = editPostRepository
        if (isStarted) return
        isStarted = true

        setupHomeUiState(editPostRepository, site)
    }

    private fun setupHomeUiState(
        editPostRepository: EditPostRepository,
        site: SiteModel
    ) {
        val prepublishingHomeUiStateList = mutableListOf<PrepublishingHomeItemUiState>().apply {
            add(
                HeaderUiState(
                    UiStringText(site.name),
                    StringUtils.notNullStr(site.iconUrl)
                )
            )

            if (editPostRepository.status != PostStatus.PRIVATE) {
                showPublicPost(editPostRepository)
            } else {
                showPrivatePost(editPostRepository)
            }

            if (!editPostRepository.isPage) {
                showNotSetPost(editPostRepository, site)
            } else {
                UiStringRes(R.string.prepublishing_nudges_home_categories_not_set)
            }

            val categoriesString = getCategoriesUseCase.getPostCategoriesString(
                editPostRepository,
                site
            )

            if (!editPostRepository.isPage) {
                add(HomeUiState(
                    navigationAction = PrepublishingScreenNavigation.Categories,
                    actionResult = if (categoriesString.isNotEmpty()) {
                        UiStringText(categoriesString)
                    } else {
                        run { UiStringRes(R.string.prepublishing_nudges_home_categories_not_set) }
                    },
                    actionClickable = true,
                    onNavigationActionClicked = ::onActionClicked
                ))
            }

            add(SocialUiState.Hidden)

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
        add(
            HomeUiState(
                navigationAction = PrepublishingScreenNavigation.Tags,
                actionResult = getPostTagsUseCase.getTags(editPostRepository)
                    ?.let { UiStringText(it) }
                    ?: run { UiStringRes(R.string.prepublishing_nudges_home_tags_not_set) },
                actionClickable = true,
                onNavigationActionClicked = ::onActionClicked
            )
        )

        val categoryString: String = getCategoriesUseCase.getPostCategoriesString(
            editPostRepository,
            site
        )
        if (categoryString.isNotEmpty()) {
            UiStringText(categoryString)
        }
    }

    private fun MutableList<PrepublishingHomeItemUiState>.showPrivatePost(
        editPostRepository: EditPostRepository
    ) {
        add(
            HomeUiState(
                navigationAction = PrepublishingScreenNavigation.Publish,
                actionResult = editPostRepository.getEditablePost()
                    ?.let {
                        UiStringText(
                            postSettingsUtils.getPublishDateLabel(
                                it
                            )
                        )
                    },
                actionTypeColor = R.color.prepublishing_action_type_disabled_color,
                actionResultColor = R.color.prepublishing_action_result_disabled_color,
                actionClickable = false,
                onNavigationActionClicked = null
            )
        )
    }

    private fun MutableList<PrepublishingHomeItemUiState>.showPublicPost(
        editPostRepository: EditPostRepository
    ) {
        add(
            HomeUiState(
                navigationAction = PrepublishingScreenNavigation.Publish,
                actionResult = editPostRepository.getEditablePost()
                    ?.let {
                        UiStringText(
                            postSettingsUtils.getPublishDateLabel(
                                it
                            )
                        )
                    },
                actionClickable = true,
                onNavigationActionClicked = ::onActionClicked
            )
        )
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

    fun updateJetpackSocialState(
        state: EditorJetpackSocialViewModel.JetpackSocialUiState?
    ) {
        val isPostPublished = editPostRepository.getPost()?.status == PostStatus.PUBLISHED.toString()
        val newState = state?.takeUnless { isPostPublished }?.let {
            SocialUiState.Visible(
                it,
                onItemClicked = { onActionClicked(PrepublishingScreenNavigation.Social) }
            )
        } ?: SocialUiState.Hidden

        _socialUiState.postValue(newState)
    }
}
