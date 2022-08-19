package org.wordpress.android.ui.mysite.jetpackbadge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackPoweredDialogViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackBrandingUtils: JetpackBrandingUtils
) : ScopedViewModel(mainDispatcher) {
    private val _action = MutableLiveData<JetpackPoweredDialogAction>()
    val action: LiveData<JetpackPoweredDialogAction> = _action

    fun openJetpackAppDownloadLink() {
        jetpackBrandingUtils.trackGetJetpackAppTapped()
        _action.value = JetpackPoweredDialogAction.OpenPlayStore
    }

    fun dismissBottomSheet() {
        jetpackBrandingUtils.trackDismissTapped()
        _action.value = JetpackPoweredDialogAction.DismissDialog
    }
}
