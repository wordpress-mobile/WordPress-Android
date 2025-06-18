package org.wordpress.android.ui.accounts.applicationpassword

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.ui.mysite.menu.MenuViewModel.SnackbarMessage
import org.wordpress.android.util.BuildConfigWrapper
import javax.inject.Inject

class ApplicationPasswordLoginViewModel @Inject constructor() : ViewModel() {

    private val _runMain = MutableSharedFlow<Boolean>()
    val runMain = _runMain.asSharedFlow()

    fun setupSite() {
        // TODO: Init site
        // TODO store credentials
    }
}
