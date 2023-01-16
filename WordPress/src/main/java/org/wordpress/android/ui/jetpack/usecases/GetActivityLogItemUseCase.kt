package org.wordpress.android.ui.jetpack.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetActivityLogItemUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val activityLogStore: ActivityLogStore
) {
    suspend fun get(
        activityId: String
    ): ActivityLogModel? =
        withContext(ioDispatcher) {
            activityLogStore.getActivityLogItemByActivityId(activityId)
        }
}
