package org.wordpress.android.ui.accounts.applicationpassword

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import javax.inject.Inject
import javax.inject.Named

class ApplicationPasswordLoginViewModel @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _onFinishedEvent = MutableSharedFlow<Boolean>()
    val onFinishedEvent = _onFinishedEvent.asSharedFlow()

    fun setupSite(rawCredentials: String) {
        // TODO: Init site
        // TODO store credentials
        viewModelScope.launch {
            saveCredentials(rawCredentials)
        }
    }

    suspend private fun saveCredentials(rawCredentials: String): Boolean = withContext(ioDispatcher) {
        if (rawCredentials.isEmpty()) {
            false
        } else {
            val credentialsStoredResult = async { applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawCredentials) }
            val credentialsStored = credentialsStoredResult.await()
            Log.d(
                "ApplicationPasswordLoginViewModel",
                "Credentials stored: $credentialsStored"
            )
            credentialsStored
        }
    }
}
