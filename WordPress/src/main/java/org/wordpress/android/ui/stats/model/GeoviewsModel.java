package org.wordpress.android.ui.stats.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class GeoviewsModel implements Serializable {
    private String mDate;
    private String mBlogID;
    private int otherViews;
    private int totalViews;
    private List<GeoviewModel> countries;

    public GeoviewsModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mDate = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String firstDayKey = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(firstDayKey);
        this.otherViews = firstDayObject.getInt("other_views");
        this.totalViews = firstDayObject.getInt("total_views");

        JSONObject countryInfoJSON = response.optJSONObject("country-info");
        JSONObject viewsJSON = firstDayObject.optJSONObject("views");

        if (viewsJSON != null && countryInfoJSON != null) {
            countries = new ArrayList<GeoviewModel>(viewsJSON.length());
            Iterator<String> countryKeys = viewsJSON.keys();

            while (countryKeys.hasNext()) {
                String currentCountryKey = countryKeys.next();
                int views = viewsJSON.getInt(currentCountryKey);
                String flagIcon = null;
                String countryFullName = null;
                JSONObject currentCountryDetails = countryInfoJSON.optJSONObject(currentCountryKey);
                if (currentCountryDetails != null) {
                    flagIcon = currentCountryDetails.optString("flag_icon");
                    countryFullName = currentCountryDetails.optString("country_full");
                }
                GeoviewModel m = new GeoviewModel(currentCountryKey, countryFullName, views, flagIcon);
                countries.add(m);
            }

            // Sort the countries by views.
            Collections.sort(countries, new java.util.Comparator<GeoviewModel>() {
                public int compare(GeoviewModel o1, GeoviewModel o2) {
                    // descending order
                    return o2.getViews() - o1.getViews();
                }
            });
        }
    }

    public String getBlogID() {
        return mBlogID;
    }

    public void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public List<GeoviewModel> getCountries() {
        return this.countries;
    }
}
