package org.wordpress.android.ui.accounts.login

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.network.rest.wpapi.WPcomLoginClient
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.config.AppConfig
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class WPcomLoginHelper @Inject constructor(
    private val loginClient: WPcomLoginClient,
    private val accountStore: AccountStore,
    private val appConfig: AppConfig
) {
    private val context: CoroutineContext = Dispatchers.IO

    fun loginUri(): Uri {
        return loginClient.loginUri(BuildConfig.OAUTH_REDIRECT_URI)
    }

    fun tryLoginWithDataString(data: String?) {
        if (data == null) {
            return
        }

        val code = this.codeFromAuthorizationUri(data) ?: return

        runBlocking {
            val tokenResult = loginClient.exchangeAuthCodeForToken(code, BuildConfig.OAUTH_REDIRECT_URI)
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
