package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class JetpackThemeResponse {
    public static class JetpackThemeListResponse {
        public List<JetpackThemeResponse> themes;
        public int count;
    }

    public String id;
    public String screenshot;
    public String name;
    public String theme_uri;
    public String description;
    public String author;
    public String author_uri;
    public String version;
    public boolean active;
    public boolean autoupdate;
    public boolean autoupdate_translation;
}
