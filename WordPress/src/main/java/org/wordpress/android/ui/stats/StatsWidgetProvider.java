package org.wordpress.android.ui.stats;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RemoteViews;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;

public class StatsWidgetProvider extends AppWidgetProvider {

    // Check if the user is logged to wpcom and the primary blog is correctly available within the app.
    // Returns the primary blog or null if not available.
    // Show the correct error in the widgets in case there is some kind of error.
    @Nullable
    private static Blog checkLoggedAndPrimaryBlog(Context context, AppWidgetManager appWidgetManager) {
        if (!AccountHelper.isSignedInWordPressDotCom()) {
            AppLog.w(AppLog.T.STATS, "Not signed to WordPress.com. Widget update skipped!");
            showError(context, appWidgetManager, context.getString(R.string.stats_widget_error_login));
            return null;
        }

        final long primaryBlogId = AccountHelper.getDefaultAccount().getPrimaryBlogId();
        final int localBlogID = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId((int) primaryBlogId);
        if (primaryBlogId == 0 || localBlogID == 0) {
            AppLog.w(AppLog.T.STATS, "No primary blog found! Widget update skipped!");
            showError(context, appWidgetManager, "No primary blog found!");
            return null;
        }

        final Blog primaryBlog = WordPress.getBlog(localBlogID);
        if (primaryBlog == null) {
            showError(context, appWidgetManager, "No primary blog found!");
            AppLog.e(AppLog.T.STATS, "Current blog is null. This should never happen here.");
            return null;
        }

        // Check credentials for jetpack blogs first
        if (!primaryBlog.isDotcomFlag()
                && !primaryBlog.hasValidJetpackCredentials() && !AccountHelper.isSignedInWordPressDotCom()) {
            showError(context, appWidgetManager, "Your Jetpack blog is not connected properly with WordPress.com into the app.");
            AppLog.w(AppLog.T.STATS, context.getString(R.string.stats_widget_error_login));
            return null;
        }

        return primaryBlog;
    }

    // This is called by the Stats service to keep widgets updated
    public static void updateWidgets(Context context, VisitModel data) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        AppLog.d(AppLog.T.STATS, "updateWidgets called");

        Blog primaryBlog = checkLoggedAndPrimaryBlog(context, appWidgetManager);
        if (primaryBlog == null) {
            return;
        }

        // make sure data is about the primary blog id. Same check is in the service.
        if (Integer.parseInt(data.getBlogID()) != primaryBlog.getRemoteBlogId()) {
            AppLog.w(AppLog.T.STATS, "Widget received stats data that doesn't belong to the primary blog!!!");
            return;
        }

        updateUI(context, appWidgetManager, primaryBlog, String.valueOf(data.getViews()),
                String.valueOf(data.getVisitors()));

        // Store the fresh data in preferences
        String currentDate = StatsUtils.getCurrentDateTZ(primaryBlog.getLocalTableBlogId());
        String prefString = data.getBlogID() + ";" + currentDate + ";" + data.getViews() + ";" + data.getVisitors() +
                ";" + data.getComments() + ";" + data.getLikes();
        AppPrefs.setStatsWidgetData(prefString);
    }

    private static void showError(Context context, AppWidgetManager appWidgetManager, String errorMessage){
        // Get all ids
        ComponentName thisWidget = new ComponentName(context, StatsWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stats_widget_layout);
            remoteViews.setViewVisibility(R.id.stats_widget_error_container, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.stats_widget_values_container, View.GONE);
            remoteViews.setTextViewText(R.id.stats_widget_error_text, errorMessage);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private static void updateUI(Context context, AppWidgetManager appWidgetManager, Blog blog, String views, String visitors) {
        // Get all ids
        ComponentName thisWidget = new ComponentName(context, StatsWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        String name =  StringEscapeUtils.unescapeHtml(blog.getNameOrHostUrl()) + " " +
                context.getString(R.string.stats_insights_today);

        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stats_widget_layout);
            remoteViews.setTextViewText(R.id.blog_title, name);
            if (views == null) {
                remoteViews.setViewVisibility(R.id.stats_widget_error_container, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.stats_widget_values_container, View.GONE);
                remoteViews.setTextViewText(R.id.stats_widget_error_text, context.getString(R.string.stats_widget_loading_data));
            } else {
                remoteViews.setViewVisibility(R.id.stats_widget_error_container, View.GONE);
                remoteViews.setViewVisibility(R.id.stats_widget_values_container, View.VISIBLE);
                remoteViews.setTextViewText(R.id.stats_widget_views, views);
                remoteViews.setTextViewText(R.id.stats_widget_visitors, visitors);
            }

            Intent intent = new Intent(context, StatsActivity.class);
            intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, blog.getLocalTableBlogId());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            remoteViews.setOnClickPendingIntent(R.id.stats_widget_outer_container, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // This is called to update the App Widget at intervals defined by the updatePeriodMillis attribute in the AppWidgetProviderInfo
        // This method is also called when the user adds the App Widget (except when there is a widget config activity).
        AppLog.i(AppLog.T.STATS, "onUpdate called");

        Blog primaryBlog = checkLoggedAndPrimaryBlog(context, appWidgetManager);
        if (primaryBlog == null) {
            return;
        }

        // Read previous data from preferences if available
        String prevWidgetDataString = AppPrefs.getStatsWidgetData();
        String views = null;
        String visitors = null;
        if (!StringUtils.isEmpty(prevWidgetDataString)) {
            String[] prevWidgetData = prevWidgetDataString.split(";");
            if (prevWidgetData.length == 6) {
                String currentDate = StatsUtils.getCurrentDateTZ(primaryBlog.getLocalTableBlogId());
                // same blogID and Today
                if (Integer.parseInt(prevWidgetData[0]) == primaryBlog.getRemoteBlogId()
                    && currentDate.equals(prevWidgetData[1])) {
                    // data is fresh
                    views = prevWidgetData[2];
                    visitors = prevWidgetData[3];
                }
            }
        }

        updateUI(context, appWidgetManager, primaryBlog, views, visitors);

        // start service to get stats
        Intent intent = new Intent(context, StatsService.class);
        intent.putExtra(StatsService.ARG_BLOG_ID, String.valueOf(primaryBlog.getRemoteBlogId()));
        intent.putExtra(StatsService.ARG_PERIOD, StatsTimeframe.DAY);
        intent.putExtra(StatsService.ARG_DATE, StatsUtils.getCurrentDateTZ(primaryBlog.getLocalTableBlogId()));
        intent.putExtra(StatsService.ARG_SECTION, new int[]{StatsService.StatsEndpointsEnum.VISITS.ordinal()});
        context.startService(intent);
    }
}
