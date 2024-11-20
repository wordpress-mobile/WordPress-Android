package org.wordpress.android.ui.accounts.login

import android.net.Uri
import android.util.Log
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

    fun loginUri(): Uri {
        return loginClient.loginUri(appSecrets.redirectUri)
    }

    fun tryLoginWithDataString(data: String?) {
        if (data == null) {
            return
        }

        val code = this.codeFromAuthorizationUri(data) ?: return

        runBlocking {
            val tokenResult = loginClient.exchangeAuthCodeForToken(code)
            accountStore.updateAccessToken(tokenResult.getOrThrow())
            Log.i("WPCOM_LOGIN", "Login Successful")
        }
    }

    fun isLoggedIn(): Boolean {
        return accountStore.hasAccessToken()
    }

    fun dispose() {
        context.cancel()
    }

    private fun codeFromAuthorizationUri(string: String): String? {
        return Uri.parse(string).getQueryParameter("code")
    }
}
