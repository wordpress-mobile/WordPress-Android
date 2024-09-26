package org.wordpress.android.ui.selfhostedusers

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.compose.components.ProgressDialogState
import org.wordpress.android.viewmodel.ScopedViewModel
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.UserListParams
import uniffi.wp_api.UserWithEditContext
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class UserListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val authRepository: AuthenticationRepository
) : ScopedViewModel(mainDispatcher) {
    private var apiClient: WpApiClient? = null

    private val _progressDialogState = MutableStateFlow<ProgressDialogState?>(null)
    val progressDialogState = _progressDialogState.asStateFlow()

    private val _users = MutableStateFlow<List<UserWithEditContext>>(emptyList())
    val users = _users.asStateFlow()

    fun setAuthenticatedSite(authenticatedSite: AuthenticatedSite) {
        apiClient = null
        authRepository.authenticationForSite(authenticatedSite)?.let {
            apiClient = WpApiClient(siteUrl = authenticatedSite.url, authentication = it)
        }
    }

    fun fetchUsers() {
        showProgressDialog(R.string.loading)
        try {
            _users.value = listOf()
            apiClient?.let { apiClient ->
                val usersResult = runBlocking {
                    apiClient.request { requestBuilder ->
                        requestBuilder.users().listWithEditContext(params = UserListParams())
                    }
                }
                _users.value = when (usersResult) {
                    is WpRequestResult.WpRequestSuccess -> usersResult.data
                    else -> listOf()
                }
            }
        } finally {
            hideProgressDialog()
        }
    }

    private fun showProgressDialog(
        @StringRes message: Int
    ) {
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
