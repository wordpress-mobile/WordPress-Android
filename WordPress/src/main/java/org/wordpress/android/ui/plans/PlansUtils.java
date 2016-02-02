package org.wordpress.android.ui.plans;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.RateLimitedTask;

import java.util.ArrayList;
import java.util.List;

public class PlansUtils {

    private static final int SECONDS_BETWEEN_PLANS_UPDATE = 20 * 60; // 20 minutes

    public static List<Plan> getGlobalPlans() {
        String plansString = AppPrefs.getGlobalPlans();
        if (TextUtils.isEmpty(plansString)) {
            return null;
        }

        List<Plan> plans = new ArrayList<>();
        try {
            JSONObject plansJSONObject = new JSONObject(plansString);
            JSONArray plansArray = plansJSONObject.getJSONArray("originalResponse");
            for (int i=0; i < plansArray.length(); i ++) {
                JSONObject currentPlanJSON = plansArray.getJSONObject(i);
                Plan currentPlan = new Plan(currentPlanJSON);
                plans.add(currentPlan);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.PLANS, "Can't parse the plans list returned from the server", e);
        }

        return plans;
    }

    public static String getGlobalPlansFeatures() {
        return AppPrefs.getGlobalPlansFeatures();
    }

    public static void updateGlobalPlans(final RestRequest.Listener listener, final RestRequest.ErrorListener errorListener) {
        WordPress.getRestClientUtils().get("plans/", new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlans(response.toString());
                }

                if (listener != null) {
                    listener.onResponse(response);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.PLANS, "Error", volleyError);
                if (errorListener!= null) {
                    errorListener.onErrorResponse(volleyError);
                }
            }
        });
    }

    /**
     *  Download all available plans from wpcom
     */
    public static RateLimitedTask sAvailablePlans = new RateLimitedTask(SECONDS_BETWEEN_PLANS_UPDATE) {
        protected boolean run() {
            updateGlobalPlans(null, null);
            return true;
        }
    };
}
