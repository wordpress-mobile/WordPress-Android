package org.wordpress.android.ui.newstats.subscribers.subscriberslist

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersListResult
import org.wordpress.android.ui.newstats.subscribers.BaseSubscribersCardViewModel
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

private const val CARD_MAX_ITEMS = 5

@HiltViewModel
class SubscribersListViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider,
    private val contextProvider: ContextProvider
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

    override suspend fun loadDataInternal(siteId: Long) {
        when (
            val result = statsRepository
                .fetchSubscribersList(
                    siteId, CARD_MAX_ITEMS
                )
        ) {
            is SubscribersListResult.Success -> {
                markLoadedSuccessfully()
                val resources = contextProvider
                    .getContext().resources
                updateState(
                    SubscribersListUiState.Loaded(
                        items = result.subscribers
                            .take(CARD_MAX_ITEMS)
                            .map {
                                SubscriberListItem(
                                    displayName =
                                        it.displayName,
                                    subscribedSince =
                                        it.subscribedSince,
                                    formattedDate =
                                        formatSubscriberDate(
                                            it.subscribedSince,
                                            resources
                                        )
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
