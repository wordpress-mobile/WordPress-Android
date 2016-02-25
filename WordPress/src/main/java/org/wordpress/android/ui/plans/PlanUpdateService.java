package org.wordpress.android.ui.plans;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.ui.plans.models.SitePlanList;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;

import de.greenrobot.event.EventBus;

/**
 * service which updates both global plans and available plans for a specific site
 */

public class PlanUpdateService extends Service {

    private static final String ARG_LOCAL_BLOG_ID = "local_blog_id";
    private int mLocalBlogId;

    public static void startService(Context context, int localTableBlogId) {
        Intent intent = new Intent(context, PlanUpdateService.class);
        intent.putExtra(ARG_LOCAL_BLOG_ID, localTableBlogId);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.PLANS, "plan update service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.PLANS, "plan update service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLocalBlogId = intent.getIntExtra(ARG_LOCAL_BLOG_ID, 0);

        /*
         * start the three-step update process
         *  Step 1: update global plans
         *  Step 2: update features for global plans
         *  Step 3: update available plans for this specific site
         */
        downloadGlobalPlans();

        return START_NOT_STICKY;
    }

    /*
     * called when plan data has been successfully updated
     */
    private void notifySuccess(@NonNull SitePlanList sitePlans) {
        EventBus.getDefault().post(new PlanEvents.PlansUpdated(sitePlans));
    }

    /*
     * called when updating plan data fails
     */
    private void notifyFailure() {
        EventBus.getDefault().post(new PlanEvents.PlansUpdateFailed());
    }

    /*
     * Step 1: download global plans
     */
    private void downloadGlobalPlans() {
        WordPress.getRestClientUtilsV1_3().get("plans/", WordPress.getRestLocaleParams(), null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlans(response.toString());
                    // proceed to step 2
                    downloadPlanFeatures();
                } else {
                    AppLog.w(AppLog.T.PLANS, "Empty response downloading global Plans");
                    notifyFailure();
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.PLANS, "Error loading plans", volleyError);
                notifyFailure();
            }
        });
    }

    /*
     * Step 2: download plan features
     */
    private void downloadPlanFeatures() {
        WordPress.getRestClientUtilsV1_2().get("plans/features/", WordPress.getRestLocaleParams(), null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlansFeatures(response.toString());
                    // proceed to step 3
                    downloadAvailablePlansForSite();
                } else {
                    AppLog.w(AppLog.T.PLANS, "Unexpected empty response from server when downloading Features");
                    notifyFailure();
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.PLANS, "Error Loading Plans/Features", volleyError);
                notifyFailure();
            }
        });
    }

    /*
     * Step 3: download plans for the specific site
     */
    private void downloadAvailablePlansForSite() {
        int remoteBlogId = WordPress.wpDB.getRemoteBlogIdForLocalTableBlogId(mLocalBlogId);
        WordPress.getRestClientUtils().get("sites/" + remoteBlogId + "/plans", WordPress.getRestLocaleParams(), null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response == null) {
                    AppLog.w(AppLog.T.PLANS, "Unexpected empty response from server");
                    notifyFailure();
                    return;
                }

                AppLog.d(AppLog.T.PLANS, response.toString());
                SitePlanList sitePlans = new SitePlanList();
                try {
                    JSONArray planIDs = response.names();
                    if (planIDs != null) {
                        for (int i = 0; i < planIDs.length(); i++) {
                            String currentKey = planIDs.getString(i);
                            JSONObject currentPlanJSON = response.getJSONObject(currentKey);
                            SitePlan sitePlan = new SitePlan(Long.valueOf(currentKey), currentPlanJSON, mLocalBlogId);
                            sitePlans.add(sitePlan);
                        }
                    }

                    // make sure the plans are in the right order
                    sitePlans.sortPlans();

                    // if we get this far, then all plan data has been successfully downloaded
                    notifySuccess(sitePlans);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.PLANS, "Can't parse the plans list returned from the server", e);
                    notifyFailure();
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.UTILS, "Error downloading site plans", volleyError);
                notifyFailure();
            }
        });
    }
}
