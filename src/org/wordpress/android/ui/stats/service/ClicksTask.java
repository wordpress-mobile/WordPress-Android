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
import org.wordpress.android.datasets.StatsClickGroupsTable;
import org.wordpress.android.datasets.StatsClicksTable;
import org.wordpress.android.models.StatsClick;
import org.wordpress.android.models.StatsClickGroup;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by nbradbury on 2/25/14.
 */
class ClicksTask extends AbsStatsTask {
    private final String mBlogId;
    private final String mDate;

    public ClicksTask(final String blogId, final String date) {
        mBlogId = StringUtils.notNullStr(blogId);
        mDate = StringUtils.notNullStr(date);
    }

    @Override
    String getTaskName() {
        return String.format("ClicksTask (%s)", mDate);
    }

    @Override
    void sendRequest() {
        WordPress.restClient.getStatsClicks(mBlogId, mDate, responseListener, errorListener);
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response == null)
            return;

        try {
            String date = response.getString("date");
            long dateMs = StatUtils.toMs(date);

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            // delete data with the same date, and data older than two days ago (keep yesterday's data)
            ContentProviderOperation delete_group = ContentProviderOperation.newDelete(StatsContentProvider.STATS_CLICK_GROUP_URI).withSelection("blogId=? AND (date=? OR date<=?)",
                    new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();
            ContentProviderOperation delete_child = ContentProviderOperation.newDelete(StatsContentProvider.STATS_CLICKS_URI).withSelection("blogId=? AND (date=? OR date<=?)",
                    new String[] { mBlogId, dateMs + "", (dateMs - TWO_DAYS) + "" }).build();

            operations.add(delete_group);
            operations.add(delete_child);


            JSONArray groups = response.getJSONArray("clicks");

             // insert groups, limited to the number that can actually be displayed
            int groupsCount = Math.min(groups.length(), StatsActivity.STATS_GROUP_MAX_ITEMS);
            for (int i = 0; i < groupsCount; i++ ) {
                JSONObject group = groups.getJSONObject(i);
                StatsClickGroup statGroup = new StatsClickGroup(mBlogId, date, group);
                ContentValues values = StatsClickGroupsTable.getContentValues(statGroup);

                ContentProviderOperation insert_group = ContentProviderOperation.newInsert(StatsContentProvider.STATS_CLICK_GROUP_URI).withValues(values).build();
                operations.add(insert_group);

                // insert children if there are any, limited to the number that can be displayed
                JSONArray clicks = group.getJSONArray("results");
                int childCount = Math.min(clicks.length(), StatsActivity.STATS_CHILD_MAX_ITEMS);
                if (childCount > 1) {
                    for (int j = 0; j < childCount; j++) {
                        StatsClick stat = new StatsClick(mBlogId, date, statGroup.getGroupId(), clicks.getJSONArray(j));
                        ContentValues v = StatsClicksTable.getContentValues(stat);
                        ContentProviderOperation insert_child = ContentProviderOperation.newInsert(StatsContentProvider.STATS_CLICKS_URI).withValues(v).build();
                        operations.add(insert_child);
                    }
                }
            }

            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            getContentResolver().notifyChange(StatsContentProvider.STATS_CLICK_GROUP_URI, null);
            getContentResolver().notifyChange(StatsContentProvider.STATS_CLICKS_URI, null);

        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
