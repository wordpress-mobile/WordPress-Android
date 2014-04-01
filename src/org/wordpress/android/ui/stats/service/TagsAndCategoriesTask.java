package org.wordpress.android.ui.stats.service;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.datasets.StatsTagsAndCategoriesTable;
import org.wordpress.android.models.StatsTagsandCategories;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

/**
 * Created by nbradbury on 2/25/14.
 */
class TagsAndCategoriesTask extends AbsStatsTask {

    private final String mBlogId;

    public TagsAndCategoriesTask(String blogId) {
        mBlogId = StringUtils.notNullStr(blogId);
    }

    @Override
    String getTaskName() {
        return "TagsAndCategoriesTask";
    }

    @Override
    String getPath() {
        return "";
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
                ContentProviderOperation op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI).withSelection("blogId=?", new String[] { mBlogId }).build();
                operations.add(op);
            }

            for (int i = 0; i < count; i++ ) {
                JSONObject result = results.getJSONObject(i);
                StatsTagsandCategories stat = new StatsTagsandCategories(mBlogId, result);
                ContentValues values = StatsTagsAndCategoriesTable.getContentValues(stat);
                ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI).withValues(values).build();
                operations.add(op);
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            getContentResolver().notifyChange(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, null);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
