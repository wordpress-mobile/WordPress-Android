package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.StoreCredentialsResult
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.UriLogin
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DeviceUtils
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ApplicationPasswordCreateParams
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpUuid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ApplicationPasswordAutoAuthDialogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @Suppress("TooGenericExceptionCaught", "LongMethod")
    fun createApplicationPassword(site: SiteModel, creationSource: String) {
        viewModelScope.launch {
            try {
                require(site.username.isNotBlank()) { "Site username is required for cookie authentication" }
                require(site.password.isNotBlank()) { "Site password is required for cookie authentication" }

                _isLoading.value = true
                val client = wpApiClientProvider.getWpApiClientCookiesNonceAuthentication(
                    site = site,
                )
                val deviceName = DeviceUtils.getInstance().getDeviceName(context)
                val appName = if (buildConfigWrapper.isJetpackApp) {
                    context.getString(R.string.application_password_app_name_jetpack, deviceName)
                } else {
                    context.getString(R.string.application_password_app_name_wordpress, deviceName)
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
                        val apiRootUrl =
                            wpApiClientProvider.getApiRootUrlFrom(site)
                        val result = applicationPasswordLoginHelper
                            .storeApplicationPasswordCredentialsFrom(
                                urlLogin = UriLogin(
                                    siteUrl = site.url,
                                    user = name,
                                    password = password,
                                    apiRootUrl = apiRootUrl
                                ),
                                creationSource = creationSource
                            )
                        when (result) {
                            is StoreCredentialsResult.Success ->
                                _navigationEvent.emit(
                                    NavigationEvent.Success
                                )
                            else ->
                                _navigationEvent.emit(
                                    NavigationEvent.Error
                                )
                        }
                    }

                    is WpRequestResult.WpError -> {
                        appLogWrapper.e(
                            AppLog.T.API,
                            "Error creating application password: WpError - ${response.errorMessage}"
                        )
                        fallbackToManualLogin(site.url)
                    }

                    is WpRequestResult.RequestExecutionFailed -> {
                        val isTimeout = response.reason is RequestExecutionErrorReason.HttpTimeoutError
                        if (isTimeout) {
                            appLogWrapper.e(AppLog.T.API, "Error creating application password: Request timed out")
                        } else {
                            appLogWrapper.e(
                                AppLog.T.API,
                                "Error creating application password: RequestExecutionFailed - " +
                                    "reason=${response.reason}, statusCode=${response.statusCode}"
                            )
                        }
                        fallbackToManualLogin(site.url)
                    }

                    else -> {
                        logCreationError(
                            site.url,
                            "${response::class.simpleName} - $response"
                        )
                        fallbackToManualLogin(site.url)
                    }
                }
            } catch (e: Exception) {
                logCreationError(site.url, e.message.orEmpty())
                fallbackToManualLogin(site.url)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun logCreationError(siteUrl: String, detail: String) {
        appLogWrapper.e(
            AppLog.T.API,
            "A_P: Error creating application password for: $siteUrl - $detail"
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fallbackToManualLogin(siteUrl: String) {
        try {
            val authUrl = applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl)
            _navigationEvent.emit(NavigationEvent.FallbackToManualLogin(authUrl))
        } catch (e: Exception) {
            appLogWrapper.e(
                AppLog.T.API,
                "A_P: Failed to get authorization URL" +
                    " for: $siteUrl - ${e.message}"
            )
            _navigationEvent.emit(NavigationEvent.Error)
        }
    }

    sealed class NavigationEvent {
        object Success : NavigationEvent()
        data class FallbackToManualLogin(val authUrl: String) : NavigationEvent()
        object Error : NavigationEvent()
    }
}
