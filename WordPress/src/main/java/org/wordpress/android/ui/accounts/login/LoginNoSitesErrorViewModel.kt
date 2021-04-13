package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.WordPress
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class LoginNoSitesErrorViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    fun signOutWordPress(application: WordPress) {
        launch {
            withContext(bgDispatcher) {
                application.wordPressComSignOut()
            }
        }
    }
}

