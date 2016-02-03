package org.wordpress.android.ui.plans.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;

import java.util.Hashtable;
import java.util.List;

public class Feature {
    /*

        We "need" the IDs of plans to parse Feature correctly, since the Feature contains the IDs of plans
        where it's available as first level key. The value is the label of the feature in that plan.
        See the example below:

        {
        "product_slug": "space",
        "title": "Space",
        "description": "Increase your available storage space and add the ability to upload audio files.",
        "1": "3GB",
        "1003": "13GB",
        "1008": "Unlimited"
    },
     */

    private String mProductSlug;
    private String mTitle;
    private String mDescription;
    private boolean mIsNotPartOfFreeTrial;
    private Hashtable<String, String> planIDToDescription = new Hashtable<>(); // plan ID/Description that have this feature in it.

    public Feature(JSONObject featureJSONObject, List<Long> plansIDS) throws JSONException {
        mProductSlug = featureJSONObject.getString("product_slug");
        mTitle = featureJSONObject.getString("title");
        mDescription = featureJSONObject.getString("description");

        // Loop all over the available plan IDs available in the app and match with features
        for (Long currentPlanID: plansIDS) {
            String currentPlanIDAsString = String.valueOf(currentPlanID);
            if (featureJSONObject.has(currentPlanIDAsString)) {
                // need to use JSONUtils because there are null values returned here. Want to remove them asap.
                String desc = JSONUtils.getString(featureJSONObject, currentPlanIDAsString);
                planIDToDescription.put(currentPlanIDAsString, desc);
            }
        }

        if (featureJSONObject.has("not_part_of_free_trial") &&
                JSONUtils.getBool(featureJSONObject, "not_part_of_free_trial")) {
            // not part of free trial
            mIsNotPartOfFreeTrial = true;
        }
    }

    public String getProductSlug() {
        return mProductSlug;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public Hashtable<String, String> getPlanIDToDescription() {
        return planIDToDescription;
    }

    public boolean isNotPartOfFreeTrial() {
        return mIsNotPartOfFreeTrial;
    }
}
