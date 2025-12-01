package org.wordpress.android.ui.accounts.login.applicationpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.Companion.ANDROID_JETPACK_CLIENT
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.Companion.ANDROID_WORDPRESS_CLIENT
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.UriLogin
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ApplicationPasswordCreateParams
import uniffi.wp_api.WpUuid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ApplicationPasswordAutoAuthDialogViewModel @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun createApplicationPassword(site: SiteModel) {
        viewModelScope.launch {
            try {
                require(site.username.isNotBlank()) { "Site username is required for cookie authentication" }
                require(site.password.isNotBlank()) { "Site password is required for cookie authentication" }

                _isLoading.value = true
                val client = wpApiClientProvider.getWpApiClientCookiesNonceAuthentication(
                    site = site,
                )
                val appName = if (buildConfigWrapper.isJetpackApp) {
                    ANDROID_JETPACK_CLIENT
                } else {
                    ANDROID_WORDPRESS_CLIENT
                }
                val appId = WpUuid()
                val response = client.request { requestBuilder ->
                    requestBuilder.applicationPasswords().createForCurrentUser(
                        params = ApplicationPasswordCreateParams(
                            appId = appId.uuidString(),
                            name =
                                "$appName-${SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault()).format(Date())}"
                        )
                    )
                }
                when (response) {
                    is WpRequestResult.Success -> {
                        val name = site.username
                        val password = response.response.data.password
                        val apiRootUrl = wpApiClientProvider.getApiRootUrlFrom(site)
                        applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
                            UriLogin(
                                siteUrl = site.url,
                                user = name,
                                password = password,
                                apiRootUrl = apiRootUrl
                            )
                        )
                        _navigationEvent.emit(NavigationEvent.Success)
                    }

                    else -> {
                        appLogWrapper.e(AppLog.T.API, "Error creating application password")
                        _navigationEvent.emit(NavigationEvent.Error)
                    }
                }
            } catch (e: Exception) {
                appLogWrapper.e(AppLog.T.API, "Exception creating application password: ${e.message}")
                _navigationEvent.emit(NavigationEvent.Error)
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class NavigationEvent {
        object Success : NavigationEvent()
        object Error : NavigationEvent()
    }
}
