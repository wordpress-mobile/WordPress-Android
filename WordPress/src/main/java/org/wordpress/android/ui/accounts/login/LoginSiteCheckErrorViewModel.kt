package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class LoginSiteCheckErrorViewModel  @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    fun start() {
        if (isStarted) return
        isStarted = true
    }
}
