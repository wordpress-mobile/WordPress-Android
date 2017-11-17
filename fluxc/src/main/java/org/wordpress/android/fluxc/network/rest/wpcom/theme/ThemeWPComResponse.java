package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class ThemeWPComResponse {
    public class MultipleWPComThemesResponse {
        public Map<String, ThemeWPComResponse> themes;
        public int count;
    }

    public class ThemeArrayResponse {
        public List<ThemeWPComResponse> themes;
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
}
