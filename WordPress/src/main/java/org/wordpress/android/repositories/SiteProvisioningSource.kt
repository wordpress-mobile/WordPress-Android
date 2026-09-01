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

// How many consecutive heals a site may attempt before we stop honouring its 401s. Two allows one
// retry after a re-mint that didn't take, without letting a self-feeding 401 run unbounded.
private const val MAX_CONSECUTIVE_HEALS = 2

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

    // Heal budget spent per site. The pipeline's own requests can 401 — validation always goes through
    // a notifier-wired client, as does the capability probe on self-hosted sites — so an unbounded
    // heal feeds itself. A heal that
    // confirmed the stored password and replaced nothing can never be the fix — the 401 came from
    // something re-provisioning can't touch — so it spends the whole budget at once; one that
    // re-minted might have worked, so it only counts against it, which bounds a host that mints
    // happily but never accepts the result. Reset when a run reaches Ready without needing a heal,
    // and by invalidate / clear.
    private val healBudgetSpent = ConcurrentHashMap<Int, Int>()

    // What the most recent run established about each site's credentials. The deferred heal reads it
    // to avoid re-running after a run that already re-minted — validation's own 401 fires the notifier
    // during the very heal that is fixing it, which would otherwise schedule a redundant second run.
    private val lastEvidence = ConcurrentHashMap<Int, HealEvidence>()

    // Sites already escalated to interactive re-auth for their current Unprovisionable state. Both
    // escalation paths funnel through escalateReauth, which uses this for idempotency; a run settling
    // anything else clears it, so a later revocation escalates again.
    private val escalated = ConcurrentHashMap.newKeySet<Int>()

    // Sites that earned an escalation while nothing was listening — the heal settled with the app
    // backgrounded. Retried by the next run that settles Unprovisionable, which in practice is the
    // onResume run, by which time an activity has registered its listener.
    private val escalationPending = ConcurrentHashMap.newKeySet<Int>()

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
        // Clear the suppression verdicts first, above the in-flight guard. Only the *relaunch* must not
        // pre-empt a mid-mint run; dropping these sets pre-empts nothing. It has to happen here because
        // MySiteViewModel.refresh builds the application-password card before it reaches this call, and
        // that card's buildCard starts a run synchronously on Main.immediate — so by the time an
        // explicit retry lands, a run is already active for exactly the sites that need clearing.
        healBudgetSpent.remove(site.id)
        escalated.remove(site.id)
        // No-op while running — see KDoc: don't pre-empt an in-flight mint.
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
        reauthOnFailure.clear()
        xmlRpcUnavailable.clear()
        healBudgetSpent.clear()
        lastEvidence.clear()
        escalated.clear()
        escalationPending.clear()
    }

    /**
     * Whether [recoverXmlRpcIfNeeded] concluded that [siteLocalId] genuinely has XML-RPC disabled, as
     * opposed to merely failing to recover the endpoint. Only the definitive case may surface the
     * XML-RPC-disabled card; a transient discovery failure (e.g. a 429 rate-limit) must not.
     */
    fun isXmlRpcUnavailable(siteLocalId: Int): Boolean = siteLocalId in xmlRpcUnavailable

    /**
     * [WpAppNotifierHandler.NotifierListener] — wordpress-rs rejected a request for [site] with invalid
     * authentication — a revoked application password; bearer clients no longer report here. Re-provision it
     * so ensureAuth re-validates and heals: for a WP.com-connected site the headless re-mint succeeds and
     * recovery is silent; for one that can't be re-minted the run settles Unprovisionable and
     * [settleHealState] escalates to interactive re-auth.
     *
     * The notifier hands us the exact [SiteModel] whose client raised the 401, so we heal that one row by
     * id — no resolving back by URL (which isn't unique: the DB constraint is on SITE_ID+URL, so two rows
     * can share a URL). When a run is already in flight, [healForInvalidAuth] defers the heal until it
     * finishes rather than pre-empting a possibly mid-mint stage — so a validate that itself 401s can't
     * spin this into a loop.
     */
    override fun onRequestedWithInvalidAuthentication(site: SiteModel) {
        if (canHeal(site)) healForInvalidAuth(site)
    }

    /**
     * Whether re-provisioning could plausibly fix a 401 for [site]:
     *
     * - WP.com Simple sites are bearer-only — no application password applies, so there is nothing to
     *   heal.
     * - An already-[SiteAuthState.Unprovisionable] site has its re-auth pending the user; re-running
     *   would just re-fail the mint on every 401.
     * - A site that has spent its heal budget ([healBudgetSpent]) has already had attempts that
     *   changed nothing, so its 401s come from something re-provisioning can't fix.
     */
    private fun canHeal(site: SiteModel): Boolean =
        !site.isWPComSimpleSite && hasHealBudget(site.id)

    /**
     * Whether [siteLocalId] may still be healed: its re-auth isn't already pending the user, and it
     * hasn't spent its heal budget on attempts that changed nothing.
     */
    private fun hasHealBudget(siteLocalId: Int): Boolean {
        val auth = (states[siteLocalId]?.value as? SiteReadiness.NeedsAuth)?.auth
        return auth !is SiteAuthState.Unprovisionable &&
            (healBudgetSpent[siteLocalId] ?: 0) < MAX_CONSECUTIVE_HEALS
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
     * [reauthOnFailure], so it never escalated on its own). And a site that has spent its heal budget
     * has already had attempts change nothing, so its 401 is not an application-password problem.
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
            // Decide under the lock, act outside it: escalateReauth reads the DB and starts an
            // Activity, and the main thread takes this same monitor on every stateFor / invalidate.
            val escalation = synchronized(this@SiteProvisioningSource) { resumeDeferredHeal(siteLocalId) }
            escalation?.let { escalateReauth(siteLocalId, it) }
        }
    }

    /**
     * The deferred half of [healForInvalidAuth], run once the in-flight pipeline finishes. Returns the
     * [SiteAuthState.Unprovisionable] to escalate for, or `null` when there's nothing to do — the
     * caller performs the escalation outside the lock.
     */
    private fun resumeDeferredHeal(siteLocalId: Int): SiteAuthState.Unprovisionable? = when {
        // Superseded: a newer run is underway, or the run we waited on itself re-minted. Validation's
        // own 401 goes through a notifier-wired client, so the heal that fixes a revoked password also
        // schedules this deferral — re-running would re-validate the fresh credential for nothing.
        jobs[siteLocalId]?.isActive == true ||
            lastEvidence[siteLocalId] == HealEvidence.Replaced -> null

        // Budget re-checked here, not just at registration time: that run may have spent it. A
        // terminal mint failure escalates instead of relaunching — a routine run's tail skipped that
        // because it never armed reauthOnFailure.
        !hasHealBudget(siteLocalId) ->
            (states[siteLocalId]?.value as? SiteReadiness.NeedsAuth)?.auth as? SiteAuthState.Unprovisionable

        else -> {
            reauthOnFailure.add(siteLocalId)
            ready.remove(siteLocalId)
            launchPipeline(siteLocalId)
            null
        }
    }

    /**
     * Records what a settled run means for future 401s, and escalates to interactive re-auth when a
     * heal couldn't recover a previously-working credential.
     *
     * [wasHeal] is the consumed [reauthOnFailure] flag: only a 401-triggered run may prompt, so a
     * routine run that happens to settle [SiteAuthState.Unprovisionable] leaves the prompt to the
     * application-password card. A heal that settled without changing the credentials is recorded in
     * [healBudgetSpent] — the 401 came from something re-provisioning can't fix.
     */
    private fun settleHealState(siteLocalId: Int, result: PipelineResult, wasHeal: Boolean) {
        lastEvidence[siteLocalId] = result.evidence
        val auth = (result.readiness as? SiteReadiness.NeedsAuth)?.auth
        if (auth is SiteAuthState.Unprovisionable) {
            // A routine run normally leaves the prompt to the card, but it must still deliver an
            // escalation that an earlier heal earned and couldn't hand to anyone.
            if (wasHeal || siteLocalId in escalationPending) escalateReauth(siteLocalId, auth)
            return
        }
        // Not stuck on auth any more, so a future revocation should be able to prompt again.
        escalated.remove(siteLocalId)
        escalationPending.remove(siteLocalId)
        when {
            wasHeal -> {
                // A heal that only re-confirmed the stored password can never be the fix, so it
                // exhausts the budget outright; one that re-minted might have worked, so it costs one.
                val cost = if (result.evidence == HealEvidence.ConfirmedUnchanged) {
                    MAX_CONSECUTIVE_HEALS
                } else {
                    1
                }
                val spent = healBudgetSpent.merge(siteLocalId, cost, Int::plus)
                appLogWrapper.w(
                    AppLog.T.MAIN,
                    "A_P: Heal for $siteLocalId spent $cost (${spent}/$MAX_CONSECUTIVE_HEALS)"
                )
            }
            // A run that reached Ready without needing a heal means whatever was failing has cleared,
            // so a later revocation gets a full budget again.
            result.readiness == SiteReadiness.Ready -> healBudgetSpent.remove(siteLocalId)
        }
    }

    /**
     * Prompts for interactive re-auth at most once per [SiteAuthState.Unprovisionable] episode.
     *
     * The episode is only recorded once a live listener has taken the prompt. Activities register
     * theirs in onResume and drop it in onPause, so a heal that settles while the app is backgrounded
     * would otherwise burn the one prompt on an empty listener map — and nothing would ask again,
     * because further 401s are blocked by the Unprovisionable state. An undelivered prompt is
     * held in [escalationPending] and retried by the next run instead.
     */
    private fun escalateReauth(siteLocalId: Int, auth: SiteAuthState.Unprovisionable) {
        val alreadyHandled = !auth.hadCredentials || siteLocalId in escalated
        val site = if (alreadyHandled) null else siteStore.getSiteByLocalId(siteLocalId)
        if (site == null) return
        if (applicationPasswordReauthNotifier.notifyReauthRequired(site.url)) {
            escalated.add(siteLocalId)
            escalationPending.remove(siteLocalId)
        } else {
            escalationPending.add(siteLocalId)
            appLogWrapper.d(
                AppLog.T.MAIN,
                "A_P: Nobody listening for re-auth on ${site.url} - will retry on the next run"
            )
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
                // Same offline distinction detectCapabilities makes: with no network this isn't the
                // site's fault, and the connectivity banner would just stack on the global
                // no-connection one.
                PipelineResult(unreachableOrTransient(), latch = false)
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

    /** [SiteReadiness.Unreachable] only when the device has a network; otherwise it's just offline. */
    private fun unreachableOrTransient(): SiteReadiness =
        if (networkUtilsWrapper.isNetworkAvailable()) {
            SiteReadiness.Unreachable
        } else {
            SiteReadiness.TransientError
        }

    private fun flowFor(siteLocalId: Int): MutableStateFlow<SiteReadiness> =
        states.getOrPut(siteLocalId) { MutableStateFlow(SiteReadiness.Probing) }

    private fun shouldRun(siteLocalId: Int): Boolean =
        jobs[siteLocalId]?.isActive != true && siteLocalId !in ready

    private suspend fun runPipeline(siteLocalId: Int): PipelineResult {
        return when (val stage = ensureAuth(siteLocalId)) {
            is AuthStage.Proceed -> coroutineScope {
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
                capabilities.await().copy(evidence = stage.evidence)
            }
            // The site itself couldn't be reached, so nothing downstream can run. This is the
            // connectivity banner's case, not the application-password card's — validation never got
            // far enough to say anything about the credentials.
            AuthStage.SiteUnreachable -> PipelineResult(SiteReadiness.Unreachable, latch = false)
            is AuthStage.Stop -> PipelineResult(SiteReadiness.NeedsAuth(stage.auth), latch = false)
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
    private suspend fun ensureAuth(siteLocalId: Int): AuthStage {
        val site = siteStore.getSiteByLocalId(siteLocalId)
            ?: return AuthStage.Stop(SiteAuthState.Unprovisionable(hadCredentials = false))
        // WP.com Simple sites are fully proxied and OAuth-bearer-authed — no application password
        // applies (the mint returns NotSupported). Capability detection works through the proxy, so
        // treat them as ready instead of blocking detection behind a mint that can never run. No
        // password is involved, so there is nothing for a heal to confirm.
        if (site.isWPComSimpleSite) return AuthStage.Proceed(HealEvidence.Inconclusive)
        val hadCredentials = !applicationPasswordLoginHelper.siteHasBadCredentials(site)
        if (hadCredentials) {
            when (applicationPasswordValidator.validate(site)) {
                ApplicationPasswordValidator.Outcome.Valid ->
                    return AuthStage.Proceed(HealEvidence.ConfirmedUnchanged)
                ApplicationPasswordValidator.Outcome.NetworkUnavailable -> {
                    // The validator maps everything ambiguous — DNS, timeout, refused, 5xx — to this
                    // outcome so it never wipes credentials on a guess. That conflates two very
                    // different situations, and only the device-offline one should stay quiet: the
                    // global offline banner already covers it. If the device is online, the *site* is
                    // what we couldn't reach, which is exactly the connectivity banner's case (#22944).
                    if (networkUtilsWrapper.isNetworkAvailable()) {
                        appLogWrapper.d(AppLog.T.MAIN, "A_P: Site unreachable during validation: ${site.url}")
                        return AuthStage.SiteUnreachable
                    }
                    appLogWrapper.d(AppLog.T.MAIN, "A_P: Device offline during validation for ${site.url}")
                    return AuthStage.Stop(SiteAuthState.Provisioning)
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
            return AuthStage.Proceed(HealEvidence.Replaced)
        }
        appLogWrapper.d(
            AppLog.T.MAIN,
            "A_P: Headless mint failed for ${site.url} (notSupported=${createResult.error?.notSupported})"
        )
        return AuthStage.Stop(SiteAuthState.Unprovisionable(hadCredentials = hadCredentials))
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
     * Reached only once [ensureAuth] returned [AuthStage.Proceed].
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
     * re-probes on the next run. [evidence] is what the run established about the credentials, which is
     * how [settleHealState] judges whether a heal accomplished anything. Internal to the pipeline —
     * consumers only ever see [readiness].
     */
    private data class PipelineResult(
        val readiness: SiteReadiness,
        val latch: Boolean,
        val evidence: HealEvidence = HealEvidence.Inconclusive,
    )

    /**
     * What [ensureAuth] settled on. Only [Stop] escapes the pipeline, as a
     * [SiteReadiness.NeedsAuth] the application-password card renders.
     */
    private sealed interface AuthStage {
        /** The credentials are usable — run the rest of the pipeline. */
        data class Proceed(val evidence: HealEvidence) : AuthStage

        /** The site couldn't be reached while the device was online, so validation says nothing about
         *  the credentials. Surfaces as [SiteReadiness.Unreachable] for the connectivity banner. */
        data object SiteUnreachable : AuthStage

        /** Stopped at the auth stage; [auth] is what the card should show. */
        data class Stop(val auth: SiteAuthState) : AuthStage
    }

    /**
     * What a run established about the site's application password — the basis for deciding whether a
     * 401-triggered heal did anything, and so whether repeating it could ever help.
     */
    private enum class HealEvidence {
        /** Nothing was established: the site was unreachable, the device was offline, the mint failed,
         *  or no application password applies to the site at all. */
        Inconclusive,

        /** The stored credentials were confirmed working and left untouched. A heal that ends here
         *  cannot be the fix for the 401 that prompted it. */
        ConfirmedUnchanged,

        /** The credentials were replaced by a fresh mint, so the heal may genuinely have healed. */
        Replaced,
    }
}

/**
 * Why a site's application password isn't usable yet. Owned by [SiteProvisioningSource] and rendered
 * by the application-password card — these are the only two auth outcomes a consumer ever observes.
 * Everything else the auth stage can settle on (usable credentials, an unreachable site) stays inside
 * the pipeline as `AuthStage`, so a `when` over this type has no unreachable branches.
 */
sealed interface SiteAuthState {
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
     *  [SiteAuthState] so the card can pick re-auth vs. first-auth vs. hidden. */
    data class NeedsAuth(val auth: SiteAuthState) : SiteReadiness

    /** Provisioned and editor capabilities are known (detected or cached). */
    data object Ready : SiteReadiness

    /** Provisioned, but the capability probe failed — the site looks unreachable.
     *  The only state that surfaces the connectivity banner. */
    data object Unreachable : SiteReadiness

    /** Provisioned, but a transient failure (e.g. offline). Retried on the next run. */
    data object TransientError : SiteReadiness
}
