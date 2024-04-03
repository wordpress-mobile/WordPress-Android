package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.AccountData
import org.wordpress.android.util.BuildConfigWrapper
import javax.inject.Inject

class AccountDataViewModelSlice @Inject constructor(
    private val accountStore: AccountStore,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
) {
    private lateinit var scope: CoroutineScope

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _uiModel = MutableLiveData<AccountData?>()
    val uiModel: LiveData<AccountData?> = _uiModel

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun onResume() {
        scope.launch {
            if(!shouldBuildCard()) _uiModel.postValue(null)
            _isRefreshing.postValue(true)
            val account = accountStore.account
            account?.let {
                val url = account.avatarUrl.orEmpty()
                val name =account.displayName?.ifEmpty {
                    account.userName.orEmpty()
                }.orEmpty()
                _uiModel.postValue(AccountData(url, name))
            }?: {
                _uiModel.postValue(null)
            }
            _isRefreshing.postValue(false)
        }
    }

    fun onRefresh() {
        onResume()
    }

    fun onCleared() {
        scope.cancel()
    }

    private fun shouldBuildCard(): Boolean {
        return (!buildConfigWrapper.isJetpackApp
                && jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures())
    }
}
