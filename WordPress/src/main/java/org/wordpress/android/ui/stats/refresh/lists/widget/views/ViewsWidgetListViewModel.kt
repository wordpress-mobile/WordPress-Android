package org.wordpress.android.ui.stats.refresh.lists.widget.views

import androidx.annotation.LayoutRes
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEGATIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEUTRAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.POSITIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OVERVIEW_ITEMS_TO_LOAD
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewMapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

const val LIST_ITEM_COUNT = 7

class ViewsWidgetListViewModel
@Inject constructor(
    private val siteStore: SiteStore,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val overviewMapper: OverviewMapper,
    private val resourceProvider: ResourceProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    private var siteId: Int? = null
    private var colorMode: Color = Color.LIGHT
    private var isWideView: Boolean = true
    private var appWidgetId: Int? = null
    private val mutableData = mutableListOf<ListItemUiModel>()
    val data: List<ListItemUiModel> = mutableData
    fun start(siteId: Int, colorMode: Color, isWideView: Boolean, appWidgetId: Int) {
        this.siteId = siteId
        this.colorMode = colorMode
        this.isWideView = isWideView
        this.appWidgetId = appWidgetId
    }

    fun onDataSetChanged(onError: (appWidgetId: Int) -> Unit) {
        siteId?.apply {
            val site = siteStore.getSiteByLocalId(this)
            if (site != null) {
                runBlocking {
                    visitsAndViewsStore.fetchVisits(site, DAYS, Top(OVERVIEW_ITEMS_TO_LOAD))
                }
                val visitsAndViewsModel = visitsAndViewsStore.getVisits(
                    site,
                    DAYS,
                    LimitMode.All
                )
                val periods = visitsAndViewsModel?.dates?.asReversed() ?: listOf()
                val uiModels = periods.mapIndexed { index, periodData ->
                    buildListItemUiModel(index, periodData, periods, site.id)
                }.take(LIST_ITEM_COUNT)
                if (uiModels != data) {
                    mutableData.clear()
                    mutableData.addAll(uiModels)
                    appWidgetId?.let {
                        appPrefsWrapper.setAppWidgetHasData(true, it)
                    }
                }
            } else {
                appWidgetId?.let { nonNullAppWidgetId ->
                    onError(nonNullAppWidgetId)
                }
            }
        }
    }

    private fun buildListItemUiModel(
        position: Int,
        selectedItem: PeriodData,
        periods: List<PeriodData>,
        localSiteId: Int
    ): ListItemUiModel {
        val layout = when (colorMode) {
            Color.DARK -> R.layout.stats_views_widget_item_dark
            Color.LIGHT -> R.layout.stats_views_widget_item_light
        }
        val previousItem = periods.getOrNull(position + 1)
        val isCurrentDay = position == 0
        val startValue = if (isWideView) MILLION else ONE_THOUSAND
        val uiModel = overviewMapper.buildTitle(selectedItem, previousItem, 0, isCurrentDay, startValue)

        val key = if (isCurrentDay) {
            resourceProvider.getString(R.string.stats_insights_today_stats)
        } else {
            statsDateFormatter.printDate(periods[position].period)
        }
        val isPositiveChangeVisible = uiModel.state == POSITIVE && isWideView && !uiModel.change.isNullOrEmpty()
        val isNegativeChangeVisible = uiModel.state == NEGATIVE && isWideView && !uiModel.change.isNullOrEmpty()
        val isNeutralChangeVisible = uiModel.state == NEUTRAL && isWideView && !uiModel.change.isNullOrEmpty()
        return ListItemUiModel(
            layout,
            key,
            uiModel.value,
            isPositiveChangeVisible,
            isNegativeChangeVisible,
            isNeutralChangeVisible,
            uiModel.change,
            selectedItem.period,
            localSiteId
        )
    }

    data class ListItemUiModel(
        @LayoutRes val layout: Int,
        val key: String,
        val value: String,
        val isPositiveChangeVisible: Boolean,
        val isNegativeChangeVisible: Boolean,
        val isNeutralChangeVisible: Boolean,
        val change: String? = null,
        val period: String,
        val localSiteId: Int
    )
}
