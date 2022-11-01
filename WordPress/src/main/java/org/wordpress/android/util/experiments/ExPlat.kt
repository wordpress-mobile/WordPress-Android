package org.wordpress.android.util.experiments

import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.Variation
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.Platform
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.experiments.ExPlat.RefreshStrategy.ALWAYS
import org.wordpress.android.util.experiments.ExPlat.RefreshStrategy.IF_STALE
import org.wordpress.android.util.experiments.ExPlat.RefreshStrategy.NEVER
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ExPlat
@Inject constructor(
    private val experiments: Lazy<Set<Experiment>>,
    private val experimentStore: ExperimentStore,
    private val appLog: AppLogWrapper,
    private val accountStore: AccountStore,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @Named(APPLICATION_SCOPE) private val coroutineScope: CoroutineScope
) {
    private val platform = Platform.WORDPRESS_ANDROID
    private val activeVariations = mutableMapOf<String, Variation>()
    private val experimentNames by lazy { experiments.get().map { it.name } }

    fun refreshIfNeeded() {
        refresh(refreshStrategy = IF_STALE)
    }

    fun forceRefresh() {
        refresh(refreshStrategy = ALWAYS)
    }

    fun clear() {
        appLog.d(T.API, "ExPlat: clearing cached assignments and active variations")
        activeVariations.clear()
        experimentStore.clearCachedAssignments()
    }

    /**
     * This returns the current active [Variation] for the provided [Experiment].
     *
     * If no active [Variation] is found, we can assume this is the first time this method is being
     * called for the provided [Experiment] during the current session. In this case, the [Variation]
     * is returned from the cached [Assignments] and then set as active. If the cached [Assignments]
     * is stale and [shouldRefreshIfStale] is `true`, then new [Assignments] are fetched and their
     * variations are going to be returned by this method on the next session.
     *
     * If the provided [Experiment] was not included in [ExPlat.start], then [Control] is returned.
     * If [BuildConfig.DEBUG] is `true`, an [IllegalArgumentException] is thrown instead.
     */
    internal fun getVariation(experiment: Experiment, shouldRefreshIfStale: Boolean): Variation {
        if (!experimentNames.contains(experiment.name)) {
            val message = "ExPlat: experiment not found: \"${experiment.name}\"! " +
                    "Make sure to include it in the set provided via constructor."
            appLog.e(T.API, message)
            if (BuildConfig.DEBUG) throw IllegalArgumentException(message) else return Control
        }
        return activeVariations.getOrPut(experiment.name) {
            getAssignments(if (shouldRefreshIfStale) IF_STALE else NEVER).getVariationForExperiment(experiment.name)
        }
    }

    private fun refresh(refreshStrategy: RefreshStrategy) {
        if (experimentNames.isNotEmpty() && (accountStore.hasAccessToken() || analyticsTracker.getAnonID() != null)) {
            getAssignments(refreshStrategy)
        }
    }

    private fun getAssignments(refreshStrategy: RefreshStrategy): Assignments {
        val cachedAssignments = experimentStore.getCachedAssignments() ?: Assignments()
        if (refreshStrategy == ALWAYS || (refreshStrategy == IF_STALE && cachedAssignments.isStale())) {
            coroutineScope.launch { fetchAssignments() }
        }
        return cachedAssignments
    }

    private suspend fun fetchAssignments() =
            experimentStore.fetchAssignments(platform, experimentNames, analyticsTracker.getAnonID()).also {
        if (it.isError) {
            appLog.d(T.API, "ExPlat: fetching assignments failed with result: ${it.error}")
        } else {
            appLog.d(T.API, "ExPlat: fetching assignments successful with result: ${it.assignments}")
        }
    }

    private enum class RefreshStrategy { ALWAYS, IF_STALE, NEVER }
}
