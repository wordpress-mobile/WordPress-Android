package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Created by nbradbury on 6/23/13.
 */
public class ReaderTopic {
    private static final int INT_DEFAULT = 0;
    private static final int INT_SUBSCRIBED = 1;
    private static final int INT_RECOMMENDED = 2;

    public static enum ReaderTopicType {SUBSCRIBED,
                                        DEFAULT,
                                        RECOMMENDED;
        public static ReaderTopicType fromInt(int value) {
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

    private String topicName;
    private String endpoint;
    public ReaderTopicType topicType;

    public String getEndpoint() {
        return StringUtils.notNullStr(endpoint);
    }
    public void setEndpoint(String endpoint) {
        this.endpoint = StringUtils.notNullStr(endpoint);
    }

    public String getTopicName() {
        return StringUtils.notNullStr(topicName);
    }
    public void setTopicName(String name) {
        this.topicName = StringUtils.notNullStr(name);
    }
    public String getCapitalizedTopicName() {
        if (topicName==null)
            return "";
        // HACK to allow iPhone, iPad, iEverything else
        if (topicName.startsWith("iP"))
            return topicName;
        return StringUtils.capitalize(topicName);
    }

    /*
     * used to ensure a topic name is valid before adding it
     */
    private static final Pattern INVALID_CHARS = Pattern.compile("^.*[~#@*+%{}<>\\[\\]|\"\\_].*$");
    public static boolean isValidTopicName(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return false;
        if (INVALID_CHARS.matcher(topicName).matches())
            return false;
        return true;
    }
}
