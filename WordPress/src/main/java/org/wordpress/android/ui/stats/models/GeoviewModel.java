package org.wordpress.android.ui.stats.models;

import java.io.Serializable;

/**
 * A model to represent a geoview stat.
 */
public class GeoviewModel implements Serializable {
    private final String mCountryShortName;
    private final String mCountryFullName;
    private int mViews;
    private final String mFlagIconURL;
    private final String mFlatFlagIconURL;

    public GeoviewModel(String countryShortName, String countryFullName, int views, String flagIcon, String flatFlagIcon) {
        this.mCountryShortName = countryShortName;
        this.mCountryFullName = countryFullName;
        this.mViews = views;
        this.mFlagIconURL = flagIcon;
        this.mFlatFlagIconURL = flatFlagIcon;
    }

    public String getCountryFullName() {
        return mCountryFullName;
    }

    public String getCountryShortName() {
        return mCountryShortName;
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }

    public String getFlagIconURL() {
        return mFlagIconURL;
    }

    public String getFlatFlagIconURL() {
        return mFlatFlagIconURL;
    }

}
