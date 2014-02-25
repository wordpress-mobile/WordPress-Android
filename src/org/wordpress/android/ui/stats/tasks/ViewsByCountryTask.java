package org.wordpress.android.ui.stats.tasks;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsGeoviewsTable;
import org.wordpress.android.models.StatsGeoview;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by nbradbury on 2/25/14.
 */
public class ViewsByCountryTask extends StatsTask {

    private final String mBlogId;
    private final String mDate;

    public ViewsByCountryTask(final String blogId, final String date) {
        mBlogId = StringUtils.notNullStr(blogId);
        mDate = StringUtils.notNullStr(date);
    }

    @Override
    public void run() {
        WordPress.restClient.getStatsGeoviews(mBlogId, mDate,
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
                }
        );
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response == null || !response.has("country-views"))
            return;

        try {
            JSONArray results = response.getJSONArray("country-views");
            int count = results.length();
            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            if (count > 0) {
                // delete data with the same date, and data older than two days ago (keep yesterday's data)
                ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_GEOVIEWS_URI)
                        .withSelection("blogId=? AND (date=? OR date<=?)", new String[]{mBlogId, dateMs + "", (dateMs - StatsService.TWO_DAYS) + ""}).build();
                operations.add(delete_op);
            }

            for (int i = 0; i < count; i++ ) {
                JSONObject result = results.getJSONObject(i);
                StatsGeoview stat = new StatsGeoview(mBlogId, result);
                ContentValues values = StatsGeoviewsTable.getContentValues(stat);
                ContentProviderOperation op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_GEOVIEWS_URI).withValues(values).build();
                operations.add(op);
            }

            ContentResolver resolver = WordPress.getContext().getContentResolver();
            resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            resolver.notifyChange(StatsContentProvider.STATS_GEOVIEWS_URI, null);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
