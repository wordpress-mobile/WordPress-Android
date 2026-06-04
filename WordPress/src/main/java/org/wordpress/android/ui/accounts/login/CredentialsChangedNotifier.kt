package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped signal that an application password was newly established for a site — either by the
 * headless Jetpack-tunnel mint on the My Site screen or by the interactive application-password
 * login. Lets credential-dependent work (e.g. editor capability detection) re-run as soon as the
 * password exists, instead of waiting for the next My Site resume/refresh.
 *
 * Emits the site's local id; collectors should re-read a fresh SiteModel so they observe the
 * just-persisted credentials rather than a stale in-memory copy.
 */
@Singleton
class CredentialsChangedNotifier @Inject constructor() {
    // replay = 1 so a collector that subscribes just after an emit still sees it — closes the
    // emit-before-collect race. DROP_OLDEST keeps tryEmit non-suspending without an unbounded buffer.
    private val _events = MutableSharedFlow<Int>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Int> = _events.asSharedFlow()

    /** Signals that [siteLocalId]'s application-password credentials were just established. */
    fun notifyChanged(siteLocalId: Int) {
        _events.tryEmit(siteLocalId)
    }
}
