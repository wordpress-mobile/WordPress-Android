package org.wordpress.android.ui.accounts.login

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpapi.WPcomLoginClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class WPcomLoginHelper @Inject constructor(
    private val loginClient: WPcomLoginClient,
    private val accountStore: AccountStore,
    appSecrets: AppSecrets,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val wpcomLoginUri = loginClient.loginUri(appSecrets.redirectUri)
    private val customTabsServiceConnection = ServiceConnection(wpcomLoginUri)

    /**
     * Returns true if the data string contains an OAuth callback code.
     */
    fun hasOAuthCallback(data: String?): Boolean {
        if (data == null) return false
        return data.toUri().getQueryParameter("code") != null
    }

    /**
     * Asynchronously exchanges the OAuth code in the data string for
     * an access token. Calls [onSuccess] on the main thread if the
     * exchange succeeds, or [onFailure] with the exception if it
     * fails.
     */
    fun tryLoginWithDataString(
        data: String,
        onSuccess: Runnable,
        onFailure: java.util.function.Consumer<Exception>
    ) {
        val code = data.toUri().getQueryParameter("code")
        if (code == null) {
            onFailure.accept(
                IllegalArgumentException("Missing OAuth code in callback")
            )
            return
        }

        scope.launch {
            loginClient.exchangeAuthCodeForToken(code).fold(
                onSuccess = { token ->
                    accountStore.updateAccessToken(token)
                    withContext(Dispatchers.Main) {
                        onSuccess.run()
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        onFailure.accept(
                            Exception(error)
                        )
                    }
                }
            )
        }
    }

    fun isLoggedIn(): Boolean {
        return accountStore.hasAccessToken()
    }

    fun dispose() {
        scope.cancel()
        customTabsServiceConnection.unbind()
    }

    fun bindCustomTabsService(context: Context) {
        customTabsServiceConnection.bind(context)
    }
}

class ServiceConnection(
    var uri: Uri
) : CustomTabsServiceConnection() {
    private var boundContext: Context? = null
    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null

    override fun onCustomTabsServiceConnected(
        name: ComponentName,
        client: CustomTabsClient
    ) {
        client.warmup(0)
        this.client = client

        val session = client.newSession(CustomTabsCallback())
        session?.mayLaunchUrl(uri, null, null)
        session?.mayLaunchUrl(
            "https://wordpress.com/log-in/".toUri(),
            null,
            null
        )

        this.session = session
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        this.client = null
        this.session = null
    }

    fun bind(context: Context) {
        // Do nothing if there is an existing service connection
        if (this.client != null) {
            return
        }

        // Get the default browser package name, this will be null if
        // the default browser does not provide a CustomTabsService
        val packageName =
            CustomTabsClient.getPackageName(context, null) ?: return

        CustomTabsClient.bindCustomTabsService(
            context,
            packageName,
            this
        )
        boundContext = context
    }

    fun unbind() {
        try {
            boundContext?.unbindService(this)
        } catch (_: IllegalArgumentException) {
            // Already unbound or never bound
        }
        boundContext = null
        client = null
        session = null
    }
}
