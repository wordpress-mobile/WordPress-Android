package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.PublishButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.prepublishing.home.usecases.GetPublishButtonLabelUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.posts.prepublishing.visibility.usecases.GetPostVisibilityUseCase
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PrepublishingHomeViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val getPostVisibilityUseCase: GetPostVisibilityUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private val postSettingsUtils: PostSettingsUtils,
    private val getPublishButtonLabelUseCase: GetPublishButtonLabelUseCase
) : ViewModel() {
    private var isStarted = false

    private val _uiState = MutableLiveData<List<PrepublishingHomeItemUiState>>()
    val uiState: LiveData<List<PrepublishingHomeItemUiState>> = _uiState

    private val _onActionClicked = MutableLiveData<Event<ActionType>>()
    val onActionClicked: LiveData<Event<ActionType>> = _onActionClicked

    private val _onPublishButtonClicked = MutableLiveData<Event<Unit>>()
    val onPublishButtonClicked: LiveData<Event<Unit>> = _onPublishButtonClicked

    fun start(editPostRepository: EditPostRepository, site: SiteModel) {
        if (isStarted) return
        isStarted = true

        setupHomeUiState(editPostRepository, site)
    }

    private fun setupHomeUiState(editPostRepository: EditPostRepository, site: SiteModel) {
        val prepublishingHomeUiStateList = listOf(
                HeaderUiState(UiStringText(site.name), StringUtils.notNullStr(site.iconUrl)),
                HomeUiState(
                        actionType = VISIBILITY,
                        actionResult = getPostVisibilityUseCase.getVisibility(editPostRepository).textRes,
                        onActionClicked = ::onActionClicked
                ),
                HomeUiState(
                        actionType = PUBLISH,
                        actionResult = editPostRepository.getPost()?.let { postImmutableModel ->
                            val label = postSettingsUtils.getPublishDateLabel(postImmutableModel)
                            if (label.isNotEmpty()) {
                                UiStringText(label)
                            } else {
                                UiStringRes(R.string.immediately)
                            }
                        },
                        onActionClicked = ::onActionClicked
                ),
                HomeUiState(
                        actionType = TAGS,
                        actionResult = getPostTagsUseCase.getTags(editPostRepository)?.let { UiStringText(it) }
                                ?: run { UiStringRes(R.string.prepublishing_nudges_home_tags_not_set) },
                        onActionClicked = ::onActionClicked
                ),
                PublishButtonUiState(UiStringRes(R.string.prepublishing_nudges_home_publish_button)) {
                    analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_PUBLISH_NOW_TAPPED)
                    _onPublishButtonClicked.postValue(Event(Unit))
                }
        )

        _uiState.postValue(prepublishingHomeUiStateList)
    }

    private fun onActionClicked(actionType: ActionType) {
        _onActionClicked.postValue(Event(actionType))
    }
}
