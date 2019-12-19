package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

@Table
@RawConstraints({"UNIQUE (FEED_ID, URL)"})
public class SubscriptionModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = -3258001887519449586L;

    @PrimaryKey
    @Column private int mId;
    @Column private String mSubscriptionId;
    @Column private String mBlogId;
    @Column private String mBlogName;
    @Column private String mFeedId;
    @Column private String mUrl;

    // Delivery Methods
    @Column private boolean mShouldNotifyPosts;
    @Column private boolean mShouldEmailPosts;
    @Column private String mEmailPostsFrequency;
    @Column private boolean mShouldEmailComments;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public String getSubscriptionId() {
        return mSubscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        mSubscriptionId = subscriptionId;
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        mBlogId = blogId;
    }

    public String getBlogName() {
        return mBlogName;
    }

    public void setBlogName(String blogName) {
        mBlogName = blogName;
    }

    public String getFeedId() {
        return mFeedId;
    }

    public void setFeedId(String feedId) {
        mFeedId = feedId;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(@NonNull String url) {
        try {
            // Normalize the URL, because it can be used as an identifier.
            mUrl = (new URI(url)).normalize().toString();
        } catch (URISyntaxException exception) {
            // Don't set the URL.
            AppLog.e(T.API, "Trying to set an invalid url: " + url);
        }
    }

    public boolean getShouldNotifyPosts() {
        return mShouldNotifyPosts;
    }

    public void setShouldNotifyPosts(boolean shouldNotifyPosts) {
        mShouldNotifyPosts = shouldNotifyPosts;
    }

    public boolean getShouldEmailPosts() {
        return mShouldEmailPosts;
    }

    public void setShouldEmailPosts(boolean shouldEmailPosts) {
        mShouldEmailPosts = shouldEmailPosts;
    }

    public String getEmailPostsFrequency() {
        return mEmailPostsFrequency;
    }

    public void setEmailPostsFrequency(String emailPostsFrequency) {
        mEmailPostsFrequency = emailPostsFrequency;
    }

    public boolean getShouldEmailComments() {
        return mShouldEmailComments;
    }

    public void setShouldEmailComments(boolean shouldEmailComments) {
        mShouldEmailComments = shouldEmailComments;
    }
}
