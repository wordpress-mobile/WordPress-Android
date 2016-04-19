package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.regex.Pattern;

public class ReaderTag implements Serializable, FilterCriteria {
    private String tagSlug;     // tag for API calls, ex: "news-current-events"
    private String tagTitle;    // tag for display, ex: "News & Current Events"
    private String endpoint;    // endpoint for updating posts with this tag
    public final ReaderTagType tagType;

    // these are the default tags, which aren't localized in the /read/menu/ response
    private static final String TAG_TITLE_LIKED = "Posts I Like";
    private static final String TAG_TITLE_DISCOVER = "Discover";
    public  static final String TAG_TITLE_DEFAULT = TAG_TITLE_DISCOVER;
    public  static final String TAG_TITLE_FOLLOWED_SITES = "Followed Sites";

    public ReaderTag(String tagSlug,
                     String tagTitle,
                     String endpoint,
                     ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagSlug)) {
            this.setTagSlug(getTagSlugFromEndpoint(endpoint));
        } else {
            this.setTagSlug(tagSlug);
        }
        this.setTagTitle(tagTitle);
        this.setEndpoint(endpoint);
        this.tagType = tagType;
    }

    public String getEndpoint() {
        return StringUtils.notNullStr(endpoint);
    }
    void setEndpoint(String endpoint) {
        this.endpoint = StringUtils.notNullStr(endpoint);
    }

    public String getTagTitle() {
        return StringUtils.notNullStr(tagTitle);
    }
    void setTagTitle(String title) {
        this.tagTitle = StringUtils.notNullStr(title);
    }

    public String getTagSlug() {
        return StringUtils.notNullStr(tagSlug);
    }
    void setTagSlug(String slug) {
        this.tagSlug = StringUtils.notNullStr(slug);
    }

    /*
     * when displaying a tag name, we want to use the title when available since it's often
     * more user-friendly
     */
    public String getTagDisplayName() {
        if (!TextUtils.isEmpty(tagTitle)) {
            return tagTitle;
        }
        if (TextUtils.isEmpty(tagSlug)) {
            return "";
        }
        // If already uppercase, assume correctly formatted
        if (Character.isUpperCase(tagSlug.charAt(0))) {
            return tagSlug;
        }
        // Accounts for iPhone, ePaper, etc.
        if (tagSlug.length() > 1 &&
                Character.isLowerCase(tagSlug.charAt(0)) &&
                Character.isUpperCase(tagSlug.charAt(1))) {
            return tagSlug;
        }
        // Capitalize anything else.
        return StringUtils.capitalize(tagSlug);
    }

    /*
     * returns the tag name for use in the application log - if this is a default tag it returns
     * the full tag name, otherwise it abbreviates the tag name since exposing followed tags
     * in the log could be considered a privacy issue
     */
    public String getTagNameForLog() {
        String tagSlug = getTagSlug();
        if (tagType == ReaderTagType.DEFAULT) {
            return tagSlug;
        } else if (tagSlug.length() >= 6) {
            return tagSlug.substring(0, 3) + "...";
        } else if (tagSlug.length() >= 4) {
            return tagSlug.substring(0, 2) + "...";
        } else if (tagSlug.length() >= 2) {
            return tagSlug.substring(0, 1) + "...";
        } else {
            return "...";
        }
    }

    /*
     * used to ensure a tag name is valid before adding it
     */
    private static final Pattern INVALID_CHARS = Pattern.compile("^.*[~#@*+%{}<>\\[\\]|\"\\_].*$");
    public static boolean isValidTagName(String tagName) {
        return !TextUtils.isEmpty(tagName)
            && !INVALID_CHARS.matcher(tagName).matches();
    }

    /*
     * extracts the tag slug from a valid read/tags/[tagSlug]/posts endpoint
     */
    private static String getTagSlugFromEndpoint(final String endpoint) {
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
     * is the passed string one of the default tags?
     */
    public static boolean isDefaultTagTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            return false;
        }
        return (title.equalsIgnoreCase(TAG_TITLE_FOLLOWED_SITES)
                || title.equalsIgnoreCase(TAG_TITLE_DISCOVER)
                || title.equalsIgnoreCase(TAG_TITLE_LIKED));
    }

    public static boolean isSameTag(ReaderTag tag1, ReaderTag tag2) {
        if (tag1 == null || tag2 == null) {
            return false;
        }
        return tag1.tagType == tag2.tagType
            && tag1.getTagSlug().equalsIgnoreCase(tag2.getTagSlug());
    }

    public boolean isPostsILike() {
        return tagType == ReaderTagType.DEFAULT && getEndpoint().endsWith("/read/liked");
    }
    public boolean isFollowedSites() {
        return tagType == ReaderTagType.DEFAULT && getEndpoint().endsWith("/read/following");
    }

    public boolean isDiscover() {
        return tagType == ReaderTagType.DEFAULT && getTagSlug().equals(TAG_TITLE_DISCOVER);
    }

    public boolean isTagTopic() {
        String endpoint = getEndpoint();
        return endpoint.toLowerCase().contains("/read/tags/");
    }
    public boolean isListTopic() {
        String endpoint = getEndpoint();
        return endpoint.toLowerCase().contains("/read/list/");
    }

    /*
     * the label is the text displayed in the dropdown filter
     */
    @Override
    public String getLabel() {
        return getTagDisplayName();
    }

    @Override
    public boolean equals(Object tag){
        if (tag == null) return false;

        if (!ReaderTag.class.isAssignableFrom(tag.getClass())) return false;

        return getLabel().equals(((ReaderTag) tag).getLabel());
    }
}
