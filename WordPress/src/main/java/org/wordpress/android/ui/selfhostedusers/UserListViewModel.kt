package org.wordpress.android.ui.selfhostedusers

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.wordpress.android.modules.UI_THREAD
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

    fun setAuthenticatedSite(authenticatedSite: AuthenticatedSite) {
        apiClient = null
        authRepository.authenticationForSite(authenticatedSite)?.let {
            apiClient = WpApiClient(siteUrl = authenticatedSite.url, authentication = it)
        }
    }

    fun fetchUsers(): List<UserWithEditContext> {
        apiClient?.let { apiClient ->
            val usersResult = runBlocking {
                apiClient.request { requestBuilder ->
                    requestBuilder.users().listWithEditContext(params = UserListParams())
                }
            }
            return when (usersResult) {
                is WpRequestResult.WpRequestSuccess -> usersResult.data
                else -> listOf()
            }
        }
        return listOf()
    }
}
