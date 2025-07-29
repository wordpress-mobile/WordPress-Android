package org.wordpress.android.ui.jetpackconnection

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class JetpackConnectionViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    private val _step = MutableStateFlow<ConnectionStep?>(null)
    val step = _step

    sealed class ConnectionStep {
        data object LoginWpCom : ConnectionStep()
        data object InstallJetpack : ConnectionStep()
        data object ConnectSite : ConnectionStep()
        data object ConnectWpCom : ConnectionStep()
        data object Finalize : ConnectionStep()
    }
}
