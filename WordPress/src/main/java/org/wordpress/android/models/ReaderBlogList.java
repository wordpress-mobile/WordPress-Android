package org.wordpress.android.models;

import android.support.annotation.NonNull;

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

    public boolean isSameList(ReaderBlogList blogs) {
        if (blogs == null || blogs.size() != this.size()) {
            return false;
        }

        for (ReaderBlog blogInfo : blogs) {
            int index = indexOfBlogId(blogInfo.blogId);
            if (index == -1) {
                return false;
            }
            ReaderBlog thisInfo = this.get(index);
            if (!thisInfo.isSameAs(blogInfo)) {
                return false;
            }
        }

        return true;
    }

    /*
     * returns true if the passed blog list has the same blogs that are in this list - differs
     * from isSameList() in that isSameList() checks for *any* changes (subscription count, etc.)
     * whereas this only checks if the passed list has any blogs that are not in this list, or
     * this list has any blogs that are not in the passed list
     */
    public boolean hasSameBlogs(@NonNull ReaderBlogList blogs) {
        if (blogs.size() != this.size()) {
            return false;
        }

        for (ReaderBlog blogInfo : blogs) {
            if (indexOfBlogId(blogInfo.blogId) == -1) {
                return false;
            }
        }

        return true;
    }
}
