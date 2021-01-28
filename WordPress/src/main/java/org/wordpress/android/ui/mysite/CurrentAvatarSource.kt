package org.wordpress.android.ui.mysite

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentAvatarSource
@Inject constructor(private val accountStore: AccountStore) : MySiteSource<CurrentAvatarUrl> {
    private val avatarUrl = MutableLiveData<CurrentAvatarUrl>()
    override fun buildSource(siteId: Int): Flow<CurrentAvatarUrl?> {
        Log.d("vojta", "Building avatar url: ${avatarUrl.value}")
        return avatarUrl.asFlow()
    }
    fun refresh() {
        avatarUrl.value = CurrentAvatarUrl(accountStore.account?.avatarUrl.orEmpty())
    }
}

