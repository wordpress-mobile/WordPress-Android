package org.wordpress.android.ui.accounts.login

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.network.rest.wpapi.WPcomLoginClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class WPcomLoginHelper @Inject constructor(
    private val loginClient: WPcomLoginClient,
    private val accountStore: AccountStore,
    private val appSecrets: AppSecrets
) {
    private val context: CoroutineContext = Dispatchers.IO

    val wpcomLoginUri = loginClient.loginUri(appSecrets.redirectUri)
    private val customTabsServiceConnection = ServiceConnection(wpcomLoginUri)
    private var processedAuthData: String? = null

    @Suppress("ReturnCount")
    fun tryLoginWithDataString(data: String?): Boolean {
        if (data == null || data == processedAuthData) {
            return false
        }

        val code = this.codeFromAuthorizationUri(data) ?: return false

        runBlocking {
            val tokenResult = loginClient.exchangeAuthCodeForToken(code)
            accountStore.updateAccessToken(tokenResult.getOrThrow())
            Log.i("WPCOM_LOGIN", "Login Successful")
        }

        processedAuthData = data
        return true
    }

    fun isLoggedIn(): Boolean {
        return accountStore.hasAccessToken()
    }

    fun dispose() {
        context.cancel()
    }

    fun bindCustomTabsService(context: Context) {
        customTabsServiceConnection.bind(context)
    }

    private fun codeFromAuthorizationUri(string: String): String? {
        return Uri.parse(string).getQueryParameter("code")
    }

    fun appendParamsToRestAuthorizationUrl(authorizationUrl: String?): String {
        return if (authorizationUrl.isNullOrEmpty()) {
            authorizationUrl.orEmpty()
        } else {
            authorizationUrl.toUri().buildUpon().apply {
                appendQueryParameter("app_name", "android-jetpack-client")
                appendQueryParameter("success_url", appSecrets.redirectUri)
            }.build().toString()
        }
    }
}

class ServiceConnection(
    var uri: Uri
): CustomTabsServiceConnection() {
    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null

    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
        client.warmup(0)
        this.client = client

        val session = client.newSession(CustomTabsCallback())
        session?.mayLaunchUrl(uri, null, null)
        session?.mayLaunchUrl(Uri.parse("https://wordpress.com/log-in/"), null, null)

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
        val packageName = CustomTabsClient.getPackageName(context, null)  ?:  return

        CustomTabsClient.bindCustomTabsService(context, packageName, this)
    }
}
