package org.wordpress.android.ui.posts.prepublishing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.ui.posts.prepublishing.PrepublishingVisibilityItemUiState.Visibility
import org.wordpress.android.ui.posts.prepublishing.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.PrepublishingVisibilityItemUiState.Visibility.PUBLIC
import org.wordpress.android.ui.posts.prepublishing.PrepublishingVisibilityItemUiState.VisibilityUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes

class PrepublishingVisibilityViewModel : ViewModel() {
    private var isStarted = false

    private val _uiState = MutableLiveData<List<VisibilityUiState>>()
    val uiState: LiveData<List<VisibilityUiState>> = _uiState

    fun start() {
        if (isStarted) return
        isStarted = true
        createVisibilityUiStates(PUBLIC)
    }

    private fun createVisibilityUiStates(currentVisibility: Visibility) {
        val items = listOf(
                VisibilityUiState(
                        visibility = PUBLIC,
                        checked = currentVisibility == PUBLIC,
                        onItemTapped = ::onVisibilityItemTapped
                ),
                VisibilityUiState(
                        visibility = PASSWORD_PROTECTED,
                        checked = currentVisibility == PASSWORD_PROTECTED,
                        onItemTapped = ::onVisibilityItemTapped
                ),
                VisibilityUiState(
                        visibility = PRIVATE,
                        checked = currentVisibility == PRIVATE,
                        onItemTapped = ::onVisibilityItemTapped
                )
        )

        _uiState.postValue(items)
    }

    private fun onVisibilityItemTapped(visibility: Visibility) {

    }
}

sealed class PrepublishingVisibilityItemUiState {
    data class VisibilityUiState(
        val visibility: Visibility,
        val checked: Boolean,
        val onItemTapped: ((Visibility) -> Unit)
    ) : PrepublishingVisibilityItemUiState()

    enum class Visibility(val textRes: UiStringRes) {
        PUBLIC(UiStringRes(R.string.prepublishing_nudges_visibility_public)),
        PASSWORD_PROTECTED(UiStringRes(R.string.prepublishing_nudges_visibility_password)),
        PRIVATE(UiStringRes(R.string.prepublishing_nudges_visibility_private))
    }
}

