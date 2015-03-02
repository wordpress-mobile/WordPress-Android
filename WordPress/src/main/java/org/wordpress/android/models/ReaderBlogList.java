package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReaderBlogList extends ArrayList<ReaderBlog> {

    @Override
    public Object clone() {
        return super.clone();
    }

    public static ReaderBlogList fromJson(JSONObject json) {
        ReaderBlogList blogs = new ReaderBlogList();
        if (json == null) {
            return blogs;
        }

        // read/following/mine response
        JSONArray jsonBlogs = json.optJSONArray("subscriptions");
        if (jsonBlogs != null) {
            for (int i = 0; i < jsonBlogs.length(); i++) {
                ReaderBlog blog = ReaderBlog.fromJson(jsonBlogs.optJSONObject(i));
                // make sure blog is valid before adding to the list - this can happen if user
                // added a URL that isn't a feed or a blog since as of 29-May-2014 the API
                // will let you follow any URL regardless if it's valid
                if (blog.hasName() || blog.hasDescription() || blog.hasUrl()) {
                    blogs.add(blog);
                }
            }
        }

        return blogs;
    }

    private int indexOfBlogId(long blogId) {
        for (int i = 0; i < size(); i++) {
            if (this.get(i).blogId == blogId) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfFeedId(long feedId) {
        for (int i = 0; i < size(); i++) {
            if (this.get(i).feedId == feedId) {
                return i;
            }
        }
        return -1;
    }

    public boolean isSameList(ReaderBlogList blogs) {
        if (blogs == null || blogs.size() != this.size()) {
            return false;
        }

        for (ReaderBlog blogInfo: blogs) {
            int index;
            if (blogInfo.feedId != 0) {
                index = indexOfFeedId(blogInfo.feedId);
            } else {
                index = indexOfBlogId(blogInfo.blogId);
            }
            if (index == -1 || !this.get(index).isSameAs(blogInfo)) {
                return false;
            }
        }

        return true;
    }
}
