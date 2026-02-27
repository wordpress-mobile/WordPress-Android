package org.wordpress.android.ui.newstats.subscribers.emails

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.EmailsStatsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.subscribers.BaseSubscribersCardViewModel
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

private const val CARD_MAX_ITEMS = 5
private const val DETAIL_MAX_ITEMS = 100

@HiltViewModel
class EmailsCardViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseSubscribersCardViewModel<EmailsCardUiState>(
    selectedSiteRepository,
    accountStore,
    statsRepository,
    resourceProvider,
    EmailsCardUiState.Loading
) {
    override val loadingState = EmailsCardUiState.Loading

    override fun errorState(
        message: String,
        isAuthError: Boolean
    ) = EmailsCardUiState.Error(message, isAuthError)

    suspend fun getDetailData(): List<EmailListItem> {
        val siteId = getSiteId() ?: return emptyList()
        val result = statsRepository.fetchEmailsSummary(
            siteId, DETAIL_MAX_ITEMS
        )
        return when (result) {
            is EmailsStatsResult.Success ->
                result.items.map {
                    EmailListItem(
                        title = it.title,
                        opens = it.opens,
                        clicks = it.clicks
                    )
                }
            is EmailsStatsResult.Error -> emptyList()
        }
    }

    override suspend fun loadDataInternal(siteId: Long) {
        when (
            val result = statsRepository
                .fetchEmailsSummary(
                    siteId, CARD_MAX_ITEMS
                )
        ) {
            is EmailsStatsResult.Success -> {
                markLoadedSuccessfully()
                updateState(
                    EmailsCardUiState.Loaded(
                        items = result.items
                            .take(CARD_MAX_ITEMS)
                            .map {
                                EmailListItem(
                                    title = it.title,
                                    opens = it.opens,
                                    clicks = it.clicks
                                )
                            }
                    )
                )
            }
            is EmailsStatsResult.Error -> {
                updateState(
                    EmailsCardUiState.Error(
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
