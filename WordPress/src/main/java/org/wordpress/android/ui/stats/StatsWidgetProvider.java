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

import com.android.volley.VolleyError;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stats.exceptions.StatsError;
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
            showMessage(context, appWidgetManager, context.getString(R.string.stats_widget_error_login));
            return null;
        }

        final long primaryBlogId = AccountHelper.getDefaultAccount().getPrimaryBlogId();
        final int localBlogID = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId((int) primaryBlogId);
        if (primaryBlogId == 0 || localBlogID == 0) {
            AppLog.w(AppLog.T.STATS, "No primary blog found! Widget update skipped!");
            showMessage(context, appWidgetManager, context.getString(R.string.stats_widget_error_generic));
            return null;
        }

        final Blog primaryBlog = WordPress.getBlog(localBlogID);
        if (primaryBlog == null) {
            showMessage(context, appWidgetManager, context.getString(R.string.stats_widget_error_generic));
            AppLog.e(AppLog.T.STATS, "Current blog is null. This should never happen here.");
            return null;
        }

        // Check credentials for jetpack blogs first
        if (!primaryBlog.isDotcomFlag()
                && !primaryBlog.hasValidJetpackCredentials() && !AccountHelper.isSignedInWordPressDotCom()) {
            showMessage(context, appWidgetManager, "Your Jetpack blog is not connected properly with WordPress.com into the app.");
            AppLog.w(AppLog.T.STATS, context.getString(R.string.stats_widget_error_login));
            return null;
        }

        return primaryBlog;
    }

    private static void showMessage(Context context, AppWidgetManager appWidgetManager, String message){
        // Get all ids
        ComponentName thisWidget = new ComponentName(context, StatsWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stats_widget_layout);
            remoteViews.setViewVisibility(R.id.stats_widget_error_container, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.stats_widget_values_container, View.GONE);
            remoteViews.setTextViewText(R.id.stats_widget_error_text, message);

            Intent intent = new Intent(context, WPMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.stats_widget_outer_container, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private static void showStatsData(Context context, AppWidgetManager appWidgetManager, Blog blog, JSONObject data) {
        // Get all ids
        ComponentName thisWidget = new ComponentName(context, StatsWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        String name =  StringEscapeUtils.unescapeHtml(blog.getNameOrHostUrl()) + " " +
                context.getString(R.string.stats_insights_today);

        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stats_widget_layout);
            remoteViews.setTextViewText(R.id.blog_title, name);

            remoteViews.setViewVisibility(R.id.stats_widget_error_container, View.GONE);
            remoteViews.setViewVisibility(R.id.stats_widget_values_container, View.VISIBLE);
            remoteViews.setTextViewText(R.id.stats_widget_views, data.optString("views", "0"));
            remoteViews.setTextViewText(R.id.stats_widget_visitors, data.optString("visitors", "0"));
            remoteViews.setTextViewText(R.id.stats_widget_comments, data.optString("comments", "0"));
            remoteViews.setTextViewText(R.id.stats_widget_likes, data.optString("likes", "0"));

            Intent intent = new Intent(context, StatsActivity.class);
            intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, blog.getLocalTableBlogId());
            intent.putExtra(StatsActivity.ARG_LAUNCHED_FROM, StatsActivity.StatsLaunchedFrom.STATS_WIDGET);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.stats_widget_outer_container, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private static void updateWidgetsOnError(Context context, AppWidgetManager appWidgetManager) {
        Blog primaryBlog = checkLoggedAndPrimaryBlog(context, appWidgetManager);
        if (primaryBlog == null) {
            return;
        }
        final String currentDate = StatsUtils.getCurrentDateTZ(primaryBlog.getLocalTableBlogId());

        JSONObject prevData = getCachedStatsData(primaryBlog, currentDate);
        if (prevData != null) {
            showStatsData(context, appWidgetManager, primaryBlog, prevData);
        } else {
            showMessage(context, appWidgetManager, context.getString(R.string.stats_widget_error_generic));
        }
    }

    // This is called by the Stats service in case of error
    public static void updateWidgets(Context context, VolleyError error) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (error == null) {
            AppLog.e(AppLog.T.STATS, "Widget received a VolleyError that is null!");
            return;
        }

        // If it's an auth error, show it in the widget UI
        if (error instanceof com.android.volley.AuthFailureError) {
            showMessage(context, appWidgetManager, context.getString(R.string.stats_widget_error_login));
            return;
        }

        updateWidgetsOnError(context, appWidgetManager);
    }

    // This is called by the Stats service in case of error
    public static void updateWidgets(Context context, StatsError error) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (error == null) {
            AppLog.e(AppLog.T.STATS, "Widget received a StatsError that is null!");
            return;
        }

        updateWidgetsOnError(context, appWidgetManager);
    }

    public static void updateWidgetsOnLogout(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        AppLog.d(AppLog.T.STATS, "updateWidgetsOnLogout called");

        checkLoggedAndPrimaryBlog(context, appWidgetManager);
    }


    public static void updateWidgetsOnLogin(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        AppLog.d(AppLog.T.STATS, "updateWidgetsOnLogin called");

        // empty string in pref.
        showMessage(context, appWidgetManager,
                context.getString(R.string.stats_widget_loading_data));
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

        try {
            String currentDate = StatsUtils.getCurrentDateTZ(primaryBlog.getLocalTableBlogId());
            JSONObject newData = new JSONObject();
            newData.put("blog_id", data.getBlogID());
            newData.put("date", currentDate);
            newData.put("views", data.getViews());
            newData.put("visitors", data.getVisitors());
            newData.put("comments", data.getComments());
            newData.put("likes", data.getLikes());
            AppPrefs.setStatsWidgetData(newData.toString());

            showStatsData(context, appWidgetManager, primaryBlog, newData);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }

    // This is called to update the App Widget at intervals defined by the updatePeriodMillis attribute in the AppWidgetProviderInfo.
    // Also called at booting time!
    // This method is also called when the user adds the App Widget (except when there is a widget config activity).
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        AppLog.d(AppLog.T.STATS, "onUpdate called");

        Blog primaryBlog = checkLoggedAndPrimaryBlog(context, appWidgetManager);
        if (primaryBlog == null) {
            return;
        }

        String currentDate = StatsUtils.getCurrentDateTZ(primaryBlog.getLocalTableBlogId());
        // Update the widget UI with previous Stats data, if available and fresh, or show the loading text.
        // If it was a prev error, the error data was not stored in pref, so the loading text is shown.
        // empty string in pref.
       JSONObject prevData = getCachedStatsData(primaryBlog, currentDate);
        if (prevData != null) {
            showStatsData(context, appWidgetManager, primaryBlog, prevData);
        } else {
            // empty string in pref.
            showMessage(context, appWidgetManager,
                    context.getString(R.string.stats_widget_loading_data));
        }

        enqueueStatsRequestForBlog(context, String.valueOf(primaryBlog.getRemoteBlogId()), currentDate);
    }

    @Nullable
    private static JSONObject getCachedStatsData(Blog primaryBlog, String currentDate) {
        String prevWidgetDataString = AppPrefs.getStatsWidgetData();
        if (!StringUtils.isEmpty(prevWidgetDataString)) {
            // we have prev data. Make sure it's fresh.
            try {
                JSONObject prevData = new JSONObject(prevWidgetDataString);
                if (!prevData.has("date") || !prevData.getString("date").equals(currentDate)) {
                    return null;
                }

                // make sure data is about the primary blog id. Same check is in the service.
                if (!prevData.has("blog_id")  ||
                        Integer.parseInt(prevData.getString("blog_id")) != primaryBlog.getRemoteBlogId()) {
                    return null;
                }

                return prevData;
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
        }
        return null;
    }

    /**
     *  This is called when an instance the App Widget is created for the first time.
     *  For example, if the user adds two instances of your App Widget, this is only called the first time.
     */
    @Override
    public void onEnabled(Context context) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_WIDGET_ADDED);
        AnalyticsTracker.flush();
    }

    /**
     * This is called when the last instance of your App Widget is deleted from the App Widget host.
     * This is where you should clean up any work done in onEnabled(Context), such as delete a temporary database.
     * @param context The Context in which this receiver is running.
     */
    @Override
    public void onDisabled(Context context) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_WIDGET_REMOVED);
        AnalyticsTracker.flush();
    }

    /**
     * This is called from the StatsService and from ApiHelper.RefreshBlogContentTask to check there is
     * at least 1 Stats Widget installed for the current blog.
     */
    public static boolean shouldUpdateWidgetForBlog(Context context, String blogID) {
        // Check if there are widgets installed on the device
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, StatsWidgetProvider.class);
        if (appWidgetManager.getAppWidgetIds(thisWidget).length == 0) {
            return false;
        }

        if (!AccountHelper.isSignedInWordPressDotCom()) {
            AppLog.w(AppLog.T.STATS, "Not signed to WordPress.com.");
            return false;
        }

        long parsedBlogID = Long.parseLong(blogID);
        final long primaryBlogId = AccountHelper.getDefaultAccount().getPrimaryBlogId();
        if (parsedBlogID != primaryBlogId) {
            return false;
        }

        return true;
    }

    public static void enqueueStatsRequestForBlog(Context context, String blogID, String date) {
        // start service to get stats
        Intent intent = new Intent(context, StatsService.class);
        intent.putExtra(StatsService.ARG_BLOG_ID, blogID);
        intent.putExtra(StatsService.ARG_PERIOD, StatsTimeframe.DAY);
        intent.putExtra(StatsService.ARG_DATE, date);
        intent.putExtra(StatsService.ARG_SECTION, new int[]{StatsService.StatsEndpointsEnum.VISITS.ordinal()});
        context.startService(intent);
    }
}
