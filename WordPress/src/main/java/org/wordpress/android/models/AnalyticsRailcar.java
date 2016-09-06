package org.wordpress.android.models;


import org.wordpress.android.util.StringUtils;

public class AnalyticsRailcar {

    private String railcarId;       // a unique ID that we use for matching render and interaction events

    private String fetchAlg;        // string identifying the algorithm that generated this
    private String fetchLang;       // the language(s) the algorithm was performed against
    private String fetchQuery;      // the user entered query

    private int fetchPosition;      // The ranked position within the results

    private long blogId;            // the blog id for this result
    private long postId;            // the post id for this result
    private long feedId;            // the feed id for this result
    private long feedItemId;        // the feed item id for this result

    public String getRailcarId() {
        return StringUtils.notNullStr(railcarId);
    }

    public void setRailcarId(String railcarId) {
        this.railcarId = railcarId;
    }

    public String getFetchAlg() {
        return StringUtils.notNullStr(fetchAlg);
    }

    public void setFetchAlg(String fetchAlg) {
        this.fetchAlg = fetchAlg;
    }

    public String getFetchLang() {
        return StringUtils.notNullStr(fetchLang);
    }

    public void setFetchLang(String fetchLang) {
        this.fetchLang = fetchLang;
    }

    public String getFetchQuery() {
        return StringUtils.notNullStr(fetchQuery);
    }

    public void setFetchQuery(String fetchQuery) {
        this.fetchQuery = fetchQuery;
    }

    public int getFetchPosition() {
        return fetchPosition;
    }

    public void setFetchPosition(int fetchPosition) {
        this.fetchPosition = fetchPosition;
    }

    public long getBlogId() {
        return blogId;
    }

    public void setBlogId(long blogId) {
        this.blogId = blogId;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public long getFeedItemId() {
        return feedItemId;
    }

    public void setFeedItemId(long feedItemId) {
        this.feedItemId = feedItemId;
    }

}
