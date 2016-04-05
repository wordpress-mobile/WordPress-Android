package org.wordpress.android.ui.plans.models;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/*
Each single Plan has a list of features sections to highlight on the plan details screen. This class model
a single section to highlight.
       "features_highlight": [
            {
                "items": [
                    "custom-design",
                    "videopress",
                    "support",
                    "space",
                    "domain_map",
                    "no-adverts\/no-adverts.php"
                ]
            },
            {
                "title": "Included with all plans",
                "items": [
                    "free-blog"
                ]
            }
        ],
 */
public class PlanFeaturesHighlightSection implements Serializable {
    private String mTitle; // title (if available) of this section
    private ArrayList<String> mItems; // slug of the features to highlight in this section

    PlanFeaturesHighlightSection(JSONObject featureSection) throws JSONException{
        mTitle = featureSection.optString("title");
        JSONArray items = featureSection.getJSONArray("items");
        mItems = new ArrayList<>(items.length());
        for (int i=0; i < items.length(); i++) {
            mItems.add(items.getString(i));
        }
    }

    public String getTitle() {
        return mTitle;
    }

    public ArrayList<String> getFeatures() {
        return mItems;
    }
}
