package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "NotNullFieldNotInitialized"})
public class WPComThemeResponse {
    public static class WPComThemeListResponse {
        @NonNull public List<WPComThemeResponse> themes;
    }

    public static class WPComThemeMobileFriendlyTaxonomy {
        @NonNull public String name;
        @NonNull public String slug;
    }

    public static class WPComThemeTaxonomies {
        @SerializedName("theme_mobile-friendly")
        @Nullable public WPComThemeMobileFriendlyTaxonomy[] theme_mobile_friendly;
    }

    public static class WPComThemeTier {
        @Nullable public String slug;
        @Nullable public String feature;
        @Nullable public String platform;
    }

    @NonNull public String id;
    @Nullable public String slug;
    @Nullable public String stylesheet;
    @NonNull public String name;
    @Nullable public String author;
    @Nullable public String author_uri;
    @Nullable public String theme_uri;
    @Nullable public String demo_uri;
    @Nullable public String version;
    @NonNull public String screenshot;
    @Nullable public String theme_type;
    @NonNull public String description;
    @Nullable public String download_uri;
    @Nullable public String price;
    @Nullable public WPComThemeTaxonomies taxonomies;
    @Nullable public WPComThemeTier theme_tier;
}
