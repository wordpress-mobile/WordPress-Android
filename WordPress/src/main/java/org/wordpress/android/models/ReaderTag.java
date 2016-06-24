package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.regex.Pattern;

public class ReaderTag implements Serializable, FilterCriteria {
    private String tagSlug;         // tag for API calls
    private String tagDisplayName;  // tag for display, usually the same as the slug
    private String tagTitle;        // title, used for default tags
    private String endpoint;        // endpoint for updating posts with this tag
    public final ReaderTagType tagType;

    // these are the default tags, which aren't localized in the /read/menu/ response
    private static final String TAG_TITLE_LIKED = "Posts I Like";
    private static final String TAG_TITLE_DISCOVER = "Discover";
    public  static final String TAG_TITLE_DEFAULT = TAG_TITLE_DISCOVER;
    public  static final String TAG_TITLE_FOLLOWED_SITES = "Followed Sites";

    public ReaderTag(String slug,
                     String displayName,
                     String title,
                     String endpoint,
                     ReaderTagType tagType) {
        // we need a slug since it's used to uniquely ID the tag (including setting it as the
        // primary key in the tag table)
        if (TextUtils.isEmpty(slug)) {
            if (!TextUtils.isEmpty(title)) {
                setTagSlug(ReaderUtils.sanitizeWithDashes(title));
            } else {
                setTagSlug(getTagSlugFromEndpoint(endpoint));
            }
        } else {
            setTagSlug(slug);
        }

        setTagDisplayName(displayName);
        setTagTitle(title);
        setEndpoint(endpoint);
        this.tagType = tagType;
    }

    public String getEndpoint() {
        return StringUtils.notNullStr(endpoint);
    }
    private void setEndpoint(String endpoint) {
        this.endpoint = StringUtils.notNullStr(endpoint);
    }

    public String getTagTitle() {
        return StringUtils.notNullStr(tagTitle);
    }
    private void setTagTitle(String title) {
        this.tagTitle = StringUtils.notNullStr(title);
    }
    private boolean hasTagTitle() {
        return !TextUtils.isEmpty(tagTitle);
    }

    public String getTagDisplayName() {
        return StringUtils.notNullStr(tagDisplayName);
    }
    private void setTagDisplayName(String displayName) {
        this.tagDisplayName = StringUtils.notNullStr(displayName);
    }

    public String getTagSlug() {
        return StringUtils.notNullStr(tagSlug);
    }
    private void setTagSlug(String slug) {
        this.tagSlug = StringUtils.notNullStr(slug);
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
        if (tagType == ReaderTagType.DEFAULT) {
            return getTagTitle();
        } else if (isTagDisplayNameAlphaNumeric()) {
            return getTagDisplayName().toLowerCase();
        } else if (hasTagTitle()) {
            return getTagTitle();
        } else {
            return getTagDisplayName();
        }
    }

    /*
     * returns true if the tag display name contains only alpha-numeric characters or hyphens
     */
    private boolean isTagDisplayNameAlphaNumeric() {
        if (TextUtils.isEmpty(tagDisplayName)) {
            return false;
        }

        for (int i=0; i < tagDisplayName.length(); i++) {
            char c = tagDisplayName.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-') {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object object){
        if (object instanceof ReaderTag) {
            ReaderTag tag = (ReaderTag) object;
            return (tag.tagType == this.tagType && tag.getLabel().equals(this.getLabel()));
        } else {
            return false;
        }
    }
}
