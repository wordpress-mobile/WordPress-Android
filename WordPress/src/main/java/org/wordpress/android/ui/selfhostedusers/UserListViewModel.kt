package org.wordpress.android.ui.selfhostedusers

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.UserListParams
import uniffi.wp_api.UserWithEditContext
import javax.inject.Inject

class UserListViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository
) : ViewModel()
{
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
