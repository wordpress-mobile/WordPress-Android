package org.wordpress.android.ui.stats.refresh.lists.widget.today

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.StatsActivity
import org.wordpress.android.ui.stats.refresh.lists.widget.SITE_ID_KEY
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.getColorMode
import org.wordpress.android.ui.stats.refresh.utils.StatsLaunchedFrom
import org.wordpress.android.util.config.StatsTrafficSubscribersTabsFeatureConfig
import javax.inject.Inject

class TodayWidgetListProvider(val context: Context, intent: Intent) : RemoteViewsFactory {
    @Inject
    lateinit var viewModel: TodayWidgetListViewModel

    @Inject
    lateinit var widgetUpdater: TodayWidgetUpdater

    @Inject
    lateinit var trafficSubscribersTabFeatureConfig: StatsTrafficSubscribersTabsFeatureConfig

    private val colorMode: Color = intent.getColorMode()
    private val siteId: Int = intent.getIntExtra(SITE_ID_KEY, 0)
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreate() {
        viewModel.start(siteId, colorMode, appWidgetId)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun onDataSetChanged() {
        viewModel.onDataSetChanged { appWidgetId ->
            widgetUpdater.updateAppWidget(
                context,
                appWidgetId = appWidgetId
            )
        }
    }

    override fun hasStableIds(): Boolean = true

    override fun getViewTypeCount(): Int = 1

    override fun onDestroy() {
    }

    override fun getCount(): Int {
        return viewModel.data.size
    }

    override fun getItemId(position: Int): Long {
        return viewModel.data[position].key.hashCode().toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val uiModel = viewModel.data[position]
        val rv = RemoteViews(context.packageName, uiModel.layout)
        rv.setTextViewText(R.id.period, uiModel.key)
        rv.setTextViewText(R.id.value, uiModel.value)
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(WordPress.LOCAL_SITE_ID, uiModel.localSiteId)
        if (trafficSubscribersTabFeatureConfig.isEnabled()) {
            intent.putExtra(StatsActivity.ARG_DESIRED_TIMEFRAME, StatsTimeframe.TRAFFIC)
        } else {
            intent.putExtra(StatsActivity.ARG_DESIRED_TIMEFRAME, StatsTimeframe.INSIGHTS)
        }
        intent.putExtra(StatsActivity.ARG_LAUNCHED_FROM, StatsLaunchedFrom.WIDGET)
        rv.setOnClickFillInIntent(R.id.container, intent)
        return rv
    }
}
