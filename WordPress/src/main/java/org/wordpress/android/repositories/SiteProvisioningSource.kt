package org.wordpress.android.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.SiteApiRestUrlRecoverer
import org.wordpress.android.ui.mysite.cards.applicationpassword.ApplicationPasswordValidator
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The single source of truth for getting a site ready to use: it provisions
 * application-password credentials, recovers the REST API root, and detects
 * editor capabilities — **in that order, each stage awaited before the next**.
 *
 * Those three things have a genuine ordering dependency (you can't probe the
 * REST API of a private Atomic host until you've minted a credential for it),
 * and they used to be spread across the connectivity banner, the editor
 * preloader, and the application-password card, each triggering its slice of
 * the work independently and racing the others. Running them as one serialized
 * per-site pipeline makes the race structurally impossible: [detectCapabilities]
 * is downstream of [ensureAuth], so the probe can never run before the mint.
 *
 * Per site there is at most one in-flight pipeline (single-flight, keyed by
 * [SiteModel.id]); concurrent callers join it rather than starting a second —
 * which also subsumes the application-password card's old single-flight guard
 * (two concurrent mints hit a 409 that destroys the winner's credentials).
 *
 * ## Entry points
 * - [stateFor] — reactive: returns a shared [StateFlow]; the first access runs
 *   the pipeline, later accesses reuse a [SiteReadiness.Ready] result.
 * - [await] — one-shot: runs the pipeline (if needed) and returns the result.
 * - [invalidate] — forces a re-run, bypassing the once-per-site gate
 *   (pull-to-refresh, retry).
 * - [clear] — cancels all work and drops all state; wire into sign-out.
 */
@Singleton
@Suppress("LongParameterList")
class SiteProvisioningSource @Inject constructor(
    private val siteStore: SiteStore,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val applicationPasswordValidator: ApplicationPasswordValidator,
    private val wpApiClientProvider: WpApiClientProvider,
    private val siteApiRestUrlRecoverer: SiteApiRestUrlRecoverer,
    private val editorSettingsRepository: EditorSettingsRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val appLogWrapper: AppLogWrapper,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) {
    private val states = ConcurrentHashMap<Int, MutableStateFlow<SiteReadiness>>()
    private val jobs = ConcurrentHashMap<Int, Job>()

    // Sites whose pipeline reached Ready this process — the dedup gate. Only a fully-ready site
    // latches; auth-needed / unreachable / transient outcomes are left to re-run on the next
    // access (so a later resume re-mints or re-probes). Reset by invalidate / clear.
    private val ready = ConcurrentHashMap.newKeySet<Int>()

    /**
     * The shared readiness state for [site]. The first call starts the pipeline;
     * later calls return the same flow without re-running once it reached
     * [SiteReadiness.Ready]. Collect it to react to provisioning / capability changes.
     */
    @Synchronized
    fun stateFor(site: SiteModel): StateFlow<SiteReadiness> {
        val flow = flowFor(site.id)
        if (shouldRun(site.id)) launchPipeline(site)
        return flow
    }

    /**
     * Runs the pipeline for [site] (if it hasn't reached Ready) and returns the
     * settled readiness. Respects the once-per-site gate; call [invalidate] first
     * to force a fresh run.
     */
    suspend fun await(site: SiteModel): SiteReadiness {
        stateFor(site)
        jobs[site.id]?.join()
        return states[site.id]?.value ?: SiteReadiness.Probing
    }

    /**
     * Forces a re-run for [site], bypassing the once-per-site gate. A no-op while
     * a run is already in flight — that run already reflects current state.
     */
    @Synchronized
    fun invalidate(site: SiteModel) {
        if (jobs[site.id]?.isActive == true) return
        ready.remove(site.id)
        launchPipeline(site)
    }

    /** Cancels all in-flight pipelines and drops all cached state (sign-out). */
    @Synchronized
    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        states.clear()
        ready.clear()
    }

    @Synchronized
    private fun launchPipeline(site: SiteModel) {
        jobs[site.id]?.cancel()
        val flow = flowFor(site.id)
        jobs[site.id] = appScope.launch {
            val readiness = runPipeline(site)
            flow.value = readiness
            if (readiness is SiteReadiness.Ready) ready.add(site.id)
        }
    }

    private fun flowFor(siteLocalId: Int): MutableStateFlow<SiteReadiness> =
        states.getOrPut(siteLocalId) { MutableStateFlow(SiteReadiness.Probing) }

    private fun shouldRun(siteLocalId: Int): Boolean =
        jobs[siteLocalId]?.isActive != true && siteLocalId !in ready

    private suspend fun runPipeline(site: SiteModel): SiteReadiness {
        // Re-read from the store so we provision against the persisted SiteModel and mutate that
        // instance in place — later stages (URL recovery, capability probe) see the fresh creds.
        val storedSite = siteStore.sites.firstOrNull { it.id == site.id } ?: site
        return when (val auth = ensureAuth(storedSite)) {
            SiteAuthState.Provisioned -> {
                recoverRestUrl(storedSite)
                detectCapabilities(storedSite)
            }
            else -> SiteReadiness.NeedsAuth(auth)
        }
    }

    /**
     * Stage 1 — ensure the site has working application-password credentials.
     * Validates stored creds with Basic auth against the direct host; on a
     * confirmed rejection wipes them and mints fresh ones via the FluxC Jetpack
     * tunnel (the only path that works for Atomic / Jetpack-WPCom-REST sites).
     */
    private suspend fun ensureAuth(site: SiteModel): SiteAuthState {
        val hadCredentials = !applicationPasswordLoginHelper.siteHasBadCredentials(site)
        if (hadCredentials) {
            when (applicationPasswordValidator.validate(site)) {
                ApplicationPasswordValidator.Outcome.Valid ->
                    return SiteAuthState.Provisioned
                ApplicationPasswordValidator.Outcome.NetworkUnavailable -> {
                    // Don't punish flaky networks — treat as in-progress and retry next run.
                    appLogWrapper.d(AppLog.T.MAIN, "A_P: Validation network error for ${site.url}")
                    return SiteAuthState.Provisioning
                }
                ApplicationPasswordValidator.Outcome.Invalid -> {
                    // Stored creds are stale (revoked, deleted) — clear them so the mint below
                    // creates fresh ones, and invalidate the cached client.
                    appLogWrapper.d(AppLog.T.MAIN, "A_P: Stored creds invalid for ${site.url}, clearing")
                    siteStore.deleteStoredApplicationPasswordCredentials(site)
                    wpApiClientProvider.clearSelfHostedClient(site.id)
                }
            }
        }
        val createResult = siteStore.createApplicationPassword(site)
        if (!createResult.isError && createResult.credentials != null) {
            wpApiClientProvider.clearSelfHostedClient(site.id)
            appLogWrapper.d(AppLog.T.MAIN, "A_P: Headless mint succeeded for ${site.url}")
            return SiteAuthState.Provisioned
        }
        appLogWrapper.d(
            AppLog.T.MAIN,
            "A_P: Headless mint failed for ${site.url} (notSupported=${createResult.error?.notSupported})"
        )
        return SiteAuthState.Unprovisionable(hadCredentials = hadCredentials)
    }

    /**
     * Stage 2 — recover the REST API root for Atomic sites minted through the
     * Jetpack tunnel, which never runs discovery and so leaves `wpApiRestUrl`
     * null. One owner for the heal that the card and preloader used to duplicate.
     */
    private suspend fun recoverRestUrl(site: SiteModel) {
        if (!site.wpApiRestUrl.isNullOrEmpty()) return
        siteApiRestUrlRecoverer.discoverApiRootUrl(site.url)?.let { apiRootUrl ->
            site.wpApiRestUrl = apiRootUrl
            siteApiRestUrlRecoverer.persistApiRootUrl(site.id, apiRootUrl)
        }
    }

    /**
     * Stage 3 — probe the REST API for editor-capability support and persist it.
     * Reached only once auth is [SiteAuthState.Provisioned], so credentials are
     * guaranteed present: a failure here is a real transport problem, not a
     * pending mint.
     */
    private suspend fun detectCapabilities(site: SiteModel): SiteReadiness {
        val ok = editorSettingsRepository.fetchEditorCapabilitiesForSite(site)
        val hasCache = editorSettingsRepository.hasCachedCapabilities(site)
        return when {
            ok || hasCache -> SiteReadiness.Ready
            !networkUtilsWrapper.isNetworkAvailable() -> SiteReadiness.TransientError
            else -> SiteReadiness.Unreachable
        }
    }
}

/**
 * Whether a site's application password is usable. Owned by [SiteProvisioningSource];
 * rendered by the application-password card.
 */
sealed interface SiteAuthState {
    /** Credentials are usable (validated, or freshly minted). */
    data object Provisioned : SiteAuthState

    /** Not usable yet, but not a terminal failure — a mint is implied / a transient
     *  validation error occurred. The card stays hidden; the next run retries. */
    data object Provisioning : SiteAuthState

    /** Terminal: the mint failed. [hadCredentials] distinguishes a re-authentication
     *  (creds went bad) from a first-time authentication prompt. */
    data class Unprovisionable(val hadCredentials: Boolean) : SiteAuthState
}

/**
 * The combined per-site readiness the [SiteProvisioningSource] exposes. The
 * connectivity banner renders [Unreachable], the application-password card
 * renders [NeedsAuth], and the editor preloader awaits a non-[Probing] value —
 * each a slice of the one state, so they can't disagree.
 */
sealed interface SiteReadiness {
    /** The pipeline is running and hasn't produced a result yet. */
    data object Probing : SiteReadiness

    /** Stopped at the auth stage — credentials aren't usable. Carries the
     *  [SiteAuthState] so the card can pick re-auth vs. first-auth. */
    data class NeedsAuth(val auth: SiteAuthState) : SiteReadiness

    /** Provisioned and editor capabilities are known (detected or cached). */
    data object Ready : SiteReadiness

    /** Provisioned, but the capability probe failed — the site looks unreachable.
     *  The only state that surfaces the connectivity banner. */
    data object Unreachable : SiteReadiness

    /** Provisioned, but a transient failure (e.g. offline). Retried on the next run. */
    data object TransientError : SiteReadiness
}
