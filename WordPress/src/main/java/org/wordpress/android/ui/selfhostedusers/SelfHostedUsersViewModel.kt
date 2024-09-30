package org.wordpress.android.ui.selfhostedusers

import android.app.Activity
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.viewmodel.ScopedViewModel
import uniffi.wp_api.UserWithEditContext
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SelfHostedUsersViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
) : ScopedViewModel(mainDispatcher) {
    private val users = ArrayList<UserWithEditContext>()

    private val _uiState = MutableStateFlow<SelfHostedUserState>(SelfHostedUserState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchUsers()
    }

    // TODO this uses dummy data for now - no network request is involved yet
    @Suppress("MagicNumber")
    private fun fetchUsers() {
        _uiState.value = SelfHostedUserState.Loading
        launch {
            delay(1000L)
            users.clear()
            users.addAll(SampleUsers.getSampleUsers())
            _uiState.value = SelfHostedUserState.UserList(users)
        }
    }

    fun onCloseClick(context: Context) {
        (context as? Activity)?.finish()
    }

    fun onUserClick(user: UserWithEditContext) {
        _uiState.value = SelfHostedUserState.UserDetail(user)
    }

    sealed class SelfHostedUserState {
        data object Loading : SelfHostedUserState()
        data class UserList(val users: List<UserWithEditContext>) : SelfHostedUserState()
        data class UserDetail(val user: UserWithEditContext) : SelfHostedUserState()
        data object Offline : SelfHostedUserState()
    }
}
