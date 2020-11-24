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
                DummyActivityType(1, "Dummy Users"),
                DummyActivityType(2,"Dummy Backup"),
                DummyActivityType(3, "Dummy Comments"),
                DummyActivityType(4, "Dummy Posts")
        )
        )
    }

    data class DummyAvailableActivityTypesResponse(
        val isError: Boolean,
        val activityTypes: List<DummyActivityType> = listOf()
    )

    data class DummyActivityType(val id: Int, val name: String)
}
