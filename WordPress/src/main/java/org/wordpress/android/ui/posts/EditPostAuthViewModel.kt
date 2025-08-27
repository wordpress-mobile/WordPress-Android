package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.auth.WordPressCookieAuthenticator
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.util.AppLog
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class EditPostAuthViewModel @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val wordPressCookieAuthenticator: WordPressCookieAuthenticator,
    private val accountStore: AccountStore,
    private val userAgent: UserAgent
) : ScopedViewModel(ioDispatcher) {
    sealed class WpComCookieAuthState {
        data object Idle : WpComCookieAuthState()
        data object Loading : WpComCookieAuthState()
        data class Success(val cookies: Map<String, String>) : WpComCookieAuthState()
        data class Error(val message: String) : WpComCookieAuthState()
    }

    private val _wpComCookieAuthState = MutableLiveData<WpComCookieAuthState>(WpComCookieAuthState.Idle)
    val wpComCookieAuthState: LiveData<WpComCookieAuthState> = _wpComCookieAuthState

    fun fetchWpComCookies() {
        _wpComCookieAuthState.postValue(WpComCookieAuthState.Loading)

        val authParams = WordPressCookieAuthenticator.AuthParams(
            username = accountStore.account.userName ?: "",
            bearerToken = accountStore.accessToken ?: "",
            userAgent = userAgent.webViewUserAgent
        )

        launch {
            val result = wordPressCookieAuthenticator.authenticateForCookies(authParams)
            when (result) {
                is WordPressCookieAuthenticator.AuthResult.Success -> {
                    _wpComCookieAuthState.postValue(WpComCookieAuthState.Success(result.cookies))
                }

                is WordPressCookieAuthenticator.AuthResult.Failure -> {
                    AppLog.e(AppLog.T.EDITOR, "Failed to fetch cookies for Simple site: ${result.error}")
                    _wpComCookieAuthState.postValue(WpComCookieAuthState.Error(result.error))
                }
            }
        }
    }

    fun getCookiesForPrivateSites(siteModel: SiteModel, privateAtomicCookie: PrivateAtomicCookie): Map<String, String> {
        val cookies = when {
            !siteModel.isPrivate -> emptyMap()
            siteModel.isWPComAtomic -> getAtomicSiteCookies(siteModel, privateAtomicCookie)
            siteModel.isWPCom -> getWpComCookies(siteModel)
            else -> emptyMap()
        }
        return cookies
    }

    private fun getAtomicSiteCookies(
        siteModel: SiteModel,
        privateAtomicCookie: PrivateAtomicCookie
    ): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        if (privateAtomicCookie.exists() && !privateAtomicCookie.isExpired()) {
            val cookieName = privateAtomicCookie.getName()
            val cookieValue = privateAtomicCookie.getValue()
            val cookieDomain = privateAtomicCookie.getDomain()
            val value = "$cookieName=$cookieValue; domain=$cookieDomain; SameSite=None; Secure; HttpOnly"
            cookies[siteModel.url] = value
        }
        return cookies
    }

    private fun getWpComCookies(siteModel: SiteModel): Map<String, String> {
        return when (val currentState = _wpComCookieAuthState.value) {
            is WpComCookieAuthState.Success -> {
                currentState.cookies
                    .entries
                    .firstOrNull { (name, _) -> name.startsWith("wordpress_logged_in") }
                    ?.let { (name, value) ->
                        val cookieString = "$name=$value; domain=.wordpress.com; SameSite=None; Secure; HttpOnly"
                        mapOf(siteModel.url to cookieString)
                    }
                    ?: emptyMap()
            }
            else -> emptyMap()
        }
    }
}
