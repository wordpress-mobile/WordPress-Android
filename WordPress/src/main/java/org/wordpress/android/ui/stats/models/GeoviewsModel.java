package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class GeoviewsModel extends BaseStatsModel {
    private String mDate;
    private long mBlogID;
    private int mOtherViews;
    private int mTotalViews;
    private List<GeoviewModel> mCountries;

    public GeoviewsModel(long blogID, JSONObject response) throws JSONException {
        mBlogID = blogID;
        mDate = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String firstDayKey = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(firstDayKey);
        mOtherViews = firstDayObject.getInt("other_views");
        mTotalViews = firstDayObject.getInt("total_views");

        JSONObject countryInfoJSON = response.optJSONObject("country-info");
        JSONArray viewsJSON = firstDayObject.optJSONArray("views");

        if (viewsJSON != null && countryInfoJSON != null) {
            mCountries = new ArrayList<>(viewsJSON.length());
            for (int i = 0; i < viewsJSON.length(); i++) {
                JSONObject currentCountryJSON = viewsJSON.getJSONObject(i);
                String currentCountryCode = currentCountryJSON.getString("country_code");
                int currentCountryViews = currentCountryJSON.getInt("views");
                String flagIcon = null;
                String flatFlagIcon = null;
                String countryFullName = null;
                JSONObject currentCountryDetails = countryInfoJSON.optJSONObject(currentCountryCode);
                if (currentCountryDetails != null) {
                    flagIcon = currentCountryDetails.optString("flag_icon");
                    flatFlagIcon = currentCountryDetails.optString("flat_flag_icon");
                    countryFullName = currentCountryDetails.optString("country_full");
                }
                GeoviewModel m = new GeoviewModel(currentCountryCode, countryFullName, currentCountryViews, flagIcon,
                                                  flatFlagIcon);
                mCountries.add(m);
            }

            // Sort the countries by views.
            Collections.sort(mCountries, new java.util.Comparator<GeoviewModel>() {
                public int compare(GeoviewModel o1, GeoviewModel o2) {
                    // descending order
                    return o2.getViews() - o1.getViews();
                }
            });
        }
    }

    public long getBlogID() {
        return mBlogID;
    }

    public void setBlogID(long blogID) {
        mBlogID = blogID;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public List<GeoviewModel> getCountries() {
        return mCountries;
    }

    public int getOtherViews() {
        return mOtherViews;
    }

    public int getTotalViews() {
        return mTotalViews;
    }
}
