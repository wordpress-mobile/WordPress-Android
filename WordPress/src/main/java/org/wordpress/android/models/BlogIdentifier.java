package org.wordpress.android.models;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * A blog is uniquely identified by the combination of xmlRpcUrl and blogId
 */
public class BlogIdentifier {
    private String mXmlRpcUrl;
    private int mBlogId;

    public BlogIdentifier(String mXmlRpcUrl, int mBlogId) {
        this.mXmlRpcUrl = mXmlRpcUrl;
        this.mBlogId = mBlogId;
    }

    public String getXmlRpcUrl() {
        return mXmlRpcUrl;
    }

    public void setXmlRpcUrl(String mXmlRpcUrl) {
        this.mXmlRpcUrl = mXmlRpcUrl;
    }

    public int getBlogId() {
        return mBlogId;
    }

    public void setBlogId(int mBlogId) {
        this.mBlogId = mBlogId;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) { // same instance
            return true;
        }
        if (!(other instanceof BlogIdentifier)) {
            return false;
        }
        BlogIdentifier o = (BlogIdentifier) other;
        return mXmlRpcUrl.equals(o.getXmlRpcUrl()) && mBlogId == o.getBlogId();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3739, 50989).append(mBlogId).append(mXmlRpcUrl).toHashCode();
    }
}