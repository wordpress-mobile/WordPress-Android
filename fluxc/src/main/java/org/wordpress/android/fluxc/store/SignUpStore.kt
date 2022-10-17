package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.rest.wpcom.account.SignUpRestClient
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class SignUpStore @Inject constructor(
    private val signUpRestClient: SignUpRestClient,
    private val coroutineEngine: CoroutineEngine,

    ) {
    suspend fun fetchUserNameSuggestions(username: String): UsernameSuggestionsResult {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchUserNameSuggestions") {
            val result = signUpRestClient.fetchUsernameSuggestions(username)
            when {
                result.isError -> UsernameSuggestionsResult(UsernameSuggestionsError(result.error?.message))
                result.result.isNullOrEmpty() -> UsernameSuggestionsResult(UsernameSuggestionsError("Empty result"))
                else -> UsernameSuggestionsResult(result.result)
            }
        }
    }

    suspend fun createWpAccount(email: String, password: String, username: String) {

    }

    data class UsernameSuggestionsResult(
        val suggestions: List<String>
    ) : Payload<UsernameSuggestionsError>() {
        constructor(error: UsernameSuggestionsError) : this(emptyList()) {
            this.error = error
        }
    }

    class UsernameSuggestionsError(
        val message: String? = null
    ) : OnChangedError
}