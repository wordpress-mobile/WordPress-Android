package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEGATIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEUTRAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.POSITIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewMapper
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date
import javax.inject.Inject

class ViewsListProvider(val context: Context, intent: Intent) : RemoteViewsFactory {
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var visitsAndViewsStore: VisitsAndViewsStore
    @Inject lateinit var statsDateFormatter: StatsDateFormatter
    @Inject lateinit var overviewMapper: OverviewMapper
    @Inject lateinit var resourceProvider: ResourceProvider
    private var appWidgetId: Int = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var showChangeColumn: Boolean = intent.getBooleanExtra(SHOW_CHANGE_VALUE_KEY, true)
    private var colorModeId: Int = intent.getIntExtra(COLOR_MODE_KEY, Color.LIGHT.ordinal)
    private var siteId: Long = intent.getLongExtra(SITE_ID_KEY, 0L)

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreate() {
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun onDataSetChanged() {
        val site = siteStore.getSiteBySiteId(siteId)
        runBlocking {
            visitsAndViewsStore.fetchVisits(site, DAYS, LimitMode.Top(7), Date())
        }
        val visitsAndViewsModel = visitsAndViewsStore.getVisits(site, DAYS, LimitMode.Top(7), Date())
        val periods = visitsAndViewsModel?.dates ?: listOf()

        if (periods != listItemList) {
            listItemList.clear()
            listItemList.addAll(periods.asReversed())
        }
    }

    override fun hasStableIds(): Boolean = true

    override fun getViewTypeCount(): Int = 1

    override fun onDestroy() {
    }

    private val listItemList = mutableListOf<VisitsAndViewsModel.PeriodData>()

    override fun getCount(): Int {
        return listItemList.size
    }

    override fun getItemId(position: Int): Long {
        return listItemList[position].period.hashCode().toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val layout = when (colorModeId) {
            Color.DARK.ordinal -> layout.stats_views_widget_item_dark
            Color.LIGHT.ordinal -> layout.stats_views_widget_item_light
            else -> layout.stats_views_widget_item_light
        }
        val rv = RemoteViews(context.packageName, layout)
        val selectedItem = listItemList[position]
        val previousItem = listItemList.getOrNull(position + 1)
        val isCurrentDay = position == 0
        val uiModel = overviewMapper.buildTitle(selectedItem, previousItem, 0, isCurrentDay)

        val key = if (isCurrentDay) {
            resourceProvider.getString(R.string.stats_insights_today_stats)
        } else {
            statsDateFormatter.printDate(listItemList[position].period)
        }
        rv.setTextViewText(R.id.period, key)
        if (uiModel.state == NEUTRAL && showChangeColumn && uiModel.change != null) {
            rv.setTextViewText(R.id.neutral_change, uiModel.change)
            rv.setViewVisibility(R.id.neutral_change, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.neutral_change, View.GONE)
        }
        if (uiModel.state == POSITIVE && showChangeColumn && uiModel.change != null) {
            rv.setTextViewText(R.id.positive_change, uiModel.change)
            rv.setViewVisibility(R.id.positive_change, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.positive_change, View.GONE)
        }
        if (uiModel.state == NEGATIVE && showChangeColumn && uiModel.change != null) {
            rv.setTextViewText(R.id.negative_change, uiModel.change)
            rv.setViewVisibility(R.id.negative_change, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.negative_change, View.GONE)
        }
        if (showChangeColumn || uiModel.change == null) {
            rv.setViewVisibility(R.id.divider, View.GONE)
        } else {
            rv.setViewVisibility(R.id.divider, View.VISIBLE)
        }
        rv.setTextViewText(R.id.value, uiModel.value)
        return rv
    }
}
