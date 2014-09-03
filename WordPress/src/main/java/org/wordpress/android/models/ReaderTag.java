package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.regex.Pattern;

public class ReaderTag implements Serializable {
    private String tagName;
    private String endpoint;
    public ReaderTagType tagType;

    public static String TAG_ID_FOLLOWING = "following";
    public static String TAG_ID_LIKED = "liked";

    // these are the default tag names, which aren't localized in the /read/menu/ response
    public static final String TAG_NAME_LIKED = "Posts I Like";
    public static final String TAG_NAME_FOLLOWING = "Blogs I Follow";
    public static final String TAG_NAME_FRESHLY_PRESSED = "Freshly Pressed";
    private static final String TAG_NAME_DEFAULT = TAG_NAME_FRESHLY_PRESSED;

    public ReaderTag(String tagName, String endpoint, ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagName)) {
            this.setTagName(getTagNameFromEndpoint(endpoint));
        } else {
            this.setTagName(tagName);
        }
        this.setEndpoint(endpoint);
        this.tagType = tagType;
    }

    public ReaderTag(String tagName, ReaderTagType tagType) {
        this.setTagName(tagName);
        this.tagType = tagType;
    }

    public static ReaderTag getDefaultTag() {
        return new ReaderTag(TAG_NAME_DEFAULT, ReaderTagType.DEFAULT);
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
        if (tagName == null) {
            return "";
        }
        // HACK to allow iPhone, iPad, iEverything else
        if (tagName.startsWith("iP")) {
            return tagName;
        }
        return StringUtils.capitalize(tagName);
    }

    /*
     * returns the tag name for use in the application log - if this is a default tag it returns
     * the full tag name, otherwise it abbreviates the tag name since exposing followed tags
     * in the log could be considered a privacy issue
     */
    public String getTagNameForLog() {
        String tagName = getTagName();
        if (tagType == ReaderTagType.DEFAULT) {
            return tagName;
        } else if (tagName.length() >= 6) {
            return tagName.substring(0, 3) + "...";
        } else if (tagName.length() >= 4) {
            return tagName.substring(0, 2) + "...";
        } else if (tagName.length() >= 2) {
            return tagName.substring(0, 1) + "...";
        } else {
            return "...";
        }
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

    /*
     * is the passed tag name one of the default tags?
     */
    public static boolean isDefaultTagName(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return false;
        }
        return (tagName.equalsIgnoreCase(TAG_NAME_FOLLOWING)
             || tagName.equalsIgnoreCase(TAG_NAME_FRESHLY_PRESSED)
             || tagName.equalsIgnoreCase(TAG_NAME_LIKED));
    }

    public static boolean isSameTag(ReaderTag tag1, ReaderTag tag2) {
        if (tag1 == null || tag2 == null) {
            return false;
        }
        return (tag1.getTagName().equalsIgnoreCase(tag2.getTagName())
             && tag1.tagType.equals(tag2.tagType));
    }
}
