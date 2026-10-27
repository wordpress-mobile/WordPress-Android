package org.wordpress.android.ui.accounts.login

import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped relay that asks the UI to start interactive application-password re-authentication for a
 * site. SiteProvisioningSource posts here only after a headless heal (validate + re-mint) has failed
 * for a site that previously had credentials — i.e. the credential is revoked and can't be recovered
 * silently. WPMainActivity / MediaBrowserActivity listen and navigate to the re-auth screen.
 *
 * The raw wordpress-rs 401 signal (WpAppNotifierHandler) now drives the provisioning pipeline's heal
 * instead of the UI directly, so a successful re-mint no longer flashes the re-auth screen. This
 * mirrors that handler's weak-listener shape so the activity add/remove lifecycle is unchanged.
 */
@Singleton
class ApplicationPasswordReauthNotifier @Inject constructor() {
    private val listeners = mutableMapOf<String, WeakReference<Listener>>()

    /**
     * Asks any listening UI to navigate to interactive re-auth for [siteUrl], returning whether a
     * live listener actually took it. Listeners are registered per activity onResume/onPause, so a
     * heal that settles while the app is backgrounded has nobody to tell — the caller needs to know
     * that so it doesn't record the prompt as delivered.
     */
    @Synchronized
    fun notifyReauthRequired(siteUrl: String): Boolean {
        cleanupDeadReferences()
        val live = listeners.values.mapNotNull { it.get() }
        live.forEach { it.onReauthRequired(siteUrl) }
        return live.isNotEmpty()
    }

    @Synchronized
    fun addListener(listener: Listener) {
        listeners[listener.toString()] = WeakReference(listener)
    }

    @Synchronized
    fun removeListener(listener: Listener) {
        listeners.remove(listener.toString())
    }

    private fun cleanupDeadReferences() {
        listeners.entries.removeAll { it.value.get() == null }
    }

    interface Listener {
        fun onReauthRequired(siteUrl: String)
    }
}
