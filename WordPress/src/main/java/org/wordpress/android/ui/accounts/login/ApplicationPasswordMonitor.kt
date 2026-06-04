package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped monitor of application-password provisioning for sites. It does two things:
 *
 * - Emits on [events] whenever a site's provisioning state settles — credentials established (headless
 *   mint or interactive login) or a mint that failed terminally — so credential-dependent work (e.g.
 *   editor capability detection) can re-evaluate immediately instead of waiting for the next My Site
 *   resume/refresh. Emits the site's local id.
 * - Tracks via [hasMintFailed] whether the most recent mint for a site failed terminally, so callers
 *   can tell "provisioning in progress" (transient — stay quiet) apart from "provisioning failed"
 *   (surface the problem rather than suppressing it).
 *
 * Collectors should re-read a fresh SiteModel so they observe just-persisted credentials.
 */
@Singleton
class ApplicationPasswordMonitor @Inject constructor() {
    // replay = 1 so a collector that subscribes just after an emit still sees it — closes the
    // emit-before-collect race. DROP_OLDEST keeps tryEmit non-suspending without an unbounded buffer.
    private val _events = MutableSharedFlow<Int>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Int> = _events.asSharedFlow()

    private val mintFailedSiteIds = ConcurrentHashMap.newKeySet<Int>()

    /** A site's application-password credentials were just established; clears any prior failure. */
    fun onCredentialsEstablished(siteLocalId: Int) {
        mintFailedSiteIds.remove(siteLocalId)
        _events.tryEmit(siteLocalId)
    }

    /** A site's application-password mint failed terminally — no credentials were obtained. */
    fun onMintFailed(siteLocalId: Int) {
        mintFailedSiteIds.add(siteLocalId)
        _events.tryEmit(siteLocalId)
    }

    /** True when the most recent mint for [siteLocalId] failed terminally. */
    fun hasMintFailed(siteLocalId: Int): Boolean = mintFailedSiteIds.contains(siteLocalId)
}
