package org.wordpress.android.ui.stats.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class GeoviewsModel implements Serializable {
    private String date;
    private String blogID;
    private int otherViews;
    private int totalViews;
    private List<GeoviewModel> countries;

    public GeoviewsModel(String blogID, JSONObject response) throws JSONException {
        this.blogID = blogID;
        this.date = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject == null || jDaysObject.length() == 0) {
            //FIXME: ???
            return;
        }

        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String firstDayKey = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(firstDayKey);
        this.otherViews = firstDayObject.optInt("other_clicks");
        this.totalViews = firstDayObject.optInt("total_clicks");

        JSONObject countryInfoJSON = response.optJSONObject("country-info");
        JSONObject viewsJSON = firstDayObject.optJSONObject("views");

        if (viewsJSON != null) {
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
        }
    }

    public String getBlogID() {
        return blogID;
    }

    public void setBlogID(String blogID) {
        this.blogID = blogID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<GeoviewModel> getCountries() {
        return this.countries;
    }
}
