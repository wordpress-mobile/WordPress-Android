package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMeScreen
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.map
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class MySiteViewModel
@Inject constructor(
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val displayUtilsWrapper: DisplayUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _currentAvatar = MediatorLiveData<String>()
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()

    val onNavigation: LiveData<Event<SiteNavigationAction>> = _onNavigation
    val uiModel: LiveData<UiModel> = merge(_currentAvatar, selectedSiteRepository.siteSelected) { avatar, siteId ->
        val state = if (siteId != null) {
            State.SiteSelected
        } else {
            // Hide actionable empty view image when screen height is under 600 pixels.
            val shouldShowImage = displayUtilsWrapper.getDisplayPixelHeight() >= 600
            State.NoSites(shouldShowImage)
        }
        UiModel(avatar.orEmpty(), state)
    }
    val onSiteSelected: LiveData<Event<Int>> = selectedSiteRepository.siteSelected
            .map { Event(it) }

    fun refresh() {
        launch {
            selectedSiteRepository.updateSiteSettingsIfNecessary()
        }
        _currentAvatar.value = accountStore.account?.avatarUrl.orEmpty()
    }

    fun onAvatarPressed() {
        _onNavigation.value = Event(OpenMeScreen)
    }

    fun onAddSitePressed() {
        _onNavigation.value = Event(AddNewSite(accountStore.hasAccessToken()))
    }

    data class UiModel(
        val accountAvatarUrl: String,
        val state: State
    )

    sealed class State {
        object SiteSelected : State()
        data class NoSites(val shouldShowImage: Boolean) : State()
    }
}
