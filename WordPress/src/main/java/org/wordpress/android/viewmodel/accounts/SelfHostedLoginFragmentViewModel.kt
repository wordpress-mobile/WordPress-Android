package org.wordpress.android.viewmodel.accounts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.accounts.LoginActivity
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpUuid
import uniffi.wp_api.createApplicationPasswordAuthenticationUrl
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class SelfHostedLoginFragmentViewModel @Inject constructor(
    private val loginActivity: Provider<LoginActivity>
): ViewModel() {
    sealed class ScreenState {
        data object Empty : ScreenState()
        data object Loading: ScreenState()
        class Error(val message: String) : ScreenState()
    }

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Empty)
    val uiState: StateFlow<ScreenState>
        get() = _uiState

    private val _siteUrl = MutableStateFlow(" ")
    val siteUrl = _siteUrl.asStateFlow()

    fun setSiteUrl(newValue: String) {
        if(newValue == "") {
            _siteUrl.value = " "
        } else {
            _siteUrl.value = newValue.trim()
        }
    }

    fun didTapContinue(context: Context) {
        print("Tapped Continue!")
        _uiState.value = ScreenState.Loading
        startSiteAutodiscovery(siteUrl.value, context)
    }

    private fun startSiteAutodiscovery(url: String, context: Context) {

        viewModelScope.launch {
            // In the background, run the API discovery test to see if we can add this site for the REST API
            try {
                val authenticationUrl = WpLoginClient()
                    .apiDiscovery(url)
                    .getOrThrow()
                    .apiDetails
                    .findApplicationPasswordsAuthenticationUrl()

                if(authenticationUrl.isNullOrEmpty()) {
                    throw Exception("Invalid Authentication URL")
                }

                Log.d("WP_RS", "Found authorization URL: $authenticationUrl")
                AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)

                val loginUri = Uri.parse(createApplicationPasswordAuthenticationUrl(
                    ParsedUrl.parse(authenticationUrl),
                    "Jetpack for Android",
                    WpUuid.parse("00000000-0000-4000-8000-000000000000"),
                    "jetpack://authorize-success",
                    "jetpack://authorize-failure"
                ).url())

                val intent = Intent(Intent.ACTION_VIEW, loginUri)
                startActivity(context, intent, null)
            } catch (ex: Exception) {
                Log.e("WP_RS", "Unable to find authorization URL:" + ex.message)
                AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
                onErrorOccurred(ex.localizedMessage)
            }
        }
    }

    private fun onErrorOccurred(error: String?) {
        _uiState.value = ScreenState.Error(error ?: "Login Failed")
    }
}
