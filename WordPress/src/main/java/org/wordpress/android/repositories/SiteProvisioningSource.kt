package org.wordpress.android.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordReauthNotifier
import org.wordpress.android.ui.accounts.login.SiteApiRestUrlRecoverer
import org.wordpress.android.ui.accounts.login.SiteXmlRpcUrlRecoverer
import org.wordpress.android.ui.accounts.login.XmlRpcRecovery
import org.wordpress.android.ui.mysite.cards.applicationpassword.ApplicationPasswordValidator
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

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
 * - [isXmlRpcUnavailable] — whether XML-RPC recovery settled on a definitive negative.
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
    private val wpAppNotifierHandler: WpAppNotifierHandler,
    private val applicationPasswordReauthNotifier: ApplicationPasswordReauthNotifier,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) : WpAppNotifierHandler.NotifierListener {
    private val states = ConcurrentHashMap<Int, MutableStateFlow<SiteReadiness>>()
    private val jobs = ConcurrentHashMap<Int, Job>()

    // Sites whose pipeline reached Ready this process — the dedup gate. Only a fully-ready site
    // latches; auth-needed / unreachable / transient outcomes are left to re-run on the next
    // access. Reset by invalidate / clear.
    private val ready = ConcurrentHashMap.newKeySet<Int>()

    // Sites whose current run was triggered by a 401 (onRequestedWithInvalidAuthentication). If such a
    // run settles Unprovisionable(hadCredentials), the headless heal failed, so we escalate to the
    // interactive re-auth UI. A routine run (onResume) never sets this, so it never pops re-auth.
    private val reauthOnFailure = ConcurrentHashMap.newKeySet<Int>()

    // Sites whose XML-RPC discovery reached a *definitive* negative. A missing xmlRpcUrl alone isn't
    // evidence XML-RPC is off (discovery also fails transiently, e.g. a 429), so only membership here
    // licenses the application-password card to show the XML-RPC-disabled warning.
    private val xmlRpcUnavailable = ConcurrentHashMap.newKeySet<Int>()

    // Sites whose last heal ran to completion without changing the credentials. Their 401s aren't an
    // application-password problem (a WP.com bearer token rejected on the proxy raises the same
    // notification), so re-running ensureAuth can only re-validate the same good password. Without this
    // the pipeline's own detection calls feed their 401 back into healForInvalidAuth and relaunch
    // forever. Cleared by invalidate / clear, so a user-initiated retry re-enables healing.
    private val healFutile = ConcurrentHashMap.newKeySet<Int>()

    // Sites already escalated to interactive re-auth for their current Unprovisionable state. Both
    // escalation paths funnel through escalateReauth, which uses this for idempotency; a run settling
    // anything else clears it, so a later revocation escalates again.
    private val escalated = ConcurrentHashMap.newKeySet<Int>()

    init {
        // Re-provision when wordpress-rs reports a request was rejected for invalid auth (the app
        // password was revoked / rotated server-side). Without this, a site latched Ready keeps its
        // silently-broken credentials until a manual pull-to-refresh; instead force ensureAuth to
        // re-validate, which wipes the dead credential and re-mints (or surfaces the re-auth card).
        wpAppNotifierHandler.addListener(this)
    }

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
     * Forces a re-run for [site] (pull-to-refresh, banner retry), bypassing the once-per-site gate.
     *
     * Deliberately a **no-op while a run is already in flight**: cancelling mid-pipeline could
     * interrupt an in-progress application-password mint after the server created it but before we
     * persisted it, orphaning the credential and tripping a 409 on the next mint. So an explicit
     * refresh that lands during an active run is coalesced into that run (the user gets its live
     * result) rather than pre-empting it; a refresh once the run is idle starts a genuinely fresh one.
     */
    @Synchronized
    fun invalidate(site: SiteModel) {
        // No-op while running — see KDoc: don't pre-empt an in-flight mint.
        if (jobs[site.id]?.isActive == true) return
        ready.remove(site.id)
        // An explicit retry is the user asking us to try everything again, so drop the futile-heal and
        // already-escalated verdicts that would otherwise suppress a 401 heal / re-auth prompt.
        healFutile.remove(site.id)
        escalated.remove(site.id)
        launchPipeline(site.id)
    }

    /** Cancels all in-flight pipelines and drops all cached state (sign-out). */
    @Synchronized
    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        states.clear()
        ready.clear()
        reauthOnFailure.clear()
        xmlRpcUnavailable.clear()
        healFutile.clear()
        escalated.clear()
    }

    /**
     * Whether [recoverXmlRpcIfNeeded] concluded that [siteLocalId] genuinely has XML-RPC disabled, as
     * opposed to merely failing to recover the endpoint. Only the definitive case may surface the
     * XML-RPC-disabled card; a transient discovery failure (e.g. a 429 rate-limit) must not.
     */
    fun isXmlRpcUnavailable(siteLocalId: Int): Boolean = siteLocalId in xmlRpcUnavailable

    /**
     * [WpAppNotifierHandler.NotifierListener] — wordpress-rs rejected a request for [site] with invalid
     * authentication (a revoked application password, or an expired WP.com bearer token). Re-provision it
     * so ensureAuth re-validates and heals: for a WP.com-connected site the headless re-mint succeeds and
     * recovery is silent; for one that can't be re-minted the run settles Unprovisionable and
     * [maybeRequestReauth] escalates to interactive re-auth. WP.com Simple sites are bearer-only (no
     * application password), so they're skipped here.
     *
     * The notifier hands us the exact [SiteModel] whose client raised the 401, so we heal that one row by
     * id — no resolving back by URL (which isn't unique: the DB constraint is on SITE_ID+URL, so two rows
     * can share a URL). Already-Unprovisionable sites are skipped — their re-auth is pending the user, and
     * re-running would just re-fail the mint on every 401. When a run is already in flight,
     * [healForInvalidAuth] defers the heal until it finishes rather than pre-empting a possibly mid-mint
     * stage, and skips it if that run already settled Unprovisionable — so a validate that itself 401s
     * can't spin this into a loop.
     */
    override fun onRequestedWithInvalidAuthentication(site: SiteModel) {
        if (site.isWPComSimpleSite) return
        val auth = (states[site.id]?.value as? SiteReadiness.NeedsAuth)?.auth
        if (auth is SiteAuthState.Unprovisionable) return
        // A previous heal already proved re-provisioning doesn't fix this site's 401s — healing again
        // would just re-validate the same working password and re-raise the same 401.
        if (site.id in healFutile) return
        healForInvalidAuth(site)
    }

    /**
     * Drive a heal for a 401. If the pipeline is idle, run it now and arm [reauthOnFailure] so a failed
     * heal escalates to interactive re-auth. If a run is already in flight we must not pre-empt a
     * possibly mid-mint stage — but the 401 must not be swallowed either: that run may have validated the
     * credential *before* it was revoked and will settle Ready, consuming nothing and healing nothing. So
     * defer a fresh heal until the active run finishes. The flag is armed only when the heal actually
     * launches, so the in-flight run's tail can't consume it for an outcome it never serviced.
     *
     * Two cases end the deferral instead of relaunching. A run that settled
     * [SiteAuthState.Unprovisionable] has already made a terminal mint attempt, so relaunching would
     * only re-fail it — escalate to interactive re-auth instead (a *routine* run never armed
     * [reauthOnFailure], so it never escalated on its own). And a site in [healFutile] has already had
     * a heal change nothing, so its 401 is not an application-password problem.
     */
    @Synchronized
    private fun healForInvalidAuth(site: SiteModel) {
        val siteLocalId = site.id
        val active = jobs[siteLocalId]
        if (active?.isActive != true) {
            reauthOnFailure.add(siteLocalId)
            ready.remove(siteLocalId)
            launchPipeline(siteLocalId)
            return
        }
        active.invokeOnCompletion { cause ->
            if (cause != null) return@invokeOnCompletion // cancelled / relaunched — a fresh run is coming
            synchronized(this@SiteProvisioningSource) {
                if (jobs[siteLocalId]?.isActive == true) return@synchronized // a newer run is already underway
                // Re-checked here, not just at registration time: the run we were waiting on may itself
                // have been the heal that proved healing futile.
                if (siteLocalId in healFutile) return@synchronized
                val settledAuth = (states[siteLocalId]?.value as? SiteReadiness.NeedsAuth)?.auth
                if (settledAuth is SiteAuthState.Unprovisionable) {
                    // Terminal mint failure — a relaunch would just re-fail it. Escalate instead, which
                    // a routine run's tail skipped because it never armed reauthOnFailure.
                    escalateReauth(siteLocalId, settledAuth)
                    return@synchronized
                }
                reauthOnFailure.add(siteLocalId)
                ready.remove(siteLocalId)
                launchPipeline(siteLocalId)
            }
        }
    }

    /**
     * Records what a settled run means for future 401s, and escalates to interactive re-auth when a
     * heal couldn't recover a previously-working credential.
     *
     * [wasHeal] is the consumed [reauthOnFailure] flag: only a 401-triggered run may prompt, so a
     * routine run that happens to settle [SiteAuthState.Unprovisionable] leaves the prompt to the
     * application-password card. A heal that settled without changing the credentials is recorded in
     * [healFutile] — the 401 came from something re-provisioning can't fix.
     */
    private fun settleHealState(siteLocalId: Int, result: PipelineResult, wasHeal: Boolean) {
        val auth = (result.readiness as? SiteReadiness.NeedsAuth)?.auth
        if (auth is SiteAuthState.Unprovisionable) {
            if (wasHeal) escalateReauth(siteLocalId, auth)
            return
        }
        // Not stuck on auth any more, so a future revocation should be able to prompt again.
        escalated.remove(siteLocalId)
        // The heal confirmed the stored password works and replaced nothing, yet a 401 still prompted
        // it — so re-provisioning cannot be the fix. Stop honouring this site's 401s until an explicit
        // retry. A run that never got that far (site unreachable, offline) is inconclusive, not futile.
        if (wasHeal && result.authConfirmed && !result.credentialsChanged) {
            appLogWrapper.w(
                AppLog.T.MAIN,
                "A_P: Heal for $siteLocalId changed no credentials - suppressing further 401 heals"
            )
            healFutile.add(siteLocalId)
        }
    }

    /** Prompts for interactive re-auth at most once per [SiteAuthState.Unprovisionable] episode. */
    private fun escalateReauth(siteLocalId: Int, auth: SiteAuthState.Unprovisionable) {
        if (!auth.hadCredentials) return
        if (!escalated.add(siteLocalId)) return
        siteStore.getSiteByLocalId(siteLocalId)?.let {
            applicationPasswordReauthNotifier.notifyReauthRequired(it.url)
        }
    }

    @Synchronized
    private fun launchPipeline(siteLocalId: Int) {
        jobs[siteLocalId]?.cancel()
        val flow = flowFor(siteLocalId)
        jobs[siteLocalId] = appScope.launch {
            // runPipeline runs on the app-lifetime appScope, a plain (non-supervisor) Job: an escaping
            // throw would cancel it and every other app-scoped coroutine. Contain any unexpected failure
            // (e.g. a SQLiteException from a stage's DB write) as Unreachable so the flow still settles
            // and the next run retries; let cancellation propagate normally.
            val result = try {
                runPipeline(siteLocalId)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "Provisioning pipeline failed for $siteLocalId: ${e::class.simpleName}: ${e.message}"
                )
                PipelineResult(SiteReadiness.Unreachable, latch = false)
            }
            flow.value = result.readiness
            // Latch the dedup gate only on a freshly live-probed Ready; a Ready served from stale cache
            // (latch = false) is left to re-probe on the next run instead of sticking for the process.
            if (result.latch) ready.add(siteLocalId)
            // settleHealState reads the DB and drives the reauth notifier, and runs after the flow has
            // already settled. Contain its throws too: on this non-supervisor appScope an escaping throw
            // here would cancel the scope and wedge provisioning for every other site. It must run
            // before this job completes so the deferred heal handler sees the verdicts it records.
            try {
                settleHealState(siteLocalId, result, wasHeal = reauthOnFailure.remove(siteLocalId))
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "Reauth escalation failed for $siteLocalId: ${e::class.simpleName}: ${e.message}"
                )
            }
        }
    }

    private fun flowFor(siteLocalId: Int): MutableStateFlow<SiteReadiness> =
        states.getOrPut(siteLocalId) { MutableStateFlow(SiteReadiness.Probing) }

    private fun shouldRun(siteLocalId: Int): Boolean =
        jobs[siteLocalId]?.isActive != true && siteLocalId !in ready

    private suspend fun runPipeline(siteLocalId: Int): PipelineResult {
        val (auth, credentialsChanged) = ensureAuth(siteLocalId)
        return when (auth) {
            SiteAuthState.Provisioned, SiteAuthState.NotApplicable -> coroutineScope {
                // Post-auth, the REST-capability chain and the XML-RPC recovery are independent — each
                // reads the site fresh and writes only its own column — so run them in parallel.
                // recoverRestUrlIfNeeded precedes detectCapabilities within its branch because the probe
                // needs the recovered REST root. Both branches read the credentials fresh from the store:
                // the mint persists them via a single writer that the generic full-row update can no
                // longer clobber (#22947), so the re-read is now trustworthy (#22905).
                val capabilities = async {
                    recoverRestUrlIfNeeded(siteLocalId)
                    detectCapabilities(siteLocalId)
                }
                val xmlRpc = async { recoverXmlRpcIfNeeded(siteLocalId) }
                xmlRpc.await()
                capabilities.await().copy(
                    credentialsChanged = credentialsChanged,
                    authConfirmed = true,
                )
            }
            // The site itself couldn't be reached, so nothing downstream can run. This is the
            // connectivity banner's case, not the application-password card's — validation never got
            // far enough to say anything about the credentials.
            SiteAuthState.SiteUnreachable ->
                PipelineResult(SiteReadiness.Unreachable, latch = false, credentialsChanged = false)
            else -> PipelineResult(
                SiteReadiness.NeedsAuth(auth),
                latch = false,
                credentialsChanged = credentialsChanged,
            )
        }
    }

    /**
     * Stage 1 — ensure the site has working application-password credentials. Validates stored creds
     * with Basic auth against the direct host; on a confirmed rejection wipes them and mints fresh
     * ones via the FluxC Jetpack tunnel. The mint persists the credentials (single-writer, #22947), so
     * the downstream stages read them back from a fresh [SiteModel] rather than having them threaded.
     */
    // Each return is a distinct auth outcome (missing site, valid, unreachable, transient, minted,
    // failed); collapsing to one return would thread a result through nested branches and read worse.
    @Suppress("ReturnCount")
    private suspend fun ensureAuth(siteLocalId: Int): AuthOutcome {
        val site = siteStore.getSiteByLocalId(siteLocalId)
            ?: return AuthOutcome(SiteAuthState.Unprovisionable(hadCredentials = false))
        // WP.com Simple sites are fully proxied and OAuth-bearer-authed — no application password
        // applies (the mint returns NotSupported). Capability detection works through the proxy, so
        // treat them as ready instead of blocking detection behind a mint that can never run.
        if (site.isWPComSimpleSite) return AuthOutcome(SiteAuthState.NotApplicable)
        val hadCredentials = !applicationPasswordLoginHelper.siteHasBadCredentials(site)
        if (hadCredentials) {
            when (applicationPasswordValidator.validate(site)) {
                // Credentials confirmed working, and untouched — nothing was re-minted.
                ApplicationPasswordValidator.Outcome.Valid ->
                    return AuthOutcome(SiteAuthState.Provisioned)
                ApplicationPasswordValidator.Outcome.NetworkUnavailable -> {
                    // The validator maps everything ambiguous — DNS, timeout, refused, 5xx — to this
                    // outcome so it never wipes credentials on a guess. That conflates two very
                    // different situations, and only the device-offline one should stay quiet: the
                    // global offline banner already covers it. If the device is online, the *site* is
                    // what we couldn't reach, which is exactly the connectivity banner's case (#22944).
                    if (networkUtilsWrapper.isNetworkAvailable()) {
                        appLogWrapper.d(AppLog.T.MAIN, "A_P: Site unreachable during validation: ${site.url}")
                        return AuthOutcome(SiteAuthState.SiteUnreachable)
                    }
                    appLogWrapper.d(AppLog.T.MAIN, "A_P: Device offline during validation for ${site.url}")
                    return AuthOutcome(SiteAuthState.Provisioning)
                }
                ApplicationPasswordValidator.Outcome.Invalid -> {
                    appLogWrapper.d(AppLog.T.MAIN, "A_P: Stored creds invalid for ${site.url}, clearing")
                    siteStore.deleteStoredApplicationPasswordCredentials(site)
                    wpApiClientProvider.clearSelfHostedClient(site.id)
                }
            }
        }
        // createApplicationPassword mints and persists the credentials; downstream stages read them
        // back from a fresh SiteModel.
        val createResult = siteStore.createApplicationPassword(site)
        if (!createResult.isError && createResult.credentials != null) {
            wpApiClientProvider.clearSelfHostedClient(site.id)
            appLogWrapper.d(AppLog.T.MAIN, "A_P: Headless mint succeeded for ${site.url}")
            // A fresh mint replaced the credentials, so a 401 that prompted this run may genuinely be
            // healed by it — this is the one path that marks the heal as having done something.
            return AuthOutcome(SiteAuthState.Provisioned, credentialsChanged = true)
        }
        appLogWrapper.d(
            AppLog.T.MAIN,
            "A_P: Headless mint failed for ${site.url} (notSupported=${createResult.error?.notSupported})"
        )
        return AuthOutcome(SiteAuthState.Unprovisionable(hadCredentials = hadCredentials))
    }

    /**
     * Stage 2a — recover the REST API root for Atomic sites minted through the
     * Jetpack tunnel (which never runs discovery and leaves `wpApiRestUrl` null).
     * Persists the one column; the capability probe (sequenced after it) re-reads it.
     */
    private suspend fun recoverRestUrlIfNeeded(siteLocalId: Int) {
        val site = siteStore.getSiteByLocalId(siteLocalId) ?: return
        // WP.com Simple sites are proxy-served — no direct REST host to recover (their wpApiRestUrl
        // is legitimately null), so don't burn a discovery call on them.
        if (site.isWPComSimpleSite || !site.wpApiRestUrl.isNullOrEmpty()) return
        siteApiRestUrlRecoverer.discoverApiRootUrl(site.url)?.let { apiRootUrl ->
            siteApiRestUrlRecoverer.persistApiRootUrl(siteLocalId, apiRootUrl)
        }
    }

    /**
     * Stage 2b (parallel) — recover the XML-RPC endpoint for true self-hosted
     * sites that don't have one. Discovers + authenticates against it with the
     * site's application-password credentials (which work for XML-RPC just as for
     * REST), and on success persists the one column; the application-password card
     * re-reads it.
     */
    private suspend fun recoverXmlRpcIfNeeded(siteLocalId: Int) {
        val site = siteStore.getSiteByLocalId(siteLocalId) ?: return
        // WP.com / Atomic / Jetpack-WPCom-REST sites talk REST end-to-end and don't use XML-RPC.
        if (site.isUsingWpComRestApi || !site.xmlRpcUrl.isNullOrEmpty()) return
        when (val recovery = siteXmlRpcUrlRecoverer.discoverAndVerifyXmlRpcUrl(site)) {
            is XmlRpcRecovery.Recovered -> {
                xmlRpcUnavailable.remove(siteLocalId)
                siteXmlRpcUrlRecoverer.persistXmlRpcUrl(siteLocalId, recovery.endpoint)
            }
            // Definitive negative — license the card. Inconclusive settles nothing, so clear any stale
            // verdict and leave the card hidden until a later run reaches a conclusion.
            XmlRpcRecovery.Unavailable -> xmlRpcUnavailable.add(siteLocalId)
            XmlRpcRecovery.Inconclusive -> xmlRpcUnavailable.remove(siteLocalId)
        }
    }

    /**
     * Stage 3 — probe the REST API for editor-capability support and persist it.
     * Reached only once auth is [SiteAuthState.Provisioned] / [SiteAuthState.NotApplicable].
     * Reads the site fresh: the mint has already persisted the credentials (single-writer, #22947),
     * so a probe failure here is a real transport problem, not a pending mint.
     */
    private suspend fun detectCapabilities(siteLocalId: Int): PipelineResult {
        val site = siteStore.getSiteByLocalId(siteLocalId)
            ?: return PipelineResult(SiteReadiness.Unreachable, latch = false)
        val ok = editorSettingsRepository.fetchEditorCapabilitiesForSite(site)
        val hasCache = editorSettingsRepository.hasCachedCapabilities(site)
        return when {
            // Live probe succeeded: latch so we stop re-probing — capabilities rarely change.
            ok -> PipelineResult(SiteReadiness.Ready, latch = true)
            // Probe failed but stale cache keeps the site usable and the banner hidden; do NOT latch,
            // so the next run re-probes and can refresh the cache / surface a genuine failure (#22944).
            hasCache -> PipelineResult(SiteReadiness.Ready, latch = false)
            !networkUtilsWrapper.isNetworkAvailable() ->
                PipelineResult(SiteReadiness.TransientError, latch = false)
            else -> PipelineResult(SiteReadiness.Unreachable, latch = false)
        }
    }

    /**
     * A settled pipeline result plus whether it should latch the per-site dedup gate ([ready]). Only a
     * freshly live-probed [SiteReadiness.Ready] latches; a Ready served from stale cache does not, so it
     * re-probes on the next run. [credentialsChanged] is true only when this run actually re-minted the
     * application password, which is how a heal proves it did something; [authConfirmed] is true when
     * the run got far enough to establish the credentials are usable, which is what makes an unchanged
     * heal conclusively futile rather than merely inconclusive. Internal to the pipeline — consumers
     * only ever see [readiness].
     */
    private data class PipelineResult(
        val readiness: SiteReadiness,
        val latch: Boolean,
        val credentialsChanged: Boolean = false,
        val authConfirmed: Boolean = false,
    )

    /** What [ensureAuth] settled on, plus whether it replaced the stored credentials. */
    private data class AuthOutcome(
        val state: SiteAuthState,
        val credentialsChanged: Boolean = false,
    )
}

/**
 * Whether a site's application password is usable. Owned by [SiteProvisioningSource];
 * rendered by the application-password card.
 */
sealed interface SiteAuthState {
    /** Credentials are usable (validated, or freshly minted). */
    data object Provisioned : SiteAuthState

    /** No application password applies — a WP.com Simple site, which is proxy-served and
     *  OAuth-bearer-authed. Treated like [Provisioned]: capability detection runs via the proxy. */
    data object NotApplicable : SiteAuthState

    /** Not usable yet, but not a terminal failure — a mint is implied / a transient
     *  validation error occurred. The card stays hidden; the next run retries. */
    data object Provisioning : SiteAuthState

    /** The site couldn't be reached at all while the device was online, so validation says nothing
     *  about the credentials. Mapped to [SiteReadiness.Unreachable] rather than wrapped in
     *  [SiteReadiness.NeedsAuth]: it's the connectivity banner's case, not the card's. */
    data object SiteUnreachable : SiteAuthState

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
