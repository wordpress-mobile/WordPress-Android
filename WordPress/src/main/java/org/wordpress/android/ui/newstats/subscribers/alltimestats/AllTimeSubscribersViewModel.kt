package org.wordpress.android.ui.newstats.subscribers.alltimestats

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersAllTimeResult
import org.wordpress.android.ui.newstats.subscribers.BaseSubscribersCardViewModel
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class AllTimeSubscribersViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseSubscribersCardViewModel<AllTimeSubscribersUiState>(
    selectedSiteRepository,
    accountStore,
    statsRepository,
    resourceProvider,
    AllTimeSubscribersUiState.Loading
) {
    override val loadingState = AllTimeSubscribersUiState.Loading

    override fun errorState(
        message: String,
        isAuthError: Boolean
    ) = AllTimeSubscribersUiState.Error(message, isAuthError)

    override suspend fun loadDataInternal(siteId: Long) {
        when (
            val result = statsRepository
                .fetchSubscribersAllTime(siteId)
        ) {
            is SubscribersAllTimeResult.Success -> {
                markLoadedSuccessfully()
                updateState(
                    AllTimeSubscribersUiState.Loaded(
                        currentCount = result.currentCount,
                        count30DaysAgo = result.count30DaysAgo,
                        count60DaysAgo = result.count60DaysAgo,
                        count90DaysAgo = result.count90DaysAgo
                    )
                )
            }
            is SubscribersAllTimeResult.Error -> {
                updateState(
                    AllTimeSubscribersUiState.Error(
                        message = resourceProvider.getString(
                            result.messageResId
                        ),
                        isAuthError = result.isAuthError
                    )
                )
            }
        }
    }
}
