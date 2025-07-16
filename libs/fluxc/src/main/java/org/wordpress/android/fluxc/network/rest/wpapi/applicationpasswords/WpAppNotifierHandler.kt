package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This is a notifier handler to have a single listener for [rs.wordpress.api.kotlin.WpApiClient]
 * This class will replicate the events to all the listener
 */
@Singleton
class WpAppNotifierHandler @Inject constructor() {
    private val listeners = mutableMapOf<String, WeakReference<NotifierListener>>()

    @Synchronized
    fun notifyRequestedWithInvalidAuthentication(site: SiteModel) {
        cleanupDeadReferences()
        listeners.forEach {
            val listener = it.value.get()
            listener?.onRequestedWithInvalidAuthentication(site.url)
        }
    }

    @Synchronized
    fun addListener(listener: NotifierListener) {
        listeners[listener.toString()] = WeakReference(listener)
    }

    @Synchronized
    fun removeListener(listener: NotifierListener) {
        listeners.remove(listener.toString())
    }

    private fun cleanupDeadReferences() {
        listeners.entries.removeAll { it.value.get() == null }
    }

    interface NotifierListener {
        fun onRequestedWithInvalidAuthentication(authenticationUrl: String)
    }
}
