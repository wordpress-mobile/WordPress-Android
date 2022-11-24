package org.wordpress.android.fluxc.store.mobile

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.JetpackMigrationRestClient
import org.wordpress.android.fluxc.store.mobile.MigrationCompleteFetchedPayload.Error
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackMigrationStore @Inject constructor(
    private val jetpackMigrationClient: JetpackMigrationRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun migrationComplete(
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "post migration-complete") {
        return@withDefaultContext jetpackMigrationClient.migrationComplete(::Error)
    }
}

sealed class MigrationCompleteFetchedPayload {
    object Success : MigrationCompleteFetchedPayload()
    class Error(val error: BaseNetworkError?) : MigrationCompleteFetchedPayload()
}
