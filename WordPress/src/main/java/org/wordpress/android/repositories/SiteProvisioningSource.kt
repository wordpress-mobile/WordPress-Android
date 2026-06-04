package org.wordpress.android.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import org.wordpress.android.ui.accounts.login.SiteXmlRpcUrlRecoverer
import org.wordpress.android.ui.mysite.cards.applicationpassword.ApplicationPasswordValidator
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The single source of truth for getting a site ready to use: it provisions
 * application-password credentials, recovers the REST API root, recovers the
 * XML-RPC endpoint (self-hosted), and detects editor capabilities.
 *
 * Capability detection can't run until a credential exists (you can't probe a
 * private Atomic host's REST API unauthenticated), so **auth is awaited first**;
 * after that, the REST-capability branch and the XML-RPC branch are independent
 * and run **in parallel**. Routing every consumer (connectivity banner, editor
 * preloader, application-password card) through this one pipeline means the
 * first-login race is structurally impossible — the probe is downstream of the
 * mint — and there's one shared, deduplicated run per site instead of each
 * consumer racing the others.
 *
 * ### No model is held across stages
 * Stages take a **`siteLocalId`**, read the `SiteModel` fresh from the store at
 * the point of use, and write back **only the one column they changed**
 * (`persistApiRootUrl` / `persistXmlRpcUrl`). Nothing keeps a mutated `SiteModel`
 * around, so the two parallel branches can't clobber each other and there's no
 * stale-model write (see #22905). The passed `SiteModel` is used for its id only.
 *
 * Per site there is at most one in-flight pipeline (single-flight, keyed by
 * [SiteModel.id]); concurrent callers join it — which also subsumes the
 * application-password card's old single-flight guard (two concurrent mints hit
 * a 409 that destroys the winner's credentials).
 *
 * ## Entry points
 * - [stateFor] — reactive: returns a shared [StateFlow]; first access runs the
 *   pipeline, later accesses reuse a [SiteReadiness.Ready] result.
 * - [await] — one-shot: runs the pipeline (if needed) and returns the result.
 * - [invalidate] — forces a re-run (pull-to-refresh, retry).
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
    private val siteXmlRpcUrlRecoverer: SiteXmlRpcUrlRecoverer,
    private val editorSettingsRepository: EditorSettingsRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val appLogWrapper: AppLogWrapper,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) {
    private val states = ConcurrentHashMap<Int, MutableStateFlow<SiteReadiness>>()
    private val jobs = ConcurrentHashMap<Int, Job>()

    // Sites whose pipeline reached Ready this process — the dedup gate. Only a fully-ready site
    // latches; auth-needed / unreachable / transient outcomes are left to re-run on the next
    // access. Reset by invalidate / clear.
    private val ready = ConcurrentHashMap.newKeySet<Int>()

    /**
     * The shared readiness state for [site]. The first call starts the pipeline;
     * later calls return the same flow without re-running once it reached
     * [SiteReadiness.Ready]. Only [SiteModel.id] is read from [site].
     */
    @Synchronized
    fun stateFor(site: SiteModel): StateFlow<SiteReadiness> {
        val flow = flowFor(site.id)
        if (shouldRun(site.id)) launchPipeline(site.id)
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
        launchPipeline(site.id)
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
    private fun launchPipeline(siteLocalId: Int) {
        jobs[siteLocalId]?.cancel()
        val flow = flowFor(siteLocalId)
        jobs[siteLocalId] = appScope.launch {
            val readiness = runPipeline(siteLocalId)
            flow.value = readiness
            if (readiness is SiteReadiness.Ready) ready.add(siteLocalId)
        }
    }

    private fun flowFor(siteLocalId: Int): MutableStateFlow<SiteReadiness> =
        states.getOrPut(siteLocalId) { MutableStateFlow(SiteReadiness.Probing) }

    private fun shouldRun(siteLocalId: Int): Boolean =
        jobs[siteLocalId]?.isActive != true && siteLocalId !in ready

    private suspend fun runPipeline(siteLocalId: Int): SiteReadiness =
        when (val auth = ensureAuth(siteLocalId)) {
            SiteAuthState.Provisioned -> coroutineScope {
                // Post-auth, the REST-capability chain and the XML-RPC recovery are independent —
                // each reads the site fresh and writes only its own column — so run them in
                // parallel. recoverRestUrl precedes detectCapabilities within its branch because
                // the probe needs the recovered REST root.
                val capabilities = async { recoverRestUrl(siteLocalId); detectCapabilities(siteLocalId) }
                val xmlRpc = async { recoverXmlRpc(siteLocalId) }
                xmlRpc.await()
                capabilities.await()
            }
            else -> SiteReadiness.NeedsAuth(auth)
        }

    /**
     * Stage 1 — ensure the site has working application-password credentials.
     * Validates stored creds with Basic auth against the direct host; on a
     * confirmed rejection wipes them and mints fresh ones via the FluxC Jetpack
     * tunnel. The mint persists the credentials, so later stages read them back.
     */
    // Each return is a distinct auth outcome (missing site, valid, transient, minted, failed);
    // collapsing to one return would thread a result through nested branches and read worse.
    @Suppress("ReturnCount")
    private suspend fun ensureAuth(siteLocalId: Int): SiteAuthState {
        val site = siteStore.getSiteByLocalId(siteLocalId)
            ?: return SiteAuthState.Unprovisionable(hadCredentials = false)
        val hadCredentials = !applicationPasswordLoginHelper.siteHasBadCredentials(site)
        if (hadCredentials) {
            when (applicationPasswordValidator.validate(site)) {
                ApplicationPasswordValidator.Outcome.Valid ->
                    return SiteAuthState.Provisioned
                ApplicationPasswordValidator.Outcome.NetworkUnavailable -> {
                    appLogWrapper.d(AppLog.T.MAIN, "A_P: Validation network error for ${site.url}")
                    return SiteAuthState.Provisioning
                }
                ApplicationPasswordValidator.Outcome.Invalid -> {
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
     * Stage 2a — recover the REST API root for Atomic sites minted through the
     * Jetpack tunnel (which never runs discovery and leaves `wpApiRestUrl` null).
     * Persists the one column; the capability probe re-reads it.
     */
    private suspend fun recoverRestUrl(siteLocalId: Int) {
        val site = siteStore.getSiteByLocalId(siteLocalId) ?: return
        if (!site.wpApiRestUrl.isNullOrEmpty()) return
        siteApiRestUrlRecoverer.discoverApiRootUrl(site.url)?.let { apiRootUrl ->
            siteApiRestUrlRecoverer.persistApiRootUrl(siteLocalId, apiRootUrl)
        }
    }

    /**
     * Stage 2b (parallel) — recover the XML-RPC endpoint for true self-hosted
     * sites that don't have one. Discovers + authenticates against it, and on
     * success persists the one column; the application-password card re-reads it.
     */
    private suspend fun recoverXmlRpc(siteLocalId: Int) {
        val site = siteStore.getSiteByLocalId(siteLocalId) ?: return
        // WP.com / Atomic / Jetpack-WPCom-REST sites talk REST end-to-end and don't use XML-RPC.
        if (site.isUsingWpComRestApi || !site.xmlRpcUrl.isNullOrEmpty()) return
        siteXmlRpcUrlRecoverer.discoverAndVerifyXmlRpcUrl(site)?.let { endpoint ->
            siteXmlRpcUrlRecoverer.persistXmlRpcUrl(siteLocalId, endpoint)
        }
    }

    /**
     * Stage 3 — probe the REST API for editor-capability support and persist it.
     * Reached only once auth is [SiteAuthState.Provisioned], so credentials are
     * guaranteed present: a failure here is a real transport problem, not a
     * pending mint.
     */
    private suspend fun detectCapabilities(siteLocalId: Int): SiteReadiness {
        val site = siteStore.getSiteByLocalId(siteLocalId) ?: return SiteReadiness.Unreachable
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
