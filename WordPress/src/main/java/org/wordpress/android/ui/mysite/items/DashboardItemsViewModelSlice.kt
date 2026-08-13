package org.wordpress.android.ui.mysite.items

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.cards.sotw2023.WpSotw2023NudgeCardViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackfeaturecard.JetpackFeatureCardViewModelSlice
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsViewModelSlice
import org.wordpress.android.util.merge
import javax.inject.Inject
import javax.inject.Named

class DashboardItemsViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val jetpackFeatureCardViewModelSlice: JetpackFeatureCardViewModelSlice,
    private val siteItemsViewModelSlice: SiteItemsViewModelSlice,
    private val sotw2023NudgeCardViewModelSlice: WpSotw2023NudgeCardViewModelSlice
) {
    private lateinit var scope: CoroutineScope

    private var job: Job? = null

    private var trackingJob : Job? = null

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
        sotw2023NudgeCardViewModelSlice.initialize(scope)
    }

    val onNavigation = merge(
        jetpackFeatureCardViewModelSlice.onNavigation,
        siteItemsViewModelSlice.onNavigation,
        sotw2023NudgeCardViewModelSlice.onNavigation
    )

    val uiModel: MutableLiveData<List<MySiteCardAndItem>> = merge(
        jetpackFeatureCardViewModelSlice.uiModel,
        siteItemsViewModelSlice.uiModel,
        sotw2023NudgeCardViewModelSlice.uiModel
    ) { jetpackFeatureCard, siteItems, sotw2023NudgeCard ->
        mergeUiModels(
            jetpackFeatureCard,
            siteItems,
            sotw2023NudgeCard
        )
    }.distinctUntilChanged() as MutableLiveData<List<MySiteCardAndItem>>

    val onSnackbarMessage = merge(
        siteItemsViewModelSlice.onSnackbarMessage,
    )

    private val _isRefreshing =  MutableLiveData<Boolean>()
    val isRefreshing = _isRefreshing.distinctUntilChanged()

    private fun mergeUiModels(
        jetpackFeatureCard: MySiteCardAndItem.Card.JetpackFeatureCard?,
        siteItems: List<MySiteCardAndItem>?,
        sotw2023NudgeCard: MySiteCardAndItem.Card.WpSotw2023NudgeCardModel?
    ): List<MySiteCardAndItem> {
        val dasbhboardSiteItems = mutableListOf<MySiteCardAndItem>()
        dasbhboardSiteItems.apply {
            sotw2023NudgeCard?.let { add(it) }
            siteItems?.let { addAll(siteItems) }
            jetpackFeatureCard?.let { add(0, it) }
        }.toList()
        if(dasbhboardSiteItems.isNotEmpty()) trackShown(dasbhboardSiteItems)
        return dasbhboardSiteItems
    }

    fun buildItems(site: SiteModel) {
        job?.cancel()
        job = scope.launch(bgDispatcher) {
            _isRefreshing.postValue(true)
            jetpackFeatureCardViewModelSlice.buildJetpackFeatureCard()
            siteItemsViewModelSlice.buildSiteItems(site)
            sotw2023NudgeCardViewModelSlice.buildCard()
            _isRefreshing.postValue(false)
        }
    }

    fun clearValue() {
        jetpackFeatureCardViewModelSlice.clearValue()
        siteItemsViewModelSlice.clearValue()
        sotw2023NudgeCardViewModelSlice.clearValue()
    }

    fun resetShownTracker() {
        trackingJob?.cancel()
        sotw2023NudgeCardViewModelSlice.resetShown()
        jetpackFeatureCardViewModelSlice.resetShown()
    }

    fun trackShown(dasbhboardSiteItems: MutableList<MySiteCardAndItem>) = with(dasbhboardSiteItems) {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            filterIsInstance<MySiteCardAndItem.Card.JetpackFeatureCard>().forEach {
                jetpackFeatureCardViewModelSlice.trackShown(it.type)
            }
            filterIsInstance<MySiteCardAndItem.Card.WpSotw2023NudgeCardModel>().forEach {
                sotw2023NudgeCardViewModelSlice.trackShown()
            }
        }
    }

    fun onCleared() {
        job?.cancel()
        trackingJob?.cancel()
        scope.cancel()
    }
}
