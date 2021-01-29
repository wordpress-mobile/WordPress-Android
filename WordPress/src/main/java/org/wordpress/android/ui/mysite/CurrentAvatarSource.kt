package org.wordpress.android.ui.mysite

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentAvatarSource
@Inject constructor(private val accountStore: AccountStore) : SiteIndependentSource<CurrentAvatarUrl> {
    private val avatarUrl = MutableStateFlow(CurrentAvatarUrl(""))
    override fun buildSource(): Flow<CurrentAvatarUrl> = avatarUrl
    fun refresh() {
        avatarUrl.value = CurrentAvatarUrl(accountStore.account?.avatarUrl.orEmpty())
    }
}
