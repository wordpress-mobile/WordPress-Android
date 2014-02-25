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
import org.wordpress.android.datasets.StatsReferrerGroupsTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.models.StatsReferrer;
import org.wordpress.android.models.StatsReferrerGroup;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by nbradbury on 2/25/14.
 */
public class ReferrersTask extends StatsTask {

    private final String mBlogId;
    private final String mDate;

    public ReferrersTask(final String blogId, final String date) {
        mBlogId = StringUtils.notNullStr(blogId);
        mDate = StringUtils.notNullStr(date);
    }

    @Override
    public void run() {
        WordPress.restClient.getStatsReferrers(mBlogId, mDate,
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

            // delete data with the same date, and data older than two days ago (keep yesterday's data)
            ContentProviderOperation delete_group_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_REFERRER_GROUP_URI)
                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { mBlogId, dateMs + "", (dateMs - StatsService.TWO_DAYS) + "" }).build();
            operations.add(delete_group_op);

            ContentProviderOperation delete_op = ContentProviderOperation.newDelete(StatsContentProvider.STATS_REFERRERS_URI)
                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] { mBlogId, dateMs + "", (dateMs - StatsService.TWO_DAYS) + "" }).build();
            operations.add(delete_op);


            JSONArray groups = response.getJSONArray("referrers");
            int groupsCount = groups.length();

            // insert groups
            for (int i = 0; i < groupsCount; i++ ) {
                JSONObject group = groups.getJSONObject(i);
                StatsReferrerGroup statGroup = new StatsReferrerGroup(mBlogId, date, group);
                ContentValues values = StatsReferrerGroupsTable.getContentValues(statGroup);
                ContentProviderOperation insert_group_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_REFERRER_GROUP_URI).withValues(values).build();
                operations.add(insert_group_op);

                // insert children, only if there is more than one entry
                JSONArray referrers = group.getJSONArray("results");
                int count = referrers.length();
                if (count > 1) {

                    for (int j = 0; j < count; j++) {
                        StatsReferrer stat = new StatsReferrer(mBlogId, date, statGroup.getGroupId(), referrers.getJSONArray(j));
                        ContentValues v = StatsReferrersTable.getContentValues(stat);
                        ContentProviderOperation insert_child_op = ContentProviderOperation.newInsert(StatsContentProvider.STATS_REFERRERS_URI).withValues(v).build();
                        operations.add(insert_child_op);
                    }
                }

            }

            ContentResolver resolver = WordPress.getContext().getContentResolver();
            resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            resolver.notifyChange(StatsContentProvider.STATS_REFERRER_GROUP_URI, null);
            resolver.notifyChange(StatsContentProvider.STATS_REFERRERS_URI, null);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (RemoteException e) {
            AppLog.e(AppLog.T.STATS, e);
        } catch (OperationApplicationException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }
}
