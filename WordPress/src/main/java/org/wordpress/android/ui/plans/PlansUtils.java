package org.wordpress.android.ui.plans;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.PhotonUtils;

import java.util.HashMap;

public class PlansUtils {

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
     * @param plan - The plan
     * @param iconSize - desired size of the returned image
     * @return string containing photon-ized url for the plan icon
     */
    public static String getIconUrlForPlan(Plan plan, int iconSize) {
        if (plan == null || !plan.hasIconUrl()) {
            return null;
        }
        return PhotonUtils.getPhotonImageUrl(plan.getIconUrl(), iconSize, iconSize);
    }

    /**
     * Weather the plan ID is a free plan.
     *
     * @param planID - The plan ID
     * @return boolean - true if the current blog is on a free plan.
     */
    private static boolean isFreePlan(long planID) {
        return planID == PlansConstants.JETPACK_FREE_PLAN_ID || planID == PlansConstants.FREE_PLAN_ID;
    }

    /**
     * Removes stored plan data - for testing purposes
     */
    @SuppressWarnings("unused")
    public static void clearPlanData() {
        AppPrefs.setGlobalPlansFeatures(null);
    }

}
