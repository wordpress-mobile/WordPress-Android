package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.Color
import javax.inject.Inject

const val SHOW_CHANGE_VALUE_KEY = "show_change_value_key"
const val COLOR_MODE_KEY = "color_mode_key"
const val SITE_ID_KEY = "site_id_key"

class StatsViewsWidget : AppWidgetProvider() {
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        (context.applicationContext as WordPress).component().inject(this)
        for (appWidgetId in appWidgetIds) {
            val minWidth = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    appPrefsWrapper,
                    siteStore,
                    minWidth > 250
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        (context.applicationContext as WordPress).component().inject(this)
        for (appWidgetId in appWidgetIds) {
            appPrefsWrapper.removeAppWidgetColorModeId(appWidgetId)
            appPrefsWrapper.removeAppWidgetSiteId(appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if (context != null) {
            // See the dimensions and
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

            // Get min width and height.
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            (context.applicationContext as WordPress).component().inject(this)
            updateAppWidget(context, appWidgetManager, appWidgetId, appPrefsWrapper, siteStore, minWidth > 250)
        }
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            appPrefsWrapper: AppPrefsWrapper,
            siteStore: SiteStore,
            showChangeColumn: Boolean = true
        ) {
            drawList(appWidgetId, context, appPrefsWrapper, siteStore, appWidgetManager, showChangeColumn)
        }

        private fun drawList(
            appWidgetId: Int,
            context: Context,
            appPrefsWrapper: AppPrefsWrapper,
            siteStore: SiteStore,
            appWidgetManager: AppWidgetManager,
            showChangeColumn: Boolean
        ) {
            val layout = when(appPrefsWrapper.getAppWidgetColorModeId(appWidgetId)) {
                Color.DARK.ordinal -> R.layout.stats_views_widget_dark
                Color.LIGHT.ordinal -> R.layout.stats_views_widget_light
                else -> R.layout.stats_views_widget_light
            }
            val views = RemoteViews(context.packageName, layout)
            val siteIconUrl = siteStore.getSiteBySiteId(appPrefsWrapper.getAppWidgetSiteId(appWidgetId))?.iconUrl
            if (siteIconUrl != null) {
                val awt = AppWidgetTarget(context, id.widget_site_icon, views, appWidgetId)
                Glide.with(context)
                        .asBitmap()
                        .load(siteIconUrl)
                        .into(awt)

                views.setViewVisibility(id.widget_site_icon, View.VISIBLE)
            } else {
                views.setViewVisibility(id.widget_site_icon, View.GONE)
            }
            val svcIntent = Intent(context, WidgetService::class.java)
            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            svcIntent.putExtra(SHOW_CHANGE_VALUE_KEY, showChangeColumn)
            svcIntent.putExtra(COLOR_MODE_KEY, appPrefsWrapper.getAppWidgetColorModeId(appWidgetId))
            svcIntent.putExtra(SITE_ID_KEY, appPrefsWrapper.getAppWidgetSiteId(appWidgetId))
            svcIntent.data = Uri.parse(
                    svcIntent.toUri(Intent.URI_INTENT_SCHEME)
            )
            views.setRemoteAdapter(id.widget_list, svcIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
