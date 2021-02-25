package org.wordpress.android.ui.jetpack.scan.details.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetThreatModelUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val scanStore: ScanStore
) {
    suspend fun get(threatId: Long) = withContext(ioDispatcher) {
        scanStore.getThreatModelByThreatId(threatId)
    }
}
