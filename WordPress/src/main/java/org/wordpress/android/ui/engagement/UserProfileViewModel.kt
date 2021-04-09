package org.wordpress.android.ui.engagement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.engagement.BottomSheetAction.HideBottomSheet
import org.wordpress.android.ui.engagement.BottomSheetAction.ShowBottomSheet
import org.wordpress.android.ui.engagement.BottomSheetUiState.UserProfileUiState
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet.UserProfile
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class UserProfileViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) { // TODOD: define if we need any THREAD/ScopedViewModel here
    private val _onBottomSheetAction = MutableLiveData<Event<BottomSheetAction>>()
    val onBottomSheetAction: LiveData<Event<BottomSheetAction>> = _onBottomSheetAction

    private val _bottomSheetUiState = MutableLiveData<BottomSheetUiState>()
    val bottomSheetUiState: LiveData<BottomSheetUiState> = _bottomSheetUiState

    fun onBottomSheetOpen(
        userProfile: UserProfile,
        onClick: ((siteId: Long, siteUrl: String) -> Unit)?
    ) {
        _bottomSheetUiState.value = with(userProfile) {
            UserProfileUiState(
                    userAvatarUrl = userAvatarUrl,
                    blavatarUrl = blavatarUrl,
                    userName = userName,
                    userLogin = userLogin,
                    userBio = userBio,
                    siteTitle = siteTitle,
                    siteUrl = siteUrl,
                    siteId = siteId,
                    onClick = onClick
            )
        }
        _onBottomSheetAction.value = Event(ShowBottomSheet)
    }

    fun onBottomSheetCancelled() {
        _onBottomSheetAction.value = Event(HideBottomSheet)
    }

    companion object {
        const val USER_PROFILE_VM_KEY = "USER_PROFILE_VIEW_MODEL_KEY"
    }
}
