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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import java.security.SecureRandom
import java.time.Clock
import javax.inject.Inject

class WPcomLoginHelper @Inject constructor(
    private val loginClient: WPcomLoginClient,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val appSecrets: AppSecrets,
    private val clock: Clock,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Only used to warm up the Custom Tab. The URL that is actually opened comes from
    // [startLogin], which adds a fresh `state`.
    private val customTabsServiceConnection = ServiceConnection(loginClient.loginUri(appSecrets.redirectUri))

    /**
     * Returns the WP.com login URL to open, with a new random `state`. The state is saved so the
     * OAuth callback can be matched to this login, even if the app process is killed meanwhile.
     *
     * A login that was already pending stays valid for [SUPERSEDED_STATE_LIFETIME_MILLIS]: the user
     * may have moved its page to the browser and still finish it there after starting this one.
     */
    fun startLogin(): Uri {
        appPrefsWrapper.pendingWpcomOAuthState?.let { previous ->
            val superseded = listOf(SupersededState(previous, clock.millis())) + readSupersededStates()
            writeSupersededStates(superseded.take(MAX_SUPERSEDED_STATES))
        }
        val state = generateState()
        appPrefsWrapper.pendingWpcomOAuthState = state
        return loginClient.loginUri(appSecrets.redirectUri, state)
    }

    /**
     * Returns true if the data string contains an OAuth callback code.
     */
    fun hasOAuthCallback(data: String?): Boolean {
        if (data == null) return false
        return data.toUri().getQueryParameter("code") != null
    }

    /**
     * Matches the `state` in the data string against the logins this app started, without using it
     * up. Lets the caller ignore a stray link before it changes any UI.
     *
     * - [OAuthCallbackStatus.VALID]: the current login, or one replaced by it less than
     *   [SUPERSEDED_STATE_LIFETIME_MILLIS] ago. [tryLoginWithDataString] will accept it.
     * - [OAuthCallbackStatus.EXPIRED]: a login this app started, but replaced too long ago.
     * - [OAuthCallbackStatus.UNKNOWN]: not a login this app started, or one already completed.
     */
    fun checkCallback(data: String?): OAuthCallbackStatus {
        val state = data?.toUri()?.getQueryParameter("state")
        val superseded = state?.let { readSupersededStates().firstOrNull { it.state == state } }
        return when {
            state == null -> OAuthCallbackStatus.UNKNOWN
            state == appPrefsWrapper.pendingWpcomOAuthState -> OAuthCallbackStatus.VALID
            superseded == null -> OAuthCallbackStatus.UNKNOWN
            clock.millis() - superseded.supersededAtMillis <= SUPERSEDED_STATE_LIFETIME_MILLIS ->
                OAuthCallbackStatus.VALID
            else -> OAuthCallbackStatus.EXPIRED
        }
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
            onFailure.accept(IllegalArgumentException("Missing OAuth code in callback"))
            return
        }

        // Only accept a callback for a login this app started. A link from anywhere else can
        // carry a code for someone else's account, and exchanging it would sign the user in to
        // that account. The states are single-use: a match clears them all, but a mismatch leaves
        // them in place so a stray link can't break a login that is in progress.
        when (checkCallback(data)) {
            OAuthCallbackStatus.VALID -> exchangeCode(code, onSuccess, onFailure)
            OAuthCallbackStatus.EXPIRED -> onFailure.accept(WPcomLoginSessionExpiredException())
            OAuthCallbackStatus.UNKNOWN -> onFailure.accept(WPcomLoginStateMismatchException())
        }
    }

    private fun exchangeCode(
        code: String,
        onSuccess: Runnable,
        onFailure: java.util.function.Consumer<Exception>
    ) {
        appPrefsWrapper.pendingWpcomOAuthState = null
        appPrefsWrapper.supersededWpcomOAuthStates = null

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

    private fun generateState(): String {
        val bytes = ByteArray(STATE_BYTES).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun readSupersededStates(): List<SupersededState> {
        val json = appPrefsWrapper.supersededWpcomOAuthStates ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                SupersededState(item.getString(KEY_STATE), item.getLong(KEY_SUPERSEDED_AT))
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun writeSupersededStates(states: List<SupersededState>) {
        val array = JSONArray()
        states.forEach {
            array.put(JSONObject().put(KEY_STATE, it.state).put(KEY_SUPERSEDED_AT, it.supersededAtMillis))
        }
        appPrefsWrapper.supersededWpcomOAuthStates = array.toString()
    }

    private data class SupersededState(val state: String, val supersededAtMillis: Long)

    companion object {
        private const val STATE_BYTES = 32
        private const val MAX_SUPERSEDED_STATES = 5
        private const val SUPERSEDED_STATE_LIFETIME_MILLIS = 10 * 60 * 1000L
        private const val KEY_STATE = "state"
        private const val KEY_SUPERSEDED_AT = "superseded_at"
    }
}

enum class OAuthCallbackStatus { VALID, EXPIRED, UNKNOWN }

class WPcomLoginStateMismatchException :
    IllegalStateException("OAuth callback state does not match a login started by the app")

class WPcomLoginSessionExpiredException :
    IllegalStateException("OAuth callback is for a login the app started, but replaced too long ago")

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
