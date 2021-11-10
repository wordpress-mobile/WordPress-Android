package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentAvatarSource
@Inject constructor(private val accountStore: AccountStore) : SiteIndependentSource<CurrentAvatarUrl> {
    private val avatarUrl = MutableLiveData<CurrentAvatarUrl>()
    val refresh: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    override fun buildSource(coroutineScope: CoroutineScope): LiveData<CurrentAvatarUrl> {
        val result = MediatorLiveData<CurrentAvatarUrl>()
        result.refreshData(false)
        result.addSource(refresh) {
            if (refresh.value == true) {
                result.refreshData(true)
            }
        }
        return result
    }

    fun refresh() {
        refresh.postValue(true)
    }

    private fun MediatorLiveData<CurrentAvatarUrl>.refreshData(
        isRefresh: Boolean
    ) {
        val url = accountStore.account?.avatarUrl.orEmpty()
        if (isRefresh) {
            refresh.postValue(false)
            if (url != avatarUrl.value?.url) {
                this@refreshData.postValue(CurrentAvatarUrl(url))
            }
        } else {
            this@refreshData.postValue(CurrentAvatarUrl(url))
        }
    }
}
