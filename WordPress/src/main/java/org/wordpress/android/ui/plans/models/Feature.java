package org.wordpress.android.ui.plans.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

public class Feature {
    /*

    {
        "product_slug": "space",
        "title": "Space",
        "description": "Increase your available storage space and add the ability to upload audio files.",
        "icon": "",
        "plans": {
            "1": {
                "title": "Media storage",
                "description": "Upload up to 3GB of photos, videos, or music.",
                "icon": ""
            },
            "1003": {
                "title": "Expanded media storage",
                "description": "Upload up to 13GB of photos, videos, or music.",
                "icon": ""
            },
            "1008": {
                "title": "Unlimited media storage",
                "description": "You can upload unlimited photos, videos, or music.",
                "icon": ""
            }
        }
    },

    OR

    {
        "product_slug": "ecommerce",
        "title": "eCommerce",
        "description": "Sell stuff right on your blog with Ecwid and Shopify.",
        "icon": "",
        "plans": {
            "1008": true
        }
    },

     */

    private String mProductSlug;
    private String mTitle;
    private String mIcon;
    private String mDescription;
    private boolean mIsNotPartOfFreeTrial;
    private final JSONObject mPlanIDToDescription;

    public Feature(JSONObject featureJSONObject) throws JSONException {
        mProductSlug = featureJSONObject.getString("product_slug");
        mTitle = featureJSONObject.getString("title");
        mIcon = featureJSONObject.optString("icon");
        mDescription = featureJSONObject.getString("description");
        mPlanIDToDescription = featureJSONObject.optJSONObject("plans");

        if (featureJSONObject.has("not_part_of_free_trial") &&
                JSONUtils.getBool(featureJSONObject, "not_part_of_free_trial")) {
            // not part of free trial
            mIsNotPartOfFreeTrial = true;
        }
    }

    public String getProductSlug() {
        return StringUtils.notNullStr(mProductSlug);
    }

    public String getTitle() {
        return StringUtils.notNullStr(mTitle);
    }

    public String getDescription() {
        return StringUtils.notNullStr(mDescription);
    }

    public boolean isNotPartOfFreeTrial() {
        return mIsNotPartOfFreeTrial;
    }

    /**
     * Return the description of this feature for a given plan.
     * If description is not provided for the given plan, fallback to the global description of the feature.
     */
    public String getDescriptionForPlan(Long planID) {
        return getPropertyForPlan(planID, "description", mDescription);
    }

    /**
     * Return the title of this feature for a given plan.
     * If title is not provided for the given plan, fallback to the global title of the feature.
     */
    public String getTitleForPlan(Long planID) {
        return getPropertyForPlan(planID, "title", mTitle);
    }

    /**
     * Return the icon of this feature for a given plan.
     * If icon is not provided for the given plan, fallback to the global icon for this feature.
     */
    public String getIconForPlan(Long planID) {
        return getPropertyForPlan(planID, "icon", mIcon);
    }

    private String getPropertyForPlan(Long planID, String propertyName, String fallback) {
        String planIdAsString = String.valueOf(planID);
        fallback = StringUtils.notNullStr(fallback);
        if (mPlanIDToDescription != null && mPlanIDToDescription.has(planIdAsString)) {
            JSONObject plan = mPlanIDToDescription.optJSONObject(planIdAsString);
            if (plan != null) { // It's not a JSON object. Just `true` in the response. That means the plan has this feature with generic description/title/icon.
                return plan.optString(
                        propertyName,
                        fallback
                );
            }
        }
        return fallback;
    }
}
