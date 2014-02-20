package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Created by nbradbury on 6/23/13.
 */
public class ReaderTag {
    private static final int INT_DEFAULT = 0;
    private static final int INT_SUBSCRIBED = 1;
    private static final int INT_RECOMMENDED = 2;

    public static String TAG_ID_FOLLOWING = "following";
    public static String TAG_ID_LIKED = "liked";

    // these are the default tag names, which aren't localized in the /read/menu/ response
    public static final String TAG_NAME_LIKED = "Posts I Like";
    public static final String TAG_NAME_FOLLOWING = "Blogs I Follow";
    public static final String TAG_NAME_FRESHLY_PRESSED = "Freshly Pressed";
    public static final String TAG_NAME_DEFAULT = TAG_NAME_FRESHLY_PRESSED;

    public static enum ReaderTagType {SUBSCRIBED,
                                      DEFAULT,
                                      RECOMMENDED;
        public static ReaderTagType fromInt(int value) {
            switch (value) {
                case INT_RECOMMENDED :
                    return RECOMMENDED;
                case INT_SUBSCRIBED :
                    return SUBSCRIBED;
                default :
                    return DEFAULT;
            }
        }
        public int toInt() {
            switch (this) {
                case SUBSCRIBED:
                    return INT_SUBSCRIBED;
                case RECOMMENDED:
                    return INT_RECOMMENDED;
                default :
                    return INT_DEFAULT;
            }
        }
    }

    private String tagName;
    private String endpoint;
    public ReaderTagType tagType;

    public ReaderTag(String tagName, String endpoint, ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagName)) {
            this.setTagName(getTagNameFromEndpoint(endpoint));
        } else {
            this.setTagName(tagName);
        }
        this.setEndpoint(endpoint);
        this.tagType = tagType;
    }

    public String getEndpoint() {
        return StringUtils.notNullStr(endpoint);
    }
    public void setEndpoint(String endpoint) {
        this.endpoint = StringUtils.notNullStr(endpoint);
    }

    /**
     * Extract tag Id from endpoint, only works for ReaderTagType.DEFAULT
     *
     * @return a string Id if tagType is ReaderTagType.DEFAULT, empty string else
     */
    public String getStringIdFromEndpoint() {
        if (tagType != ReaderTagType.DEFAULT) {
            return "";
        }
        String[] splitted = endpoint.split("/");
        if (splitted != null && splitted.length > 0) {
            return splitted[splitted.length - 1];
        }
        return "";
    }

    public String getTagName() {
        return StringUtils.notNullStr(tagName);
    }
    public void setTagName(String name) {
        this.tagName = StringUtils.notNullStr(name);
    }
    public String getCapitalizedTagName() {
        if (tagName == null)
            return "";
        // HACK to allow iPhone, iPad, iEverything else
        if (tagName.startsWith("iP"))
            return tagName;
        return StringUtils.capitalize(tagName);
    }

    /*
     * used to ensure a tag name is valid before adding it
     */
    private static final Pattern INVALID_CHARS = Pattern.compile("^.*[~#@*+%{}<>\\[\\]|\"\\_].*$");
    public static boolean isValidTagName(String tagName) {
        if (TextUtils.isEmpty(tagName))
            return false;
        if (INVALID_CHARS.matcher(tagName).matches())
            return false;
        return true;
    }

    /*
     * extracts the tag name from a valid read/tags/[tagName]/posts endpoint
     */
    private static String getTagNameFromEndpoint(final String endpoint) {
        if (TextUtils.isEmpty(endpoint))
            return "";

        // make sure passed endpoint is valid
        if (!endpoint.endsWith("/posts"))
            return "";
        int start = endpoint.indexOf("/read/tags/");
        if (start == -1)
            return "";

        // skip "/read/tags/" then find the next "/"
        start += 11;
        int end = endpoint.indexOf("/", start);
        if (end == -1)
            return "";

        return endpoint.substring(start, end);
    }
}
