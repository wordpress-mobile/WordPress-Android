package org.wordpress.android.ui.accounts.applicationpassword

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "ApplicationPasswordLoginViewModel"

class ApplicationPasswordLoginViewModel @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
) : ViewModel() {
    private val _onFinishedEvent = MutableSharedFlow<Boolean>()
    val onFinishedEvent = _onFinishedEvent.asSharedFlow()

    /**
     * This method is called to set up the site with the provided raw data.
     *
     * @param rawData The raw data containing the callback data from the application password login.
     */
    fun setupSite(rawData: String) {
        viewModelScope.launch {
            val stored = storeCredentials(rawData)
            _onFinishedEvent.emit(stored)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun storeCredentials(rawData: String): Boolean = withContext(ioDispatcher) {
        try {
            if (rawData.isEmpty()) {
                Log.e(TAG, "Cannot store credentials: rawData is empty")
                false
            } else {
                val credentialsStored = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData)
                credentialsStored
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing credentials", e)
            false
        }
    }
}
