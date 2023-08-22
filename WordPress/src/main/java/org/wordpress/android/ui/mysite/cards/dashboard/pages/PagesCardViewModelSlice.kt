package org.wordpress.android.ui.mysite.cards.dashboard.pages

import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PagesCardViewModelSlice @Inject constructor(
    private val cardsTracker: CardsTracker,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    fun getPagesCardBuilderParams(pagesCardModel: PagesCardModel?): PagesCardBuilderParams {
        return PagesCardBuilderParams(
            pageCard = pagesCardModel,
            onPagesItemClick = this::onPagesItemClick,
            onFooterLinkClick = this::onPagesCardFooterLinkClick,
            moreMenuClickParams = PagesCardBuilderParams.MoreMenuParams(
                onMoreMenuClick = this::onPagesCardMoreMenuClick,
                onHideThisCardItemClick = this::onPagesCardHideThisCardClick,
                onAllPagesItemClick = this::onAllPagesMenuItemClick
            )
        )
    }

    private fun onPagesCardMoreMenuClick() {
        cardsTracker.trackPagesCardMoreMenuClicked()
    }

    private fun onAllPagesMenuItemClick() {
        // todo implement the tracking for navigation to all pages
        _onNavigation.value = Event(
            SiteNavigationAction
                .OpenPages(requireNotNull(selectedSiteRepository.getSelectedSite()))
        )
    }

    private fun onPagesCardHideThisCardClick() {
        // todo implement the logic to hide the card and add tracking logic
    }

    private fun onPagesItemClick(params: PagesCardBuilderParams.PagesItemClickParams) {
        cardsTracker.trackPagesItemClicked(params.pagesCardType)
        _onNavigation.value = Event(getNavigationActionForPagesItem(params.pagesCardType, params.pageId))
    }

    private fun getNavigationActionForPagesItem(
        pagesCardType: PagesCardContentType,
        pageId: Int
    ): SiteNavigationAction {
        return when (pagesCardType) {
            PagesCardContentType.SCHEDULED -> {
                SiteNavigationAction.OpenPagesScheduledTab(
                    requireNotNull(selectedSiteRepository.getSelectedSite()),
                    pageId
                )
            }

            PagesCardContentType.DRAFT -> {
                SiteNavigationAction.OpenPagesDraftsTab(
                    requireNotNull(selectedSiteRepository.getSelectedSite()),
                    pageId
                )
            }

            PagesCardContentType.PUBLISH -> {
                SiteNavigationAction.OpenPages(requireNotNull(selectedSiteRepository.getSelectedSite()))
            }
        }
    }

    private fun onPagesCardFooterLinkClick() {
        cardsTracker.trackPagesCardFooterClicked()
        _onNavigation.value =
            Event(
                SiteNavigationAction.TriggerCreatePageFlow(
                    requireNotNull(selectedSiteRepository.getSelectedSite())
                )
            )
    }
}
