package org.wordpress.android.ui.mysite.cards.dashboard.pages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PagesCardViewModelSlice @Inject constructor(
    private val cardsTracker: CardsTracker,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val pagesCardBuilder: PagesCardBuilder
) {
    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.PagesCard?>()
    val uiModel: LiveData<MySiteCardAndItem.Card.PagesCard?> = _uiModel

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    fun buildCard(pagesCardModel: PagesCardModel?) {
        _uiModel.postValue(
            pagesCardBuilder.build(getPagesCardBuilderParams(pagesCardModel))
        )
    }

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
        cardsTracker.trackCardMoreMenuClicked(CardsTracker.Type.PAGES.label)
    }

    private fun onAllPagesMenuItemClick() {
        cardsTracker.trackCardMoreMenuItemClicked(CardsTracker.Type.PAGES.label, PagesMenuItemType.ALL_PAGES.label)
        _onNavigation.value = Event(
            SiteNavigationAction
                .OpenPages(requireNotNull(selectedSiteRepository.getSelectedSite()))
        )
    }

    private fun onPagesCardHideThisCardClick() {
        cardsTracker.trackCardMoreMenuItemClicked(CardsTracker.Type.PAGES.label, PagesMenuItemType.HIDE_THIS.label)
        appPrefsWrapper.setShouldHidePagesDashboardCard(selectedSiteRepository.getSelectedSite()!!.siteId, true)
        _uiModel.value = null
    }

    private fun onPagesItemClick(params: PagesCardBuilderParams.PagesItemClickParams) {
        cardsTracker.trackCardItemClicked(CardsTracker.Type.PAGES.label, params.pagesCardType.toSubtypeValue().label)
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
        cardsTracker.trackCardFooterLinkClicked(
            CardsTracker.Type.PAGES.label,
            CardsTracker.PagesSubType.CREATE_PAGE.label
        )
        _onNavigation.value =
            Event(
                SiteNavigationAction.TriggerCreatePageFlow(
                    requireNotNull(selectedSiteRepository.getSelectedSite())
                )
            )
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }
}

enum class PagesMenuItemType(val label: String) {
    ALL_PAGES("all_pages"),
    HIDE_THIS("hide_this")
}

fun PagesCardContentType.toSubtypeValue(): CardsTracker.PagesSubType {
    return when (this) {
        PagesCardContentType.DRAFT -> CardsTracker.PagesSubType.DRAFT
        PagesCardContentType.PUBLISH -> CardsTracker.PagesSubType.PUBLISHED
        PagesCardContentType.SCHEDULED -> CardsTracker.PagesSubType.SCHEDULED
    }
}
