package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.AccountData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountDataViewModelSlice @Inject constructor(
    private val accountStore: AccountStore
) {
    private lateinit var scope: CoroutineScope

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _uiModel = MutableLiveData<AccountData>()
    val uiModel: LiveData<AccountData> = _uiModel

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    val refresh = MutableLiveData(false)

    private fun getAccountData() {
        scope.launch {
            _isRefreshing.postValue(true)
            val url = accountStore.account?.avatarUrl.orEmpty()
            val name = accountStore.account?.displayName?.ifEmpty { accountStore.account?.userName.orEmpty() }.orEmpty()
            _uiModel.postValue(AccountData(url, name))
            _isRefreshing.postValue(false)
        }
    }

    fun refresh() {
        getAccountData()
    }
}
