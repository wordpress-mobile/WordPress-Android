package org.wordpress.android.fluxc.network.rest.wpcom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum WPCOMREST {
    // Me
    ME("/me/"),
    ME_SETTINGS("/me/settings/"),
    ME_SITES("/me/sites/"),

    // Posts
    POSTS("/sites/$site/posts/"),
    POST_NEW("/sites/$site/posts/new"),
    POST_DELETE("/sites/$site/posts/$post_ID/delete"),

    // Sites
    SITES("/sites/"),
    SITES_NEW("/sites/new"),

    // Users
    USERS_NEW("/users/new"),

    // Media
    MEDIA_ALL("/sites/%s/media"),
    MEDIA_ITEM("/sites/%s/media/%s"),
    MEDIA_NEW("/sites/%s/media/new"),
    MEDIA_DELETE("/sites/%s/media/%s/delete");

    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com/rest";
    private static final String WPCOM_PREFIX_V1 = WPCOM_REST_PREFIX + "/v1";
    private static final String WPCOM_PREFIX_V1_1 = WPCOM_REST_PREFIX + "/v1.1";

    private final String mEndpoint;

    WPCOMREST(String endpoint) {
        mEndpoint = endpoint;
    }

    @Override
    public String toString() {
        return mEndpoint;
    }

    public String getUrlV1_1(String...args) {
        return WPCOM_PREFIX_V1_1 + String.format(mEndpoint, args);
    }
    private String withParams(long... params) {
        if (params == null || params.length == 0) {
            return mEndpoint;
        }
        Pattern urlPattern = Pattern.compile("(\\$[^\\/]*)");
        Matcher matcher = urlPattern.matcher(mEndpoint);
        StringBuffer stringBuffer = new StringBuffer();
        int lastMatch = 0;
        while (matcher.find() && params.length > lastMatch) {
            String replacement = Long.toString(params[lastMatch]);
            matcher.appendReplacement(stringBuffer, replacement);
            lastMatch++;
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    public String withSiteId(long siteId) {
        return withParams(siteId);
    }

    public String withSiteIdAndContentId(long siteId, long contentId) {
        return withParams(siteId, contentId);
    }

    public String getUrlV1() {
        return WPCOM_PREFIX_V1 + mEndpoint;
    }

    public String getUrlV1WithSiteId(long siteId) {
        return WPCOM_PREFIX_V1 + withParams(siteId);
    }

    public String getUrlV1WithSiteIdAndContentId(long siteId, long contentId) {
        return WPCOM_PREFIX_V1 + withParams(siteId, contentId);
    }

    public String getUrlV1_1() {
        return WPCOM_PREFIX_V1_1 + mEndpoint;
    }
}
