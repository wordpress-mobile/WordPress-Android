package org.wordpress.android.ui.plans;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;

/**
 * service which updates both global plans and available plans for a specific site
 */

public class PlanUpdateService extends Service {
    private int mNumActiveRequests;
    private SiteModel mSite;
    private final List<Plan> mSitePlans = new ArrayList<>();

    @Inject @Named("v1.2") RestClientUtils mRestClientUtilsV1_2;

    public static void startService(Context context, SiteModel site) {
        Intent intent = new Intent(context, PlanUpdateService.class);
        intent.putExtra(WordPress.SITE, site);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        if (context == null) return;

        Intent intent = new Intent(context, PlanUpdateService.class);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.PLANS, "plan update service > created");
        ((WordPress) getApplication()).component().inject(this);
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.PLANS, "plan update service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSite = (SiteModel) intent.getSerializableExtra(WordPress.SITE);

        mNumActiveRequests = 2;
        downloadPlanFeatures();
        downloadAvailablePlansForSite();

        return START_NOT_STICKY;
    }

    /*
     * called when any plan data has been successfully updated
     */
    private void requestCompleted() {
        // send event once all requests have successfully completed
        mNumActiveRequests--;
        if (mNumActiveRequests == 0) {
            EventBus.getDefault().post(new PlanEvents.PlansUpdated(mSite, mSitePlans));
        }
    }

    /*
     * called when updating any plan data fails
     */
    private void requestFailed() {
        EventBus.getDefault().post(new PlanEvents.PlansUpdateFailed());
    }

    /*
     * download features for the global plans
     */
    private void downloadPlanFeatures() {
        mRestClientUtilsV1_2.get("plans/features/", RestClientUtils.getRestLocaleParams(PlanUpdateService.this), null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlansFeatures(response.toString());
                    requestCompleted();
                } else {
                    AppLog.w(AppLog.T.PLANS, "Unexpected empty response from server when downloading Features");
                    requestFailed();
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.PLANS, "Error Loading Plans/Features", volleyError);
                requestFailed();
            }
        });
    }

    /*
     * download plans for the specific site
     */
    private void downloadAvailablePlansForSite() {
        // TODO: STORES: This must be moved to a store
        long remoteBlogId = mSite.getSiteId();
        mRestClientUtilsV1_2.get("sites/" + remoteBlogId + "/plans", RestClientUtils.getRestLocaleParams(PlanUpdateService.this), null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response == null) {
                    AppLog.w(AppLog.T.PLANS, "Unexpected empty response from server");
                    requestFailed();
                    return;
                }

                AppLog.d(AppLog.T.PLANS, response.toString());
                mSitePlans.clear();

                try {
                    JSONArray plansArray = response.getJSONArray("originalResponse");
                    for (int i=0; i < plansArray.length(); i ++) {
                        JSONObject currentPlanJSON = plansArray.getJSONObject(i);
                        Plan currentPlan = new Plan(currentPlanJSON);
                        mSitePlans.add(currentPlan);
                    }
                    requestCompleted();
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.PLANS, "Can't parse the plans list returned from the server", e);
                    requestFailed();
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.UTILS, "Error downloading site plans", volleyError);
                requestFailed();
            }
        });
    }
}
