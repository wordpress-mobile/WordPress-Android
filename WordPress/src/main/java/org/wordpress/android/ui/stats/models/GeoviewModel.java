package org.wordpress.android.ui.stats.models;

import java.io.Serializable;

/**
 * A model to represent a geoview stat.
 */
public class GeoviewModel implements Serializable {
    private final String mCountryShortName;
    private final String mCountryFullName;
    private int mViews;
    private String mImageUrl;

    public GeoviewModel(String countryShortName, String countryFullName, int views, String imageUrl) {
        this.mCountryShortName = countryShortName;
        this.mCountryFullName = countryFullName;
        this.mViews = views;
        this.mImageUrl = imageUrl;
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

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = imageUrl;
    }
}
