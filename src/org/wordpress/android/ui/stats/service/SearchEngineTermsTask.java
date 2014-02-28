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
import org.wordpress.android.datasets.StatsSearchEngineTermsTable;
import org.wordpress.android.models.StatsSearchEngineTerm;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by nbradbury on 2/25/14.
 */
class SearchEngineTermsTask extends AbsStatsTask {

    private final String mBlogId;
    private final String mDate;

    public SearchEngineTermsTask(final String blogId, final String date) {
        mBlogId = StringUtils.notNullStr(blogId);
        mDate = StringUtils.notNullStr(date);
    }

    @Override
    void sendRequest() {
        WordPress.restClient.getStatsSearchEngineTerms(mBlogId, mDate, responseListener, errorListener);
    }

    @Override
    String getTaskName() {
        return String.format("SearchEngineTermsTask (%s)", mDate);
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response == null)
            return;

        try {
            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI).withSelection("blogId=? AND (date=? OR date<=?)",
                    new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();

            operations.add(delete_op);

            JSONArray results = response.getJSONArray("search-terms");

            int count = Math.min(results.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);
            for (int i = 0; i < count; i++ ) {
                JSONArray result = results.getJSONArray(i);
                StatsSearchEngineTerm stat = new StatsSearchEngineTerm(mBlogId, date, result);
                ContentValues values = StatsSearchEngineTermsTable.getContentValues(stat);
                getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);

                ContentProviderOperation insert_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI).withValues(values).build();
                operations.add(insert_op);
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            getContentResolver().notifyChange(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, null);

        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
