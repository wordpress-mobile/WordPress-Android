package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;

public class ReaderTag implements Serializable, FilterCriteria {
    public static final String FOLLOWING_PATH = "/read/following";
    public static final String LIKED_PATH = "/read/liked";
    public static final String DISCOVER_PATH = String.format(Locale.US, "read/sites/%d/posts",
            ReaderConstants.DISCOVER_SITE_ID);

    public static final String TAG_TITLE_FOLLOWED_SITES = "Followed Sites";
    public static final String TAG_SLUG_BOOKMARKED = "bookmarked-posts";
    public static final String TAG_TITLE_DEFAULT = TAG_TITLE_FOLLOWED_SITES;
    public static final String TAG_ENDPOINT_DEFAULT = FOLLOWING_PATH;

    private String mTagSlug; // tag for API calls
    private String mTagDisplayName; // tag for display, usually the same as the slug
    private String mTagTitle; // title, used for default tags
    private String mEndpoint; // endpoint for updating posts with this tag

    private boolean mIsDefaultInMemoryTag;

    public final ReaderTagType tagType;

    public ReaderTag(String slug,
                     String displayName,
                     String title,
                     String endpoint,
                     ReaderTagType tagType) {
        this(slug, displayName, title, endpoint, tagType, false);
    }

    public ReaderTag(String slug,
                     String displayName,
                     String title,
                     String endpoint,
                     ReaderTagType tagType,
                     boolean isDefaultInMemoryTag) {
        // we need a slug since it's used to uniquely ID the tag (including setting it as the
        // primary key in the tag table)
        if (TextUtils.isEmpty(slug)) {
            if (tagType == ReaderTagType.BOOKMARKED) {
                setTagSlug(TAG_SLUG_BOOKMARKED);
            } else if (!TextUtils.isEmpty(title)) {
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
        mIsDefaultInMemoryTag = isDefaultInMemoryTag;
    }

    public String getEndpoint() {
        return StringUtils.notNullStr(mEndpoint);
    }

    public void setEndpoint(String endpoint) {
        this.mEndpoint = StringUtils.notNullStr(endpoint);
    }

    public String getTagTitle() {
        return StringUtils.notNullStr(mTagTitle);
    }

    public void setTagTitle(String title) {
        this.mTagTitle = StringUtils.notNullStr(title);
    }

    private boolean hasTagTitle() {
        return !TextUtils.isEmpty(mTagTitle);
    }

    public String getTagDisplayName() {
        return StringUtils.notNullStr(mTagDisplayName);
    }

    public void setTagDisplayName(String displayName) {
        this.mTagDisplayName = StringUtils.notNullStr(displayName);
    }

    public String getTagSlug() {
        return StringUtils.notNullStr(mTagSlug);
    }

    private void setTagSlug(String slug) {
        this.mTagSlug = StringUtils.notNullStr(slug);
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
        } else if (tagType == ReaderTagType.BOOKMARKED) {
            return ReaderTagType.BOOKMARKED.name();
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
    @SuppressWarnings("RegExpRedundantEscape") private static final Pattern INVALID_CHARS =
            Pattern.compile("^.*[~#@*+%{}<>\\[\\]|\"\\_].*$");

    public static boolean isValidTagName(String tagName) {
        return !TextUtils.isEmpty(tagName)
               && !INVALID_CHARS.matcher(tagName).matches();
    }

    /*
     * extracts the tag slug from a valid read/tags/[mTagSlug]/posts endpoint
     */
    private static String getTagSlugFromEndpoint(final String endpoint) {
        if (TextUtils.isEmpty(endpoint)) {
            return "";
        }

        // make sure passed endpoint is valid
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
        return tag1 != null && tag2 != null && tag1.tagType == tag2.tagType && tag1.getTagSlug()
                                                                                   .equalsIgnoreCase(tag2.getTagSlug());
    }

    public boolean isPostsILike() {
        return tagType == ReaderTagType.DEFAULT && getEndpoint().endsWith(LIKED_PATH);
    }

    public boolean isFollowedSites() {
        return tagType == ReaderTagType.DEFAULT && getEndpoint().endsWith(FOLLOWING_PATH);
    }

    public boolean isDefaultInMemoryTag() {
        return tagType == ReaderTagType.DEFAULT && mIsDefaultInMemoryTag;
    }

    public boolean isBookmarked() {
        return tagType == ReaderTagType.BOOKMARKED;
    }

    public boolean isDiscover() {
        return tagType == ReaderTagType.DEFAULT && getEndpoint().endsWith(DISCOVER_PATH);
    }

    public boolean isTagTopic() {
        String endpoint = getEndpoint();
        return endpoint.toLowerCase(Locale.ROOT).contains("/read/tags/");
    }

    public boolean isListTopic() {
        String endpoint = getEndpoint();
        return endpoint.toLowerCase(Locale.ROOT).contains("/read/list/");
    }

    /*
     * the label is the text displayed in the dropdown filter
     */
    @Override
    public String getLabel() {
        if (isTagDisplayNameAlphaNumeric()) {
            return getTagDisplayName().toLowerCase(Locale.ROOT);
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

    /**
     * Local only tag used to identify post cards for the "discover" tab in the Reader.
     */
    public static ReaderTag createDiscoverPostCardsTag() {
        return new ReaderTag("", "", "", "", ReaderTagType.DISCOVER_POST_CARDS);
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
