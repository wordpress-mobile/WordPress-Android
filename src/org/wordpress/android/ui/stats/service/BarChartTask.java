package org.wordpress.android.ui.stats.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.models.StatsBarChartData;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsBarChartUnit;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

class BarChartTask extends AbsStatsTask {

    private final String mBlogId;
    private final StatsBarChartUnit mBarChartUnit;

    public BarChartTask(String blogId, StatsBarChartUnit barChartUnit) {
        mBlogId = StringUtils.notNullStr(blogId);
        mBarChartUnit = barChartUnit;
    }

    @Override
    String getTaskName() {
        return String.format("BarChartTask (%s)", mBarChartUnit.toString());
    }

    @Override
    void sendRequest() {
        WordPress.getRestClientUtils().getStatsBarChartData(mBlogId, mBarChartUnit, 30, responseListener, errorListener);
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response == null || !response.has("data"))
            return;

        Uri uri = StatsContentProvider.STATS_BAR_CHART_DATA_URI;
        try {
            JSONArray results = response.getJSONArray("data");

            int count = results.length();

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            // delete old stats and insert new ones
            if (count > 0) {
                ContentProviderOperation op = ContentProviderOperation.newDelete(uri).withSelection("blogId=? AND unit=?", new String[] { mBlogId, mBarChartUnit.name() }).build();
                operations.add(op);
            }

            for (int i = 0; i < count; i++ ) {
                JSONArray result = results.getJSONArray(i);
                StatsBarChartData stat = new StatsBarChartData(mBlogId, mBarChartUnit, result);
                ContentValues values = StatsBarChartDataTable.getContentValues(stat);

                if (values != null && uri != null) {
                    ContentProviderOperation op = ContentProviderOperation.newInsert(uri).withValues(values).build();
                    operations.add(op);
                }
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            getContentResolver().notifyChange(uri, null);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
