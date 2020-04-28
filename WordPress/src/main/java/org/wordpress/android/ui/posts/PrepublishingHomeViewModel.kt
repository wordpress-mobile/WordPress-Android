package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.PrepublishingButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.PrepublishingHomeHeaderUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.PrepublishingHomeUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PrepublishingHomeViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val postSettingsUtils: PostSettingsUtils
) : ViewModel() {
    private var isStarted = false

    private val _uiState = MutableLiveData<List<PrepublishingHomeItemUiState>>()
    val uiState: LiveData<List<PrepublishingHomeItemUiState>> = _uiState

    private val _onActionClicked = MutableLiveData<Event<ActionType>>()
    val onActionClicked: LiveData<Event<ActionType>> = _onActionClicked

    fun start(editPostRepository: EditPostRepository, site: SiteModel) {
        if (isStarted) return
        isStarted = true

        setupHomeUiState(editPostRepository, site)
    }

    // TODO remove hardcoded Public with live data from the EditPostRepository / user changes.
    private fun setupHomeUiState(editPostRepository: EditPostRepository, site: SiteModel) {
        val prepublishingHomeUiStateList = listOf(
                PrepublishingHomeHeaderUiState(UiStringText(site.name), site.iconUrl?.let { it } ?: run { "" }),
                PrepublishingHomeUiState(
                        actionType = PUBLISH,
                        actionResult = editPostRepository.getPost()?.let { postImmutableModel ->
                            UiStringText(
                                    postSettingsUtils.getPublishDateLabel(postImmutableModel)
                            )
                        },
                        onActionClicked = ::onActionClicked
                ),
                PrepublishingHomeUiState(
                        actionType = VISIBILITY,
                        actionResult = UiStringText("Public"),
                        onActionClicked = ::onActionClicked
                ),
                PrepublishingHomeUiState(
                        actionType = TAGS,
                        actionResult = getPostTagsUseCase.getTags(editPostRepository)?.let { UiStringText(it) }
                                ?: run { UiStringRes(R.string.prepublishing_nudges_home_tags_not_set) },
                        onActionClicked = ::onActionClicked
                ),
                PrepublishingButtonUiState(UiStringRes(R.string.prepublishing_nudges_home_publish_button), {})
        )

        _uiState.postValue(prepublishingHomeUiStateList)
    }

    private fun onActionClicked(actionType: ActionType) {
        _onActionClicked.postValue(Event(actionType))
    }
}
