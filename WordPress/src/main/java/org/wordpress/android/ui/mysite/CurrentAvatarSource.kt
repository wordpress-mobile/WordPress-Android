package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentAvatarSource
@Inject constructor(private val accountStore: AccountStore) : SiteIndependentSource<CurrentAvatarUrl> {
    private val avatarUrl = MutableLiveData<CurrentAvatarUrl>()
    override fun buildSource(): Flow<CurrentAvatarUrl?> {
        return avatarUrl.asFlow().onStart { CurrentAvatarUrl("") }
    }
    fun refresh() {
        avatarUrl.value = CurrentAvatarUrl(accountStore.account?.avatarUrl.orEmpty())
    }
}

