package org.wordpress.android.ui.mysite.personalisation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PersonalisationViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableLiveData<List<DashboardCardState>>()
    val uiState: LiveData<List<DashboardCardState>> = _uiState

    fun getCardStateList() {
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId ?: return
        val items = mutableListOf<DashboardCardState>()
        items.add(CardType.STATS.order, getStatsCard(siteId))
        items.add(CardType.DRAFT_POSTS.order, getDraftPostsCard(siteId))
    }

    private fun getStatsCard(siteId: Long) = DashboardCardState(
        title = R.string.my_site_todays_stat_card_title,
        enabled = appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(siteId),
        cardType = CardType.STATS
    )

    private fun getDraftPostsCard(siteId: Long) = DashboardCardState(
        title = R.string.,
        enabled = appPrefsWrapper.getShouldHidePostDashboardCard(siteId, PostCardType.DRAFT.name),
        cardType = CardType.DRAFT_POSTS
    )

    fun onToggle(currentState: Boolean, cardType: CardType) {
        // todo., the logic to toggle the hide or show of the cards
    }
}
