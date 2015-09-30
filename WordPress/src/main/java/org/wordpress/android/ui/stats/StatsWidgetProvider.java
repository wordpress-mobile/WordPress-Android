package org.wordpress.android.ui.stats;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.android.volley.VolleyError;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
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

import java.util.ArrayList;

public class StatsWidgetProvider extends AppWidgetProvider {

    private static void showMessage(Context context, int[] allWidgets, String message){
        if (allWidgets.length == 0){
            return;
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        for (int widgetId : allWidgets) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stats_widget_layout);
            int remoteBlogID = getRemoteBlogIDFromWidgetID(widgetId);
            int localId = StatsUtils.getLocalBlogIdFromRemoteBlogId(remoteBlogID);
            Blog blog = WordPress.getBlog(localId);
            String name;
            if (blog != null) {
                name =  StringEscapeUtils.unescapeHtml(blog.getNameOrHostUrl()) + " " +
                        context.getString(R.string.stats_insights_today);
            } else {
                name = context.getString(R.string.stats_widget_name);
            }
            remoteViews.setTextViewText(R.id.blog_title, name);

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

    private static void showStatsData(Context context, int[] allWidgets, Blog blog, JSONObject data) {
        if (allWidgets.length == 0){
            return;
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        String name =  StringEscapeUtils.unescapeHtml(blog.getNameOrHostUrl()) + " " +
                context.getString(R.string.stats_insights_today);

        for (int widgetId : allWidgets) {
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
            intent.putExtra(StatsActivity.ARG_DESIDERED_TIMEFRAME, StatsTimeframe.DAY);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, blog.getLocalTableBlogId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.stats_widget_outer_container, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private static void updateWidgetsOnError(Context context, int remoteBlogID) {
        int[] widgetIDs = getWidgetIDsFromRemoteBlogID(remoteBlogID);
        if (widgetIDs.length == 0){
            return;
        }

        int localId = StatsUtils.getLocalBlogIdFromRemoteBlogId(remoteBlogID);
        Blog blog = WordPress.getBlog(localId);
        if (blog == null) {
            AppLog.e(AppLog.T.STATS, "No blog found in the db!");
            return;
        }

        showMessage(context, widgetIDs, context.getString(R.string.stats_widget_error_generic));
    }

    public static void updateWidgetsOnLogout(Context context) {
        refreshAllWidgets(context);
    }

    public static void updateWidgetsOnLogin(Context context) {
        refreshAllWidgets(context);
    }

    // This is called by the Stats service in case of error
    public static void updateWidgets(Context context, int remoteBlogID, VolleyError error) {
        if (error == null) {
            AppLog.e(AppLog.T.STATS, "Widget received a VolleyError that is null!");
            return;
        }

        // If it's an auth error, show it in the widget UI
        if (error instanceof com.android.volley.AuthFailureError) {
            int[] widgetIDs = getWidgetIDsFromRemoteBlogID(remoteBlogID);
            if (widgetIDs.length == 0){
                return;
            }
            showMessage(context, widgetIDs, context.getString(R.string.stats_widget_error_login));
            return;
        }

        updateWidgetsOnError(context, remoteBlogID);
    }

    // This is called by the Stats service in case of error
    public static void updateWidgets(Context context, int remoteBlogID, StatsError error) {
        if (error == null) {
            AppLog.e(AppLog.T.STATS, "Widget received a StatsError that is null!");
            return;
        }

        updateWidgetsOnError(context, remoteBlogID);
    }

    // This is called by the Stats service to keep widgets updated
    public static void updateWidgets(Context context, int remoteBlogID, VisitModel data) {
        AppLog.d(AppLog.T.STATS, "updateWidgets called for the blogID " + remoteBlogID);

        int[] widgetIDs = getWidgetIDsFromRemoteBlogID(remoteBlogID);
        if (widgetIDs.length == 0){
            return;
        }
        int localId = StatsUtils.getLocalBlogIdFromRemoteBlogId(remoteBlogID);
        Blog blog = WordPress.getBlog(localId);
        if (blog == null) {
            AppLog.e(AppLog.T.STATS, "No blog found in the db!");
            return;
        }
        try {
            String currentDate = StatsUtils.getCurrentDateTZ(blog.getLocalTableBlogId());
            JSONObject newData = new JSONObject();
            newData.put("blog_id", data.getBlogID());
            newData.put("date", currentDate);
            newData.put("views", data.getViews());
            newData.put("visitors", data.getVisitors());
            newData.put("comments", data.getComments());
            newData.put("likes", data.getLikes());
            showStatsData(context, widgetIDs, blog, newData);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }

    // This is called to update the App Widget at intervals defined by the updatePeriodMillis attribute in the AppWidgetProviderInfo.
    // Also called at booting time!
    // This method is NOT called when the user adds the App Widget.
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        AppLog.d(AppLog.T.STATS, "onUpdate called");
        refreshWidgets(context, appWidgetIds);
    }

    /**
     *  This is called when an instance the App Widget is created for the first time.
     *  For example, if the user adds two instances of your App Widget, this is only called the first time.
     */
    @Override
    public void onEnabled(Context context) {
        AppPrefs.resetStatsWidgets();
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
        AppPrefs.resetStatsWidgets();
    }

    public static void enqueueStatsRequestForBlog(Context context, String remoteBlogID, String date) {
        // start service to get stats
        Intent intent = new Intent(context, StatsService.class);
        intent.putExtra(StatsService.ARG_BLOG_ID, remoteBlogID);
        intent.putExtra(StatsService.ARG_PERIOD, StatsTimeframe.DAY);
        intent.putExtra(StatsService.ARG_DATE, date);
        intent.putExtra(StatsService.ARG_SECTION, new int[]{StatsService.StatsEndpointsEnum.VISITS.ordinal()});
        context.startService(intent);
    }


    public static synchronized boolean isBlogDisplayedInWidget(int remoteBlogID) {
        String prevWidgetKeysString = AppPrefs.getStatsWidgetsKeys();
        if (StringUtils.isEmpty(prevWidgetKeysString)) {
            return false;
        }
        try {
            JSONObject prevKeys = new JSONObject(prevWidgetKeysString);
            JSONArray allKeys = prevKeys.names();
            for (int i=0; i < allKeys.length(); i ++) {
                String currentKey = allKeys.getString(i);
                int currentBlogID = prevKeys.getInt(currentKey);
                if (currentBlogID == remoteBlogID) {
                    return true;
                }
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
        return false;
    }

    private static synchronized int[] getWidgetIDsFromRemoteBlogID(int remoteBlogID) {
        String prevWidgetKeysString = AppPrefs.getStatsWidgetsKeys();
        if (StringUtils.isEmpty(prevWidgetKeysString)) {
            return new int[0];
        }
        ArrayList<Integer> widgetIDs = new ArrayList<>();

        try {
            JSONObject prevKeys = new JSONObject(prevWidgetKeysString);
            JSONArray allKeys = prevKeys.names();
            for (int i=0; i < allKeys.length(); i ++) {
                String currentKey = allKeys.getString(i);
                int currentBlogID = prevKeys.getInt(currentKey);
                if (currentBlogID == remoteBlogID) {
                    AppLog.d(AppLog.T.STATS, "The blog with remoteID " + remoteBlogID + " is displayed in a widget");
                    widgetIDs.add(Integer.parseInt(currentKey));
                }
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
        return ArrayUtils.toPrimitive(widgetIDs.toArray(new Integer[widgetIDs.size()]));
    }

    private static synchronized int getRemoteBlogIDFromWidgetID(int widgetID) {
        String prevWidgetKeysString = AppPrefs.getStatsWidgetsKeys();
        if (StringUtils.isEmpty(prevWidgetKeysString)) {
            return 0;
        }
        try {
            JSONObject prevKeys = new JSONObject(prevWidgetKeysString);
            return prevKeys.optInt(String.valueOf(widgetID), 0);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
        return 0;
    }

    // This is called by the Widget config activity at the end if the process
     static void setupNewWidget(Context context, int widgetID, int localBlogID) {
        AppLog.d(AppLog.T.STATS, "setupNewWidget called");

        Blog blog = WordPress.getBlog(localBlogID);
        if (blog == null) {
            AppLog.e(AppLog.T.STATS, "setupNewWidget: No blog found in the db!");
            return;
        }

        // Store the widgetID+blogID into preferences
        String prevWidgetKeysString = AppPrefs.getStatsWidgetsKeys();
        JSONObject prevKeys = null;
        if (!StringUtils.isEmpty(prevWidgetKeysString)) {
            try {
                prevKeys = new JSONObject(prevWidgetKeysString);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
        }

        try {
            if (prevKeys == null) {
                prevKeys = new JSONObject();
            }
            prevKeys.put(String.valueOf(widgetID), blog.getRemoteBlogId());
            AppPrefs.setStatsWidgetsKeys(prevKeys.toString());
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        }

        String currentDate = StatsUtils.getCurrentDateTZ(localBlogID);

        // empty string in pref.
        showMessage(context, new int[]{widgetID},
                context.getString(R.string.stats_widget_loading_data));

        enqueueStatsRequestForBlog(context, String.valueOf(blog.getRemoteBlogId()), currentDate);
    }

    private static void refreshWidgets(Context context, int[] appWidgetIds) {
        // If not signed into WordPress inform the user
        if (!AccountHelper.isSignedIn()) {
            showMessage(context, appWidgetIds, context.getString(R.string.stats_widget_error_login));
            return;
        }

        for (int widgetId : appWidgetIds) {
            int remoteBlogID = getRemoteBlogIDFromWidgetID(widgetId);
            // This could happen on logout when prefs are erased completely
            if (remoteBlogID == 0) {
                showMessage(context, new int[] {widgetId}, context.getString(R.string.stats_widget_error_readd_widget));
                continue;
            }
            int localId = StatsUtils.getLocalBlogIdFromRemoteBlogId(remoteBlogID);
            if (localId == 0 || WordPress.getBlog(localId) == null) {
                // No blog in the app
                showMessage(context, new int[] {widgetId}, context.getString(R.string.stats_widget_error_readd_widget));
                continue;
            }
            String currentDate = StatsUtils.getCurrentDateTZ(localId);
            enqueueStatsRequestForBlog(context, String.valueOf(remoteBlogID), currentDate);
        }
    }

    private static void refreshAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, StatsWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        refreshWidgets(context, allWidgetIds);
    }
}