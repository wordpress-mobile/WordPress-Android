package org.wordpress.android.util.experiments

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.FetchAssignmentsPayload
import org.wordpress.android.fluxc.store.ExperimentStore.Platform
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.experiments.ExPlat.RefreshStrategy.ALWAYS
import org.wordpress.android.util.experiments.ExPlat.RefreshStrategy.IF_STALE
import org.wordpress.android.util.experiments.ExPlat.RefreshStrategy.NEVER
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ExPlat
@Inject constructor(
    private val experimentStore: ExperimentStore,
    private val appLog: AppLogWrapper,
    @Named(APPLICATION_SCOPE) private val coroutineScope: CoroutineScope
) {
    private val platform = Platform.WORDPRESS_COM

    fun refreshIfNeeded() {
        getAssignments(refreshStrategy = IF_STALE)
    }

    fun forceRefresh() {
        getAssignments(refreshStrategy = ALWAYS)
    }

    fun clear() {
        appLog.d(T.API, "ExPlat: clearing cached assignments")
        experimentStore.clearCachedAssignments()
    }

    internal fun getVariation(experiment: Experiment, shouldRefreshIfStale: Boolean) =
            getAssignments(if (shouldRefreshIfStale) IF_STALE else NEVER).getVariationForExperiment(experiment.name)

    private fun getAssignments(refreshStrategy: RefreshStrategy): Assignments {
        val cachedAssignments = experimentStore.getCachedAssignments() ?: Assignments()
        if (refreshStrategy == ALWAYS || (refreshStrategy == IF_STALE && cachedAssignments.isStale())) {
            coroutineScope.launch { fetchAssignments() }
        }
        return cachedAssignments
    }

    private suspend fun fetchAssignments() {
        val result = experimentStore.fetchAssignments(FetchAssignmentsPayload(platform))
        if (result.isError) {
            appLog.d(T.API, "ExPlat: fetching assignments failed with result: ${result.error}")
        } else {
            appLog.d(T.API, "ExPlat: fetching assignments successful with result: ${result.assignments}")
        }
    }

    private enum class RefreshStrategy { ALWAYS, IF_STALE, NEVER }
}
