package org.wordpress.android.ui.accounts.login

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.network.rest.wpapi.WPcomLoginClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class WPcomLoginHelperTest {
    private val appSecrets = AppSecrets("app-id", "app-secret", REDIRECT_URI)

    // Request bodies sent to the token endpoint, i.e. every code the helper tried to exchange
    private val tokenRequests = CopyOnWriteArrayList<String>()
    private val tokenRequestSent = CountDownLatch(1)
    private val tokenEndpoint = Interceptor { chain ->
        tokenRequests.add(Buffer().also { chain.request().body?.writeTo(it) }.readUtf8())
        tokenRequestSent.countDown()
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(HTTP_OK)
            .message("OK")
            .body(TOKEN_RESPONSE.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private var storedState: String? = null
    private val appPrefsWrapper: AppPrefsWrapper = mock<AppPrefsWrapper>().also { prefs ->
        whenever(prefs.pendingWpcomOAuthState).thenAnswer { storedState }
        Mockito.doAnswer { invocation ->
            storedState = invocation.getArgument<String?>(0)
            null
        }.`when`(prefs).pendingWpcomOAuthState = anyOrNull()
    }

    private val helper = WPcomLoginHelper(
        loginClient = WPcomLoginClient(Dispatchers.Unconfined, appSecrets, setOf(tokenEndpoint)),
        accountStore = mock<AccountStore>(),
        appPrefsWrapper = appPrefsWrapper,
        appSecrets = appSecrets,
    )

    private var failure: Exception? = null

    private fun handleCallback(data: String) {
        helper.tryLoginWithDataString(data, onSuccess = {}, onFailure = { failure = it })
    }

    @Test
    fun `starting a login saves a new state and sends it to WordPress_com`() {
        val first = helper.startLogin()
        val firstState = storedState
        val second = helper.startLogin()

        assertThat(firstState).hasSize(STATE_HEX_LENGTH)
        assertThat(first.getQueryParameter("state")).isEqualTo(firstState)
        assertThat(second.getQueryParameter("state")).isEqualTo(storedState).isNotEqualTo(firstState)
        assertThat(second.getQueryParameter("redirect_uri")).isEqualTo(REDIRECT_URI)
    }

    @Test
    fun `a callback with the saved state exchanges the code and uses up the state`() {
        val state = helper.startLogin().getQueryParameter("state")

        handleCallback(callback(code = "the-code", state = state))

        assertThat(tokenRequestSent.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue
        assertThat(tokenRequests.single()).contains("code=the-code")
        assertThat(storedState).isNull()
        assertThat(failure).isNull()
    }

    @Test
    fun `a callback with a different state is rejected and the login in progress is kept`() {
        helper.startLogin()
        val savedState = storedState

        handleCallback(callback(code = "attacker-code", state = "attacker-state"))

        assertThat(failure).isInstanceOf(WPcomLoginStateMismatchException::class.java)
        assertThat(storedState).isEqualTo(savedState)
        assertThat(tokenRequests).isEmpty()
    }

    @Test
    fun `a callback without a state is rejected`() {
        helper.startLogin()

        handleCallback(callback(code = "attacker-code", state = null))

        assertThat(failure).isInstanceOf(WPcomLoginStateMismatchException::class.java)
        assertThat(tokenRequests).isEmpty()
    }

    @Test
    fun `a callback when no login was started is rejected`() {
        handleCallback(callback(code = "attacker-code", state = "any-state"))

        assertThat(failure).isInstanceOf(WPcomLoginStateMismatchException::class.java)
        assertThat(tokenRequests).isEmpty()
    }

    @Test
    fun `a callback can only be used once`() {
        val state = helper.startLogin().getQueryParameter("state")
        handleCallback(callback(code = "the-code", state = state))
        assertThat(tokenRequestSent.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue

        handleCallback(callback(code = "replayed-code", state = state))

        assertThat(failure).isInstanceOf(WPcomLoginStateMismatchException::class.java)
        assertThat(tokenRequests).hasSize(1)
    }

    @Test
    fun `only a callback with the saved state matches the pending login`() {
        assertThat(helper.matchesPendingLogin(callback(code = "c", state = "any-state"))).isFalse

        val state = helper.startLogin().getQueryParameter("state")

        assertThat(helper.matchesPendingLogin(callback(code = "c", state = state))).isTrue
        assertThat(helper.matchesPendingLogin(callback(code = "c", state = "attacker-state"))).isFalse
        assertThat(helper.matchesPendingLogin(callback(code = "c", state = null))).isFalse
        assertThat(helper.matchesPendingLogin(null)).isFalse
        // Checking doesn't use up the state
        assertThat(storedState).isEqualTo(state)
    }

    private fun callback(code: String, state: String?): String =
        Uri.parse(REDIRECT_URI).buildUpon()
            .appendQueryParameter("code", code)
            .apply { state?.let { appendQueryParameter("state", it) } }
            .build()
            .toString()

    companion object {
        private const val REDIRECT_URI = "jetpack://wpcom-authorize"
        private const val STATE_HEX_LENGTH = 64
        private const val TIMEOUT_SECONDS = 5L
        private const val HTTP_OK = 200
        private const val TOKEN_RESPONSE = """{"access_token":"token","token_type":"bearer",""" +
            """"blog_id":"0","blog_url":"","scope":"global"}"""
    }
}
