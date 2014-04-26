package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;

public class ReaderRecommendedBlog {
    public long blogId;
    public long followRecoId;
    public int score;

    private String title;
    private String blogDomain;
    private String imageUrl;
    private String reason;

    public static ReaderRecommendedBlog fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }

        ReaderRecommendedBlog blog = new ReaderRecommendedBlog();

        blog.blogId = json.optLong("blog_id");
        blog.followRecoId = json.optLong("follow_reco_id");
        blog.score = json.optInt("score");

        blog.setTitle(JSONUtil.getString(json, "title"));
        blog.setImageUrl(JSONUtil.getString(json, "image"));
        blog.setReason(JSONUtil.getString(json, "reason"));
        blog.setBlogDomain(JSONUtil.getString(json, "blog_domain"));

        return blog;
    }

    public String getTitle() {
        return StringUtils.notNullStr(title);
    }
    public void setTitle(String title) {
        this.title = StringUtils.notNullStr(title);
    }

    public String getReason() {
        return StringUtils.notNullStr(reason);
    }
    public void setReason(String reason) {
        this.reason = StringUtils.notNullStr(reason);
    }

    public String getBlogDomain() {
        return StringUtils.notNullStr(blogDomain);
    }
    public void setBlogDomain(String domain) {
        if (domain == null) {
            this.blogDomain = "";
        } else {
            // remove http:// from the domain (JSON response will have it)
            int pos = domain.indexOf("://");
            if (pos > 0) {
                this.blogDomain = domain.substring(pos + 3);
            } else {
                this.blogDomain = StringUtils.notNullStr(domain);
            }
        }
    }

    public String getImageUrl() {
        return StringUtils.notNullStr(imageUrl);
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = StringUtils.notNullStr(imageUrl);
    }




}
