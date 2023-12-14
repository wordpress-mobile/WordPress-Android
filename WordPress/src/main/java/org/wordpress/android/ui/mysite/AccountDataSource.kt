package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.AccountData
import javax.inject.Inject

class AccountDataSource @Inject constructor(
    private val accountStore: AccountStore
) : SiteIndependentSource<AccountData> {
    override val refresh = MutableLiveData(false)

    override fun build(coroutineScope: CoroutineScope): LiveData<AccountData> {
        val result = MediatorLiveData<AccountData>()
        result.addSource(refresh) { result.refreshData(refresh.value) }
        refresh()
        return result
    }

    private fun MediatorLiveData<AccountData>.refreshData(
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> {
                val url = accountStore.account?.avatarUrl.orEmpty()
                val name =
                    accountStore.account?.displayName?.ifEmpty { accountStore.account?.userName.orEmpty() }.orEmpty()
                setState(AccountData(url,name))
            }
            false -> Unit // Do nothing
        }
    }
}
