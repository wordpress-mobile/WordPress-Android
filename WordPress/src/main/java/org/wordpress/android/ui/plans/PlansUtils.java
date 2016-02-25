package org.wordpress.android.ui.plans;

import android.support.annotation.NonNull;
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
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.PhotonUtils;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlansUtils {

    private static String deviceCurrencyCode; // ISO 4217 currency code.
    private static String deviceCurrencySymbol;

    private static final String DOLLAR_SYMBOL = "$";
    private static final String DOLLAR_ISO4217_CODE = "USD";

    /**
     * Returns the price for the passed plan formatted for the user's locale, defaults
     * to USD if user's locale isn't matched
     *
     * TODO: endpoint will be updated to include the formatted price, so this method is temporary
     */
    public static String getPlanDisplayPrice(@NonNull Plan plan) {
        // lookup currency code/symbol on first use
        if (deviceCurrencyCode == null) {
            Currency currency = Currency.getInstance(Locale.getDefault());
            deviceCurrencyCode = currency.getCurrencyCode();
            deviceCurrencySymbol = currency.getSymbol(Locale.getDefault());
        }

        String currencySymbol;
        int priceValue;
        Hashtable<String, Integer> pricesMap = plan.getPrices();
        if (pricesMap.containsKey(deviceCurrencyCode)) {
            currencySymbol = deviceCurrencySymbol;
            priceValue = pricesMap.get(deviceCurrencyCode);
            return currencySymbol + FormatUtils.formatInt(priceValue);
        } else {
            // locale not found, default to USD
            currencySymbol = DOLLAR_SYMBOL;
            priceValue = pricesMap.get(DOLLAR_ISO4217_CODE);
            return currencySymbol + FormatUtils.formatInt(priceValue) + " " + DOLLAR_ISO4217_CODE;
        }
    }

    public interface AvailablePlansListener {
        void onResponse(List<SitePlan> plans);
        void onError();
    }

    public static boolean downloadAvailablePlansForSite(int localTableBlogID, final AvailablePlansListener listener) {
        final Blog blog = WordPress.getBlog(localTableBlogID);
        if (blog == null || !isPlanFeatureAvailableForBlog(blog)) {
            return false;
        }

        Map<String, String> params = getDefaultRestCallParameters();
        WordPress.getRestClientUtils().get("sites/" + blog.getDotComBlogId() + "/plans", params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response == null) {
                    AppLog.w(AppLog.T.PLANS, "Unexpected empty response from server");
                    if (listener != null) {
                        listener.onError();
                    }
                    return;
                }

                AppLog.d(AppLog.T.PLANS, response.toString());
                List<SitePlan> plans = new ArrayList<>();
                try {
                    JSONArray planIDs = response.names();
                    if (planIDs != null) {
                        for (int i = 0; i < planIDs.length(); i++) {
                            String currentKey = planIDs.getString(i);
                            JSONObject currentPlanJSON = response.getJSONObject(currentKey);
                            SitePlan currentPlan = new SitePlan(Long.valueOf(currentKey), currentPlanJSON, blog);
                            plans.add(currentPlan);
                        }
                    }
                    if (listener != null) {
                        listener.onResponse(plans);
                    }
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.PLANS, "Can't parse the plans list returned from the server", e);
                    if (listener != null) {
                        listener.onError();
                    }
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.UTILS, "Error downloading site plans for the site with ID " + blog.getDotComBlogId(), volleyError);
                if (listener != null) {
                    listener.onError();
                }
            }
        });

        return true;
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
    public static HashMap<String, Feature> getFeatures() {
        String featuresString = AppPrefs.getGlobalPlansFeatures();
        if (TextUtils.isEmpty(featuresString)) {
            return null;
        }

        HashMap<String, Feature> features = new HashMap<>();
        try {
            JSONObject featuresJSONObject = new JSONObject(featuresString);
            JSONArray featuresArray = featuresJSONObject.getJSONArray("originalResponse");
            for (int i=0; i < featuresArray.length(); i ++) {
                JSONObject currentFeatureJSON = featuresArray.getJSONObject(i);
                Feature currentFeature = new Feature(currentFeatureJSON);
                features.put(currentFeature.getProductSlug(), currentFeature);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.PLANS, "Can't parse the features list returned from the server", e);
            return null;
        }

        return features;
    }

    /**
     * Returns the url of the image to display for the passed plan
     *
     * @param planId - ID of the global plan
     * @param iconSize - desired size of the returned image
     * @return string containing photon-ized plan icon
     */
    public static String getIconUrlForPlan(long planId, int iconSize) {
        Plan plan = getGlobalPlan(planId);
        if (plan == null || !plan.hasIconUrl()) {
            return null;
        }
        return PhotonUtils.getPhotonImageUrl(plan.getIconUrl(), iconSize, iconSize);
    }

    public static void downloadGlobalPlans() {
        Map<String, String> params = getDefaultRestCallParameters();
        WordPress.getRestClientUtilsV1_3().get("plans/", params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlans(response.toString());
                } else {
                    AppLog.w(AppLog.T.PLANS, "Empty response downloading global Plans!");
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.PLANS, "Error loading plans/", volleyError);
            }
        });
    }

    /*
     * Download Features from the WordPress.com backend.
     *
     * Return true if the request is enqueued. False otherwise.
     */
    public static void downloadFeatures() {
        Map<String, String> params = getDefaultRestCallParameters();
        WordPress.getRestClientUtilsV1_2().get("plans/features/", params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    AppLog.d(AppLog.T.PLANS, response.toString());
                    // Store the response into App Prefs
                    AppPrefs.setGlobalPlansFeatures(response.toString());
                } else {
                    AppLog.w(AppLog.T.PLANS, "Unexpected empty response from server when downloading Features!");
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.PLANS, "Error Loading Plans/Features", volleyError);
            }
        });
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
        return !TextUtils.isEmpty(AppPrefs.getGlobalPlans()) &&
                blog != null && blog.getPlanID() != 0;
    }

    /**
     * Compares two plan products - assumes lower product IDs are "lesser" than higher product IDs
     */
    public static final int LESSER_PRODUCT = -1;
    public static final int EQUAL_PRODUCT = 0;
    public static final int GREATER_PRODUCT = 1;
    public static int compareProducts(long lhsProductId, long rhsProductId) {
        // this duplicates Long.compare(), which wasn't added until API 19
        return lhsProductId < rhsProductId ? LESSER_PRODUCT : (lhsProductId == rhsProductId ? EQUAL_PRODUCT : GREATER_PRODUCT);
    }

    /**
     * Removes stored plan data - for testing purposes
     */
    public static void clearPlanData() {
        AppPrefs.setGlobalPlans(null);
        AppPrefs.setGlobalPlansFeatures(null);
    }

}
