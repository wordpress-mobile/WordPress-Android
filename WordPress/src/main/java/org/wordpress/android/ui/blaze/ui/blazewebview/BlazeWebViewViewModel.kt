package org.wordpress.android.ui.blaze.ui.blazewebview

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

@HiltViewModel
class BlazeWebViewViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val siteRepository: SelectedSiteRepository
) : ViewModel() {
    // todo: fill in the state
    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState> = _screenState

    fun start() {
        // todo: these are here just to use the values to pass compile, probably an annotation for this too!
        Log.i(javaClass.simpleName, "Username ${accountStore.account.userName}")
        Log.i(javaClass.simpleName, "SiteId ${siteRepository.getSelectedSite()?.siteId}")
        Log.i(javaClass.simpleName, "Hide Promote with blaze ${blazeFeatureUtils.hidePromoteWithBlazeCard()}")
    }
}

// todo: annmarie move this to a new file
sealed class ScreenState {
    val title: UiString = UiString.UiStringRes(R.string.blaze_activity_title)
  //  val actionText: UiString = UiString.UiStringRes(R.string.cancel)
    open val actionVisible: Boolean = true

    data class Loading(
        override val actionVisible: Boolean = true
    ): ScreenState()

    data class HiddenStep(
        override val actionVisible: Boolean = false
    ): ScreenState()
}
