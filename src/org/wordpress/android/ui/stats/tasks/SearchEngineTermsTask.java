package org.wordpress.android.ui.stats.tasks;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsSearchEngineTermsTable;
import org.wordpress.android.models.StatsSearchEngineTerm;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by nbradbury on 2/25/14.
 */
public class SearchEngineTermsTask extends StatsTask {

    private final String mBlogId;
    private final String mDate;

    public SearchEngineTermsTask(final String blogId, final String date) {
        mBlogId = StringUtils.notNullStr(blogId);
        mDate = StringUtils.notNullStr(date);
    }

    @Override
    public void run() {
        WordPress.restClient.getStatsSearchEngineTerms(mBlogId, mDate,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        parseResponse(response);
                    }
                },
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.STATS, error);
                    }
                });
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
                    new String[] { mBlogId, dateMs + "", (dateMs - StatsService.TWO_DAYS) + "" }).build();

            operations.add(delete_op);

            JSONArray results = response.getJSONArray("search-terms");

            Context context = WordPress.getContext();

            int count = results.length();
            for (int i = 0; i < count; i++ ) {
                JSONArray result = results.getJSONArray(i);
                StatsSearchEngineTerm stat = new StatsSearchEngineTerm(mBlogId, date, result);
                ContentValues values = StatsSearchEngineTermsTable.getContentValues(stat);
                context.getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);

                ContentProviderOperation insert_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI).withValues(values).build();
                operations.add(insert_op);
            }

            ContentResolver resolver = context.getContentResolver();
            resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            resolver.notifyChange(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, null);

        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
