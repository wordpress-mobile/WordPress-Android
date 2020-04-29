package org.wordpress.android.ui.posts.prepublishing.visibility

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLIC
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.VisibilityUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PrepublishingVisibilityViewModel @Inject constructor(
    private val getPostVisibilityUseCase: GetPostVisibilityUseCase,
    private val updatePostPasswordUseCase: UpdatePostPasswordUseCase,
    private val updatePostStatusUseCase: UpdatePostStatusUseCase
) : ViewModel() {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository

    private val _uiState = MutableLiveData<List<VisibilityUiState>>()
    val uiState: LiveData<List<VisibilityUiState>> = _uiState

    private val _showPasswordDialog = MutableLiveData<Event<Unit>>()
    val showPasswordDialog: LiveData<Event<Unit>> = _showPasswordDialog

    fun start(editPostRepository: EditPostRepository) {
        if (isStarted) return
        isStarted = true

        this.editPostRepository = editPostRepository
        updateUiState()
    }

    private fun updateUiState() {
        val currentVisibility = getPostVisibilityUseCase.getVisibility(editPostRepository)
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
        when {
            visibility == PASSWORD_PROTECTED -> _showPasswordDialog.postValue(Event(Unit))

            editPostRepository.password.isNotEmpty() -> {
                updatePostPasswordUseCase.updatePassword("", editPostRepository) {
                    updatePostStatus(visibility)
                }
            }

            else -> updatePostStatus(visibility)
        }
    }

    private fun updatePostStatus(visibility: Visibility) {
        updatePostStatusUseCase.updatePostStatus(visibility, editPostRepository, ::updateUiState)
    }

    fun onPostPasswordChanged(password: String) {
        updatePostPasswordUseCase.updatePassword(password, editPostRepository, ::updateUiState)
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

