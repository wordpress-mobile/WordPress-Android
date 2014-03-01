package org.wordpress.android.ui.stats.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsTopPostsAndPagesTable;
import org.wordpress.android.models.StatsTopPostsAndPages;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by nbradbury on 2/25/14.
 */
class TopPostsAndPagesTask extends AbsStatsTask {

    private final String mBlogId;
    private final String mDate;

    public TopPostsAndPagesTask(final String blogId, final String date) {
        mBlogId = StringUtils.notNullStr(blogId);
        mDate = StringUtils.notNullStr(date);
    }

    @Override
    String getTaskName() {
        return String.format("TopPostsAndPagesTask (%s)", mDate);
    }

    @Override
    void sendRequest() {
        WordPress.getRestClientUtils().getStatsTopPosts(mBlogId, mDate, responseListener, errorListener);
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response == null || !response.has("top-posts"))
            return;

        try {
            JSONArray results = response.getJSONArray("top-posts");
            int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);

            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            // delete data with the same date, and data older than two days ago (keep yesterday's data)
            ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI)
                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
            operations.add(delete_op);

            for (int i = 0; i < count; i++ ) {
                JSONObject result = results.getJSONObject(i);
                StatsTopPostsAndPages stat = new StatsTopPostsAndPages(mBlogId, result);
                ContentValues values = StatsTopPostsAndPagesTable.getContentValues(stat);
                ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI).withValues(values).build();
                operations.add(op);
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            getContentResolver().notifyChange(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, null);

        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
