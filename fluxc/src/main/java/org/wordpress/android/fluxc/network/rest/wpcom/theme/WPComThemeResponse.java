package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import com.google.gson.annotations.SerializedName;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class WPComThemeResponse {
    public static class WPComThemeListResponse {
        public List<WPComThemeResponse> themes;
    }

    public static class WPComThemeMobileFriendlyTaxonomy {
        public String name;
        public String slug;
    }

    public static class WPComThemeTaxonomies {
        @SerializedName("theme_mobile-friendly")
        public WPComThemeMobileFriendlyTaxonomy[] theme_mobile_friendly;
    }

    public String id;
    public String slug;
    public String stylesheet;
    public String name;
    public String author;
    public String author_uri;
    public String theme_uri;
    public String demo_uri;
    public String version;
    public String screenshot;
    public String description;
    public String download_uri;
    public String price;
    public WPComThemeTaxonomies taxonomies;
}
