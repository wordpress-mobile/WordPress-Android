package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import androidx.annotation.NonNull;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "NotNullFieldNotInitialized"})
public class JetpackThemeResponse {
    public static class JetpackThemeListResponse {
        @NonNull public List<JetpackThemeResponse> themes;
        public int count;
    }

    @NonNull public String id;
    @NonNull public String screenshot;
    @NonNull public String name;
    @NonNull public String theme_uri;
    @NonNull public String description;
    @NonNull public String author;
    @NonNull public String author_uri;
    @NonNull public String version;
    public boolean active;
    public boolean autoupdate;
    public boolean autoupdate_translation;
}
