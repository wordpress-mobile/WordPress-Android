package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

class QuickStartCardViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val quickStartRepository: QuickStartRepository,
    private val quickStartStore: QuickStartStore,
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardsTracker: CardsTracker,
    private val quickStartTracker: QuickStartTracker,
    private val quickStartCardBuilder: QuickStartCardBuilder
) {
    private lateinit var scope: CoroutineScope

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.QuickStartCard?>()
    val uiModel: LiveData<MySiteCardAndItem.Card.QuickStartCard?> = _uiModel

    fun initialize(coroutineScope: CoroutineScope) {
        this.scope = coroutineScope
    }

    fun build(selectedSite: SiteModel) {
        scope.launch(bgDispatcher) {
            _isRefreshing.postValue(true)
            if (!quickStartUtilsWrapper.isQuickStartAvailableForTheSite(selectedSite)) {
                _uiModel.postValue(null)
                _isRefreshing.postValue(false)
                return@launch
            }

            quickStartRepository.resetTask()
            if (selectedSite.showOnFront == ShowOnFront.POSTS.value) {
                _isRefreshing.postValue(true)
            }
            postQuickStartCard(selectedSite)
        }
    }

    private fun postQuickStartCard(
        selectedSite: SiteModel
    ) {
        val siteLocalId = selectedSite.id
        val quickStartTaskTypes = quickStartRepository.getQuickStartTaskTypes()
        val categories =
            if (quickStartRepository.quickStartType
                    .isQuickStartInProgress(quickStartStore, siteLocalId.toLong())
            ) {
                quickStartTaskTypes.map { quickStartRepository.buildQuickStartCategory(siteLocalId, it) }
                    .filter { !isEmptyCategory(siteLocalId, it.taskType) }
            } else {
                listOf()
            }

        if (shouldShowQuickStartCard(categories, selectedSite)) {
            postState(categories)
        } else {
            postState(listOf())
        }
    }

    fun postState(categories: List<QuickStartRepository.QuickStartCategory>) {
        _isRefreshing.postValue(false)
        if(categories.isNotEmpty())
            _uiModel.postValue(buildQuickStartCard(
                QuickStartCardBuilderParams(
                    quickStartCategories = categories,
                    moreMenuClickParams = QuickStartCardBuilderParams.MoreMenuParams(
                        onMoreMenuClick = this::onQuickStartMoreMenuClick,
                        onHideThisMenuItemClick = this::onQuickStartHideThisMenuItemClick
                    ),
                    onQuickStartTaskTypeItemClick = this::onQuickStartTaskTypeItemClick
                ),
            ))
    }

    private fun buildQuickStartCard(params: QuickStartCardBuilderParams): MySiteCardAndItem.Card.QuickStartCard? {
        return params.quickStartCategories.takeIf { it.isNotEmpty() }?.let {
            quickStartCardBuilder.build(params)
        }
    }

    private fun onQuickStartMoreMenuClick(quickStartCardType: QuickStartCardType) =
        quickStartTracker.trackMoreMenuClicked(quickStartCardType)

    private fun onQuickStartHideThisMenuItemClick(quickStartCardType: QuickStartCardType) {
        quickStartTracker.trackMoreMenuItemClicked(quickStartCardType)
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            when (quickStartCardType) {
                QuickStartCardType.GET_TO_KNOW_THE_APP -> {
                    quickStartRepository.onHideShowGetToKnowTheAppCard(selectedSite.siteId)
                }

                QuickStartCardType.NEXT_STEPS -> {
                    quickStartRepository.onHideNextStepsCard(selectedSite.siteId)
                }
            }
            clearActiveQuickStartTask()
            _uiModel.postValue(null)
        }
    }

    private fun onQuickStartTaskTypeItemClick(type: QuickStartTaskType) {
        clearActiveQuickStartTask()
        cardsTracker.trackQuickStartCardItemClicked(type)
        _onNavigation.value = Event(
            SiteNavigationAction.OpenQuickStartFullScreenDialog(type,
                quickStartCardBuilder.getTitle(type))
        )
    }

    fun clearActiveQuickStartTask() {
        quickStartRepository.clearActiveTask()
    }

    private fun shouldShowQuickStartCard(
        categories: List<QuickStartRepository.QuickStartCategory>,
        selectedSite: SiteModel?
    ): Boolean {
        selectedSite?.let { site ->
            return if (categories.any { it.taskType == QuickStartTaskType.GET_TO_KNOW_APP }) {
                quickStartRepository.shouldShowGetToKnowTheAppCard(site.siteId)
            } else {
                quickStartRepository.shouldShowNextStepsCard(site.siteId)
            }
        }
        return false
    }

    private fun isEmptyCategory(
        siteLocalId: Int,
        taskType: QuickStartTaskType
    ): Boolean {
        val completedTasks = quickStartStore.getCompletedTasksByType(siteLocalId.toLong(), taskType)
        val unCompletedTasks = quickStartStore.getUncompletedTasksByType(siteLocalId.toLong(), taskType)
        return (completedTasks + unCompletedTasks).isEmpty()
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }

    fun trackShown(it: MySiteCardAndItem.Card.QuickStartCard) {
        quickStartTracker.trackShown(it.type)
    }

    fun resetShown() {
        quickStartTracker.resetShown()
    }
}
