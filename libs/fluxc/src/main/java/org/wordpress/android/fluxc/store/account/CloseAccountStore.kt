package org.wordpress.android.fluxc.store.account

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.rest.wpcom.account.close.CloseAccountRestClient
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.store.account.CloseAccountStore.CloseAccountErrorType.EXISTING_ATOMIC_SITES
import org.wordpress.android.fluxc.store.account.CloseAccountStore.CloseAccountErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloseAccountStore @Inject constructor(
    private val restClient: CloseAccountRestClient,
    private val coroutineEngine: CoroutineEngine,
) {
    suspend fun closeAccount(): CloseAccountResult {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "closeAccount") {
            val result = restClient.closeAccount()
            when {
                result.isError -> {
                    val errorType = when (result.error?.apiError) {
                        EXISTING_ATOMIC_SITES.errorKey -> EXISTING_ATOMIC_SITES
                        else -> GENERIC_ERROR
                    }
                    CloseAccountResult(
                        CloseAccountError(
                            type = errorType,
                            message = result.error?.message
                        )
                    )
                }

                else -> CloseAccountResult()
            }
        }
    }

    class CloseAccountResult() : Payload<CloseAccountError>() {
        constructor(error: CloseAccountError) : this() {
            this.error = error
        }
    }

    class CloseAccountError(
        val type: CloseAccountErrorType,
        val message: String? = null
    ) : OnChangedError


    enum class CloseAccountErrorType(val errorKey: String) {
        EXISTING_ATOMIC_SITES("atomic-site"),
        GENERIC_ERROR("generic-error")
    }
}
