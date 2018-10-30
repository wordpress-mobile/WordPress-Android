package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.VerticalAction
import org.wordpress.android.fluxc.action.VerticalAction.FETCH_VERTICALS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.network.rest.wpcom.vertical.VerticalRestClient
import org.wordpress.android.fluxc.persistence.VerticalSqlUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerticalStore @Inject constructor(
    private val verticalSqlUtils: VerticalSqlUtils,
    private val verticalRestClient: VerticalRestClient,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? VerticalAction ?: return

        when (actionType) {
            FETCH_VERTICALS -> TODO()
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }
}
