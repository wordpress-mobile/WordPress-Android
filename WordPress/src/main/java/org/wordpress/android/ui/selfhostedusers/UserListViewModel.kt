package org.wordpress.android.ui.selfhostedusers

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.compose.components.ProgressDialogState
import org.wordpress.android.viewmodel.ScopedViewModel
import uniffi.wp_api.UserWithEditContext
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class UserListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
) : ScopedViewModel(mainDispatcher) {
    private val _progressDialogState = MutableStateFlow<ProgressDialogState?>(null)
    val progressDialogState = _progressDialogState.asStateFlow()

    private val _users = MutableStateFlow<List<UserWithEditContext>>(emptyList())
    val users = _users.asStateFlow()

    // TODO this uses dummy data for now - no network request is involved yet
    fun fetchUsers() {
        showProgressDialog(R.string.loading)
        _users.value = listOf()
        launch {
            delay(1000L)
            _users.value = SampleUsers.getSampleUsers()
            hideProgressDialog()
        }
    }

    private fun showProgressDialog(@StringRes message: Int) {
        _progressDialogState.value =
            ProgressDialogState(
                message = message,
                showCancel = false,
                dismissible = false
            )
    }

    private fun hideProgressDialog() {
        _progressDialogState.value = null
    }

    fun onCloseClick(context: Context) {
        (context as? Activity)?.finish()
    }
}
