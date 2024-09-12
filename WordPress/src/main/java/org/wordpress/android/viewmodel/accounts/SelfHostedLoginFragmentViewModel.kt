package org.wordpress.android.viewmodel.accounts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpUuid
import uniffi.wp_api.createApplicationPasswordAuthenticationUrl
import javax.inject.Inject

@HiltViewModel
class SelfHostedLoginFragmentViewModel @Inject constructor(

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

    @SuppressLint("StaticFieldLeak")
    var context: Context? = null

    private var discoveryTask: Job? = null

    fun setSiteUrl(newValue: String) {
        if(newValue == "") {
            _siteUrl.value = " "
        } else {
            _siteUrl.value = newValue.trim()
        }
    }

    fun didTapContinue() {
        print("Tapped Continue!")
        _uiState.value = ScreenState.Loading

        discoveryTask?.cancel()

        Log.d("WP_RS", "Attempting autodiscovery")
        discoveryTask = viewModelScope.launch(Dispatchers.IO) {
            Log.d("WP_RS", "In Coroutine")

            // In the background, run the API discovery test to see if we can add this site for the REST API
            try {
                Log.d("WP_RS", siteUrl.value)
                val uri = getLoginUri(siteUrl.value)
                Log.d("WP_RS", "$uri")
                showLoginScreen(uri)
            } catch (ex: Exception) {
                Log.e("WP_RS", "Unable to find authorization URL:" + ex.message)
                AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
                onErrorOccurred(ex.localizedMessage)
            }

            Log.d("WP_RS", "Done trying")
        }
    }

    private suspend fun getLoginUri(string: String): Uri {
        Log.d("WP_RS", string)

        val authenticationUrl = WpLoginClient()
            .apiDiscovery(string)
            .getOrThrow()
            .apiDetails
            .findApplicationPasswordsAuthenticationUrl()

        Log.d("WP_RS", "Received reply")

        if(authenticationUrl.isNullOrEmpty()) {
            throw Exception("Invalid Authentication URL")
        }

        Log.d("WP_RS", "Found authorization URL: $authenticationUrl")
        AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)

        return Uri.parse(createApplicationPasswordAuthenticationUrl(
            ParsedUrl.parse(authenticationUrl),
            "Jetpack for Android",
            WpUuid.parse("00000000-0000-4000-8000-000000000000"),
            "jetpack://authorize-success",
            "jetpack://authorize-failure"
        ).url())
    }

    private fun showLoginScreen(loginUri: Uri) = runBlocking(Dispatchers.Main) {
        context?.let {
            startActivity(it, Intent(Intent.ACTION_VIEW, loginUri), null)
        }
    }

    private fun onErrorOccurred(error: String?) {
        _uiState.value = ScreenState.Error(error ?: "Login Failed")
    }
}
