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
import org.wordpress.android.datasets.StatsMostCommentedTable;
import org.wordpress.android.models.StatsMostCommented;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by nbradbury on 2/25/14.
 */
class CommentsMostTask extends AbsStatsTask {

    private final String mBlogId;

    public CommentsMostTask(String blogId) {
        mBlogId = StringUtils.notNullStr(blogId);
    }

    @Override
    void sendRequest() {
        WordPress.getRestClientUtils().getStatsMostCommented(mBlogId, responseListener, errorListener);
    }

    @Override
    String getTaskName() {
        return "CommentsMostTask";
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response == null || !response.has("result"))
            return;

        try {
            JSONArray results = response.getJSONArray("result");
            int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            if (count > 0) {
                ContentProviderOperation op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_MOST_COMMENTED_URI).withSelection("blogId=?", new String[] { mBlogId }).build();
                operations.add(op);
            }

            for (int i = 0; i < count; i++ ) {
                JSONObject result = results.getJSONObject(i);
                StatsMostCommented stat = new StatsMostCommented(mBlogId, result);
                ContentValues values = StatsMostCommentedTable.getContentValues(stat);
                ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_MOST_COMMENTED_URI).withValues(values).build();
                operations.add(op);
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            getContentResolver().notifyChange(StatsContentProvider.STATS_MOST_COMMENTED_URI, null);

        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
