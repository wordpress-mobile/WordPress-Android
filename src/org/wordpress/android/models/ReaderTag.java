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

    public String getEndpoint() {
        return StringUtils.notNullStr(endpoint);
    }
    public void setEndpoint(String endpoint) {
        this.endpoint = StringUtils.notNullStr(endpoint);
    }

    public String getTagName() {
        return StringUtils.notNullStr(tagName);
    }
    public void setTagName(String name) {
        this.tagName = StringUtils.notNullStr(name);
    }
    public String getCapitalizedTagName() {
        if (tagName ==null)
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
}
