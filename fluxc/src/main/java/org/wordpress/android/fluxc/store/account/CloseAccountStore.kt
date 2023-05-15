package org.wordpress.android.fluxc.store.account

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.rest.wpcom.account.close.CloseAccountRestClient
import org.wordpress.android.fluxc.store.Store.OnChangedError
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
                result.isError -> CloseAccountResult(CloseAccountError(result.error?.message))
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
        val message: String? = null
    ) : OnChangedError
}
