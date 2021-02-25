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
    override fun buildSource(coroutineScope: CoroutineScope): LiveData<CurrentAvatarUrl> {
        val result = MediatorLiveData<CurrentAvatarUrl>()
        result.value = CurrentAvatarUrl(accountStore.account?.avatarUrl.orEmpty())
        result.addSource(avatarUrl) {
            result.value = it
        }
        return result
    }
    fun refresh() {
        val url = accountStore.account?.avatarUrl.orEmpty()
        if (url != avatarUrl.value?.url) {
            avatarUrl.postValue(CurrentAvatarUrl(url))
        }
    }
}
