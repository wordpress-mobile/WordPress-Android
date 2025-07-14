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

    fun notifyRequestedWithInvalidAuthentication(site: SiteModel) {
        listeners.forEach {
            val listener = it.value.get()
            listener?.onRequestedWithInvalidAuthentication(site.url)
        }
    }

    fun addListener(listener: NotifierListener) {
        listeners.put(listener.toString(), WeakReference(listener))
    }

    fun removeListener(listener: NotifierListener) {
        listeners.remove(listener.toString())
    }

    interface NotifierListener {
        fun onRequestedWithInvalidAuthentication(authenticationUrl: String)
    }
}
