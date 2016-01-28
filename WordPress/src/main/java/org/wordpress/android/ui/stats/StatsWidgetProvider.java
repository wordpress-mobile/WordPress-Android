package org.wordpress.android.ui.stats;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;
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
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;

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
                name = context.getString(R.string.stats_widget_name_for_blog);
                name = String.format(name, StringEscapeUtils.unescapeHtml(blog.getNameOrHostUrl()));
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

    private static void updateTabValue(Context context, RemoteViews remoteViews, int viewId, String text) {
        remoteViews.setTextViewText(viewId, text);
        if (text.equals("0")) {
            remoteViews.setTextColor(viewId, context.getResources().getColor(R.color.grey));
        }
    }

    private static void showStatsData(Context context, int[] allWidgets, Blog blog, JSONObject data) {
        if (allWidgets.length == 0){
            return;
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        String name = context.getString(R.string.stats_widget_name_for_blog);
        name = String.format(name, StringEscapeUtils.unescapeHtml(blog.getNameOrHostUrl()));

        for (int widgetId : allWidgets) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stats_widget_layout);
            remoteViews.setTextViewText(R.id.blog_title, name);

            remoteViews.setViewVisibility(R.id.stats_widget_error_container, View.GONE);
            remoteViews.setViewVisibility(R.id.stats_widget_values_container, View.VISIBLE);

            // Update Views
            updateTabValue(context, remoteViews, R.id.stats_widget_views, data.optString("views", " 0"));

            // Update Visitors
            updateTabValue(context, remoteViews, R.id.stats_widget_visitors, data.optString("visitors", " 0"));

            // Update Comments
            updateTabValue(context, remoteViews, R.id.stats_widget_comments, data.optString("comments", " 0"));

            // Update Likes
            updateTabValue(context, remoteViews, R.id.stats_widget_likes, data.optString("likes", " 0"));

            Intent intent = new Intent(context, StatsActivity.class);
            intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, blog.getLocalTableBlogId());
            intent.putExtra(StatsActivity.ARG_LAUNCHED_FROM, StatsActivity.StatsLaunchedFrom.STATS_WIDGET);
            intent.putExtra(StatsActivity.ARG_DESIRED_TIMEFRAME, StatsTimeframe.DAY);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, blog.getLocalTableBlogId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.stats_widget_outer_container, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private static void ShowCacheIfAvailableOrGenericError(Context context, int remoteBlogID) {
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

        String currentDate = StatsUtils.getCurrentDateTZ(localId);

        // Show cached data if available
        JSONObject cache = getCacheDataForBlog(remoteBlogID, currentDate);
        if (cache != null) {
            showStatsData(context, widgetIDs, blog, cache);
        } else {
            showMessage(context, widgetIDs, context.getString(R.string.stats_widget_error_generic));
        }
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

            // Check if Jetpack or .com
            int localId = StatsUtils.getLocalBlogIdFromRemoteBlogId(remoteBlogID);
            Blog blog = WordPress.getBlog(localId);
            if (blog == null) {
                return;
            }

            if (blog.isDotcomFlag()) {
                // User cannot access stats for this .com blog
                showMessage(context, widgetIDs, context.getString(R.string.stats_widget_error_no_permissions));
            } else {
                // Not logged into wpcom, or the main .com account of the app is not linked with this blog
                showMessage(context, widgetIDs, context.getString(R.string.stats_sign_in_jetpack_different_com_account));
            }
            return;
        }

        ShowCacheIfAvailableOrGenericError(context, remoteBlogID);
    }

    // This is called by the Stats service in case of error
    public static void updateWidgets(Context context, int remoteBlogID, StatsError error) {
        if (error == null) {
            AppLog.e(AppLog.T.STATS, "Widget received a StatsError that is null!");
            return;
        }

        ShowCacheIfAvailableOrGenericError(context, remoteBlogID);
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

            // Store new data in cache
            String prevDataAsString = AppPrefs.getStatsWidgetsData();
            JSONObject prevData = null;
            if (!StringUtils.isEmpty(prevDataAsString)) {
                try {
                    prevData = new JSONObject(prevDataAsString);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, e);
                }
            }
            try {
                if (prevData == null) {
                    prevData = new JSONObject();
                }
                prevData.put(data.getBlogID(), newData);
                AppPrefs.setStatsWidgetsData(prevData.toString());
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            }

            // Show data on the screen now!
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
        AppLog.d(AppLog.T.STATS, "onEnabled called");
        // Note: don't erase prefs here, since for some reasons this method is called after the booting of the device.
    }

    /**
     * This is called when the last instance of your App Widget is deleted from the App Widget host.
     * This is where you should clean up any work done in onEnabled(Context), such as delete a temporary database.
     * @param context The Context in which this receiver is running.
     */
    @Override
    public void onDisabled(Context context) {
        AppLog.d(AppLog.T.STATS, "onDisabled called");
        AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_WIDGET_REMOVED);
        AnalyticsTracker.flush();
        AppPrefs.resetStatsWidgetsKeys();
        AppPrefs.resetStatsWidgetsData();
    }

    /**
     * This is called every time an App Widget is deleted from the App Widget host.
     * @param context The Context in which this receiver is running.
     * @param widgetIDs Widget IDs to set blank. We cannot remove widget from home screen.
     */
    @Override
    public void onDeleted(Context context, int[] widgetIDs) {
        setRemoteBlogIDForWidgetIDs(widgetIDs, null);
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

    private static synchronized JSONObject getCacheDataForBlog(int remoteBlogID, String date) {
        String prevDataAsString = AppPrefs.getStatsWidgetsData();
        if (StringUtils.isEmpty(prevDataAsString)) {
            AppLog.i(AppLog.T.STATS, "No cache found for the widgets");
            return null;
        }

        try {
            JSONObject prevData = new JSONObject(prevDataAsString);
            if (!prevData.has(String.valueOf(remoteBlogID))) {
                AppLog.i(AppLog.T.STATS, "No cache found for the blog ID " + remoteBlogID);
                return null;
            }

            JSONObject cache = prevData.getJSONObject(String.valueOf(remoteBlogID));
            String dateStoredInCache = cache.optString("date");
            if (date.equals(dateStoredInCache)) {
                AppLog.i(AppLog.T.STATS, "Cache found for the blog ID " + remoteBlogID);
                return cache;
            } else {
                AppLog.i(AppLog.T.STATS, "Cache found for the blog ID " + remoteBlogID + " but the date value doesn't match!!");
                return null;
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
            return null;
        }
    }

    public static synchronized boolean isBlogDisplayedInWidget(int remoteBlogID) {
        String prevWidgetKeysString = AppPrefs.getStatsWidgetsKeys();
        if (StringUtils.isEmpty(prevWidgetKeysString)) {
            return false;
        }
        try {
            JSONObject prevKeys = new JSONObject(prevWidgetKeysString);
            JSONArray allKeys = prevKeys.names();
            if (allKeys == null) {
                return false;
            }
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
            if (allKeys == null) {
                return new int[0];
            }
            for (int i=0; i < allKeys.length(); i ++) {
                String currentKey = allKeys.getString(i);
                int currentBlogID = prevKeys.getInt(currentKey);
                if (currentBlogID == remoteBlogID) {
                    AppLog.d(AppLog.T.STATS, "The blog with remoteID " + remoteBlogID + " is displayed in the widget " + currentKey);
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


    // Store the association between widgetIDs and the remote blog id into prefs.
    private static void setRemoteBlogIDForWidgetIDs(int[] widgetIDs, String remoteBlogID) {
        String prevWidgetKeysString = AppPrefs.getStatsWidgetsKeys();
        JSONObject prevKeys = null;
        if (!StringUtils.isEmpty(prevWidgetKeysString)) {
            try {
                prevKeys = new JSONObject(prevWidgetKeysString);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
        }

        if (prevKeys == null) {
            prevKeys = new JSONObject();
        }

        for (int widgetID : widgetIDs) {
            try {
                prevKeys.put(String.valueOf(widgetID), remoteBlogID);
                AppPrefs.setStatsWidgetsKeys(prevKeys.toString());
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
        }
    }

    // This is called by the Widget config activity at the end if the process
    static void setupNewWidget(Context context, int widgetID, int localBlogID) {
        AppLog.d(AppLog.T.STATS, "setupNewWidget called");

        Blog blog = WordPress.getBlog(localBlogID);
        if (blog == null) {
            // it's unlikely that blog is null here.
            // This method is called from config activity which has loaded the blog fine.
            showMessage(context, new int[]{widgetID},
                    context.getString(R.string.stats_widget_error_readd_widget));
            AppLog.e(AppLog.T.STATS, "setupNewWidget: No blog found in the db!");
            return;
        }

        // At this point the remote ID cannot be null.
        String remoteBlogID = blog.getDotComBlogId();
        // Add the following check just to be safe
        if (remoteBlogID == null) {
            showMessage(context, new int[]{widgetID},
                    context.getString(R.string.stats_widget_error_readd_widget));
            return;
        }

        AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_WIDGET_ADDED, remoteBlogID);
        AnalyticsTracker.flush();

        // Store the association between the widget ID and the remote blog id into prefs.
        setRemoteBlogIDForWidgetIDs(new int[] {widgetID}, remoteBlogID);

        String currentDate = StatsUtils.getCurrentDateTZ(localBlogID);

        // Load cached data if available and show it immediately
        JSONObject cache = getCacheDataForBlog(Integer.parseInt(remoteBlogID), currentDate);
        if (cache != null) {
            showStatsData(context, new int[] {widgetID}, blog, cache);
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            showMessage(context, new int[] {widgetID}, context.getString(R.string.no_network_title));
        } else {
            showMessage(context, new int[] {widgetID}, context.getString(R.string.stats_widget_loading_data));
            enqueueStatsRequestForBlog(context, remoteBlogID, currentDate);
        }
    }


    private static void refreshWidgets(Context context, int[] appWidgetIds) {
        // If not signed into WordPress inform the user
        if (!AccountHelper.isSignedIn()) {
            showMessage(context, appWidgetIds, context.getString(R.string.stats_widget_error_no_account));
            return;
        }

        SparseArray<ArrayList<Integer>> blogsToWidgetIDs = new SparseArray<>();
        for (int widgetId : appWidgetIds) {
            int remoteBlogID = getRemoteBlogIDFromWidgetID(widgetId);
            if (remoteBlogID == 0) {
                // This could happen on logout when prefs are erased completely since we cannot remove
                // widgets programmatically from the screen, or during the configuration of new widgets!!!
                AppLog.e(AppLog.T.STATS, "No remote blog ID for widget ID " + widgetId);
                showMessage(context, new int[] {widgetId}, context.getString(R.string.stats_widget_error_readd_widget));
                continue;
            }

            ArrayList<Integer> widgetIDs = blogsToWidgetIDs.get(remoteBlogID, new ArrayList<Integer>());
            widgetIDs.add(widgetId);
            blogsToWidgetIDs.append(remoteBlogID, widgetIDs);
        }

        // we now have an optimized data structure for our needs. BlogId -> widgetIDs list
        for(int i = 0; i < blogsToWidgetIDs.size(); i++) {
            int remoteBlogID = blogsToWidgetIDs.keyAt(i);
            // get the object by the key.
            ArrayList<Integer> widgetsList = blogsToWidgetIDs.get(remoteBlogID);
            int[] currentWidgets = ArrayUtils.toPrimitive(widgetsList.toArray(new Integer[widgetsList.size()]));

            int localId = StatsUtils.getLocalBlogIdFromRemoteBlogId(remoteBlogID);
            Blog blog = WordPress.getBlog(localId);
            if (localId == 0 || blog == null) {
                // No blog in the app
                showMessage(context, currentWidgets, context.getString(R.string.stats_widget_error_readd_widget));
                continue;
            }
            String currentDate = StatsUtils.getCurrentDateTZ(localId);

            // Load cached data if available and show it immediately
            JSONObject cache = getCacheDataForBlog(remoteBlogID, currentDate);
            if (cache != null) {
                showStatsData(context, currentWidgets, blog, cache);
            }

            // If network is not available check if NO cache, and show the generic error
            // If network is available always start a refresh, and show prev data or the loading in progress message.
            if (!NetworkUtils.isNetworkAvailable(context)) {
                if (cache == null) {
                    showMessage(context, currentWidgets, context.getString(R.string.stats_widget_error_generic));
                }
            } else {
                if (cache == null) {
                    showMessage(context, currentWidgets, context.getString(R.string.stats_widget_loading_data));
                }
                // Make sure to refresh widget data now.
                enqueueStatsRequestForBlog(context, String.valueOf(remoteBlogID), currentDate);
            }
        }
    }

    private static void refreshAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, StatsWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        refreshWidgets(context, allWidgetIds);
    }
}