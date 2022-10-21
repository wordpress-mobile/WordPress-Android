package org.wordpress.android.fluxc.store.signup

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.rest.wpcom.account.signup.SignUpRestClient
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignUpStore @Inject constructor(
    private val signUpRestClient: SignUpRestClient,
    private val coroutineEngine: CoroutineEngine,
) {
    companion object {
        const val EMPTY_SUCCESSFUL_RESPONSE = "Empty successful response"
    }

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

    suspend fun createWpAccount(email: String, password: String, username: String): CreateWpAccountResult {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createWpAccount") {
            val result = signUpRestClient.createWPAccount(email, password, username)
            when {
                result.isError -> CreateWpAccountResult(
                    CreateWpAccountError(result.error?.apiError)
                )
                result.result == null -> CreateWpAccountResult(
                    CreateWpAccountError(EMPTY_SUCCESSFUL_RESPONSE)
                )
                else -> CreateWpAccountResult(result.result.success, result.result.token)
            }
        }
    }

    data class CreateWpAccountResult(
        val success: Boolean,
        val token: String
    ) : Payload<CreateWpAccountError>() {
        constructor(error: CreateWpAccountError) : this(false, "") {
            this.error = error
        }
    }

    class CreateWpAccountError(
        val apiError: String?
    ) : OnChangedError
}
