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
import org.wordpress.android.datasets.StatsTopCommentersTable;
import org.wordpress.android.models.StatsTopCommenter;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by nbradbury on 2/25/14.
 */
class CommentsTopTask extends AbsStatsTask {

    private final String mBlogId;

    public CommentsTopTask(String blogId) {
        mBlogId = StringUtils.notNullStr(blogId);
    }

    @Override
    void sendRequest() {
        WordPress.getRestClientUtils().getStatsTopCommenters(mBlogId, responseListener, errorListener);
    }

    @Override
    String getTaskName() {
        return "CommentsTopTask";
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
                ContentProviderOperation op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_TOP_COMMENTERS_URI).withSelection("blogId=?", new String[] { mBlogId }).build();
                operations.add(op);
            }

            for (int i = 0; i < count; i++ ) {
                JSONObject result = results.getJSONObject(i);
                StatsTopCommenter stat = new StatsTopCommenter(mBlogId, result);
                ContentValues values = StatsTopCommentersTable.getContentValues(stat);
                ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_TOP_COMMENTERS_URI).withValues(values).build();
                operations.add(op);
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            getContentResolver().notifyChange(StatsContentProvider.STATS_TOP_COMMENTERS_URI, null);

        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
