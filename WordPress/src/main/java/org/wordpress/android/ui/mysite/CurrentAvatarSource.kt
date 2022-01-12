package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import javax.inject.Inject

class CurrentAvatarSource @Inject constructor(
    private val accountStore: AccountStore
) : SiteIndependentSource<CurrentAvatarUrl> {
    override val refresh = MutableLiveData(false)

    override fun build(coroutineScope: CoroutineScope): LiveData<CurrentAvatarUrl> {
        val result = MediatorLiveData<CurrentAvatarUrl>()
        result.addSource(refresh) { result.refreshData(refresh.value) }
        refresh()
        return result
    }

    private fun MediatorLiveData<CurrentAvatarUrl>.refreshData(
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> {
                val url = accountStore.account?.avatarUrl.orEmpty()
                setState(CurrentAvatarUrl(url))
            }
            false -> Unit // Do nothing
        }
    }
}
