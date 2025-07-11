package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.FLUXC_SCOPE
import org.wordpress.android.fluxc.module.FLUXC_UI_THREAD
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * This is a notifier handler to have a single listener for [rs.wordpress.api.kotlin.WpApiClient]
 * This class will replicate the events to all the listener
 */
@Singleton
class WpAppNotifierHandler @Inject constructor(
    @Named(FLUXC_SCOPE) private val scope: CoroutineScope,
    @Named(FLUXC_UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val wpLoginClient: WpLoginClient,
    private val appLogWrapper: AppLogWrapper,
) {
    private val listeners = mutableMapOf<String, WeakReference<NotifierListener>>()

    fun notifyRequestedWithInvalidAuthentication(site: SiteModel) {
        scope.launch {
            val urlDiscoveryResult = wpLoginClient.apiDiscovery(site.url)
            if (urlDiscoveryResult is ApiDiscoveryResult.Success) {
                listeners.forEach {
                    val listener = it.value.get()
                    withContext(mainDispatcher) {
                        listener?.onRequestedWithInvalidAuthentication(site.url)
                    }
                }
            } else {
                appLogWrapper.e(AppLog.T.API, "Error during API discovery reauthentication for ${site.url}")
            }
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
