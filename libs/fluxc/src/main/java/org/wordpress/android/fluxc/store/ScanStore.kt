package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.scan.ScanRestClient
import org.wordpress.android.fluxc.persistence.ScanSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanStore @Inject constructor(
    private val scanRestClient: ScanRestClient,
    private val scanSqlUtils: ScanSqlUtils,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    // Payloads
    class FetchScanStatePayload(
        val site: SiteModel
    ) : Payload<BaseNetworkError>()

    class FetchedScanStatePayload(
        val scanStateModel: ScanStateModel? = null,
        val site: SiteModel
    ) : Payload<ScanStateError>() {
        constructor(
            error: ScanStateError,
            site: SiteModel
        ) : this(site = site) {
            this.error = error
        }
    }

    // Errors
    enum class ScanStateErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class ScanStateError(var type: ScanStateErrorType, var message: String? = null) : OnChangedError
}
