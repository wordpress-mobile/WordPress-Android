package org.wordpress.android.ui.activitylog.list.filter

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class will be deleted as soon as we implement fetchAvailableActivityTypes endpoint in ActivityLogStore.
 */
@Singleton
class DummyActivityTypesProvider @Inject constructor() {
    suspend fun fetchAvailableActivityTypes(siteId: Long): DummyAvailableActivityTypesResponse {
        delay(1000)
        return DummyAvailableActivityTypesResponse(
                false, listOf(
                DummyActivityType("Dummy Users"),
                DummyActivityType("Dummy Backup"),
                DummyActivityType("Dummy Comments"),
                DummyActivityType("Dummy Posts"),
        )
        )
    }

    data class DummyAvailableActivityTypesResponse(val isError: Boolean, val activityTypes: List<DummyActivityType>)
    data class DummyActivityType(val name: String)
}