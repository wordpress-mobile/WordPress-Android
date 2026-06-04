package org.wordpress.android.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Single owner of "what does this site's editor REST API support" as an
 * observable, per-site state.
 *
 * Capability detection ([EditorSettingsRepository.fetchEditorCapabilitiesForSite])
 * has two async preconditions on Atomic sites — an application password and a
 * recovered REST root — provisioned elsewhere on the My Site screen. Routing
 * every consumer (connectivity banner, editor preloader) through this detector
 * means one probe per site, shared and deduplicated, instead of each consumer
 * re-deriving the same state and racing the same preconditions.
 *
 * State is keyed by [SiteModel.id] — the local DB row id, stable across the
 * process lifetime — mirroring `GutenbergEditorPreloader`.
 *
 * ## Entry points
 * - [stateFor] — the reactive entry point. Returns a shared [StateFlow]; the
 *   first access starts detection, later accesses reuse the cached result
 *   (capabilities rarely change). A failed probe is retried on the next access.
 * - [awaitProbe] — the one-shot entry point for callers that just need the
 *   probe to have run (and its capabilities persisted) before continuing.
 * - [refresh] — forces a re-probe, bypassing the once-per-site gate
 *   (pull-to-refresh, banner retry, newly established credentials).
 * - [clear] — cancels all work and drops all state; wire into sign-out.
 */
@Singleton
class EditorCapabilityDetector @Inject constructor(
    private val editorSettingsRepository: EditorSettingsRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) {
    private val states =
        ConcurrentHashMap<Int, MutableStateFlow<EditorCapabilityDetectionState>>()
    private val jobs = ConcurrentHashMap<Int, Job>()

    // Sites whose live probe succeeded this process — the dedup gate. Only a
    // successful fetch latches; a failed one is left to retry on the next
    // access, matching the connectivity banner's previous per-slice behaviour.
    // Reset by refresh / clear.
    private val probedOk = ConcurrentHashMap.newKeySet<Int>()

    /**
     * The shared detection state for [site]. The first call starts detection;
     * later calls return the same flow without re-probing once it has
     * succeeded. Collect it to react to capability changes.
     */
    @Synchronized
    fun stateFor(site: SiteModel): StateFlow<EditorCapabilityDetectionState> {
        val flow = flowFor(site.id)
        if (shouldProbe(site.id)) launchDetection(site)
        return flow
    }

    /**
     * Ensures detection has run for [site] (so its capabilities are persisted)
     * and returns the settled state. Respects the once-per-site gate; call
     * [refresh] first to force a fresh probe.
     */
    suspend fun awaitProbe(site: SiteModel): EditorCapabilityDetectionState {
        stateFor(site)
        jobs[site.id]?.join()
        return states[site.id]?.value ?: EditorCapabilityDetectionState.Pending
    }

    /**
     * Forces a re-probe for [site], bypassing the once-per-site gate. A no-op
     * while a probe is already in flight — that probe's result is fresh enough.
     */
    @Synchronized
    fun refresh(site: SiteModel) {
        if (jobs[site.id]?.isActive == true) return
        probedOk.remove(site.id)
        launchDetection(site)
    }

    /** Cancels all in-flight detection and drops all cached state (sign-out). */
    @Synchronized
    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        states.clear()
        probedOk.clear()
    }

    @Synchronized
    private fun launchDetection(site: SiteModel) {
        jobs[site.id]?.cancel()
        val flow = flowFor(site.id)
        jobs[site.id] = appScope.launch {
            flow.value = detect(site)
        }
    }

    private fun flowFor(siteLocalId: Int): MutableStateFlow<EditorCapabilityDetectionState> =
        states.getOrPut(siteLocalId) {
            MutableStateFlow(EditorCapabilityDetectionState.Pending)
        }

    private fun shouldProbe(siteLocalId: Int): Boolean =
        jobs[siteLocalId]?.isActive != true && siteLocalId !in probedOk

    private suspend fun detect(site: SiteModel): EditorCapabilityDetectionState {
        val ok = editorSettingsRepository.fetchEditorCapabilitiesForSite(site)
        if (ok) probedOk.add(site.id)
        val hasCache = editorSettingsRepository.hasCachedCapabilities(site)
        return when {
            ok || hasCache -> EditorCapabilityDetectionState.Ready
            editorSettingsRepository.isAwaitingApplicationPassword(site) ->
                EditorCapabilityDetectionState.Pending
            !networkUtilsWrapper.isNetworkAvailable() ->
                EditorCapabilityDetectionState.TransientError
            else -> EditorCapabilityDetectionState.Unreachable
        }
    }
}

/**
 * Observable lifecycle of editor-capability detection for one site — distinct
 * from `org.wordpress.android.ui.posts.EditorCapabilityState`, which models a
 * resolved settings-row capability. This is the *detection* state the
 * connectivity banner and editor preloader subscribe to.
 */
sealed interface EditorCapabilityDetectionState {
    /**
     * Not determined yet — still probing, or waiting on an application password
     * being minted asynchronously. Consumers hold; the banner stays hidden.
     */
    data object Pending : EditorCapabilityDetectionState

    /**
     * Capabilities are known (freshly detected, or cached from a prior run).
     * Read them via [EditorSettingsRepository]'s getters.
     */
    data object Ready : EditorCapabilityDetectionState

    /**
     * Credentials are present but the transport probe failed — the site looks
     * unreachable. The only state that surfaces the connectivity banner.
     */
    data object Unreachable : EditorCapabilityDetectionState

    /** A transient failure (e.g. device offline). Retried on the next probe. */
    data object TransientError : EditorCapabilityDetectionState
}
