package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.regex.Pattern;

public class ReaderTag implements Serializable, FilterCriteria {
    private static final String TAG_TITLE_DISCOVER = "Discover";
    public static final String TAG_TITLE_FOLLOWED_SITES = "Followed Sites";
    public static final String TAG_TITLE_DEFAULT = TAG_TITLE_FOLLOWED_SITES;

    private String mTagSlug; // tag for API calls
    private String mTagDisplayName; // tag for display, usually the same as the slug
    private String mTagTitle; // title, used for default tags
    private String mEndpoint; // mEndpoint for updating posts with this tag

    public final ReaderTagType tagType;

    public ReaderTag(String slug,
                     String displayName,
                     String title,
                     String endpoint,
                     ReaderTagType tagType) {
        // we need a slug since it's used to uniquely ID the tag (including setting it as the
        // primary key in the tag table)
        if (TextUtils.isEmpty(slug)) {
            if (!TextUtils.isEmpty(title)) {
                setmTagSlug(ReaderUtils.sanitizeWithDashes(title));
            } else {
                setmTagSlug(getTagSlugFromEndpoint(endpoint));
            }
        } else {
            setmTagSlug(slug);
        }

        setmTagDisplayName(displayName);
        setmTagTitle(title);
        setmEndpoint(endpoint);
        this.tagType = tagType;
    }

    public String getmEndpoint() {
        return StringUtils.notNullStr(mEndpoint);
    }

    private void setmEndpoint(String mEndpoint) {
        this.mEndpoint = StringUtils.notNullStr(mEndpoint);
    }

    public String getmTagTitle() {
        return StringUtils.notNullStr(mTagTitle);
    }

    private void setmTagTitle(String title) {
        this.mTagTitle = StringUtils.notNullStr(title);
    }

    private boolean hasTagTitle() {
        return !TextUtils.isEmpty(mTagTitle);
    }

    public String getmTagDisplayName() {
        return StringUtils.notNullStr(mTagDisplayName);
    }

    private void setmTagDisplayName(String displayName) {
        this.mTagDisplayName = StringUtils.notNullStr(displayName);
    }

    public String getmTagSlug() {
        return StringUtils.notNullStr(mTagSlug);
    }

    private void setmTagSlug(String slug) {
        this.mTagSlug = StringUtils.notNullStr(slug);
    }

    /*
     * returns the tag name for use in the application log - if this is a default tag it returns
     * the full tag name, otherwise it abbreviates the tag name since exposing followed tags
     * in the log could be considered a privacy issue
     */
    public String getTagNameForLog() {
        String tagSlug = getmTagSlug();
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
     * extracts the tag slug from a valid read/tags/[mTagSlug]/posts mEndpoint
     */
    private static String getTagSlugFromEndpoint(final String endpoint) {
        if (TextUtils.isEmpty(endpoint)) {
            return "";
        }

        // make sure passed mEndpoint is valid
        if (!endpoint.endsWith("/posts")) {
            return "";
        }
        int start = endpoint.indexOf("/read/tags/");
        if (start == -1) {
            return "";
        }

        // skip "/read/tags/" then find the next "/"
        start += 11;
        int end = endpoint.indexOf("/", start);
        if (end == -1) {
            return "";
        }

        return endpoint.substring(start, end);
    }

    public static boolean isSameTag(ReaderTag tag1, ReaderTag tag2) {
        if (tag1 == null || tag2 == null) {
            return false;
        }
        return tag1.tagType == tag2.tagType
               && tag1.getmTagSlug().equalsIgnoreCase(tag2.getmTagSlug());
    }

    public boolean isPostsILike() {
        return tagType == ReaderTagType.DEFAULT && getmEndpoint().endsWith("/read/liked");
    }

    public boolean isFollowedSites() {
        return tagType == ReaderTagType.DEFAULT && getmEndpoint().endsWith("/read/following");
    }

    public boolean isDiscover() {
        return tagType == ReaderTagType.DEFAULT && getmTagSlug().equals(TAG_TITLE_DISCOVER);
    }

    public boolean isTagTopic() {
        String endpoint = getmEndpoint();
        return endpoint.toLowerCase().contains("/read/tags/");
    }

    public boolean isListTopic() {
        String endpoint = getmEndpoint();
        return endpoint.toLowerCase().contains("/read/list/");
    }

    /*
     * the label is the text displayed in the dropdown filter
     */
    @Override
    public String getLabel() {
        if (isTagDisplayNameAlphaNumeric()) {
            return getmTagDisplayName().toLowerCase();
        } else if (hasTagTitle()) {
            return getmTagTitle();
        } else {
            return getmTagDisplayName();
        }
    }

    /*
     * returns true if the tag display name contains only alpha-numeric characters or hyphens
     */
    private boolean isTagDisplayNameAlphaNumeric() {
        if (TextUtils.isEmpty(mTagDisplayName)) {
            return false;
        }

        for (int i = 0; i < mTagDisplayName.length(); i++) {
            char c = mTagDisplayName.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-') {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ReaderTag) {
            ReaderTag tag = (ReaderTag) object;
            return (tag.tagType == this.tagType && tag.getLabel().equals(this.getLabel()));
        } else {
            return false;
        }
    }
}
