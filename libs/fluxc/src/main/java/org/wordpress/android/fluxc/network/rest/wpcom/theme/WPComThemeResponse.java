package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import com.google.gson.annotations.SerializedName;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class WPComThemeResponse {
    public class WPComThemeListResponse {
        public List<WPComThemeResponse> themes;
    }

    public class WPComThemeMobileFriendlyTaxonomy {
        public String name;
        public String slug;
        public int term_id;
    }

    public class WPComThemeTaxonomies {
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
    public String theme_type;
    public String description;
    public String download_uri;
    public String price;
    public WPComThemeTaxonomies taxonomies;
}
