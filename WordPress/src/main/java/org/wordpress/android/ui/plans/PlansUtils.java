package org.wordpress.android.ui.plans;

import android.content.Context;
import android.support.annotation.Nullable;
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
import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlansUtils {

    private static HashMap<Integer, List<SitePlan>> availablePlansForSites = new HashMap<>();

    public interface AvailablePlansListener {
        void onResponse(List<SitePlan> plans);
        void onError(Exception volleyError);
    }

    public static boolean downloadAvailablePlansForSite(boolean force, final Blog blog, final AvailablePlansListener listener) {
        if (!PlansUtils.isPlanFeatureAvailableForBlog(blog)) {
            return false;
        }

        if (!force && PlansUtils.getAvailablePlansForSite(blog) != null) {
            // Plans for the site already available.
            return false;
        }

        Map<String, String> params = getDefaultRestCallParameters();
        WordPress.getRestClientUtils().get("sites/" + blog.getDotComBlogId() + "/plans", params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    List<SitePlan> plans = new ArrayList<>();
                    try {
                        JSONArray planIDs = response.names();
                        if (planIDs != null) {
                            for (int i=0; i < planIDs.length(); i ++) {
                                String currentKey = planIDs.getString(i);
                                JSONObject currentPlanJSON = response.getJSONObject(currentKey);
                                SitePlan currentPlan = new SitePlan(Long.valueOf(currentKey), currentPlanJSON, blog);
                                plans.add(currentPlan);
                            }
                        }
                        availablePlansForSites.put(blog.getLocalTableBlogId(), plans);
                        if (listener!= null) {
                            listener.onResponse(plans);
                        }
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.PLANS, "Can't parse the plans list returned from the server", e);
                        if (listener!= null) {
                            listener.onError(e);
                        }
                    }
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.UTILS, "Error", volleyError);
                if (listener!= null) {
                    listener.onError(volleyError);
                }
            }
        });

        return true;
    }

    @Nullable
    public static List<SitePlan> getAvailablePlansForSite(Blog blog) {
        return availablePlansForSites.get(blog.getLocalTableBlogId());
    }

    @Nullable
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

    @Nullable
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

    @Nullable
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

    @Nullable
    public static List<Feature> getFeatures() {
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

    public static boolean downloadGlobalPlans(final Context ctx, final RestRequest.Listener listener, final RestRequest.ErrorListener errorListener) {
        if (!NetworkUtils.isNetworkAvailable(ctx)) {
            return false;
        }
        Map<String, String> params = getDefaultRestCallParameters();
        WordPress.getRestClientUtils().get("plans/", params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlans(response.toString());

                    // Load details of features from the server.
                    downloadFeatures(ctx, null, null);
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
        return true;
    }

    /*
     * Download Features from the WordPress.com backend.
     *
     * Return true if the request is enqueued. False otherwise.
     */
    public static boolean downloadFeatures(final Context ctx, final RestRequest.Listener listener, final RestRequest.ErrorListener errorListener) {
        if (!NetworkUtils.isNetworkAvailable(ctx)) {
            return false;
        }
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

        return true;
    }

    /**
     * This function returns default parameters used in all REST Calls in Plans.
     *
     * The "locale" parameter fox example is one of those we need to add to the request. It must be set to retrieve
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

       // fastes than above but not completely safe.
      return !TextUtils.isEmpty(AppPrefs.getGlobalPlans()) &&
              blog.getPlanID() != 0;
    }
}
