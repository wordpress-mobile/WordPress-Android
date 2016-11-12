package org.wordpress.android.models;

import java.util.Map;

/**
 * A Model for parsing the result of wpcom.getFeatures() to retrieve
 * features for a hosted WordPress.com blog.
 */
public class FeatureSet {
    private int mBlogId;

    private boolean mIsVideopressEnabled = false;
    // add future features here

    public FeatureSet(int blogId, Map<?,?> map) {
        setBlogId(blogId);

        if (map.containsKey("videopress_enabled"))
            setIsVideopressEnabled((Boolean) map.get("videopress_enabled"));

    }

    public boolean isVideopressEnabled() {
        return mIsVideopressEnabled;
    }

    public void setIsVideopressEnabled(boolean enabled) {
        this.mIsVideopressEnabled = enabled;
    }

    public int getBlogId() {
        return mBlogId;
    }

    public void setBlogId(int blogId) {
        this.mBlogId = blogId;
    }
}
