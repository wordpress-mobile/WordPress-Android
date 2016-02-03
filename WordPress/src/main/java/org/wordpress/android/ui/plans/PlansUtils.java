package org.wordpress.android.ui.plans;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.RateLimitedTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlansUtils {

    private static final int SECONDS_BETWEEN_PLANS_UPDATE = 20 * 60; // 20 minutes

    public static Plan getGlobalPlan(long planId) {
        List<Plan> plans = getGlobalPlans();
        if (plans == null || plans.size() == 0) {
            return null;
        }

        for (Plan current: plans) {
            if (current.getProductID() == planId) {
                return  current;
            }
        }

        return null;
    }

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
            return null;
        }

        return plans;
    }

    public static List<Long> getGlobalPlansIDS() {
        List<Plan> plans = getGlobalPlans();
        if (plans == null) {
            return null;
        }

        List<Long> plansIDS = new ArrayList<>(plans.size());
        for (Plan currentPlan: plans) {
            plansIDS.add(currentPlan.getProductID());
        }

        return plansIDS;
    }

    public static List<Feature> getGlobalPlansFeatures() {
        String featuresString = AppPrefs.getGlobalPlansFeatures();
        if (TextUtils.isEmpty(featuresString)) {
            return null;
        }

        List<Long> plansIDS = getGlobalPlansIDS();
        if (plansIDS == null || plansIDS.size() == 0) {
            //no plans stored in the app. Features are attached to plans. We can probably returns null here.
            //TODO: Check if we need to return null or features with empty links to plans
            return null;
        }

        List<Feature> features = new ArrayList<>();
        try {
            JSONObject featuresJSONObject = new JSONObject(featuresString);
            JSONArray featuresArray = featuresJSONObject.getJSONArray("originalResponse");
            for (int i=0; i < featuresArray.length(); i ++) {
                JSONObject currentFeatureJSON = featuresArray.getJSONObject(i);
                Feature currentFeature = new Feature(currentFeatureJSON, plansIDS);
                features.add(currentFeature);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.PLANS, "Can't parse the features list returned from the server", e);
            return null;
        }

        return features;
    }

    public static void updateGlobalPlans(final RestRequest.Listener listener, final RestRequest.ErrorListener errorListener) {
        Map<String, String> params = getDefaultRestCallParameters();
        WordPress.getRestClientUtils().get("plans/", params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlans(response.toString());

                    // Load details of features from the server.
                    updateGlobalPlansFeatures(null, null);
                }

                if (listener != null) {
                    listener.onResponse(response);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.PLANS, "Error loading plans/", volleyError);
                if (errorListener!= null) {
                    errorListener.onErrorResponse(volleyError);
                }
            }
        });
    }

    public static void updateGlobalPlansFeatures(final RestRequest.Listener listener, final RestRequest.ErrorListener errorListener) {
        Map<String, String> params = getDefaultRestCallParameters();
        WordPress.getRestClientUtils().get("plans/features/", params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlansFeatures(response.toString());
                }
                if (listener != null) {
                    listener.onResponse(response);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.PLANS, "Error Loading Plans/Features", volleyError);
                if (errorListener!= null) {
                    errorListener.onErrorResponse(volleyError);
                }
            }
        });
    }


    /**
     *  Download all available plans from wpcom. If the call ends with success it start to download features.
     */
    public static RateLimitedTask sAvailablePlans = new RateLimitedTask(SECONDS_BETWEEN_PLANS_UPDATE) {
        protected boolean run() {
            updateGlobalPlans(null, null);
            return true;
        }
    };

    /**
     * This function returns default parameters used in all REST Calls in Plans.
     *
     * The "locale" parameter fox ex is one of those we need to add to the request. It must be set to retrieve
     * the localized version of plans descriptions and avoid hardcode them in code.
     *
     * @return The map with default parameters.
     */
    private static Map<String, String> getDefaultRestCallParameters() {
        String deviceLanguageCode = Locale.getDefault().getLanguage();
        Map<String, String> params = new HashMap<>();
        if (!TextUtils.isEmpty(deviceLanguageCode)) {
            params.put("locale", deviceLanguageCode);
        }
        return params;
    }

    /**
     * This function return true if Plans are available for a blog.
     * Basically this means that Plans data were downloaded from the server,
     * and the blog has the product_id stored in the DB.
     *
     * @param blog to test
     * @return True if Plans are enabled on the blog
     */
    public static boolean isPlanFeatureAvailableForBlog(Blog blog) {
        /*List<Long> plansIDS = getGlobalPlansIDS();
        if (plansIDS == null || plansIDS.size() == 0) {
            return false;
        }*/

        // fast implementation of the function above. Less safe though.
        String plansString = AppPrefs.getGlobalPlans();
        if (TextUtils.isEmpty(plansString)) {
            return false;
        }
        return (blog.getPlanID() != 0);
    }
}
