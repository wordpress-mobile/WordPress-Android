package org.wordpress.android.ui.posts.prepublishing.visibility

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.DRAFT
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PENDING_REVIEW
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLISH
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.VisibilityUiState
import org.wordpress.android.ui.posts.prepublishing.visibility.usecases.GetPostVisibilityUseCase
import org.wordpress.android.ui.posts.prepublishing.visibility.usecases.UpdatePostPasswordUseCase
import org.wordpress.android.ui.posts.prepublishing.visibility.usecases.UpdateVisibilityUseCase
import org.wordpress.android.ui.posts.trackPrepublishingNudges
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PrepublishingVisibilityViewModel @Inject constructor(
    private val getPostVisibilityUseCase: GetPostVisibilityUseCase,
    private val updatePostPasswordUseCase: UpdatePostPasswordUseCase,
    private val updatePostStatusUseCase: UpdateVisibilityUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ViewModel() {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository

    private val _uiState = MutableLiveData<List<VisibilityUiState>>()
    val uiState: LiveData<List<VisibilityUiState>> = _uiState

    private val _showPasswordDialog = MutableLiveData<Event<Unit>>()
    val showPasswordDialog: LiveData<Event<Unit>> = _showPasswordDialog

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>> = _navigateToHomeScreen

    private val _dismissBottomSheet = MutableLiveData<Event<Unit>>()
    val dismissBottomSheet: LiveData<Event<Unit>> = _dismissBottomSheet

    private val _toolbarUiState = MutableLiveData<UiString>()
    val toolbarUiState: LiveData<UiString> = _toolbarUiState

    fun start(editPostRepository: EditPostRepository) {
        this.editPostRepository = editPostRepository
        if (isStarted) return
        isStarted = true

        setToolbarUiState()
        updateUiState()
    }

    private fun setToolbarUiState() {
        _toolbarUiState.postValue(UiStringRes(R.string.prepublishing_nudges_toolbar_title_visibility))
    }

    private fun updateUiState() {
        val currentVisibility = getPostVisibilityUseCase.getVisibility(editPostRepository)
        val items = listOf(
                VisibilityUiState(
                        visibility = PUBLISH,
                        checked = currentVisibility == PUBLISH,
                        onItemTapped = ::onVisibilityItemTapped
                ),
                VisibilityUiState(
                        visibility = DRAFT,
                        checked = currentVisibility == DRAFT,
                        onItemTapped = ::onVisibilityItemTapped
                ),
                VisibilityUiState(
                        visibility = PENDING_REVIEW,
                        checked = currentVisibility == PENDING_REVIEW,
                        onItemTapped = ::onVisibilityItemTapped
                ),
                VisibilityUiState(
                        visibility = PRIVATE,
                        checked = currentVisibility == PRIVATE,
                        onItemTapped = ::onVisibilityItemTapped
                ),
                VisibilityUiState(
                        visibility = PASSWORD_PROTECTED,
                        checked = currentVisibility == PASSWORD_PROTECTED,
                        onItemTapped = ::onVisibilityItemTapped
                )
        )

        _uiState.postValue(items)
    }

    private fun onVisibilityItemTapped(visibility: Visibility) {
        when {
            visibility == PASSWORD_PROTECTED -> _showPasswordDialog.postValue(Event(Unit))

            editPostRepository.password.isNotEmpty() -> {
                // clears the current password so that the PostStatus can be updated and getPostVisibilityUseCase
                // will utilize the PostStatus to determine the visibility when the uiState is being updated.
                val emptyPassword = ""
                updatePostPasswordUseCase.updatePassword(emptyPassword, editPostRepository) {
                    updatePostStatus(visibility)
                }
            }

            else -> updatePostStatus(visibility)
        }
    }

    private fun updatePostStatus(visibility: Visibility) {
        analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_VISIBILITY_CHANGED)
        updatePostStatusUseCase.updatePostVisibility(visibility, editPostRepository, ::updateUiState)
    }

    fun onPostPasswordChanged(password: String) {
        analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_PASSWORD_CHANGED)
        updatePostPasswordUseCase.updatePassword(password, editPostRepository, ::updateUiState)
    }

    fun onCloseButtonClicked() = _dismissBottomSheet.postValue(Event(Unit))

    fun onBackButtonClicked() = _navigateToHomeScreen.postValue(Event(Unit))
}

sealed class PrepublishingVisibilityItemUiState {
    data class VisibilityUiState(
        val visibility: Visibility,
        val checked: Boolean,
        val onItemTapped: ((Visibility) -> Unit)
    ) : PrepublishingVisibilityItemUiState()

    enum class Visibility(val textRes: UiStringRes) {
        PUBLISH(UiStringRes(R.string.post_status_publish_post)),
        DRAFT(UiStringRes(R.string.post_status_draft)),
        PENDING_REVIEW(UiStringRes(R.string.post_status_pending_review)),
        PRIVATE(UiStringRes(R.string.post_status_post_private)),
        PASSWORD_PROTECTED(UiStringRes(R.string.prepublishing_nudges_visibility_password))
    }
}
