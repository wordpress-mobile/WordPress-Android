package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;

public class ReaderFollowedBlog {
    public long blogId;
    private String url;

    /* set by the response to the read/following/mine endpoint
    {
			"ID": "146313067",
			"blog_ID": "52451191",
			"URL": "http://nickbradbury.com",
			"date_subscribed": "2014-04-23T00:15:02+00:00"
		},
    }*/

    public static ReaderFollowedBlog fromJson(JSONObject json) {
        ReaderFollowedBlog blog = new ReaderFollowedBlog();
        if (json == null) {
            return blog;
        }

        blog.blogId = json.optLong("blog_ID");
        blog.setUrl(JSONUtil.getString(json, "URL"));

        return blog;
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }
}
