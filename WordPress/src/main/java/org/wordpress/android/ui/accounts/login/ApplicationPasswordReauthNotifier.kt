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

    /** Asks any listening UI to navigate to interactive re-auth for [siteUrl]. */
    @Synchronized
    fun notifyReauthRequired(siteUrl: String) {
        cleanupDeadReferences()
        listeners.values.forEach { it.get()?.onReauthRequired(siteUrl) }
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
