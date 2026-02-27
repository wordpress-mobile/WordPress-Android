package org.wordpress.android.ui.newstats.subscribers.subscriberslist

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersListResult
import org.wordpress.android.ui.newstats.subscribers.BaseSubscribersCardViewModel
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

private const val CARD_MAX_ITEMS = 5
private const val DETAIL_MAX_ITEMS = 100

@HiltViewModel
class SubscribersListViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseSubscribersCardViewModel<SubscribersListUiState>(
    selectedSiteRepository,
    accountStore,
    statsRepository,
    resourceProvider,
    SubscribersListUiState.Loading
) {
    override val loadingState = SubscribersListUiState.Loading

    override fun errorState(
        message: String,
        isAuthError: Boolean
    ) = SubscribersListUiState.Error(message, isAuthError)

    suspend fun getDetailData(): List<SubscriberListItem> {
        val siteId = getSiteId() ?: return emptyList()
        val result = statsRepository.fetchSubscribersList(
            siteId, DETAIL_MAX_ITEMS
        )
        return when (result) {
            is SubscribersListResult.Success ->
                result.subscribers.map {
                    SubscriberListItem(
                        displayName = it.displayName,
                        subscribedSince =
                            it.subscribedSince
                    )
                }
            is SubscribersListResult.Error -> emptyList()
        }
    }

    override suspend fun loadDataInternal(siteId: Long) {
        when (
            val result = statsRepository
                .fetchSubscribersList(
                    siteId, CARD_MAX_ITEMS
                )
        ) {
            is SubscribersListResult.Success -> {
                markLoadedSuccessfully()
                updateState(
                    SubscribersListUiState.Loaded(
                        items = result.subscribers
                            .take(CARD_MAX_ITEMS)
                            .map {
                                SubscriberListItem(
                                    displayName =
                                        it.displayName,
                                    subscribedSince =
                                        it.subscribedSince
                                )
                            }
                    )
                )
            }
            is SubscribersListResult.Error -> {
                updateState(
                    SubscribersListUiState.Error(
                        message = resourceProvider
                            .getString(
                                result.messageResId
                            ),
                        isAuthError = result.isAuthError
                    )
                )
            }
        }
    }
}
