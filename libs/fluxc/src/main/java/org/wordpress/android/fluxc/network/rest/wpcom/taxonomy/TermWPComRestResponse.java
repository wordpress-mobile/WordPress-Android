package org.wordpress.android.fluxc.network.rest.wpcom.taxonomy;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;

@SuppressWarnings("NotNullFieldNotInitialized")
public class TermWPComRestResponse implements Response {
    public static class TermsResponse {
        @NonNull public List<TermWPComRestResponse> terms;
    }

    public long ID;
    @NonNull public String name;
    @NonNull public String slug;
    @NonNull public String description;
    public int post_count;
    public long parent;
}
